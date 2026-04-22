# Talky Remove Script Library Design

**Date:** 2026-04-16

## Goal

Remove the in-app script library experience so Talky becomes a simpler recording-first tool for now. The app should focus on home, recording, history, video detail, and settings without any script browsing, editing, or linking flows.

## Approved Direction

- Remove the `Scripts` top-level destination entirely.
- Reduce bottom navigation from `首页 / 文稿库 / 我的` to `首页 / 我的`.
- Remove all script-related pages from runtime navigation:
  - `ScriptsActivity`
  - `ScriptDetailActivity`
  - `ScriptEditorActivity`
- Remove script actions from existing screens:
  - Home no longer shows the script library entry.
  - Recording becomes a free-recording page without script loading or teleprompter content.
  - Video detail no longer exposes a "view script" action.
- Update copy so the app consistently describes itself as a local recording practice tool, not a script management tool.

## Data Strategy

For this pass, remove script-library behavior from the runtime app but avoid risky database surgery. Existing video history should stay safe. Script-related database structures can remain dormant until a later schema cleanup or replacement feature.

## Return Rules

- Main navigation becomes `HomeActivity(MainActivity)` and `SettingsActivity`.
- Task pages remain independent activities using the standard Android back stack.
- Recording still saves directly into video detail after finalize succeeds.

## Implementation Notes

- Delete or unregister script-related activities and fragments from the runtime surface.
- Replace any remaining script references in navigation, layout bindings, and strings.
- Keep the multi-activity shell introduced earlier.
- Verify that no runtime code still references `ScriptsActivity`, `ScriptDetailActivity`, or `ScriptEditorActivity`.
