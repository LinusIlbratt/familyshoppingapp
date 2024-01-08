package com.example.familyshoppingapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class SearchHiddenGemsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchHiddenGemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_hidden_gems, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val onItemClicked: (HiddenGem) -> Unit = { hiddenGem ->
            Log.d("!!!", "Item clicked: ${hiddenGem.name}")
            openSearchHiddenGemsFragment(hiddenGem)
        }

        val backArrow = view.findViewById<ImageView>(R.id.backArrow)
        backArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView = view.findViewById(R.id.recyclerView_search_results)
        adapter = SearchHiddenGemsAdapter(emptyList(), onItemClicked)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        val searchView = view.findViewById<SearchView>(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    searchHiddenGems(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        adapter = SearchHiddenGemsAdapter(emptyList()) { hiddenGem ->
            openSearchHiddenGemsFragment(hiddenGem)
        }
        recyclerView.adapter = adapter
    }


    private fun searchHiddenGems(query: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("hidden_gems")
            .whereArrayContainsAny(
                "tags",
                listOf(query)
            )
            .get()
            .addOnSuccessListener { documents ->
                val searchResults = documents.mapNotNull { document ->
                    document.toObject(HiddenGem::class.java)
                }
                updateRecyclerView(searchResults)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Sökningen misslyckades: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun openSearchHiddenGemsFragment(hiddenGem: HiddenGem) {
        val detailFragment = SearchHiddenGemInfoFragment.newInstance(hiddenGem)

        parentFragmentManager.beginTransaction()
            .replace(R.id.search_gem_fragment_container, detailFragment)
            .addToBackStack(null)  // Lägg till i backstacken
            .commit()
    }



    private fun updateRecyclerView(newHiddenGems: List<HiddenGem>) {
        if (newHiddenGems.isEmpty()) {
            Toast.makeText(context, "Inga resultat hittades.", Toast.LENGTH_LONG).show()
        } else {
            adapter.updateData(newHiddenGems)
        }
    }

}

