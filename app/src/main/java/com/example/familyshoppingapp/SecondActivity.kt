package com.example.familyshoppingapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

class SecondActivity : AppCompatActivity() {

    private val database = Firebase.firestore
    private val productsRef = database.collection("products")
    private val shoppingItemList = mutableListOf<ShoppingItem>()
    private var snapshotListener: ListenerRegistration? = null
    private lateinit var adapter: ProductAdapter
    private lateinit var listId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        listId = intent.getStringExtra("LIST_ID") ?: "defaultListId"

        val listTitle = intent.getStringExtra("LIST_TITLE") ?: "Default Title"

        val titleTextView = findViewById<TextView>(R.id.shoppingListTitelText)
        titleTextView.text = listTitle

        adapter = ProductAdapter(productsRef, shoppingItemList) { documentId ->
            removeItemsFromDatabase(documentId)
        }

        val backArrow = findViewById<ImageView>(R.id.backArrow)
        backArrow.setOnClickListener {
            finish()
        }

        val resetButton = findViewById<Button>(R.id.resetAllButton)
        resetButton.setOnClickListener {
            adapter.resetAllProducts()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val floatingButton: FloatingActionButton = findViewById(R.id.addItem)
        floatingButton.setOnClickListener {

            addNewItemPopUpWindow()
        }
        setupSnapshotListener()
    }

    private fun setupSnapshotListener() {
        snapshotListener = productsRef.whereEqualTo("listId", listId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.d("!!!", "Line 70, Second Activity. Error loading items:")
                    return@addSnapshotListener
                }
                shoppingItemList.clear()
                snapshot?.forEach { document ->
                    val item = document.toObject<ShoppingItem>().copy(documentId = document.id)
                    if (item.listId == listId) {
                        shoppingItemList.add(item)
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        snapshotListener?.remove()
    }

    private fun addItemsToDatabase(shoppingItem: ShoppingItem) {
        val newItem = shoppingItem.copy(listId = listId)
        productsRef.add(newItem)
            .addOnSuccessListener { documentReference ->

            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error adding item", Toast.LENGTH_LONG).show()
            }
    }


    private fun addNewItemPopUpWindow() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.add_shopping_item, null)
        val editItemName = dialogLayout.findViewById<EditText>(R.id.addItemName)

        builder.setView(dialogLayout)
            .setPositiveButton("Add") { dialog, which ->
                val itemName = editItemName.text.toString()

                if (itemName.isBlank()) {
                    Toast.makeText(this, "Item name cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    val newItem = ShoppingItem(name = itemName, listId = listId)
                    addItemsToDatabase(newItem)
                }
            }
        builder.show()
    }

    private fun removeItemsFromDatabase(documentId: String) {
        productsRef.document(documentId).delete()
            .addOnSuccessListener {

            }
            .addOnFailureListener { e ->

            }
    }
}