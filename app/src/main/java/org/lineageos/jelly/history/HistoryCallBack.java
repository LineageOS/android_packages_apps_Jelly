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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.View;

import org.lineageos.jelly.R;

class HistoryCallBack extends ItemTouchHelper.SimpleCallback {
    private final ContentResolver mResolver;
    private final Drawable mBackground;
    private final Drawable mDelete;
    private final OnDeleteListener mDeleteListener;
    private final int mMargin;

    public interface OnDeleteListener {
        void onItemDeleted(ContentValues data);
    }

    HistoryCallBack(Context context, OnDeleteListener deleteListener) {
        super(0, ItemTouchHelper.LEFT);
        mResolver = context.getContentResolver();
        mBackground = new ColorDrawable(ContextCompat.getColor(context, R.color.colorDelete));
        mDelete = ContextCompat.getDrawable(context, R.drawable.ic_delete_action);
        mMargin = (int) context.getResources().getDimension(R.dimen.delete_margin);
        mDeleteListener = deleteListener;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder holder, int swipeDir) {
        Uri uri = ContentUris.withAppendedId(HistoryProvider.Columns.CONTENT_URI,
                holder.getItemId());
        ContentValues values = null;
        Cursor cursor = mResolver.query(uri, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                values = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursor, values);
            }
            cursor.close();
        }
        mResolver.delete(uri, null, null);
        if (values != null && mDeleteListener != null) {
            mDeleteListener.onItemDeleted(values);
        }
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
