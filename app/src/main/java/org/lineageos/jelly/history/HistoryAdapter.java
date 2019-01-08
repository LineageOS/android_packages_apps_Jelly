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
import android.database.Cursor;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.lineageos.jelly.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class HistoryAdapter extends RecyclerView.Adapter<HistoryHolder> {
    private final Context mContext;
    private final DateFormat mHistoryDateFormat;
    private Cursor mCursor;

    private int mIdColumnIndex;
    private int mTitleColumnIndex;
    private int mUrlColumnIndex;
    private int mTimestampColumnIndex;

    HistoryAdapter(Context context) {
        mContext = context;
        mHistoryDateFormat = new SimpleDateFormat(context.getString(R.string.history_date_format),
                Locale.getDefault());
        setHasStableIds(true);
    }

    public void swapCursor(Cursor cursor) {
        if (cursor == mCursor) {
            return;
        }
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = cursor;
        if (mCursor != null) {
            mIdColumnIndex = cursor.getColumnIndexOrThrow(HistoryProvider.Columns._ID);
            mTitleColumnIndex = cursor.getColumnIndexOrThrow(HistoryProvider.Columns.TITLE);
            mUrlColumnIndex = cursor.getColumnIndexOrThrow(HistoryProvider.Columns.URL);
            mTimestampColumnIndex = cursor.getColumnIndexOrThrow(HistoryProvider.Columns.TIMESTAMP);
        }
        notifyDataSetChanged();
    }

    @Override
    public HistoryHolder onCreateViewHolder(ViewGroup parent, int type) {
        return new HistoryHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false));
    }

    @Override
    public void onBindViewHolder(HistoryHolder holder, int position) {
        if (!mCursor.moveToPosition(position)) {
            return;
        }
        long timestamp = mCursor.getLong(mTimestampColumnIndex);
        String summary = mHistoryDateFormat.format(new Date(timestamp));
        String title = mCursor.getString(mTitleColumnIndex);
        String url = mCursor.getString(mUrlColumnIndex);
        holder.bind(mContext, title, url, summary, timestamp);
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
