/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ext

import android.content.Intent
import android.net.Uri

fun buildShareIntent(vararg uris: Uri) = Intent().apply {
    when (uris.size) {
        0 -> {
            action = Intent.ACTION_SEND
            type = "text/plain"
        }

        else -> {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "*/*"
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                uris.toCollection(ArrayList()),
            )
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }
}
