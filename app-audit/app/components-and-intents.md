# Components, intents, and external integrations

## Deep links

| Contract | Classification | Evidence-backed behavior |
|---|---|---|
| `larix:` custom scheme | CONFIRMED | `LaunchActivity` resolves `ACTION_VIEW` + `BROWSABLE` for scheme `larix`. Import settings presents a `larix://` text field (screen 117). Payload grammar beyond that prefix is UNKNOWN. |

The audit did not submit an import payload because exported/imported settings can contain private stream destinations or keys. Any rebuild must define a versioned, validated, non-secret-by-default import format and require explicit confirmation before replacing settings.

## System intents and handoffs

| Surface/action | Result | Test status | Evidence |
|---|---|---|---|
| Recording > Save to | Android DocumentsUI folder picker | opened, canceled, no destination changed | screen 082 |
| Import/Export > Scan QR code | app-owned prompt asks to install “Barcode Scanner” | prompt observed, install declined | screen 118 |
| Help > Discord | likely external browser/app link | UNTESTED to avoid exposing signed-in browser state | screen 128 control |
| USB OTG help text | says tapping opens website details | UNTESTED external link | screens 123–125 |
| Send cameras debug details | likely share/email handoff containing device details | UNTESTED privacy-sensitive | screen 128 |
| Export settings | likely text/QR/share flow containing configuration | UNTESTED privacy-sensitive | screen 116 |

## Streaming and content integrations

- **CONFIRMED UI contracts:** generic protocol URL input; RTMP target types; Twitch quick setup; Kick quick setup; custom SRTLA URLs; Streamlabs API key; Streamelements dashboard with manual login; Toonation API key; custom chatbox URL; custom page URL; transparent web overlays; QR/import workflow.
- **CONFIRMED runtime observation:** PID-scoped logs contain `com.newrelic.android` instrumentation and a failed attempt to reach the New Relic mobile collector. No payload, account configuration, or traffic contents were inspected; whether telemetry belongs in a replacement is a product/privacy decision.
- **STRONG INFERENCE:** protocol handling includes RTMP, SRT, SRTLA, and a bonding service because those names and URL examples are visible across screens 016–028 and 120–138.
- **UNKNOWN:** endpoint paths, authentication exchange, retry policy, TLS behavior, Twitch/Kick API calls, stream-status APIs, chat transport, New Relic configuration/data policy, other analytics, or any server-owned contract. No traffic interception or private backend probing was performed.

## WebView security requirements for a rebuild

These are recommendations, not observed internals: isolate untrusted overlay content, allow only needed schemes, default file/content access off, gate geolocation per overlay and Android permission, keep debugging off in release, provide explicit cookie clearing with confirmation, and never inject credentials into arbitrary pages.
