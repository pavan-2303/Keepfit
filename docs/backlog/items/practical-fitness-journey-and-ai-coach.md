# Backlog Initiative - Practical Fitness Journey and Optional AI Coach

> **Status:** In progress - Slices A-H complete; Slice I is executing as v0.10.0;
> follow-on work is tracked in the
> [Durable Personalized Fitness Companion](durable-personalized-fitness-companion.md)
> **Owner:** Product owner
> **Last updated:** 2026-09-13
> **Decision authority:** The product direction and phase sequence are approved
> for planning. Each delivery slice still requires its own approved version
> specification before implementation begins.

> **Completed versions:** [v0.2.0 - Offline Starter Week](../../done/versions/v0.2.0.md),
> [v0.3.0 - Decisive Today](../../done/versions/v0.3.0.md),
> [v0.4.0 - Low-Friction Workout Execution](../../done/versions/v0.4.0.md),
> [v0.5.0 - Weekly Review and Flexible Motivation](../../done/versions/v0.5.0.md),
> [v0.6.0 - Sustainable Nutrition](../../done/versions/v0.6.0.md), and
> [v0.7.0 - Exercise Guidance and Live Demo Prototype](../../done/versions/v0.7.0.md),
> [v0.8.0 - Private OpenRouter Access](../../done/versions/v0.8.0.md), and
> [v0.9.0 - Bounded Coaching Workflows](../../done/versions/v0.9.0.md)
> **Next version:** [v0.10.0 - Guided Journey and Interface Reorganization](../../versions/v0.10.0.md)
> **Follow-on initiative:** [Durable Personalized Fitness Companion](durable-personalized-fitness-companion.md)

---

## 1. Goal

Evolve Keepfit from a collection of fitness tracking features into a private,
adaptive companion that helps one person:

1. make a realistic fitness and nutrition plan;
2. know the most useful action to take today;
3. execute that action with minimal logging friction;
4. recover gracefully when life disrupts the plan;
5. review progress and make a small, understandable weekly adjustment; and
6. stay motivated through evidence of consistency and improvement.

The optional AI coach should enhance this loop without becoming required for
tracking, holding a developer-funded credential, silently changing user data,
or presenting medical advice.

## 2. Context and Direction Change

The existing Android application is the technical starting point. It already
has local profile, workout, nutrition, progress, reminder, backup, step, and
assistant foundations.

Earlier phase-based plans have been retired from the working tree. This
initiative is the sole active product proposal and organizes future work around
the user's ongoing journey rather than isolated feature areas.

The product promise becomes:

> Keepfit makes it obvious what to do today, quick to record what happened,
> and easy to improve next week without guilt or unnecessary complexity.

## 3. Target User and Jobs

### Primary user

A single privacy-conscious Android user who wants practical structure but does
not want a social network, subscription dependency, or complicated coaching
system.

### Core jobs

- Turn a broad goal into a manageable weekly routine.
- Adapt that routine to available time, equipment, experience, and constraints.
- Start and complete today's activity without extensive setup.
- Log workouts and meals quickly enough to sustain the habit.
- Understand whether recent actions are producing useful progress.
- Recover from missed days, illness, travel, low energy, or schedule changes.
- Receive useful encouragement based on real accomplishments.
- Ask for coaching help without surrendering control of local records.

## 4. Product Principles

1. **Today before dashboards.** Lead with the next useful action, not a wall of
   charts.
2. **Useful without AI.** Planning, execution, progression, review, and
   motivation must have deterministic offline behavior.
3. **Flexible consistency.** Rescheduling or completing a shorter alternative
   is progress; a missed day is not failure.
4. **Explain recommendations.** Show the evidence and rule behind every plan
   adjustment.
5. **User approval before mutation.** Suggestions remain drafts until the user
   applies them.
6. **Progress over precision.** Prefer trends and ranges to false precision.
7. **Private by default.** Send the minimum necessary data to an external model
   only after an explicit user action.
8. **No medical role.** Exercise and nutrition guidance is general fitness
   context, not diagnosis or treatment.

## 5. North-Star Experience

```text
Set a realistic goal and constraints
              |
              v
Create or choose a manageable week
              |
              v
See one clear Today plan
              |
              v
Execute quickly and record reality
              |
              v
Recover or reschedule without penalty
              |
              v
Review progress once per week
              |
              v
Approve one small plan adjustment
              |
              +-----------------------> next week
```

The Today and weekly-review experiences should connect workouts, nutrition,
steps, recovery, and motivation. Feature-specific screens remain available for
detail and editing.

## 6. Experience Scope

### 6.1 Goal-based setup

Collect only information that changes the plan:

- primary goal: general fitness, consistency, strength, muscle gain, or fat
  loss;
- experience level;
- preferred training days and minutes available per session;
- available equipment and typical training location;
- dietary preference and desired nutrition-tracking depth;
- exercises, movements, or circumstances the user wants to avoid; and
- optional baseline activity, height, weight, and measurements.

Provide a deterministic starter plan using local templates and transparent
rules. The user can edit every choice. AI may offer an alternative only after
the local version works.

### 6.2 Decisive Today screen

The main screen should answer "What should I do today?" and contain:

- today's planned workout and a one-tap start action;
- shorten, substitute, reschedule, and skip controls;
- calorie and protein progress at the user's selected tracking depth;
- steps when available;
- an optional recovery or basic habit check-in;
- one contextual reminder or encouragement; and
- a clear completion state when today's chosen commitments are done.

The user should not need to navigate through several feature areas during a
normal day.

### 6.3 Resilient weekly planning

Support:

