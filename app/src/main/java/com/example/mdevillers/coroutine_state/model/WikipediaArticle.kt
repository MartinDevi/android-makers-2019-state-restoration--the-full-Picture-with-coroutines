package com.example.mdevillers.coroutine_state.model

import android.os.Parcel
import android.os.Parcelable

class WikipediaArticle(
    val title: String,
    val description: String,
    val extract: String,
    val imageUrl: String
): Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(extract)
        parcel.writeString(imageUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR :
        Parcelable.Creator<WikipediaArticle> {
        override fun createFromParcel(parcel: Parcel): WikipediaArticle =
            WikipediaArticle(parcel)
        override fun newArray(size: Int): Array<WikipediaArticle?> = arrayOfNulls(size)
    }
}