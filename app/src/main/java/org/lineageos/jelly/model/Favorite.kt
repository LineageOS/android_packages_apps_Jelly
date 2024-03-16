/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.model

import android.os.Parcel
import android.os.Parcelable

data class Favorite(
    val id: Long,
    val title: String,
    val url: String,
    val color: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(), parcel.readString()!!, parcel.readString()!!, parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(title)
        parcel.writeString(url)
        parcel.writeInt(color)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Favorite> {
        override fun createFromParcel(parcel: Parcel): Favorite {
            return Favorite(parcel)
        }

        override fun newArray(size: Int): Array<Favorite?> {
            return arrayOfNulls(size)
        }
    }
}
