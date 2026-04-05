package com.alauncher.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.alauncher.data.db.MediaHistoryDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors active media sessions on the device.
 * Requires NotificationListenerService to be enabled.
 */
@Singleton
class MediaSessionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaHistoryDao: MediaHistoryDao,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastLoggedTrack: String? = null // "title|artist|pkg" to avoid duplicate logs
    private val sessionManager: MediaSessionManager? =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    fun observeMediaState(): Flow<MediaState> = callbackFlow {
        val listenerComponent = ComponentName(
            context, "com.alauncher.notification.NotificationCaptureService"
        )

        // Track registered callbacks so we can unregister them
        val activeCallbacks = mutableMapOf<MediaController, MediaController.Callback>()

        fun emitCurrentState() {
            val state = buildMediaState(getActiveSessions(listenerComponent))
            // Log track to history when it changes
            state.activeSource?.let { source ->
                val trackKey = "${source.title}|${source.artist}|${source.packageName}"
                if (source.title != null && trackKey != lastLoggedTrack && source.isPlaying) {
                    lastLoggedTrack = trackKey
                    scope.launch {
                        mediaHistoryDao.logPlay(
                            title = source.title,
                            artist = source.artist,
                            album = source.album,
                            packageName = source.packageName,
                            durationMs = source.durationMs,
                        )
                    }
                }
            }
            trySend(state)
        }

        fun unregisterAllCallbacks() {
            activeCallbacks.forEach { (controller, callback) ->
                try {
                    controller.unregisterCallback(callback)
                } catch (_: Exception) {}
            }
            activeCallbacks.clear()
        }

        fun registerCallbacksOn(controllers: List<MediaController>?) {
            unregisterAllCallbacks()
            controllers?.forEach { controller ->
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
                        emitCurrentState()
                    }

                    override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                        emitCurrentState()
                    }
                }
                controller.registerCallback(callback)
                activeCallbacks[controller] = callback
            }
        }

        val sessionListener = object : MediaSessionManager.OnActiveSessionsChangedListener {
            override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
                registerCallbacksOn(controllers)
                emitCurrentState()
            }
        }

        try {
            val controllers = getActiveSessions(listenerComponent)
            registerCallbacksOn(controllers)
            emitCurrentState()
            sessionManager?.addOnActiveSessionsChangedListener(sessionListener, listenerComponent)
        } catch (e: SecurityException) {
            trySend(MediaState())
        }

        awaitClose {
            unregisterAllCallbacks()
            try {
                sessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
            } catch (_: Exception) {}
        }
    }

    private fun getActiveSessions(component: ComponentName): List<MediaController>? {
        return try {
            sessionManager?.getActiveSessions(component)
        } catch (e: SecurityException) {
            null
        }
    }

    private fun buildMediaState(controllers: List<MediaController>?): MediaState {
        if (controllers.isNullOrEmpty()) return MediaState()

        val sources = controllers.mapNotNull { controller ->
            val metadata = controller.metadata ?: return@mapNotNull null
            val playbackState = controller.playbackState

            MediaSource(
                packageName = controller.packageName,
                title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE),
                artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
                    ?: metadata.getString(android.media.MediaMetadata.METADATA_KEY_AUTHOR),
                album = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM),
                durationMs = metadata.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION)
                    .takeIf { it > 0 },
                positionMs = playbackState?.position?.takeIf { it >= 0 },
                isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
                controller = controller,
            )
        }

        val activeSource = sources.firstOrNull { it.isPlaying } ?: sources.firstOrNull()

        return MediaState(
            hasMedia = sources.isNotEmpty(),
            activeSource = activeSource,
            allSources = sources,
        )
    }
}

data class MediaState(
    val hasMedia: Boolean = false,
    val activeSource: MediaSource? = null,
    val allSources: List<MediaSource> = emptyList(),
)

data class MediaSource(
    val packageName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val positionMs: Long?,
    val isPlaying: Boolean,
    val controller: MediaController,
) {
    fun play() = controller.transportControls.play()
    fun pause() = controller.transportControls.pause()
    fun skipNext() = controller.transportControls.skipToNext()
    fun skipPrev() = controller.transportControls.skipToPrevious()
    fun togglePlayPause() = if (isPlaying) pause() else play()

    val progress: Float
        get() {
            val dur = durationMs ?: return 0f
            val pos = positionMs ?: return 0f
            return if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
        }

    val remainingMs: Long?
        get() {
            val dur = durationMs ?: return null
            val pos = positionMs ?: return null
            return (dur - pos).coerceAtLeast(0)
        }
}
