/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.shortcut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import org.lineageos.jelly.R

class BackgroundShortcutAdapter :
    ListAdapter<BackgroundShortcut, BackgroundShortcutAdapter.ViewHolder>(diffCallback) {

    var getSelected: ((id: String) -> Boolean) = { false }
    var onLayoutClick: ((id: String) -> Boolean) = { false }
    var onSelectedChange: ((id: String, value: Boolean) -> Unit) = { _, _ -> }
    var onDestroyClick: ((id: String) -> Unit) = {}
    var onOpenClick: ((id: String) -> Unit) = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_background_shortcut, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val layout = view.findViewById<LinearLayout>(R.id.shortcutLayout)
        private val selected = view.findViewById<MaterialCheckBox>(R.id.shortcutSelected)
        private val name = view.findViewById<TextView>(R.id.shortcutName)
        private val actions = view.findViewById<LinearLayout>(R.id.shortcutActions)
        private val destroy = view.findViewById<LinearLayout>(R.id.shortcutDestroy)
        private val open = view.findViewById<LinearLayout>(R.id.shortcutOpen)

        fun bind(shortcut: BackgroundShortcut) {
            selected.isChecked = getSelected(shortcut.id)
            name.text = shortcut.name
            actions.isVisible = shortcut.isRunning

            layout.setOnClickListener {
                selected.isChecked = onLayoutClick(shortcut.id)
            }
            selected.setOnCheckedChangeListener { _, isChecked ->
                onSelectedChange(shortcut.id, isChecked)
            }
            destroy.setOnClickListener { onDestroyClick(shortcut.id) }
            open.setOnClickListener { onOpenClick(shortcut.id) }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<BackgroundShortcut>() {
            override fun areItemsTheSame(
                oldItem: BackgroundShortcut,
                newItem: BackgroundShortcut
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: BackgroundShortcut,
                newItem: BackgroundShortcut
            ): Boolean = oldItem == newItem
        }
    }
}
