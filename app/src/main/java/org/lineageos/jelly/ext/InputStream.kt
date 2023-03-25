/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.ext

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

fun InputStream.readString(
    encoding: String
) = InputStreamReader(this, encoding).use { inputStreamReader ->
    BufferedReader(inputStreamReader).use { bufferedReader ->
        val result = StringBuilder()
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            result.append(line)
        }
        bufferedReader.close()
        result.toString()
    }
}
