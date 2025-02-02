/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.models

import android.graphics.Bitmap

data class MediaSessionMetadata(
    var id: String? = null,
    var title: String? = null,
    var artist: String? = null,
    var album: String? = null,
    var prevTrackAction: Boolean = false,
    var nextTrackAction: Boolean = false,
    var isPlaying: Boolean = false,
    var isBuffering: Boolean = false,
    var currentTime: Long = 0L,
    var duration: Long = 0L,
    var playbackRate: Float = 1.0F,
    var artwork: String? = null,
    var cover: Bitmap? = null
)
