/*
 * SPDX-FileCopyrightText: 2020-2023 The LineageOS Project
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
import android.widget.TextView
import org.lineageos.jelly.R
import org.lineageos.jelly.utils.PrefsUtils
import org.lineageos.jelly.utils.PrefsUtils.SuggestionProviderType
import java.util.Locale

class SuggestionsAdapter(private val context: Context) : BaseAdapter(), Filterable {
    private val inflater = LayoutInflater.from(context)
    private var items: List<String> = listOf()
    private val filter = ItemFilter()
    private var queryText: String? = null

    override fun getCount() = items.size

    override fun getItem(position: Int) = items[position]

    override fun getItemId(position: Int) = 0L

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_suggestion, parent, false)
        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        val suggestion = items[position]
        queryText?.also { query ->
            val spannable = SpannableStringBuilder(suggestion)
            val lcSuggestion = suggestion.lowercase(Locale.getDefault())
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
            titleTextView.text = spannable
        } ?: run {
            titleTextView.text = suggestion
        }
        return view
    }

    override fun getFilter(): Filter = filter

    private inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = FilterResults()
            constraint?.takeUnless { it.isBlank() }?.let {
                val provider = provider
                val query = it.toString().lowercase(Locale.getDefault()).trim()
                val results = provider.fetchResults(query)
                if (results.isNotEmpty()) {
                    filterResults.count = results.size
                    filterResults.values = results
                    queryText = query
                    items = results
                }
            }
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            notifyDataSetChanged()
        }

        private val provider: SuggestionProvider
            get() = when (PrefsUtils.getSuggestionProvider(context)) {
                SuggestionProviderType.BAIDU -> BaiduSuggestionProvider()
                SuggestionProviderType.BING -> BingSuggestionProvider()
                SuggestionProviderType.DUCK -> DuckSuggestionProvider()
                SuggestionProviderType.GOOGLE -> GoogleSuggestionProvider()
                SuggestionProviderType.YAHOO -> YahooSuggestionProvider()
                else -> NoSuggestionProvider()
            }
    }
}
