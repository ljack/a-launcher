# A-Launcher Architecture

## Module Structure

Single `app` module (monolith to start, extract modules later if needed).

```
app/src/main/java/com/alauncher/
├── ALauncherApp.kt                  # Application class, Hilt entry point
├── MainActivity.kt                  # Launcher activity (CATEGORY_HOME)
├── di/                              # Hilt dependency injection modules
│   ├── AppModule.kt                 # Singletons: database, datastore, repos
│   ├── SearchModule.kt              # Gemma model, search engine
│   └── MediaModule.kt               # MediaSession dependencies
├── data/                            # Data layer
│   ├── db/                          # Room database
│   │   ├── ALauncherDatabase.kt     # Room database definition
│   │   ├── AppEntity.kt             # Installed app record
│   │   ├── UsageEventEntity.kt      # Launch event with timestamp
│   │   ├── AppPositionEntity.kt     # Persisted spatial position
│   │   ├── NotificationEntity.kt    # Persistent notification storage
│   │   ├── AppDao.kt
│   │   ├── UsageDao.kt
│   │   ├── PositionDao.kt
│   │   └── NotificationDao.kt
│   ├── prefs/                       # DataStore preferences
│   │   └── LauncherPrefs.kt         # User settings, calibration data
│   ├── repository/                  # Repository pattern
│   │   ├── AppRepository.kt         # App discovery, install/uninstall
│   │   ├── UsageRepository.kt       # Usage tracking, gravity scores
│   │   ├── PositionRepository.kt    # Spatial position management
│   │   ├── NotificationRepository.kt
│   │   ├── MediaRepository.kt       # Active media sessions
│   │   └── SearchRepository.kt      # Search index, Gemma integration
│   └── model/                       # Domain models
│       ├── LauncherApp.kt           # App with icon, label, gravity score
│       ├── AppPosition.kt           # 2D position + velocity + home region
│       ├── GravityScore.kt          # Usage gravity calculation
│       ├── AppLayer.kt              # In-app content layer
│       ├── PersistentNotification.kt
│       └── MediaState.kt            # Current playback state
├── engine/                          # Core launcher engine
│   ├── GravityEngine.kt             # Usage gravity computation
│   │   ├── Calculates gravity scores from usage events
│   │   ├── Time-of-day weighting
│   │   └── Smooth position interpolation
│   ├── SpatialLayout.kt             # Position assignment algorithm
│   │   ├── Force-directed layout (apps repel each other)
│   │   ├── Gravity pulls high-score apps toward center
│   │   └── Position damping (slow drift, spatial memory)
│   ├── GestureEngine.kt             # Gesture recognition
│   │   ├── Thumb anchor detection
│   │   ├── Orbit/pan gesture
│   │   ├── Pinch-to-zoom
│   │   ├── Launch confirmation gesture
│   │   ├── Flick-up for search
│   │   └── Pause/resume state machine
│   └── LayerEngine.kt               # App layer content resolution
│       ├── ShortcutManager integration
│       ├── NotificationListener content extraction
│       └── Layer content caching
├── search/                          # AI search
│   ├── SearchEngine.kt              # Orchestrates search pipeline
│   ├── FuzzyMatcher.kt              # Fast string matching (fallback)
│   ├── NegativeFilter.kt            # NOT / exclusion logic
│   ├── GemmaService.kt              # On-device Gemma model inference
│   └── SearchIndex.kt               # Pre-built search index of app metadata
├── media/                           # Media control
│   ├── MediaSessionMonitor.kt       # Discovers active MediaSessions
│   ├── MediaController.kt           # Play/pause/skip/source-switch
│   └── MediaNotificationService.kt  # Foreground service for persistence
├── notification/                    # Notification system
│   ├── NotificationCaptureService.kt # NotificationListenerService impl
│   └── NotificationManager.kt       # Storage, grouping, badge counts
├── receiver/                        # Broadcast receivers
│   ├── BootReceiver.kt              # BOOT_COMPLETED
│   ├── PackageChangeReceiver.kt     # App install/uninstall/update
│   └── WallpaperReceiver.kt         # Wallpaper change events
└── ui/                              # Compose UI layer
    ├── theme/                       # Material3 theme
    │   ├── Theme.kt
    │   ├── Color.kt
    │   └── Type.kt
    ├── spatial/                     # Spatial field rendering
    │   ├── SpatialField.kt          # Main canvas composable
    │   ├── AppOrb.kt                # Individual app element
    │   ├── OrbAnimation.kt          # Float/drift/settle animations
    │   ├── SpatialGestures.kt       # Compose gesture handlers
    │   └── ZoomController.kt        # Pinch-to-zoom state
    ├── layers/                      # App layer UI
    │   ├── LayerOverlay.kt          # Layer content display
    │   ├── ChatLayerContent.kt      # WhatsApp/Telegram chat list
    │   ├── BrowserLayerContent.kt   # Chrome tabs
    │   ├── CalendarLayerContent.kt  # Upcoming events
    │   └── MediaLayerContent.kt     # Media app content
    ├── search/                      # Search UI
    │   ├── SearchOverlay.kt         # Search input + results
    │   ├── SearchBar.kt             # Thumb-anchored search input
    │   └── SearchResults.kt         # Results as floating orbs
    ├── media/                       # Media hub UI
    │   ├── MediaHub.kt              # Expandable media controls
    │   ├── MediaIndicator.kt        # Minimal playing indicator
    │   └── SourceSwitcher.kt        # Media source selector
    ├── notification/                # Notification tray UI
    │   ├── NotificationTray.kt      # Persistent notification panel
    │   └── NotificationBadge.kt     # Badge on app orbs
    └── screen/                      # Screen-level composables
        ├── HomeScreen.kt            # Main launcher screen
        └── HomeViewModel.kt         # State management for home
```

