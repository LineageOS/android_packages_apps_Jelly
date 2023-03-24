/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.history

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import org.lineageos.jelly.R

internal class HistoryAnimationDecorator(context: Context) : ItemDecoration() {
    private val background = ColorDrawable(ContextCompat.getColor(context, R.color.colorDelete))

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!parent.itemAnimator!!.isRunning) {
            super.onDraw(c, parent, state)
            return
        }
        var firstComingUp: View? = null
        var lastComingDown: View? = null
        val left = 0
        var top = 0
        val right = parent.width
        var bottom = 0
        val layoutManager = parent.layoutManager!!
        val size = layoutManager.childCount
        for (i in 0 until size) {
            val child = layoutManager.getChildAt(i)!!
            if (child.translationY < 0) {
                lastComingDown = child
            } else if (child.translationY > 0 && firstComingUp == null) {
                firstComingUp = child
            }
        }
        if (firstComingUp != null && lastComingDown != null) {
            top = lastComingDown.bottom + lastComingDown.translationY.toInt()
            bottom = firstComingUp.top + firstComingUp.translationY.toInt()
        } else if (firstComingUp != null) {
            top = firstComingUp.top
            bottom = firstComingUp.top + firstComingUp.translationY.toInt()
        } else if (lastComingDown != null) {
            top = lastComingDown.bottom + lastComingDown.translationY.toInt()
            bottom = lastComingDown.bottom
        }
        background.setBounds(left, top, right, bottom)
        background.draw(c)
        super.onDraw(c, parent, state)
    }
}
