# Backlog Initiative - Durable Personalized Fitness Companion

> **Status:** Complete - v0.11.0 through v0.19.0 delivered and final repository hardening verified
> **Owner:** Product owner
> **Last updated:** 2026-09-16
> **Decision authority:** The product outcomes, sequencing, and source-licensing
> boundaries below are approved. Each planned version receives a frozen version
> specification before implementation starts.

---

## Goal

Make Keepfit dependable across reinstalls, useful for several local profiles,
complete without runtime catalogue APIs, and capable of sustaining private,
long-running conversations with a chosen Coach. Improve onboarding,
transformation tracking, and visual character without weakening the offline,
local-first product.

## User Value

- A person can recover structured fitness records after reinstall when Android
  backup is available and always has an explicit encrypted-backup fallback.
- Several people can use one device without mixing plans, logs, measurements,
  photos, reviews, or Coach conversations.
- A large exercise catalogue is available immediately and offline.
- First run captures useful context but still permits a fully manual plan.
- Transformation check-ins can stay simple or expand to detailed pose sets.
- Coach conversations persist, use the active profile deliberately, and offer
  distinct communication styles.
- The application feels polished and motivating without noisy gamification.

## Why This Matters

The v0.10.0 reconstruction made the main journey clearer, but phone review
identified trust and continuity gaps that affect daily adoption: assistant
formatting, reinstall recovery, single-profile assumptions, shallow first-run
context, temporary conversation history, and limited transformation poses.
Addressing those foundations before adding more isolated features makes the
application more practical to maintain and safer to extend.

## Approved Product Decisions

1. The v0.10.0 Markdown-rendering defect is corrected in its live execution
   record before that version closes; it is not hidden inside a future feature.
2. Android platform backup is a convenience, not a guarantee. Explicit
   encrypted export and restore remains the authoritative full-data recovery
   path.
3. Automatic OpenRouter credential recovery is optional, encrypted, and
   validated after restoration. Failure falls back to reconnecting; Keepfit
   does not request an OpenRouter management key.
4. Profiles are local and switch through a compact avatar menu. No account,
   social relationship, or family-role model is introduced.
5. Exercises and foods may be shared device-wide reference libraries, while
   plans, logs, goals, saved meals, reviews, measurements, photos, journey
   answers, enabled poses, and Coach conversations belong to a profile.
6. The hasaneyldrm exercise dataset may supply MIT-covered metadata and
   instruction text. Its Gym visual images and GIFs are excluded unless
   Keepfit obtains a separate licence.
7. The first four relaxed transformation poses are enabled by default. The
   remaining eleven are individually selectable per profile.
8. The attached pose poster is a taxonomy and composition reference only.
   Keepfit ships original, rights-recorded reference artwork.
9. BMI is derived from height and latest weight. Age and an optional
   calculation-sex field are collected only where they drive visible behavior;
   gender is not presented as a BMI input.
10. AI planning is optional. Offline starter planning and a manual-plan path
    remain available without OpenRouter.
11. Named Coaches are personality presets over the selected provider model,
    not independent human trainers or unrestricted agents. All share the same
    privacy, safety, and approval rules.

## Scope

### Trust and continuity

- Render a safe Markdown subset in Coach messages without executing HTML or
  loading model-supplied remote media.
- Add scoped Android backup rules for Room and non-secret preferences.
- Exclude credentials, temporary OAuth state, imported exercise media, and
  transformation photos from platform backup.
- Keep encrypted manual backup and restore for complete database, settings,
  and app-private media recovery.
- Evaluate an explicit opt-in Android Block Store adapter for restoring the
  same OpenRouter token after reinstall, with availability and token-validity
  fallbacks.

### Multiple local profiles

- Add, edit, archive, and switch local profiles from an avatar menu.
- Store the active profile identifier in DataStore.
- Migrate existing records atomically to the existing profile.
- Add profile ownership to every personal plan, log, goal, review, preference,
  transformation, and Coach record and query.
- Include every profile in encrypted backups and prove there is no cross-profile
  leakage.

### Transformation pose sets

- Replace the four-value angle concept with stable pose identifiers.
- Migrate `FRONT`, `RIGHT`, `BACK`, and `LEFT` to their relaxed equivalents.
- Support the approved 15-pose taxonomy.
- Default to front relaxed, right side relaxed, back relaxed, and left side
  relaxed; allow any additional pose to be enabled or disabled per profile.
- Permit incomplete check-ins and show completion as a count, not a failure.
- Compare only the same pose across dates and explain missing counterparts.
- Provide original reference thumbnails, short positioning instructions, and a
  camera-alignment overlay.
- Follow the stable taxonomy and migration mapping in the
  [transformation pose catalogue](../../references/transformation-pose-catalogue.md).
- Normalize oversized imports, remove location metadata, keep files private,
  and preserve backup compatibility.

