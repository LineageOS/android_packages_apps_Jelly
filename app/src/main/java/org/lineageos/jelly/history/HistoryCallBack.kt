/*
 * Copyright (C) 2020 The LineageOS Project
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
package org.lineageos.jelly.history

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.jelly.R

class HistoryCallBack(context: Context, deleteListener: OnDeleteListener?) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
    private val mResolver: ContentResolver = context.contentResolver
    private val mBackground: Drawable
    private val mDelete: Drawable?
    private val mDeleteListener: OnDeleteListener?
    private val mMargin: Int

    init {
        mBackground = ColorDrawable(ContextCompat.getColor(context, R.color.colorDelete))
        mDelete = ContextCompat.getDrawable(context, R.drawable.ic_delete_action)
        mMargin = context.resources.getDimension(R.dimen.delete_margin).toInt()
        mDeleteListener = deleteListener
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(holder: RecyclerView.ViewHolder, swipeDir: Int) {
        val uri = ContentUris.withAppendedId(
            HistoryProvider.Columns.CONTENT_URI,
            holder.itemId
        )
        var values: ContentValues? = null
        val cursor = mResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                values = ContentValues()
                DatabaseUtils.cursorRowToContentValues(cursor, values)
            }
            cursor.close()
        }
        mResolver.delete(uri, null, null)
        if (values != null && mDeleteListener != null) {
            mDeleteListener.onItemDeleted(values)
        }
    }

    override fun onChildDraw(
        c: Canvas, recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        val view = viewHolder.itemView
        if (viewHolder.adapterPosition == -1) {
            return
        }
        mBackground.setBounds(
            view.right + dX.toInt(), view.top, view.right,
            view.bottom
        )
        mBackground.draw(c)
        val iconLeft = view.right - mMargin - mDelete!!.intrinsicWidth
        val iconTop = view.top +
                (view.bottom - view.top - mDelete.intrinsicHeight) / 2
        val iconRight = view.right - mMargin
        val iconBottom = iconTop + mDelete.intrinsicHeight
        mDelete.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        mDelete.draw(c)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    interface OnDeleteListener {
        fun onItemDeleted(data: ContentValues?)
    }
}