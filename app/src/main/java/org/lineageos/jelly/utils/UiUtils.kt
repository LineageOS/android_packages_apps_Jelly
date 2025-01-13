/*
 * SPDX-FileCopyrightText: 2020-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.utils

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
import android.view.Window
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.palette.graphics.Palette

object UiUtils {
    fun isColorLight(color: Int) = ColorUtils.calculateLuminance(color) > 0.5f

    fun getColor(bitmap: Bitmap, incognito: Boolean): Int {
        val palette = Palette.from(bitmap).generate()
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
     * Hides the keyboard.
     *
     * @param window the [Window] where the view is attached to.
     * @param view The [View] that is currently accepting input.
     */
    fun hideKeyboard(window: Window, view: View) {
        WindowInsetsControllerCompat(window, view).hide(WindowInsetsCompat.Type.ime())
    }

    /**
     * Shows the keyboard.
     *
     * @param window the [Window] where the view is attached to.
     * @param view The [View] that is currently accepting input.
     */
    fun showKeyboard(window: Window, view: View) {
        WindowInsetsControllerCompat(window, view).show(WindowInsetsCompat.Type.ime())
    }
}
