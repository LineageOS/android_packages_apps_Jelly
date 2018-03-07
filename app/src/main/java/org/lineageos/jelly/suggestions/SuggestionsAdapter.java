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

package org.lineageos.jelly.suggestions;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.lineageos.jelly.R;
import org.lineageos.jelly.utils.PrefsUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SuggestionsAdapter extends BaseAdapter implements Filterable {
    private final ArrayList<String> mItems = new ArrayList<>();
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final ItemFilter mFilter;
    private String mQueryText;

    public SuggestionsAdapter(Context context) {
        super();
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mFilter = new ItemFilter();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_suggestion, parent, false);
        }

        TextView title = convertView.findViewById(R.id.title);
        String suggestion = mItems.get(position);

        if (mQueryText != null) {
            SpannableStringBuilder spannable = new SpannableStringBuilder(suggestion);
            String lcSuggestion = suggestion.toLowerCase(Locale.getDefault());
            int queryTextPos = lcSuggestion.indexOf(mQueryText);
            while (queryTextPos >= 0) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD),
                        queryTextPos, queryTextPos + mQueryText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                queryTextPos = lcSuggestion.indexOf(mQueryText, queryTextPos + mQueryText.length());
            }
            title.setText(spannable);
        } else {
            title.setText(suggestion);
        }
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() == 0) {
                return results;
            }

            SuggestionProvider provider = getProvider();
            String query = constraint.toString().toLowerCase(Locale.getDefault()).trim();

            if (provider != null) {
                List<String> items = provider.fetchResults(query);
                results.count = items.size();
                results.values = items;
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mItems.clear();
            if (results.values != null) {
                List<String> items = (List<String>) results.values;
                mItems.addAll(items);
            }
            mQueryText = constraint != null
                    ? constraint.toString().toLowerCase(Locale.getDefault()).trim() : null;
            notifyDataSetChanged();
        }

        private SuggestionProvider getProvider() {
            switch (PrefsUtils.getSuggestionProvider(mContext)) {
                case BAIDU:
                    return new BaiduSuggestionProvider();
                case BING:
                    return new BingSuggestionProvider();
                case DUCK:
                    return new DuckSuggestionProvider();
                case GOOGLE:
                    return new GoogleSuggestionProvider();
                case YAHOO:
                    return new YahooSuggestionProvider();
            }
            return null;
        }
    }
}