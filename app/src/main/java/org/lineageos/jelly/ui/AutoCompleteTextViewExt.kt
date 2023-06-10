/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Rect
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

class AutoCompleteTextViewExt : AppCompatAutoCompleteTextView {
    private var focusChangeListener: OnFocusChangeListener? = null
    private var positionX = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) :
            super(context, attrs, defStyle)

    override fun getOnFocusChangeListener() = focusChangeListener!!

    // Override View's focus change listener handling so that we're able to
    // call it before the actual focus change handling, in particular before
    // the IME is fired up.
    override fun setOnFocusChangeListener(l: OnFocusChangeListener) {
        focusChangeListener = l
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        focusChangeListener?.onFocusChange(this, gainFocus)
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    override fun onScrollChanged(x: Int, y: Int, oldX: Int, oldY: Int) {
        super.onScrollChanged(x, y, oldX, oldY)
        positionX = x
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
        val widthEnd = width + positionX
        val percent = (width * 0.2).toFloat()
        val fadeStart = positionX / widthEnd
        val stopStart = if (positionX > 0) (positionX + percent) / widthEnd else 0f
        val stopEnd = (widthEnd - if (lineWidth > widthEnd) percent else 0f) / widthEnd
        paint.shader = getGradient(widthEnd, fadeStart, stopStart, stopEnd, textColor)
        super.onDraw(canvas)
    }

    companion object {
        private fun getGradient(
            widthEnd: Float, fadeStart: Float,
            stopStart: Float, stopEnd: Float, color: Int
        ) = LinearGradient(
            0f, 0f, widthEnd, 0f,
            intArrayOf(color, Color.TRANSPARENT, color, color, Color.TRANSPARENT),
            floatArrayOf(0f, fadeStart, stopStart, stopEnd, 1f), Shader.TileMode.CLAMP
        )
    }
}
