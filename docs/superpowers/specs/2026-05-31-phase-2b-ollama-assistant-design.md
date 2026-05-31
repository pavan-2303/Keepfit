# Keepfit Phase 2B Ollama Assistant Design

## Scope

Phase 2B adds optional, private AI assistance on top of the completed offline
tracker. The assistant must remain non-authoritative: it may summarize,
suggest, and draft, but it must not mutate plans or core data without explicit
user review and confirmation.

This phase is intentionally decomposed into smaller working slices:

1. assistant foundation, settings, and secure credentials;
2. live chat against Ollama with a fake adapter for tests;
3. local fitness summary assembly for explicit assistant requests;
4. reviewable workout-plan draft generation and apply flow;
5. reliability, UX polish, and device verification.

## Current Codebase Fit

- `app` owns cross-feature navigation and dependency wiring.
- `core:preferences` already stores simple app settings in DataStore and is the
  right place for assistant toggles, endpoint, and model name.
- `feature:settings` already hosts optional integrations and backup/reminder
  controls, so assistant configuration belongs there first.
- `feature:workouts`, `feature:nutrition`, and `feature:transformation`
  already expose read models through repositories and `Flow`. The assistant
  should read these via focused summary adapters rather than by reaching into
  feature UI code.
- No `feature:assistant` module exists yet, so Phase 2B should introduce it as
  a real module, not a temporary app-level implementation.

## External API Constraints

Based on the current official Ollama docs:

- direct cloud access is available at `https://ollama.com/api`;
- chat requests should use `POST /api/chat`;
- direct cloud access requires API-key authentication.

That means Keepfit should target:

- direct Ollama Cloud API usage with a stored API key outside DataStore;
- direct cloud model names such as `gpt-oss:120b` rather than local bridge
  names ending in `-cloud`.

## Decisions

### 1. Introduce `feature:assistant`

Create `feature:assistant` as the Phase 2B boundary for:

- assistant settings and connection status;
- chat UI and chat state;
- local summary prompt assembly;
- draft plan generation and review state;
- fake and real Ollama adapters.

`feature:assistant` must not depend directly on other `feature:*` modules. Any
shared assistant-facing read models should live in a focused `core:*` surface
or be exposed through app-wired repository interfaces.

### 2. Keep secrets out of DataStore

Store these in `core:preferences`:

- assistant enabled toggle;
- Ollama Cloud API URL;
- default model name.

Store the API key in Android secure credential storage behind a small
interface, so tests can use an in-memory fake without persisting secrets in
DataStore.

### 3. Add the assistant entry to Settings first

The first user-visible entry point should be an assistant card in Settings,
because it keeps the optional integration clearly separated from the offline
core and allows configuration before chat usage.

After configuration is in place, expose an assistant surface from Settings via
its own route. This avoids adding a sixth bottom-navigation destination during
the initial rollout.

### 4. Use explicit request boundaries

The assistant should not continuously watch or push data. All summary assembly
must happen only when the user explicitly requests an assistant action, such as:

- open chat and ask a question;
- generate a weekly progress summary;
- request a draft workout plan.

This preserves the app’s local-first privacy model and keeps AI costs and
latency user-controlled.

### 5. Build summaries locally before calling Ollama

Keepfit should assemble compact local summaries from existing repository data
before invoking Ollama. The model receives:

- recent workout history and records;
- current plan shape;
- recent nutrition totals and goal adherence;
- recent weight and weekly progress data;
- optional steps only when available.

This keeps the prompt deterministic, avoids granting the model raw database
access, and makes the summary assembler testable without a network.

### 6. Keep plan drafts reviewable and separate from application

Assistant-generated plan suggestions should produce a draft structure that is
rendered in-app for review. The user must explicitly choose to apply it, and
the apply action should go through the existing workout repository APIs.

The assistant never writes directly to Room.

## Proposed Delivery Slices

### Slice 1: Foundation and Settings

- Add `feature:assistant`.
- Add assistant settings models and persistence.
- Add secure token storage abstraction.
- Add Settings UI for enable toggle, endpoint, model, token entry, and
  connection test.
- Add a fake Ollama adapter plus a real HTTP adapter interface.

Outcome: the app can be configured for Ollama safely, and tests can run without
network access.

### Slice 2: Chat

- Add assistant chat screen and route.
- Add conversation state with in-memory history first.
- Wire fake adapter for tests and real `/api/chat` adapter for runtime.
- Handle unreachable endpoint, invalid URL, timeout, and non-2xx responses
  without losing the draft user message.

Outcome: the app can send and receive assistant messages.

### Slice 3: Local Summaries

- Add a summary assembler that reads existing repositories and produces compact
  prompt sections.
- Add explicit actions for progress summary and recent context insertion.
- Keep prompt-building deterministic and unit tested.

Outcome: the assistant answers with user-specific context rather than generic
fitness advice.

### Slice 4: Draft Weekly Plan Proposals

- Define a structured assistant response shape for draft plans.
- Ask Ollama for JSON output for draft plan proposals.
- Render draft workouts in a review screen.
- Apply approved drafts through existing workout-plan write paths only.

Outcome: the assistant can help create a plan without silently mutating data.

### Slice 5: Hardening and Verification

- Add Compose UI tests for settings, chat error retention, and draft review.
- Add fake-adapter tests and prompt-assembly tests.
- Run app build, unit tests, lint, and manual device verification.
- Update README and architecture docs to reflect the assistant workflow.

Outcome: Phase 2B is releasable and aligned with roadmap exit criteria.

## Error Handling

- Disabled assistant: hide chat actions and show a clear optional-integration
  empty state.
- Invalid endpoint: block connection test and send with a validation message.
- Unreachable Ollama: keep the user’s typed prompt intact and show a failure
  message.
- Empty model name: block save/send.
- Token missing for remote/cloud endpoint: allow save, but fail connection test
  with a clear auth message.
- Invalid assistant JSON for plan drafts: show a recoverable failure and do not
  apply any plan changes.

## Testing Strategy

- Unit tests for assistant settings validation, credential storage abstraction,
  chat-state mapping, prompt assembly, and structured draft-plan parsing.
- Fake Ollama adapter for all non-network tests.
- Compose UI tests for assistant settings, chat send/retry behavior, and draft
  review/apply flow.
- Broader app build verification after each slice.

## Recommendation

Implement Phase 2B as the five slices above, in order. This is the lowest-risk
path to a working end product because it establishes privacy-safe settings and
test seams before introducing live chat or plan generation, while still
delivering a usable assistant early in the phase.

## References

- Ollama API introduction: https://docs.ollama.com/api
- Ollama chat endpoint: https://docs.ollama.com/api/chat
- Ollama authentication: https://docs.ollama.com/api/authentication
