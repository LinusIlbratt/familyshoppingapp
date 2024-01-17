package com.example.familyshoppingapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore


class MenuActivity : AppCompatActivity(), ShoppingListFragment.OnListSelectedListener,
    OnMapReadyCallback {

    interface ProductAdapterInterface {
        fun updateProductImage(documentId: String, imageUrl: String)
    }

    var productAdapterInterface: ProductAdapterInterface? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var user: User
    private var closeButton: Button? = null
    private var saveLocationButton: Button? = null
    private var userLocationMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        getUserData()
        initUIComponents()
        setupBackStackListener()
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

    override fun onMapReady(googleMap: GoogleMap) {
        getUserLastKnownLocation(googleMap)

        getCurrentLocation { location ->
            location?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                googleMap.apply {
                    moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f)) // Zoom level
                    userLocationMarker = addMarker(MarkerOptions().position(userLatLng).title("Your Position"))

                    // Check if location permission is granted
                    if (ActivityCompat.checkSelfPermission(this@MenuActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this@MenuActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        isMyLocationEnabled = true // Enable the blue dot for user's location
                    } else {
                        // TODO: Consider calling ActivityCompat#requestPermissions here to request the missing permissions
                    }
                }
            } ?: run {
                // TODO: Handle if user location isn't possible
            }
        }
    }

    private fun getUserLastKnownLocation(googleMap: GoogleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO Handle if access is denied
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {

                val userLatLng = LatLng(it.latitude, it.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            } ?: run {

            }
        }
    }


    private fun getUserData() {
        val bundle = intent.extras
        user = bundle?.getParcelable("USER_DATA")
            ?: throw IllegalStateException("No USER_DATA provided")
    }

    private fun initUIComponents() {
        // add all UI components here

        val btnHiddenGem = findViewById<ImageButton>(R.id.btn_HiddenGems)
        btnHiddenGem.setOnClickListener {
            showHiddenGemsFragment()
        }

        val btnCreateShoppingList = findViewById<ImageButton>(R.id.btn_createShoppingList)
        btnCreateShoppingList.setOnClickListener {
            showShoppingListFragment()
        }

        val btnFindHiddenGem = findViewById<ImageButton>(R.id.btn_findHiddenGems)
        btnFindHiddenGem.setOnClickListener {
            showSearchHiddenGemsFragment()
        }

        val btnSaveGPS = findViewById<ImageButton>(R.id.btn_saveCarGPS)
        btnSaveGPS.setOnClickListener {
            //saveParkingLocation()
            openParkingMap()
        }

        val btnFindCar = findViewById<ImageButton>(R.id.btn_findCar)
        btnFindCar.setOnClickListener {
            findParkingLocation()
        }

    }

    private fun setupBackStackListener() {
        val menuHeaderText = findViewById<TextView>(R.id.menu_titleText)
        val hiddenGemTextView = findViewById<TextView>(R.id.hidden_gems_textView)
        val createShoppingListTextView = findViewById<TextView>(R.id.create_shopping_list_textView)
        val findHiddenGemTextView = findViewById<TextView>(R.id.search_hidden_gem_TextView)

        val btnHiddenGem = findViewById<ImageButton>(R.id.btn_HiddenGems)
        val btnCreateShoppingList = findViewById<ImageButton>(R.id.btn_createShoppingList)
        val btnFindHiddenGem = findViewById<ImageButton>(R.id.btn_findHiddenGems)
        val listFragmentContainer = findViewById<FrameLayout>(R.id.list_fragment_container)

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                menuHeaderText.visibility = View.VISIBLE
                hiddenGemTextView.visibility = View.VISIBLE
                createShoppingListTextView.visibility = View.VISIBLE
                findHiddenGemTextView.visibility = View.VISIBLE

                btnHiddenGem.visibility = View.VISIBLE
                btnCreateShoppingList.visibility = View.VISIBLE
                btnFindHiddenGem.visibility = View.VISIBLE
                listFragmentContainer.visibility = View.GONE
            }
        }
    }


    private fun showHiddenGemsFragment() {
        findViewById<TextView>(R.id.menu_titleText).visibility = View.GONE
        findViewById<TextView>(R.id.hidden_gems_textView).visibility = View.GONE
        findViewById<TextView>(R.id.create_shopping_list_textView).visibility = View.GONE
        findViewById<TextView>(R.id.search_hidden_gem_TextView).visibility = View.GONE

        findViewById<ImageButton>(R.id.btn_HiddenGems).visibility = View.GONE
        findViewById<ImageButton>(R.id.btn_createShoppingList).visibility = View.GONE
        findViewById<ImageButton>(R.id.btn_findHiddenGems).visibility = View.GONE

        findViewById<FrameLayout>(R.id.hidden_gem_fragment_container).visibility = View.VISIBLE

        val hiddenGemsFragment = HiddenGemsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.hidden_gem_fragment_container, hiddenGemsFragment)
            .addToBackStack(null)
            .commit()
    }


    private fun showShoppingListFragment() {
        findViewById<TextView>(R.id.menu_titleText).visibility = View.GONE
        findViewById<TextView>(R.id.hidden_gems_textView).visibility = View.GONE
        findViewById<TextView>(R.id.create_shopping_list_textView).visibility = View.GONE
        findViewById<TextView>(R.id.search_hidden_gem_TextView).visibility = View.GONE

        findViewById<ImageButton>(R.id.btn_HiddenGems).visibility = View.GONE
        findViewById<ImageButton>(R.id.btn_createShoppingList).visibility = View.GONE
        findViewById<ImageButton>(R.id.btn_findHiddenGems).visibility = View.GONE

        findViewById<FrameLayout>(R.id.list_fragment_container).visibility = View.VISIBLE

        val shoppingListFragment = ShoppingListFragment.newInstance(user).also {
            it.setOnListSelectedListener(this)
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.list_fragment_container, shoppingListFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showSearchHiddenGemsFragment() {
        findViewById<TextView>(R.id.menu_titleText).visibility = View.GONE
        findViewById<TextView>(R.id.hidden_gems_textView).visibility = View.GONE
        findViewById<TextView>(R.id.create_shopping_list_textView).visibility = View.GONE
        findViewById<TextView>(R.id.search_hidden_gem_TextView).visibility = View.GONE

        findViewById<ImageButton>(R.id.btn_HiddenGems).visibility = View.GONE
        findViewById<ImageButton>(R.id.btn_createShoppingList).visibility = View.GONE
        findViewById<ImageButton>(R.id.btn_findHiddenGems).visibility = View.GONE

        findViewById<FrameLayout>(R.id.search_gem_fragment_container).visibility = View.VISIBLE
        findViewById<FrameLayout>(R.id.hidden_gem_fragment_container).visibility = View.GONE

        val searchHiddenGemsFragment = SearchHiddenGemsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.search_gem_fragment_container, searchHiddenGemsFragment)
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted. Request for permission.
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION)
            return
        }

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000 //
            fastestInterval = 2000
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.locations
                    .filterNotNull()
                    .minByOrNull { it.accuracy }

                location?.let {
                    Log.d("!!!", "Accuracy: ${it.accuracy}")
                    callback(it)
                    fusedLocationProviderClient.removeLocationUpdates(this)
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }


    private fun openParkingMap() {
        findViewById<FrameLayout>(R.id.map_fragment_container).visibility = View.VISIBLE

        val mapFragment = SupportMapFragment.newInstance().apply {
            getMapAsync(this@MenuActivity)
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_fragment_container, mapFragment)
            .commit()

        addMapButtons()
    }

    private fun addMapButtons() {
        val frameLayout = findViewById<FrameLayout>(R.id.map_fragment_container)


        closeButton = Button(this).apply {
            text = "Close Map"
            setOnClickListener {
                removeMapFragmentAndButtons()
            }
            frameLayout.addView(this, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.END
            })
        }


        saveLocationButton = Button(this).apply {
            text = "Save Location"
            setOnClickListener {
                saveParkingLocation()
            }
            frameLayout.addView(this, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.END
            })
        }
    }

    private fun removeMapFragmentAndButtons() {
        val frameLayout = findViewById<FrameLayout>(R.id.map_fragment_container)

        closeButton?.let {
            frameLayout.removeView(it)
            closeButton = null
        }
        saveLocationButton?.let {
            frameLayout.removeView(it)
            saveLocationButton = null
        }

        supportFragmentManager.findFragmentById(R.id.map_fragment_container)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }




    private fun saveParkingLocation() {
        userLocationMarker?.let { marker ->
            val latitude = marker.position.latitude
            val longitude = marker.position.longitude

            val parkingLocation = ParkingLocation(
                userId = user.userId,
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis()
            )

            parkingHero(user, parkingLocation)
        }
    }



    private fun findParkingLocation() {
        FirebaseFirestore.getInstance().collection("parkingLocations")
            .document(user.userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val parkingLocation = document.toObject(ParkingLocation::class.java)
                    parkingLocation?.let {
                        it.latitude?.let { it1 ->
                            it.longitude?.let { it2 ->
                                showDirectionsInGoogleMap(it1, it2)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to retrieve parking location", Toast.LENGTH_SHORT)
                    .show()
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
                    Toast.makeText(this, "Could not find the Google Maps App", Toast.LENGTH_SHORT)
                        .show()
                }
            } ?: run {
                Toast.makeText(this, "Could not fetch the current location", Toast.LENGTH_SHORT).show()
            }
        }
    }


    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
    }

}
