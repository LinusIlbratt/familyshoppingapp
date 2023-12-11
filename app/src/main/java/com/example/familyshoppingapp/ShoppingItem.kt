package com.example.familyshoppingapp

import com.google.firebase.firestore.DocumentId

data class ShoppingItem(
    var name: String? = null,
    var isAdded: Boolean = false,
    var quantity: Int = 1,
    var documentId: String? = null
)