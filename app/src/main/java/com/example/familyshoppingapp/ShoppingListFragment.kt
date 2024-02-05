package com.example.familyshoppingapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ShoppingListFragment : Fragment(), InviteDialogFragment.InvitationResponseListener {

    interface OnListSelectedListener {
        fun onListSelected(listId: String, listTitle: String)
    }

    private var listSelectedListener: OnListSelectedListener? = null

    fun setOnListSelectedListener(listener: OnListSelectedListener) {
        this.listSelectedListener = listener
    }

    override fun onInvitationAccepted(invitation: Invitation) {
        acceptInvitation(invitation)
    }

    override fun onInvitationDeclined(invitation: Invitation) {
        declineInvitation(invitation)
    }

    private fun showInvitationsPopup(invitationsList: List<Invitation>) {
        if (isAdded) {
            invitationsList.forEach { invitation ->
                if (invitation.status == "pending") {
                    val dialogFragment = InviteDialogFragment(invitation)
                    dialogFragment.setInvitationResponseListener(this) // 'this' refers to an instance of InvitationResponseListener
                    dialogFragment.show(childFragmentManager, "InvitationDialog")
                }
            }
        }
    }


    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CardListsAdapter
    private lateinit var user: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_shopping_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        user = arguments?.getParcelable("USER_DATA")
            ?: throw IllegalStateException("No USER_DATA provided")

        setupRecyclerView()
        loadUserLists(user.userId)
        invitationListener()

        val backArrow = view.findViewById<ImageView>(R.id.backArrow)
        backArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
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
                        Toast.makeText(
                            context,
                            "Error loading invitations: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addSnapshotListener
                    }

                    val invitationsList = mutableListOf<Invitation>()
                    snapshots?.forEach { doc ->
                        val invitation =
                            doc.toObject(Invitation::class.java).copy(documentId = doc.id)
                        invitationsList.add(invitation)
                    }

                    showInvitationsPopup(invitationsList)
                }
        } else {
            Toast.makeText(context, "Invalid email, please create one", Toast.LENGTH_SHORT)
                .show() // This exception should never occur since you cant sign in without an google mail.
        }
    }

    private fun setupRecyclerView() {
        recyclerView = view?.findViewById(R.id.recyclerViewShopList) ?: return
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)


        adapter = CardListsAdapter(
            onItemClicked = { shoppingList ->
                if (shoppingList == null || shoppingList.isCardEmpty) {
                    popUpForNewCardList()
                } else {

                    listSelectedListener?.onListSelected(
                        shoppingList.documentId ?: "defaultListId",
                        shoppingList.name ?: "Default List"
                    )
                }
            },
            onInviteClicked = { listId ->
                showInvitePopup(listId)
            },
            onItemLongClicked = { shoppingList ->
                // Handle long click for item deletion
                if (shoppingList != null && !shoppingList.isCardEmpty) {
                    showDeleteList(shoppingList)
                }
            }
        )

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter
    }

    private fun loadUserLists(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val listsCollection = db.collection("shoppingLists")


        listsCollection.whereArrayContains("members", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading lists: ${e.message}",
                        Toast.LENGTH_LONG
                    )
                        .show()
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
        val popUpView =
            LayoutInflater.from(requireContext()).inflate(R.layout.add_shopping_list, null)
        val editTextListName = popUpView.findViewById<EditText>(R.id.editTextListName)
        val editTextCategory = popUpView.findViewById<EditText>(R.id.editTextCategory)

        AlertDialog.Builder(requireActivity(), R.style.CustomAlertDialog)
            .setView(popUpView)
            .setTitle("Create a new list")
            .setPositiveButton("Save") { dialog, which ->
                val name = editTextListName.text.toString()
                val category = editTextCategory.text.toString()
                if (name.isBlank() || category.isBlank()) {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_LONG)
                        .show()
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
                Toast.makeText(requireContext(), "List added:", Toast.LENGTH_SHORT).show()
                loadUserLists(user.userId) // Update the users lists
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error adding list: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showInvitePopup(listId: String) {

        val inviteView = LayoutInflater.from(requireContext()).inflate(R.layout.send_invite, null)
        val emailEditText = inviteView.findViewById<EditText>(R.id.editTextEmailAddress)

        AlertDialog.Builder(requireActivity(), R.style.CustomAlertDialog)
            .setView(inviteView)
            .setTitle("")
            .setPositiveButton("Send") { invite, which ->
                val email = emailEditText.text.toString()
                sendInvite(listId, email)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteList(shoppingLists: ShoppingLists) {
        AlertDialog.Builder(requireActivity())
            .setTitle("Delete List")
            .setMessage("Are you sure you want to delete the list '${shoppingLists.name}'?")
            .setPositiveButton("Delete") { dialog, which ->
                deleteShoppingList(shoppingLists, user.userId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteShoppingList(shoppingLists: ShoppingLists, userId: String) {
        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        shoppingLists.documentId?.let { documentId ->
            db.collection("shoppingLists").document(documentId).get()
                .addOnSuccessListener { document ->
                    val members = document.get("members") as? List<String> ?: emptyList()

                    if (members.size <= 1) {

                        deleteAllProductsAndList(documentId, db, storage)
                    } else {

                        removeUserFromList(documentId, userId, db)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error fetching list details", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun deleteAllProductsAndList(listId: String, db: FirebaseFirestore, storage: FirebaseStorage) {

        db.collection("products").whereEqualTo("listId", listId).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val imageUrl = document.getString("imageUrl")
                    imageUrl?.let {

                        val imageRef = storage.getReferenceFromUrl(it)
                        imageRef.delete()
                    }

                    db.collection("products").document(document.id).delete()
                }

                
                db.collection("shoppingLists").document(listId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "List deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error deleting list", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching products", Toast.LENGTH_LONG).show()
            }
    }

    private fun removeUserFromList(listId: String, userId: String, db: FirebaseFirestore) {
        db.collection("shoppingLists").document(listId)
            .update("members", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "You have left the list", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error leaving list", Toast.LENGTH_LONG).show()
            }
    }

    private fun sendInvite(listId: String, email: String) {
        val db = FirebaseFirestore.getInstance()


        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { users ->
                if (users.isEmpty) {
                    Toast.makeText(context, "No user found with this email", Toast.LENGTH_LONG)
                        .show()
                } else {
                    val userId = users.documents.first().id


                    db.collection("shoppingLists").document(listId).get()
                        .addOnSuccessListener { document ->
                            val members = document.get("members") as? List<String> ?: listOf()

                            if (userId in members) {
                                Toast.makeText(
                                    requireContext(),
                                    "User is already a member in your list",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                checkForPendingInvitation(listId, email, db)
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                requireContext(),
                                "Error checking list membership:",
                                Toast.LENGTH_LONG
                            ).show()

                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Can't find user by email:", Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun checkForPendingInvitation(listId: String, email: String, db: FirebaseFirestore) {
        db.collection("invitations")
            .whereEqualTo("listId", listId)
            .whereEqualTo("invitedEmail", email)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    checkIfEmailExists(email, listId, db)
                } else {
                    Toast.makeText(
                        context,
                        "An invite is already pending to this email",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { e ->

            }
    }

    private fun checkIfEmailExists(email: String, listId: String, db: FirebaseFirestore) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    createNewInvite(listId, email, db)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No user found with this email",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { e ->

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
                Toast.makeText(requireContext(), "Invitation sent successfully", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Sending invitation failed, try again later",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
    }


    private fun acceptInvitation(invitation: Invitation) {
        try {
            val db = FirebaseFirestore.getInstance()

            // Update status on the invitation
            db.collection("invitations").document(invitation.documentId)
                .update("status", "accepted")
                .addOnSuccessListener {
                    addUserToList(
                        invitation.listId,
                        user.userId
                    ) // Adding member to the members list
                    Toast.makeText(requireContext(), "Invitation accepted", Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        "Can't accept the invitation, please try again",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } catch (e: Exception) {
            Log.d("!!!", "Exception: ${e.message}")
        }
    }


    private fun addUserToList(listId: String, userId: String) {
        val db = FirebaseFirestore.getInstance()
        val listRef = db.collection("shoppingLists").document(listId)

        listRef.update("members", FieldValue.arrayUnion(userId))
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Member added to list successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Can't add member to list.", Toast.LENGTH_LONG)
                    .show()
            }
    }


    private fun declineInvitation(invitation: Invitation) {
        val db = FirebaseFirestore.getInstance()
        db.collection("invitations").document(invitation.documentId)
            .update("status", "declined")
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Invitation declined", Toast.LENGTH_SHORT).show()
                loadUserLists(user.userId) // Update user UI
            }
            .addOnFailureListener { e ->

            }
    }

    companion object {
        fun newInstance(user: User): ShoppingListFragment {
            val fragment = ShoppingListFragment()
            val args = Bundle()
            args.putParcelable("USER_DATA", user)
            fragment.arguments = args
            return fragment
        }
    }

}
