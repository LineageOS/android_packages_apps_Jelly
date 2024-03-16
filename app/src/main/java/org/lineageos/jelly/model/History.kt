/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.model

data class History(
    val id: Long,
    val title: String,
    val url: String,
    val timestamp: Long
)
