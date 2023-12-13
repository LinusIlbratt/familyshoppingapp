package com.example.familyshoppingapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class TestActivity : AppCompatActivity() {

    private lateinit var listId: String
    private val database = Firebase.firestore
    private val productsRef = database.collection("products")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        listId = intent.getStringExtra("LIST_ID") ?: return
        Log.d("!!!", "List ID: $listId")

        val floatingTest: FloatingActionButton = findViewById(R.id.floatingTest)
        floatingTest.setOnClickListener {
            Log.d("!!!", "test")
            addNewItemPopUpWindow()
        }
    }

    private fun addNewItemPopUpWindow() {
        Log.d("!!!", "Inside addNewItemPopUp")
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.add_shopping_item, null)
        val editItemName = dialogLayout.findViewById<EditText>(R.id.addItemName)

        builder.setView(dialogLayout)
            .setPositiveButton("Add") { dialog, which ->
                val itemName = editItemName.text.toString()

                if (itemName.isNotEmpty()) {
                    val newItem = ShoppingItem(name = itemName, listId = listId) // Inkludera listId här
                    addItemsToDatabase(newItem)
                }
            }
        builder.show()
    }

    private fun addItemsToDatabase(shoppingItem: ShoppingItem) {
        val newItem = shoppingItem.copy(listId = listId)
        productsRef.add(newItem)
            .addOnSuccessListener { documentReference ->
                Log.d("!!!", "Item added to Firestore")
            }
            .addOnFailureListener { e ->
                Log.w("!!!", "Error adding item to Firestore")
            }
    }
}