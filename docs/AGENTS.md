# Documentation Workflow Rules

This file governs planning and release-record work inside `docs/`.

## Purpose

The documentation system separates:

- `backlog/` for initiative planning
- `versions/` for execution-ready sprint specs
- `execution/` for live delivery tracking
- `done/` for immutable completion records

## Mandatory rules

### Explore before planning

- Read relevant code and docs before writing new plans
- Reuse current architecture and established feature flows

### Large features start in backlog

- Multi-sprint work begins in `backlog/items/`

### Sprint versions must be real

- Each version should fit roughly 8-12 engineering days
- Each version must ship a working increment
- Placeholder-only versions are forbidden

### Version specs freeze when execution starts

- Do not edit `versions/` docs after implementation begins
- Record scope changes in `execution/` instead

### Done records are immutable

- Do not edit done records except through dated addenda
- Each done record must include a changelog section

## Folder rules

- `backlog/` = initiative docs
- `versions/` = sprint version specs
- `execution/` = in-progress tracking
- `done/` = immutable completion history
- `references/` = timeless or archived material

## What not to do

- do not put sprint work directly into backlog items
- do not use roadmap themes as committed sprint plans
- do not mutate version specs mid-execution
- do not use done docs as live planning documents