- editable weekly availability;
- moving a planned workout to another day;
- preserving a missed workout for rescheduling instead of silently losing it;
- full, shortened, and minimum versions of a session;
- travel, illness, pause, and reduced-volume modes;
- recovery and mobility days; and
- a preview explaining how a proposed change affects the week.

Changing one week must not accidentally rewrite reusable workout templates or
historical sessions.

### 6.4 Fast workout execution

Improve the active workout flow with:

- previous values pre-filled as editable suggestions;
- one action to repeat the previous set;
- automatic rest timing after a completed set;
- clear exercise substitution;
- warm-up guidance when configured;
- session persistence across backgrounding or process recreation;
- transparent progression suggestions based on previous completed sessions;
- a minimum-session path for limited time; and
- a short optional post-workout energy and difficulty check-in.

Initial progression rules should be local and explainable. Example: suggest a
small load increase after the top of the target repetition range is completed
with acceptable difficulty in two consecutive sessions.

### 6.5 Sustainable nutrition

Offer selectable tracking depth rather than forcing detailed macros:

1. calories and macros;
2. calorie and protein focus;
3. meal-quality check-ins only; or
4. nutrition tracking disabled.

Practical shortcuts include:

- reusable meal templates;
- recent meals and foods;
- repeat meal or day;
- serving presets;
- target ranges rather than exact pass/fail thresholds; and
- clear independence between copied entries and their source templates.

Photo-based estimation, barcode catalogs, and remote food databases are not
part of this initiative's first delivery slices.

### 6.6 Weekly review and adaptation

Provide a review that can be completed in about two minutes:

- planned versus completed workouts;
- strength or repetition improvements;
- nutrition consistency at the chosen tracking depth;
- weight trend rather than a single reading;
- steps and optional recovery consistency;
- achievements and interruptions;
- one or two suggested changes for the coming week; and
- an explicit approve, edit, or dismiss decision.

The review must be generated locally first. An AI-enhanced explanation is an
optional request using the same local summary.

### 6.7 Motivation model

Avoid leaderboards, coins, shame, and fragile all-or-nothing streaks. Prefer:

- planned-versus-completed consistency;
- active weeks within a rolling window;
- personal records and repetition milestones;
- strength, weight, measurement, and photo trends;
- recognition for returning after an interruption;
- monthly recaps assembled on-device; and
- reminders that can be completed, shortened, snoozed, or rescheduled.

Motivational text should cite a real local event, such as completing three of
four planned sessions or returning after a week away.

### 6.8 Optional exercise catalogue and demonstrations

Keepfit should reduce first-run setup by offering useful exercises before the
user builds a personal library. The dependable path is an offline starter
catalogue that begins with 12-20 common exercises for the first quick-start
slice and grows to about 30-50 exercises. Metadata and every included media
asset must be owned by Keepfit or have clear redistribution rights. Prioritize
demonstrations for the most-used core exercises; an exercise may ship with
verified instructions before media rather than using an unlicensed placeholder.

An optional online catalogue may supplement that library. The initial
prototype candidate is AscendAPI ExerciseDB V1 because its hosted free endpoint
currently offers about 1,500 exercises, instructions, muscle and equipment
metadata, and 180p GIF demonstrations without signup or an API key.

The online experience should:

- let the user browse and search by exercise name, body area, muscle, and
  equipment;
- show instructions, attribution, and a looping demonstration when available;
- remain visibly optional and show a useful offline or unavailable state;
- preserve manual exercise creation and private media import;
- never send workout history, profile data, or other personal fitness records
  to the catalogue provider; and
- treat demonstrations as general form references, not medical,
  rehabilitation, or injury-specific guidance.

ExerciseDB is approved only as a prototype candidate. Its current caching
policy allows storage only when the active plan explicitly grants it, and
media URLs may rotate. Before a public release, obtain written confirmation of
commercial display, attribution, caching, and local-persistence rights. Until
those rights are confirmed, do not copy provider metadata or media into Room or
app-private storage, and do not enable an "Add to library" action that would
persist provider content.

Use Coil 3 with `coil-gif` to display prototype GIFs in Compose. Prefer short,
silent, looping MP4 or WebM files rendered with Android Media3 for media Keepfit
owns because they are generally more efficient than GIFs. Dependency versions
and provider terms must be rechecked when this work is sliced into a version.

## 7. Optional AI Coach

### 7.1 Role

The coach should operate over compact structured summaries and bounded tools.
It may:

- explain today's workout;
- draft a starter week from user-approved goals and constraints;
- suggest an exercise replacement;
- produce a shorter version of a workout;
- summarize the week;
- propose a schedule, volume, or progression adjustment;
- suggest meals from the user's existing foods and preferences; and
- answer general fitness questions with visible safety guidance.

It must not:

- write to Room or files without a separately validated and approved command;
- diagnose, treat, or make medication recommendations;
- generate extreme calorie deficits or unsafe progression;
- upload photos, identity fields, birth dates, or free-form private notes by
  default;
- run in the background merely because a screen opened; or
- block any offline workflow when unavailable.

### 7.2 Recommendation presentation

Every data-changing recommendation should show a diff-like proposal:

```text
Observed: Friday's workout was missed in three recent weeks.
Current: Friday, 60 minutes.
Proposed: Saturday, 40 minutes, same exercise order.
Reason: Saturday matches the availability recorded in weekly check-ins.

[Apply] [Edit] [Dismiss]
```

The application, not the model, validates identifiers, ranges, plan conflicts,
exercise references, and mutation order.

## 8. OpenRouter Access and Model Strategy

### 8.1 Decision history

The following options were considered during discovery:

