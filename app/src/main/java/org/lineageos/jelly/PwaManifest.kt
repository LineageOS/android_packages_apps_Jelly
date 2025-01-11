/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly

data class PwaManifest(
    val startUrl: String,
    val display: String,
    val themeColor: String,
    val shortName: String,
    val name: String,
)
