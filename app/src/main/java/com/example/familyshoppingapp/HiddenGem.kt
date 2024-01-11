package com.example.familyshoppingapp
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HiddenGem(
    val id: String = "",
    val name: String = "",
    var userId: String? = null,
    var description: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var imageUrl: String? = null,
    val tag: String = "",
    var isShared: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = listOf(),
    var rating: Float = 0.0f,
    var visitCount: Int = 0
) : Parcelable
