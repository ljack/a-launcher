---
name: launcher-build
description: Build stage for the Android launcher app. Scaffolds the project and implements features from the spec using subagents. Use after launcher-spec has produced docs/spec.md and docs/architecture.md.
---

# Launcher Build

Implements the Android launcher app from the specification.

## Prerequisites

- `docs/spec.md` and `docs/architecture.md` must exist (created by `/skill:launcher-spec`)
- Android SDK available, `ANDROID_HOME` or `ANDROID_SDK_ROOT` set

## How It Works

### Step 1: Check spec exists

Read `docs/spec.md`. If it doesn't exist, tell the user to run `/skill:launcher-spec` first.

### Step 2: Scaffold (if no project exists yet)

If there's no `build.gradle.kts` in the project root, scaffold the Android project yourself (don't delegate this — do it directly):

1. Create the Gradle wrapper and project structure:
   - `settings.gradle.kts`, root `build.gradle.kts`
   - `app/build.gradle.kts` with Compose, Hilt, Room, etc.
   - `gradle/libs.versions.toml` for version catalog
   - `app/src/main/AndroidManifest.xml` with launcher intent filter
   - `app/src/main/java/com/alauncher/` package structure per architecture doc
   - Basic `MainActivity.kt` with `setContent { }` and `@AndroidEntryPoint`
   - `app/src/main/res/values/` with themes, strings, colors

2. Download Gradle wrapper:
   ```bash
   gradle wrapper --gradle-version 8.12
   ```
   If `gradle` isn't available, create wrapper files manually.

3. Verify build:
   ```bash
   ./gradlew assembleDebug
   ```

### Step 3: Incremental Implementation

Read `docs/spec.md` and break it into implementable chunks. For each chunk, use the subagent tool as a **chain** with `agentScope: "both"` and `confirmProjectAgents: false`:

1. **scout** — Find current code state relevant to the feature
2. **android-builder** — Implement the feature
3. **android-tester** — Build and verify

If the tester reports errors, run another chain:
1. **scout** — Find the errors in context
2. **android-builder** — Fix the errors
3. **android-tester** — Verify the fix

Repeat until the build passes.

### Step 4: Update Spec

After each successful feature implementation, update `docs/spec.md` to mark the feature as ✅ completed.

## Feature Order

Implement in this order (unless spec says otherwise):
1. Basic launcher activity (HOME category, manifest)
2. App list/grid loading from PackageManager
3. App launching
4. Home screen layout
5. App drawer
6. Search
7. Gestures/navigation
8. Widgets (if in spec)
9. Settings/customization
10. Unique features

## Files
- All source code under `app/src/main/java/com/alauncher/`
- Resources under `app/src/main/res/`
- Build files at project root
