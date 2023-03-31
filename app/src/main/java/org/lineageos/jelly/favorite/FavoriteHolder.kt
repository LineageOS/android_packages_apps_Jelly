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
import kotlin.reflect.safeCast

class FavoriteHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val rowFavoriteCard = view.findViewById<CardView>(R.id.rowFavoriteCard)
    private val rowFavoriteTitle = view.findViewById<TextView>(R.id.rowFavoriteTitle)
    fun bind(context: Context, id: Long, title: String?, url: String, color: Int) {
        val adjustedTitle = title?.takeUnless {
            it.isEmpty()
        } ?: url.split("/").toTypedArray()[2]
        this.rowFavoriteTitle.text = adjustedTitle
        this.rowFavoriteTitle.setTextColor(if (UiUtils.isColorLight(color)) Color.BLACK else Color.WHITE)
        rowFavoriteCard.setCardBackgroundColor(color)
        rowFavoriteCard.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
        }
        rowFavoriteCard.setOnLongClickListener {
            FavoriteActivity::class.safeCast(context)?.editItem(id, adjustedTitle, url)
            true
        }
    }
}
