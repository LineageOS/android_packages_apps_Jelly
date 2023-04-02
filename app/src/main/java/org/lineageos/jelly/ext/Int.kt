/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ext

import android.content.res.Resources.getSystem
import kotlin.math.roundToInt

val Int.toPx: Int
    get() = (this * getSystem().displayMetrics.density).roundToInt()
