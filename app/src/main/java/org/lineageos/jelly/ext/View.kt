/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ext

import android.app.Activity
import android.content.ContextWrapper
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.get
import kotlin.reflect.safeCast

internal val View.activity: Activity?
    get() = ContextWrapper::class.safeCast(context)?.let { contextWrapper ->
        Activity::class.safeCast(contextWrapper) ?: contextWrapper.baseContext
    } as? Activity

internal fun View.requireActivity() = activity!!

inline fun <reified T : ViewModel> View.viewModels() = lazy {
    ViewModelProvider(findViewTreeViewModelStoreOwner()!!).get<T>()
}
