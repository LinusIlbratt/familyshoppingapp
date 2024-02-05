package com.example.familyshoppingapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.Date
import java.util.Locale
import java.util.UUID

class ProductListFragment : Fragment(), OnCameraIconClickListener {

    private val database = Firebase.firestore
    private val productsRef = database.collection("products")
    private val shoppingItemList = mutableListOf<ShoppingItem>()
    private var snapshotListener: ListenerRegistration? = null
    private var currentShoppingItem: ShoppingItem? = null
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var adapter: ProductAdapter
    private lateinit var listId: String
    private lateinit var startCameraLauncher: ActivityResultLauncher<Intent>
    private val CAMERA_REQUEST_CODE = 100
    private val GALLERY_IMAGE_REQUEST_CODE = 101
    private val storage = FirebaseStorage.getInstance()
    private val storageReference = storage.reference
    private val productImageUris = mutableMapOf<String, Uri>()
    private lateinit var productAdapter: ProductAdapter
    private val imageUpdateLiveData = MutableLiveData<String>()
    private var currentDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                currentShoppingItem?.let { item ->
                    productImageUris[item.documentId]?.let { uri ->
                        uploadImageToFirestore(uri, item)
                        currentDialog?.dismiss()  // Stänger dialogrutan
                    }
                }
            }
        }

        requestMicrophonePermission()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MenuActivity) {
            context.productAdapterInterface = object : MenuActivity.ProductAdapterInterface {
                override fun updateProductImage(documentId: String, imageUrl: String) {
                    productAdapter.updateProductImage(documentId, imageUrl)
                }
            }
        }
    }


    override fun onDetach() {
        super.onDetach()
        if (activity is MenuActivity) {
            (activity as MenuActivity).productAdapterInterface = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_item_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listId = arguments?.getString("LIST_ID") ?: "defaultListId"
        val listTitle = arguments?.getString("LIST_TITLE") ?: "Default Title"

        val titleTextView = view.findViewById<TextView>(R.id.shoppingListTitelText)
        titleTextView.text = listTitle

        adapter = ProductAdapter(
            productsRef,
            shoppingItemList,
            { documentId ->
                val item = shoppingItemList.find { it.documentId == documentId }
                removeItemsFromDatabase(documentId, item?.imageUrl)
            },
            this, // OnCameraIconClickListener
            object : OnGalleryIconClickListener {
                override fun onGalleryIconClick(item: ShoppingItem) {
                    openDeviceGallery(item)
                }
            }
        )

        val backArrow = view.findViewById<ImageView>(R.id.backArrow)
        backArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val resetButton = view.findViewById<Button>(R.id.resetAllButton)
        resetButton.setOnClickListener {
            adapter.resetAllProducts()
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val floatingButton: FloatingActionButton = view.findViewById(R.id.addItem)
        floatingButton.setOnClickListener {

            addNewItemPopUpWindow()
        }

        setupSnapshotListener()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {

                }

                override fun onBeginningOfSpeech() {

                }

                override fun onRmsChanged(rmsdB: Float) {

                }

                override fun onBufferReceived(buffer: ByteArray?) {

                }

                override fun onEndOfSpeech() {

                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        // ... andra fall
                        else -> "Unknown speech recognizer error"
                    }
                    Log.e("SpeechRecognizer", errorMessage)
                }

                override fun onResults(results: Bundle) {
                    val spokenText =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)
                            ?: ""
                    if (spokenText.isNotBlank()) {
                        val newItem = ShoppingItem(name = spokenText, listId = listId)
                        addItemsToDatabase(newItem)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {

                }

                override fun onEvent(eventType: Int, params: Bundle?) {

                }
                // Implementera andra metoder i RecognitionListener
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { imageUri ->
                currentShoppingItem?.let { item ->
                    uploadImageToFirestore(imageUri, item)
                    currentShoppingItem = null // Nollställ den efter användning
                    currentDialog?.dismiss()  // Stänger dialogrutan
                }
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_MICROPHONE_PERMISSION_CODE -> {
                // Hantera mikrofonbehörighetsresultat
            }

            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Kamerabehörighet beviljad, starta kameran
                } else {
                    // Kamerabehörighet nekad, hantera det
                }
            }
        }
    }


    private fun setupSnapshotListener() {
        snapshotListener = productsRef.whereEqualTo("listId", listId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.d("!!!", "Line 70, Second Activity. Error loading items:")
                    return@addSnapshotListener
                }
                shoppingItemList.clear()
                snapshot?.forEach { document ->
                    val item = document.toObject<ShoppingItem>().copy(documentId = document.id)
                    if (item.listId == listId) {
                        shoppingItemList.add(item)
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }

    override fun onDestroy() {
        snapshotListener?.remove()

        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }

        super.onDestroy()
    }

    private fun openDeviceGallery(item: ShoppingItem) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_IMAGE_REQUEST_CODE)
        currentShoppingItem = item
    }

    private fun addItemsToDatabase(shoppingItem: ShoppingItem) {
        val newItem = shoppingItem.copy(listId = listId)
        productsRef.add(newItem)
            .addOnSuccessListener { documentReference ->

            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error adding item", Toast.LENGTH_LONG).show()
            }
    }


    private fun addNewItemPopUpWindow() {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_add_shopping_item, null)
        val editItemName = dialogLayout.findViewById<EditText>(R.id.addItemName)
        val addItemVoice = dialogLayout.findViewById<ImageView>(R.id.voiceIconImage)

        addItemVoice.setOnClickListener {
            startVoiceRecognition()
        }

        builder.setView(dialogLayout)
            .setPositiveButton("Add") { dialog, which ->
                val itemName = editItemName.text.toString()

                if (itemName.isBlank()) {
                    Toast.makeText(
                        requireContext(),
                        "Item name cannot be empty",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val newItem = ShoppingItem(name = itemName, listId = listId)
                    addItemsToDatabase(newItem)
                }
            }
            .setNegativeButton("Close") { dialog, which ->
                // Inget behov av att skriva någon kod här, dialogrutan stängs automatiskt
            }
        builder.show()
    }

    private fun removeItemsFromDatabase(documentId: String, imageUrl: String?) {
        Log.d("!!!", "Försöker ta bort dokument och bild: $documentId")

        // Remove document from firestore
        productsRef.document(documentId).delete()
            .addOnSuccessListener {
                Log.d("!!!", "Dokument borttaget: $documentId")

                // if there is an image, remove it
                imageUrl?.let { url ->
                    deleteImageFromFirebase(url)
                }

                // remove the item from the list
                shoppingItemList.removeAll { it.documentId == documentId }
                // Uppdatera RecyclerView
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("!!!", "Error removing document: $documentId", e)
            }
    }

    private fun startVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speechRecognizer.startListening(intent)
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_MICROPHONE_PERMISSION_CODE
            )
        }
    }

    private fun requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_MICROPHONE_PERMISSION_CODE
            )
        }
    }

    override fun onCameraIconClick(item: ShoppingItem) {
        Log.d("!!!", "Selected item ID: ${item.documentId}")
        currentShoppingItem = item
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            startCamera(item)
        }
    }

    private fun startCamera(item: ShoppingItem) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

        val imageUri = FileProvider.getUriForFile(
            requireContext(),
            "com.example.familyshoppingapp.fileprovider",
            imageFile
        )

        // Spara URI i Map endast om item.documentId inte är null
        item.documentId?.let { id ->
            productImageUris[id] = imageUri
            Log.d("!!!", "Saved URI for item ID $id: $imageUri")
        }

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        }
        startCameraLauncher.launch(cameraIntent)
    }


    private fun uploadImageToFirestore(imageUri: Uri, item: ShoppingItem) {
        item.oldImageUrl = item.imageUrl
        val filename = UUID.randomUUID().toString()
        val ref = storageReference.child("images/$filename")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    imageUpdateLiveData.postValue(imageUrl)
                    item.oldImageUrl?.let { oldImageUrl ->
                        deleteImageFromFirebase(oldImageUrl)
                    }
                    item.imageUrl = imageUrl
                    updateItemInDatabase(item.documentId, item)

                    // Uppdatera produktbilden i ProductListFragment
                    (activity as? MenuActivity)?.let { activity ->
                        val currentFragment = activity.supportFragmentManager.findFragmentById(R.id.list_fragment_container)
                        if (currentFragment is ProductListFragment) {
                            item.documentId?.let { it1 -> currentFragment.updateProductImage(it1, imageUrl) }
                        }
                    }
                }
            }
            .addOnFailureListener {
                // Hantera misslyckad uppladdning
            }
    }

    private fun updateItemInDatabase(documentId: String?, shoppingItem: ShoppingItem) {
        documentId?.let {
            productsRef.document(it).set(shoppingItem)
                .addOnSuccessListener {
                    Log.d("Firestore", "Document successfully updated with new image URL")
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error updating document", e)
                }
        }
    }

    private fun deleteImageFromFirebase(imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageRef.delete().addOnSuccessListener {
                Log.d("!!!", "Bild borttagen: $imageUrl")
            }.addOnFailureListener {
                Log.w("!!!", "Attempted to delete image with null or empty URL.")
            }
        }
    }

    fun updateProductImage(documentId: String, imageUrl: String) {
        // Se till att adaptern är initialiserad
        if (::productAdapter.isInitialized) {
            productAdapter.updateProductImage(documentId, imageUrl)
        }
    }

    companion object {
        private const val REQUEST_MICROPHONE_PERMISSION_CODE = 1
        private const val REQUEST_IMAGE_CAPTURE = 2
        fun newInstance(listId: String, listTitle: String): ProductListFragment {
            val fragment = ProductListFragment()
            val args = Bundle().apply {
                putString("LIST_ID", listId)
                putString("LIST_TITLE", listTitle)
            }
            fragment.arguments = args
            return fragment
        }
    }

}