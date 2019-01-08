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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.lineageos.jelly.utils.IntentUtils;
import org.lineageos.jelly.utils.NetworkSecurityPolicyUtils;
import org.lineageos.jelly.utils.PrefsUtils;
import org.lineageos.jelly.utils.UiUtils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstance) {
            super.onCreate(savedInstance);
            addPreferencesFromResource(R.xml.settings);

            PreferenceCategory securityCategory = (PreferenceCategory)
                    findPreference("category_security");

            Preference homePage = findPreference("key_home_page");
            homePage.setSummary(PrefsUtils.getHomePage(getContext()));
            homePage.setOnPreferenceClickListener(preference -> {
                editHomePage(preference);
                return true;
            });

            Preference clearCookie = findPreference("key_cookie_clear");
            clearCookie.setOnPreferenceClickListener(preference -> {
                CookieManager.getInstance().removeAllCookies(null);
                Toast.makeText(getContext(), getString(R.string.pref_cookie_clear_done),
                        Toast.LENGTH_LONG).show();
                return true;
            });

            SwitchPreference reachMode = (SwitchPreference) findPreference(("key_reach_mode"));
            if (UiUtils.isTablet(getContext())) {
                getPreferenceScreen().removePreference(reachMode);
            } else {
                reachMode.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(IntentUtils.EVENT_CHANGE_UI_MODE);
                    intent.putExtra(IntentUtils.EVENT_CHANGE_UI_MODE, reachMode.isChecked());
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                    return true;
                });
            }

            SwitchPreference clearTextTraffic = (SwitchPreference)
                    findPreference("key_clear_text_traffic");
            if (NetworkSecurityPolicyUtils.isSupported()) {
                clearTextTraffic.setOnPreferenceChangeListener((preference, value) -> {
                    NetworkSecurityPolicyUtils.setCleartextTrafficPermitted((Boolean) value);
                    return true;
                });
            } else {
                securityCategory.removePreference(clearTextTraffic);
            }
        }

        private void editHomePage(Preference preference) {
            Context context = getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            AlertDialog alertDialog = builder.create();
            LayoutInflater inflater = alertDialog.getLayoutInflater();

            View homepageView = inflater.inflate(R.layout.dialog_homepage_edit,
                    new LinearLayout(context));
            EditText editText = homepageView.findViewById(R.id.homepage_edit_url);
            editText.setText(PrefsUtils.getHomePage(context));

            builder.setTitle(R.string.pref_start_page_dialog_title)
                    .setMessage(R.string.pref_start_page_dialog_message)
                    .setView(homepageView)
                    .setPositiveButton(android.R.string.ok,
                            (dialog, which) -> {
                                String url = editText.getText().toString().isEmpty() ?
                                        getString(R.string.default_home_page) :
                                        editText.getText().toString();
                                PrefsUtils.setHomePage(context, url);
                                preference.setSummary(url);
                            })
                    .setNeutralButton(R.string.pref_start_page_dialog_reset,
                            (dialog, which) -> {
                                String url = getString(R.string.default_home_page);
                                PrefsUtils.setHomePage(context, url);
                                preference.setSummary(url);
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }
}
