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
package org.lineageos.jelly.history;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.lineageos.jelly.R;

import java.util.List;

class HistoryAdapter extends RecyclerView.Adapter<HistoryHolder> {

    private final Context mContext;
    private List<HistoryItem> mList;

    HistoryAdapter(Context context, List<HistoryItem> list) {
        mContext = context;
        mList = list;
    }

    @Override
    public HistoryHolder onCreateViewHolder(ViewGroup parent, int type) {
        return new HistoryHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false));
    }

    @Override
    public void onBindViewHolder(HistoryHolder holder, int position) {
        holder.setData(mContext, mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    void updateList(List<HistoryItem> list) {
        mList = list;
        notifyDataSetChanged();
    }

    void removeItem(long id) {
        int position = 0;
        for (; position < mList.size(); position++) {
            if (mList.get(position).getId() == id) {
                break;
            }
        }

        if (position == mList.size()) {
            return;
        }

        mList.remove(position);
        notifyItemRemoved(position);

        if (mList.isEmpty()) {
            // Show empty status
            ((HistoryActivity) mContext).refresh();
        }
    }
}
