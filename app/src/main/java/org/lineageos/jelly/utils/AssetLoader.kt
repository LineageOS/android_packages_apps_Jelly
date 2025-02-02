/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.utils

import android.content.res.Resources

object AssetLoader {
    private val cache = mutableMapOf<String, String>()

    fun loadAsset(resources: Resources, fileName: String): String = cache.getOrPut(fileName) {
        resources.assets.open(fileName).bufferedReader().use { it.readText() }
    }
}
