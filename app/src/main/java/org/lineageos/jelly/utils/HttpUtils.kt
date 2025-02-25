/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.reflect.cast

object HttpUtils {
    suspend fun bitmap(url: String, callback: (bitmap: Bitmap?) -> Unit) {
        val bitmap = runCatching {
            val connection = HttpURLConnection::class.cast(
                URL(url).openConnection()
            )
            try {
                connection.connect()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) null
                else connection.inputStream.buffered().use { BitmapFactory.decodeStream(it) }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
        withContext(Dispatchers.Main) {
            callback(bitmap)
        }
    }
}
