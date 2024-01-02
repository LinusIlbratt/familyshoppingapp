package com.example.familyshoppingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HiddenGemsAdapter(private val hiddenGemsList: List<HiddenGem>) : RecyclerView.Adapter<HiddenGemsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titelTextView: TextView = view.findViewById(R.id.hiddenGemTextTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.hidden_gem_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hiddenGem = hiddenGemsList[position]
        holder.titelTextView.text = hiddenGem.name

    }

    override fun getItemCount(): Int {
        return hiddenGemsList.size
    }
}