## Data Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        UI Layer (Compose)                       │
│                                                                 │
│  HomeScreen ←── HomeViewModel ←── Repositories ←── Room/DS     │
│      │              │                                           │
│  SpatialField    GestureEngine                                  │
│      │              │                                           │
│  AppOrbs ←── SpatialLayout ←── GravityEngine ←── UsageRepo     │
│      │                                                          │
│  LayerOverlay ←── LayerEngine ←── Shortcuts + Notifications    │
│      │                                                          │
│  SearchOverlay ←── SearchEngine ←── FuzzyMatcher + Gemma       │
│      │                                                          │
│  MediaHub ←── MediaSessionMonitor ←── Android MediaSession     │
│      │                                                          │
│  NotifTray ←── NotificationManager ←── NotifCaptureService     │
└─────────────────────────────────────────────────────────────────┘
```

## Data Models

```kotlin
// Core app representation
data class LauncherApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: ImageBitmap,       // Cached app icon
    val gravityScore: Float,     // 0.0 - 1.0, computed by GravityEngine
    val position: AppPosition,   // Current spatial position
    val notificationCount: Int,  // Badge count
    val hasLayer: Boolean,       // Whether deep nav is available
)

// Spatial position with physics
data class AppPosition(
    val x: Float,                // Current X in spatial field
    val y: Float,                // Current Y in spatial field
    val homeX: Float,            // "Home" X (where it settles)
    val homeY: Float,            // "Home" Y
    val velocityX: Float = 0f,   // Current drift velocity
    val velocityY: Float = 0f,
    val zLayer: Int = 0,         // Depth layer (0 = front, higher = further)
)

// Usage gravity calculation
data class GravityScore(
    val packageName: String,
    val frequency: Float,        // Normalized launch count
    val recency: Float,          // Decay-weighted recency
    val timeOfDayWeight: Float,  // Contextual time relevance
    val composite: Float,        // Final weighted score
)

// Room entity for usage tracking
@Entity(tableName = "usage_events")
data class UsageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestamp: Long,         // System.currentTimeMillis()
    val hourOfDay: Int,          // 0-23 for time-of-day patterns
    val dayOfWeek: Int,          // 1-7
    val sessionDurationMs: Long? = null,
)

// Room entity for persisted positions
@Entity(tableName = "app_positions")
data class AppPositionEntity(
    @PrimaryKey val packageName: String,
    val x: Float,
    val y: Float,
    val homeX: Float,
    val homeY: Float,
    val zLayer: Int,
    val lastUpdated: Long,
)

// Room entity for persistent notifications
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long,
    val dismissed: Boolean = false,
    val pinned: Boolean = false,
    val contentIntent: String?,  // Serialized PendingIntent key
)

