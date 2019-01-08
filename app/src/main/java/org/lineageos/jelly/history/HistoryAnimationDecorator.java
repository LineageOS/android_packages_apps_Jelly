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
package org.lineageos.jelly.history;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import org.lineageos.jelly.R;

class HistoryAnimationDecorator extends RecyclerView.ItemDecoration {

    private final Drawable mBackground;

    HistoryAnimationDecorator(Context context) {
        mBackground = new ColorDrawable(ContextCompat.getColor(context, R.color.colorDelete));
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (!parent.getItemAnimator().isRunning()) {
            super.onDraw(c, parent, state);
            return;
        }

        View firstComingUp = null;
        View lastComingDown = null;
        int left = 0;
        int top = 0;
        int right = parent.getWidth();
        int bottom = 0;

        int size = parent.getLayoutManager().getChildCount();
        for (int i = 0; i < size; i++) {
            View child = parent.getLayoutManager().getChildAt(i);
            if (child.getTranslationY() < 0) {
                lastComingDown = child;
            } else if (child.getTranslationY() > 0 && firstComingUp == null) {
                firstComingUp = child;
            }
        }

        if (firstComingUp != null && lastComingDown != null) {
            top = lastComingDown.getBottom() + (int) lastComingDown.getTranslationY();
            bottom = firstComingUp.getTop() + (int) firstComingUp.getTranslationY();
        } else if (firstComingUp != null) {
            top = firstComingUp.getTop();
            bottom = firstComingUp.getTop() + (int) firstComingUp.getTranslationY();
        } else if (lastComingDown != null) {
            top = lastComingDown.getBottom() + (int) lastComingDown.getTranslationY();
            bottom = lastComingDown.getBottom();
        }

        mBackground.setBounds(left, top, right, bottom);
        mBackground.draw(c);

        super.onDraw(c, parent, state);
    }


}