### Bundled offline exercise catalogue

- Pin and import an audited revision of
  `hasaneyldrm/exercises-dataset` at build time.
- Include the MIT licence and copyright notice in repository and in-app legal
  notices.
- Convert source identifiers to deterministic UUIDs while retaining source ID
  and provenance.
- Normalize names, equipment, muscles, instructions, and duplicate entries.
- Ship searchable offline metadata and English instructions for the approved
  records; add other languages only after quality review.
- Remove the runtime ExerciseDB catalogue path once offline coverage and
  upgrade compatibility are verified.
- Add original or separately licensed visuals in small, rights-recorded packs;
  exercises remain useful through text when no media is present.

### Onboarding and planning

- Use progressive steps for name, birth date/age, height, initial weight,
  goals, experience, availability, duration, equipment, movement constraints,
  and nutrition preference.
- Explain why sensitive or optional inputs are requested and allow them to be
  skipped and edited later.
- Save initial weight as the first measurement and derive BMI locally.
- Offer three outcomes: use the offline starter plan, ask AI to personalize a
  draft, or skip planning and build manually.

### Persistent named Coaches and AI planning

- Persist conversations and messages in Room by profile and Coach.
- Send a bounded recent-message window plus a rolling summary instead of the
  entire conversation indefinitely.
- Keep fitness facts authoritative in Room and assemble them only when the
  question calls for personal progress context.
- Provide new-chat, rename, delete, clear-memory, context-disclosure, and
  backup controls.
- Offer several named, visually distinct communication styles, including warm,
  direct, and analytical, while preserving common safety behavior.
- Generate AI plans through strict structured output referencing bundled
  exercise UUIDs; validate locally and show a draft before any apply action.
- Allow the same preview-and-approval workflow to review manual workout and
  nutrition plans.

### Visual identity and motion

- Strengthen typography, color, illustration, surfaces, and Coach identity.
- Use native Compose motion for navigation, progressive disclosure, progress,
  completion, and small celebrations.
- Add restrained haptics where they confirm a completed action.
- Respect system animation settings, large text, screen readers, contrast,
  battery, and low-end-device performance.

## Non-Scope

- A Keepfit account, hosted backend, mandatory cloud sync, or social network.
- Developer-funded AI inference or a packaged shared provider key.
- Automatic upload of transformation photos or raw personal records to AI.
- Reuse of Gym visual media without a separate valid licence.
- Shipping all 1,324 exercises with custom animation in one sprint.
- Medical diagnosis, treatment, rehabilitation prescriptions, eating-disorder
  coaching, humiliation, or unsafe "brutal" Coach behavior.
- A paid-model decision before free-model reliability and usage are measured.

## Dependencies and Constraints

- v0.10.0 closes only after its Markdown defect is corrected and verified.
- Automatic Android restore depends on device backup settings, Google services,
  quota, timing, and OEM behavior; UI wording must not promise certainty.
- Block Store support is optional and must be hidden when unavailable.
- Multi-profile ownership must land before persistent Coach memory and
  per-profile pose settings.
- The stable offline catalogue must land before AI-generated plans reference
  exercise identifiers.
- The source dataset is pinned to an audited commit; updates are deliberate,
  diffed, normalized, and revalidated rather than pulled during every build.
- Transformation and exercise media require per-asset rights records.
- Every schema change increments the Room version, exports the schema, updates
  backup compatibility, and includes upgrade tests.

## Product and System Impact

- `app`: active-profile routing, profile switcher, onboarding, recovery UX.
- `core:database`: profile ownership, pose identifiers/preferences, Coach
  conversations, catalogue provenance, and migrations.
- `core:preferences`: active profile, optional credential-recovery consent,
  non-secret backup rules, and reduced-motion preference where needed.
- `core:media`: pose guidance assets and privacy-preserving photo processing.
- `feature:assistant`: Markdown, durable conversations, personas, structured
  plan review, context disclosure, and credential recovery adapter.
- `feature:transformation`: selectable pose sets, guided capture, and matching
  comparison.
- `feature:workouts`: bundled catalogue import/search and stable AI references.
- `feature:nutrition`, `feature:review`, and `feature:settings`: complete
  profile scoping and recovery controls.

## Proposed Sprint Decomposition

