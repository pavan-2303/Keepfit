# Backlog Initiative - Open-source Release and Content Stewardship

> **Status:** Approved - first slice complete
> **Owner:** Product owner
> **Last updated:** 2026-09-16

---

## Goal

Publish Keepfit's code under Apache-2.0 without accidentally licensing future
original exercise media, while making third-party attribution unobtrusive and
preparing the repository for responsible public releases.

## User Value

- Exercise guidance is easier to scan during a workout.
- Licence text no longer competes with exercise instructions.
- Users can still inspect every third-party notice from one predictable place.
- Contributors can understand which material is open source and which media
  requires separate permission.

## Why This Matters

Keepfit is approaching public distribution. Its current per-exercise source
line is visually noisy, while relying only on README attribution would omit
licence information from installed binary distributions. Clear mixed-licence
boundaries protect both downstream users and future original media.

## Scope

- Apache-2.0 licensing for Keepfit source code.
- Explicit restricted-media boundaries for separately identified future assets.
- Consolidated repository and in-app third-party notices.
- A structured, workout-friendly exercise detail experience.
- Future community-health, CI, release, and repository-governance setup after
  the owner approves publication details.

## Non-Scope

- Rewording upstream instructions to conceal their source.
- Claiming copyright over upstream catalogue metadata or instructions.
- Publishing the repository, creating a tag, or creating a GitHub release
  without an explicit release decision.
- Adding proprietary media before ownership and model-release records exist.

## Dependencies and Constraints

- The pinned dataset's MIT notice must accompany distributions containing its
  metadata and English instructions.
- GitHub public-repository terms permit viewing and forking hosted content, so
  source-quality restricted media should remain outside the public repository.
- Mixed licensing must identify its file and directory boundaries explicitly.

## Product and System Impact

- `feature:workouts`: exercise detail information architecture and tests.
- `feature:settings`: compact, centralized open-source licence disclosure.
- Repository root and catalogue assets: licence and notice files.
- Release documentation: public-distribution checks and immutable evidence.

## Proposed Sprint Decomposition

| Planned Version | Sprint Goal | Estimated Effort | Notes |
|---|---|---:|---|
| [v0.20.0](../../done/versions/v0.20.0.md) | Establish licence boundaries and make exercise details practical | 5-8d | Complete |
| Unversioned follow-up | Add community health, CI, release automation, and first public release | 5-8d | Requires explicit visibility, release, and signing decisions |

## Completion Criteria

- [x] Apache-2.0 and restricted-media boundaries are unambiguous.
- [x] Third-party notices remain available without appearing on every exercise.
- [x] Exercise details are structured and accessible at phone width and large text.
- [ ] Public repository and release setup is completed or explicitly descoped.

## Open Questions

- What legal person or entity should be named as the copyright holder for
  future restricted Keepfit media?
- Should the first public release be v0.20.0 or a later stability build?
