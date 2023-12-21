package com.example.familyshoppingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog

class MenuActivity : AppCompatActivity() {

    private lateinit var user: User
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val bundle = intent.extras
        user = bundle?.getParcelable("USER_DATA") ?: throw IllegalStateException("No USER_DATA provided")

        val btnCreateShoppingList = findViewById<Button>(R.id.btn_createShoppingList)
        btnCreateShoppingList.setOnClickListener {
            showShoppingListFragment()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                // Inga fragment i backstack, visa knapparna och dölj fragmentcontainern
                findViewById<Button>(R.id.btnFindStores).visibility = View.VISIBLE
                findViewById<Button>(R.id.btn_createShoppingList).visibility = View.VISIBLE
                findViewById<FrameLayout>(R.id.shoppingList_fragment_container).visibility = View.GONE
            }
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
        } else {
            AlertDialog.Builder(this)
                .setMessage("Do you want to exit?")
                .setPositiveButton("Yes") { _, _ -> finish() }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun showShoppingListFragment() {
        findViewById<Button>(R.id.btnFindStores).visibility = View.GONE
        findViewById<Button>(R.id.btn_createShoppingList).visibility = View.GONE

        findViewById<FrameLayout>(R.id.shoppingList_fragment_container).visibility = View.VISIBLE

        val fragment = ShoppingListFragment.newInstance(user)
        supportFragmentManager.beginTransaction()
            .replace(R.id.shoppingList_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}