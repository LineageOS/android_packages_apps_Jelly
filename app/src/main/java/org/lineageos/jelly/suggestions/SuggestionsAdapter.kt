/*
 * Copyright (C) 2020-2023 The LineageOS Project
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
import kotlin.reflect.safeCast

class SuggestionsAdapter(private val context: Context) : BaseAdapter(), Filterable {
    private val inflator: LayoutInflater = LayoutInflater.from(context)
    private val items = mutableListOf<String>()
    private val filter = ItemFilter()
    private var queryText: String? = null
    override fun getCount() = items.size

    override fun getItem(position: Int) = items[position]

    override fun getItemId(position: Int) = 0L

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: this.inflator.inflate(R.layout.item_suggestion, parent, false)!!
        val title = view.findViewById<TextView>(R.id.title)
        val suggestion = items[position]
        title.text = queryText?.let { query ->
            val spannable = SpannableStringBuilder(suggestion)
            val lcSuggestion = suggestion.lowercase(Locale.getDefault())
            var queryTextPos = lcSuggestion.indexOf(query)
            while (queryTextPos >= 0) {
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    queryTextPos, queryTextPos + query.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                queryTextPos = lcSuggestion.indexOf(query, queryTextPos + query.length)
            }
            spannable
        } ?: suggestion
        return view
    }

    override fun getFilter(): Filter = filter

    private inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            constraint.takeUnless { it.isNullOrBlank() }?.let { p ->
                val provider = provider
                val query = p.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
                provider?.let {
                    val items = it.fetchResults(query)
                    results.count = items.size
                    results.values = items
                }
            }
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            items.clear()
            List::class.safeCast(results.values)?.let { values ->
                val items = values.filterIsInstance<String>()
                this@SuggestionsAdapter.items.addAll(items)
                queryText = constraint.toString().lowercase(Locale.getDefault()).trim {
                    it <= ' '
                }
            }
            notifyDataSetChanged()
        }

        private val provider: SuggestionProvider?
            get() {
                return when (PrefsUtils.getSuggestionProvider(context)) {
                    SuggestionProviderType.BAIDU -> BaiduSuggestionProvider()
                    SuggestionProviderType.BING -> BingSuggestionProvider()
                    SuggestionProviderType.DUCK -> DuckSuggestionProvider()
                    SuggestionProviderType.GOOGLE -> GoogleSuggestionProvider()
                    SuggestionProviderType.YAHOO -> YahooSuggestionProvider()
                    else -> null
                }
            }
    }
}
