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
import com.google.android.gms.location.LocationServices
import android.location.Location


class HiddenGemDetailFragment : Fragment() {
    private lateinit var hiddenGem: HiddenGem
    private lateinit var descriptionEditText: EditText
    private lateinit var editButton: Button
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("detail", "onCreate called")
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
    }


    private fun initViews(view: View) {
        val backArrow = view.findViewById<ImageView>(R.id.backArrow)
        view.findViewById<TextView>(R.id.detail_titel).text = hiddenGem.name

        descriptionEditText = view.findViewById(R.id.detail_description_edit)
        editButton = view.findViewById(R.id.btn_edit_desc)
        saveButton = view.findViewById(R.id.btn_save_desc)

        descriptionEditText.setText(hiddenGem.description)
        descriptionEditText.background = null
        descriptionEditText.isFocusable = false
        descriptionEditText.isFocusableInTouchMode = false
        descriptionEditText.isCursorVisible = false

        backArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupListeners() {
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
    }

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            // Tillstånd är inte beviljat, begär det
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Tillstånd är beviljat, fortsätt med att spara GPS-position
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
                // Tillstånd beviljades, fortsätt med att spara GPS-position
                saveCurrentLocation()
            } else {
                // Tillstånd nekades, hantera det (visa en förklaring, etc.)
            }
        }
    }

    private fun saveCurrentLocation() {

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return
        }

        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

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
                // Framgångsrik uppsparning
                Toast.makeText(context, "Hidden Gem updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Hantering av fel
                Toast.makeText(context, "Error updating Hidden Gem: ${e.message}", Toast.LENGTH_SHORT).show()
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
                // Framgångsrik uppsparning
                Toast.makeText(context, "Description updated successfully", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                // Hantering av fel
                Toast.makeText(
                    context,
                    "Error updating description: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    companion object {
        private const val HIDDEN_GEM = "hidden_gem"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        fun newInstance(hiddenGem: HiddenGem): HiddenGemDetailFragment {
            val args = Bundle().apply {
                putParcelable(HIDDEN_GEM, hiddenGem)
            }
            return HiddenGemDetailFragment().apply {
                arguments = args
            }
        }
    }
}
