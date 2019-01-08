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
package org.lineageos.jelly.favorite;

import android.content.Context;
import android.database.Cursor;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.lineageos.jelly.R;

class FavoriteAdapter extends RecyclerView.Adapter<FavoriteHolder> {
    private final Context mContext;
    private Cursor mCursor;

    private int mIdColumnIndex;
    private int mTitleColumnIndex;
    private int mUrlColumnIndex;
    private int mColorColumnIndex;

    FavoriteAdapter(Context context) {
        mContext = context;
        setHasStableIds(true);
    }

    void swapCursor(Cursor cursor) {
        if (cursor == mCursor) {
            return;
        }
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = cursor;
        if (mCursor != null) {
            mIdColumnIndex = cursor.getColumnIndexOrThrow(FavoriteProvider.Columns._ID);
            mTitleColumnIndex = cursor.getColumnIndexOrThrow(FavoriteProvider.Columns.TITLE);
            mUrlColumnIndex = cursor.getColumnIndexOrThrow(FavoriteProvider.Columns.URL);
            mColorColumnIndex = cursor.getColumnIndexOrThrow(FavoriteProvider.Columns.COLOR);
        }
        notifyDataSetChanged();
    }

    @Override
    public FavoriteHolder onCreateViewHolder(ViewGroup parent, int type) {
        return new FavoriteHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false));
    }

    @Override
    public void onBindViewHolder(FavoriteHolder holder, int position) {
        if (!mCursor.moveToPosition(position)) {
            return;
        }
        long id = mCursor.getLong(mIdColumnIndex);
        String title = mCursor.getString(mTitleColumnIndex);
        String url = mCursor.getString(mUrlColumnIndex);
        int color = mCursor.getInt(mColorColumnIndex);
        holder.bind(mContext, id, title, url, color);
    }

    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    @Override
    public long getItemId(int position) {
        return mCursor.moveToPosition(position) ? mCursor.getLong(mIdColumnIndex) : -1;
    }
}
