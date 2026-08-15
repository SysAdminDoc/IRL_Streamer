# Inferred user-facing data model

This is a clean-room UI model, not a claim about the original database or API. Entities and relationships are inferred only from visible forms, lists, state, and persistence.

## Configuration root

`AppSettings` aggregates singleton preference groups: Streamer, Bonding, Video, Audio, Recording, Display, Advanced, and import/export metadata. **STRONG INFERENCE:** these are locally persisted because values survive force-stop and no account/login is required to open them.

## Streamer profile

Visible fields include Twitch username, Kick username, chat layout toggles, platform/bot/command visibility, user count, custom chatbox URL, dashboard API-key fields, Streamelements enablement, custom page URL, and scale values. Usernames/URLs/keys are optional by visible summaries. API keys are secret fields conceptually even if the exact widget masking behavior was not fully observed.

## Bonding profile and link policy

One profile controls mode selection and a collection of link policies for Cellular, Wi-Fi, and Ethernet/USB. Each link has `enabled:Boolean` and `weight:Int` (observed default 100). Weights are relative. Mode/connection relationships are visible but exact storage types are **UNKNOWN**.

## Outgoing connection

An ordered/list entity with at least: Name, URL, active state, protocol derived from URL, target type, optional login/password, and service-specific quick-setup fields. Relationships:

- zero or more connections can exist;
- zero active connections blocks Start;
- bonding selection refers to connections;
- Twitch/Kick dialogs are creation helpers, not confirmed separate stored entities.

No connection was saved, so edit/delete ordering, duplicate rules, required Name, and secret redaction are **UNKNOWN**.

## Video profile

A singleton containing physical camera/start camera, multi-camera flag, resolution, FPS/range, orientation/live-rotation flags, focus/WB/anti-flicker/exposure, bitrate policy/value/mode, keyframe interval, codec/profile, stabilization/noise-reduction modes, adaptive algorithm, and adaptive-frame-rate flag. Capability-dependent choices form a relationship with the current device/camera.

## Audio profile

Singleton fields: prefer Bluetooth, source, channels, bitrate, sample rate, keep-speaker-awake, input gain, AEC, and noise suppression. Device effects/providers are capability-dependent.

## Recording profile

Singleton fields: record enabled, split enabled, section minutes, snapshot format, snapshot quality, SAF enabled, destination summary/tree URI. Resulting media files are external artifacts, not observed entities.

## Overlay layer

An ordered collection with a common identity/name and z-order. Subtypes:

- Picture layer: URL or selected file, periodic refresh.
- Text layer: HTML, periodic refresh/interval, active, scale, position, z-order.
- Web overlay: URL, CSS, view mode, position or custom coordinates, z-order, width, height, scale.

Separate standby and pause selections reference text/picture layers by identity. Higher z-order renders in front. The built-in Timestamp is a prepopulated Text layer.

## Runtime session state

Ephemeral state includes current physical camera, zoom/exposure, microphone mute, current FPS, current/target bitrate, conditioner, per-link live state/weight, active overlay toggles, preview/encoder status, battery telemetry, and diagnostic log lines. It must be reconciled with persisted profiles but should not all be written continuously.

## Notification model

The foreground-service notification has ID 101, a single low-importance channel, content intent, and Start/Exit actions. Notification text content was intentionally not rendered because opening the shade could expose unrelated private notifications.

## Unknown backend model

Accounts, subscriptions, analytics, remote chat/message entities, stream sessions, server capabilities, authentication tokens, and API schemas are **UNKNOWN**. Do not invent or clone them from brand expectations.

