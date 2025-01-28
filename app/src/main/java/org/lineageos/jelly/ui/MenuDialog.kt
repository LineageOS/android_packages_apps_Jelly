/*
 * SPDX-FileCopyrightText: 2020-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import com.google.android.material.materialswitch.MaterialSwitch
import org.lineageos.jelly.R

class MenuDialog(
    context: Context,
    private val onClickListener: (option: Option) -> Unit
) {
    private val layoutInflater = LayoutInflater.from(context)

    private val view = layoutInflater.inflate(R.layout.menu_dialog, LinearLayout(context)).apply {
        measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
    }

    private val backButton by lazy { view.findViewById<ImageButton>(R.id.backButton) }
    private val forwardButton by lazy { view.findViewById<ImageButton>(R.id.forwardButton) }
    private val refreshButton by lazy { view.findViewById<ImageButton>(R.id.refreshButton) }
    private val addToFavoriteButton by lazy { view.findViewById<ImageButton>(R.id.addToFavoriteButton) }
    private val shareButton by lazy { view.findViewById<ImageButton>(R.id.shareButton) }

    private val newTabButton by lazy { view.findViewById<LinearLayout>(R.id.newTabButton) }
    private val newPrivateTabButton by lazy { view.findViewById<LinearLayout>(R.id.newPrivateTabButton) }

    private val favoritesButton by lazy { view.findViewById<LinearLayout>(R.id.favoritesButton) }
    private val historyButton by lazy { view.findViewById<LinearLayout>(R.id.historyButton) }
    private val downloadsButton by lazy { view.findViewById<LinearLayout>(R.id.downloadsButton) }

    private val addToHomeScreenButton by lazy { view.findViewById<LinearLayout>(R.id.addToHomeScreenButton) }
    private val findInPageButton by lazy { view.findViewById<LinearLayout>(R.id.findInPageButton) }
    private val desktopViewSwitch by lazy { view.findViewById<MaterialSwitch>(R.id.desktopViewSwitch) }
    private val printButton by lazy { view.findViewById<LinearLayout>(R.id.printButton) }
    private val backgroundShortcutsButton by lazy {
        view.findViewById<LinearLayout>(R.id.backgroundShortcutsButton)
    }
    private val settingsButton by lazy { view.findViewById<LinearLayout>(R.id.settingsButton) }

    private val popupWindow = PopupWindow(
        view,
        RelativeLayout.LayoutParams.WRAP_CONTENT,
        RelativeLayout.LayoutParams.WRAP_CONTENT,
        true
    ).apply {
        elevation = context.resources.getDimension(R.dimen.toolbar_elevation)
    }

    var isDesktopMode = false
        set(value) {
            field = value

            desktopViewSwitch.isChecked = value
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
        BACKGROUND_SHORTCUTS,
        SETTINGS,
    }

    init {
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
        desktopViewSwitch.setOnCheckedChangeListener { _, _ -> triggerOption(Option.DESKTOP_VIEW) }
        printButton.setOnClickListener { triggerOption(Option.PRINT) }
        backgroundShortcutsButton.setOnClickListener { triggerOption(Option.BACKGROUND_SHORTCUTS) }
        settingsButton.setOnClickListener { triggerOption(Option.SETTINGS) }
    }

    fun showAsDropdownMenu(anchor: View, isReachMode: Boolean = false) {
        val xOffset = anchor.width - view.measuredWidth
        val yOffset = if (isReachMode) {
            -(anchor.height + view.measuredHeight)
        } else {
            0
        }
        popupWindow.showAsDropDown(anchor, xOffset, yOffset)
    }

    fun dismiss() {
        popupWindow.dismiss()
    }

    private fun triggerOption(option: Option) {
        onClickListener(option)
    }
}
