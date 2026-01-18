/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.suggestions

data class SuggestItem(val title: String, val url: String? = null) {
    override fun toString() = url ?: title
}
