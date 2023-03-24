/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
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

class HistoryCallBack(
    context: Context,
    private val deleteListener: OnDeleteListener?
) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
    private val resolver: ContentResolver = context.contentResolver
    private val background = ColorDrawable(ContextCompat.getColor(context, R.color.colorDelete))
    private val delete = ContextCompat.getDrawable(context, R.drawable.ic_delete_action)
    private val margin = context.resources.getDimension(R.dimen.delete_margin).toInt()

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) = false

    override fun onSwiped(holder: RecyclerView.ViewHolder, swipeDir: Int) {
        val uri = ContentUris.withAppendedId(
            HistoryProvider.Columns.CONTENT_URI,
            holder.itemId
        )
        var values: ContentValues? = null
        val cursor = resolver.query(uri, null, null, null, null)
        cursor?.let {
            if (it.moveToFirst()) {
                values = ContentValues()
                DatabaseUtils.cursorRowToContentValues(cursor, values)
            }
            it.close()
        }
        resolver.delete(uri, null, null)
        if (values != null) {
            deleteListener?.onItemDeleted(values)
        }
    }

    override fun onChildDraw(
        c: Canvas, recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        val view = viewHolder.itemView
        if (viewHolder.bindingAdapterPosition == -1) {
            return
        }
        background.setBounds(
            view.right + dX.toInt(), view.top, view.right,
            view.bottom
        )
        background.draw(c)
        val delete = delete!!
        val iconLeft = view.right - margin - delete.intrinsicWidth
        val iconTop = view.top +
                (view.bottom - view.top - delete.intrinsicHeight) / 2
        val iconRight = view.right - margin
        val iconBottom = iconTop + delete.intrinsicHeight
        delete.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        delete.draw(c)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    interface OnDeleteListener {
        fun onItemDeleted(data: ContentValues?)
    }
}
