package com.example.familyshoppingapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParkingLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
) : Parcelable