| Option | Decision | Reason |
|---|---|---|
| Developer-funded shared model key | Reject | Creates abuse, credential-extraction, and uncontrolled-cost risk |
| User-pasted Gemini API key | Reject for the normal experience | A mobile app must access the secret, and asking users to paste a provider key creates an avoidable trust burden |
| Gemini OAuth or user-controlled proxy | Defer | More setup and infrastructure than the low-budget personal product currently justifies |
| OpenRouter OAuth with PKCE | Proceed to evaluation | User-visible authorization, user-controlled quota, revocation, and no manually copied key |
| `openrouter/free` random routing | Reject as the primary coach | Model behavior, structured output, safety, and provider privacy can vary between requests |
| Named OpenRouter free model | Proceed to evaluation | Predictable model behavior can be tested before considering paid inference |

### 8.2 Authentication decision

Use OpenRouter OAuth with PKCE rather than asking the user to paste an API key
or shipping a developer-funded inference key.

```text
Connect OpenRouter
  -> generate verifier and S256 challenge
  -> authorize in the system browser
  -> return to a one-time localhost receiver in the private build
  -> exchange the one-time code
  -> encrypt the user-controlled token locally
```

OpenRouter's current OAuth contract accepts HTTPS callbacks and localhost or
`127.0.0.1` callbacks, but not Android custom schemes. Slice G therefore uses
the documented localhost path for the backend-free private APK. An owned HTTPS
domain plus a verified Android App Link remains a public-release gate.

Credential requirements:

- use Android Keystore-backed encryption;
- never store the token in Room or ordinary DataStore;
- never log or display the complete token;
- exclude the token and OAuth verifier from backups;
- erase the token on disconnect;
- reject stale or mismatched OAuth state; and
- preserve all offline functionality when authentication fails or is revoked.

The user authorizes OpenRouter and consumes their own OpenRouter quota or later
credits. Keepfit does not pay for or centrally proxy requests in this design.

### 8.3 Free-model candidate

The initial evaluation candidate is:

`inclusionai/ling-3.0-flash-sante:free`

Selection rationale as checked again on 2026-09-13:

- health-oriented reasoning and safety focus;
- free prompt and completion pricing;
- tool and tool-choice support;
- 262K context; and
- strong then-current latency and endpoint availability.

This is a candidate, not a permanent dependency. It was released recently and
must pass Keepfit's own evaluation before beta use. Free endpoint availability,
limits, provider policy, and model quality may change without an app release.

Do not use `openrouter/free` as the normal production route. Random model
selection would make coaching behavior, validation reliability, and privacy
harder to evaluate. It may be used only in development experiments or as an
explicitly approved fallback.

### 8.4 Structured interaction

The selected free model accepts tools but does not enforce JSON through
`response_format`. Use required function/tool calls for structured actions and
validate their arguments locally.

Initial tool contracts should include:

- `propose_weekly_plan`;
- `propose_schedule_change`;
- `propose_workout_shortening`;
- `propose_exercise_substitution`;
- `summarize_week`; and
- `suggest_existing_food_meal`.

Unknown tools, unexpected fields, unresolved identifiers, excessive values,
and invalid enum values must fail closed and leave local data unchanged.

### 8.5 Free usage policy

OpenRouter currently documents 50 free-model requests per day for an account
that has not purchased credits, increasing to 1,000 free-model requests per
day after at least USD 10 of credit purchases. These provider limits are not a
product guarantee.

Keepfit should additionally enforce a conservative local policy:

- default maximum of 10 AI requests per local calendar day during beta;
- no automatic retry loops that can consume many requests;
- exponential backoff only for transient failures;
- preserve user text and drafts after quota or availability errors;
- cache completed summaries and proposals locally;
- show the model actually used and the time of generation; and
- query the current-key endpoint for available account status where supported.

The initial cap should be configurable in development but not presented as an
authoritative statement of the provider's remaining quota. Local counts can
drift if the same OpenRouter account is used elsewhere.

### 8.6 Privacy routing

For every request, require providers that do not collect data:

```json
{
  "provider": {
    "data_collection": "deny"
  }
}
```

If the requested model has no eligible endpoint under that policy, fail with a
clear privacy-preserving message. Do not silently relax the policy.

Before the first request, show exactly which categories will leave the device.
Default remote context is limited to aggregates and user-approved constraints,
for example:

- workout count and dates;
- exercise names and aggregate performance;
- approximate nutrition totals and goal ranges;
- weight trend if explicitly enabled;
- step aggregates if explicitly enabled; and
- the user's current question.

Never send transformation photos. Exclude display name, birth date, raw notes,
file paths, stable database identifiers, and backup contents.

### 8.7 Later low-cost model

Do not select a paid model merely because the free endpoint becomes
inconvenient. First validate that users repeatedly benefit from the workflow.

When the evaluation gate is met:

1. run the same evaluation set against current low-cost OpenRouter models;
2. compare task success, safety, latency, privacy-eligible availability, and
   cost per completed user workflow;
3. choose a named model rather than an unbounded paid router;
4. require explicit user confirmation before any paid request;
5. respect the user's OpenRouter key spending limit and show estimated cost;
6. keep free and AI-disabled modes available; and
7. make the model identifier replaceable without changing domain behavior.

## 9. Technical Approach

### 9.1 Preserve existing boundaries

Continue using:

- Compose screen -> ViewModel -> repository/adaptor flow;
- Room as the source of truth for fitness records;
- app-private media storage;
- DataStore for non-secret preferences;
- Android secure credential storage for the OpenRouter token; and
- `app` for coordination that crosses feature boundaries.

Do not add a backend, account database, telemetry SDK, or remote configuration
service for this initiative.

### 9.2 Exercise catalogue adapter

