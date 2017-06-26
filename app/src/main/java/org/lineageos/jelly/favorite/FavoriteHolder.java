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
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
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
        mCard = (CardView) view.findViewById(R.id.row_favorite_card);
        mTitle = (TextView) view.findViewById(R.id.row_favorite_title);
    }

    void setData(Context context, Favorite item) {
        String title = item.getTitle();
        if (title == null || title.isEmpty()) {
            title = item.getUrl().split("/")[2];
        }
        mTitle.setText(title);
        mTitle.setTextColor(UiUtils.isColorLight(item.getColor()) ? Color.BLACK : Color.WHITE);
        mCard.setCardBackgroundColor(item.getColor());

        mCard.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setData(Uri.parse(item.getUrl()));
            context.startActivity(intent);
        });

        mCard.setOnLongClickListener(v -> {
            ((FavoriteActivity) context).editItem(item);
            return true;
        });
    }
}
