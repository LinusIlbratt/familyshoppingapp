package com.example.familyshoppingapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

class MainActivity : AppCompatActivity() {

    private val database = Firebase.firestore
    private val productsRef = database.collection("products")
    private val shoppingItemList = mutableListOf<ShoppingItem>()
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = ProductAdapter(shoppingItemList) { documentId ->
            removeItemsFromDatabase(documentId)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        productsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("!!!", "Listen failed!", e)
                return@addSnapshotListener
            }

            shoppingItemList.clear()
            snapshot?.forEach { document ->
                val item = document.toObject<ShoppingItem>().copy(documentId = document.id)
                shoppingItemList.add(item)
            }
            adapter.notifyDataSetChanged()
        }

        val floatingButton: FloatingActionButton = findViewById(R.id.addItem)
        floatingButton.setOnClickListener {
            Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show()
            addNewItemPopUpWindow()

        }

    }

    private fun addItemsToDatabase(shoppingItem: ShoppingItem) {
        productsRef.add(shoppingItem)
            .addOnSuccessListener { documentReference ->
                Log.d("!!!", "Item added to Firestore")
            }
            .addOnFailureListener { e ->
                Log.w("!!!", "Error adding item to Firestore")
            }
    }


    private fun addNewItemPopUpWindow() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.add_shopping_item, null)
        val editItemName = dialogLayout.findViewById<EditText>(R.id.editItemName)

        builder.setView(dialogLayout)
            .setPositiveButton("Add") { dialog, which ->
                val itemName = editItemName.text.toString()

                if (itemName.isNotEmpty()) {
                    val newItem = ShoppingItem(name = itemName)
                    addItemsToDatabase(newItem)
                }
            }
        builder.show()

    }

    private fun removeItemsFromDatabase(documentId: String) {
        productsRef.document(documentId).delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Document successfully deleted")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error deleting document", e)
            }
    }


}