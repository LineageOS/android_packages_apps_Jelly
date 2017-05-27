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
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.lineageos.jelly.MainActivity;
import org.lineageos.jelly.R;
import org.lineageos.jelly.utils.UiUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class HistoryHolder extends RecyclerView.ViewHolder {

    private final LinearLayout mRootLayout;
    private final TextView mTitle;
    private final TextView mSummary;

    HistoryHolder(View view) {
        super(view);
        mRootLayout = (LinearLayout) view.findViewById(R.id.row_history_layout);
        mTitle = (TextView) view.findViewById(R.id.row_history_title);
        mSummary = (TextView) view.findViewById(R.id.row_history_summary);
    }

    void setData(Context context, HistoryItem item) {
        String title = item.getTitle();
        if (title == null || title.isEmpty()) {
            title = item.getUrl().split("/")[2];
        }
        mTitle.setText(title);
        mSummary.setText(new SimpleDateFormat(context.getString(R.string.history_date_format),
                Locale.getDefault()).format(new Date(item.getId())));

        mRootLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setData(Uri.parse(item.getUrl()));
            context.startActivity(intent);
        });

        mRootLayout.setOnLongClickListener(v -> {
            HistoryAdapter adapter = ((HistoryActivity) context).getAdapter();
            HistoryDatabaseHandler dbHandler = new HistoryDatabaseHandler(context);
            dbHandler.deleteItem(item.getId());
            adapter.removeItem(item.getId());
            Snackbar.make(v, R.string.snackbar_history_item_deleted, Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.cancel, l -> {
                        dbHandler.addItem(item);
                        adapter.addItem(item);
                    }).show();
            return true;
        });

        int background;
        switch (UiUtils.getPositionInTime(item.getId())) {
            case 0:
                background = R.color.history_last_hour;
                break;
            case 1:
                background = R.color.history_today;
                break;
            case 2:
                background = R.color.history_this_week;
                break;
            case 3:
                background = R.color.history_this_month;
                break;
            default:
                background = R.color.history_earlier;
                break;
        }
        mRootLayout.setBackground(new ColorDrawable(ContextCompat.getColor(context, background)));
    }

}
