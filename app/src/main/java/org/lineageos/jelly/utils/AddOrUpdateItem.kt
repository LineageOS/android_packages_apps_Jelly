/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.utils

import android.content.ContentResolver

interface AddOrUpdateItem {
    fun addOrUpdateItem(
        resolver: ContentResolver, d1: String, d2: String, d3: String
    )
}
