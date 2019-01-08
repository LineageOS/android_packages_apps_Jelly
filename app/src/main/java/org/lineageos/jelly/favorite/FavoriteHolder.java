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
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.lineageos.jelly.MainActivity;
import org.lineageos.jelly.R;
import org.lineageos.jelly.utils.UiUtils;

class FavoriteHolder extends RecyclerView.ViewHolder {
    private final CardView mCard;
    private final TextView mTitle;

    FavoriteHolder(View view) {
        super(view);
        mCard = view.findViewById(R.id.row_favorite_card);
        mTitle = view.findViewById(R.id.row_favorite_title);
    }

    void bind(Context context, long id, String title, String url, int color) {
        String adjustedTitle = title == null || title.isEmpty() ? url.split("/")[2] : title;
        mTitle.setText(adjustedTitle);
        mTitle.setTextColor(UiUtils.isColorLight(color) ? Color.BLACK : Color.WHITE);
        mCard.setCardBackgroundColor(color);

        mCard.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);
        });

        mCard.setOnLongClickListener(v -> {
            ((FavoriteActivity) context).editItem(id, adjustedTitle, url);
            return true;
        });
    }
}
