package com.example.mdevillers.coroutine_state.model

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable

data class WikipediaImage(
    val bitmap: Bitmap
): Parcelable {
    constructor(parcel: Parcel) : this(parcel.readParcelable<Bitmap>(Bitmap::class.java.classLoader)!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(bitmap, flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WikipediaImage> {
        override fun createFromParcel(parcel: Parcel): WikipediaImage = WikipediaImage(parcel)
        override fun newArray(size: Int): Array<WikipediaImage?> = arrayOfNulls(size)
    }
}