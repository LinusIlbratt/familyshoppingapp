package com.example.familyshoppingapp

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText
    private val db = Firebase.firestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth
        emailView = findViewById(R.id.emailEditText)
        passwordView = findViewById(R.id.passwordEditText)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
            }
        }

        val signUpButton = findViewById<Button>(R.id.btn_signUp)
        signUpButton.setOnClickListener {
            signUp()
        }

        val signInButton = findViewById<Button>(R.id.btn_signIn)
        signInButton.setOnClickListener {
            signIn()
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
    }

    private fun goToSecondActivity() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {

            val userId = firebaseUser.uid
            val email = firebaseUser.email ?: ""


            val user = User(userId = userId, email = email)


            val intent = Intent(this, FirstActivity::class.java).apply {
                putExtra("USER_DATA", user)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "You are not logged in. Please log in to continue", Toast.LENGTH_LONG).show()
        }
    }

    private fun signIn() {
        val email = emailView.text.toString()
        val password = passwordView.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_LONG).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Successfully signed in", Toast.LENGTH_SHORT).show()
                    goToSecondActivity()
                } else {
                    Toast.makeText(this, "Sign in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signUp() {
        val email = emailView.text.toString()
        val password = passwordView.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_LONG).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // User created, adding information in firestore
                    val firebaseUser = auth.currentUser
                    val user = hashMapOf(
                        "userId" to firebaseUser?.uid,
                        "email" to firebaseUser?.email
                    )

                    firebaseUser?.uid?.let {
                        db.collection("users").document(it).set(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Account created and user info saved successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to save user info: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }

                    goToSecondActivity()
                } else {
                    Toast.makeText(this, "Account creation failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
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
            // Hantera undantag
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    // Kolla om användaren är ny eller redan finns
                    if (task.result?.additionalUserInfo?.isNewUser == true) {

                        saveUserDataToFirestore(firebaseUser)
                    }
                    goToSecondActivity()
                }
            } else {
                // Hantera misslyckad autentisering
            }
        }
    }

    private fun saveUserDataToFirestore(firebaseUser: FirebaseUser) {
        val user = hashMapOf(
            "userId" to firebaseUser.uid,
            "email" to firebaseUser.email
            // Du kan lägga till fler användaruppgifter här om det behövs
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