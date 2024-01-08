package com.example.familyshoppingapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HiddenGemsFragment : Fragment(), OnHiddenGemClickListener {

    private lateinit var hiddenGemsAdapter: HiddenGemsAdapter
    private lateinit var recyclerView: RecyclerView
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hidden_gems, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("!!!", "onViewCreated called in HiddenGemsFragment")
        recyclerView = view.findViewById(R.id.recyclerViewGemList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        hiddenGemsAdapter = HiddenGemsAdapter(emptyList(), this)
        recyclerView.adapter = hiddenGemsAdapter

        val backArrow = view.findViewById<ImageView>(R.id.backArrow)
        backArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val fabAddHiddenGem = view.findViewById<FloatingActionButton>(R.id.fab_add_hidden_gem)
        fabAddHiddenGem.setOnClickListener {
            showAddHiddenGemDialog()
        }
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
                    Log.d(
                        "HiddenGemsFragment",
                        "Hidden Gem: name='${hiddenGem.name}', tag='${hiddenGem.tag}'"
                    )
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
        builder?.apply {
            setView(dialogView)
                .setTitle("Add New Hidden Gem")
                .setPositiveButton("Add") { dialog, _ ->
                    val name = dialogView.findViewById<EditText>(R.id.titel_name).text.toString().trim()
                    val category = dialogView.findViewById<EditText>(R.id.category_name).text.toString().trim()
                    val tagsInput = dialogView.findViewById<EditText>(R.id.tags_input).text.toString().trim()

                    if (name.isNotEmpty() && category.isNotEmpty() && tagsInput.isNotEmpty()) {
                        val tagsList = tagsInput.split(",").map { it.trim() }
                        val newHiddenGem = HiddenGem(name = name, tag = category, tags = tagsList)
                        addNewHiddenGemToFirestore(newHiddenGem)
                    } else {
                        Toast.makeText(context, "All fields are required, including at least one tag", Toast.LENGTH_LONG).show()
                    }
                }
        }
        builder?.create()?.show()
    }


    private fun addNewHiddenGemToFirestore(newHiddenGem: HiddenGem) {
        val firestore = FirebaseFirestore.getInstance()
        val hiddenGemsCollection = firestore.collection("hidden_gems")

        val newDocumentRef = hiddenGemsCollection.document()

        val updatedHiddenGem = newHiddenGem.copy(id = newDocumentRef.id)

        newDocumentRef.set(updatedHiddenGem)
            .addOnSuccessListener {
                Log.d("!!!", "Hidden Gem added with ID: ${newDocumentRef.id}")
            }
            .addOnFailureListener { e ->
                Log.w("!!!", "Error adding document", e)
            }
    }


    override fun onHiddenGemClicked(hiddenGem: HiddenGem) {
        Log.d("detail", "Hidden Gem clicked: ${hiddenGem.name}")
        val detailFragment = HiddenGemDetailFragment.newInstance(hiddenGem)
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.hidden_gem_fragment_container, detailFragment)
            ?.addToBackStack(null)
            ?.commit()
    }

    override fun onRemoveIconClicked(hiddenGem: HiddenGem) {
        val firestore = FirebaseFirestore.getInstance()
        val hiddenGemsCollection = firestore.collection("hidden_gems")

        hiddenGemsCollection.document(hiddenGem.id)
            .delete()
            .addOnSuccessListener {
                Log.d("!!!", "Hidden Gem deleted with ID: ${hiddenGem.id}")
                // Update recyclerview if needed
            }
            .addOnFailureListener { e ->
                Log.w("!!!", "Error deleting document", e)
            }
    }


}

