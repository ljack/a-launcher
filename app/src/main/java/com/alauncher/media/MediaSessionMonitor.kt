package com.alauncher.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors active media sessions on the device.
 * Requires NotificationListenerService to be enabled.
 */
@Singleton
class MediaSessionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sessionManager: MediaSessionManager? =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    /**
     * Observe active media sessions as [MediaState] updates.
     * Falls back to empty state if notification listener permission not granted.
     */
    fun observeMediaState(): Flow<MediaState> = callbackFlow {
        val listenerComponent = ComponentName(context, "com.alauncher.notification.NotificationCaptureService")

        val listener = object : MediaSessionManager.OnActiveSessionsChangedListener {
            override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
                val state = buildMediaState(controllers)
                trySend(state)
                // Register per-controller callbacks for playback changes
                controllers?.forEach { controller ->
                    controller.registerCallback(object : MediaController.Callback() {
                        override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
                            val updated = buildMediaState(getActiveSessions(listenerComponent))
                            trySend(updated)
                        }

                        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                            val updated = buildMediaState(getActiveSessions(listenerComponent))
                            trySend(updated)
                        }
                    })
                }
            }
        }

        // Initial state
        try {
            val controllers = getActiveSessions(listenerComponent)
            trySend(buildMediaState(controllers))

            sessionManager?.addOnActiveSessionsChangedListener(listener, listenerComponent)

            // Also register callbacks on initial controllers
            controllers?.forEach { controller ->
                controller.registerCallback(object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(playbackState: PlaybackState?) {
                        val updated = buildMediaState(getActiveSessions(listenerComponent))
                        trySend(updated)
                    }

                    override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                        val updated = buildMediaState(getActiveSessions(listenerComponent))
                        trySend(updated)
                    }
                })
            }
        } catch (e: SecurityException) {
            // NotificationListener not enabled — emit empty state
            trySend(MediaState())
        }

        awaitClose {
            try {
                sessionManager?.removeOnActiveSessionsChangedListener(listener)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()

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

/**
 * Represents current media playback state across all sources.
 */
data class MediaState(
    val hasMedia: Boolean = false,
    val activeSource: MediaSource? = null,
    val allSources: List<MediaSource> = emptyList(),
)

/**
 * Represents a single media source (app).
 */
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
