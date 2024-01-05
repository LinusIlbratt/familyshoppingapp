package com.example.familyshoppingapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SearchHiddenGemsFragment : Fragment() {

    // Andra variabler och funktioner

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_hidden_gems, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val searchView = view.findViewById<SearchView>(R.id.search_view)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView_search_results)

        val adapter = SearchHiddenGemsAdapter(emptyList())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    // Här kan du anropa din sökfunktion och uppdatera adaptern
                    //adapter.updateData(sökresultaten)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }


    private fun searchHiddenGems(query: String) {
        // Hämta data från Firestore baserat på query
        // Exempel: Firestore-db.collection("hidden_gems").whereEqualTo("tag", query).get()...
    }
}

