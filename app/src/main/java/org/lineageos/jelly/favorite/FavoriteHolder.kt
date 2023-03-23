/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.favorite

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.jelly.MainActivity
import org.lineageos.jelly.R
import org.lineageos.jelly.utils.UiUtils

class FavoriteHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val card: CardView = view.findViewById(R.id.row_favorite_card)
    private val title: TextView = view.findViewById(R.id.row_favorite_title)
    fun bind(context: Context, id: Long, title: String?, url: String, color: Int) {
        val adjustedTitle = if (title.isNullOrEmpty()) {
            url.split("/").toTypedArray()[2]
        } else {
            title
        }
        this.title.text = adjustedTitle
        this.title.setTextColor(if (UiUtils.isColorLight(color)) Color.BLACK else Color.WHITE)
        card.setCardBackgroundColor(color)
        card.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        }
        card.setOnLongClickListener {
            (context as FavoriteActivity).editItem(id, adjustedTitle, url)
            true
        }
    }
}
