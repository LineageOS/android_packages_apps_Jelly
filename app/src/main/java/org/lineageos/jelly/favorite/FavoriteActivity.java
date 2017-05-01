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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.lineageos.jelly.R;
import org.lineageos.jelly.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavoriteActivity extends AppCompatActivity {
    private RecyclerView mList;
    private View mEmptyView;

    private FavoriteDatabaseHandler mDbHandler;
    private FavoriteAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setContentView(R.layout.activity_favorites);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        mList = (RecyclerView) findViewById(R.id.favorite_list);
        mEmptyView = findViewById(R.id.favorite_empty_layout);

        mDbHandler = new FavoriteDatabaseHandler(this);
        mAdapter = new FavoriteAdapter(this, new ArrayList<>());
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

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    void refresh() {
        List<Favorite> items = mDbHandler.getAllItems();
        // Reverse database list order
        Collections.reverse(items);
        mAdapter.updateList(items);

        if (items.isEmpty()) {
            mList.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    boolean editItem(Favorite item) {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_favorite_edit, null);
        EditText titleEdit = (EditText) view.findViewById(R.id.favorite_edit_title);
        EditText urlEdit = (EditText) view.findViewById(R.id.favorite_edit_url);

        titleEdit.setText(item.getTitle());
        urlEdit.setText(item.getUrl());

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
                            String url = urlEdit.getText().toString();
                            String title = titleEdit.getText().toString();
                            if (url.isEmpty()) {
                                urlEdit.setError(error);
                                urlEdit.requestFocus();
                            }
                            item.setTitle(title);
                            item.setUrl(url);
                            mDbHandler.updateItem(item);
                            refresh();
                            dialog.dismiss();
                        }))
                .setNeutralButton(R.string.favorite_edit_delete,
                        (dialog, which) -> {
                            mDbHandler.deleteItem(item.getId());
                            mAdapter.removeItem(item.getId());
                            dialog.dismiss();
                        })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> dialog.dismiss())
                .show();
        return true;
    }

}
