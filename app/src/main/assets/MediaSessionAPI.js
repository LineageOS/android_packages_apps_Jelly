/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

(() => {
    /**
     * MediaSession API Implementation based on official documents
     * @see https://developer.mozilla.org/en-US/docs/Web/API/Media_Session_API
     */
    if (!window.MediaMetadata) {
        /**
         * @see https://developer.mozilla.org/en-US/docs/Web/API/MediaMetadata
         */
        class MediaMetadata {
            constructor({ title = '', artist = '', album = '', artwork = [] } = {}) {
                this.title = title;
                this.artist = artist;
                this.album = album;
                this.artwork = artwork;
            }
        }
        window.MediaMetadata = MediaMetadata;
    }

    if (!window.MediaSession) {
        /**
         * @see https://developer.mozilla.org/en-US/docs/Web/API/MediaSession
         */
        class MediaSession {
            constructor() {
                this.metadata = null;
                this.playbackState = 'none';
            }

            setActionHandler(type, callback) {
            }

            setCameraActive(active) {
            }

            setMicrophoneActive(active) {
            }

            setPositionState(stateDict) {
            }
        }
        window.MediaSession = MediaSession;
    }

    if (!navigator.mediaSession) {
        navigator.mediaSession = new MediaSession();
    }
})();
