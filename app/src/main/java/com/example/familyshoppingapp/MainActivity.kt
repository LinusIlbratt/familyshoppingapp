package com.example.familyshoppingapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class MainActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        googleSignInLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    handleSignInResult(task)
                }
            }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInButton = findViewById<Button>(R.id.btn_google_sign_in)
        googleSignInButton.setOnClickListener {
            googleSignIn()
        }

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        findViewById<Button>(R.id.btn_email_sign_up).setOnClickListener {
            showSignUpDialog()
        }

        findViewById<Button>(R.id.btn_email_sign_in).setOnClickListener {
            showSignInDialog()
        }
    }

    private fun showSignUpDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sign_up, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.editTextPassword)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Sign Up")
            .setPositiveButton("Register") { dialog, which ->
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()
                signUpUser(email, password)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSignInDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sign_in, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.editTextPassword)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Sign In")
            .setPositiveButton("Login") { dialog, which ->
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()
                signInUser(email, password)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun signUpUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registrering lyckad, uppdatera UI med användarens information
                    val firebaseUser = auth.currentUser
                    updateUI(firebaseUser)
                } else {
                    // Om registreringen misslyckas, visa ett meddelande till användaren
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun signInUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Inloggning lyckad, uppdatera UI med användarens information
                    val firebaseUser = auth.currentUser
                    updateUI(firebaseUser)
                } else {
                    // Om inloggningen misslyckas, visa ett meddelande till användaren
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Användaren är inloggad, navigera till MenuActivity
            goToMenuActivity()
        } else {
            // Användaren är inte inloggad, förbli på denna skärm
        }
    }


    private fun goToMenuActivity() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {

            val userId = firebaseUser.uid
            val email = firebaseUser.email ?: ""


            val user = User(userId = userId, email = email)


            val intent = Intent(this, MenuActivity::class.java).apply {
                putExtra("USER_DATA", user)
            }
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                "You are not logged in. Please log in to continue",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // TODO Handle exception
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    // Check if the user is new or a existing one
                    if (task.result?.additionalUserInfo?.isNewUser == true) {

                        saveUserDataToFirestore(firebaseUser)
                    }
                    goToMenuActivity()
                }
            } else {
                // TODO Handle failed authentication
            }
        }
    }

    private fun saveUserDataToFirestore(firebaseUser: FirebaseUser) {
        val user = hashMapOf(
            "userId" to firebaseUser.uid,
            "email" to firebaseUser.email
            // Add more user information here if needed
        )

        db.collection("users").document(firebaseUser.uid).set(user)
            .addOnSuccessListener {
                Log.d("Firestore", "User data saved successfully")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error saving user data", e)
            }
    }

}