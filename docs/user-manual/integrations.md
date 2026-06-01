# Health Connect and Assistant

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

- Keepfit does not copy step history into Room.
- If Health Connect is unsupported, the rest of the app still works normally.
- If permission is denied or revoked, the card shows an unavailable state.

## Ollama Assistant

The assistant is optional and currently configured for direct Ollama Cloud API
usage through build-time app configuration.

Current capabilities:

- enable or disable assistant access;
- test the configured connection;
- open a chat screen;
- send and retry normal chat requests;
- generate a progress summary from local workout, nutrition, progress, and
  optional steps data;
- request a draft weekly plan;
- review, dismiss, or apply the draft only after you inspect it.

### Configure the Assistant

1. Open `Settings`.
2. Enable `optional Ollama Cloud assistant`.
3. Save the setting.
4. Use `Test connection`.
5. Tap `Open assistant`.

### Use the Assistant

Inside the assistant screen you can:

1. Ask a normal question and tap `Send`.
2. Tap `Summarize progress` to generate a summary from your local data.
3. Type an optional goal or constraint, then tap `Draft weekly plan`.
4. Review the draft card before choosing `Apply draft` or `Dismiss draft`.

If you leave the input blank before tapping `Draft weekly plan`, Keepfit uses a
safe default request for a balanced weekly plan.

### Build-Time Cloud Configuration

This build expects the following values to be supplied while building the app:

```text
keepfit.ollama.baseUrl=https://ollama.com/api
keepfit.ollama.generalModel=mistral-large-3:675b
keepfit.ollama.reasoningModel=qwen3.5:397b
keepfit.ollama.apiKey=your-api-key
```

General assistant chat uses `mistral-large-3:675b`. Deeper reasoning tasks
such as plan curation and later analysis flows use `qwen3.5:397b`.

Use direct model names. Do not use names ending in `-cloud` when calling
`ollama.com/api` directly.

### Assistant Safety Notes

- The assistant is general fitness guidance only.
- It is not medical advice.
- Keep core workflows independent of the assistant.
- Assistant requests are sent to Ollama Cloud when you use this feature.
- If the endpoint is unreachable, drafted chat text should remain available for
  retry.
- Applying a draft weekly plan can create missing exercises with generic
  defaults so the schedule can be applied without manual setup first.
