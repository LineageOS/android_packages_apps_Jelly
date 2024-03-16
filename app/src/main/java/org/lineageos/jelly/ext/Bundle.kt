/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ext

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import kotlin.reflect.KClass

fun <T : Parcelable> Bundle.getParcelableArrayList(
    key: String?, clazz: KClass<T>
): List<T>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getParcelableArrayList(key, clazz.java)
} else {
    @Suppress("DEPRECATION") getParcelableArrayList<T>(key)?.let { parcelableArray ->
        parcelableArray.filterNotNull().takeIf { it.size == parcelableArray.size }
    }
}
