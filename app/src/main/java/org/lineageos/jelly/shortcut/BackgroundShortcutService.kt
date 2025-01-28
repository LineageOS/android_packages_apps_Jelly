/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.shortcut

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.appcompat.view.ContextThemeWrapper
import org.lineageos.jelly.R
import org.lineageos.jelly.webview.WebViewExt

class BackgroundShortcutService : Service() {

    private val binder: ServiceBinder = ServiceBinder()
    private val shortcuts: MutableMap<String, WebViewExt> = mutableMapOf()
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START_FOREGROUND_ACTION -> startForegroundService()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        shortcuts.values.forEach { it.destroy() }
        shortcuts.clear()
        super.onDestroy()
    }

    fun getRunning(): Set<String> = shortcuts.keys.toSet()

    fun getWebView(backgroundShortcut: BackgroundShortcut) = shortcuts.getOrPut(
        backgroundShortcut.id
    ) {
        val themeContext = ContextThemeWrapper(this, R.style.Theme_Jelly)
        WebViewExt.newInstance(
            themeContext,
            backgroundShortcut,
            this
        )
    }.also {
        updateNotification()
    }

    fun destroyWebView(id: String) {
        if (!shortcuts.containsKey(id)) return
        shortcuts[id]!!.destroy()
        shortcuts.remove(id)
        if (shortcuts.isEmpty()) stopForeground(STOP_FOREGROUND_REMOVE)
        else updateNotification()
    }

    private fun startForegroundService() {
        createNotificationChannel()
        startForeground(SERVICE_NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        val channelName = getString(R.string.background_shortcuts_title)
        val channelDescription = getString(R.string.background_shortcuts_notification_channel)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = channelDescription }
        notificationManager.createNotificationChannel(channel)
    }

    private fun notificationTitle() = getString(R.string.background_shortcuts_title)

    private fun notificationContent() = shortcuts.size.let { size ->
        when (size) {
            1 -> getString(R.string.background_shortcuts_notification_content_single)
            else -> getString(R.string.background_shortcuts_notification_content_multiple)
        }
    }

    private fun notificationIntent() = PendingIntent.getActivity(
        this,
        ACTIVITY_REQUEST_CODE,
        Intent(this, BackgroundShortcutActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )

    private fun buildNotification(): Notification {
        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
            val content = notificationContent()
            setContentTitle(notificationTitle())
            setContentText("${shortcuts.size} $content")
            setSmallIcon(R.drawable.ic_external)
            setContentIntent(notificationIntent())
        }.build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        notificationManager.notify(SERVICE_NOTIFICATION_ID, notification)
    }

    inner class ServiceBinder : Binder() {
        fun getService(): BackgroundShortcutService = this@BackgroundShortcutService
    }

    companion object {
        const val START_FOREGROUND_ACTION = "start_foreground_action"

        private const val NOTIFICATION_CHANNEL_ID = "background_shortcut_channel"
        private const val SERVICE_NOTIFICATION_ID = 1
        private const val ACTIVITY_REQUEST_CODE = 0
    }
}
