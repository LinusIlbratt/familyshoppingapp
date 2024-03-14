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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MyPlacesFragment : Fragment(), OnHiddenGemClickListener {

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
            val userId = getCurrentUserId()
            if (userId != null) {
                showAddHiddenGemDialog(userId)
            } else {

            }
        }
    }


    override fun onStart() {
        super.onStart()
        val userId = getCurrentUserId()
        if (userId != null) {
            setupFirestoreListener(userId)
        } else {

        }
    }


    override fun onStop() {
        super.onStop()
        firestoreListener?.remove()
    }


    private fun setupFirestoreListener(userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestoreListener = firestore.collection("hidden_gems")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("!!!", "Firestore Listener failed. (HiddenGemsFragment)", e)
                    return@addSnapshotListener
                }

                val hiddenGemsList = snapshots?.mapNotNull { document ->
                    val hiddenGem = document.toObject(HiddenGem::class.java)
                    Log.d(
                        "!!!",
                        "Hidden Gem: name='${hiddenGem.name}', tag='${hiddenGem.tag}'"
                    )
                    if (hiddenGem.name.isNullOrEmpty() || hiddenGem.tag.isNullOrEmpty()) {
                        Log.w("!!!", "Hidden Gem has null or empty name/tag")
                    }
                    hiddenGem
                } ?: emptyList()
                Log.d("!!!", "Fetched Hidden Gems: $hiddenGemsList (HiddenGemsFragment)")

                updateRecyclerView(hiddenGemsList)
            }
    }

    private fun updateHiddenGemsData() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("hidden_gems")
            .get()
            .addOnSuccessListener { documents ->
                val hiddenGemsList = documents.mapNotNull { document ->
                    document.toObject(HiddenGem::class.java)
                }
                updateRecyclerView(hiddenGemsList)
            }
            .addOnFailureListener { e ->

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


    private fun showAddHiddenGemDialog(userId: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_hidden_gem, null)

        val builder = context?.let { AlertDialog.Builder(it, R.style.CustomAlertDialog) }
        builder?.apply {
            setView(dialogView)
                .setTitle("Add New Hidden Gem")
                .setPositiveButton("Add") { dialog, _ ->
                    val name = dialogView.findViewById<EditText>(R.id.titel_name).text.toString().trim()
                    val category = dialogView.findViewById<EditText>(R.id.category_name).text.toString().trim()
                    val tagsInput = dialogView.findViewById<EditText>(R.id.tags_input).text.toString().trim()

                    if (name.isNotEmpty() && category.isNotEmpty() && tagsInput.isNotEmpty()) {
                        val tagsList = tagsInput.split(",").map { it.trim() }
                        val newHiddenGem = HiddenGem(name = name, tag = category, tags = tagsList, userId = userId)
                        addNewHiddenGemToFirestore(newHiddenGem, userId) // Skicka userId som parameter
                    } else {
                        CustomToast.showCustomToast(context, "All fields are required, including least one tag", Toast.LENGTH_LONG)
                    }
                }
                .setNegativeButton("Close") { dialog, which ->

                }
        }
        builder?.create()?.show()
    }


    private fun addNewHiddenGemToFirestore(newHiddenGem: HiddenGem, userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val hiddenGemsCollection = firestore.collection("hidden_gems")

        val newDocumentRef = hiddenGemsCollection.document()
        val updatedHiddenGem = newHiddenGem.copy(id = newDocumentRef.id, userId = userId)

        newDocumentRef.set(updatedHiddenGem)
            .addOnSuccessListener {
                Log.d("!!!", "Hidden Gem added with ID: ${newDocumentRef.id}")
                updateHiddenGemsData()
            }
            .addOnFailureListener { e ->
                Log.w("!!!", "Error adding document", e)
            }
    }


    override fun onHiddenGemClicked(hiddenGem: HiddenGem) {
        Log.d("detail", "Hidden Gem clicked: ${hiddenGem.name}")
        val detailFragment = MyPlacesDetailFragment.newInstance(hiddenGem)
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.hidden_gem_fragment_container, detailFragment)
            ?.addToBackStack(null)
            ?.commit()
    }

    override fun onRemoveIconClicked(hiddenGem: HiddenGem) {
        showDeleteDialog(hiddenGem)
    }

    private fun showDeleteDialog(hiddenGem: HiddenGem) {
        AlertDialog.Builder(requireActivity())
            .setTitle("Delete List")
            .setMessage("Are you sure you want to delete your hidden gem '${hiddenGem.name}'?")
            .setPositiveButton("Delete") { dialog, which ->
                deleteHiddenGem(hiddenGem)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteHiddenGem(hiddenGem: HiddenGem) {
        val firestore = FirebaseFirestore.getInstance()
        val hiddenGemsCollection = firestore.collection("hidden_gems")

        hiddenGemsCollection.document(hiddenGem.id)
            .delete()
            .addOnSuccessListener {
                Log.d("!!!", "Hidden Gem deleted with ID: ${hiddenGem.id}")
                updateHiddenGemsData()
            }
            .addOnFailureListener { e ->
                Log.w("!!!", "Error deleting document", e)
            }
    }

    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }


}
