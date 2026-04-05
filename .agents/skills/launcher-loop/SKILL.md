---
name: launcher-loop
description: The loop-de-loop workflow for the Android launcher. Runs build-test-review-fix cycles continuously. Use for iterating on the launcher after initial build, or to implement the next feature from the spec.
---

# Launcher Loop

Continuous build → test → review → fix loop for the Android launcher.

## How It Works

This is the main development loop. Each iteration:

### 1. Determine What to Do

Read `docs/spec.md` and find the next uncompleted feature (not marked ✅). If the user provided a specific request via arguments, use that instead.

Present to the user:
> **Next up:** [feature name/description]
> Proceed? (or tell me what to work on instead)

### 2. Build Phase

Use the subagent tool chain with `agentScope: "both"` and `confirmProjectAgents: false`:

```
Chain:
1. scout → Find code relevant to the feature
2. android-builder → Implement the feature
3. android-tester → Build and verify
```

### 3. Review Phase

If the build succeeded, run:

```
Chain:
1. android-reviewer → Review the implementation against spec
```

### 4. Fix Phase

If the reviewer or tester found issues, run:

```
Chain:
1. android-builder → Fix the issues found: {previous}
2. android-tester → Verify the fixes
```

Repeat step 3-4 until the reviewer is satisfied (max 3 iterations to avoid infinite loops).

### 5. Mark Complete

Update `docs/spec.md` to mark the feature as ✅ completed.

### 6. Loop

Tell the user what was completed, show the build status, and ask:
> **Completed:** [feature]
> **Next up:** [next feature from spec]
> Continue to next feature, or give me a different task?

If the user says continue, go back to step 2 with the next feature.

## Usage

```
/skill:launcher-loop                    # Pick next feature from spec
/skill:launcher-loop add gesture nav    # Work on a specific thing
/skill:launcher-loop fix build errors   # Fix current build issues
```

## Error Recovery

If the build fails 3 times on the same error:
1. Read the full error output
2. Read the relevant source files yourself (don't delegate)
3. Fix it directly
4. Run `./gradlew assembleDebug` to verify
5. Then continue the loop
