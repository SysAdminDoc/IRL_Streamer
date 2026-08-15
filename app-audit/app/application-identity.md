# Application identity

| Field | Classification | Value | Evidence |
|---|---|---|---|
| App label | CONFIRMED | IRL Pro | screens 002, 129 |
| Package | CONFIRMED | `app.irlpro.android` | package dump |
| Version | CONFIRMED | 3.5.23 | package dump; screen 129 |
| Version code | CONFIRMED | 305230 | package dump; screen 129 |
| Minimum SDK | CONFIRMED | 28 | package dump |
| Target SDK | CONFIRMED | 34 | package dump |
| Installer | CONFIRMED | Google Play (`com.android.vending`) | package dump |
| Launcher activity | CONFIRMED | `com.wmspanel.streamer.LaunchActivity` | resolver evidence |
| Primary activity | CONFIRMED | `com.wmspanel.streamer.StreamerServiceActivity` | screen/activity evidence |
| Settings activity | CONFIRMED | `com.wmspanel.streamer.preference.SettingsActivity` | screen/activity evidence |
| Author/version text | CONFIRMED | “Built by WilliamH” | screen 129 |
| Licensed code notice | CONFIRMED | “Includes licensed SRTLA code” | screen 129 |

The Play listing supplied by the operator is `https://play.google.com/store/apps/details?id=app.irlpro.android`. Public-store metadata is contextual only; this audit’s reconstruction facts come from the installed app and ADB evidence.

The observed package has code, allows Android backup, and is installed/enabled for user 0. Internal libraries, source architecture, backend contracts, signing configuration, and persistence technology remain **UNKNOWN** because static analysis was disabled.

Brand caution: the name “IRL Pro,” its magenta camera icon, and any third-party service names are not automatically licensed for a clean-room replacement. Use an authorized identity or original replacement assets.

