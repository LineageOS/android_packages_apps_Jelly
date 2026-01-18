/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.suggestions

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import org.lineageos.jelly.R
import java.util.Locale

class SuggestionsAdapter(context: Context) : BaseAdapter(), Filterable {
    private val inflater = LayoutInflater.from(context)
    private var items = listOf<SuggestItem>()
    private val filter = ItemFilter()
    private var queryText: String? = null

    var suggestionProvider: SuggestionProvider? = null

    override fun getCount() = items.size

    override fun getItem(position: Int) = items[position]

    override fun getItemId(position: Int) = 0L

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layout = (convertView ?: inflater.inflate(
            R.layout.item_suggestion, parent, false
        )) as LinearLayout
        val suggestion = items[position]
        val title = layout.findViewById<TextView>(R.id.suggest_title)
        val url = layout.findViewById<TextView>(R.id.suggest_url)
        url.isVisible = suggestion.url != null
        queryText?.also { query ->
            title.text = getSpannable(query, suggestion.title)
            url.text = getSpannable(query, suggestion.url ?: "")
        } ?: run {
            title.text = suggestion.title
            url.text = suggestion.url
        }
        return layout
    }

    private fun getSpannable(query: String, text: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(text)
        val lcSuggestion = text.lowercase(Locale.getDefault())
        var queryTextPos = lcSuggestion.indexOf(query)
        while (queryTextPos >= 0) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                queryTextPos,
                queryTextPos + query.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            queryTextPos = lcSuggestion.indexOf(query, queryTextPos + query.length)
        }
        return spannable
    }

    override fun getFilter(): Filter = filter

    private inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = FilterResults()
            constraint?.takeUnless { it.isBlank() }?.let {
                val query = it.toString().lowercase(Locale.getDefault()).trim()
                val results = suggestionProvider?.fetchResults(query) ?: listOf()
                filterResults.count = results.size
                filterResults.values = results
                queryText = query
                items = results
            }
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            notifyDataSetChanged()
        }
    }
}
