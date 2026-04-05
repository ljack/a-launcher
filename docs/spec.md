# A-Launcher Specification

> A living, spatial launcher where apps orbit around your thumb based on usage gravity.
> AI-powered search finds anything. Navigation extends into apps themselves.

## Core Principles

1. **Spatial memory** — Apps settle into stable positions. They drift slowly with usage patterns, never jump. Muscle memory is sacred.
2. **Thumb-anchored** — UI orients around where the user's thumb touches. Works left/right hand, one-handed, bike-mounted.
3. **Pause & resume** — Interaction state persists. Look away, come back, it's still there. No snap-back to defaults.
4. **Beautiful & distinct** — People look twice. Not a grid. Not a void. Information-rich but not noisy.
5. **Fast & smooth** — 60fps minimum, instant response, zero jank.

---

## Features

### F1: Spatial App Field ⬜

The home screen displays apps as visual elements floating in a spatial field over the user's wallpaper.

**Behavior:**
- Apps are rendered as circular orbs/cards with their icon and name
- Position is determined by **usage gravity**: most-used apps cluster near the center/thumb zone, less-used apps drift outward
- Apps hold their positions — they settle and only drift slowly over days/weeks as usage patterns change
- The spatial field extends beyond the visible screen; users pan/orbit to explore
- Visual cues (density, brightness, subtle paths/connections) help the user intuit where app clusters are
- Wallpaper is visible behind the floating app elements

**Acceptance Criteria:**
- [ ] Apps render as floating elements over wallpaper
- [ ] App positions persist across launcher restarts
- [ ] Position recalculation is gradual (animated over multiple sessions, not instant)
- [ ] Smooth 60fps rendering with 200+ apps loaded
- [ ] Visual density communicates app frequency without text labels needed

### F2: Thumb-Anchored Navigation ⬜

The entire UI orients around the user's thumb position. No fixed layout assumptions.

**Behavior:**
- When the user touches the screen, the nearest apps gravitate toward the touch point
- The "orbit" of apps is relative to thumb position, not screen center
- Works identically for left-hand and right-hand use
- Drag to orbit/browse through the spatial field
- Pinch to zoom: zoom out reveals more apps (less detail), zoom in shows fewer apps (more detail, app layers)
- Release on an app to select it; a **confirmation gesture** (short swipe or double-tap) launches it
- If the user lifts their thumb and comes back within 5 seconds, the view is exactly where they left it (pause & resume)
- After 5 seconds of inactivity, the view slowly (over 1 second) drifts back to the default resting state

**Acceptance Criteria:**
- [ ] Touch anchors the UI to thumb position
- [ ] Left-hand and right-hand usage feels natural
- [ ] Pinch-to-zoom navigates depth layers
- [ ] Gesture-to-launch prevents accidental launches
- [ ] State persists for 5 seconds after thumb lift
- [ ] Smooth drift-back animation after timeout

### F3: App Layers (Deep Navigation) ⬜

Navigation extends INTO apps, exposing their content as sub-layers within the launcher.

**Behavior:**
- Pinching into / zooming into an app reveals its **layer**: recent content from that app
- Layer examples:
  - **WhatsApp / Telegram**: Recent chats (top 5-10), tap to open that specific chat
  - **Chrome**: Open tabs, tap to switch to a tab
  - **Gmail**: Recent emails (top 5), tap to open
  - **Calendar**: Next 3-5 upcoming events
  - **YouTube**: Recently watched / recommended
  - **Audible**: Current audiobook with progress
- Layers are loaded lazily and cached
- Not all apps have layers — only apps with supported integrations
- Layer data comes from:
  - Notification content (accessible via NotificationListenerService)
  - Accessibility services (optional, for deeper integration)
  - App shortcuts (ShortcutManager API)
  - Usage stats

**Acceptance Criteria:**
- [ ] Zoom-into-app reveals sub-content layer
- [ ] At least 3 app integrations working (WhatsApp, Chrome, Calendar)
- [ ] Layer content is fresh (updated within last minute)
- [ ] Tapping a layer item opens the app at that specific content
- [ ] Graceful fallback for apps without layers (just launches the app)

### F4: AI-Powered Search ⬜

Intelligent, semantic search powered by on-device Gemma SLM.

**Behavior:**
- Activated by quick flick-up gesture from bottom of screen
- Search overlay appears anchored to thumb position
- Matches against: app name, icon label, package name, app store description, app content (via layers)
- **Negative search**: prefix with `-` or `NOT` to exclude (e.g., `NOT games`, `music -youtube`)
- **Semantic search**: understands intent ("something to track my run" → Strava, Nike Run Club)
- Results appear as floating orbs, same visual style as the spatial field
- Typing is fast: large touch targets, auto-suggestions
- On-device Gemma model handles semantic understanding
- Falls back to fuzzy string matching if Gemma is unavailable/loading

