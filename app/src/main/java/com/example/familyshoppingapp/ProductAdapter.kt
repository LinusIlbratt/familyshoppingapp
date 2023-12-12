package com.example.familyshoppingapp

import android.content.Context
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
        val buttonDelete: Button = view.findViewById(R.id.buttonDelete)
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
            if (currentItem.quantity > 1) {
                currentItem.quantity -= 1
                holder.amountTextView.text = "x${currentItem.quantity}"
            }
        }

        // Change alpha value if a product is added to the cart
        if (currentItem.isAdded) {
            holder.itemView.alpha = 0.5f
        } else {
            holder.itemView.alpha = 1.0f
        }

        holder.buttonAddToCart.setOnClickListener {
            val currentItem = shoppingItemList[position]
            currentItem.isAdded = !currentItem.isAdded

            holder.itemView.alpha = if (currentItem.isAdded) 0.5f else 1.0f

            currentItem.documentId?.let { documentId ->
                updateItemInDatabase(documentId, currentItem)
            }
        }

        holder.buttonDelete.setOnClickListener {
            currentItem.documentId?.let { id ->
                onDeleteClicked(id)
            }
        }

        holder.buttonEdit.setOnClickListener {
            val currentItem = shoppingItemList[position]
            val context = holder.itemView.context
            showEditPopUp(context, currentItem, position)
        }

    }

    override fun getItemCount() = shoppingItemList.size

    fun removeItem(position: Int) {
        shoppingItemList.removeAt(position)
        notifyItemRemoved(position)
    }

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

}
