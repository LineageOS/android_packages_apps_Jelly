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
package org.lineageos.jelly.webview

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.net.Uri
import android.text.TextUtils
import android.view.LayoutInflater
import android.webkit.HttpAuthHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import org.lineageos.jelly.R
import org.lineageos.jelly.ui.UrlBarController
import org.lineageos.jelly.utils.IntentUtils
import org.lineageos.jelly.utils.UrlUtils
import java.net.URISyntaxException

internal class WebClient(private val mUrlBarController: UrlBarController) : WebViewClient() {
    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        mUrlBarController.onPageLoadStarted(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        mUrlBarController.onPageLoadFinished(view.context, view.certificate)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.isForMainFrame) {
            val webViewExt = view as WebViewExt
            val url = request.url.toString()
            val needsLookup = (request.hasGesture()
                    || !TextUtils.equals(url, webViewExt.lastLoadedUrl))
            if (!webViewExt.isIncognito
                && needsLookup
                && !request.isRedirect
                && startActivityForUrl(view, url)
            ) {
                return true
            } else if (webViewExt.requestHeaders.isNotEmpty()) {
                webViewExt.followUrl(url)
                return true
            }
        }
        return false
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView,
        handler: HttpAuthHandler, host: String, realm: String
    ) {
        val context = view.context
        val builder = AlertDialog.Builder(context)
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.auth_dialog, LinearLayout(context))
        val username = dialogView.findViewById<EditText>(R.id.username)
        val password = dialogView.findViewById<EditText>(R.id.password)
        val authDetail = dialogView.findViewById<TextView>(R.id.auth_detail)
        val text = context.getString(R.string.auth_dialog_detail, view.url)
        authDetail.text = text
        builder.setView(dialogView)
            .setTitle(R.string.auth_dialog_title)
            .setPositiveButton(R.string.auth_dialog_login)
            { _: DialogInterface?, _: Int ->
                handler.proceed(
                    username.text.toString(), password.text.toString()
                )
            }
            .setNegativeButton(android.R.string.cancel)
            { _: DialogInterface?, _: Int ->
                handler.cancel()
            }
            .setOnDismissListener { handler.cancel() }
            .show()
    }

    private fun startActivityForUrl(view: WebView, url: String): Boolean {
        var intent: Intent
        val context = view.context
        intent = try {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        } catch (ex: URISyntaxException) {
            return false
        }
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.component = null
        intent.selector = null
        val m = UrlUtils.ACCEPTED_URI_SCHEMA.matcher(url)
        if (m.matches()) {
            // If there only are browsers for this URL, handle it ourselves
            intent = makeHandlerChooserIntent(context, intent, url) ?: return false
        } else {
            val packageName = intent.getPackage()
            if (packageName != null
                && context.packageManager.resolveActivity(intent, 0) == null
            ) {
                // Explicit intent, but app is not installed - try to redirect to Play Store
                val storeUri = Uri.parse("market://search?q=pname:$packageName")
                intent = Intent(Intent.ACTION_VIEW, storeUri)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
            }
        }
        try {
            context.startActivity(intent)
            return true
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(
                view, context.getString(R.string.error_no_activity_found),
                Snackbar.LENGTH_LONG
            ).show()
        }
        return false
    }

    private fun makeHandlerChooserIntent(context: Context, intent: Intent, url: String): Intent? {
        val pm = context.packageManager
        val activities = pm.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_RESOLVED_FILTER
        )
        if (activities.isEmpty()) {
            return null
        }
        val chooserIntents = ArrayList<Intent>()
        val ourPackageName = context.packageName
        activities.sortWith(ResolveInfo.DisplayNameComparator(pm))
        for (resolveInfo in activities) {
            val filter = resolveInfo.filter ?: continue
            val info = resolveInfo.activityInfo
            if (!info.enabled || !info.exported) {
                continue
            }
            if (filter.countDataAuthorities() == 0
                && !TextUtils.equals(info.packageName, ourPackageName)
            ) {
                continue
            }
            val targetIntent = Intent(intent)
            targetIntent.setPackage(info.packageName)
            chooserIntents.add(targetIntent)
        }
        if (chooserIntents.isEmpty()) {
            return null
        }
        val lastIntent = chooserIntents.removeAt(chooserIntents.size - 1)
        if (chooserIntents.isEmpty()) {
            // there was only one, no need to show the chooser
            return if (ourPackageName.equals(lastIntent.getPackage())) null else lastIntent
        }
        val changeIntent = Intent(IntentUtils.EVENT_URL_RESOLVED)
            .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
            .putExtra(IntentUtils.EXTRA_URL, url)
        val pi = PendingIntent.getBroadcast(
            context, 0, changeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
        )
        val chooserIntent = Intent.createChooser(lastIntent, null)
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            chooserIntents.toTypedArray()
        )
        chooserIntent.putExtra(Intent.EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER, pi.intentSender)
        return chooserIntent
    }

}