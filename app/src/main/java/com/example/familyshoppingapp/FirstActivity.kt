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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
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

        invitationListener()
    }

    private fun invitationListener() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email

        if (userEmail != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("invitations")
                .whereEqualTo("invitedEmail", userEmail)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        // Hantera fel
                        return@addSnapshotListener
                    }

                    val invitationsList = mutableListOf<Invitation>()
                    for (doc in snapshots!!) {
                        val invitation = doc.toObject(Invitation::class.java).copy(documentId = doc.id)
                        invitationsList.add(invitation)
                    }

                    showInvitationsPopup(invitationsList)
                }
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewLists)

        // Uppdatera för att inkludera den nya callbacken
        adapter = CardListsAdapter(
            onItemClicked = { shoppingList ->
                if (shoppingList == null || shoppingList.isCardEmpty) {
                    popUpForNewCardList()
                } else {
                    val intent = Intent(this, SecondActivity::class.java)
                    intent.putExtra("LIST_ID", shoppingList.documentId)
                    Log.d("!!!", "Starting SecondActivity with LIST_ID: ${shoppingList.documentId}")
                    startActivity(intent)
                }
            },
            onInviteClicked = { listId ->
                showInvitePopup(listId)
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }

    private fun loadUserLists(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val listsCollection = db.collection("users").document(userId).collection("shoppingLists")

        listsCollection.get().addOnSuccessListener { documents ->
            val userLists = documents.mapNotNull { list ->
                list.toObject(ShoppingLists::class.java).apply {
                    documentId = list.id
                }
            }.toMutableList()
            userLists.add(ShoppingLists(isCardEmpty = true))
            adapter.setItems(userLists)
        }.addOnFailureListener { e ->

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
            .addOnSuccessListener { documentReference ->

                Log.d("!!!", "List added with ID: ${documentReference.id}")
                loadUserLists(userId)
            }
            .addOnFailureListener { e ->
                Log.w("!!!", "Error adding document", e)
            }
    }

    fun showInvitePopup(listId: String) {
        val inviteView = LayoutInflater.from(this).inflate(R.layout.send_invite, null)
        val emailEditText = inviteView.findViewById<EditText>(R.id.editTextEmailAddress)

        AlertDialog.Builder(this)
            .setView(inviteView)
            .setTitle("")
            .setPositiveButton("Send") { invite, which ->
                val email = emailEditText.text.toString()
                sendInvite(listId, email)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun sendInvite(listId: String, email: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("invitations")
            .whereEqualTo("listId", listId)
            .whereEqualTo("invitedEmail", email)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Skapa ny inbjudan
                    createNewInvite(listId, email, db)
                } else {
                    // Redan en "pending" inbjudan finns
                    // Hantera detta fall (t.ex. visa ett meddelande till användaren)
                }
            }
            .addOnFailureListener { e ->
                // Hantera eventuella fel
            }
    }

    private fun createNewInvite(listId: String, email: String, db: FirebaseFirestore) {
        val newInvite = hashMapOf(
            "listId" to listId,
            "invitedEmail" to email,
            "status" to "pending",
            "invitedBy" to userId
        )

        db.collection("invitations").add(newInvite)
            .addOnSuccessListener {
                // lyckas?
            }
            .addOnFailureListener { e ->
                // misslyckas?
            }
    }


    private fun showInvitationsPopup(invitationsList: List<Invitation>) {
        invitationsList.forEach { invitation ->
            if (invitation.status == "pending") {
                AlertDialog.Builder(this)
                    .setTitle("Invitation Received")
                    .setMessage("You have been invited to ${invitation.listId} by ${invitation.invitedBy}. Do you want to accept?")
                    .setPositiveButton("Accept") { dialog, which ->
                        Log.d("!!!", "accept pressed")
                        acceptInvitation(invitation)
                    }
                    .setNegativeButton("Decline") { dialog, which ->
                        declineInvitation(invitation)
                    }
                    .show()
            }
        }
    }

    private fun acceptInvitation(invitation: Invitation) {
        try {
            val db = FirebaseFirestore.getInstance()

            // Uppdatera status för inbjudan
            db.collection("invitations").document(invitation.documentId)
                .update("status", "accepted")
                .addOnSuccessListener {
                    // Uppdatering lyckades, nu lägg till användaren i listans tillgängliga användare
                    addUserToList(invitation.listId, userId)
                    // Uppdatera användargränssnittet här efter att ha accepterat inbjudan
                    loadUserLists(userId)
                }
                .addOnFailureListener {
                    // Hantera eventuella fel
                }
        } catch (e: Exception) {
            // Hantera undantag här och logga dem för att felsöka
            Log.e("Exception", "Error accepting invitation: ${e.message}")
        }
    }

    private fun addUserToList(listId: String, userId: String) {
        val db = FirebaseFirestore.getInstance()
        val listRef = db.collection("shoppingLists").document(listId)

        // Antag att listan har ett fält 'members' som är en lista av användar-ID:n
        listRef.update("members", FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                // Användaren har lagts till i listan
            }
            .addOnFailureListener {
                // Hantera eventuella fel
            }
    }

    private fun declineInvitation(invitation: Invitation) {
        val db = FirebaseFirestore.getInstance()
        db.collection("invitations").document(invitation.documentId)
            .update("status", "declined")
            .addOnSuccessListener {
                // Uppdatering lyckades
                // Uppdatera användargränssnittet här efter att ha avvisat inbjudan
                loadUserLists(userId)
            }
            .addOnFailureListener {
                // Hantera eventuella fel
            }
    }

}
