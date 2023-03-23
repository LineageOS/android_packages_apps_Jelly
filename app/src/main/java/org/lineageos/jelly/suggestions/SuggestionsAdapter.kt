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

class SuggestionsAdapter(private val mContext: Context) : BaseAdapter(), Filterable {
    private val mInflator: LayoutInflater = LayoutInflater.from(mContext)
    private val mItems = ArrayList<String>()
    private val mFilter: ItemFilter
    private var mQueryText: String? = null
    override fun getCount(): Int {
        return mItems.size
    }

    override fun getItem(position: Int): Any {
        return mItems[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: this.mInflator.inflate(R.layout.item_suggestion, parent, false)!!
        val title = view.findViewById<TextView>(R.id.title)
        val suggestion = mItems[position]
        if (mQueryText != null) {
            val query = mQueryText!!
            val spannable = SpannableStringBuilder(suggestion)
            val lcSuggestion = suggestion.lowercase(Locale.getDefault())
            var queryTextPos = lcSuggestion.indexOf(query)
            while (queryTextPos >= 0) {
                spannable.setSpan(StyleSpan(Typeface.BOLD),
                        queryTextPos, queryTextPos + query.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                queryTextPos = lcSuggestion.indexOf(query, queryTextPos + query.length)
            }
            title.text = spannable
        } else {
            title.text = suggestion
        }
        return view
    }

    override fun getFilter(): Filter {
        return mFilter
    }

    private inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            if (constraint.isNullOrBlank()) {
                return results
            }
            val provider = provider
            val query = constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
            val items = provider.fetchResults(query)
            results.count = items.size
            results.values = items
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            mItems.clear()
            val values = results.values
            if (values != null && values is List<*>) {
                val items = values.filterIsInstance<String>()
                mItems.addAll(items)
                mQueryText = constraint.toString().lowercase(Locale.getDefault()).trim {
                    it <= ' '
                }
            }
            notifyDataSetChanged()
        }

        private val provider: SuggestionProvider
            get() {
                return when (PrefsUtils.getSuggestionProvider(mContext)) {
                    SuggestionProviderType.BAIDU -> BaiduSuggestionProvider()
                    SuggestionProviderType.BING -> BingSuggestionProvider()
                    SuggestionProviderType.DUCK -> DuckSuggestionProvider()
                    SuggestionProviderType.GOOGLE -> GoogleSuggestionProvider()
                    SuggestionProviderType.YAHOO -> YahooSuggestionProvider()
                    else -> NoSuggestionProvider()
                }
            }
    }

    init {
        mFilter = ItemFilter()
    }
}
