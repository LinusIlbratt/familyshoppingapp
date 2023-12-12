package com.example.familyshoppingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CardListsAdapter(private val onItemClicked: (ShoppingLists?) -> Unit) : RecyclerView.Adapter<CardListsAdapter.CardViewHolder>() {

    var items: MutableList<ShoppingLists?> = mutableListOf()

    fun setItems(list: List<ShoppingLists>) {
        items.clear()
        items.addAll(list)
        items.add(null)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_layout, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val item = items[position]
        if (item == null) {
            // Konfigurera vyn för 'lägg till ny lista'-kortet
        } else {
            // Konfigurera vyn för en vanlig shoppinglista
            holder.bind(item)
        }
        holder.itemView.setOnClickListener { onItemClicked(item) }
    }

    override fun getItemCount() = items.size

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(shoppingList: ShoppingLists) {
            // Bind data to your views
            // Exempel: itemView.textViewName.text = shoppingList.name
        }
    }
}