// App layer content (not persisted, runtime only)
sealed class AppLayer {
    data class Chats(val items: List<ChatItem>) : AppLayer()
    data class Tabs(val items: List<TabItem>) : AppLayer()
    data class Events(val items: List<EventItem>) : AppLayer()
    data class Media(val item: MediaItem) : AppLayer()
    data class Shortcuts(val items: List<ShortcutItem>) : AppLayer()
}

// Media playback state
data class MediaState(
    val isPlaying: Boolean,
    val title: String?,
    val artist: String?,         // Or author for audiobooks
    val albumArt: ImageBitmap?,
    val progress: Float,         // 0.0 - 1.0
    val remainingMs: Long?,      // For Audible
    val sourcePackage: String,
    val sourceLabel: String,
)
```

## Technology Stack

| Component | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.1.x |
| UI | Jetpack Compose | BOM 2025.03.00+ |
| Design | Material 3 | Latest |
| DI | Hilt | 2.56+ |
| Database | Room | 2.7+ |
| Preferences | DataStore | 1.1+ |
| Image loading | Coil | 3.x |
| Async | Coroutines + Flow | 1.10+ |
| AI/ML | Google AI Edge (Gemma) | Latest |
| Animation | Compose Animation | (from BOM) |
| Graphics | Compose Canvas | (from BOM) |

## Build Configuration

- **AGP**: 8.9.x (latest stable)
- **Target SDK**: 35 (Android 15)
- **Min SDK**: 35 (latest only)
- **Compile SDK**: 35
- **Gradle**: 8.12+
- **Java target**: 17
- **Compose compiler**: Kotlin 2.1 built-in

## Gradle Version Catalog (`gradle/libs.versions.toml`)

```toml
[versions]
agp = "8.9.1"
kotlin = "2.1.10"
compose-bom = "2025.03.00"
hilt = "2.56"
room = "2.7.1"
datastore = "1.1.4"
coil = "3.1.0"
coroutines = "1.10.1"
lifecycle = "2.9.0"
navigation = "2.9.0"
ksp = "2.1.10-1.0.31"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
compose-runtime = { group = "androidx.compose.runtime", name = "runtime" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.10.1" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.16.0" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room = { id = "androidx.room", version.ref = "room" }
```

## Manifest Configuration

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".ALauncherApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.ALauncher">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:stateNotNeeded="true"
            android:screenOrientation="portrait"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <!-- Notification Listener -->
        <service
            android:name=".notification.NotificationCaptureService"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            android:exported="false">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>

        <!-- Boot Receiver -->
        <receiver
            android:name=".receiver.BootReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

        <!-- Package Change Receiver -->
        <receiver
            android:name=".receiver.PackageChangeReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.PACKAGE_ADDED" />
                <action android:name="android.intent.action.PACKAGE_REMOVED" />
                <action android:name="android.intent.action.PACKAGE_REPLACED" />
                <data android:scheme="package" />
            </intent-filter>
        </receiver>

    </application>
</manifest>
```

## Key Design Decisions

### 1. Force-Directed Spatial Layout
Apps are positioned using a force-directed graph algorithm:
- **Gravity force**: Pulls high-score apps toward center
- **Repulsion force**: Apps push each other apart (no overlap)
- **Damping**: High damping factor (0.95+) means positions change very slowly
- **Home anchor**: Each app has a "home position" it gravitates back to

This creates the organic, living feel while maintaining spatial memory.

### 2. Canvas-Based Rendering
The spatial field uses Compose `Canvas` rather than standard layout composables:
- Better performance for 200+ moving elements
- Custom hit-testing for thumb-anchored interaction
- Direct control over draw order (z-layers)
- Efficient dirty-rect rendering

### 3. State Machine for Gestures
```
IDLE → TOUCHING → ORBITING → ZOOMING → LAUNCHING
  ↑        ↓           ↓          ↓
  └── PAUSED ←──────────┘──────────┘
       (5s timeout → IDLE)
```

### 4. Layered Architecture
```
UI (Compose) → ViewModel → Repository → Data Source (Room/API/System)
```
- ViewModels expose StateFlow
- Repositories abstract data sources
- Room for persistence, DataStore for preferences
- System services accessed via repository wrappers

### 5. On-Device AI
- Gemma model loaded lazily on first search
- Inference runs on background thread via coroutine
- Model cached in app internal storage
- Fallback to fuzzy string matching if model unavailable
