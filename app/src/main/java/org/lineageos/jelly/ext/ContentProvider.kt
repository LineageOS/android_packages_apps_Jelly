/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ext

import android.content.ContentProvider

fun ContentProvider.requireContextExt() = context ?: throw IllegalStateException()
