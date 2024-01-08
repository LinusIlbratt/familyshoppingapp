package com.example.familyshoppingapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import com.google.android.gms.location.LocationServices
import android.location.Location
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng


class HiddenGemDetailFragment : Fragment() {
    private lateinit var hiddenGem: HiddenGem
    private lateinit var descriptionEditText: EditText
    private lateinit var editButton: Button
    private lateinit var saveButton: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var shareButton: Button
    private lateinit var stopSharingButton: Button
    private lateinit var photoHolder: ImageView
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            hiddenGem = it.getParcelable(HIDDEN_GEM)
                ?: throw IllegalArgumentException("Hidden Gem is required")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("detail", "onCreateView called")
        return inflater.inflate(R.layout.fragment_hidden_gem_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        loadAndSetHiddenGemSharingStatus()

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Kameratillstånd beviljades
            } else {
                // Kameratillstånd nekades
            }
        }
    }


    private fun initViews(view: View) {

        photoHolder = view.findViewById(R.id.hidden_gem_detail_photoHolder)

        view.findViewById<TextView>(R.id.detail_titel).text = hiddenGem.name

        shareButton = view.findViewById(R.id.btn_share_public)
        stopSharingButton = view.findViewById(R.id.btn_stop_sharing_public)

        descriptionEditText = view.findViewById(R.id.detail_description_edit)
        editButton = view.findViewById(R.id.btn_edit_desc)
        saveButton = view.findViewById(R.id.btn_save_desc)

        descriptionEditText.setText(hiddenGem.description)
        descriptionEditText.background = null
        descriptionEditText.isFocusable = false
        descriptionEditText.isFocusableInTouchMode = false
        descriptionEditText.isCursorVisible = false

        val backArrow = view.findViewById<ImageView>(R.id.backArrow)
        backArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupListeners() {

        photoHolder.setOnClickListener {
            onPhotoHolderClicked()
        }

        editButton.setOnClickListener {
            descriptionEditText.background =
                ResourcesCompat.getDrawable(resources, R.drawable.edit_text_background, null)
            descriptionEditText.isFocusable = true
            descriptionEditText.isFocusableInTouchMode = true
            descriptionEditText.isCursorVisible = true

            descriptionEditText.setSelection(descriptionEditText.text.length)
            descriptionEditText.requestFocus()

            saveButton.visibility = View.VISIBLE
            editButton.visibility = View.GONE
        }

        saveButton.setOnClickListener {
            hiddenGem.description = descriptionEditText.text.toString()
            saveHiddenGemDesc(hiddenGem)

            descriptionEditText.background = null
            descriptionEditText.isFocusable = false
            descriptionEditText.isFocusableInTouchMode = false
            descriptionEditText.isCursorVisible = false
            saveButton.visibility = View.GONE
            editButton.visibility = View.VISIBLE
        }

        val saveGpsButton = view?.findViewById<Button>(R.id.btn_save_gps)
        if (saveGpsButton != null) {
            saveGpsButton.setOnClickListener {
                checkAndRequestLocationPermission()
            }
        }
        val showGpsButton = view?.findViewById<Button>(R.id.btn_show_gps)
        if (showGpsButton != null) {
            showGpsButton.setOnClickListener {
                showDirectionsInGoogleMap()
            }
        }

        shareButton.setOnClickListener {
            toggleHiddenGemSharing(true)
        }

        stopSharingButton.setOnClickListener {
            toggleHiddenGemSharing(false)
        }
    }

    private fun loadAndSetHiddenGemSharingStatus() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("hidden_gems").document(hiddenGem.id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val updatedHiddenGem = document.toObject(HiddenGem::class.java)
                    updatedHiddenGem?.let {
                        it.isShared = document.getBoolean("isShared") ?: false
                        this.hiddenGem = it
                        updateButtonsBasedOnSharingStatus(hiddenGem.isShared)
                        Log.d("HiddenGemDetailFragment", "Loaded HiddenGem isShared status: ${hiddenGem.isShared}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("HiddenGemDetailFragment", "Error loading Hidden Gem details", e)
            }
    }

    private fun onPhotoHolderClicked() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted
            showPhotoHolderDialogPopup()
        } else {
            // Ask for permission
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showPhotoHolderDialogPopup() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.photo_holder_dialog_popup, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        // Handle click on symbols
        val openGallery = dialogView.findViewById<ImageView>(R.id.open_gallery_icon)
        openGallery.setOnClickListener {

        }

        val openCamera = dialogView.findViewById<ImageView>(R.id.open_camera_icon)
        openCamera.setOnClickListener {

        }

        dialog.show()
    }


    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {


            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {

            saveCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                saveCurrentLocation()
            } else {

            }
        }
    }

    private fun saveCurrentLocation() {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }

        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    hiddenGem.latitude = location.latitude
                    hiddenGem.longitude = location.longitude

                    saveHiddenGemGeneralData(hiddenGem)
                }
            }
            .addOnFailureListener { e ->

            }
    }

    private fun saveHiddenGemGeneralData(hiddenGem: HiddenGem) {
        if (hiddenGem.id.isEmpty()) {
            Log.w("HiddenGemDetailFragment", "Hidden Gem ID is empty")
            Toast.makeText(context, "Error: Hidden Gem ID is missing", Toast.LENGTH_SHORT).show()
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val hiddenGemsCollection = firestore.collection("hidden_gems")

        hiddenGemsCollection.document(hiddenGem.id).set(hiddenGem)
            .addOnSuccessListener {

                Toast.makeText(context, "Hidden Gem updated successfully", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    context,
                    "Error updating Hidden Gem: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun saveHiddenGemDesc(hiddenGem: HiddenGem) {
        if (hiddenGem.id.isEmpty()) {
            Log.w("HiddenGemDetailFragment", "Hidden Gem ID is empty")
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val hiddenGemsCollection = firestore.collection("hidden_gems")

        hiddenGemsCollection.document(hiddenGem.id).set(hiddenGem)
            .addOnSuccessListener {

                Toast.makeText(context, "Description updated successfully", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    context,
                    "Error updating description: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showDirectionsInGoogleMap() {
        getCurrentLocation { location ->
            location?.let { currentLocation ->
                val destination = LatLng(hiddenGem.latitude, hiddenGem.longitude)

                val intentUri = Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&origin=" +
                            "${currentLocation.latitude},${currentLocation.longitude}&destination=" +
                            "${destination.latitude},${destination.longitude}&travelmode=driving"
                )

                val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                mapIntent.setPackage("com.google.android.apps.maps")

                if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(context, "Google Maps-appen hittades inte", Toast.LENGTH_SHORT)
                        .show()
                }
            } ?: run {
                Toast.makeText(context, "Kunde inte hämta nuvarande plats", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun getCurrentLocation(onLocationReceived: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->

                onLocationReceived(location)
            }
            .addOnFailureListener {

                onLocationReceived(null)
            }
    }


    private fun toggleHiddenGemSharing(shouldBeShared: Boolean) {
        hiddenGem.isShared = shouldBeShared

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("hidden_gems").document(hiddenGem.id)
            .update("isShared", shouldBeShared)
            .addOnSuccessListener {
                updateButtonsBasedOnSharingStatus(shouldBeShared)
                val message = if (shouldBeShared) "Delas nu publikt!" else "Slutar dela publikt."
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Kunde inte uppdatera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun updateButtonsBasedOnSharingStatus(isShared: Boolean) {
        if (isShared) {
            shareButton.visibility = View.GONE
            stopSharingButton.visibility = View.VISIBLE
        } else {
            shareButton.visibility = View.VISIBLE
            stopSharingButton.visibility = View.GONE
        }
    }


    companion object {
        const val HIDDEN_GEM = "hidden_gem"
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101

        fun newInstance(hiddenGem: HiddenGem, isEditable: Boolean = true): HiddenGemDetailFragment {
            val args = Bundle().apply {
                putParcelable(HIDDEN_GEM, hiddenGem)
            }
            return HiddenGemDetailFragment().apply {
                arguments = args
            }
        }
    }
}
