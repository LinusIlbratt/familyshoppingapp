package com.example.familyshoppingapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class HiddenGemsFragment : Fragment() {

    private lateinit var hiddenGemsSectionAdapter: HiddenGemsSectionAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hidden_gems, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewGemList)

        val hiddenGemsList = listOf<HiddenGem>()

        val fabAddHiddenGem = view.findViewById<FloatingActionButton>(R.id.fab_add_hidden_gem)
        fabAddHiddenGem.setOnClickListener {
            showAddHiddenGemDialog()
        }

        val hiddenGemsByCategory = hiddenGemsList.groupBy { it.tag }
        val sections = hiddenGemsByCategory.map { (category, hiddenGems) ->
            HiddenGemListSection(header = category, items = hiddenGems)
        }

        hiddenGemsSectionAdapter = HiddenGemsSectionAdapter(sections)
        recyclerView.adapter = hiddenGemsSectionAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }

    private fun showAddHiddenGemDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_hidden_gem, null)
        val builder = context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setView(dialogView)
                .setTitle("Add New Hidden Gem")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add") { dialog, which ->
                    val name = dialogView.findViewById<EditText>(R.id.hidden_gem_name).text.toString()
                    val category = dialogView.findViewById<EditText>(R.id.category_name).text.toString()

                    val newHiddenGem = HiddenGem(name, "", 0.0, 0.0, null, category)
                    addNewHiddenGemToFirestore(newHiddenGem)
                }
        }

        builder?.create()?.show()
    }

    private fun addNewHiddenGemToFirestore(newHiddenGem: HiddenGem) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("hidden_gems")
            .add(newHiddenGem)
            .addOnSuccessListener { documentReference ->
                Log.d("HiddenGemsFragment", "DocumentSnapshot added with ID: ${documentReference.id}")
                // Eventuellt uppdatera din RecyclerView här om nödvändigt
            }
            .addOnFailureListener { e ->
                Log.w("HiddenGemsFragment", "Error adding document", e)
            }
    }

}
