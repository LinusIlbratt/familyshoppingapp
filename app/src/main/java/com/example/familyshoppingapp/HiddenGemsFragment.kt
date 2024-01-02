package com.example.familyshoppingapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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

        val hiddenGemsByCategory = hiddenGemsList.groupBy { it.tag }
        val sections = hiddenGemsByCategory.map { (category, hiddenGems) ->
            HiddenGemListSection(header = category, items = hiddenGems)
        }

        hiddenGemsSectionAdapter = HiddenGemsSectionAdapter(hiddenGemsList)
        recyclerView.adapter = hiddenGemsSectionAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }
}
