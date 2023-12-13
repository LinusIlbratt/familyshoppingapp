package com.example.familyshoppingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CardListsAdapter(private val onItemClicked: (ShoppingLists?) -> Unit) : RecyclerView.Adapter<CardListsAdapter.CardViewHolder>() {

    private var items: MutableList<ShoppingLists?> = mutableListOf()

    fun setItems(list: List<ShoppingLists>) {
        items.clear()
        items.addAll(list)
        items.add(null) // Lägger till null för det tomma kortet
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_layout, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {


        val item = items[position]
        if (item != null) {
            if (item.isCardEmpty) {
                holder.imageViewAddIcon.visibility = View.VISIBLE
                holder.textViewListName.visibility = View.GONE
                holder.textViewCategory.visibility = View.GONE
            } else {
                holder.imageViewAddIcon.visibility = View.GONE
                holder.textViewListName.visibility = View.VISIBLE
                holder.textViewListName.text = item.name
                holder.textViewCategory.visibility = View.VISIBLE
                holder.textViewCategory.text = item.category
            }
        }
        holder.itemView.setOnClickListener {
            onItemClicked(item)
        }
    }

    override fun getItemCount() = items.size

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewAddIcon: ImageView = itemView.findViewById(R.id.imageViewAddIcon)
        val textViewListName: TextView = itemView.findViewById(R.id.textViewListName)
        val textViewCategory: TextView = itemView.findViewById(R.id.textViewCategory)
        fun bind(shoppingList: ShoppingLists) {
            // Bind data to your views
            // Exempel: itemView.textViewName.text = shoppingList.name
        }
    }
}
