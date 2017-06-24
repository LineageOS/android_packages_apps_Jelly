package org.lineageos.jelly.suggestions;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.lineageos.jelly.R;

import java.util.ArrayList;

public class SuggestionsAdapter extends ArrayAdapter<SuggestionItem> {
    public SuggestionsAdapter(@NonNull Context context,
                              @NonNull ArrayList<SuggestionItem> suggestions) {
        super(context, 0, suggestions);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        SuggestionItem item = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_history, parent, false);
        }
        // Lookup view for data population
        TextView title = (TextView) convertView.findViewById(R.id.row_history_title);
        // Populate the data into the template view using the data object
        title.setText(item.getSuggestion());
        // Return the completed view to render on screen
        return convertView;
    }
}
