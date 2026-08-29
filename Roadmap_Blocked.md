# Blocked roadmap items

Items that need external input or a rig this machine does not have. Each keeps its original text plus the blocker.

- [ ] P2 — IS-99 Inline form validation replaces the audited validation toast
  Why: audit state 101 records the original app showing a 3.5 second system toast for a blank-save validation error, which capture timing can miss entirely (IS-48) and which leaves no error next to the field. Inline validation is the improvement, but it changes a captured screen.
  Evidence: `app-audit/screens/screen-specs/101_overlay_blank_save_validation.json` state_name "Blank HTML Save validation toast; draft discarded"; RESEARCH.md 2026-08-29.
  Touches: `ui/settings/Forms.kt`, `ui/components/FormTextField`, `docs/known-deviations.md` (needs a new D-number, precedent D010)
  Acceptance: a blank required field shows its error under the field with no toast; the geometry gate still passes on state 101 and the deviation is recorded.
  Complexity: M
  BLOCKED: changing an audited screen needs the 145-state geometry gate to re-run, and the harness AVD does not exist on this machine. `emulator -list-avds` has no `issue-sweep-api36` and no android-36 system image is installed, so captures cannot be compared against the baseline that was recorded at 2316x1080 / 450 dpi on API 36. Install the android-36 image and recreate the AVD, or re-baseline deliberately with `-BaselineReason`, before taking this item.
