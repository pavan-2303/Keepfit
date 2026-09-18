# Keepfit Documentation

This `docs/` directory is the entry point for product planning, execution
tracking, shipped release records, and reference material.

## Documentation Lifecycle

```text
backlog initiative
  -> sprint version spec
  -> execution log
  -> done record
```

## Folder Map

| Folder | Purpose |
|---|---|
| `AGENTS.md` | Documentation-specific operating rules |
| `backlog/` | Multi-sprint initiative planning |
| `versions/` | Execution-ready sprint version specs |
| `execution/` | Live tracking for versions in progress |
| `done/` | Immutable completion records |
| `ROADMAP.md` | Future themes and sequencing without speculative semver |
| `references/` | Timeless references, guides, architecture, and archive material |

Existing Keepfit references remain in their established locations:

- [`architecture/`](architecture/) - current system architecture and data model
- [`user-manual/`](user-manual/) - end-user guidance

Superseded planning documents are removed from the working tree. Git history
is the archive for old plans and designs.

## Versioning Rules

- the Android app currently reports version `0.24.0`; v0.11.0 through
  [v0.19.0](done/versions/v0.19.0.md) and the first two documented initiatives are complete
- [v0.20.0](done/versions/v0.20.0.md) completed the exercise-clarity and licence-boundary increment
- [v0.21.0](done/versions/v0.21.0.md) completed the release-candidate navigation and conversation-integrity increment
- [v0.22.0](done/versions/v0.22.0.md) completed the practical template-maintenance increment
- [v0.23.0](done/versions/v0.23.0.md) completed the personal exercise-building increment
- [v0.24.0](done/versions/v0.24.0.md) completed the assessment-driven planning increment
- future sprint delivery increments from there
- sprint versions should be real, execution-ready, and user-visible
- large features must start in backlog before they are sliced into versions

For detailed behavior, read `docs/AGENTS.md`.
