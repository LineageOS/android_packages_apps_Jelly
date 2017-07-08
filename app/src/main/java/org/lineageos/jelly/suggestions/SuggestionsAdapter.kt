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
import java.util.*

class SuggestionsAdapter(private val mContext: Context) : BaseAdapter(), Filterable {
    private val mItems = ArrayList<String>()
    private val mInflater: LayoutInflater = LayoutInflater.from(mContext)
    private val mFilter: ItemFilter
    private var mQueryText: String? = null

    init {
        mFilter = ItemFilter()
    }

    override fun getCount(): Int {
        return mItems.size
    }

    override fun getItem(position: Int): Any {
        return mItems[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, old_convertView: View?, parent: ViewGroup): View {
        var convertView = old_convertView
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_suggestion, parent, false)
        }

        val title = convertView!!.findViewById<TextView>(R.id.title)
        val suggestion = mItems[position]

        if (mQueryText != null) {
            val spannable = SpannableStringBuilder(suggestion)
            val lcSuggestion = suggestion.toLowerCase(Locale.getDefault())
            var queryTextPos = lcSuggestion.indexOf(mQueryText!!)
            while (queryTextPos >= 0) {
                spannable.setSpan(StyleSpan(Typeface.BOLD),
                        queryTextPos, queryTextPos + mQueryText!!.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                queryTextPos = lcSuggestion.indexOf(mQueryText!!, queryTextPos + mQueryText!!.length)
            }
            title.text = spannable
        } else {
            title.text = suggestion
        }
        return convertView
    }

    override fun getFilter(): Filter {
        return mFilter
    }

    private inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): Filter.FilterResults {
            val results = Filter.FilterResults()
            if (constraint.isNullOrEmpty()) {
                return results
            }

            val provider = provider
            val query = constraint.toString().toLowerCase(Locale.getDefault()).trim { it <= ' ' }

            if (provider != null) {
                val items = provider.fetchResults(query)
                results.count = items.size
                results.values = items
            }

            return results
        }

        override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults) {
            mItems.clear()
            if (results.values != null) {
                val items = results.values as List<String>
                mItems.addAll(items)
            }
            mQueryText = constraint?.toString()?.toLowerCase(Locale.getDefault())?.trim { it <= ' ' }
            notifyDataSetChanged()
        }

        private val provider: SuggestionProvider?
            get() {
                when (PrefsUtils.getSuggestionProvider(mContext)) {
                    PrefsUtils.SuggestionProviderType.BAIDU -> return BaiduSuggestionProvider()
                    PrefsUtils.SuggestionProviderType.BING -> return BingSuggestionProvider()
                    PrefsUtils.SuggestionProviderType.DUCK -> return DuckSuggestionProvider()
                    PrefsUtils.SuggestionProviderType.GOOGLE -> return GoogleSuggestionProvider()
                    PrefsUtils.SuggestionProviderType.YAHOO -> return YahooSuggestionProvider()
                    PrefsUtils.SuggestionProviderType.NONE -> return null
                }
            }
    }
}