Keep the optional remote catalogue behind an `ExerciseCatalogProvider`
interface. `feature:workouts` consumes provider-independent catalogue models;
the remote implementation owns HTTP, response mapping, provider errors, and
attribution metadata. Fake and offline implementations must support tests and
the unavailable state.

Room remains authoritative only for Keepfit-owned exercise records. Remote
catalogue responses are display data until the selected provider grants the
necessary persistence rights. If imports are later permitted, the version spec
must define provenance fields, attribution retention, media ownership, backup
behavior, URL expiry, and deletion behavior before changing the schema.

Do not ship a shared provider API key in the Android package. The first
candidate is keyless. A future provider that requires credentials needs a
separately approved user-authorization or trusted-service design and must not
weaken the offline workflow.

### 9.3 Assistant refactor

Evolve the existing assistant implementation rather than adding a parallel UI:

- keep `AssistantRepository` as the behavior boundary;
- split provider-independent requests from provider credentials;
- add an `OpenRouterAssistantRepository`;
- add OAuth, credential, quota, and privacy-policy adapters;
- replace free-form plan parsing with bounded tool calls;
- retain fake repositories and deterministic fixtures;
- keep Ollama available only until OpenRouter parity and migration behavior are
  verified, then explicitly decide whether to remove it; and
- remove build-time provider secrets when they are no longer required.

### 9.4 Local domain additions

Exact entities require a version spec and migration design. Likely persisted
concepts are:

- journey goal and planning constraints;
- per-week availability and temporary schedule exceptions;
- optional daily recovery or habit check-ins;
- post-workout energy and difficulty;
- locally generated weekly reviews;
- suggestion state and approval outcome; and
- local AI request count and last-reset metadata.

Prefer extending existing entities when their meaning remains clear. Any Room
change must increment the schema version, export its schema, add migration
tests, and update the data-model reference.

## 10. Phase-wise Development Plan

This plan orders delivery by user value and risk. A phase is a group of one or
more sprint-sized slices; it is not an application version. Version numbers
remain unassigned until the next slice is promoted into an approved version
specification. Each slice should fit roughly 8-12 engineering days and leave
the application runnable with a complete working increment.

Estimated effort is for engineering work, not elapsed calendar time. It
excludes waiting for exercise-content creation, licensing confirmation, store
review, or access to physical test devices.

| Phase | Delivery slices | User-visible outcome | Estimated effort | Exit gate |
|---|---|---|---|---|
| 1. Quick start and Today | A-B | A new user can create an editable offline week and act on a clear, flexible Today plan | 16-24d | Setup-to-start flow works offline without AI or a remote catalogue |
| 2. Faster execution and feedback | C-D | Workouts take less effort to log, survive interruption, and lead to a useful weekly review | 16-24d | Session recovery, local progression, and weekly adjustment are verified |
| 3. Sustainable nutrition | E | The user can choose an appropriate tracking depth and use nutrition shortcuts from Today | 8-12d | All supported tracking depths remain understandable and independent |
| 4. Exercise guidance and discovery | F | The offline catalogue is useful and an optional online browser can show attributed animated demonstrations | 8-12d plus content preparation | Offline catalogue, provider isolation, licensing gate, and outage behavior pass |
| 5. Optional AI coach | G-H | The user can authorize OpenRouter and request safe, reviewable coaching drafts using their own quota | 16-24d | Credentials, quota, privacy, tool validation, and offline fallback pass |
| 6. Hardening and private beta | I | The complete journey is reliable, accessible, recoverable, and ready for a measured beta | 8-12d | Release checklist and evaluation gates pass without critical issues |

The total indicative engineering effort is 72-108 days. The plan deliberately
puts the useful offline journey first so Phases 1-4 can stand on their own if
AI work is delayed or stopped.

Recommended product checkpoints:

- after Phase 1, begin daily internal use of planning and Today;
- after Phase 2, evaluate whether the core journey is practical enough to keep
  using before adding more breadth;
- after Phase 4, consider a non-AI release candidate, excluding the remote
  provider if its rights are unresolved; and
- after Phase 6, consider an opt-in AI beta and only then investigate paid
  inference.

### 10.1 Phase 1 - Quick start and Today

#### Slice A - Goal setup and deterministic starter week

**Delivered increment**

A new user records the few constraints that affect a plan and receives an
editable weekly workout proposal without using AI or creating every exercise
manually.

**Scope**

- Add goal, experience, available-days, session-duration, equipment, and
  avoid-list inputs with explicit defaults.
- Add the minimum owned or clearly licensed offline exercise set needed for
  home, bodyweight, dumbbell, and gym starter plans.
- Add deterministic plan-selection rules and explain why each template was
  selected.
- Preview the proposed week before saving; allow day, template, and exercise
  edits.
- Preserve existing profiles, exercises, templates, schedules, and history.
- Include new structured data in backup and restore behavior.

**Verification focus**

- Unit tests for every goal/equipment/availability rule and contradictory input.
- Room DAO and migration tests for any persisted journey fields.
- Compose tests from setup through preview, edit, approve, and reopen.
- Backup round-trip coverage for any new persisted records.

**Exit gate**

A fresh offline installation can reach a useful, editable week, while an
existing user can adopt the flow without losing or replacing current data.

#### Slice B - Decisive Today and resilient planning

**Delivered increment**

Today shows one primary action and lets the user shorten, substitute,
reschedule, skip, or complete it without corrupting reusable templates or
history.

**Scope**

- Compose a single Today state from the active week, workout state, nutrition
  summary, steps availability, and local reminders.
- Add explicit full, shortened, minimum, rescheduled, substituted, skipped, and
  completed states.
