/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.lineageos.jelly.R
import org.lineageos.jelly.ext.getParcelableArray
import org.lineageos.jelly.model.History

class HistoryCSVFragment private constructor() : DialogFragment() {
    private val data by lazy {
        arguments?.getParcelableArray("data", History::class) ?: emptyArray()
    }
    private val contentResolver by lazy { requireContext().contentResolver }

    private val createHistoryContract =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
            if (it == null) return@registerForActivityResult

            lifecycleScope.launch(Dispatchers.IO) {
                contentResolver.openOutputStream(it)?.use { stream ->
                    CsvWriter().openAsync(stream) {
                        data.forEach { data ->
                            writeRow(listOf(data.title, data.url, data.timestamp))
                        }
                    }
                }
            }
        }

    private val openHistoryContract =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it == null) return@registerForActivityResult

            lifecycleScope.launch(Dispatchers.IO) {
                contentResolver.openInputStream(it)?.use { stream ->
                    CsvReader().openAsync(stream) {
                        readAllAsSequence().forEach { row ->
                            HistoryProvider.addOrUpdateItem(
                                contentResolver, row[0], row[1], row[2].toLong()
                            )
                        }
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.csv_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.export_csv).setOnClickListener {
            createHistoryContract.launch("jelly_history.csv")
        }
        view.findViewById<MaterialButton>(R.id.import_csv).setOnClickListener {
            openHistoryContract.launch(arrayOf("text/*"))
        }
    }

    companion object {
        fun create(history: Array<History>): HistoryCSVFragment {
            val fragment = HistoryCSVFragment()
            val args = Bundle()
            args.putParcelableArray("data", history)
            fragment.arguments = args
            return fragment
        }
    }
}
