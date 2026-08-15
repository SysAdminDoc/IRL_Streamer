# Persistence and lifecycle

## Launch measurements

| Scenario | ADB launch state | Total time | Wait time | Classification |
|---|---:|---:|---:|---|
| After `am force-stop`, saved data retained | COLD | 330 ms | 347 ms | CONFIRMED, one device/sample |
| From Home with warm task | HOT | 119 ms | 121 ms | CONFIRMED, one device/sample |
| After Back moved task to launcher, relaunch | WARM | 174 ms | 182 ms | CONFIRMED, one device/sample |

Measurements are approximate, device-specific, and influenced by an already-installed/configured app. They are not benchmark-grade percentiles.

## State persistence

- **CONFIRMED:** preferences survive navigation away, background/resume, and force-stop/relaunch.
- **CONFIRMED:** temporary audit changes were restored: platform icons off, link weights 100, resolution-matched bitrate on, gain 0 dB, audio meter on, grid off, safe margins off with 16:9/5%, Timestamp inactive, no web overlays, no outgoing connections.
- **CONFIRMED:** unsaved connection/overlay drafts disappear after Cancel/Back; no test records remained.
- **CONFIRMED:** settings scroll positions often return near the previously visited section during the same activity session, consistent with retained preference-fragment scroll state. Exact cross-process scroll persistence is **UNKNOWN**.
- **CONFIRMED:** Back from nested settings returns to the parent; Back from a modal dismisses it; Back from the live console shows the launcher.
- **CONFIRMED:** the camera/microphone foreground service and ongoing notification are active while the console is running. After Back, the task is backgrounded and the process/service remain represented by the task/service evidence.
- **CONFIRMED copy:** “Quit if inactive in background” says the app quits after a timeout when backgrounded with no active connections. The timeout duration and actual stop transition are **UNTESTED**.
- **CONFIRMED:** “Keep streaming when not in focus” is always enabled and disabled as an editable control.

## Rotation and configuration

The app forces landscape from a portrait launcher. Rotation/live-rotation/reverse-orientation settings were not changed. Multi-window, process death under memory pressure, configuration changes, locale switch, font-scale changes, and device reboot persistence are **UNKNOWN**.

## Rebuild persistence contract

Persist user settings transactionally and separately from unsaved form drafts. Preserve connection/layer ordering and IDs, keep secrets encrypted with Android Keystore-backed storage, persist SAF URI grants, restore the last safe camera/profile configuration, and never start an unintended broadcast solely because the process is recreated. A foreground service must expose explicit Start/Exit notification actions and correctly reconcile UI state after process/service rebinding.

