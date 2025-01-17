/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle

class RedirectorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityManager = getSystemService(ActivityManager::class.java)
        val taskList = activityManager.appTasks.filter {
            it.taskInfo.baseIntent.component?.className == MainActivity::class.java.name
        }

        if (taskList.isNotEmpty()) {
            // Bring the most recent task (MainActivity) to the foreground
            taskList[0].moveToFront()
        } else {
            // No existing task, start a fresh instance
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startActivity(intent)
        }

        finish()
    }
}
