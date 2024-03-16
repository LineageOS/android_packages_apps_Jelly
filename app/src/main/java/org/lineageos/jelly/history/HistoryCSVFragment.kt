/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider.getUriForFile
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.jelly.BuildConfig
import org.lineageos.jelly.R
import org.lineageos.jelly.ext.getParcelableArray
import org.lineageos.jelly.model.History
import java.io.File

class HistoryCSVFragment private constructor() : DialogFragment() {
    private val history by lazy {
        arguments?.getParcelableArray("history", History::class) ?: emptyArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // this fragment will be displayed in a dialog
        showsDialog = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.history_csv_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.history_export_csv).setOnClickListener {
            lifecycleScope.launch {
                val file = withContext(Dispatchers.IO) {
                    // Create a temporary file and write the history to it
                    val file = File(
                        requireContext().cacheDir, "jelly_history.csv"
                    ).apply {
                        createNewFile()
                    }
                    CsvWriter().openAsync(file) {
                        writeRow(listOf("title", "url", "timestamp"))
                        history.forEach {
                            writeRow(listOf(it.title, it.url, it.timestamp))
                        }
                    }
                    file
                }
                val uri = getUriForFile(requireContext(), PROVIDER, file)
                withContext(Dispatchers.Main) {
                    // Share the file
                    val intent = ShareCompat.IntentBuilder(requireActivity()).setType("text/csv")
                        .setStream(uri).intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(
                        Intent.createChooser(
                            intent, getString(R.string.history_csv_export)
                        )
                    )
                }
            }
        }
        view.findViewById<MaterialButton>(R.id.history_import_csv).setOnClickListener {}
    }

    companion object {
        private const val PROVIDER = "${BuildConfig.APPLICATION_ID}.fileprovider"

        fun create(history: Array<History>): HistoryCSVFragment {
            val fragment = HistoryCSVFragment()
            val args = Bundle()
            args.putParcelableArray("history", history)
            fragment.arguments = args
            return fragment
        }
    }
}