# Talky Multi-Activity Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single-activity navigation shell with a multi-activity Android structure while reusing existing fragments as screen content.

**Architecture:** Three top-level activities share bottom navigation, while five task activities each host a single fragment and use explicit `Intent` routing. Existing fragment UIs stay in place; only shell and routing behavior change.

**Tech Stack:** Android Views, AppCompat, ViewBinding, FragmentManager, explicit `Intent` navigation, Room, CameraX, Media3

---

### Task 1: Add shared activity shells

**Files:**
- Create: `app/src/main/java/com/lixiangyang/talky/ui/common/BaseFragmentHostActivity.kt`
- Create: `app/src/main/java/com/lixiangyang/talky/ui/common/BaseBottomNavActivity.kt`
- Create: `app/src/main/res/layout/activity_fragment_host.xml`
- Create: `app/src/main/res/layout/activity_bottom_nav_host.xml`

- [ ] Create a single-fragment activity shell for task pages.
- [ ] Create a bottom-navigation activity shell for top-level pages.
- [ ] Keep fragment hosting logic centralized so individual activities stay small.

### Task 2: Create activity entry points

**Files:**
- Modify: `app/src/main/java/com/lixiangyang/talky/MainActivity.kt`
- Create: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptsActivity.kt`
- Create: `app/src/main/java/com/lixiangyang/talky/ui/settings/SettingsActivity.kt`
- Create: `app/src/main/java/com/lixiangyang/talky/ui/history/HistoryActivity.kt`
- Create: `app/src/main/java/com/lixiangyang/talky/ui/recording/RecordingActivity.kt`
- Create: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptDetailActivity.kt`
- Create: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptEditorActivity.kt`
- Create: `app/src/main/java/com/lixiangyang/talky/ui/video/VideoDetailActivity.kt`

- [ ] Make `MainActivity` the Home activity using the bottom-nav shell.
- [ ] Add activity classes for every approved destination.
- [ ] Ensure each activity creates the correct fragment with the right arguments.

### Task 3: Replace fragment navigation calls

**Files:**
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/home/HomeFragment.kt`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptsFragment.kt`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptDetailFragment.kt`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptEditorFragment.kt`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/history/HistoryFragment.kt`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/recording/RecordingFragment.kt`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/video/VideoDetailFragment.kt`

- [ ] Replace `findNavController()` usage with explicit activity launches.
- [ ] Replace toolbar/back behavior with `finish()`.
- [ ] Preserve route parameters like `scriptId` and `practiceId`.

### Task 4: Fix save/result flows that depend on navigation

**Files:**
- Modify: `app/src/main/java/com/lixiangyang/talky/data/repository/TalkyRepository.kt`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/scripts/ScriptEditorViewModel.kt`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/recording/RecordingViewModel.kt`
- Modify: `app/src/main/java/com/lixiangyang/talky/ui/recording/RecordingFragment.kt`

- [ ] Return inserted ids where downstream activities need them.
- [ ] Make recording success route into `VideoDetailActivity`.
- [ ] Keep editor save and save+record flows coherent under explicit `Intent` navigation.

### Task 5: Remove obsolete shell dependencies and verify

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/navigation/nav_graph.xml`
- Modify: `app/src/main/res/layout/activity_main.xml` or remove from runtime usage

- [ ] Register all activities in the manifest and move launcher intent to the Home entry point.
- [ ] Leave old nav graph unused or minimal, but ensure runtime no longer depends on it.
- [ ] Run `./gradlew :app:assembleDebug` and `./gradlew :app:installDebug`.
- [ ] Smoke test Home -> History -> VideoDetail and Scripts -> ScriptDetail -> Recording.
