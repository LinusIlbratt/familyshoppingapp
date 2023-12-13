package com.example.familyshoppingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.IllegalStateException

class FirstActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CardListsAdapter
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)
        Log.d("!!!", "First Activity")

        userId = intent.getStringExtra("USER_ID") ?: throw IllegalStateException("No USER_ID provided")

        setupRecyclerView()
        loadUserLists(userId)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewLists)

        adapter = CardListsAdapter { shoppingList ->
            if (shoppingList == null || shoppingList.isCardEmpty) {

                popUpForNewCardList()
            } else {
                val intent = Intent(this, SecondActivity::class.java)
                intent.putExtra("LIST_ID", shoppingList.documentId)
                startActivity(intent)
            }
        }
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }

    private fun loadUserLists(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val listsCollection = db.collection("users").document(userId).collection("shoppingLists")

        listsCollection.get().addOnSuccessListener { documents ->
            val userLists = documents.mapNotNull { doc ->
                doc.toObject(ShoppingLists::class.java)
            }.toMutableList()
            userLists.add(ShoppingLists(isCardEmpty = true)) // Lägg till 'null' för att representera det tomma kortet
            adapter.setItems(userLists)
        }.addOnFailureListener { e ->
            // Hantera eventuella fel här, t.ex. visa ett felmeddelande till användaren
            Log.w("!!!", "Error getting documents: ", e)
        }
    }

    private fun popUpForNewCardList() {
        val popUpView = LayoutInflater.from(this).inflate(R.layout.add_shopping_list, null)
        val editTextListName = popUpView.findViewById<EditText>(R.id.editTextListName)
        val editTextCategory = popUpView.findViewById<EditText>(R.id.editTextCategory)

        AlertDialog.Builder(this)
            .setView(popUpView)
            .setTitle("Create a new list")
            .setPositiveButton("Save") { dialog, which ->
                val name = editTextListName.text.toString()
                val category = editTextCategory.text.toString()
                if (name.isNotBlank() && category.isNotBlank()) {
                    addNewList(name, category)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addNewList(name: String, category: String) {
        val newList = ShoppingLists(name, category)
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).collection("shoppingLists")
            .add(newList)
            .addOnSuccessListener {
                loadUserLists(userId)
            }
            .addOnFailureListener { e ->
                // Hantera fel, t.ex. visa ett felmeddelande
            }
    }
}
