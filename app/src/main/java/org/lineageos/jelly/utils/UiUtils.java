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
package org.lineageos.jelly.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import androidx.annotation.AttrRes;
import androidx.annotation.DimenRes;
import androidx.annotation.StyleRes;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import org.lineageos.jelly.R;

public final class UiUtils {

    private UiUtils() {
    }

    public static boolean isColorLight(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        float hsl[] = new float[3];
        ColorUtils.RGBToHSL(red, green, blue, hsl);
        return hsl[2] > 0.5f;
    }

    public static int getColor(Bitmap bitmap, boolean incognito) {
        Palette palette = Palette.from(bitmap).generate();
        final int fallback = Color.TRANSPARENT;
        return incognito ? palette.getMutedColor(fallback) : palette.getVibrantColor(fallback);
    }

    public static Bitmap getShortcutIcon(Bitmap bitmap, int themeColor) {
        Bitmap out = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getWidth(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getWidth());
        float radius = bitmap.getWidth() / 2;
        paint.setAntiAlias(true);
        paint.setColor(themeColor);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(radius, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return Bitmap.createScaledBitmap(out, 192, 192, true);
    }

    public static int getPositionInTime(long timeMilliSec) {
        long diff = System.currentTimeMillis() - timeMilliSec;

        long hour = 1000 * 60 * 60;
        long day = hour * 24;
        long week = day * 7;
        long month = day * 30;

        return hour > diff ? 0 : day > diff ? 1 : week > diff ? 2 : month > diff ? 3 : 4;
    }

    public static float dpToPx(Resources res, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }

    /**
     * Shows the software keyboard.
     *
     * @param view The currently focused {@link View}, which would receive soft keyboard input.
     */
    public static void showKeyboard(View view) {
        InputMethodManager imm = view.getContext().getSystemService(InputMethodManager.class);
        imm.toggleSoftInputFromWindow(view.getWindowToken(), 0, 0);
    }

    /**
     * Hides the keyboard.
     *
     * @param view The {@link View} that is currently accepting input.
     */
    public static void hideKeyboard(View view) {
        InputMethodManager imm = view.getContext().getSystemService(InputMethodManager.class);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Sets the specified image button to the given state, while modifying or
     * "graying-out" the icon as well
     *
     * @param enabled The state of the menu item
     * @param button  The menu item to modify
     */
    public static void setImageButtonEnabled(ImageButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.4f);
    }

    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.is_tablet);
    }

    public static boolean isReachModeEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("key_reach_mode", false);
    }

    public static float getDimenAttr(Context context, @StyleRes int style, @AttrRes int dimen) {
        int[] args = { dimen };
        TypedArray array = context.obtainStyledAttributes(style, args);
        float result = array.getDimension(0, 0f);
        array.recycle();
        return result;
    }
}
