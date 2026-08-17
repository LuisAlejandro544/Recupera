package com.example.shizuku.model

import android.os.Parcel
import android.os.Parcelable

data class ShizukuScannedItem(
    val absolutePath: String,
    val fileName: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String,
    val sourcePackage: String,
    val isFromVendorTrash: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        absolutePath = parcel.readString() ?: "",
        fileName = parcel.readString() ?: "",
        sizeBytes = parcel.readLong(),
        lastModified = parcel.readLong(),
        mimeType = parcel.readString() ?: "application/octet-stream",
        sourcePackage = parcel.readString() ?: "",
        isFromVendorTrash = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(absolutePath)
        parcel.writeString(fileName)
        parcel.writeLong(sizeBytes)
        parcel.writeLong(lastModified)
        parcel.writeString(mimeType)
        parcel.writeString(sourcePackage)
        parcel.writeByte(if (isFromVendorTrash) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ShizukuScannedItem> {
        override fun createFromParcel(parcel: Parcel): ShizukuScannedItem = ShizukuScannedItem(parcel)
        override fun newArray(size: Int): Array<ShizukuScannedItem?> = arrayOfNulls(size)
    }
}
