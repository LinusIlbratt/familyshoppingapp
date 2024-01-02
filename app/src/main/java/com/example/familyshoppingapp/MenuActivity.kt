package com.example.familyshoppingapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog

//class MenuActivity : AppCompatActivity(), ShoppingListFragment.OnListSelectedListener {
//
//    interface ProductAdapterInterface {
//        fun updateProductImage(documentId: String, imageUrl: String)
//    }
//
//    var productAdapterInterface: ProductAdapterInterface? = null
//
//    private lateinit var user: User
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_menu)
//
//        val bundle = intent.extras
//        user = bundle?.getParcelable("USER_DATA") ?: throw IllegalStateException("No USER_DATA provided")
//
//        val btnCreateShoppingList = findViewById<Button>(R.id.btn_createShoppingList)
//        btnCreateShoppingList.setOnClickListener {
//            showShoppingListFragment()
//        }
//
//        supportFragmentManager.addOnBackStackChangedListener {
//            if (supportFragmentManager.backStackEntryCount == 0) {
//                // Inga fragment i backstack, visa knapparna och dölj fragmentcontainern
//                findViewById<Button>(R.id.btnFindStores).visibility = View.VISIBLE
//                findViewById<Button>(R.id.btn_createShoppingList).visibility = View.VISIBLE
//                findViewById<FrameLayout>(R.id.list_fragment_container).visibility = View.GONE
//            }
//        }
//    }
//
//    override fun onBackPressed() {
//        if (supportFragmentManager.backStackEntryCount > 0) {
//            super.onBackPressed()
//        } else {
//            AlertDialog.Builder(this)
//                .setMessage("Do you want to exit?")
//                .setPositiveButton("Yes") { _, _ -> finish() }
//                .setNegativeButton("No", null)
//                .show()
//        }
//    }
//
//    private fun showShoppingListFragment() {
//        findViewById<Button>(R.id.btnFindStores).visibility = View.GONE
//        findViewById<Button>(R.id.btn_createShoppingList).visibility = View.GONE
//
//        findViewById<FrameLayout>(R.id.list_fragment_container).visibility = View.VISIBLE
//
//        val fragment = ShoppingListFragment.newInstance(user).also {
//            it.setOnListSelectedListener(this)
//        }
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.list_fragment_container, fragment)
//            .addToBackStack(null)
//            .commit()
//    }
//
//    override fun onListSelected(listId: String, listTitle: String) {
//        // Starta ItemListFragment med vald lista
//        val itemListFragment = ProductListFragment.newInstance(listId, listTitle)
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.list_fragment_container, itemListFragment)
//            .addToBackStack(null)
//            .commit()
//    }
//}
