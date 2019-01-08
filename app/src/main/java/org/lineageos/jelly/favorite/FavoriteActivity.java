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
package org.lineageos.jelly.favorite;

import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.lineageos.jelly.R;
import org.lineageos.jelly.utils.UiUtils;

public class FavoriteActivity extends AppCompatActivity {
    private RecyclerView mList;
    private View mEmptyView;

    private FavoriteAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setContentView(R.layout.activity_favorites);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        mList = findViewById(R.id.favorite_list);
        mEmptyView = findViewById(R.id.favorite_empty_layout);

        mAdapter = new FavoriteAdapter(this);

        getLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                return new CursorLoader(FavoriteActivity.this, FavoriteProvider.Columns.CONTENT_URI,
                        null, null, null, FavoriteProvider.Columns._ID + " DESC");
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                mAdapter.swapCursor(cursor);

                if (cursor.getCount() == 0) {
                    mList.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mAdapter.swapCursor(null);
            }
        });

        mList.setLayoutManager(new GridLayoutManager(this, 2));
        mList.setItemAnimator(new DefaultItemAnimator());
        mList.setAdapter(mAdapter);

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

    void editItem(long id, String title, String url) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_favorite_edit, new LinearLayout(this));
        EditText titleEdit = view.findViewById(R.id.favorite_edit_title);
        EditText urlEdit = view.findViewById(R.id.favorite_edit_url);

        titleEdit.setText(title);
        urlEdit.setText(url);

        String error = getString(R.string.favorite_edit_error);
        urlEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().contains("http")) {
                    urlEdit.setError(error);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().contains("http")) {
                    urlEdit.setError(error);
                }
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.favorite_edit_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.favorite_edit_positive,
                        ((dialog, which) -> {
                            String updatedUrl = urlEdit.getText().toString();
                            String updatedTitle = titleEdit.getText().toString();
                            if (url.isEmpty()) {
                                urlEdit.setError(error);
                                urlEdit.requestFocus();
                            }
                            new UpdateFavoriteTask(getContentResolver(), id, updatedTitle,
                                    updatedUrl).execute();
                            dialog.dismiss();
                        }))
                .setNeutralButton(R.string.favorite_edit_delete,
                        (dialog, which) -> {
                            new DeleteFavoriteTask(getContentResolver()).execute(id);
                            dialog.dismiss();
                        })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> dialog.dismiss())
                .show();
    }

    private static class UpdateFavoriteTask extends AsyncTask<Void, Void, Void> {
        private final ContentResolver contentResolver;
        private final long id;
        private final String title;
        private final String url;

        UpdateFavoriteTask(ContentResolver contentResolver, long id, String title, String url) {
            this.contentResolver = contentResolver;
            this.id = id;
            this.title = title;
            this.url = url;
        }

        @Override
        protected Void doInBackground(Void... params) {
            FavoriteProvider.updateItem(contentResolver, id, title, url);
            return null;
        }
    }

    private static class DeleteFavoriteTask extends AsyncTask<Long, Void, Void> {
        private final ContentResolver contentResolver;

        DeleteFavoriteTask(ContentResolver contentResolver) {
            this.contentResolver = contentResolver;
        }

        @Override
        protected Void doInBackground(Long... ids) {
            for (Long id : ids) {
                Uri uri = ContentUris.withAppendedId(FavoriteProvider.Columns.CONTENT_URI, id);
                contentResolver.delete(uri, null, null);
            }
            return null;
        }
    }
}
