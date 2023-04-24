/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly

import android.content.DialogInterface
import android.os.Bundle
import android.webkit.CookieManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import org.lineageos.jelly.utils.PrefsUtils

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { finish() }
    }

    class MyPreferenceFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
        override fun onCreatePreferences(savedInstance: Bundle?, rootKey: String?) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.settings, rootKey)

            findPreference<Preference>("key_home_page")?.let {
                bindPreferenceSummaryToValue(it, getString(R.string.default_home_page))
            }
            if (resources.getBoolean(R.bool.is_tablet)) {
                findPreference<SwitchPreference>("key_reach_mode")?.let {
                    preferenceScreen.removePreference(it)
                }
            }
        }

        private fun bindPreferenceSummaryToValue(preference: Preference, def: String) {
            preference.onPreferenceChangeListener = this

            onPreferenceChange(
                preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.context)
                    .getString(preference.key, def)
            )
        }

        override fun onPreferenceChange(preference: Preference, value: Any?): Boolean {
            val stringValue = value.toString()

            if (preference is ListPreference) {
                val prefIndex = preference.findIndexOfValue(stringValue)
                if (prefIndex >= 0) {
                    preference.setSummary(preference.entries[prefIndex])
                }
            } else {
                preference.summary = stringValue
            }
            return true
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            return when (preference.key) {
                "key_home_page" -> {
                    editHomePage(preference)
                    true
                }
                "key_cookie_clear" -> {
                    CookieManager.getInstance().removeAllCookies(null)
                    Toast.makeText(
                        preference.context, getString(R.string.pref_cookie_clear_done),
                        Toast.LENGTH_LONG
                    ).show()
                    true
                }
                else -> {
                    super.onPreferenceTreeClick(preference)
                }
            }
        }

        private fun editHomePage(preference: Preference) {
            val builder = AlertDialog.Builder(preference.context)
            val alertDialog = builder.create()
            val inflater = alertDialog.layoutInflater
            val homepageView = inflater.inflate(
                R.layout.dialog_homepage_edit,
                LinearLayout(preference.context)
            )
            val editText = homepageView.findViewById<EditText>(R.id.homepage_edit_url)
            editText.setText(PrefsUtils.getHomePage(preference.context))
            builder.setTitle(R.string.pref_start_page_dialog_title)
                .setMessage(R.string.pref_start_page_dialog_message)
                .setView(homepageView)
                .setPositiveButton(
                    android.R.string.ok
                ) { _: DialogInterface?, _: Int ->
                    val url = editText.text.toString().ifEmpty {
                        getString(R.string.default_home_page)
                    }
                    PrefsUtils.setHomePage(preference.context, url)
                    preference.summary = url
                }
                .setNeutralButton(
                    R.string.pref_start_page_dialog_reset
                ) { _: DialogInterface?, _: Int ->
                    val url = getString(R.string.default_home_page)
                    PrefsUtils.setHomePage(preference.context, url)
                    preference.summary = url
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
}
