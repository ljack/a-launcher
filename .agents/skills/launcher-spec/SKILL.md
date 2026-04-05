---
name: launcher-spec
description: Spec discovery stage for the Android launcher app. Interactively explores ideas with the user, then delegates to the android-architect agent to produce a formal spec and architecture document. Use when starting fresh or revising the launcher concept.
---

# Launcher Spec Discovery

Interactive specification discovery for a unique Android home launcher app.

## How It Works

This skill guides a two-phase spec process:

### Phase 1: Discovery Interview (you do this directly)

Ask the user these questions one at a time, building on their answers. Don't dump all questions at once — have a conversation:

1. **Core concept** — What makes this launcher unique? What's the one-sentence elevator pitch?
2. **Home screen** — What does the main screen look like? App grid? List? Something else entirely?
3. **App drawer** — How do users find and launch apps? Search? Categories? Gestures?
4. **Navigation** — Swipe gestures? Tabs? Pages? How do users move between sections?
5. **Widgets** — Support Android widgets? Custom widgets? Neither?
6. **Wallpaper** — Wallpaper support? Dynamic/live wallpapers? Custom backgrounds?
7. **Customization** — What can users customize? Themes? Layouts? Icon packs?
8. **Notifications** — Show notification badges? Notification panel access?
9. **Search** — Universal search? App search only? Device search?
10. **Unique features** — Any features no other launcher has? This is where the magic lives.

After gathering answers, summarize back to the user for confirmation.

### Phase 2: Formal Spec (delegate to agent)

Once the user confirms, use the subagent tool to run the `android-architect` agent with scope `"both"` (set `agentScope: "both"`, `confirmProjectAgents: false`):

```
Task: Create the formal specification and architecture for an Android launcher app with these requirements:
[paste the confirmed requirements summary here]

Write the spec to docs/spec.md and architecture to docs/architecture.md.
Create the directory with mkdir -p docs if needed.
```

### Phase 3: Review

After the architect finishes, read `docs/spec.md` and `docs/architecture.md` and present a summary to the user. Ask if they want to modify anything.

If they do, run the architect again with the modifications.

## Files Produced
- `docs/spec.md` — Living specification
- `docs/architecture.md` — Technical architecture
