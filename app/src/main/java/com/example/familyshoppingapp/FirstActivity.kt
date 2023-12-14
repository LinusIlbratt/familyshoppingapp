package com.example.familyshoppingapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
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
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)

        user = intent.getParcelableExtra("USER_DATA") ?: throw IllegalStateException("No USER_DATA provided")

        setupRecyclerView()

        loadUserLists(user.userId)

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
                        Toast.makeText(this, "Error loading invitations: ${e.message}", Toast.LENGTH_LONG).show()
                        return@addSnapshotListener
                    }

                    val invitationsList = mutableListOf<Invitation>()
                    snapshots?.forEach { doc ->
                        val invitation = doc.toObject(Invitation::class.java).copy(documentId = doc.id)
                        invitationsList.add(invitation)
                    }

                    showInvitationsPopup(invitationsList)
                }
        } else {
            Toast.makeText(this, "Invalid email, please create one", Toast.LENGTH_SHORT).show() // This exception should never occur since you cant sign in without an email.
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewLists)


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
        val listsCollection = db.collection("shoppingLists")


        listsCollection.whereArrayContains("members", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading lists: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                val userLists = snapshots?.mapNotNull { doc ->
                    doc.toObject(ShoppingLists::class.java).apply {
                        documentId = doc.id
                    }
                }?.toMutableList() ?: mutableListOf()

                userLists.add(ShoppingLists(isCardEmpty = true))
                adapter.setItems(userLists)
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
                if (name.isBlank() || category.isBlank()) {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_LONG).show()
                } else {
                    addNewList(name, category)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun addNewList(name: String, category: String) {
        val newList = ShoppingLists(name, category, members = listOf(user.userId))
        val db = FirebaseFirestore.getInstance()

        db.collection("shoppingLists")
            .add(newList)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "List added:", Toast.LENGTH_SHORT).show()
                loadUserLists(user.userId) // Update the users lists
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error adding list: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showInvitePopup(listId: String) {
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

    private fun sendInvite(listId: String, email: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("invitations")
            .whereEqualTo("listId", listId)
            .whereEqualTo("invitedEmail", email)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    createNewInvite(listId, email, db)
                } else {
                    Toast.makeText(this, "An invite is already pending to this email", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error sending invite: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun createNewInvite(listId: String, email: String, db: FirebaseFirestore) {
        val newInvite = hashMapOf(
            "listId" to listId,
            "invitedEmail" to email,
            "status" to "pending",
            "invitedBy" to user.email
        )

        db.collection("invitations").add(newInvite)
            .addOnSuccessListener {
                // TODO Add notification for invitation being sent
            }
            .addOnFailureListener { e ->
                // TODO Handle exception!
            }
    }


    private fun showInvitationsPopup(invitationsList: List<Invitation>) {
        invitationsList.forEach { invitation ->
            if (invitation.status == "pending") {
                val dialogFragment = InviteDialogFragment(invitation)
                dialogFragment.show(supportFragmentManager, "InvitationDialog")
            }
        }
    }

    fun acceptInvitation(invitation: Invitation) {
        try {
            val db = FirebaseFirestore.getInstance()

            // Update invite status
            db.collection("invitations").document(invitation.documentId)
                .update("status", "accepted")
                .addOnSuccessListener {
                    // Add members to the members list
                    addUserToList(invitation.listId, user.userId)
                    // Update User UI
                    loadUserLists(user.userId)
                }
                .addOnFailureListener {
                    // TODO Handle exception!
                }
        } catch (e: Exception) {
            Log.e("Exception", "Error accepting invitation: ${e.message}")
        }
    }

    private fun addUserToList(listId: String, userId: String) {
        val db = FirebaseFirestore.getInstance()
        val listRef = db.collection("shoppingLists").document(listId)

        listRef.update("members", FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                // TODO add notification for a member being added to the members list?
            }
            .addOnFailureListener {
                // TODO Handle exception!
            }
    }

    fun declineInvitation(invitation: Invitation) {
        val db = FirebaseFirestore.getInstance()
        db.collection("invitations").document(invitation.documentId)
            .update("status", "declined")
            .addOnSuccessListener {

                // Update user UI after declining an invite
                loadUserLists(user.userId)
            }
            .addOnFailureListener {
                // TODO Handle exception!
            }
    }

}
