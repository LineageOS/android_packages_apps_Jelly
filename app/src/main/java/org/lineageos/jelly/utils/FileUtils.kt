/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.utils

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object FileUtils {
    fun readStringFromStream(inputStream: InputStream,
                             encoding: String): String {
        val reader = BufferedReader(InputStreamReader(inputStream, encoding))
        val result = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            result.append(line)
        }
        return result.toString()
    }
}