- Store week-specific changes separately from reusable workout templates.
- Explain the effect of a proposed change before it is applied.
- Preserve an unfinished or missed workout as a recoverable choice.

**Verification focus**

- Repository tests for each transition and for template/history preservation.
- Compose tests for every Today state, including empty and offline states.
- Process-recreation tests around a pending Today action where practical.

**Exit gate**

The user can understand and adapt today's plan from one screen, and every
action produces a deterministic, reversible, or clearly confirmed result.

### 10.2 Phase 2 - Faster execution and feedback

#### Slice C - Low-friction workout execution

**Delivered increment**

An active workout is faster to record, survives interruption, and offers
transparent local progression without AI.

**Scope**

- Prefill previous values as editable suggestions and add repeat-previous-set.
- Start the configured rest timer when a set is completed.
- Support substitution and minimum-session variants inside the active workout.
- Restore the active session after backgrounding and process recreation.
- Add optional energy and difficulty feedback after completion.
- Generate bounded progression suggestions from completed history and explain
  the rule used.

**Verification focus**

- Unit tests for progression, minimum-session selection, and previous-set rules.
- Repository tests for partial sessions, substitutions, and completion.
- Compose and process-recreation tests for the active workout path.

**Exit gate**

A user can complete and recover a realistic session without duplicate sets,
lost progress, or unexplained progression changes.

#### Slice D - Weekly review and flexible motivation

**Delivered increment**

The user completes a local two-minute review and approves, edits, or dismisses
one understandable adjustment for the following week.

**Scope**

- Calculate planned-versus-completed workouts, performance improvements,
  interruptions, returns, and available nutrition/step trends locally.
- Present achievements without all-or-nothing streaks or judgmental language.
- Propose no more than two bounded schedule, volume, or recovery changes.
- Keep suggestions as drafts until explicit approval.
- Persist review and approval outcomes only where they improve future local
  recommendations.

**Verification focus**

- Unit tests for trend windows, interruptions, return recognition, and proposal
  selection.
- Repository tests proving previews do not mutate plans.
- Compose tests for approve, edit, dismiss, insufficient-data, and paused states.

**Exit gate**

Weekly review and motivation work entirely offline and never require the AI
coach to produce or apply an adjustment.

### 10.3 Phase 3 - Sustainable nutrition

#### Slice E - Tracking depth and practical shortcuts

**Delivered increment**

The user chooses detailed macros, calorie-and-protein focus, meal-quality
check-ins, or disabled nutrition and sees only the relevant Today experience.

**Scope**

- Add tracking-depth preference and target ranges.
- Reuse existing foods, saved meals, favorites, recents, and duplicate-day
  behavior through faster entry paths.
- Add serving presets and repeat-meal actions without linking copied entries to
  later template edits.
- Integrate the selected depth into Today and weekly review.
- Preserve detailed nutrition records when the user temporarily chooses a
  simpler mode or disables the feature.

**Verification focus**

- Unit tests for ranges, copied-entry independence, and depth-specific summaries.
- Compose tests for each depth and switching between them.
- Migration and backup tests for any new preference or check-in data.

**Exit gate**

Changing tracking depth never deletes existing nutrition history, and the
quickest supported mode can be logged in a few deliberate actions.

### 10.4 Phase 4 - Exercise guidance and discovery

#### Slice F - Offline catalogue and online prototype

**Delivered increment**

The local exercise library is useful on first launch, and the user may open an
optional online browser to search exercises and view attributed animated
demonstrations.

**Scope**

- Grow the offline catalogue toward 30-50 verified exercises and maintain an
  asset-rights register; prioritize demonstration media for common movements.
- Add `ExerciseCatalogProvider`, provider-independent models, fake provider,
  network adapter, and explicit unavailable states.
- Prototype keyless AscendAPI ExerciseDB V1 search without sending personal
  fitness data.
- Render remote GIFs with Coil and owned video media with Media3.
- Show attribution, loading, missing-media, timeout, offline, and provider-error
  states.
- Keep remote content transient until commercial and persistence rights are
  confirmed in writing.

**Verification focus**

- Contract tests for mapping, malformed data, attribution, URL rotation, and
  failures.
- Compose tests with fake responses; no live network in automated tests.
- Manual verification on constrained bandwidth and Android 12 plus a current
  Android version.
- Release review proving provider content is not persisted or backed up without
  permission.

**Exit gate**

The offline library remains complete enough for normal planning when the
provider is disabled. The prototype cannot reach a public build until provider
rights are documented and the selected caching behavior matches those rights.

### 10.5 Phase 5 - Optional AI coach

#### Slice G - OpenRouter authorization, privacy, and quota

**Delivered increment**

The user can connect, test, inspect, and disconnect their own OpenRouter access
without pasting a key or weakening any offline workflow.

**Scope**

- Implement OAuth PKCE through the system browser and OpenRouter's supported
  localhost callback for the private APK; require a verified Android App Link
  before public distribution.
- Store the resulting credential with Android Keystore-backed encryption and
  exclude it from Room, logs, DataStore, and backups.
- Add connect, connecting, connected, revoked, invalid, quota-exhausted,
  provider-unavailable, and disconnected states.
- Enforce the local beta limit, defaulting to 10 user-initiated requests per
  local day, with no background calls or retry loops.
- Require privacy-preserving provider routing and disclose every outgoing data
  category before first use.
- Keep Ollama only as a temporary compatibility path until OpenRouter parity is
  demonstrated.

**Verification focus**

- OAuth state, PKCE, loopback callback, disconnect, and credential-storage tests.
- Tests proving credentials are absent from backups and ordinary persistence.
- Recorded error fixtures for authentication, quota, timeout, and provider
  failures.
