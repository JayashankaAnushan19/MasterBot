# MasterBot — Project Spec

## 1. What this is

MasterBot is an offline-first, gamified mastery app for robotics knowledge, covering three pillars: **IT, Mechanical, Electronic**. It has no login. All content, rules, and progress logic are driven by data living in the `MasterBot_Repo` git repository — the Android app is a renderer + local engine that syncs with that repo. The person using it is already on the robotics path and wants an energetic tool to sharpen and master skills — not a beginner-hand-holding app.

Tone: energetic, confident, momentum-building. Gamified but not childish. Think "training simulator for someone already skilled," not "tutorial for a beginner."

Repo: `MasterBot_Repo` — **public**, so the app can clone/pull with zero authentication. Never commit secrets, tokens, or credentials to this repo.

---

## 2. Repo structure

```
MasterBot_Repo/
├── README.md
├── schema/
│   ├── card.schema.json          # validates every cards.yaml entry
│   └── note.schema.md            # format rules for notes.md
├── rules/
│   └── adaptation_rules.yaml     # the single source of truth for weighting/SRS logic
├── subjects/
│   ├── it/
│   │   ├── programming/
│   │   │   └── <topic-slug>/
│   │   │       ├── notes.md
│   │   │       ├── cards.yaml
│   │   │       └── audio/        # optional pre-generated TTS mp3s
│   │   ├── algorithms/
│   │   ├── linux-embedded/
│   │   └── networking/
│   ├── mechanical/
│   │   ├── statics-dynamics/
│   │   ├── materials-manufacturing/
│   │   ├── kinematics/
│   │   └── actuators-drivetrains/
│   └── electronic/
│       ├── circuit-theory/
│       ├── sensors/
│       ├── power-systems/
│       └── microcontrollers/
├── tools/
│   └── build_index.py            # CI script: validates + flattens repo into index.json
├── index.json                    # AUTO-GENERATED — do not hand-edit
└── .github/workflows/build-index.yml
```

### Design rule
The app never parses raw markdown/YAML for indexing — it only ever pulls `index.json` for fast sync, then lazily fetches a topic's `notes.md`/`audio/` on demand when opened. `index.json` is rebuilt by CI on every push to `main`.

---

## 3. `cards.yaml` format (per topic)

```yaml
topic: "ros2-nodes-topics"
pillar: "it"
module: "programming"
difficulty_base: 2          # 1 (foundation) - 5 (advanced/expert)
cards:
  - id: "it-prog-ros2-001"
    type: "qa"               # qa | mcq | fill_blank | matching
    question: "What is the difference between a ROS2 topic and a service?"
    answer: "Topics are asynchronous pub/sub streams; services are synchronous request/response calls."
    options: []               # populated only for type: mcq
    tags: ["ros2", "communication"]
    weight_seed: 1.0          # starting difficulty weight before any answer history
```

## 4. `notes.md` format (per topic)
Plain markdown, human-written, explanatory — the "textbook" behind the cards. First H1 = topic title. No strict schema beyond that; content quality matters more than format here.

## 5. `rules/adaptation_rules.yaml` — the adaptive engine spec

This is the file both the sync engine and app read to decide what to show and when. Keep it declarative — no logic embedded in app code that isn't traceable back to this file.

```yaml
version: 1

spaced_repetition:
  algorithm: "sm2_modified"
  initial_interval_days: 1
  ease_factor_default: 2.5
  ease_factor_min: 1.3

weighting:
  on_correct_fast:      { ease_delta: +0.15, weight_delta: -0.2 }
  on_correct_slow:      { ease_delta: +0.05, weight_delta: -0.05 }
  on_incorrect:         { ease_delta: -0.2,  weight_delta: +0.4 }
  slow_response_threshold_ms: 6000

module_health:
  weak_module_trigger:
    condition: "avg_weight_over_last_n_days >= 0.6"
    window_days: 7
    action: "boost_new_concept_share"
    boost_multiplier: 1.5
  mastery_thresholds:
    bronze: { avg_weight_below: 0.5, min_cards_reviewed: 10 }
    silver: { avg_weight_below: 0.3, min_cards_reviewed: 25 }
    gold:   { avg_weight_below: 0.15, min_cards_reviewed: 50 }

daily_task_generation:
  review_cards_count: 8
  new_concept_count: 1
  bonus_task_enabled: true
  listening_session_enabled: true

rewards:
  coins_per_correct: 10
  coins_per_fast_correct_bonus: 5
  streak_multiplier_per_week: 0.1
  streak_multiplier_cap: 2.0
```

Every future content/logic update should be a diff to this file plus the `subjects/` tree — never a silent behavior change buried in app code.

---

## 6. Android app architecture

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin, native | Best offline reliability, first-class background work |
| Git sync | JGit | Clone/pull/push without shelling out to a git binary |
| Local storage | Room (SQLite) | Fast structured queries, works fully offline |
| Background jobs | WorkManager | Reminders, background sync |
| TTS | Android `TextToSpeech` API | Zero-authoring listening mode; falls back to pre-baked mp3s if present in `audio/` |
| No login | N/A | Public repo, anonymous clone/pull; local progress has no server dependency |

### Sync safety rule (non-negotiable)
Sync must **never** disrupt an in-progress review session or destroy unsynced local progress:
1. Pulled data lands in a **staging table**, never overwrites live tables directly.
2. Merge from staging → live only happens at safe boundaries: app cold start, or when no active session is running.
3. Local answer logs and new/edited notes are **pushed before any pull** on each sync cycle, so local progress is never at risk of being clobbered by an incoming pull.
4. If a sync arrives mid-session, the current session finishes against its original snapshot; merge happens immediately after.

---

## 7. Build stages (execution order)

- **Stage 0** — Repo skeleton: folder structure, schema files, empty `adaptation_rules.yaml`, README. *(this file's companion deliverable)*
- **Stage 1** — Seed 2-3 real topics per pillar by hand to validate the format against real content.
- **Stage 2** — `tools/build_index.py` + GitHub Action: validate all `cards.yaml` against schema, flatten into `index.json`, auto-commit.
- **Stage 3** — Local engine only (no UI): implement SRS + weighting logic against sample `index.json` + fake answer logs, prove correctness in isolation.
- **Stage 4** — Android skeleton: Room + JGit clone/pull (read-only), one swipeable review screen end-to-end.
- **Stage 5** — Offline-safe sync (staging table, merge-on-boundary, push-before-pull) per section 6 above.
- **Stage 6** — Rewards, streaks, coins, WorkManager notifications.
- **Stage 7** — Fun layer: mini-games (drag-and-drop circuit matching, pub/sub matching game), weekly boss review mixing all 3 pillars, robot avatar progression tied to mastery badges.
- **Stage 8** — In-app note-taking (queued writes, pushed on sync) + listening mode (TTS/audio playback), reusing the Stage 5 offline-queue pattern.

---

## 8. Guardrails for whoever (human or Claude) works on this repo next

- `index.json` is generated, never hand-edited.
- Any change to scoring/weighting behavior must be expressed as a change to `adaptation_rules.yaml`, not hardcoded in app logic.
- Never commit API keys, tokens, or credentials — repo is public.
- Content additions always go through: add/edit `notes.md` + `cards.yaml` → push → CI rebuilds `index.json` → app picks it up on next pull.
- Preserve the sync-safety rule in section 6 in every future change to sync code — it is the single most important UX guarantee of this app (never lose or interrupt a user's in-progress work).
