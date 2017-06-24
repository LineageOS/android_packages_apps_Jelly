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
    private final ArrayList<SuggestionItem> mItems = new ArrayList<>();
    private final Context mContext;
    private final ItemFilter mFilter;

    public SuggestionsAdapter(Context context) {
        super();
        mContext = context;
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
        // Get the data item for this position
        SuggestionItem item = mItems.get(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_suggestion, parent, false);
        }
        // Lookup view for data population
        TextView title = (TextView) convertView.findViewById(R.id.title);
        // Populate the data into the template view using the data object
        title.setText(item.getSuggestion());
        // Return the completed view to render on screen
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
                List<SuggestionItem> items = provider.fetchResults(query);
                results.count = items.size();
                results.values = items;
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mItems.clear();
            if (results.values != null) {
                List<SuggestionItem> items = (List<SuggestionItem>) results.values;
                mItems.addAll(items);
            }
            notifyDataSetChanged();
        }

        private SuggestionProvider getProvider() {
            switch (PrefsUtils.getSuggestionProvider(mContext)) {
                case BAIDU: return new BaiduSuggestionProvider();
                case DUCK: return new DuckSuggestionProvider();
                case GOOGLE: return new GoogleSuggestionProvider();
            }
            return null;
        }
    }
}