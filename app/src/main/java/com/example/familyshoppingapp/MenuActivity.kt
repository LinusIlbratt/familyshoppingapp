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
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


class MenuActivity : AppCompatActivity(), ShoppingListFragment.OnListSelectedListener {

    interface ProductAdapterInterface {
        fun updateProductImage(documentId: String, imageUrl: String)
    }

    var productAdapterInterface: ProductAdapterInterface? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val bundle = intent.extras
        user = bundle?.getParcelable("USER_DATA")
            ?: throw IllegalStateException("No USER_DATA provided")

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

                findViewById<Button>(R.id.btn_HiddenGems).visibility = View.VISIBLE
                findViewById<Button>(R.id.btn_createShoppingList).visibility = View.VISIBLE
                findViewById<FrameLayout>(R.id.list_fragment_container).visibility = View.GONE
            }
        }

        val btnSaveGPS = findViewById<Button>(R.id.btn_saveCarGPS)
        btnSaveGPS.setOnClickListener {
            getCurrentLocation { location ->
                location?.let {
                    val parkingLocation = ParkingLocation(
                        user.userId,
                        it.latitude,
                        it.longitude,
                        System.currentTimeMillis()
                    )
                    parkingHero(user, parkingLocation)
                } ?: run {
                    Toast.makeText(
                        this,
                        "Could not find the location, please try again",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


        val btnFindCar = findViewById<Button>(R.id.btn_findCar)
        btnFindCar.setOnClickListener {
            FirebaseFirestore.getInstance().collection("parkingLocations")
                .document(user.userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val parkingLocation = document.toObject(ParkingLocation::class.java)
                        parkingLocation?.let {
                            it.latitude?.let { it1 -> it.longitude?.let { it2 ->
                                showDirectionsInGoogleMap(it1,
                                    it2
                                )
                            } }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to retrieve parking location", Toast.LENGTH_SHORT)
                        .show()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Behörighet beviljad. Anropa getCurrentLocation och gör något med platsen
                    getCurrentLocation { location ->
                        location?.let {
                            // TODO Show the user the saved location on a popup or something?
                        } ?: run {
                            Toast.makeText(
                                this,
                                "Could not find the location, please try again",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {

                    Toast.makeText(
                        this,
                        "Location permission is required to use this feature",
                        Toast.LENGTH_LONG
                    ).show()
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

    private fun getCurrentLocation(callback: (Location?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Behörighet inte beviljad, hantera detta
            callback(null)
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                callback(location)
            } else {
                Toast.makeText(
                    this,
                    "Could not find the location, please try again",
                    Toast.LENGTH_LONG
                ).show()
                callback(null)
            }
        }.addOnFailureListener {
            Toast.makeText(
                this,
                "An error occurred when trying to get the location.",
                Toast.LENGTH_LONG
            ).show()
            callback(null)
        }
    }


    private fun parkingHero(user: User, parkingLocation: ParkingLocation) {

        val parkingDocument =
            FirebaseFirestore.getInstance().collection("parkingLocations").document(user.userId)

        parkingDocument.set(parkingLocation)
            .addOnSuccessListener {
                Toast.makeText(this, "Your car is parked", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // Hantera misslyckandet att spara platsen
            }
    }

    private fun showDirectionsInGoogleMap(
        destinationLatitude: Double,
        destinationLongitude: Double
    ) {
        getCurrentLocation { currentLocation ->
            currentLocation?.let {
                val intentUri = Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&origin=" +
                            "${it.latitude},${it.longitude}&destination=" +
                            "$destinationLatitude,$destinationLongitude&travelmode=walking"
                )

                val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                mapIntent.setPackage("com.google.android.apps.maps")

                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(this, "Google Maps-appen hittades inte", Toast.LENGTH_SHORT)
                        .show()
                }
            } ?: run {
                Toast.makeText(this, "Kunde inte hämta nuvarande plats", Toast.LENGTH_SHORT).show()
            }
        }
    }


    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
    }

}