- Manual revoke and reconnect verification.

**Exit gate**

Authorization and every failure state preserve user work and offline features;
no shared developer credential exists in the application package.

**Completion:** Delivered in
[v0.8.0 - Private OpenRouter Access](../../done/versions/v0.8.0.md).

#### Slice H - Bounded coaching workflows

**Delivered increment**

The optional coach can explain Today and return validated weekly-review,
schedule, shortening, substitution, and existing-food meal drafts that the user
must review before applying.

**Scope**

- Separate provider-independent coaching requests from provider credentials.
- Use required tool calls for structured proposals and validate every field,
  identifier, range, enum, and mutation locally.
- Build compact, user-approved summaries; never send photos, identity fields,
  raw notes, paths, or database identifiers.
- Show observed evidence, current state, proposed state, and reason.
- Preserve prompts and completed proposals locally across quota or connectivity
  failures.
- Refuse medical, rehabilitation, medication, extreme-deficit, and unsafe
  progression requests with appropriate guidance.

**Verification focus**

- At least 50 stable evaluation cases from Section 11.3.
- Parser and validator tests for malformed, excessive, unknown, and malicious
  tool arguments.
- Compose tests for preview, edit, approve, dismiss, quota, and offline states.
- Repository tests proving no proposal writes before approval.

**Exit gate**

The deterministic safety, validation, privacy, and approval gates pass, and
turning the coach off leaves the complete deterministic experience intact. The
feature remains pre-beta until the named free model also passes the recorded
live quality thresholds.

**Completion:** Delivered as a pre-beta implementation in
[v0.9.0 - Bounded Coaching Workflows](../../done/versions/v0.9.0.md).

### 10.6 Phase 6 - Journey and interface reorganization

#### Slice I - Guided onboarding and practical daily hierarchy

**Delivered increment**

A new user moves from relevant plan questions to a reviewed starter week in one
guided flow, while a returning user sees one compact next action and reaches
secondary features through a predictable hierarchy.

**Scope**

- Replace name-and-height-only registration with short, skippable steps for
  goal, experience, recent activity, schedule, duration, preferred time,
  equipment, movement preferences, nutrition depth, and optional body context.
- Explain why optional sensitive inputs are useful and keep them unnecessary
  for workout planning.
- Preview the deterministic starter week before completing onboarding.
- Reorganize primary navigation around Today, Plan, Log, Progress, and a
  query-only Coach; move Settings to a profile-level destination.
- Keep one dominant action above the fold and place adaptations behind a single
  Adjust entry point.
- Separate frequent daily actions from exercise, food, template, integration,
  backup, and other administration.
- Establish a restrained training-notebook visual system with compact rows,
  meaningful cards, intentional type scale, and less unused vertical space.

**Verification focus**

- First-run and existing-user migration tests with no data loss.
- Task-based Compose tests for create plan, start today, adjust today, log meal,
  and review progress with bounded interaction counts.
- Android 12/current-version checks for large font, TalkBack, keyboard, back,
  scroll position, and one-handed primary actions.
- Reachability map proving every existing feature remains accessible.

**Exit gate**

A new user obtains a useful plan without discovering separate setup screens,
and the five most common returning-user tasks each begin from a clear primary
destination without a wall of equally weighted cards.

The supporting research is recorded in the
[Fitness App UX Benchmark](../../references/fitness-app-ux-benchmark.md).

**Execution:** In progress as
[v0.10.0 - Guided Journey and Interface Reorganization](../../versions/v0.10.0.md).

### 10.7 Phase 7 - Hardening and private beta

#### Slice J - Reliability, accessibility, and readiness decision

**Delivered increment**

The end-to-end journey is tested on supported Android versions and can enter a
small opt-in beta with explicit decisions about the exercise provider, Ollama,
and any future low-cost model.

**Scope**

- Run end-to-end setup, Today, workout, nutrition, review, catalogue, assistant,
  backup, restore, and process-death scenarios.
- Resolve accessibility, large-font, keyboard, contrast, loading, and empty-state
  issues.
- Measure assistant task success, tool validity, safety, latency, availability,
  and request cost using the fixed evaluation set.
- Review exercise-provider licensing, attribution, caching, reliability, and
  production eligibility.
- Decide whether Ollama remains, whether the online catalogue can ship, and
  whether testing a named low-cost paid model is justified.
- Update README, architecture, data model, privacy disclosure, and user manual
  to match only what actually ships.

**Verification focus**

- Full unit, lint, build, Room migration, backup, and connected UI suites.
- Manual Android 12 and current-version device or emulator matrix.
- Credential, privacy, and archive inspection.
- A written beta checklist with no unresolved critical or high-severity issue.

**Exit gate**

Every shipped integration has an explicit go, limited-beta, or no-go decision,
and core planning, logging, review, backup, and restore remain reliable offline.

### 10.8 Dependency and sequencing rules

```text
Slice A -> Slice B -> Slice C -> Slice D -> Slice E
    |                                  |
    +---------------> Slice F          +-> Slice H
                                           ^
Slice G -----------------------------------+

Slices A-H -> Slice I -> Slice J
```

- Implement A-E in order because each depends on stable domain behavior from
  the previous slice.
- Exercise-content research and rights review may run alongside A-E, but Slice
  F implementation starts only when the offline asset set and provider rules
  are sufficiently clear.
- Slice G may be developed independently after the core Today workflow is
  stable; Slice H requires both local weekly summaries from D and secure access
  from G.
- Do not start I until Slice H is complete or explicitly descoped. Do not start
  J until the reorganized journey is stable.
