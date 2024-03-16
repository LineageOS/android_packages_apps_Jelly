/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ext

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

fun <T : Parcelable> Bundle.getParcelable(key: String?, clazz: KClass<T>) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, clazz.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key)
    }

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

inline fun <reified T : Serializable> Bundle.getSerializable(key: String?, clazz: KClass<T>) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, clazz.java)
    } else {
        @Suppress("DEPRECATION")
        T::class.safeCast(getSerializable(key))
    }
