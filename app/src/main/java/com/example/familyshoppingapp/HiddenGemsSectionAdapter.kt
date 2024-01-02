package com.example.familyshoppingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.lang.IllegalStateException

class HiddenGemsSectionAdapter(private val sections: List<HiddenGemListSection>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        var totalSize = 0
        for (section in sections) {
            if (position == totalSize) {
                return TYPE_HEADER
            }
            totalSize += 1 + section.items.size
            if (position < totalSize) {
                return TYPE_ITEM
            }
        }
        throw IllegalStateException("Invalid position")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.section_header, parent, false))
            TYPE_ITEM -> ItemViewHolder(inflater.inflate(R.layout.hidden_gem_item, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var totalSize = 0
        for (section in sections) {
            if (position == totalSize) {
                (holder as HeaderViewHolder).headerTextView.text = section.header
                return
            }
            totalSize += 1
            if (position < totalSize + section.items.size) {
                val item = section.items[position - totalSize]
                (holder as ItemViewHolder).titleTextView.text = item.name
                return
            }
            totalSize += section.items.size
        }
    }

    override fun getItemCount(): Int {
        return sections.sumOf { it.items.size + 1 }
    }

}

class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val headerTextView: TextView = view.findViewById(R.id.sectionHeader)
}

class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val titleTextView: TextView = view.findViewById(R.id.hiddenGemTextTitle)
    // Här kan du lägga till fler vy-element som hör till varje listobjekt
}
