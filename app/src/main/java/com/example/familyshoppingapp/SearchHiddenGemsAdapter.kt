package com.example.familyshoppingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchHiddenGemsAdapter(private var hiddenGems: List<HiddenGem>) :
    RecyclerView.Adapter<SearchHiddenGemsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.text_view_search_result)
        // Fler views kan läggas till här
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_hidden_gem_items, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hiddenGem = hiddenGems[position]
        holder.textView.text = hiddenGem.name
        // Sätt andra värden på views här
    }

    override fun getItemCount() = hiddenGems.size

    fun updateData(newHiddenGems: List<HiddenGem>) {
        hiddenGems = newHiddenGems
        notifyDataSetChanged()
    }
}
