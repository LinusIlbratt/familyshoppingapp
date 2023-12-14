package com.example.familyshoppingapp

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.toObject

class InviteDialogFragment(private val invitation: Invitation) : DialogFragment() {

    private lateinit var listName: String
//    private lateinit var invitedByEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getListName(invitation.listId)
//        getInvitedByEmail(invitation.invitedBy)
    }

    private fun getListName(listId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("shoppingLists").document(listId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val shoppingList = document.toObject(ShoppingLists::class.java)
                    listName = shoppingList?.name ?: "Unknown"
                    updateDialog()
                } else {
                    // TODO Handle exception if the list doesnt exist
                }
            }
            .addOnFailureListener {
                // TODO Handle exception
            }
    }

//    private fun getInvitedByEmail(userId: String) {
//        val auth = FirebaseAuth.getInstance()
//
//    }

    private fun updateDialog() {
        dialog?.let { dialog ->
            (dialog as AlertDialog).setMessage("You have been invited to the list $listName by ${invitation.invitedBy}. Do you want to accept?")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Invitation Received")
                .setMessage("Fetching list details....") // Temporary message until list name is fetched
                .setPositiveButton("Accept") { dialog, which ->

                    (activity as FirstActivity).acceptInvitation(invitation)
                }
                .setNegativeButton("Decline") { dialog, which ->

                    (activity as FirstActivity).declineInvitation(invitation)
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}