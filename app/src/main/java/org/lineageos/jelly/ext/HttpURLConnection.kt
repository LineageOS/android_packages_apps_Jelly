/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ext

import java.net.HttpURLConnection
import java.nio.charset.Charset
import java.util.Locale

fun HttpURLConnection.getCharset(defaultEncoding: String) = contentEncoding?.let {
    return Charset.forName(it)
} ?: contentType.split(";").toTypedArray().map { str ->
    str.trim { it <= ' ' }
}.firstOrNull {
    it.lowercase(Locale.US).startsWith("charset=")
}?.let {
    Charset.forName(it.substring(8))
} ?: Charset.forName(defaultEncoding)
