package com.example.familyshoppingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(private val productList: MutableList<Product>, private val onDeleteClicked: (Int) -> Unit) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewProductName: TextView = view.findViewById(R.id.productNameTextView)
        val buttonAddToCart: Button = view.findViewById(R.id.buttonAddToCart)
        val buttonDelete: Button = view.findViewById(R.id.buttonDelete)
        val buttonEdit: Button = view.findViewById(R.id.buttonEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentItem = productList[position]
        holder.textViewProductName.text = currentItem.name

        // Change alpha value if a product is added to the cart
        if (currentItem.isAdded) {
            holder.itemView.alpha = 0.5f
        } else {
            holder.itemView.alpha = 1.0f
        }

        holder.buttonAddToCart.setOnClickListener {
            currentItem.isAdded = !currentItem.isAdded
            notifyItemChanged(position)
        }

        holder.buttonDelete.setOnClickListener {
            onDeleteClicked(position)
        }

        holder.buttonEdit.setOnClickListener {
            //onEditClicked(currentItem)
        }

    }

    override fun getItemCount() = productList.size

    fun removeItem(position: Int) {
        productList.removeAt(position)
        notifyItemRemoved(position)
    }
}