- Promote only the next slice to `docs/versions/`; create its execution log when
  implementation starts and its done record only after verification completes.
- Never modify a frozen version specification to absorb later-phase work.

### 10.9 Current delivery state

Slice A was completed as v0.2.0. Its frozen version specification includes
migration, backup, deterministic-rule, Compose-flow, and existing-user
preservation tests. Online catalogue and AI work are explicitly excluded from
this first slice.

The working starter set is deliberately small and balanced across bodyweight,
dumbbell, and common gym access:

| Movement area | Initial exercises |
|---|---|
| Squat and legs | Chair squat, bodyweight squat, reverse lunge, goblet squat, leg press |
| Hinge and posterior chain | Glute bridge, dumbbell Romanian deadlift |
| Push | Wall or incline push-up, dumbbell bench press, dumbbell overhead press |
| Pull and posture | One-arm dumbbell row, resistance-band row, lat pulldown, prone W raise |
| Core and stability | Dead bug, bird dog |

The v0.2.0 set uses original text instructions and no bundled media. More
detailed progressions, contraindication wording, and any future media still
require a focused content and rights review.

Slice B was completed as v0.3.0. Today now presents one primary workout action,
reviewable day-specific adaptations, and seven-day missed-workout recovery.
Dated occurrence snapshots remain separate from recurring plans and reusable
templates.

Slice C was completed as v0.4.0. Active workouts now provide prior-value reuse,
automatic rest timing, safe session-only adaptations, optional completion
feedback, and bounded local progression.

Slice D was completed as v0.5.0. The most recently completed ISO week now
produces a deterministic offline review with evidence-based encouragement and
at most two editable proposals. Preview is read-only, and approval creates one
dated occurrence for the coming week without rewriting the recurring plan.
Slice E was completed as v0.6.0. Nutrition now supports detailed macros,
calorie-and-protein focus, meal-quality check-ins, and a history-safe disabled
mode. Target ranges, serving presets, scoped previous-meal repeat, and
mode-specific Today and review signals keep the workflow useful without
forcing precision. Slice F was completed as v0.7.0. The exercise browser now
separates an editable private library, 40 original offline guides, and a
transient view-only ExerciseDB prototype. Remote results and media are not
persisted or backed up, and public use remains gated on written provider
rights.

Slice G was completed as v0.8.0. OpenRouter access now uses user-authorized
OAuth PKCE, Keystore-encrypted credentials, privacy-preserving routing, and a
visible local ten-request daily allowance without a packaged provider key.

## 11. Evaluation and Testing

### 11.1 Product acceptance

- A new user can reach a useful starter week without creating exercises or
  foods manually first.
- A returning user can identify and begin today's primary action in one screen.
- A missed workout can be rescheduled or shortened without damaging templates
  or history.
- A weekly review is understandable without AI.
- Motivation recognizes consistency and returning after breaks.
- Disabling AI leaves every planning, logging, and review workflow operational.

### 11.2 Automated tests

- Unit tests for starter-plan rules, progression, rescheduling, minimum
  sessions, trend calculations, and motivation selection.
- Repository tests proving history and templates are not changed by previews.
- Room DAO and migration instrumentation tests for every schema change.
- Compose tests for onboarding, Today, rescheduling, shortened workouts,
  weekly review, exercise catalogue states, OAuth states, draft review, and
  failure recovery.
- Contract tests for exercise search, missing media, malformed provider data,
  attribution, rotating media URLs, timeouts, and offline behavior.
- Contract tests using recorded OpenRouter response/error fixtures without
  network access.
- Parser and validator tests for missing, unknown, excessive, and malicious
  tool arguments.
- Credential tests proving secrets are absent from Room, DataStore, logs, and
  backup archives.

### 11.3 AI evaluation set

Create at least 50 stable cases covering:

- beginner, experienced, limited-equipment, short-session, and travel plans;
- under-recovery, repeated missed days, and inconsistent nutrition;
- exercise substitution and minimum-workout requests;
- contradictory or incomplete user constraints;
- extreme dieting, unsafe progression, injury, diagnosis, and medication
  requests;
- prompt injection inside user notes or exercise names;
- malformed tool calls and unresolved identifiers; and
- quota, authentication, timeout, provider-unavailable, and privacy-routing
  failures.

For every candidate model, record:

- valid-tool-call rate;
- task completion rate;
- safety-policy pass rate;
- median and high-percentile latency;
- privacy-eligible endpoint availability;
- average requests and tokens per completed workflow; and
- estimated paid cost for the same workload.

No model proceeds to beta because of general benchmark rankings alone.

### 11.4 Manual verification

- Android 12 and a current Android emulator.
- Process death during an active workout and OAuth flow.
- Offline launch and all non-AI core workflows.
- Exercise catalogue search, animation playback, attribution, provider outage,
  and confirmation that no personal fitness data is transmitted.
- OpenRouter connect, revoke, reconnect, and disconnect.
- Daily limit, transient rate limit, invalid token, and provider outage.
- Screen-reader labels, large font, keyboard behavior, and color contrast.
- Backup export/restore with confirmation that no assistant credential is
  present.

## 12. Rollout

1. **Internal fixtures:** develop the experience against fake assistant and
   deterministic local data.
2. **Developer trial:** use a separate OpenRouter account and sanitized test
   data.
3. **Private beta:** opt-in users authorize their own OpenRouter accounts; cap
   calls and collect feedback manually rather than adding telemetry.
4. **Quality gate:** require acceptable evaluation, privacy, and reliability
   results before recommending AI in onboarding.
5. **Cost gate:** only then evaluate a named low-cost paid model and require
   explicit user opt-in to paid inference.

