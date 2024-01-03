package com.example.familyshoppingapp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

interface OnHiddenGemClickListener {
    fun onHiddenGemClicked(hiddenGem: HiddenGem)
}

class HiddenGemsAdapter(var items: List<SectionItem>, private val listener: OnHiddenGemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SectionItem.Header -> TYPE_HEADER
            is SectionItem.Item -> TYPE_ITEM
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                HeaderViewHolder(inflater.inflate(R.layout.section_header, parent, false))
            }
            TYPE_ITEM -> {
                val itemView = inflater.inflate(R.layout.hidden_gem_item, parent, false)
                ItemViewHolder(itemView, listener)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SectionItem.Header -> (holder as HeaderViewHolder).headerTextView.text = item.title
            is SectionItem.Item -> {
                holder as ItemViewHolder
                holder.titleTextView.text = item.hiddenGem.name
            }
        }
    }



    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerTextView: TextView = view.findViewById(R.id.sectionHeader)
    }

    class ItemViewHolder(view: View, listener: OnHiddenGemClickListener) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.hidden_gem_title)
    }
}

