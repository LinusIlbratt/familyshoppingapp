package com.example.familyshoppingapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParkingLocation(
    val userId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long? = null
) : Parcelable
