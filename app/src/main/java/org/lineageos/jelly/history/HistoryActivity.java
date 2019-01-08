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

import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.lineageos.jelly.R;
import org.lineageos.jelly.utils.UiUtils;

public class HistoryActivity extends AppCompatActivity {
    private View mEmptyView;

    private HistoryAdapter mAdapter;
    private final RecyclerView.AdapterDataObserver mAdapterDataObserver =
            new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            updateHistoryView(mAdapter.getItemCount() == 0);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView list = findViewById(R.id.history_list);
        mEmptyView = findViewById(R.id.history_empty_layout);

        mAdapter = new HistoryAdapter(this);

        getLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return new CursorLoader(HistoryActivity.this, HistoryProvider.Columns.CONTENT_URI,
                        null, null, null, HistoryProvider.Columns.TIMESTAMP + " DESC");
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                mAdapter.swapCursor(cursor);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mAdapter.swapCursor(null);
            }
        });

        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new HistoryAnimationDecorator(this));
        list.setItemAnimator(new DefaultItemAnimator());
        list.setAdapter(mAdapter);

        mAdapter.registerAdapterDataObserver(mAdapterDataObserver);

        ItemTouchHelper helper = new ItemTouchHelper(new HistoryCallBack(this, values -> {
            View rootView = findViewById(R.id.coordinator_layout);
            Snackbar.make(rootView, R.string.history_snackbar_item_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.history_snackbar_item_deleted_message, l -> {
                        getContentResolver().insert(HistoryProvider.Columns.CONTENT_URI, values);
                    })
                    .show();
        }));
        helper.attachToRecyclerView(list);

        int listTop = list.getTop();
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                boolean elevate = recyclerView.getChildAt(0) != null &&
                        recyclerView.getChildAt(0).getTop() < listTop;
                toolbar.setElevation(elevate ? UiUtils.dpToPx(getResources(),
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

    private void deleteAll() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(getString(R.string.history_delete_title));
        dialog.setMessage(getString(R.string.history_deleting_message));
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);
        dialog.show();

        new DeleteAllHistoryTask(getContentResolver(), dialog).execute();
    }

    private static class DeleteAllHistoryTask extends AsyncTask<Void, Void, Void> {
        private final ContentResolver contentResolver;
        private final ProgressDialog dialog;

        DeleteAllHistoryTask(ContentResolver contentResolver, ProgressDialog dialog) {
            this.contentResolver = contentResolver;
            this.dialog = dialog;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            contentResolver.delete(HistoryProvider.Columns.CONTENT_URI, null, null);
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            new Handler().postDelayed(dialog::dismiss, 1000);
        }
    }
}
