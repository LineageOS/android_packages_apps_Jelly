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
    private var keyView: TextView? = null
    private var valueView: TextView? = null

    init {
        View.inflate(context, R.layout.key_value_view, this)
        keyView = findViewById(R.id.key)
        valueView = findViewById(R.id.value)
    }

    fun setText(@StringRes attributeTextResId: Int, value: String) {
        if (value.isNotEmpty()) {
            keyView!!.setText(attributeTextResId)
            valueView!!.text = value
        } else {
            visibility = View.GONE
        }
    }
}
