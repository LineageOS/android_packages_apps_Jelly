/*
 * SPDX-FileCopyrightText: 2020-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
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
import com.google.android.material.checkbox.MaterialCheckBox
import org.lineageos.jelly.utils.SharedPreferencesExt
import kotlin.reflect.safeCast

class SettingsActivity : AppCompatActivity() {
    // Views
    private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
        private val sharedPreferencesExt by lazy { SharedPreferencesExt(requireContext()) }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            setDivider(ColorDrawable(Color.TRANSPARENT))
            setDividerHeight(0)
        }

        override fun onCreatePreferences(savedInstance: Bundle?, rootKey: String?) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.settings, rootKey)

            findPreference<Preference>("key_home_page")?.let {
                it.onPreferenceChangeListener = this
                it.summary = homePagePrefSummary()
            }
            if (resources.getBoolean(R.bool.is_tablet)) {
                findPreference<SwitchPreference>("key_reach_mode")?.let {
                    preferenceScreen.removePreference(it)
                }
            }
        }

        override fun onPreferenceChange(preference: Preference, value: Any?): Boolean {
            val stringValue = value.toString()

            ListPreference::class.safeCast(preference)?.also {
                val prefIndex = it.findIndexOfValue(stringValue)
                if (prefIndex >= 0) {
                    preference.summary = it.entries[prefIndex]
                }
            } ?: run {
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
            val homepageUrlEditText = homepageView.findViewById<EditText>(R.id.homepageUrlEditText)
            homepageUrlEditText.setText(sharedPreferencesExt.homePage)
            val homepageAutoload = homepageView.findViewById<MaterialCheckBox>(
                R.id.homePageAutoloadCheckBox
            )
            homepageAutoload.isChecked = sharedPreferencesExt.homePageAutoload
            homepageAutoload.setOnCheckedChangeListener { _, checked ->
                homepageUrlEditText.isEnabled = checked
            }
            homepageUrlEditText.isEnabled = homepageAutoload.isChecked
            builder.setTitle(R.string.pref_start_page_dialog_title)
                .setMessage(R.string.pref_start_page_dialog_message)
                .setView(homepageView)
                .setPositiveButton(
                    android.R.string.ok
                ) { _: DialogInterface?, _: Int ->
                    val url = homepageUrlEditText.text.toString().ifEmpty {
                        sharedPreferencesExt.defaultHomePage
                    }
                    sharedPreferencesExt.homePage = url
                    sharedPreferencesExt.homePageAutoload = homepageAutoload.isChecked
                    preference.summary = homePagePrefSummary()
                }
                .setNeutralButton(
                    R.string.pref_start_page_dialog_reset
                ) { _: DialogInterface?, _: Int ->
                    val url = sharedPreferencesExt.defaultHomePage
                    val autoload = sharedPreferencesExt.defaultHomePageAutoload
                    sharedPreferencesExt.homePage = url
                    sharedPreferencesExt.homePageAutoload = autoload
                    preference.summary = homePagePrefSummary()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        private fun homePagePrefSummary() =
            if (sharedPreferencesExt.homePageAutoload) {
                sharedPreferencesExt.homePage
            } else {
                getString(R.string.pref_start_page_autoload_disabled)
            }
    }
}
