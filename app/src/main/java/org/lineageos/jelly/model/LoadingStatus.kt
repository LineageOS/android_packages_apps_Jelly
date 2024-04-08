/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.model

/**
 * Loading status of a web page.
 */
sealed class LoadingStatus(
    val url: String,
) {
    class Loading(
        url: String,
        val progress: Int,
    ) : LoadingStatus(url)

    class Success(
        url: String
    ) : LoadingStatus(url)

    class Error(
        url: String
    ) : LoadingStatus(url)
}
