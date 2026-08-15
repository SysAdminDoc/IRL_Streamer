# Deterministic fixtures

The shipping low-light preview is `app/src/main/res/drawable-nodpi/preview_fixture.png`. It was generated specifically for this clean-room project as a generic, non-identifying 16:9 camera-like scene and contains no text, logos, people, or copied audit pixels.

Telemetry, network values, and log messages are deterministic constants in the live UI. They are intentionally sanitized and must never include the original phone's identifiers or private runtime data.
