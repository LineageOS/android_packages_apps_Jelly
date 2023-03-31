/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager

object UiUtils {
    fun isColorLight(color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val hsl = FloatArray(3)
        ColorUtils.RGBToHSL(red, green, blue, hsl)
        return hsl[2] > 0.5f
    }

    fun getColor(bitmap: Bitmap?, incognito: Boolean): Int {
        val palette = Palette.from(bitmap!!).generate()
        val fallback = Color.TRANSPARENT
        return if (incognito) palette.getMutedColor(fallback) else palette.getVibrantColor(fallback)
    }

    fun getShortcutIcon(bitmap: Bitmap, themeColor: Int): Bitmap {
        val out = Bitmap.createBitmap(
            bitmap.width, bitmap.width,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(out)
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.width)
        val radius = bitmap.width / 2.toFloat()
        paint.isAntiAlias = true
        paint.color = themeColor
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(radius, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return Bitmap.createScaledBitmap(out, 192, 192, true)
    }

    fun dpToPx(res: Resources, dp: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.displayMetrics)

    /**
     * Shows the software keyboard.
     *
     * @param view The currently focused [View], which would receive soft keyboard input.
     */
    fun showKeyboard(view: View) {
        val imm = view.context.getSystemService(InputMethodManager::class.java)
        imm.showSoftInput(view, 0)
    }

    /**
     * Hides the keyboard.
     *
     * @param view The [View] that is currently accepting input.
     */
    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Sets the specified image button to the given state, while modifying or
     * "graying-out" the icon as well
     *
     * @param enabled The state of the menu item
     * @param button  The menu item to modify
     */
    fun setImageButtonEnabled(button: ImageButton, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1.0f else 0.4f
    }

    fun isReachModeEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean("key_reach_mode", false)
    }

    fun getDimenAttr(context: Context, @StyleRes style: Int, @AttrRes dimen: Int): Float {
        val args = intArrayOf(dimen)
        val array = context.obtainStyledAttributes(style, args)
        val result = array.getDimension(0, 0f)
        array.recycle()
        return result
    }
}
