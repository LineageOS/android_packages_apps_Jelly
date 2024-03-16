/*
 * SPDX-FileCopyrightText: 2020-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.favorite

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.jelly.R
import org.lineageos.jelly.model.Favorite
import org.lineageos.jelly.utils.UiUtils

class FavoriteAdapter : ListAdapter<Favorite, FavoriteAdapter.FavoriteHolder>(diffCallback) {
    var onCardClick: ((Favorite) -> Unit) = { }
    var onCardLongClick: ((Favorite) -> Unit) = { }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = FavoriteHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
    )

    override fun onBindViewHolder(holder: FavoriteHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FavoriteHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val card = view.findViewById<CardView>(R.id.rowFavoriteCard)
        private val title = view.findViewById<TextView>(R.id.rowFavoriteTitle)

        fun bind(favorite: Favorite) {
            title.text = favorite.title.takeUnless {
                it.isEmpty()
            } ?: favorite.url.split("/").toTypedArray()[2]
            when (UiUtils.isColorLight(favorite.color)) {
                true -> title.setTextColor(Color.BLACK)
                false -> title.setTextColor(Color.WHITE)
            }
            card.setCardBackgroundColor(favorite.color)
            card.setOnClickListener {
                onCardClick(favorite)
            }
            card.setOnLongClickListener {
                onCardLongClick(favorite)
                true
            }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<Favorite>() {
            override fun areItemsTheSame(oldItem: Favorite, newItem: Favorite) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Favorite, newItem: Favorite) =
                oldItem == newItem
        }
    }
}
