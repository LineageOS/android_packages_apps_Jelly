/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.model

import android.net.http.SslCertificate

/**
 * Loading status of a web page.
 */
sealed class LoadingStatus(
    val url: String,
) {
    class Loading(
        url: String,
    ) : LoadingStatus(url)

    class Success(
        url: String,
        val sslCertificate: SslCertificate?,
    ) : LoadingStatus(url)
}
