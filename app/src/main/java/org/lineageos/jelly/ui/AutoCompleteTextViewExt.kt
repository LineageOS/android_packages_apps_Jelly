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
package org.lineageos.jelly.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

class AutoCompleteTextViewExt : AppCompatAutoCompleteTextView {
    private var mFocusChangeListener: OnFocusChangeListener? = null
    private var mPositionX = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) :
            super(context, attrs, defStyle)

    override fun getOnFocusChangeListener(): OnFocusChangeListener {
        return mFocusChangeListener!!
    }

    // Override View's focus change listener handling so that we're able to
    // call it before the actual focus change handling, in particular before
    // the IME is fired up.
    override fun setOnFocusChangeListener(l: OnFocusChangeListener) {
        mFocusChangeListener = l
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (mFocusChangeListener != null) {
            mFocusChangeListener!!.onFocusChange(this, gainFocus)
        }
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
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
        val percent: Float = (width * 0.2).toFloat()
        val fadeStart = mPositionX / widthEnd
        val stopStart: Float = if (mPositionX > 0) (mPositionX + percent) / widthEnd else 0f
        val stopEnd: Float = (widthEnd - if (lineWidth > widthEnd) percent else 0f) / widthEnd
        paint.shader = getGradient(widthEnd, fadeStart, stopStart, stopEnd, textColor)
        super.onDraw(canvas)
    }

    companion object {
        private fun getGradient(
            widthEnd: Float, fadeStart: Float,
            stopStart: Float, stopEnd: Float, color: Int
        ): LinearGradient {
            return LinearGradient(
                0f, 0f, widthEnd, 0f,
                intArrayOf(color, Color.TRANSPARENT, color, color, Color.TRANSPARENT),
                floatArrayOf(0f, fadeStart, stopStart, stopEnd, 1f), Shader.TileMode.CLAMP
            )
        }
    }
}