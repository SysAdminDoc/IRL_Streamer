# Geometry validation report

Generated: 2026-08-15 05:34:42 -04:00
States compared: 145 of 145 with audit evidence
Target: element origins within 2 px
Recorded baseline: 33.6% within 2 px

## Method

Each replica UI hierarchy is matched against the audit UI hierarchy for the same
state. Nodes are paired by visible label, choosing the nearest candidate by origin
so that repeated labels are not mispaired. Only the element origin (left, top) is
scored: an Android `TextView` in a preference row stretches to the full row width
while the equivalent Compose `Text` wraps its glyphs, so right and bottom edges are
not comparable between the two toolkits and are reported for information only.

## Result

| Measure | Result |
|---|---:|
| Mean absolute origin error | 48.78 px |
| Origins within 2 px | 33.6% |
| Origins within 4 px | 36.5% |
| States with >=95% of origins within 2 px | 0 / 145 |
| States that measured nothing | 0 |
| States with no replica dump | 0 |

## Worst states

| Screen | Matched | Unmatched | max dleft | max dtop | Mean origin error px | Within 2 px |
|---|---:|---:|---:|---:|---:|---:|
| 101_overlay_blank_save_validation | 6 | 0 | 580 | 512 | 257.083 | 33.33% |
| 100_overlay_new_text_layer_form_actions | 6 | 0 | 580 | 512 | 257.083 | 33.33% |
| 114_web_overlay_custom_position_coordinates | 10 | 0 | 1751 | 93 | 200.3 | 40.0% |
| 110_web_overlay_webview_options | 8 | 0 | 580 | 310 | 186.938 | 37.5% |
| 104_overlay_timestamp_refresh_controls | 7 | 2 | 1881 | 163 | 180.929 | 42.86% |
| 099_overlay_new_text_layer_form_lower | 8 | 1 | 1911 | 198 | 177.625 | 43.75% |
| 075_audio_processing | 7 | 2 | 1863 | 224 | 168.357 | 57.14% |
| 010_streamer_settings_custom_page | 4 | 6 | 45 | 605 | 156.0 | 50.0% |
| 011_streamer_settings_final | 4 | 6 | 45 | 605 | 156.0 | 50.0% |
| 028_rtmp_authorization_fields | 6 | 4 | 180 | 361 | 154.75 | 16.67% |
| 024_new_connection_invalid_url | 9 | 0 | 580 | 198 | 143.333 | 16.67% |
| 023_new_connection_form_bottom | 9 | 0 | 580 | 198 | 142.0 | 16.67% |
| 082_recording_save_to_control | 15 | 3 | 1439 | 151 | 133.767 | 20.0% |
| 074_audio_input_gain_maximum | 9 | 0 | 1865 | 98 | 129.222 | 44.44% |
| 071_audio_settings_lower | 9 | 0 | 1863 | 98 | 129.111 | 44.44% |
| 009_streamer_settings_alerts | 3 | 6 | 45 | 692 | 129.0 | 50.0% |
| 026_new_connection_rtmp_advanced | 8 | 3 | 580 | 63 | 108.188 | 12.5% |
| 052_video_exposure_compensation_menu_lower | 7 | 1 | 91 | 148 | 82.214 | 14.29% |
| 053_video_exposure_compensation_menu_bottom | 7 | 0 | 91 | 109 | 71.071 | 14.29% |
| 063_video_noise_reduction_menu | 7 | 0 | 91 | 109 | 71.071 | 14.29% |

Per-element detail for every state is in `validation/reports/geometry/<screen>.json`.
