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
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.storage.FirebaseStorage
import java.util.Collections


interface OnGalleryIconClickListener {
    fun onGalleryIconClick(item: ShoppingItem)
}

interface OnCameraIconClickListener {
    fun onCameraIconClick(item: ShoppingItem)
}

interface OnImageUpdatedListener {
    fun onImageUpdated(imageUrl: String)
}

class ProductAdapter(
    private val productsRef: CollectionReference,
    private val shoppingItemList: MutableList<ShoppingItem>,
    private val onDeleteClicked: (String) -> Unit,
    private val onCameraIconClickListener: OnCameraIconClickListener,
    private val onGalleryIconClickListener: OnGalleryIconClickListener,
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewProductName: TextView = view.findViewById(R.id.productNameTextView)
        val buttonAddToCart: ImageButton = view.findViewById(R.id.buttonAddToCart)
        val buttonEdit: Button = view.findViewById(R.id.buttonEdit)
        val amountTextView: TextView = view.findViewById(R.id.amountTextView)
        val buttonAdd: ImageButton = view.findViewById(R.id.buttonAdd)
        val buttonSubtract: ImageButton = view.findViewById(R.id.buttonSubtract)
    }

    interface OnItemMoveCompleteListener {
        fun onItemMoveCompleted()
    }

    var onItemMoveCompleteListener: OnItemMoveCompleteListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentItem = shoppingItemList[holder.adapterPosition]
        holder.textViewProductName.text = currentItem.name
        holder.amountTextView.text = "x${currentItem.quantity}"

        updateItemAppearanceBasedOnItemInCart(currentItem, holder)

        holder.buttonAdd.setOnClickListener {
            currentItem.quantity += 1
            holder.amountTextView.text = "x${currentItem.quantity}"
            // Update the object in firestore
            currentItem.documentId?.let { documentId ->
                updateItemInDatabase(documentId, currentItem)
            }
        }

        holder.textViewProductName.setOnClickListener {
            showProductPopup(holder.itemView.context, currentItem, object : OnImageUpdatedListener {
                override fun onImageUpdated(imageUrl: String) {
                    currentItem.documentId?.let { documentId ->
                        updateProductImage(documentId, imageUrl)
                    }
                }
            })
        }

        holder.buttonSubtract.setOnClickListener {
            if (currentItem.quantity > 1) {
                currentItem.quantity -= 1
                holder.amountTextView.text = "x${currentItem.quantity}"
                currentItem.documentId?.let { documentId ->
                    updateItemInDatabase(documentId, currentItem)
                }
            } else {
                showItemDeleteConfirm(holder.itemView.context, holder.adapterPosition)
            }
        }

        holder.buttonAddToCart.setOnClickListener {
            currentItem.isAdded = !currentItem.isAdded
            updateItemAppearanceBasedOnItemInCart(currentItem, holder)
            currentItem.documentId?.let { it1 -> updateItemInDatabase(it1, currentItem) }
        }

        holder.buttonEdit.setOnClickListener {
            val item = shoppingItemList[holder.adapterPosition]
            showEditPopUp(holder.itemView.context, item, holder.adapterPosition)
        }

    }

    private fun updateItemAppearanceBasedOnItemInCart(currentItem: ShoppingItem, holder: ProductViewHolder) {
        if (currentItem.isAdded) {
            holder.itemView.alpha = 0.5f
            holder.textViewProductName.paintFlags = holder.textViewProductName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.itemView.alpha = 1.0f
            holder.textViewProductName.paintFlags = holder.textViewProductName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    override fun getItemCount() = shoppingItemList.size


    private fun updateItemInDatabase(documentId: String, shoppingItem: ShoppingItem) {
        productsRef.document(documentId).set(shoppingItem)
            .addOnSuccessListener {
                Log.d("Firestore", "Document successfully updated")
                notifyDataSetChanged() // Informera adaptern om att datan har ändrats
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error updating document", e)
            }
    }

    private fun showEditPopUp(context: Context, item: ShoppingItem, position: Int) {
        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog)
        val inflater = LayoutInflater.from(context)
        val dialogLayout = inflater.inflate(R.layout.edit_item, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.editItemName)
        editText.setText(item.name)

        builder.setView(dialogLayout)
            .setPositiveButton("Save") { dialog, which ->
                val newName = editText.text.toString()
                if (newName.isNotEmpty() && newName != item.name) {
                    item.name = newName // Uppdatera namnet på objektet
                    shoppingItemList[position].name = newName // Uppdatera listan
                    notifyItemChanged(position) // Meddela adaptern att uppdatera vyn
                    item.documentId?.let { documentId ->
                        updateItemInDatabase(documentId, item) // Uppdatera objektet i databasen
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }



    fun updateProductImage(documentId: String, imageUrl: String) {
        val index = shoppingItemList.indexOfFirst { it.documentId == documentId }
        if (index != -1) {
            shoppingItemList[index].imageUrl = imageUrl
            notifyItemChanged(index)
        }
    }

    private fun showItemDeleteConfirm(context: Context, position: Int) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_custom_message_text, null)
        val messageView = view.findViewById<TextView>(R.id.dialog_message)

        val message = context.getString(R.string.delete_product_item)
        messageView.text = message

        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog)
        builder.setView(view)
            .setPositiveButton("Yes") { dialog, which ->
                val id = shoppingItemList[position].documentId
                id?.let { onDeleteClicked(it) }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun removeImage(item: ShoppingItem) {
        // Remove image from firebase
        item.imageUrl?.let { imageUrl ->
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageRef.delete().addOnSuccessListener {
                Log.d("!!!", "Image deleted")
            }.addOnFailureListener {
                Log.w("!!!", "Error deleting image")
            }
        }

        // Update ShoppingItem and remove the image url
        item.imageUrl = null
        item.documentId?.let { documentId ->
            updateItemInDatabase(documentId, item)
        }
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
        notifyDataSetChanged()
    }

    private fun showProductPopup(
        context: Context,
        item: ShoppingItem,
        onImageUpdatedListener: OnImageUpdatedListener
    ): AlertDialog {
        val dialogLayout = LayoutInflater.from(context).inflate(R.layout.product_popup, null)
        val uploadImageToImageView =
            dialogLayout.findViewById<ImageView>(R.id.uploadImageToImageView)
        val imageViewCamera = dialogLayout.findViewById<ImageView>(R.id.cameraIcon)
        val galleryIcon = dialogLayout.findViewById<ImageView>(R.id.galleryIcon)

        // Create dialog popup
        val dialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setView(dialogLayout)
            .setPositiveButton("Close", null)
            .create()

        // Load image if there is one
        loadProductImage(item.imageUrl, uploadImageToImageView, context)

        // Handle long-press to delete iamge
        setupImageLongClick(uploadImageToImageView, context, item, dialog)

        // Handle click on Gallery Icon
        setupGalleryIconClick(galleryIcon, item, dialog)

        // Handle click on Camera Icon
        setupCameraIconClick(imageViewCamera, item, dialog)

        // Visa dialogen
        dialog.show()
        return dialog
    }

    private fun loadProductImage(imageUrl: String?, imageView: ImageView, context: Context) {
        imageUrl?.let {
            Glide.with(context).load(it).into(imageView)
        }
    }

    private fun setupImageLongClick(
        imageView: ImageView,
        context: Context,
        item: ShoppingItem,
        dialog: AlertDialog
    ) {
        imageView.setOnLongClickListener {
            val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog)
            val dialogLayout =
                LayoutInflater.from(context).inflate(R.layout.dialog_custom_message_text, null)
            val messageView = dialogLayout.findViewById<TextView>(R.id.dialog_message)

            val message = context.getString(R.string.delete_image_confirmation_text)
            messageView.text = message

            builder.setView(dialogLayout)
                .setPositiveButton("Yes") { _, _ ->
                    removeImage(item)
                    dialog.dismiss()
                }
                .setNegativeButton("No", null)
                .show()
            true
        }
    }


    private fun setupGalleryIconClick(
        galleryIcon: ImageView,
        item: ShoppingItem,
        dialog: AlertDialog
    ) {
        galleryIcon.setOnClickListener {
            onGalleryIconClickListener.onGalleryIconClick(item)
            dialog.dismiss()
        }
    }

    private fun setupCameraIconClick(
        imageView: ImageView,
        item: ShoppingItem,
        dialog: AlertDialog
    ) {
        imageView.setOnClickListener {
            onCameraIconClickListener.onCameraIconClick(item)
            dialog.dismiss() // Stäng huvuddialogen
        }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        Collections.swap(shoppingItemList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        onItemMoveCompleteListener?.onItemMoveCompleted()
    }


}


