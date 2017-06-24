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
package org.lineageos.jelly.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;

import org.lineageos.jelly.suggestions.SuggestionItem;
import org.lineageos.jelly.suggestions.SuggestionsAdapter;

import java.util.ArrayList;
import java.util.Collection;

public class AutoCompleteTextViewExt extends AppCompatAutoCompleteTextView {

    private int mPositionX;
    ArrayList<SuggestionItem> historyItems = new ArrayList<>();
    SuggestionsAdapter mAdaper;

    public AutoCompleteTextViewExt(Context context) {
        this(context, null);
    }

    public AutoCompleteTextViewExt(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoCompleteTextViewExt(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mAdaper = new SuggestionsAdapter(context, historyItems);
    }

    public void addAll(Collection<SuggestionItem> historyItems) {
        mAdaper.addAll(historyItems);
    }

    private static LinearGradient getGradient(float widthEnd, float fadeStart,
                                              float stopStart, float stopEnd, int color) {
        return new LinearGradient(0, 0, widthEnd, 0,
                new int[]{color, Color.TRANSPARENT, color, color, Color.TRANSPARENT},
                new float[]{0, fadeStart, stopStart, stopEnd, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldX, int oldY) {
        super.onScrollChanged(x, y, oldX, oldY);
        mPositionX = x;
        requestLayout();
    }

    @Override
    public void onDraw(Canvas canvas) {
        float lineWidth = getLayout().getLineWidth(0);
        float width = getMeasuredWidth();

        if (getText() == null || getText().length() == 0 || lineWidth <= width) {
            getPaint().setShader(null);
            super.onDraw(canvas);
            return;
        }

        int textColor = getCurrentTextColor();
        float widthEnd = width + mPositionX;
        float percent = (int) (width * 0.2);

        float fadeStart = mPositionX / widthEnd;

        float stopStart = mPositionX > 0 ? ((mPositionX + percent) / widthEnd) : 0;
        float stopEnd = (widthEnd - (lineWidth > widthEnd ? percent : 0)) / widthEnd;
        getPaint().setShader(getGradient(widthEnd, fadeStart, stopStart, stopEnd, textColor));
        super.onDraw(canvas);
    }

    @Override
    public void onFocusChanged(boolean gainFocus, int direction, Rect prevFocusedRect) {
        super.onFocusChanged(gainFocus, direction, prevFocusedRect);
        if (!gainFocus) {
            setSelection(0);
        }
    }
}
