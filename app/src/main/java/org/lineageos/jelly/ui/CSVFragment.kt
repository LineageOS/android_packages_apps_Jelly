/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ui

import android.os.Bundle
import android.os.Parcelable
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
import org.lineageos.jelly.ext.getParcelableArrayList
import org.lineageos.jelly.utils.AddOrUpdate
import kotlin.reflect.KClass

class CSVFragment<T : Parcelable>(
    private val kind: String,
    private val provider: AddOrUpdate,
    private val dataToList: (T) -> List<Any?>,
    private val clazz: KClass<T>
) : DialogFragment() {
    private val data by lazy {
        arguments?.getParcelableArrayList("data", clazz) ?: listOf()
    }
    private val contentResolver by lazy { requireContext().contentResolver }

    private val createHistoryContract =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
            if (it == null) return@registerForActivityResult

            lifecycleScope.launch(Dispatchers.IO) {
                contentResolver.openOutputStream(it)?.use { stream ->
                    CsvWriter().openAsync(stream) {
                        data.forEach { data ->
                            writeRow(dataToList(data))
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
                            provider.addOrUpdateItem(
                                contentResolver, row[0], row[1], row[2]
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
            createHistoryContract.launch("jelly_${kind}.csv")
        }
        view.findViewById<MaterialButton>(R.id.import_csv).setOnClickListener {
            openHistoryContract.launch(arrayOf("text/*"))
        }
    }

    companion object {
        fun <T : Parcelable> create(
            kind: String,
            provider: AddOrUpdate,
            data: List<T>,
            dataToList: (T) -> List<Any?>,
            clazz: KClass<T>
        ): CSVFragment<T> {
            val fragment = CSVFragment(kind, provider, dataToList, clazz)
            val args = Bundle()
            args.putParcelableArrayList("data", ArrayList(data))
            fragment.arguments = args
            return fragment
        }
    }
}
