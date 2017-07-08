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
package org.lineageos.jelly.favorite

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_favorite.view.*
import org.lineageos.jelly.MainActivity
import org.lineageos.jelly.utils.UiUtils

internal class FavoriteHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun setData(context: Context, item: Favorite) {
        var title = item.title
        if (title.isEmpty()) {
            title = item.url.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2]
        }
        itemView.row_favorite_title.text = title
        itemView.row_favorite_title.setTextColor(if (UiUtils.isColorLight(item.color)) Color.BLACK else Color.WHITE)
        itemView.row_favorite_card.setCardBackgroundColor(item.color)

        itemView.row_favorite_card.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            intent.data = Uri.parse(item.url)
            context.startActivity(intent)
        }

        itemView.row_favorite_card.setOnLongClickListener {
            (context as FavoriteActivity).editItem(item)
            true
        }
    }
}
