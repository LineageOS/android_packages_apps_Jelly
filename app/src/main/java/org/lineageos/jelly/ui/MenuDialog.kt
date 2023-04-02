/*
 * SPDX-FileCopyrightText: 2020-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.lineageos.jelly.R
import org.lineageos.jelly.ext.toPx

class MenuDialog(
    private val context: Context,
    private val onClickListener: (option: Option) -> Unit
) {
    private val layoutInflater = LayoutInflater.from(context)

    private val view = layoutInflater.inflate(R.layout.menu_dialog, null).apply {
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
    private val addToHomeScreenButton by lazy { view.findViewById<LinearLayout>(R.id.addToHomeScreenButton) }
    private val findOnPageButton by lazy { view.findViewById<LinearLayout>(R.id.findOnPageButton) }
    private val desktopViewButton by lazy { view.findViewById<LinearLayout>(R.id.desktopViewButton) }
    private val printButton by lazy { view.findViewById<LinearLayout>(R.id.printButton) }
    private val settingsButton by lazy { view.findViewById<LinearLayout>(R.id.settingsButton) }

    private val desktopViewIcon by lazy { view.findViewById<ImageView>(R.id.desktopViewIcon) }
    private val desktopViewText by lazy { view.findViewById<TextView>(R.id.desktopViewText) }

    private val popupWindow = PopupWindow(
        view,
        RelativeLayout.LayoutParams.WRAP_CONTENT,
        RelativeLayout.LayoutParams.WRAP_CONTENT,
        true
    )

    var isDesktopMode = false
        set(value) {
            field = value

            desktopViewIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    if (isDesktopMode) {
                        R.drawable.ic_mobile
                    } else {
                        R.drawable.ic_desktop
                    }
                )
            )
            desktopViewText.text = context.resources.getString(
                if (isDesktopMode) {
                    R.string.menu_mobile_mode
                } else {
                    R.string.menu_desktop_mode
                }
            )
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
        ADD_TO_HOME_SCREEN,
        FIND_ON_PAGE,
        DESKTOP_VIEW,
        PRINT,
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
        addToHomeScreenButton.setOnClickListener { triggerOption(Option.ADD_TO_HOME_SCREEN) }
        findOnPageButton.setOnClickListener { triggerOption(Option.FIND_ON_PAGE) }
        desktopViewButton.setOnClickListener { triggerOption(Option.DESKTOP_VIEW) }
        printButton.setOnClickListener { triggerOption(Option.PRINT) }
        settingsButton.setOnClickListener { triggerOption(Option.SETTINGS) }
    }

    fun showAsDropdownMenu(anchor: View, isReachMode: Boolean = false, padding: Int = 16.toPx) {
        val xOffset = anchor.width - view.measuredWidth - padding
        val yOffset = if (isReachMode) {
            -(anchor.height + view.measuredHeight + padding)
        } else {
            padding
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
