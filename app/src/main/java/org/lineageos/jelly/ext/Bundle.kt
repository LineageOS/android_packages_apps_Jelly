/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ext

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

inline fun <reified T : Parcelable> Bundle.getParcelableArray(
    key: String?, clazz: KClass<T>
): Array<T>? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArray(key, clazz.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableArray(key)?.let { parcelableArray ->
            parcelableArray.mapNotNull { parcelable ->
                T::class.safeCast(parcelable)
            }.toTypedArray().takeIf { it.size == parcelableArray.size }
        }
    }

fun <T : Parcelable> Bundle.getParcelableArrayList(
    key: String?, clazz: KClass<T>
): List<T>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getParcelableArrayList(key, clazz.java)
} else {
    @Suppress("DEPRECATION") getParcelableArrayList<T>(key)?.let { parcelableArray ->
        parcelableArray.filterNotNull().takeIf { it.size == parcelableArray.size }
    }
}
