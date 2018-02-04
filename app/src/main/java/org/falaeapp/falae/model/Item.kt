package org.falaeapp.falae.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by marcelo on 4/11/17.
 */

data class Item(val name: String, var imgSrc: String, val speech: String, val category: Category, val linkTo: String?, val isPrivate: Boolean) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            Category.values()[parcel.readInt()],
            parcel.readString(),
            parcel.readByte() != 0.toByte())

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(this.name)
        dest.writeString(this.imgSrc)
        dest.writeString(this.speech)
        dest.writeInt(this.category.ordinal)
        dest.writeString(this.linkTo)
        dest.writeByte((if (isPrivate) 1 else 0).toByte())
    }

    companion object {

        val CREATOR: Parcelable.Creator<Item> = object : Parcelable.Creator<Item> {
            override fun createFromParcel(source: Parcel): Item = Item(source)

            override fun newArray(size: Int): Array<Item?> = arrayOfNulls(size)
        }
    }
}
