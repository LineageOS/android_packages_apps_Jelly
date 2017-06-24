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
import android.os.AsyncTask;
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
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class SuggestionsAdapter extends BaseAdapter implements Filterable {
    private final ArrayList<SuggestionItem> items = new ArrayList<>();
    private final Context mContext;
    private final ItemFilter mFilter;
    private PrefsUtils.Suggestion mSuggestionChoice;
    private SuggestionsTask mSuggestionsTask;

    public SuggestionsAdapter(Context context) {
        super();
        mContext = context;
        mFilter = new ItemFilter(this);
        mSuggestionChoice = PrefsUtils.getSuggestionProvider(mContext);
        mSuggestionsTask = new SuggestionsTask(this);
    }

    private void addAll(Collection<SuggestionItem> list) {
        items.addAll(list);
        notifyDataSetChanged();
    }

    private void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        SuggestionItem item = items.get(position);
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

    private class SuggestionsTask extends AsyncTask<String, Void, List<SuggestionItem>> {
        private SuggestionsAdapter suggestionsAdapter;
        private boolean mDisabled;

        SuggestionsTask(SuggestionsAdapter suggestionsAdapter) {
            this.suggestionsAdapter = suggestionsAdapter;
        }

        void disable() {
            mDisabled = true;
        }

        @Override
        protected List<SuggestionItem> doInBackground(String... query) {
            List<SuggestionItem> items;

            switch (mSuggestionChoice) {
                case SUGGESTION_BAIDU:
                    items = new BaiduSuggestionsModel().fetchResults(query[0]);
                    break;
                case SUGGESTION_DUCK:
                    items = new DuckSuggestionsModel().fetchResults(query[0]);
                    break;
                case SUGGESTION_GOOGLE:
                    items = new GoogleSuggestionsModel().fetchResults(query[0]);
                    break;
                case SUGGESTION_NONE:
                default:
                    items = new ArrayList<>();
            }

            return items;
        }

        protected void onPostExecute(List<SuggestionItem> suggestionItems) {
            if (!mDisabled) {
                suggestionsAdapter.clear();
                suggestionsAdapter.addAll(suggestionItems);
            }
        }
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private class ItemFilter extends Filter {
        private final SuggestionsAdapter mSuggestionsAdapter;

        ItemFilter(SuggestionsAdapter mSuggestionsAdapter) {
            this.mSuggestionsAdapter = mSuggestionsAdapter;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() == 0) {
                clear();
                return results;
            }
            String query = constraint.toString().toLowerCase(Locale.getDefault()).trim();

            mSuggestionsTask.disable();
            mSuggestionsTask = new SuggestionsTask(mSuggestionsAdapter);
            mSuggestionsTask.execute(query);

            results.count = 1;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
        }
    }

}