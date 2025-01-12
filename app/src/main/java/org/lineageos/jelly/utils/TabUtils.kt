/*
 * SPDX-FileCopyrightText: 2020-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.lineageos.jelly.MainActivity

object TabUtils {
    fun openInNewTab(context: Context, url: String?, incognito: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse(System.currentTimeMillis().toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(IntentUtils.EXTRA_INCOGNITO, incognito)
            putExtra(IntentUtils.EXTRA_IGNORE_DATA, true)
            putExtra(IntentUtils.EXTRA_PAGE_URL, url)
        }
        context.startActivity(intent)
    }
}
