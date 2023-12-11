package com.example.familyshoppingapp

import com.google.firebase.firestore.DocumentId

data class ShoppingItem(
    val name: String? = null,
    var isAdded: Boolean = false,
    var documentId: String? = null
)
