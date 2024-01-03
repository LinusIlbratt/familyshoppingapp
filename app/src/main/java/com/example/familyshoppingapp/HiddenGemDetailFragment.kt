package com.example.familyshoppingapp

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
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class HiddenGemDetailFragment : Fragment() {
    private lateinit var hiddenGem: HiddenGem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("detail", "onCreate called")
        arguments?.let {
            hiddenGem = it.getParcelable(HIDDEN_GEM) ?: throw IllegalArgumentException("Hidden Gem is required")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("detail", "onCreateView called")
        return inflater.inflate(R.layout.fragment_hidden_gem_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backArrow = view.findViewById<ImageView>(R.id.backArrow)
        backArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<TextView>(R.id.detail_titel).text = hiddenGem.name
        val descriptionEditText = view.findViewById<EditText>(R.id.detail_description_edit)
        val editButton = view.findViewById<Button>(R.id.btn_edit_desc)
        val saveButton = view.findViewById<Button>(R.id.btn_save_desc)


        descriptionEditText.setText(hiddenGem.description)
        descriptionEditText.background = null
        descriptionEditText.isFocusable = false
        descriptionEditText.isFocusableInTouchMode = false
        descriptionEditText.isCursorVisible = false

        editButton.setOnClickListener {

            descriptionEditText.background = ResourcesCompat.getDrawable(resources, R.drawable.edit_text_background, null)
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

            hiddenGem.description = descriptionEditText.text.toString()
            saveHiddenGemDesc(hiddenGem)

            descriptionEditText.background = null
            descriptionEditText.isFocusable = false
            descriptionEditText.isFocusableInTouchMode = false
            descriptionEditText.isCursorVisible = false
            saveButton.visibility = View.GONE
            editButton.visibility = View.VISIBLE
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
                Toast.makeText(context, "Description updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Hantering av fel
                Toast.makeText(context, "Error updating description: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }




    companion object {
        private const val HIDDEN_GEM = "hidden_gem"

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
