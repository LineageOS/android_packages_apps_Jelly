/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.js

import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.lineageos.jelly.models.MediaSessionMetadata
import org.lineageos.jelly.utils.HttpUtils
import org.lineageos.jelly.webview.WebViewExt

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
    fun onCreate(title: String, artist: String, album: String?, artwork: String?) {
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
            service().onMediaSessionEvent(it, true)
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
    fun onPlaying(currentTime: Float, duration: Float) {
        metadata?.let {
            it.currentTime = toMs(currentTime)
            it.duration = toMs(duration)
            it.isPlaying = true
            it.isBuffering = false
            service().onMediaSessionEvent(it)
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
            ACTION_PREV_TRACK -> {
                prevTrackAction = handler
                metadata?.let {
                    it.prevTrackAction = prevTrackAction
                    service().onMediaSessionEvent(it)
                }
            }
            ACTION_NEXT_TRACK -> {
                nextTrackAction = handler
                metadata?.let {
                    it.nextTrackAction = nextTrackAction
                    service().onMediaSessionEvent(it)
                }
            }
        }
    }

    @JavascriptInterface
    fun onMetadataUpdate(title: String, artist: String, album: String?, artwork: String?) {
        metadata?.let {
            it.title = title
            it.artist = artist
            it.album = album
            service().onMediaSessionEvent(it)
            getArtwork(artwork)
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
            HttpUtils.bitmap(artwork) { callback(it) }
        }
    }

    private fun shortcut() = webView.backgroundShortcut!!
    private fun service() = webView.backgroundShortcutService!!
    private fun toMs(value: Float) = (value * 1000F).toLong()

    companion object {
        const val INTERFACE = "JsMediaSession"

        const val ACTION_PREV_TRACK = "previoustrack"
        const val ACTION_NEXT_TRACK = "nexttrack"

        const val PLAY_ACTION = "_JsMediaSessionPlayAction"
        const val PAUSE_ACTION = "_JsMediaSessionPauseAction"
        const val SEEK_TO_ACTION = "_JsMediaSessionSeekToAction"
        const val PREV_TRACK_ACTION = "_JsMediaSessionPrevTrackAction"
        const val NEXT_TRACK_ACTION = "_JsMediaSessionNextTrackAction"
        const val DESTROY_ACTION = "_JsMediaSessionDestroyAction"

        private const val ID = "MediaId"
        private const val MEDIA = "MediaElement"
        private const val ACTIONS = "MediaActions"
        private const val MONKEY_PATCH_ONCE_KEY = "JsMediaSessionMonkeyPatch"
        const val SCRIPT = """
            (() => {
                if (window.$MONKEY_PATCH_ONCE_KEY || !$INTERFACE.isBackgroundService()) return;

                window.$MONKEY_PATCH_ONCE_KEY = true;
                let $ID = 0;
                let $MEDIA = null;
                let $ACTIONS = new Map();

                const setActionHandler = navigator.mediaSession.setActionHandler;
                navigator.mediaSession.setActionHandler = function (action, handler) {
                    $INTERFACE.onSetActionHandler(action, !!handler);
                    $ACTIONS.set(action, handler);
                    setActionHandler.apply(this, [action, handler]);
                };

                const artwork = (items) => {
                    let src = null;
                    let minWidth = 99;
                    const maxWidth = 999;
                    items.forEach((item) => {
                        if (!item.sizes) return;
                        const width = Number(item.sizes.split('x')[0]);
                        if (width >= minWidth && width <= maxWidth) {
                            minWidth = width;
                            src = item.src;
                        }
                    });
                    return src;
                };

                const metadataArgs = (metadata) => [
                    metadata?.title ?? document.title,
                    metadata?.artist ?? location.hostname,
                    metadata?.album ?? null,
                    artwork(metadata?.artwork ?? []),
                ];

                const metadataProxy = new Proxy({ metadata: navigator.mediaSession.metadata }, {
                    set: function(target, property, value) {
                        if (property === 'metadata' && target.metadata !== value) {
                            target.metadata = value;
                            if (value) $INTERFACE.onMetadataUpdate(...metadataArgs(value));
                        }
                        return true;
                    }
                });

                Object.defineProperty(navigator.mediaSession, 'metadata', {
                    set: function(value) {
                        metadataProxy.metadata = value;
                    },
                    get: function() {
                        return metadataProxy.metadata;
                    }
                });

                const duration = (media) => (
                    !Number.isNaN(media.duration) &&
                    media.duration !== Infinity
                ) ? media.duration : 0;

                const onCreate = (media, event) => {
                    if ($MEDIA && $MEDIA.$ID === media.$ID) {
                        if (event === 'play') $INTERFACE.onPlay();
                        return;
                    }
                    if (media.muted || media.paused) return;
                    media.onwaiting = () => {
                        $INTERFACE.onBuffering();
                    };
                    media.onplaying = () => {
                        $INTERFACE.onPlaying(media.currentTime, duration(media));
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
                    media.onemptied = () => {
                        if (!navigator.mediaSession.metadata) $DESTROY_ACTION();
                    };
                    $PAUSE_ACTION(false);
                    $MEDIA = media;
                    $INTERFACE.onCreate(...metadataArgs(navigator.mediaSession.metadata));
                };

                const play = HTMLMediaElement.prototype.play;
                HTMLMediaElement.prototype.play = function (...args) {
                    const media = this;
                    if (!media.$ID) media.$ID = ++$ID;
                    media.onplay = () => onCreate(media, 'play');
                    media.onvolumechange = () => onCreate(media, 'volumechange');
                    play.apply(media, args);
                };

                window.$PLAY_ACTION = () => {
                    $MEDIA?.play();
                };
                window.$PAUSE_ACTION = (emitEvent = true) => {
                    if (!$MEDIA) return;
                    if (!emitEvent) $MEDIA.onpause = null;
                    $MEDIA.pause();
                };
                window.$SEEK_TO_ACTION = (currentTime) => {
                    if (!$MEDIA) return;
                    $MEDIA.currentTime = currentTime;
                };
                window.$PREV_TRACK_ACTION = () => {
                    const prevTrack = $ACTIONS?.get('$ACTION_PREV_TRACK');
                    if ($MEDIA && duration($MEDIA) && ($MEDIA.currentTime > 4 || !prevTrack)) {
                        $SEEK_TO_ACTION(0);
                        return;
                    }
                    if (prevTrack) prevTrack();
                };
                window.$NEXT_TRACK_ACTION = () => {
                    const nextTrack = $ACTIONS?.get('$ACTION_NEXT_TRACK');
                    if (nextTrack) nextTrack();
                };
                window.$DESTROY_ACTION = () => {
                    $PAUSE_ACTION(false);
                    $MEDIA = null;
                    $INTERFACE.onDestroy();
                };
            })();
        """
    }
}
