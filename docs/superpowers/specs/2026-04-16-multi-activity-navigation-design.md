# Talky Multi-Activity Navigation Design

**Date:** 2026-04-16

## Goal

Replace the current single-`Activity` + `NavHostFragment` shell with a multi-`Activity` structure that feels closer to a traditional Android app. Top-level areas should feel independent, and task-heavy screens such as recording, detail, and editor flows should no longer appear as sub-pages inside the same root shell.

## Approved Structure

### Top-level activities

- `MainActivity` acts as the Home entry activity.
- `ScriptsActivity` hosts the script library.
- `SettingsActivity` hosts the settings page.

These three activities share the bottom navigation and should switch between each other directly.

### Task activities

- `HistoryActivity`
- `RecordingActivity`
- `ScriptDetailActivity`
- `ScriptEditorActivity`
- `VideoDetailActivity`

These activities do not show the bottom navigation. They are task-focused and should rely on the standard Android back stack.

## Reuse Strategy

The current fragments and layouts remain the content units for now. Each new activity hosts exactly one fragment. This keeps the UI stable while replacing only the navigation shell and routing behavior.

## Navigation Rules

- Bottom navigation exists only in `MainActivity`, `ScriptsActivity`, and `SettingsActivity`.
- Switching tabs starts the target top-level activity and closes the current top-level activity so tab stacks do not pile up.
- `HistoryActivity` opens from Home.
- `RecordingActivity` opens from Home and script-related pages.
- `ScriptDetailActivity` opens from `ScriptsActivity` and `VideoDetailActivity`.
- `ScriptEditorActivity` opens from `ScriptsActivity`.
- `VideoDetailActivity` opens from `HistoryActivity` and after recording finalization succeeds.

## Return Rules

- Top-level activities do not return to one another with the system back button.
- Task activities return to the activity that launched them.
- Successful recording should navigate to `VideoDetailActivity`.
- Saving a script from the editor should finish the editor and return to the previous screen.

## Implementation Notes

- Remove `NavHostFragment` routing from the runtime path.
- Replace fragment `findNavController()` calls with explicit `Intent` navigation or `finish()`.
- Keep argument passing via fragment arguments, but now populate those arguments from the hosting activity.
- Keep repository and ViewModel logic intact unless needed to support the new navigation flow.
