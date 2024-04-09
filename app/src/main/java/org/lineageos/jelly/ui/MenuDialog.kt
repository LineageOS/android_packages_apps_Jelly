/*
 * SPDX-FileCopyrightText: 2020-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.material.materialswitch.MaterialSwitch
import org.lineageos.jelly.R
import org.lineageos.jelly.ext.viewModels
import org.lineageos.jelly.viewmodels.WebViewModel

class MenuDialog(
    context: Context,
    private val onClickListener: (option: Option) -> Unit
) : PopupWindow(
    LayoutInflater.from(context).inflate(R.layout.menu_dialog, FrameLayout(context)).apply {
        measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
    },
    RelativeLayout.LayoutParams.WRAP_CONTENT,
    RelativeLayout.LayoutParams.WRAP_CONTENT,
    true
) {
    // Views
    private val backButton by lazy { contentView.findViewById<ImageButton>(R.id.backButton) }
    private val forwardButton by lazy { contentView.findViewById<ImageButton>(R.id.forwardButton) }
    private val refreshButton by lazy { contentView.findViewById<ImageButton>(R.id.refreshButton) }
    private val addToFavoriteButton by lazy { contentView.findViewById<ImageButton>(R.id.addToFavoriteButton) }
    private val shareButton by lazy { contentView.findViewById<ImageButton>(R.id.shareButton) }

    private val newTabButton by lazy { contentView.findViewById<LinearLayout>(R.id.newTabButton) }
    private val newPrivateTabButton by lazy { contentView.findViewById<LinearLayout>(R.id.newPrivateTabButton) }

    private val favoritesButton by lazy { contentView.findViewById<LinearLayout>(R.id.favoritesButton) }
    private val historyButton by lazy { contentView.findViewById<LinearLayout>(R.id.historyButton) }
    private val downloadsButton by lazy { contentView.findViewById<LinearLayout>(R.id.downloadsButton) }

    private val addToHomeScreenButton by lazy { contentView.findViewById<LinearLayout>(R.id.addToHomeScreenButton) }
    private val findInPageButton by lazy { contentView.findViewById<LinearLayout>(R.id.findInPageButton) }
    private val desktopViewSwitch by lazy { contentView.findViewById<MaterialSwitch>(R.id.desktopViewSwitch) }
    private val printButton by lazy { contentView.findViewById<LinearLayout>(R.id.printButton) }
    private val settingsButton by lazy { contentView.findViewById<LinearLayout>(R.id.settingsButton) }

    private var currentAnchor: View? = null
        set(value) {
            if (value === field) {
                return
            }

            field?.let {
                onDetachedFromWindow(it)
            }

            field = value

            value?.let {
                onAttachedToWindow(it)
            }
        }

    private val desktopViewSwitchCheckedListener = CompoundButton.OnCheckedChangeListener { _, _ ->
        triggerOption(Option.DESKTOP_VIEW)
    }

    private val desktopModeObserver = Observer { desktopMode: Boolean ->
        desktopViewSwitch.apply {
            setOnCheckedChangeListener(null)
            isChecked = desktopMode
            setOnCheckedChangeListener(desktopViewSwitchCheckedListener)
        }
    }

    enum class Option {
        BACK,
        FORWARD,
        REFRESH,
        ADD_TO_FAVORITE,
        SHARE,
        NEW_TAB,
        NEW_PRIVATE_TAB,
        FAVORITES,
        HISTORY,
        DOWNLOADS,
        ADD_TO_HOME_SCREEN,
        FIND_IN_PAGE,
        DESKTOP_VIEW,
        PRINT,
        SETTINGS,
    }

    init {
        elevation = context.resources.getDimension(R.dimen.toolbar_elevation)

        setOnDismissListener {
            currentAnchor = null
        }

        backButton.setOnClickListener { triggerOption(Option.BACK) }
        forwardButton.setOnClickListener { triggerOption(Option.FORWARD) }
        refreshButton.setOnClickListener { triggerOption(Option.REFRESH) }
        addToFavoriteButton.setOnClickListener { triggerOption(Option.ADD_TO_FAVORITE) }
        shareButton.setOnClickListener { triggerOption(Option.SHARE) }

        newTabButton.setOnClickListener { triggerOption(Option.NEW_TAB) }
        newPrivateTabButton.setOnClickListener { triggerOption(Option.NEW_PRIVATE_TAB) }

        favoritesButton.setOnClickListener { triggerOption(Option.FAVORITES) }
        historyButton.setOnClickListener { triggerOption(Option.HISTORY) }
        downloadsButton.setOnClickListener { triggerOption(Option.DOWNLOADS) }

        addToHomeScreenButton.setOnClickListener { triggerOption(Option.ADD_TO_HOME_SCREEN) }
        findInPageButton.setOnClickListener { triggerOption(Option.FIND_IN_PAGE) }
        desktopViewSwitch.setOnCheckedChangeListener(desktopViewSwitchCheckedListener)
        printButton.setOnClickListener { triggerOption(Option.PRINT) }
        settingsButton.setOnClickListener { triggerOption(Option.SETTINGS) }
    }

    fun showAsDropdownMenu(anchor: View, isReachMode: Boolean = false) {
        currentAnchor = anchor

        val xOffset = anchor.width - contentView.measuredWidth
        val yOffset = if (isReachMode) {
            -(anchor.height + contentView.measuredHeight)
        } else {
            0
        }

        showAsDropDown(anchor, xOffset, yOffset)
    }

    private fun triggerOption(option: Option) {
        onClickListener(option)
    }

    private fun onAttachedToWindow(anchor: View) {
        val model by anchor.viewModels<WebViewModel>()

        model.desktopMode.observe(anchor.findViewTreeLifecycleOwner()!!, desktopModeObserver)
    }

    private fun onDetachedFromWindow(anchor: View) {
        val model by anchor.viewModels<WebViewModel>()

        model.desktopMode.removeObserver(desktopModeObserver)
    }
}
