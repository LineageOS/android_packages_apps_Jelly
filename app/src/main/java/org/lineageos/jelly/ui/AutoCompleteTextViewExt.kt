/*
 * Copyright (C) 2017 The LineageOS Project
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
package org.lineageos.jelly.ui

import android.content.Context
import android.graphics.*
import android.support.v7.widget.AppCompatAutoCompleteTextView
import android.util.AttributeSet

class AutoCompleteTextViewExt : AppCompatAutoCompleteTextView {

    private var mPositionX: Int = 0

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private fun getGradient(widthEnd: Float, fadeStart: Float,
            stopStart: Float, stopEnd: Float, color: Int): LinearGradient {
        return LinearGradient(0f, 0f, widthEnd, 0f,
                intArrayOf(color, Color.TRANSPARENT, color, color, Color.TRANSPARENT),
                floatArrayOf(0f, fadeStart, stopStart, stopEnd, 1f), Shader.TileMode.CLAMP)
    }

    override fun onScrollChanged(x: Int, y: Int, oldX: Int, oldY: Int) {
        super.onScrollChanged(x, y, oldX, oldY)
        mPositionX = x
        requestLayout()
    }

    public override fun onDraw(canvas: Canvas) {
        val lineWidth = layout.getLineWidth(0)
        val width = measuredWidth.toFloat()

        if (text == null || text.isEmpty() || lineWidth <= width) {
            paint.shader = null
            super.onDraw(canvas)
            return
        }

        val textColor = currentTextColor
        val widthEnd = width + mPositionX
        val percent = (width * 0.2).toInt().toFloat()

        val fadeStart = mPositionX / widthEnd

        val stopStart = if (mPositionX > 0) (mPositionX + percent) / widthEnd else 0.toFloat()
        val stopEnd = (widthEnd - if (lineWidth > widthEnd) percent else 0.toFloat()) / widthEnd
        paint.shader = getGradient(widthEnd, fadeStart, stopStart, stopEnd, textColor)
        super.onDraw(canvas)
    }

    public override fun onFocusChanged(gainFocus: Boolean, direction: Int, prevFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, prevFocusedRect)
        if (!gainFocus) {
            setSelection(0)
        }
    }
}
