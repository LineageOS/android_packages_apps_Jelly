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

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.lineageos.jelly.R;
import org.lineageos.jelly.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView mList;
    private View mEmptyView;

    private HistoryDatabaseHandler mDbHandler;
    private HistoryAdapter mAdapter;
    private RecyclerView.AdapterDataObserver mAdapterDataObserver;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setContentView(R.layout.activity_history);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        mList = (RecyclerView) findViewById(R.id.history_list);
        mEmptyView = findViewById(R.id.history_empty_layout);

        mDbHandler = new HistoryDatabaseHandler(this);
        mAdapter = new HistoryAdapter(this, new ArrayList<>());
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.addItemDecoration(new HistoryAnimationDecorator(this));
        mList.setItemAnimator(new DefaultItemAnimator());
        mList.setAdapter(mAdapter);

        mAdapterDataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                updateHistoryView(mAdapter.getItemCount() == 0);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateHistoryView(mAdapter.getItemCount() == 0);
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateHistoryView(mAdapter.getItemCount() == 0);
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                updateHistoryView(mAdapter.getItemCount() == 0);
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                updateHistoryView(mAdapter.getItemCount() == 0);
            }
        };
        mAdapter.registerAdapterDataObserver(mAdapterDataObserver);

        ItemTouchHelper helper = new ItemTouchHelper(new HistoryCallBack(this, mList
        ));
        helper.attachToRecyclerView(mList);

        int listTop = mList.getTop();
        mList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                toolbar.setElevation(recyclerView.getChildAt(0).getTop() < listTop ?
                        UiUtils.dpToPx(getResources(),
                                getResources().getDimension(R.dimen.toolbar_elevation)) : 0);
            }
        });
    }

    @Override
    public void onDestroy() {
        mAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != R.id.menu_history_delete) {
            return super.onOptionsItemSelected(item);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.history_delete_title)
                .setMessage(R.string.history_delete_message)
                .setPositiveButton(R.string.history_delete_positive,
                        (dialog, which) -> deleteAll())
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .show();
        return true;
    }

    private void updateHistoryView(boolean empty) {
        mEmptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    void refresh() {
        List<HistoryItem> items = mDbHandler.getAllItems();
        mAdapter.updateList(items);
        updateHistoryView(items.isEmpty());
    }

    private void deleteAll() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(getString(R.string.history_delete_title));
        dialog.setMessage(getString(R.string.history_deleting_message));
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
        dialog.show();

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                mDbHandler.deleteAll();
                return true;
            }

            @Override
            protected void onPostExecute(Boolean param) {
                new Handler().postDelayed(() -> {
                    dialog.dismiss();
                    refresh();
                }, 1000);
            }
        }.execute();
    }

    HistoryAdapter getAdapter() {
        return mAdapter;
    }
}
