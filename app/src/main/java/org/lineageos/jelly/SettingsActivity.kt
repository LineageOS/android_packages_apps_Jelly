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
package org.lineageos.jelly

import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.webkit.CookieManager
import android.widget.LinearLayout
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.dialog_homepage_edit.*
import org.lineageos.jelly.utils.PrefsUtils

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { finish() }
    }

    class MyPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstance: Bundle?) {
            super.onCreate(savedInstance)
            addPreferencesFromResource(R.xml.settings)

            val homePage = findPreference("key_home_page")
            homePage.summary = PrefsUtils.getHomePage(context)
            homePage.setOnPreferenceClickListener { preference ->
                editHomePage(preference)
                true
            }

            val clearCookie = findPreference("key_cookie_clear")
            clearCookie.setOnPreferenceClickListener {
                CookieManager.getInstance().removeAllCookies(null)
                Toast.makeText(context, getString(R.string.pref_cookie_clear_done),
                        Toast.LENGTH_LONG).show()
                true
            }
        }

        private fun editHomePage(preference: Preference) {
            val builder = AlertDialog.Builder(context)
            val alertDialog = builder.create()
            val inflater = alertDialog.layoutInflater

            val homepageView = inflater.inflate(R.layout.dialog_homepage_edit,
                    LinearLayout(context))
            homepage_edit_url.setText(PrefsUtils.getHomePage(context))

            builder.setTitle(R.string.pref_start_page_dialog_title)
                    .setMessage(R.string.pref_start_page_dialog_message)
                    .setView(homepageView)
                    .setPositiveButton(android.R.string.ok
                    ) { _, _ ->
                        val url = if (homepage_edit_url.text.toString().isEmpty())
                            getString(R.string.default_home_page)
                        else
                            homepage_edit_url.text.toString()
                        PrefsUtils.setHomePage(context, url)
                        preference.summary = url
                    }
                    .setNeutralButton(R.string.pref_start_page_dialog_reset
                    ) { _, _ ->
                        val url = getString(R.string.default_home_page)
                        PrefsUtils.setHomePage(context, url)
                        preference.summary = url
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
    }
}
