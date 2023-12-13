package com.example.familyshoppingapp

data class ShoppingLists(
    var name: String? = null,
    var category: String? = null,
    var documentId: String? = null,
    var isCardEmpty: Boolean = false,
    var members: List<String> = listOf()
)
