package com.example.familyshoppingapp

sealed class SectionItem {
    data class Header(val title: String) : SectionItem()
    data class Item(val hiddenGem: HiddenGem) : SectionItem()
}
