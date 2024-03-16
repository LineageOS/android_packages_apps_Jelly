/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.model

data class Favorite(
    val id: Long,
    val title: String,
    val url: String,
    val color: Int
)
