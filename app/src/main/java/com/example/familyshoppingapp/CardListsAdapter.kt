package com.example.familyshoppingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CardListsAdapter(
    private val onItemClicked: (ShoppingLists?) -> Unit,
    private val onInviteClicked: (String) -> Unit,
    private val onItemLongClicked: (ShoppingLists?) -> Unit
) : RecyclerView.Adapter<CardListsAdapter.CardViewHolder>() {

    private var items: MutableList<ShoppingLists?> = mutableListOf()

    fun setItems(list: List<ShoppingLists>) {
        items.clear()
        if (list.isEmpty()) {
            items.add(null) // Lägger till ett tomt kort om listan är tom
        } else {
            items.addAll(list)
        }
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
                holder.inviteButton.visibility = View.GONE
                holder.addSignOnInviteButton.visibility = View.GONE
                holder.addMemberText.visibility = View.GONE
            } else {
                holder.imageViewAddIcon.visibility = View.GONE
                holder.textViewListName.visibility = View.VISIBLE
                holder.textViewListName.text = item.name
                holder.textViewCategory.visibility = View.VISIBLE
                holder.textViewCategory.text = item.category
                holder.inviteButton.visibility = View.VISIBLE
                holder.addSignOnInviteButton.visibility = View.VISIBLE
                holder.addMemberText.visibility = View.VISIBLE
            }
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClicked(item)
            true
        }

        holder.itemView.setOnClickListener {
            onItemClicked(item)
        }
        holder.inviteButton.setOnClickListener {
            item?.let { shoppingList ->
                shoppingList.documentId?.let { it1 -> onInviteClicked(it1) }
            }
        }
    }

    override fun getItemCount() = items.size

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewAddIcon: ImageView = itemView.findViewById(R.id.imageViewAddIcon)
        val textViewListName: TextView = itemView.findViewById(R.id.textViewListName)
        val textViewCategory: TextView = itemView.findViewById(R.id.textViewCategory)
        val inviteButton: FloatingActionButton = itemView.findViewById(R.id.inviteButton)
        val addMemberText: TextView = itemView.findViewById(R.id.addMemberText)
        val addSignOnInviteButton: ImageView = itemView.findViewById(R.id.addSignOnInviteBtn)
        fun bind(shoppingList: ShoppingLists) {
            // Bind data to your views
            // Exempel: itemView.textViewName.text = shoppingList.name
        }
    }
}
