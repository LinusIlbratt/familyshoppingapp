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

class MyPlacesFragment : Fragment(), OnMyPlacesClickListener {

    private lateinit var myPlacesAdapter: MyPlacesAdapter
    private lateinit var recyclerView: RecyclerView
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_places, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("!!!", "onViewCreated called in HiddenGemsFragment")
        recyclerView = view.findViewById(R.id.recyclerViewMyPlacesList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        myPlacesAdapter = MyPlacesAdapter(emptyList(), this)
        recyclerView.adapter = myPlacesAdapter

        val verticalSpaceHeight = resources.getDimensionPixelSize(R.dimen.recycler_view_item_spacing)
        recyclerView.addItemDecoration(RecycleviewItemVerticalSpacing(verticalSpaceHeight))

        val backArrow = view.findViewById<ImageView>(R.id.backArrow)
        backArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val fabAddHiddenGem = view.findViewById<FloatingActionButton>(R.id.fab_add_hidden_gem)
        fabAddHiddenGem.setOnClickListener {
            val userId = getCurrentUserId()
            if (userId != null) {
                showAddMyPlacesDialog(userId)
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
                    val myPlace = document.toObject(MyPlace::class.java)
                    Log.d(
                        "!!!",
                        "Hidden Gem: name='${myPlace.name}', tag='${myPlace.tag}'"
                    )
                    if (myPlace.name.isNullOrEmpty() || myPlace.tag.isNullOrEmpty()) {
                        Log.w("!!!", "Hidden Gem has null or empty name/tag")
                    }
                    myPlace
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
                    document.toObject(MyPlace::class.java)
                }
                updateRecyclerView(hiddenGemsList)
            }
            .addOnFailureListener { e ->

            }
    }

    private fun updateRecyclerView(myPlaceList: List<MyPlace>) {
        Log.d("update", "Updating RecyclerView with items: $myPlaceList")
        myPlacesAdapter.updateItems(myPlaceList)
    }


    private fun showAddMyPlacesDialog(userId: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_my_places, null)

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
                        val newMyPlace = MyPlace(name = name, tag = category, tags = tagsList, userId = userId)
                        addNewMyPlacesToFirestore(newMyPlace, userId) // Skicka userId som parameter
                    } else {
                        CustomToast.showCustomToast(context, "All fields are required, including least one tag", Toast.LENGTH_LONG)
                    }
                }
                .setNegativeButton("Close") { dialog, which ->

                }
        }
        builder?.create()?.show()
    }


    private fun addNewMyPlacesToFirestore(newMyPlace: MyPlace, userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val hiddenGemsCollection = firestore.collection("hidden_gems")

        val newDocumentRef = hiddenGemsCollection.document()
        val updatedHiddenGem = newMyPlace.copy(id = newDocumentRef.id, userId = userId)

        newDocumentRef.set(updatedHiddenGem)
            .addOnSuccessListener {
                Log.d("!!!", "Hidden Gem added with ID: ${newDocumentRef.id}")
                updateHiddenGemsData()
            }
            .addOnFailureListener { e ->
                Log.w("!!!", "Error adding document", e)
            }
    }


    override fun onMyPlacesClicked(myPlace: MyPlace) {
        Log.d("detail", "Hidden Gem clicked: ${myPlace.name}")
        val detailFragment = MyPlacesDetailFragment.newInstance(myPlace)
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.hidden_gem_fragment_container, detailFragment)
            ?.addToBackStack(null)
            ?.commit()
    }

    override fun onRemoveIconClicked(myPlace: MyPlace) {
        showDeleteDialog(myPlace)
    }

    private fun showDeleteDialog(myPlace: MyPlace) {
        AlertDialog.Builder(requireActivity())
            .setTitle("Delete List")
            .setMessage("Are you sure you want to delete your hidden gem '${myPlace.name}'?")
            .setPositiveButton("Delete") { dialog, which ->
                deleteHiddenGem(myPlace)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteHiddenGem(myPlace: MyPlace) {
        val firestore = FirebaseFirestore.getInstance()
        val hiddenGemsCollection = firestore.collection("hidden_gems")

        hiddenGemsCollection.document(myPlace.id)
            .delete()
            .addOnSuccessListener {
                Log.d("!!!", "Hidden Gem deleted with ID: ${myPlace.id}")
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

