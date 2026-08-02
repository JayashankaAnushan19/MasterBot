# `notes.md` format rules

Every topic folder (`subjects/<pillar>/<module>/<topic-slug>/notes.md`) is a plain,
human-written markdown file â the "textbook" behind that topic's `cards.yaml`.
Content quality matters far more than structure here, but a few rules keep it
usable by the app and consistent across topics:

## Required

- The file MUST start with a single `#` H1, and that H1 is used as the topic's
  display title in the app. Example: `# ROS2 Nodes & Topics`.
- Only one H1 per file.

## Recommended structure

No section beyond the H1 is enforced, but topics read best when they follow a
loose shape like:

```markdown
# <Topic Title>

Short framing paragraph: why this matters, where it shows up in real robotics work.

## Core concepts
...

## How it works
...

## Common pitfalls
...

## Quick reference
...
```

## Style

- Write for someone already on the robotics path, not a first-time learner.
  Skip "what is a variable"-level scaffolding; assume competence, build precision.
- Prefer concrete examples (code, circuits, equations, real component names) over
  abstract description.
- Keep it skimmable: short paragraphs, headers, lists, code blocks where relevant.
- Markdown only â no embedded HTML, no external image hotlinking (bundle images
  alongside `notes.md` in the topic folder if ever needed).

## Relationship to `cards.yaml`

`notes.md` is not indexed or parsed for cards â it's a separate, on-demand fetch
opened when the learner taps "study" on a card or topic. Every concept a
`cards.yaml` entry tests should be explainable by reading `notes.md`, but there is
no required 1:1 mapping between notes sections and card IDs.
