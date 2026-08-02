#!/usr/bin/env python3
"""
Validates every subjects/**/cards.yaml against schema/card.schema.json and
subjects/**/notes.md against the notes format rule, then flattens the whole
subjects/ tree into a single index.json at the repo root.

Usage: python tools/build_index.py [--check]

--check   Validate and report only; do not write index.json. Used in CI to
          fail fast on a bad diff before the auto-commit step runs.

Exit code is non-zero if any validation fails.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

import yaml
from jsonschema import Draft7Validator

REPO_ROOT = Path(__file__).resolve().parent.parent
SUBJECTS_DIR = REPO_ROOT / "subjects"
SCHEMA_PATH = REPO_ROOT / "schema" / "card.schema.json"
INDEX_PATH = REPO_ROOT / "index.json"

VALID_PILLARS = {"it", "mechanical", "electronic"}
SLUG_RE = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")


class ValidationError(Exception):
    pass


def load_schema() -> Draft7Validator:
    with open(SCHEMA_PATH, encoding="utf-8") as f:
        schema = json.load(f)
    return Draft7Validator(schema)


def validate_notes_md(notes_path: Path, errors: list[str]) -> None:
    if not notes_path.exists():
        errors.append(f"{notes_path}: missing notes.md")
        return
    text = notes_path.read_text(encoding="utf-8")
    lines = [line for line in text.splitlines() if line.strip() != ""]
    if not lines:
        errors.append(f"{notes_path}: file is empty")
        return
    if not lines[0].startswith("# "):
        errors.append(f"{notes_path}: first non-blank line must be a single H1 ('# Title')")
    h1_count = sum(1 for line in lines if line.startswith("# "))
    if h1_count > 1:
        errors.append(f"{notes_path}: found {h1_count} H1 headings, must be exactly 1")


def validate_and_load_cards(
    cards_path: Path,
    pillar_slug: str,
    module_slug: str,
    topic_slug: str,
    validator: Draft7Validator,
    seen_ids: dict[str, Path],
    errors: list[str],
) -> dict | None:
    try:
        with open(cards_path, encoding="utf-8") as f:
            data = yaml.safe_load(f)
    except yaml.YAMLError as e:
        errors.append(f"{cards_path}: YAML parse error: {e}")
        return None

    schema_errors = sorted(validator.iter_errors(data), key=lambda e: e.path)
    if schema_errors:
        for e in schema_errors:
            loc = "/".join(str(p) for p in e.path) or "<root>"
            errors.append(f"{cards_path}: schema violation at '{loc}': {e.message}")
        return None

    if data["pillar"] != pillar_slug:
        errors.append(
            f"{cards_path}: pillar '{data['pillar']}' does not match folder path pillar '{pillar_slug}'"
        )
    if data["module"] != module_slug:
        errors.append(
            f"{cards_path}: module '{data['module']}' does not match folder path module '{module_slug}'"
        )
    if data["topic"] != topic_slug:
        errors.append(
            f"{cards_path}: topic '{data['topic']}' does not match folder name '{topic_slug}'"
        )

    for card in data["cards"]:
        cid = card["id"]
        if cid in seen_ids:
            errors.append(
                f"{cards_path}: duplicate card id '{cid}' (also used in {seen_ids[cid]})"
            )
        else:
            seen_ids[cid] = cards_path

    return data


def build_index() -> tuple[dict, list[str]]:
    errors: list[str] = []
    validator = load_schema()
    seen_ids: dict[str, Path] = {}
    topics = []

    if not SUBJECTS_DIR.exists():
        return {}, [f"{SUBJECTS_DIR}: subjects/ directory not found"]

    for pillar_dir in sorted(SUBJECTS_DIR.iterdir()):
        if not pillar_dir.is_dir():
            continue
        pillar_slug = pillar_dir.name
        if pillar_slug not in VALID_PILLARS:
            errors.append(f"{pillar_dir}: unknown pillar '{pillar_slug}', expected one of {sorted(VALID_PILLARS)}")
            continue

        for module_dir in sorted(pillar_dir.iterdir()):
            if not module_dir.is_dir():
                continue
            module_slug = module_dir.name

            for topic_dir in sorted(module_dir.iterdir()):
                if not topic_dir.is_dir():
                    continue
                topic_slug = topic_dir.name
                cards_path = topic_dir / "cards.yaml"
                notes_path = topic_dir / "notes.md"

                if not cards_path.exists():
                    # topic folder has no cards.yaml yet -> not a topic, skip silently
                    # (e.g. stray non-topic subfolder); only flag if notes.md exists
                    # without cards.yaml, which is a real authoring mistake.
                    if notes_path.exists():
                        errors.append(f"{topic_dir}: notes.md present but cards.yaml missing")
                    continue

                validate_notes_md(notes_path, errors)
                data = validate_and_load_cards(
                    cards_path, pillar_slug, module_slug, topic_slug, validator, seen_ids, errors
                )
                if data is None:
                    continue

                audio_dir = topic_dir / "audio"
                has_audio = audio_dir.is_dir() and any(audio_dir.iterdir())

                topics.append(
                    {
                        "id": f"{pillar_slug}/{module_slug}/{topic_slug}",
                        "pillar": pillar_slug,
                        "module": module_slug,
                        "topic": topic_slug,
                        "difficulty_base": data["difficulty_base"],
                        "notes_path": str(notes_path.relative_to(REPO_ROOT)).replace("\\", "/"),
                        "has_audio": has_audio,
                        "cards": data["cards"],
                    }
                )

    topics.sort(key=lambda t: t["id"])
    index = {
        "version": 1,
        "generated_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "topic_count": len(topics),
        "card_count": sum(len(t["cards"]) for t in topics),
        "topics": topics,
    }
    return index, errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="validate only, do not write index.json")
    args = parser.parse_args()

    index, errors = build_index()

    if errors:
        print(f"FAILED: {len(errors)} validation error(s):\n", file=sys.stderr)
        for e in errors:
            print(f"  - {e}", file=sys.stderr)
        return 1

    print(f"OK: {index['topic_count']} topic(s), {index['card_count']} card(s) validated.")

    if not args.check:
        with open(INDEX_PATH, "w", encoding="utf-8", newline="\n") as f:
            json.dump(index, f, indent=2, ensure_ascii=False)
            f.write("\n")
        print(f"Wrote {INDEX_PATH.relative_to(REPO_ROOT)}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
