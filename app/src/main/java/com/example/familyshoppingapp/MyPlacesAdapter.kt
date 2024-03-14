package com.example.familyshoppingapp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

interface OnMyPlacesClickListener {
    fun onMyPlacesClicked(myPlace: MyPlace)
    fun onRemoveIconClicked(myPlace: MyPlace)
}

class MyPlacesAdapter(
    private var items: List<MyPlace>,
    private val listener: OnMyPlacesClickListener
) : RecyclerView.Adapter<MyPlacesAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.my_places_item, parent, false)
        return ItemViewHolder(itemView, listener)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<MyPlace>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    class ItemViewHolder(view: View, private val listener: OnMyPlacesClickListener) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.findViewById(R.id.my_places_title)
        private val removeIcon: ImageButton = view.findViewById(R.id.remove_icon)

        fun bind(myPlace: MyPlace) {
            titleTextView.text = myPlace.name

            itemView.setOnClickListener {
                listener.onMyPlacesClicked(myPlace)
            }

            removeIcon.setOnClickListener {
                listener.onRemoveIconClicked(myPlace)
            }
        }
    }
}

