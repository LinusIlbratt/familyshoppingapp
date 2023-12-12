package com.example.familyshoppingapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.IllegalStateException

class FirstActivity : AppCompatActivity() {

    lateinit var recyclerView: RecyclerView
    lateinit var adapter: CardListsAdapter
    lateinit var userId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)

        userId = intent.getStringExtra("USER_ID") ?: throw IllegalStateException("No USER_ID provided")

        setupRecyclerView()
        loadUserLists(userId)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewLists)
        adapter = CardListsAdapter { shoppingList ->
            if (shoppingList == null) {
                // Kod för att skapa en ny lista
            } else {
                // Kod för att öppna en befintlig lista
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
            }
            adapter.setItems(userLists)
        }.addOnFailureListener { e ->
            // Hantera eventuella fel här, t.ex. visa ett felmeddelande till användaren
            Log.w("Firestore", "Error getting documents: ", e)
        }
    }

    private fun addNewList(name: String, category: String) {
        val newList = ShoppingLists(name, category)
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).collection("shoppingLists")
            .add(newList)
            .addOnSuccessListener {
                // Uppdatera RecyclerView med den nya listan
                loadUserLists(userId)
            }
            .addOnFailureListener { e ->
                // Hantera fel, t.ex. visa ett felmeddelande
            }
    }
}