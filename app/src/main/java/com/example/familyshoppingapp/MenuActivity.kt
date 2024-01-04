package com.example.familyshoppingapp

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest


class MenuActivity : AppCompatActivity(), ShoppingListFragment.OnListSelectedListener {

    interface ProductAdapterInterface {
        fun updateProductImage(documentId: String, imageUrl: String)
    }

    var productAdapterInterface: ProductAdapterInterface? = null

    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val bundle = intent.extras
        user = bundle?.getParcelable("USER_DATA") ?: throw IllegalStateException("No USER_DATA provided")

        val btnHiddenGem = findViewById<Button>(R.id.btn_HiddenGems)
        btnHiddenGem.setOnClickListener {
            showHiddenGemsFragment()
        }

        val btnCreateShoppingList = findViewById<Button>(R.id.btn_createShoppingList)
        btnCreateShoppingList.setOnClickListener {
            showShoppingListFragment()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                // Inga fragment i backstack, visa knapparna och dölj fragmentcontainern
                findViewById<Button>(R.id.btn_HiddenGems).visibility = View.VISIBLE
                findViewById<Button>(R.id.btn_createShoppingList).visibility = View.VISIBLE
                findViewById<FrameLayout>(R.id.list_fragment_container).visibility = View.GONE
            }
        }

        val btnSaveGPS = findViewById<Button>(R.id.btn_saveCarGPS)
        btnSaveGPS.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                // If access is not granted, ask for it
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)
            } else {
                // If access is already granted, continue with getting the position
                getCurrentLocation()
            }
        }

        val btnFindCar = findViewById<Button>(R.id.btn_findCar)
        btnFindCar.setOnClickListener {
            Log.d("!!!", "Find Car Button Pressed")
            // logic
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
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                    getCurrentLocation()
                } else {
                    // TODO Handle access declined
                }
            }
        }
    }



    private fun showHiddenGemsFragment() {
        findViewById<Button>(R.id.btn_HiddenGems).visibility = View.GONE
        findViewById<Button>(R.id.btn_createShoppingList).visibility = View.GONE

        findViewById<FrameLayout>(R.id.hidden_gem_fragment_container).visibility = View.VISIBLE

        val hiddenGemsFragment = HiddenGemsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.hidden_gem_fragment_container, hiddenGemsFragment)
            .addToBackStack(null)
            .commit()
    }


    private fun showShoppingListFragment() {
        findViewById<Button>(R.id.btn_HiddenGems).visibility = View.GONE
        findViewById<Button>(R.id.btn_createShoppingList).visibility = View.GONE

        findViewById<FrameLayout>(R.id.list_fragment_container).visibility = View.VISIBLE

        val fragment = ShoppingListFragment.newInstance(user).also {
            it.setOnListSelectedListener(this)
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.list_fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onListSelected(listId: String, listTitle: String) {

        val itemListFragment = ProductListFragment.newInstance(listId, listTitle)
        supportFragmentManager.beginTransaction()
            .replace(R.id.list_fragment_container, itemListFragment)
            .addToBackStack(null)
            .commit()
    }

    fun getCurrentLocation() {

    }

    fun parkingHero(user: User, parkingLocation: ParkingLocation) {
        val userDocument = FirebaseFirestore.getInstance().collection("users").document(user.userId)

        userDocument.set(mapOf("parkingLocation" to parkingLocation))
            .addOnSuccessListener {
                // success
            }
            .addOnFailureListener {
                // fail
            }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
    }

}
