package com.example.familyshoppingapp
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HiddenGem(
    val name: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String? = null,
    val tag: String = ""
) : Parcelable
