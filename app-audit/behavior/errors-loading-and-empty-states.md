# Errors, loading, empty, and disabled states

## Captured states

| State | Observable behavior | Classification | Evidence |
|---|---|---|---|
| Launch initialization | black preview/control shell, codec-resolution toast, preview frames appear, FPS stabilizes | CONFIRMED | cold-launch video; 144 |
| No outgoing connections | list exposes New/Manage; manager empty | CONFIRMED | 021, 029 |
| Start with no active connection | blocking modal, Cancel/Create connection; no start | CONFIRMED | 142 |
| Incomplete connection form | Save disabled while URL empty | CONFIRMED | 024 |
| Blank Text overlay HTML | toast; draft remains | CONFIRMED | 101 |
| No custom web overlays | New enabled; Delete multiple disabled | CONFIRMED | 115 |
| Missing QR scanner | install prompt; audit selected No | CONFIRMED | 118 |
| Preferred Camera API unavailable | row disabled | CONFIRMED current device | 126 |
| Horizon demo unavailable | switch disabled | CONFIRMED current device | 122 |
| USB Camera unavailable | switch disabled | CONFIRMED current device | 123 |
| Live rotation unavailable | switch disabled in current state | CONFIRMED | 033 |
| Network Stats idle | section visible without values while no stream | CONFIRMED | 134 |
| Reload feedback | non-blocking asynchronous toast | CONFIRMED | 143 |

## Loading patterns

No full-screen spinner or skeleton was observed. Camera startup uses immediate shell rendering plus black video surface and toast feedback. Web/remote content loading, connection negotiation, image fetch, QR scan, and recording finalization were not safely exercised, so their indicators/timeouts are **UNKNOWN**.

## Failure states not induced

Networking was not disabled; permissions were not revoked; camera/microphone contention was not created; storage was not filled; invalid credentials were not submitted; and remote endpoints were not contacted. Offline, timeout, DNS, TLS, auth, server rejection, disk-full, SAF-permission-loss, encoder-unsupported, thermal shutdown, and background-policy failures are all **UNKNOWN**.

## Reconstruction error requirements

The rebuild should add deterministic, user-readable errors for every unknown class above, preserve drafts where safe, prevent duplicate start/save actions, provide retry for transient operations, and distinguish configuration validation from transport/runtime failures. Secret values must never be echoed in errors or logs.

