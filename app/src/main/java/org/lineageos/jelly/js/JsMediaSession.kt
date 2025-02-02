/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.js

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.jelly.models.MediaSessionMetadata
import org.lineageos.jelly.webview.WebViewExt
import java.net.HttpURLConnection
import java.net.URL
import kotlin.reflect.cast

@Keep
class JsMediaSession(private val webView: WebViewExt) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var metadata: MediaSessionMetadata? = null
    private var artworkJob: Job? = null

    private var prevTrackAction: Boolean = false
    private var nextTrackAction: Boolean = false

    @JavascriptInterface
    fun isBackgroundService(): Boolean = webView.backgroundShortcutService != null

    @JavascriptInterface
    fun onCreate(title: String, artist: String, album: String, artwork: String?) {
        metadata = MediaSessionMetadata(
            shortcut().id,
            title,
            artist,
            album,
            prevTrackAction,
            nextTrackAction,
        ).apply { isPlaying = true }
        service().onMediaSessionEvent(metadata!!, true)
        getArtwork(artwork)
    }

    @JavascriptInterface
    fun onDestroy() {
        service().onMediaSessionDestroy(shortcut().id)
        metadata = null
    }

    @JavascriptInterface
    fun onPlay() {
        metadata?.let {
            it.isPlaying = true
            service().onMediaSessionEvent(it)
        }
    }

    @JavascriptInterface
    fun onBuffering() {
        metadata?.let {
            it.isBuffering = true
            service().onMediaSessionEvent(it)
        }
    }

    @JavascriptInterface
    fun onPlaying(
        title: String,
        artist: String,
        album: String,
        artwork: String?,
        currentTime: Float,
        duration: Float
    ) {
        metadata?.let {
            it.title = title
            it.artist = artist
            it.album = album
            it.currentTime = toMs(currentTime)
            it.duration = toMs(duration)
            it.isPlaying = true
            it.isBuffering = false
            service().onMediaSessionEvent(it)
            getArtwork(artwork)
        }
    }

    @JavascriptInterface
    fun onRateChange(currentTime: Float, playbackRate: Float) {
        metadata?.let {
            it.currentTime = toMs(currentTime)
            it.playbackRate = playbackRate
            service().onMediaSessionEvent(it)
        }
    }

    @JavascriptInterface
    fun onSeekTo(currentTime: Float) {
        metadata?.let {
            it.currentTime = toMs(currentTime)
            service().onMediaSessionEvent(it)
        }
    }

    @JavascriptInterface
    fun onPause(currentTime: Float) {
        metadata?.let {
            it.currentTime = toMs(currentTime)
            it.isPlaying = false
            service().onMediaSessionEvent(it)
        }
    }

    @JavascriptInterface
    fun onSetActionHandler(action: String, handler: Boolean) {
        when (action) {
            "previoustrack" -> {
                prevTrackAction = handler
                metadata?.let {
                    it.prevTrackAction = prevTrackAction
                    service().onMediaSessionEvent(it)
                }
            }
            "nexttrack" -> {
                nextTrackAction = handler
                metadata?.let {
                    it.nextTrackAction = nextTrackAction
                    service().onMediaSessionEvent(it)
                }
            }
        }
    }

    private fun getArtwork(artwork: String?) {
        metadata?.let {
            if (it.artwork == artwork) return
            it.artwork = artwork
            resolveArtwork(artwork) { bitmap ->
                it.cover = bitmap
                service().onMediaSessionEvent(it)
            }
        }
    }

    private fun resolveArtwork(artwork: String?, callback: (bitmap: Bitmap?) -> Unit) {
        artworkJob?.cancel()
        if (artwork == null) {
            callback(null)
            return
        }
        artworkJob = scope.launch {
            val bitmap = runCatching {
                val connection = HttpURLConnection::class.cast(
                    URL(artwork).openConnection()
                )
                connection.connect()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) null
                else connection.inputStream.buffered().use {
                    BitmapFactory.decodeStream(it)
                }
            }.getOrNull()
            withContext(Dispatchers.Main) {
                callback(bitmap)
            }
        }
    }

    private fun shortcut() = webView.backgroundShortcut!!
    private fun service() = webView.backgroundShortcutService!!
    private fun toMs(value: Float) = (value * 1000F).toLong()

    companion object {
        const val INTERFACE = "JsMediaSession"

        const val PLAY_ACTION = "_JsMediaSessionPlayAction"
        const val PAUSE_ACTION = "_JsMediaSessionPauseAction"
        const val SEEK_TO_ACTION = "_JsMediaSessionSeekToAction"
        const val PREV_TRACK_ACTION = "_JsMediaSessionPrevTrackAction"
        const val NEXT_TRACK_ACTION = "_JsMediaSessionNextTrackAction"
        const val DESTROY_ACTION = "_JsMediaSessionDestroyAction"

        private const val MEDIA = "JsMediaSessionMediaElement"
        private const val ACTIONS = "JsMediaSessionActions"
        private const val MONKEY_PATCH_ONCE_KEY = "JsMediaSessionMonkeyPatch"
        const val SCRIPT = """
            (() => {
                if (window.$MONKEY_PATCH_ONCE_KEY || !$INTERFACE.isBackgroundService()) return;

                window.$MONKEY_PATCH_ONCE_KEY = true;
                let $MEDIA = null;
                let $ACTIONS = null;

                const setActionHandler = navigator.mediaSession.setActionHandler;
                navigator.mediaSession.setActionHandler = function (action, handler) {
                    $INTERFACE.onSetActionHandler(action, !!handler);
                    if (handler) {
                        if (!$ACTIONS) $ACTIONS = new Map();
                        $ACTIONS.set(action, handler);
                    } else if (action === 'play') {
                        $ACTIONS = null;
                        $INTERFACE.onDestroy();
                    }
                    setActionHandler.apply(this, [action, handler]);
                };

                const artwork = () => {
                    let src = null;
                    let minWidth = 99;
                    const maxWidth = 999;
                    (navigator.mediaSession.metadata?.artwork ?? []).forEach((item) => {
                        if (!item.sizes) return;
                        const width = Number(item.sizes.split('x')[0]);
                        if (width >= minWidth && width <= maxWidth) {
                            minWidth = width;
                            src = item.src;
                        }
                    });
                    return src;
                };

                const metadataArgs = (extra = []) => {
                    const metadata = navigator.mediaSession.metadata;
                    return [
                        metadata.title,
                        metadata.artist,
                        metadata.album,
                        artwork(),
                        ...extra,
                    ];
                };

                const onCreate = (media) => {
                    const metadata = navigator.mediaSession.metadata;
                    if (!metadata || media.muted || media.paused) return;
                    media.onwaiting = () => {
                        $INTERFACE.onBuffering();
                    };
                    media.onplaying = () => {
                        const duration = (
                            !Number.isNaN(media.duration) &&
                            media.duration !== Infinity
                        ) ? media.duration : 0;
                        $INTERFACE.onPlaying(...metadataArgs([media.currentTime, duration]));
                    };
                    media.onratechange = () => {
                        $INTERFACE.onRateChange(media.currentTime, media.playbackRate);
                    };
                    media.onseeked = () => {
                        $INTERFACE.onSeekTo(media.currentTime);
                    };
                    media.onpause = () => {
                        $INTERFACE.onPause(media.currentTime);
                    };
                    media.onended = () => {
                        $INTERFACE.onPause(media.currentTime);
                    };
                    $MEDIA = media;
                    $INTERFACE.onCreate(...metadataArgs());
                };

                const play = HTMLMediaElement.prototype.play;
                HTMLMediaElement.prototype.play = function (...args) {
                    const media = this;
                    media.onplay = () => {
                        if ($MEDIA?.src === media.src) {
                            $INTERFACE.onPlay(...metadataArgs());
                            return;
                        }
                        onCreate(media);
                    };
                    media.onvolumechange = () => {
                        if ($MEDIA?.src === media.src) return;
                        onCreate(media);
                    };
                    play.apply(media, args);
                };

                window.$PLAY_ACTION = () => {
                    $MEDIA?.play();
                };
                window.$PAUSE_ACTION = () => {
                    $MEDIA?.pause();
                };
                window.$SEEK_TO_ACTION = (currentTime) => {
                    if (!$MEDIA) return;
                    $MEDIA.currentTime = currentTime;
                };
                window.$PREV_TRACK_ACTION = () => {
                    const prevTrack = $ACTIONS?.get('previoustrack');
                    if (prevTrack) prevTrack();
                };
                window.$NEXT_TRACK_ACTION = () => {
                    const nextTrack = $ACTIONS?.get('nexttrack');
                    if (nextTrack) nextTrack();
                };
                window.$DESTROY_ACTION = () => {
                    $PAUSE_ACTION();
                    $MEDIA = null;
                    $INTERFACE.onDestroy();
                };
            })();
        """
    }
}
