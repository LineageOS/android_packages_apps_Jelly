/*
 * SPDX-FileCopyrightText: 2020-2023 The LineageOS Project
 * SPDX-FileCopyrightText: 2010 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.utils

import android.net.Uri
import android.util.Patterns
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import java.util.Locale
import java.util.regex.Pattern

object UrlUtils {
    val ACCEPTED_URI_SCHEMA: Pattern = Pattern.compile(
        "(?i)" +  // switch on case insensitive matching
                "(" +  // begin group for schema
                "(?:http|https|content|file|chrome)://" +
                "|(?:inline|data|about|javascript):" +
                ")" +
                "(.*)"
    )

    /**
     * Attempts to determine whether user input is a URL or search
     * terms.  Anything with a space is passed to search if canBeSearch is true.
     *
     *
     * Converts to lowercase any mistakenly uppercased schema (i.e.,
     * "Http://" converts to "http://"
     *
     * @return Original or modified URL
     */
    fun smartUrlFilter(url: String): String? {
        var inUrl = url.trim { it <= ' ' }
        val hasSpace = inUrl.indexOf(' ') != -1
        val matcher = ACCEPTED_URI_SCHEMA.matcher(inUrl)
        if (matcher.matches()) {
            // force scheme to lowercase
            val scheme = matcher.group(1)
            val lcScheme = scheme!!.lowercase(Locale.getDefault())
            if (lcScheme != scheme) {
                inUrl = lcScheme + matcher.group(2)
            }
            if (hasSpace && Patterns.WEB_URL.matcher(inUrl).matches()) {
                inUrl = inUrl.replace(" ", "%20")
            }
            return inUrl
        }
        return if (!hasSpace && Patterns.WEB_URL.matcher(inUrl).matches()) {
            URLUtil.guessUrl(inUrl)
        } else null
    }

    /**
     * Formats a launch-able uri out of the template uri by replacing the template parameters with
     * actual values.
     */
    fun getFormattedUri(templateUri: String?, query: String?) =
        URLUtil.composeSearchUrl(query, templateUri, "{searchTerms}")!!

    /** Regex used to parse content-disposition headers */
    private val CONTENT_DISPOSITION_PATTERN = Pattern.compile(
        "attachment;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1\\s*$",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Parse the Content-Disposition HTTP Header. The format of the header
     * is defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html
     * This header provides a filename for content that is going to be
     * downloaded to the file system. We only support the attachment type.
     * Note that RFC 2616 specifies the filename value must be double-quoted.
     * Unfortunately some servers do not quote the value so to maintain
     * consistent behaviour with other browsers, we allow unquoted values too.
     */
    private fun parseContentDisposition(contentDisposition: String) = runCatching {
        val m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition)
        if (m.find()) {
            return@runCatching m.group(2)
        }

        return@runCatching null
    }.getOrNull()

    /**
     * Guesses canonical filename that a download would have, using
     * the URL and contentDisposition. File extension, if not defined,
     * is added based on the mimetype
     * @param url Url to the content
     * @param contentDisposition Content-Disposition HTTP header or {@code null}
     * @param mimeType Mime-type of the content or {@code null}
     *
     * @return suggested filename
     */
    fun guessFileName(
        url: String?,
        contentDisposition: String?,
        mimeType: String?
    ): String {
        var tempFilename: String? = null
        var extension: String? = null

        // If we couldn't do anything with the hint, move toward the content disposition
        contentDisposition?.let { disposition ->
            tempFilename = parseContentDisposition(disposition)
            tempFilename?.let {
                val index = it.lastIndexOf('/') + 1
                if (index > 0) {
                    tempFilename = it.substring(index)
                }
            }
        }

        // If all the other http-related approaches failed, use the plain uri
        if (tempFilename == null) {
            runCatching { Uri.parse(url) }.getOrNull()?.let {
                tempFilename = it.lastPathSegment
            }
        }

        // Finally, if couldn't get filename from URI, get a generic filename
        var filename = tempFilename ?: "downloadfile"

        // Split filename between base and extension
        // Add an extension if filename does not have one
        val dotIndex = filename.indexOf('.')
        if (dotIndex < 0) {
            mimeType?.let {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
            }
            extension = extension ?: mimeType?.let {
                if (it.lowercase().startsWith("text/")) {
                    if (it.equals("text/html", ignoreCase = true)) {
                        "html"
                    } else {
                        "txt"
                    }
                } else {
                    "bin"
                }
            }
        } else {
            mimeType?.takeUnless { it == "application/octet-stream" }.let { type ->
                // Compare the last segment of the extension against the mime type.
                // If there's a mismatch, discard the entire extension.
                val lastDotIndex = filename.lastIndexOf('.')
                val typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    filename.substring(lastDotIndex + 1)
                )
                typeFromExt?.takeIf { !typeFromExt.equals(type, ignoreCase = true) }?.let {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
                }
            }
            extension = extension ?: filename.substring(dotIndex + 1)
            filename = filename.substring(0, dotIndex)
        }

        return "${filename}.${extension}"
    }
}
