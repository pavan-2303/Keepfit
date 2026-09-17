# Privacy

Keepfit is designed as a local-first Android application.

## Data Stored on the Device

Workout history, nutrition entries, profiles, measurements, transformation
photos, imported exercise media, settings, and Coach conversations are stored
locally. Transformation photos and imported media use app-private storage.

Keepfit does not include analytics, advertising, telemetry, remote logging, or
a Keepfit account service.

## Optional External Services

- **OpenRouter:** Used only after the user connects their own account and sends
  a Coach request. Requests may include the typed question and bounded context
  described in the app. Credentials use Android secure storage and are not
  included in Keepfit backups.
- **Health Connect:** Read-only step access is requested only when the user
  enables it. Keepfit does not duplicate Health Connect records into Room.
- **Android backup and transfer:** Supported devices may recover allowlisted
  structured records and ordinary settings. Private media and secrets are
  excluded. Users can also create an explicit encrypted backup.

External providers apply their own privacy terms. Core tracking works without
these optional integrations.

## Public Contributions

GitHub issues and pull requests are public. Never attach real health records,
photos, provider credentials, backups, or signing material when reporting a
problem.
