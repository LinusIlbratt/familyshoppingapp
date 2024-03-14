package com.example.familyshoppingapp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class MyDiscoveriesAdapter(private var hiddenGems: List<HiddenGem>, private val onItemClicked: (HiddenGem) -> Unit) :
    RecyclerView.Adapter<MyDiscoveriesAdapter.ViewHolder>() {

    class ViewHolder(view: View, private val onItemClicked: (HiddenGem) -> Unit) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.text_view_search_result)

        fun bind(hiddenGem: HiddenGem) {
            textView.text = hiddenGem.name

            itemView.setOnClickListener {
                Log.d("!!!", "Clicked on: ${hiddenGem.name}")
                onItemClicked(hiddenGem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_hidden_gem_items, parent, false)
        return ViewHolder(view, onItemClicked)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hiddenGem = hiddenGems[position]
        holder.bind(hiddenGem)
    }

    override fun getItemCount() = hiddenGems.size

    fun updateData(newHiddenGems: List<HiddenGem>) {
        hiddenGems = newHiddenGems
        notifyDataSetChanged()
    }
}
