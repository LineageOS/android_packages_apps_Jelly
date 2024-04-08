/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class WebViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * Whether the current session is in incognito mode.
     */
    val isIncognito = MutableLiveData(false)

    /**
     * The current URL.
     */
    val url = MutableLiveData<String>()
}
