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
package org.lineageos.jelly;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.lineageos.jelly.utils.PrefsUtils;
import org.lineageos.jelly.utils.UiUtils;

// "noSuchMethodError" is thrown if lambda is used in this class (wtf)
@SuppressWarnings("Convert2Lambda")
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstance) {
            super.onCreate(savedInstance);
            addPreferencesFromResource(R.xml.settings);

            Preference homePage = findPreference("key_home_page");
            homePage.setSummary(PrefsUtils.getHomePage(getContext()));
            homePage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    editHomePage(preference);
                    return true;
                }
            });

            Preference clearCookie = findPreference("key_cookie_clear");
            clearCookie.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CookieManager.getInstance().removeAllCookies(null);
                    Toast.makeText(getContext(), getString(R.string.pref_cookie_clear_done),
                            Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        }

        private void editHomePage(Preference preference) {
            EditText editText = new EditText(getContext());
            editText.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            editText.setHint(R.string.pref_start_page);
            editText.setText(PrefsUtils.getHomePage(getContext()));

            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.pref_start_page_dialog_title)
                    .setMessage(R.string.pref_start_page_dialog_message)
                    .setView(editText)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String url = editText.getText().toString().isEmpty() ?
                                            getString(R.string.default_home_page) :
                                            editText.getText().toString();
                                    PrefsUtils.setHomePage(getContext(), url);
                                    preference.setSummary(url);
                                }
                    })
                    .setNeutralButton(R.string.pref_start_page_dialog_reset,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String url = getString(R.string.default_home_page);
                                    PrefsUtils.setHomePage(getContext(), url);
                                    preference.setSummary(url);
                                }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }
}
