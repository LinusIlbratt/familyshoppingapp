package com.example.familyshoppingapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng

class SearchHiddenGemInfoFragment : Fragment() {

    private lateinit var hiddenGem: HiddenGem

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
        return inflater.inflate(R.layout.fragment_search_hidden_gem_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentManager = parentFragmentManager
        val fragmentBackstackCount = fragmentManager.backStackEntryCount

        for (i in 0 until fragmentBackstackCount) {
            val fragmentBackStackEntry = fragmentManager.getBackStackEntryAt(i)
            val fragmentName = fragmentBackStackEntry.name
            Log.d("FragmentBackstack", "Fragment at position $i: $fragmentName")
        }


        initViews(view)
        setupListeners()
    }

    private fun initViews(view: View) {
        val photoHolder = view.findViewById<ImageView>(R.id.hidden_gem_detail_photoHolder)

        val titleTextView = view.findViewById<TextView>(R.id.detail_title)
        titleTextView.text = hiddenGem.name

        val descriptionTextView = view.findViewById<TextView>(R.id.detail_description_textView)
        descriptionTextView.text = hiddenGem.description

        val backArrow = view.findViewById<ImageView>(R.id.backArrow)
        backArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        hiddenGem.imageUrl?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .into(photoHolder)
        }
    }

    private fun setupListeners() {
        val showGpsButton = view?.findViewById<ImageButton>(R.id.btn_show_gps)
        showGpsButton?.setOnClickListener {
            showDirectionsInGoogleMap()
        }
    }

    private fun showDirectionsInGoogleMap() {
        val destination = LatLng(hiddenGem.latitude, hiddenGem.longitude)

        val intentUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=" +
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
    }

    companion object {
        private const val HIDDEN_GEM = "hidden_gem"

        fun newInstance(hiddenGem: HiddenGem): SearchHiddenGemInfoFragment {
            val args = Bundle().apply {
                putParcelable(HIDDEN_GEM, hiddenGem)
            }
            return SearchHiddenGemInfoFragment().apply {
                arguments = args
            }
        }
    }
}
