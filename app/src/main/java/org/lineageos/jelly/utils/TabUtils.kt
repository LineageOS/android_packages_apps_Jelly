/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
