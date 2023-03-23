/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.lineageos.jelly.MainActivity

object TabUtils {
    fun openInNewTab(context: Context, url: String?, incognito: Boolean) {
        val intent = Intent(context, MainActivity::class.java)
        if (url != null && url.isNotEmpty()) {
            intent.data = Uri.parse(url)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        intent.putExtra(IntentUtils.EXTRA_INCOGNITO, incognito)
        context.startActivity(intent)
    }
}
