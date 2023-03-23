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
package org.lineageos.jelly.history

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import org.lineageos.jelly.R

internal class HistoryAnimationDecorator(context: Context?) : ItemDecoration() {
    private val mBackground: Drawable

    init {
        mBackground = ColorDrawable(ContextCompat.getColor(context!!, R.color.colorDelete))
    }

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
        val size = parent.layoutManager!!.childCount
        for (i in 0 until size) {
            val child = parent.layoutManager!!.getChildAt(i)
            if (child!!.translationY < 0) {
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
        mBackground.setBounds(left, top, right, bottom)
        mBackground.draw(c)
        super.onDraw(c, parent, state)
    }
}
