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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.lineageos.jelly.MainActivity;
import org.lineageos.jelly.R;
import org.lineageos.jelly.utils.UiUtils;

class HistoryHolder extends RecyclerView.ViewHolder {

    private final LinearLayout mRootLayout;
    private final TextView mTitle;
    private final TextView mSummary;

    HistoryHolder(View view) {
        super(view);
        mRootLayout = view.findViewById(R.id.row_history_layout);
        mTitle = view.findViewById(R.id.row_history_title);
        mSummary = view.findViewById(R.id.row_history_summary);
    }

    void bind(final Context context, String title, String url, String summary, long timestamp) {
        final String historyTitle = TextUtils.isEmpty(title) ? url.split("/")[2] : title;
        mTitle.setText(historyTitle);
        mSummary.setText(summary);

        mRootLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);
        });

        int background;
        switch (UiUtils.getPositionInTime(timestamp)) {
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
