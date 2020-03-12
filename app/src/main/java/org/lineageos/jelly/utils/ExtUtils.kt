package org.lineageos.jelly.utils

import android.content.ContentProvider

fun ContentProvider.requireContext() = context ?: throw Exception()