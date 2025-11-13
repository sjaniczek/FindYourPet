package com.edu.wszib.findyourpet.foundfragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentFoundEditBinding
import com.edu.wszib.findyourpet.inputmasks.DateInputMask
import com.edu.wszib.findyourpet.models.FoundPetData
import com.edu.wszib.findyourpet.models.FoundPetViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import java.util.*

class FoundEditFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseRef: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private var _binding: FragmentFoundEditBinding? = null
    private val binding get() = _binding!!
    private var imageUrl: String? = null
    private var imageUri: Uri? = null
    private lateinit var currentLocation: LatLng
    private lateinit var dateAdded: String
    private lateinit var foundPetKey: String
    private val foundPetViewModel: FoundPetViewModel by activityViewModels()
    private var isUploading = false

    companion object {
        private const val TAG = "FoundEditFragment"
        const val FOUND_EDIT_POST_KEY = "post_key"
        private const val databaseUrl =
            "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) launchImagePicker()
        }

    private val getImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                imageUri = result.data?.data
                imageUrl = imageUri.toString()
                binding.ivFoundEditPet.setImageURI(imageUri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoundEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DateInputMask(binding.etFoundEditPetDate).listen()

        foundPetKey = requireArguments().getString(FOUND_EDIT_POST_KEY)
            ?: throw IllegalArgumentException("Must pass EXTRA_POST_KEY")
        auth = Firebase.auth
        database = Firebase.database(databaseUrl)
        databaseRef = database.reference.child("found_pets").child(foundPetKey)
        storage = Firebase.storage

        // Przywrócenie danych z ViewModel lub z Firebase
        if (foundPetViewModel.foundPetData != null) {
            restoreDataFromViewModel()
        } else {
            fetchDataAndUpdateUI()
        }

        binding.buttonChooseFoundEditPic.setOnClickListener { checkPermissionAndPickImage() }

        binding.buttonGoToMap.setOnClickListener {
            saveFormDataToViewModel()
            findNavController().navigate(
                FoundEditFragmentDirections.actionFoundEditFragmentToFoundMapsFragment(
                    true,
                    currentLocation,
                    foundPetKey
                )
            )
        }

        binding.buttonFoundEditAccept.setOnClickListener { updateImageAndForm() }
    }

    private fun restoreDataFromViewModel() {
        foundPetViewModel.foundPetData?.let { data ->
            binding.rgFoundEditType.findViewWithTag<RadioButton>(data.foundPetType)?.isChecked = true
            binding.rgFoundEditBehavior.findViewWithTag<RadioButton>(data.foundPetBehavior)?.isChecked = true
            binding.etFoundEditAddress.setText(data.foundPetDecodedAddress)
            binding.etFoundEditPetDate.setText(data.foundPetDate)
            binding.etFoundEditPetAdditionalInfo.setText(data.foundPetAdditionalPetInfo)
            binding.etFoundEditFinderName.setText(data.foundPetFinderName)
            binding.etFoundEditFinderNumber.setText(data.foundPetPhoneNumber)
            binding.etFoundEditFinderEmail.setText(data.foundPetEmailAddress)
            binding.etFoundEditFinderAdditionalInfo.setText(data.foundPetAdditionalFinderInfo)
            imageUri = foundPetViewModel.imageUri
            imageUrl = foundPetViewModel.foundPetData?.foundPetImageUrl
            if (imageUri != null) binding.ivFoundEditPet.setImageURI(imageUri)
            currentLocation = data.foundPetLocation?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(0.0, 0.0)
            dateAdded = data.foundPetDateAdded.toString()
        }
    }

    private fun fetchDataAndUpdateUI() {
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue<FoundPetData>()
                data?.let {
                    binding.rgFoundEditType.findViewWithTag<RadioButton>(it.foundPetType)?.isChecked = true
                    binding.rgFoundEditBehavior.findViewWithTag<RadioButton>(it.foundPetBehavior)?.isChecked = true
                    binding.etFoundEditAddress.setText(it.foundPetDecodedAddress)
                    binding.etFoundEditPetDate.setText(it.foundPetDate)
                    binding.etFoundEditPetAdditionalInfo.setText(it.foundPetAdditionalPetInfo)
                    binding.etFoundEditFinderName.setText(it.foundPetFinderName)
                    binding.etFoundEditFinderNumber.setText(it.foundPetPhoneNumber)
                    binding.etFoundEditFinderEmail.setText(it.foundPetEmailAddress)
                    binding.etFoundEditFinderAdditionalInfo.setText(it.foundPetAdditionalFinderInfo)
                    imageUrl = it.foundPetImageUrl
                    currentLocation = it.foundPetLocation?.let { loc -> LatLng(loc.latitude, loc.longitude) } ?: LatLng(0.0, 0.0)
                    dateAdded = it.foundPetDateAdded.toString()
                    Picasso.get().load(imageUrl).into(binding.ivFoundEditPet)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Błąd przy wczytywaniu danych", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "fetchDataAndUpdateUI failed: ${error.message}")
            }
        })
    }

    private fun checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            } else launchImagePicker()
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else launchImagePicker()
        }
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getImageLauncher.launch(intent)
    }

    private fun saveFormDataToViewModel() {
        val data = FoundPetData(
            foundPetType = binding.rgFoundEditType.findViewById<RadioButton>(binding.rgFoundEditType.checkedRadioButtonId)?.text.toString(),
            foundPetBehavior = binding.rgFoundEditBehavior.findViewById<RadioButton>(binding.rgFoundEditBehavior.checkedRadioButtonId)?.text.toString(),
            foundPetDecodedAddress = binding.etFoundEditAddress.text.toString(),
            foundPetDate = binding.etFoundEditPetDate.text.toString(),
            foundPetAdditionalPetInfo = binding.etFoundEditPetAdditionalInfo.text.toString(),
            foundPetFinderName = binding.etFoundEditFinderName.text.toString(),
            foundPetPhoneNumber = binding.etFoundEditFinderNumber.text.toString(),
            foundPetEmailAddress = binding.etFoundEditFinderEmail.text.toString(),
            foundPetAdditionalFinderInfo = binding.etFoundEditFinderAdditionalInfo.text.toString(),
            foundPetImageUrl = imageUrl,
            foundPetDateAdded = dateAdded,
            foundPetLocation = FoundPetData.FoundLocation(currentLocation)
        )
        foundPetViewModel.saveFormData(data, imageUri)
    }

    private fun validateFields(): Boolean {
        return !(binding.etFoundEditAddress.text.isNullOrEmpty() ||
                binding.etFoundEditPetDate.text.isNullOrEmpty() ||
                binding.etFoundEditFinderName.text.isNullOrEmpty() ||
                binding.etFoundEditFinderEmail.text.isNullOrEmpty() ||
                binding.etFoundEditFinderNumber.text.isNullOrEmpty() ||
                binding.rgFoundEditType.checkedRadioButtonId == -1 ||
                binding.rgFoundEditBehavior.checkedRadioButtonId == -1)
    }

    private fun validateFieldsAndImage(uri: Uri?): Boolean {
        return validateFields() && (uri != null || imageUrl != null)
    }

    private fun updateImageAndForm() {
        if (isUploading) return
        isUploading = true
        binding.buttonFoundEditAccept.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(context, "Brak zalogowanego użytkownika", Toast.LENGTH_SHORT).show()
            resetUploadUI()
            return
        }

        val dbRef = database.reference

        if (imageUri != null) {
            if (!validateFieldsAndImage(imageUri)) {
                Toast.makeText(context, "Wypełnij wszystkie pola i dodaj zdjęcie", Toast.LENGTH_SHORT).show()
                resetUploadUI()
                return
            }

            val mimeType = context?.contentResolver?.getType(imageUri!!) ?: "image/jpeg"
            val fileRef = storage.reference.child("images/${UUID.randomUUID()}")
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("owner", userId)
                .setCustomMetadata("postId", foundPetKey)
                .build()

            fileRef.putFile(imageUri!!, metadata).addOnSuccessListener { task ->
                task.storage.downloadUrl.addOnSuccessListener { uri ->
                    val data = createFoundPetData(uri.toString(), foundPetKey)
                    val values = data.toMap()
                    val updates = hashMapOf<String, Any>(
                        "/found_pets/$foundPetKey" to values,
                        "/users/$userId/found_pets/$foundPetKey" to values
                    )
                    dbRef.updateChildren(updates).addOnSuccessListener {
                        Toast.makeText(context, "Ogłoszenie edytowane", Toast.LENGTH_SHORT).show()
                        clearData()
                        resetUploadUI()
                        findNavController().navigate(
                            FoundEditFragmentDirections.actionFoundEditFragmentToMainFragment()
                        )
                    }.addOnFailureListener { e ->
                        Toast.makeText(context, "Błąd podczas aktualizacji", Toast.LENGTH_SHORT).show()
                        resetUploadUI()
                        Log.e(TAG, "Update failed: ${e.message}", e)
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Błąd podczas uploadu zdjęcia", Toast.LENGTH_SHORT).show()
                resetUploadUI()
                Log.e(TAG, "Image upload failed: ${e.message}", e)
            }

        } else {
            // brak zmiany zdjęcia
            if (!validateFields()) {
                Toast.makeText(context, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                resetUploadUI()
                return
            }
            val data = createFoundPetData(imageUrl, foundPetKey)
            val values = data.toMap()
            val updates = hashMapOf<String, Any>(
                "/found_pets/$foundPetKey" to values,
                "/users/$userId/found_pets/$foundPetKey" to values
            )
            dbRef.updateChildren(updates).addOnSuccessListener {
                Toast.makeText(context, "Ogłoszenie edytowane", Toast.LENGTH_SHORT).show()
                clearData()
                resetUploadUI()
                findNavController().navigate(
                    FoundEditFragmentDirections.actionFoundEditFragmentToMainFragment()
                )
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Błąd podczas aktualizacji", Toast.LENGTH_SHORT).show()
                resetUploadUI()
                Log.e(TAG, "Update failed: ${e.message}", e)
            }
        }
    }

    private fun createFoundPetData(imageUrl: String?, key: String?): FoundPetData {
        return FoundPetData(
            foundPetOwnerId = auth.currentUser?.uid,
            foundPetId = key,
            foundPetType = binding.rgFoundEditType.findViewById<RadioButton>(binding.rgFoundEditType.checkedRadioButtonId)?.text.toString(),
            foundPetDate = binding.etFoundEditPetDate.text.toString(),
            foundPetFinderName = binding.etFoundEditFinderName.text.toString(),
            foundPetPhoneNumber = binding.etFoundEditFinderNumber.text.toString(),
            foundPetEmailAddress = binding.etFoundEditFinderEmail.text.toString(),
            foundPetDecodedAddress = binding.etFoundEditAddress.text.toString(),
            foundPetBehavior = binding.rgFoundEditBehavior.findViewById<RadioButton>(binding.rgFoundEditBehavior.checkedRadioButtonId)?.text.toString(),
            foundPetAdditionalPetInfo = binding.etFoundEditPetAdditionalInfo.text.toString(),
            foundPetAdditionalFinderInfo = binding.etFoundEditFinderAdditionalInfo.text.toString(),
            foundPetDateAdded = dateAdded,
            foundPetImageUrl = imageUrl,
            foundPetLocation = foundPetViewModel.foundPetData?.foundPetLocation ?: FoundPetData.FoundLocation(currentLocation)
        )
    }

    private fun clearData() {
        foundPetViewModel.foundPetData = null
        foundPetViewModel.imageUri = null
    }

    private fun resetUploadUI() {
        isUploading = false
        binding.buttonFoundEditAccept.isEnabled = true
        binding.progressBar.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
