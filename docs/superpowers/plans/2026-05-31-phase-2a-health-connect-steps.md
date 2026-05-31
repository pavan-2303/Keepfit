# Keepfit Phase 2A Health Connect Steps Implementation Plan

**Goal:** Add optional Health Connect step tracking without coupling it to the
offline MVP.

**Architecture:** Add `feature:steps` for Health Connect availability,
permission handling, aggregate reads, and Today dashboard UI.

---

### Task 1: Branding

- [x] Add launcher icons from `assets/logo-icon.png`.
- [x] Use the same asset as lightweight in-app branding.

### Task 2: Health Connect Steps

- [x] Add `feature:steps` module and Health Connect dependency.
- [x] Implement availability detection and permission status mapping.
- [x] Implement read-only daily and seven-day step aggregation.
- [x] Add Today dashboard steps UI with unavailable, denied, and connected states.
- [x] Verify compile, tests, lint, build, install, and emulator launch behavior.
