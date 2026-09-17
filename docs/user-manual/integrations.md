# Optional Integrations

## Health Connect Steps

Health Connect step tracking is optional.

When available, Keepfit can show:

- today's step total;
- seven-day total;
- seven-day average.

### Enable Steps

1. Install or update Health Connect if your device requires it.
2. Open Keepfit.
3. Go to `Today`.
4. Use the steps card to grant read-only step permission.

### Notes

- Keepfit reads only the aggregate date ranges needed by Today or an opened
  weekly review and does not copy step history into Room.
- If Health Connect is unsupported, the rest of the app still works normally.
- If permission is denied or revoked, the card shows an unavailable state.

## OpenRouter Coach

The assistant is optional. It contains no Keepfit-funded or build-time API key.
You authorize your own OpenRouter account in the system browser, and requests
use your account's free-model quota or credits.

Current capabilities:

- connect, inspect, and disconnect your OpenRouter access;
- choose Mira for a warm style, Rook for a direct style, or Atlas for an
  analytical style;
- keep separate profile-owned conversations across app restarts;
- switch, rename, clear future Coach memory, or delete a conversation;
- ask ordinary training and nutrition questions;
- ask about recent personal progress and receive insights from compact local
  workout, nutrition, step, and transformation summaries;
- create a workout-plan draft from saved journey answers using only exercises
  in your personal catalogue;
- review the current week, propose a week from existing templates, or suggest
  a meal from saved foods; and
- retry a failed request without changing local records.

### Connect the Assistant

1. Open the `Coach` tab.
2. Read the outbound-data disclosure and choose `Connect OpenRouter`.
3. Approve access in the system browser and return to Keepfit.
4. Use the connection menu if you want to check or disconnect access.

On supported devices, open `Settings` > `Connections` and enable `Recover
access after reinstall` if you want Google Block Store to retain this token
separately. Keepfit validates recovered access before using it. Turning the
switch off or disconnecting requests deletion; when the service is unavailable,
reconnect through Coach instead.

### Use the Assistant

Inside Coach you can:

1. Choose a Coach when starting a conversation. This changes communication
   style, not safety or privacy boundaries. Choosing a Coach alone does not
   create a history entry; the conversation is saved after you send the first
   question.
2. Type a general question, or choose a starter question.
3. Ask about "my progress", "my recent workouts", or similar personal history
   when you want Keepfit to include a compact local snapshot.
4. Look for the context notice below the response to confirm local activity was
   included.
5. Open conversation history to switch threads. Use a thread's options to
   rename it, clear future Coach memory while retaining the transcript, or
   permanently delete it.
6. Use `Create plan` for a new catalogue-backed week. Inspect the vertical
   weekday preview and choose `Apply plan` only when every target is correct.
7. Use `Review` for a weekly summary, an existing-template week, or a saved-food
   meal proposal. These remain previews until you explicitly apply them.

Coach never writes as part of an ordinary answer. Plan and review actions use
strict structured contracts, show the full proposal first, and require a
separate approval. Dismissal, invalid output, and failed storage leave the
existing records unchanged.

### Privacy and Usage Limits

- The private beta uses `inclusionai/ling-3.0-flash-sante:free`; it never falls
  back automatically to a paid model.
- Keepfit does not enforce a daily request allowance. OpenRouter and the
  selected provider enforce the user's free-tier, rate, and credit limits.
- Every model request asks OpenRouter to deny provider data collection and use
  only zero-data-retention endpoints. If none is eligible, the request fails
  without relaxing the privacy rule.
- Chat sends the conversation you type. Questions about your own progress may
  also send compact workout, nutrition, step, and transformation aggregates.
- Keepfit stores the full transcript locally under the active profile. A model
  request sends only a capped recap of earlier content plus the latest 12
  messages. Clearing Coach memory excludes earlier messages from later
  requests without deleting the local transcript.
- Weight, BMI, height, transformation photos, body measurements, profile and
  personal-record identifiers, private paths, and raw database rows are
  excluded. Plan creation sends at most 60 short-lived exercise aliases and
  labels so Keepfit can reject invented exercises without exposing Room IDs.
- OpenRouter records request metadata such as model, token counts, and latency.
  Its prompt logging is off by default according to its current documentation.
- The encrypted credential and OAuth transaction are not included in Keepfit
  fitness or Android backups. Optional Block Store recovery is a separate,
  explicit choice. Disconnect removes local access and requests recovery-entry
  deletion.

### Assistant Safety Notes

- The assistant is general fitness guidance only.
- It is not medical advice.
- Requests for diagnosis, rehabilitation, medication changes, extreme dieting,
  or unsafe progression are refused locally.
- Keep core workflows independent of the assistant.
- Assistant requests are sent through OpenRouter when you use this feature.
- Sending clears the composer immediately. If the endpoint is unreachable,
  use the separate retry action; Keepfit retains that failed payload in memory
  without putting old text back into the input box.
- Provider output cannot write fitness data directly. Only locally validated
  plan or review proposals can reach an explicit apply action.
