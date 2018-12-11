package mvpcoroutines.com.example.mdevillers.mvpcoroutines.model

import android.os.Parcel
import android.os.Parcelable

class Article(
    val title: String,
    val description: String,
    val extract: String,
    val thumbnailUrl: String
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
        parcel.writeString(thumbnailUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR :
        Parcelable.Creator<Article> {
        override fun createFromParcel(parcel: Parcel): Article =
            Article(parcel)
        override fun newArray(size: Int): Array<Article?> = arrayOfNulls(size)
    }
}