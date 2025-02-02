/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.shortcut

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.view.ContextThemeWrapper
import org.lineageos.jelly.R
import org.lineageos.jelly.js.JsMediaSession
import org.lineageos.jelly.models.MediaSessionMetadata
import org.lineageos.jelly.webview.WebViewExt

class BackgroundShortcutService : Service() {

    private val binder: ServiceBinder = ServiceBinder()
    private val shortcuts: MutableMap<String, WebViewExt> = mutableMapOf()
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    private var mediaSession: MediaSession? = null
    private var mediaSessionMetadata: MediaSessionMetadata? = null
    private val mediaSessionCallback = object : MediaSession.Callback() {
        override fun onPlay() {
            mediaSessionAction("${JsMediaSession.PLAY_ACTION}()")
        }

        override fun onPause() {
            mediaSessionAction("${JsMediaSession.PAUSE_ACTION}()")
        }

        override fun onSeekTo(pos: Long) {
            val position = pos / 1000F
            mediaSessionAction("${JsMediaSession.SEEK_TO_ACTION}($position)")
        }

        override fun onSkipToPrevious() {
            mediaSessionAction("${JsMediaSession.PREV_TRACK_ACTION}()")
        }

        override fun onSkipToNext() {
            mediaSessionAction("${JsMediaSession.NEXT_TRACK_ACTION}()")
        }

        override fun onCustomAction(action: String, extras: Bundle?) {
            if (action != DESTROY_MEDIA_SESSION) return
            mediaSessionAction("${JsMediaSession.DESTROY_ACTION}()")
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START_FOREGROUND_ACTION -> startForegroundService()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        destroyMediaSession()
        shortcuts.values.forEach { it.destroy() }
        shortcuts.clear()
        super.onDestroy()
    }

    fun getRunning(): Set<String> = shortcuts.keys.toSet()

    fun getWebView(backgroundShortcut: BackgroundShortcut) = shortcuts.getOrPut(
        backgroundShortcut.id
    ) {
        val themeContext = ContextThemeWrapper(this, R.style.Theme_Jelly)
        WebViewExt.newInstance(
            themeContext,
            backgroundShortcut,
            this
        )
    }.also {
        updateNotification()
    }

    fun destroyWebView(id: String) {
        if (!shortcuts.containsKey(id)) return
        shortcuts[id]!!.destroy()
        shortcuts.remove(id)
        if (mediaSessionMetadata?.id == id) destroyMediaSession()
        if (shortcuts.isEmpty()) stopForeground(STOP_FOREGROUND_REMOVE)
        else updateNotification()
    }

    fun onMediaSessionEvent(metadata: MediaSessionMetadata, init: Boolean = false) {
        if (!init && mediaSessionMetadata?.id != metadata.id) return
        mediaSessionMetadata = metadata
        mediaSession = buildMediaSession()
        updateNotification()
    }

    fun onMediaSessionDestroy(id: String) {
        if (mediaSessionMetadata?.id != id) return
        destroyMediaSession()
        updateNotification()
    }

    private fun mediaSessionAction(script: String) {
        mediaSessionMetadata?.let { metadata ->
            shortcuts[metadata.id]?.let { webView ->
                webView.post {
                    webView.evaluateJavascript(script, null)
                }
            }
        }
    }

    private fun destroyMediaSession() {
        mediaSession?.release()
        mediaSession = null
        mediaSessionMetadata = null
    }

    private fun startForegroundService() {
        createNotificationChannel()
        startForeground(SERVICE_NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        val channelName = getString(R.string.background_shortcuts_title)
        val channelDescription = getString(R.string.background_shortcuts_notification_channel)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = channelDescription }
        notificationManager.createNotificationChannel(channel)
    }

    private fun notificationTitle() = getString(R.string.background_shortcuts_title)

    private fun notificationContent() = when (shortcuts.size) {
        1 -> getString(R.string.background_shortcuts_notification_content_single)
        else -> getString(R.string.background_shortcuts_notification_content_multiple)
    }

    private fun notificationIntent() = PendingIntent.getActivity(
        this,
        ACTIVITY_REQUEST_CODE,
        Intent(this, BackgroundShortcutActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )

    private fun buildMediaMetadata(): MediaMetadata {
        val metadata = mediaSessionMetadata!!
        return MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, metadata.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, metadata.artist)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, metadata.duration)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, metadata.album)
            .apply {
                metadata.cover?.let {
                    putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
                }
            }.build()
    }

    private fun buildPlaybackState(): PlaybackState {
        val metadata = mediaSessionMetadata!!
        val state = if (metadata.isPlaying && metadata.isBuffering) {
            PlaybackState.STATE_BUFFERING
        } else if (metadata.isPlaying) {
            PlaybackState.STATE_PLAYING
        } else {
            PlaybackState.STATE_PAUSED
        }
        var actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE
        if (metadata.duration > 0L || metadata.prevTrackAction) {
            actions = actions or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        }
        if (metadata.duration > 0L) actions = actions or PlaybackState.ACTION_SEEK_TO
        if (metadata.nextTrackAction) actions = actions or PlaybackState.ACTION_SKIP_TO_NEXT
        val destroyActionName = getString(R.string.media_session_destroy_action)
        val destroyAction = PlaybackState.CustomAction
            .Builder(DESTROY_MEDIA_SESSION, destroyActionName, R.drawable.ic_cancel)
            .build()
        return PlaybackState.Builder()
            .setState(state, metadata.currentTime, metadata.playbackRate)
            .setActions(actions)
            .addCustomAction(destroyAction)
            .build()
    }

    private fun buildMediaSession() = (
            mediaSession ?: MediaSession(this, MEDIA_SESSION_TAG)
    ).apply {
        isActive = true
        setMetadata(buildMediaMetadata())
        setPlaybackState(buildPlaybackState())
        setCallback(mediaSessionCallback)
    }.also {
        mediaSession = it
    }

    private fun buildNotification() = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_external)
        .setContentIntent(notificationIntent())
        .setContentTitle(notificationTitle())
        .setContentText("${shortcuts.size} ${notificationContent()}")
        .apply {
            mediaSession?.let {
                setStyle(Notification.MediaStyle().setMediaSession(it.sessionToken))
            }
        }.build()

    private fun updateNotification() {
        val notification = buildNotification()
        notificationManager.notify(SERVICE_NOTIFICATION_ID, notification)
    }

    inner class ServiceBinder : Binder() {
        fun getService(): BackgroundShortcutService = this@BackgroundShortcutService
    }

    companion object {
        const val START_FOREGROUND_ACTION = "start_foreground_action"
        private const val DESTROY_MEDIA_SESSION = "destroy_media_session"
        private const val MEDIA_SESSION_TAG = "media_session_tag"

        private const val NOTIFICATION_CHANNEL_ID = "background_shortcut_channel"
        private const val SERVICE_NOTIFICATION_ID = 1
        private const val ACTIVITY_REQUEST_CODE = 0
    }
}