**Acceptance Criteria:**
- [ ] Flick-up gesture activates search
- [ ] String matching works across app name, label, description
- [ ] Negative search filters results
- [ ] Semantic search returns relevant results for intent-based queries
- [ ] Results render in <200ms for string search
- [ ] Gemma model runs on-device without network
- [ ] Graceful fallback to fuzzy matching

### F5: Media Control Hub ⬜

Built-in media controls, not a widget. Part of the launcher's native UI.

**Behavior:**
- Persistent but unobtrusive media indicator when audio is playing
- Expands on touch to show:
  - Play / Pause / Skip controls
  - Current track/audiobook info (title, artist/author, remaining time)
  - **Source switcher**: tap to switch between active media apps (YT Music, Audible, Spotify, etc.)
- Uses Android MediaSession API to control any media app
- Visual style matches the spatial field aesthetic
- Can be accessed from any state (home, search, layers)

**Acceptance Criteria:**
- [ ] Detects active media sessions
- [ ] Play/pause/skip controls work for any media app
- [ ] Source switching between multiple media apps
- [ ] Shows current track metadata (title, artist, progress)
- [ ] Audible integration shows remaining time
- [ ] Controls accessible from all launcher states

### F6: Persistent Notification Tray ⬜

Notifications that don't disappear until the user explicitly dismisses them.

**Behavior:**
- Uses NotificationListenerService to capture all notifications
- Notifications are stored locally and persist even after the source app clears them
- Accessed via gesture (swipe from edge or dedicated area in the spatial field)
- Grouped by app
- Tap to open the relevant app/content
- Explicit dismiss: swipe away or long-press to clear
- Badge indicators on apps in the spatial field show unread notification count
- Option to pin critical notifications

**Acceptance Criteria:**
- [ ] Notifications persist beyond system clearing
- [ ] Grouped by app with unread count
- [ ] Tap opens relevant content
- [ ] Manual dismiss required
- [ ] Badge indicators visible on app orbs in spatial field

### F7: Usage Gravity Engine ⬜

The system that determines app positions based on usage patterns.

**Behavior:**
- Tracks: launch count, launch recency, time-of-day patterns, session duration
- **Gravity score** = weighted combination of frequency + recency + contextual relevance
- Apps with high gravity score cluster near the center/default thumb zone
- Score updates are smoothed: positions change gradually over days, not instantly
- Time-of-day awareness: morning apps (Gmail, Calendar) drift closer in the morning; evening apps (YouTube, Netflix) drift closer at night
- Position history is stored; apps have a "home region" they tend to return to
- New apps start at the periphery and drift inward as they're used

**Acceptance Criteria:**
- [ ] Launch events are tracked with timestamp
- [ ] Gravity score computed from frequency + recency + time-of-day
- [ ] App positions update gradually (over multiple sessions)
- [ ] Time-of-day patterns influence positioning
- [ ] New apps appear at periphery
- [ ] Position history persists across restarts

### F8: Launcher Activity & System Integration ⬜

The foundational Android launcher behavior.

**Behavior:**
- Registers as a home launcher (CATEGORY_HOME + CATEGORY_DEFAULT)
- User can set as default launcher
- Handles back button (returns to home)
- Handles home button (returns to default state)
- Loads all installed apps from PackageManager
- Listens for app install/uninstall/update broadcasts
- Manages wallpaper (WallpaperManager)
- Handles screen rotation (or locks to portrait — TBD)
- Boot-completed receiver to start on device boot

**Acceptance Criteria:**
- [ ] Can be set as default launcher
- [ ] Home and back buttons work correctly
- [ ] All installed apps are discovered
- [ ] App changes (install/uninstall) reflected in real-time
- [ ] Wallpaper displays behind spatial field
- [ ] Survives process death and recreation

---

## Non-Goals (v1)

- Icon pack support
- Traditional app drawer / grid layout
- Multiple home screen pages
- Traditional Android widgets (replaced by layers + media hub)
- Tablet/foldable optimization
- Landscape orientation

---

## Permissions Required

| Permission | Purpose |
|---|---|
| `QUERY_ALL_PACKAGES` | Discover all installed apps |
| `RECEIVE_BOOT_COMPLETED` | Start on device boot |
| `INTERNET` | Gemma model download (one-time) |
| `READ_EXTERNAL_STORAGE` | Wallpaper access (if needed) |
| `FOREGROUND_SERVICE` | Media control persistence |
| NotificationListenerService | Persistent notifications + app layers |
| UsageStatsPermission | Usage gravity engine data |
| AccessibilityService (optional) | Deep app layer integration |
