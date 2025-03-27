/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.models

import android.net.Uri

data class WebShare(
    val url: String?,
    val text: String?,
    val title: String?,
    val files: List<Uri>
)