## 13. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Free model disappears or becomes unreliable | Named model adapter, connection health, clear unavailable state, replaceable model ID, offline workflow |
| New health-focused model is overconfident | Fixed safety evaluation, bounded tools, local validation, general-guidance language |
| OAuth token is extracted from a compromised device | PKCE, Keystore-backed encryption, no backup/logging, revocation and disconnect |
| Provider retains fitness context | `data_collection: deny`, explicit disclosure, aggregate-only payload, fail closed |
| AI changes a plan incorrectly | Draft-only tools, diff preview, deterministic validation, explicit approval |
| Quota is consumed by background behavior | User-initiated requests, caching, conservative daily cap, no automatic retry loops |
| Exercise catalogue terms or media URLs change | Optional adapter, no permanent provider storage without rights, owned offline starter catalogue, recheck before release |
| Remote demonstrations are inaccurate or unavailable | Curate the owned starter set, show instructions and attribution, allow private media replacement, provide clear empty states |
| The app becomes too complex | Today-first navigation, selectable tracking depth, progressive disclosure, usability tests |
| Motivation becomes judgmental | Flexible consistency language, recognize recovery and returns, allow pause modes |
| Paid usage surprises a user | No automatic free-to-paid fallback, estimated cost, OpenRouter spending limit, explicit confirmation |

## 14. Non-Scope

- Social feeds, public profiles, leaderboards, or competitive challenges.
- A Keepfit-hosted inference proxy or developer-funded shared API key.
- Medical diagnosis, treatment, rehabilitation, or medication guidance.
- Silent or autonomous changes to fitness records.
- Automatic upload of transformation photos or raw private notes.
- Cloud synchronization or a Keepfit account system.
- Telemetry or remote behavioral analytics.
- Selecting or committing to a paid model before evaluation.

## 15. Initiative Completion Criteria

- [x] A user can set constraints and obtain an editable, useful starter week.
- [x] Today connects planning, execution, and completion in one primary flow.
- [x] Missed or constrained days have forgiving recovery paths.
- [x] Workout and nutrition logging have sustainable shortcuts.
- [x] Weekly review and motivation work fully offline.
- [x] A useful owned or clearly licensed starter exercise catalogue works
  offline.
- [ ] The optional online catalogue has documented commercial, attribution,
  caching, and persistence rights before public release.
- [x] Catalogue failure never prevents exercise creation, planning, or workout
  logging.
- [x] OpenRouter connection uses OAuth PKCE with secure local credential storage.
- [ ] The free-model candidate passes the agreed evaluation thresholds.
- [x] AI drafts are structured, validated, reviewable, and never direct writes.
- [x] Privacy and quota failures preserve all local workflows and user input.
- [ ] The paid-model decision is based on measured workflow quality and cost.
- [x] Architecture, data-model, README, user-manual, and privacy documentation
  reflect the delivered behavior.

## 16. Working Decisions and Remaining Gates

The initiative direction and phase order are approved. These planning defaults
allow early phases to proceed without silently deciding later integration
questions.

| Decision | Working default | Required by |
|---|---|---|
| Initial goals | General fitness, consistency, strength, muscle gain, and fat loss | Slice A version approval |
| Starter exercise content | Confirm 12-20 essential exercises for Slice A; grow toward 30-50 in Slice F | Slice A version approval |
| Recovery check-in | Energy and difficulty only; sleep, soreness, and stress remain later options | Slice C version approval |
| Nutrition depths | Detailed macros, calorie/protein focus, meal-quality check-in, and disabled | Slice E version approval |
| ExerciseDB V1 | Approved for a non-production prototype only; public use and persistence require written rights | Slice F release gate |
| AI daily cap | Removed in v0.10; OpenRouter and the selected provider own account limits | Slice I product-owner decision |
| Weight in AI context | Excluded by default; offer a separate explicit opt-in | Slice G version approval |
| Ollama | Retain during OpenRouter migration; final keep/remove decision occurs in hardening | Slice J |
| AI quality thresholds | 100% fixture validation/privacy/safety, at least 90% valid tool calls, and at least 85% task completion | Slice H version approval |
| Information architecture | Guided first run; Today, Plan, Log, Progress, Coach; Settings outside daily navigation | Slice I version approval |
| Paid AI model | No selection until free-model workflow quality and measured demand justify evaluation | After Slice J |

## 17. External References

Exercise-provider and OpenRouter information is volatile. Recheck the exercise
catalogue sources when Slice F is versioned and OpenRouter sources when Slice G
is versioned.

- [OpenRouter OAuth PKCE](https://openrouter.ai/docs/guides/overview/auth/oauth)
- [OpenRouter free-model limits](https://openrouter.ai/docs/faq)
- [Ling 3.0 Flash Sante free model](https://openrouter.ai/inclusionai/ling-3.0-flash-sante:free)
- [Free-model router behavior](https://openrouter.ai/docs/guides/routing/routers/free-router)
- [Provider data-collection filtering](https://openrouter.ai/docs/guides/routing/provider-selection)
- [OpenRouter data collection](https://openrouter.ai/docs/guides/privacy/data-collection)
- [Current-key usage endpoint](https://openrouter.ai/docs/api/api-reference/api-keys/get-current-key)
- [AscendAPI ExerciseDB V1](https://docs.ascendapi.com/products/edb-v1/overview)
- [AscendAPI caching policy](https://docs.ascendapi.com/guides/caching)
- [Coil GIF support](https://coil-kt.github.io/coil/gifs/)
- [Android Media3 playback](https://developer.android.com/media/media3/exoplayer/hello-world)
- [Fitness app UX benchmark](../../references/fitness-app-ux-benchmark.md)
