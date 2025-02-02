/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.models

import android.graphics.Bitmap

data class MediaSessionMetadata(
    var id: String = "",
    var title: String = "",
    var artist: String = "",
    var album: String = "",
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
