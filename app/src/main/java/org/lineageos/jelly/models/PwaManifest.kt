/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.models

data class PwaManifest(
    val id: String,
    val startUrl: String,
    val display: String,
    val themeColor: String,
    val shortName: String,
    val name: String,
)
