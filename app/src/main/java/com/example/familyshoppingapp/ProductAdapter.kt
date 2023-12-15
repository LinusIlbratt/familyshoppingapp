package com.example.familyshoppingapp

import android.content.Context
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentId

class ProductAdapter(
    private val productsRef: CollectionReference,
    private val shoppingItemList: MutableList<ShoppingItem>,
    private val onDeleteClicked: (String) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewProductName: TextView = view.findViewById(R.id.productNameTextView)
        val buttonAddToCart: ImageButton = view.findViewById(R.id.buttonAddToCart)
        val buttonEdit: Button = view.findViewById(R.id.buttonEdit)
        val amountTextView: TextView = view.findViewById(R.id.amountTextView)
        val buttonAdd: ImageButton = view.findViewById(R.id.buttonAdd)
        val buttonSubtract: ImageButton = view.findViewById(R.id.buttonSubtract)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentItem = shoppingItemList[position]
        holder.textViewProductName.text = currentItem.name
        holder.amountTextView.text = "x${currentItem.quantity}"

        holder.buttonAdd.setOnClickListener {
            currentItem.quantity += 1
            holder.amountTextView.text = "x${currentItem.quantity}"
        }

        holder.buttonSubtract.setOnClickListener {
            val currentItem = shoppingItemList[position]
            if (currentItem.quantity > 1) {
                // Minska kvantiteten
                currentItem.quantity -= 1
                holder.amountTextView.text = "x${currentItem.quantity}"
                currentItem.documentId?.let { documentId ->
                    updateItemInDatabase(documentId, currentItem)
                }
            } else {
                // Visa en dialogruta för att bekräfta borttagning
                showDeleteConfirmation(holder.itemView.context, position)
            }
        }

        // Change alpha value if a product is added to the cart
        if (currentItem.isAdded) {
            holder.itemView.alpha = 0.5f
            holder.textViewProductName.paintFlags = holder.textViewProductName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.itemView.alpha = 1.0f
            holder.textViewProductName.paintFlags = holder.textViewProductName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.buttonAddToCart.setOnClickListener {
            val currentItem = shoppingItemList[position]
            currentItem.isAdded = !currentItem.isAdded

            if (currentItem.isAdded) {
                holder.itemView.alpha = 0.5f
                holder.textViewProductName.paintFlags = holder.textViewProductName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.itemView.alpha = 1.0f
                holder.textViewProductName.paintFlags = holder.textViewProductName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            currentItem.documentId?.let { documentId ->
                updateItemInDatabase(documentId, currentItem)
            }
        }

        holder.buttonEdit.setOnClickListener {
            val currentItem = shoppingItemList[position]
            val context = holder.itemView.context
            showEditPopUp(context, currentItem, position)
        }

    }

    override fun getItemCount() = shoppingItemList.size


    private fun updateItemInDatabase(documentId: String, shoppingItem: ShoppingItem) {
        productsRef.document(documentId).set(shoppingItem)
            .addOnSuccessListener {
                Log.d("Firestore", "Document successfully updated")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error updating document", e)
            }
    }

    private fun showEditPopUp(context: Context, item: ShoppingItem, position: Int) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogLayout = inflater.inflate(R.layout.edit_item, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.editItemName)

        editText.setText(item.name)

        builder.setView(dialogLayout)
            .setPositiveButton("Save") { dialog, which ->
                val newName = editText.text.toString()
                if (newName.isNotEmpty() && newName != item.name) {
                    item.name = newName // Update name
                    shoppingItemList[position] = item // Update list
                    notifyItemChanged(position)
                    item.documentId?.let { documentId ->
                        updateItemInDatabase(documentId, item)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(context: Context, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Delete Product")
            .setMessage("Do you want to delete this product?")
            .setPositiveButton("Yes") { dialog, which ->
                val id = shoppingItemList[position].documentId
                id?.let { onDeleteClicked(it) }
            }
            .setNegativeButton("No", null)
            .show()
    }

    fun resetAllProducts() {
        for (item in shoppingItemList) {
            if (item.isAdded) {
                item.isAdded = false
                item.documentId?.let { documentId ->
                    updateItemInDatabase(documentId, item)
                }
            }
        }
        notifyDataSetChanged() // Informera adaptern om att datan har ändrats
    }

}
