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
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

import org.lineageos.jelly.R

internal class FavoriteAdapter(private val mContext: Context) : RecyclerView.Adapter<FavoriteHolder>() {
    private val mList = mutableListOf<Favorite>()

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): FavoriteHolder {
        return FavoriteHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_favorite, parent, false))
    }

    override fun onBindViewHolder(holder: FavoriteHolder, position: Int) {
        holder.setData(mContext, mList[position])
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun updateList(list: List<Favorite>) {
        mList.addAll(list)
        notifyDataSetChanged()
    }

    fun removeItem(id: Long) {
        var position = 0
        while (position < mList.size) {
            if (mList[position].id == id) {
                break
            }
            position++
        }

        if (position == mList.size) {
            return
        }

        mList.removeAt(position)
        notifyItemRemoved(position)

        if (mList.isEmpty()) {
            // Show empty status
            (mContext as FavoriteActivity).refresh()
        }
    }
}
