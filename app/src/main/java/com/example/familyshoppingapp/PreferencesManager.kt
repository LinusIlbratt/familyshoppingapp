package com.example.familyshoppingapp

import android.content.Context

object PreferencesManager {
    private const val PREFS_NAME = "ProductListPrefs"

    fun saveProductOrder(context: Context, productIds: List<String>) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("productOrder", productIds.joinToString(","))
            apply()
        }
    }

    fun loadProductOrder(context: Context): List<String>? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val orderString = sharedPreferences.getString("productOrder", null)
        return orderString?.split(",")
    }
}