| Planned Version | Sprint Goal | Estimated Effort | Working Increment |
|---|---|---:|---|
| v0.11.0 | Reinstall-safe data continuity | 8-12d | Structured records and non-secret preferences can restore through scoped Android backup; full encrypted backup remains available; optional AI credential recovery fails safely. |
| v0.12.0 | Multiple local profiles with complete isolation | 10-12d | Users can add and switch profiles from the app bar, and every personal workflow reads and writes only the active profile. |
| v0.13.0 | Flexible transformation pose sets | 8-12d | Existing photos migrate, the first four poses remain simple, and any of 15 original-guided poses can be enabled and compared. |
| v0.14.0 | Large bundled offline exercise catalogue | 10-12d | The audited MIT metadata catalogue is searchable offline with stable Keepfit IDs and no runtime catalogue API. |
| v0.15.0 | Owned core exercise guidance media | 8-12d | A prioritized pack of roughly 20-30 common exercises gains original or separately licensed lightweight guidance media and a reusable rights-aware pipeline. |
| v0.16.0 | Progressive onboarding and manual planning | 8-12d | First run records useful baseline context, calculates BMI locally, and offers offline, AI, or fully manual planning paths. |
| v0.17.0 | Persistent named Coaches | 10-12d | Profile-scoped conversations survive restart, maintain bounded memory, and support selectable warm, direct, and analytical Coach styles. |
| v0.18.0 | AI-assisted plan creation and review | 8-12d | A connected user can request a validated plan draft during onboarding or review a manual workout/nutrition plan before explicitly applying changes. |
| v0.19.0 | Intentional visual identity and motion | 8-12d | Core journeys receive consistent styling, meaningful motion, restrained delight, accessibility, and performance verification. |

Version specifications are created one at a time. The next spec is prepared
after the preceding version closes and its dependency remains valid.

## Sequencing Rules

1. Correct Markdown and close v0.10.0.
2. Protect existing data before introducing more profile-owned data.
3. Land profile isolation before pose preferences and Coach history.
4. Land stable exercise identifiers before AI plan generation.
5. Stabilize onboarding and Coach memory before enabling assistant mutations.
6. Apply broad visual polish after the primary information architecture and
   behavior are stable.

The exercise media pack may proceed alongside onboarding only after catalogue
IDs and the media rights pipeline are stable. It does not block text-first
offline exercise use.

## Testing and Evidence Strategy

- Start every feature or fix with a focused failing test.
- Add Room migration tests from every supported schema to the new schema.
- Add cross-profile isolation tests for each DAO and repository.
- Test replacement install, uninstall/reinstall restore where the platform
  supports it, explicit encrypted export/restore, unavailable backup services,
  and invalid recovered tokens.
- Validate every imported catalogue record against a checked-in schema and
  produce deterministic duplicate, missing-field, and rights reports.
- Compose-test compact width, 200% font, keyboard, screen-reader labels,
  incomplete photo check-ins, missing comparisons, offline onboarding, and
  disconnected AI flows.
- Maintain AI fixtures for Markdown, persona consistency, privacy filtering,
  long-history summarization, invalid exercise IDs, unsafe plans, and provider
  quota failures.
- Measure APK size, startup, catalogue search, scrolling, image memory, and
  animation performance on a representative physical phone.

## Completion Criteria

- [x] Current-version Markdown correction is shipped and verified in
      [v0.10.0](../../done/versions/v0.10.0.md).
- [x] Recovery behavior is accurate, privacy-preserving, and tested without
      promising guaranteed platform restore.
- [x] Multiple profiles cannot read or mutate one another's personal data.
- [x] The offline catalogue is large, searchable, deterministic, and legally
      distributable without a runtime provider.
- [x] The first four transformation poses stay quick while all approved poses
      are selectable and comparable.
- [x] Onboarding supports offline, AI-assisted, and manual planning without a
      forced account.
- [x] Coach conversations persist with bounded memory and profile-aware
      context controls.
- [x] AI plan changes remain locally validated and approval-only.
- [x] Visual polish passes accessibility and emulator performance verification;
      representative physical-phone feel is part of owner acceptance review.
- [x] All planned versions are complete or explicitly descoped into a new
      initiative.

## Conditional Future Checks

These are not unfinished work in the delivered private version. They become
release gates only if their related future scope is approved.

- Reconsider calculation sex only if a future approved energy-target feature
  provides enough visible value to justify collecting that sensitive input.
- Validate Block Store behavior on the supported physical-device matrix before
  promising the AI connection can be recovered.
- Review non-English catalogue instructions before enabling each language.
- Approve every original or licensed exercise and pose asset in the rights
  register before it enters the APK.

## Research References

- [Exercise dataset](https://github.com/hasaneyldrm/exercises-dataset)
- [Dataset MIT and media exception](https://github.com/hasaneyldrm/exercises-dataset/blob/main/LICENSE)
- [Dataset media notice](https://github.com/hasaneyldrm/exercises-dataset/blob/main/NOTICE.md)
- [Android Auto Backup](https://developer.android.com/identity/data/autobackup)
- [Android Block Store](https://developer.android.com/identity/block-store)
- [OpenRouter OAuth PKCE](https://openrouter.ai/docs/guides/overview/auth/oauth)
- [Compose animation guidance](https://developer.android.com/develop/ui/compose/quick-guides/content/video/animation-in-compose)
- [Transformation pose catalogue](../../references/transformation-pose-catalogue.md)
