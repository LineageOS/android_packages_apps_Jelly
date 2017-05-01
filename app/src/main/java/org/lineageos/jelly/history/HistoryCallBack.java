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
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import org.lineageos.jelly.R;

class HistoryCallBack extends ItemTouchHelper.SimpleCallback {

    private RecyclerView mList;

    private Drawable mBackground;
    private Drawable mDelete;

    private int mMargin;

    HistoryCallBack(Context context, RecyclerView list, int dragDirs, int swipeDirs) {
        super(dragDirs, swipeDirs);

        mList = list;
        mBackground = new ColorDrawable(ContextCompat.getColor(context, R.color.colorDelete));
        mDelete = ContextCompat.getDrawable(context, R.drawable.ic_delete_action);
        mMargin = (int) context.getResources().getDimension(R.dimen.delete_margin);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder holder, int swipeDir) {
        ((HistoryAdapter) mList.getAdapter()).removeItemAtPosition(holder.getAdapterPosition());
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View view = viewHolder.itemView;

        if (viewHolder.getAdapterPosition() == -1) {
            return;
        }

        mBackground.setBounds(view.getRight() + (int) dX, view.getTop(), view.getRight(),
                view.getBottom());
        mBackground.draw(c);

        int iconLeft = view.getRight() - mMargin - mDelete.getIntrinsicWidth();
        int iconTop = view.getTop() +
                (view.getBottom() - view.getTop() - mDelete.getIntrinsicHeight()) / 2;
        int iconRight = view.getRight() - mMargin;
        int iconBottom = iconTop + mDelete.getIntrinsicHeight();
        mDelete.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        mDelete.draw(c);

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
