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
import com.google.firebase.firestore.ListenerRegistration

class HiddenGemsFragment : Fragment() {

    private lateinit var hiddenGemsAdapter: HiddenGemsAdapter
    private lateinit var recyclerView: RecyclerView
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hidden_gems, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewGemList)

        val fabAddHiddenGem = view.findViewById<FloatingActionButton>(R.id.fab_add_hidden_gem)
        fabAddHiddenGem.setOnClickListener {
            showAddHiddenGemDialog()
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        hiddenGemsAdapter = HiddenGemsAdapter(emptyList())
        recyclerView.adapter = hiddenGemsAdapter

        return view
    }


    override fun onStart() {
        super.onStart()
        setupFirestoreListener()
    }


    override fun onStop() {
        super.onStop()
        firestoreListener?.remove()
    }


    private fun setupFirestoreListener() {
        val firestore = FirebaseFirestore.getInstance()
        firestoreListener = firestore.collection("hidden_gems")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("HiddenGemsFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val hiddenGemsList = snapshots?.mapNotNull { document ->
                    val hiddenGem = document.toObject(HiddenGem::class.java)
                    Log.d("HiddenGemsFragment", "Hidden Gem: name='${hiddenGem.name}', tag='${hiddenGem.tag}'")
                    if (hiddenGem.name.isNullOrEmpty() || hiddenGem.tag.isNullOrEmpty()) {
                        Log.w("HiddenGemsFragment", "Hidden Gem has null or empty name/tag")
                    }
                    hiddenGem
                } ?: emptyList()
                Log.d("HiddenGemsFragment", "Fetched Hidden Gems: $hiddenGemsList")

                updateRecyclerView(hiddenGemsList)
            }
    }



    private fun updateRecyclerView(hiddenGemsList: List<HiddenGem>) {
        val sectionItems = createSectionList(hiddenGemsList)
        Log.d("section", "Updating RecyclerView with items: $sectionItems")
        hiddenGemsAdapter.items = sectionItems
        hiddenGemsAdapter.notifyDataSetChanged()
    }



    private fun createSectionList(hiddenGems: List<HiddenGem>): List<SectionItem> {
        val sectionList = mutableListOf<SectionItem>()
        val hiddenGemsByCategory = hiddenGems.groupBy { it.tag }

        hiddenGemsByCategory.forEach { (category, gems) ->
            sectionList.add(SectionItem.Header(category))
            gems.forEach { gem ->
                sectionList.add(SectionItem.Item(gem))
            }
        }
        return sectionList
    }


    private fun showAddHiddenGemDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_hidden_gem, null)

        val builder = context?.let { AlertDialog.Builder(it) }
        if (builder != null) {
            builder.setView(dialogView)
                .setTitle("Add New Hidden Gem")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add") { dialog, _ ->
                    val name = dialogView.findViewById<EditText>(R.id.titel_name).text.toString().trim()
                    val category = dialogView.findViewById<EditText>(R.id.category_name).text.toString().trim()

                    if (name.isNotEmpty() && category.isNotEmpty()) {
                        val newHiddenGem = HiddenGem(name = name, tag = category)
                        addNewHiddenGemToFirestore(newHiddenGem)
                    }
                }
        }
        if (builder != null) {
            builder.create().show()
        }
    }

    private fun addNewHiddenGemToFirestore(newHiddenGem: HiddenGem) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("hidden_gems")
            .add(newHiddenGem)
            .addOnSuccessListener { documentReference ->
                Log.d("HiddenGemsFragment", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("HiddenGemsFragment", "Error adding document", e)
            }
    }




}

