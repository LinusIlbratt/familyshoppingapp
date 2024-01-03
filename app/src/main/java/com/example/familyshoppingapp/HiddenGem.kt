package com.example.familyshoppingapp

data class HiddenGem(
    val name: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String? = null,
    val tag: String = ""
) {
    // empty constructor for Firebase deserialization
    constructor() : this("", "", 0.0, 0.0, null, "")
}
