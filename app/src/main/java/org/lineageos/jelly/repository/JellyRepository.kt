/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.repository

import android.content.Context
import org.lineageos.jelly.flow.FavoriteFlow
import org.lineageos.jelly.flow.HistoryFlow

object JellyRepository {
    fun favorites(context: Context) = FavoriteFlow(context).flowData()
    fun history(context: Context) = HistoryFlow(context).flowData()
}
