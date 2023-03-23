/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import org.lineageos.jelly.R

class KeyValueView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {
    private var mKeyView: TextView? = null
    private var mValueView: TextView? = null

    init {
        View.inflate(context, R.layout.key_value_view, this)
        mKeyView = findViewById(R.id.key)
        mValueView = findViewById(R.id.value)
    }

    fun setText(@StringRes attributeTextResId: Int, value: String) {
        if (value.isNotEmpty()) {
            mKeyView!!.setText(attributeTextResId)
            mValueView!!.text = value
        } else {
            visibility = View.GONE
        }
    }
}
