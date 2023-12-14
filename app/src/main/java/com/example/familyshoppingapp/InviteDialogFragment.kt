package com.example.familyshoppingapp

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class InviteDialogFragment(private val invitation: Invitation) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Invitation Received")
                .setMessage("You have been invited to ${invitation.listId} by ${invitation.invitedBy}. Do you want to accept?")
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