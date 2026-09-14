# Optional Integrations

## Live Exercise Demonstrations

The Workouts exercise-source rail includes an optional `Live demos` prototype.
It uses the keyless ExerciseDB hosted API only after you submit a search. The
request contains the exercise name and optional body-area, muscle, and equipment
filters you entered; it does not include profile, workout, nutrition, progress,
notes, identifiers, or private media.

Results are view-only, not stored in Keepfit or included in backups. Use the
40-item `Offline guide` whenever a connection or the provider is unavailable.

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
- ask ordinary training and nutrition questions;
- ask about recent personal progress and receive insights from compact local
  workout, nutrition, step, and transformation summaries; and
- retry a failed query without exposing any data-changing action.

### Connect the Assistant

1. Open the `Coach` tab.
2. Read the outbound-data disclosure and choose `Connect OpenRouter`.
3. Approve access in the system browser and return to Keepfit.
4. Use the connection menu if you want to check or disconnect access.

### Use the Assistant

Inside Coach you can:

1. Type a general question, or choose a starter question.
2. Ask about "my progress", "my recent workouts", or similar personal history
   when you want Keepfit to include a compact local snapshot.
3. Look for the context notice below the response to confirm local activity was
   included.

Coach is query-only in v0.10. It cannot edit plans, add food, or write local
records.

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
- Weight, BMI, height, transformation photos, body measurements, identifiers,
  private paths, and raw database rows are excluded.
- OpenRouter records request metadata such as model, token counts, and latency.
  Its prompt logging is off by default according to its current documentation.
- The encrypted credential and OAuth transaction are not included in Keepfit
  backups. Disconnect removes them from the device.

### Assistant Safety Notes

- The assistant is general fitness guidance only.
- It is not medical advice.
- Requests for diagnosis, rehabilitation, medication changes, extreme dieting,
  or unsafe progression are refused locally.
- Keep core workflows independent of the assistant.
- Assistant requests are sent through OpenRouter when you use this feature.
- If the endpoint is unreachable, drafted chat text should remain available for
  retry.
- Provider output cannot write fitness data in the current Coach experience.
