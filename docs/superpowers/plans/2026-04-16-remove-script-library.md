# Talky Remove Script Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the in-app script library experience and simplify Talky into a recording-first app with only Home and Settings as top-level destinations.

**Architecture:** Keep the existing multi-activity shell, but collapse the top-level navigation to two entries and strip script-related runtime flows from home, recording, video detail, and manifest registration. Preserve the current Room schema for now so this refactor does not risk user video data.

**Tech Stack:** Android Views, AppCompat, ViewBinding, explicit `Intent` navigation, Room, CameraX, Media3

---

### Task 1: Remove script destinations from the runtime shell

**Files:**
- Modify: `app/src/main/res/menu/bottom_nav_menu.xml`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/common/BaseBottomNavActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] Remove the `文稿库` menu item from the bottom navigation resource.
- [ ] Update bottom-nav routing so it only switches between `MainActivity` and `SettingsActivity`.
- [ ] Unregister script-related activities from the manifest.

### Task 2: Simplify the home entry page

**Files:**
- Modify: `app/src/main/res/layout/fragment_home.xml`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/home/HomeFragment.kt`

- [ ] Remove the script-library entry card from the home layout.
- [ ] Rebalance the remaining card layout so `历史练习` still looks intentional.
- [ ] Delete the script-entry click logic from `HomeFragment`.

### Task 3: Turn recording into a pure free-recording flow

**Files:**
- Modify: `app/src/main/res/layout/fragment_recording.xml`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/recording/RecordingActivity.kt`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/recording/RecordingFragment.kt`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/recording/RecordingViewModel.kt`

- [ ] Remove script argument passing from `RecordingActivity`.
- [ ] Remove script loading and teleprompter-specific UI from `RecordingFragment`.
- [ ] Keep recording start/stop/save behavior intact after removing script references.
- [ ] Remove script state from `RecordingViewModel`.

### Task 4: Remove script linkage from video detail and settings copy

**Files:**
- Modify: `app/src/main/res/layout/fragment_video_detail.xml`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/video/VideoDetailFragment.kt`
- Modify: `app/src/main/res/layout/fragment_settings.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] Remove the "查看文稿" action from video detail.
- [ ] Delete the corresponding click handling from `VideoDetailFragment`.
- [ ] Update settings/privacy copy so it no longer mentions scripts or default script font size.
- [ ] Remove obsolete string resources that only served script flows.

### Task 5: Delete obsolete script UI classes and verify

**Files:**
- Delete: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptsActivity.kt`
- Delete: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptDetailActivity.kt`
- Delete: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptEditorActivity.kt`
- Delete: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptsFragment.kt`
- Delete: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptDetailFragment.kt`
- Delete: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptEditorFragment.kt`
- Delete: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptsViewModel.kt`
- Delete: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptDetailViewModel.kt`
- Delete: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptEditorViewModel.kt`
- Delete: `app/src/main/res/layout/fragment_scripts.xml`
- Delete: `app/src/main/res/layout/fragment_script_detail.xml`
- Delete: `app/src/main/res/layout/fragment_script_editor.xml`

- [ ] Delete script-specific UI/activity classes that are no longer reachable.
- [ ] Delete the script layouts tied to those classes.
- [ ] Run `rg` to confirm no runtime references remain to `ScriptsActivity`, `ScriptDetailActivity`, or `ScriptEditorActivity`.
- [ ] Run `./gradlew :app:assembleDebug`.
