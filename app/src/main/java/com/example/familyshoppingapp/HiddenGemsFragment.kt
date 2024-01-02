package com.example.familyshoppingapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HiddenGemsFragment : Fragment() {

    private lateinit var hiddenGemsAdapter: HiddenGemsAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hidden_gems, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewGemList) // Referera till RecyclerView i din layout

        // Här kan du hämta din data för HiddenGem-objekten
        val hiddenGemsList = listOf<HiddenGem>() // Ersätt med din data

        hiddenGemsAdapter = HiddenGemsAdapter(hiddenGemsList)
        recyclerView.adapter = hiddenGemsAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }
}
