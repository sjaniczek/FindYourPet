package com.edu.wszib.findyourpet.ui.foundfragments

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
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentCreateFoundBinding

import com.edu.wszib.findyourpet.inputmasks.DateInputMask
import com.edu.wszib.findyourpet.models.FoundPetData
import com.edu.wszib.findyourpet.models.FoundPetViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.storageMetadata
import java.text.SimpleDateFormat
import java.util.*

class FoundCreateFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var _binding: FragmentCreateFoundBinding? = null
    private val binding get() = _binding!!
    private var imageUri: Uri? = null
    private val foundPetViewModel: FoundPetViewModel by activityViewModels()

    companion object {
        private const val TAG = "FoundCreateFragment"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d(TAG, "Permission result: $isGranted")
            if (isGranted) launchImagePicker()
        }

    private val getImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                imageUri = result.data?.data
                Log.d(TAG, "Selected imageUri: $imageUri")
                binding.ivFoundPet.setImageURI(imageUri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateFoundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        database = Firebase.database("https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/")
        storage = Firebase.storage
        DateInputMask(binding.etFoundPetDate).listen()
        restoreDataFromViewModel()

        binding.buttonChooseFoundPic.setOnClickListener { checkPermissionAndPickImage() }

        binding.buttonGoToMap.setOnClickListener {
            saveFormDataToViewModel()
            findNavController().navigate(
                FoundCreateFragmentDirections.actionFoundCreateFragmentToFoundMapsFragment(
                    false, LatLng(52.06, 19.25), "foundpetkey"
                )
            )
        }

        binding.buttonFoundAccept.setOnClickListener {
            uploadImageAndForm()
        }
    }

    private fun restoreDataFromViewModel() {
        foundPetViewModel.foundPetData?.let { data ->
            binding.rgFoundType.findViewWithTag<RadioButton>(data.foundPetType)?.isChecked = true
            binding.rgFoundBehavior.findViewWithTag<RadioButton>(data.foundPetBehavior)?.isChecked = true
            binding.etFoundAddress.setText(data.foundPetDecodedAddress)
            binding.etFoundPetDate.setText(data.foundPetDate)
            binding.etFoundPetAdditionalInfo.setText(data.foundPetAdditionalPetInfo)
            binding.etFoundFinderName.setText(data.foundPetFinderName)
            binding.etFoundFinderNumber.setText(data.foundPetPhoneNumber)
            binding.etFoundFinderEmail.setText(data.foundPetEmailAddress)
            binding.etFoundFinderAdditionalInfo.setText(data.foundPetAdditionalFinderInfo)
            imageUri = foundPetViewModel.imageUri
            if (imageUri != null) binding.ivFoundPet.setImageURI(imageUri)
        }
    }

    private fun saveFormDataToViewModel() {
        val data = FoundPetData(
            foundPetDate = binding.etFoundPetDate.text.toString(),
            foundPetDecodedAddress = binding.etFoundAddress.text.toString(),
            foundPetAdditionalPetInfo = binding.etFoundPetAdditionalInfo.text.toString(),
            foundPetFinderName = binding.etFoundFinderName.text.toString(),
            foundPetPhoneNumber = binding.etFoundFinderNumber.text.toString(),
            foundPetEmailAddress = binding.etFoundFinderEmail.text.toString(),
            foundPetAdditionalFinderInfo = binding.etFoundFinderAdditionalInfo.text.toString(),
            foundPetType = binding.rgFoundType.findViewById<RadioButton>(binding.rgFoundType.checkedRadioButtonId)?.text.toString(),
            foundPetBehavior = binding.rgFoundBehavior.findViewById<RadioButton>(binding.rgFoundBehavior.checkedRadioButtonId)?.text.toString(),
            foundPetLocation = foundPetViewModel.foundPetData?.foundPetLocation
        )
        foundPetViewModel.saveFormData(data, imageUri)
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

    private fun validateFieldsAndImage(): Boolean {
        return !(binding.etFoundAddress.text.isNullOrEmpty() ||
                binding.etFoundPetDate.text.isNullOrEmpty() ||
                binding.etFoundFinderName.text.isNullOrEmpty() ||
                binding.etFoundFinderEmail.text.isNullOrEmpty() ||
                binding.etFoundFinderNumber.text.isNullOrEmpty() ||
                binding.rgFoundType.checkedRadioButtonId == -1 ||
                binding.rgFoundBehavior.checkedRadioButtonId == -1 ||
                imageUri == null)
    }

    private var isUploading = false
    private fun uploadImageAndForm() {
        if (isUploading) return

        saveFormDataToViewModel()

        if (!validateFieldsAndImage()) {
            Toast.makeText(context, "Wypełnij wszystkie pola i dodaj zdjęcie", Toast.LENGTH_SHORT).show()
            return
        }

        isUploading = true
        binding.buttonFoundAccept.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(context, "Brak zalogowanego użytkownika", Toast.LENGTH_SHORT).show()
            resetUploadUI()
            return
        }

        val databaseRef = database.reference
        val foundPetKey = databaseRef.child("found_pets").push().key ?: run {
            Log.w(TAG, "Nie udało się otrzymać klucza foundPetKey")
            resetUploadUI()
            return
        }

        val mimeType = context?.contentResolver?.getType(imageUri!!) ?: "image/jpeg"
        val fileSize = context?.contentResolver?.openFileDescriptor(imageUri!!, "r")?.use { it.statSize }
        if (fileSize != null && fileSize > 10 * 1024 * 1024) {
            Toast.makeText(context, "Plik jest za duży (max 10 MB)", Toast.LENGTH_SHORT).show()
            resetUploadUI()
            return
        }

        val metadata = storageMetadata {
            setContentType(mimeType)
            setCustomMetadata("owner", userId)
            setCustomMetadata("postId", foundPetKey)
        }

        val fileRef = storage.reference.child("images/${UUID.randomUUID()}")
        fileRef.putFile(imageUri!!, metadata).addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                val formData = foundPetViewModel.foundPetData!!
                val foundPetData = formData.copy(
                    foundPetId = foundPetKey,
                    foundPetOwnerId = userId,
                    foundPetImageUrl = imageUrl,
                    foundPetDateAdded = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
                val foundPetValues = foundPetData.toMap()
                val updates = hashMapOf<String, Any>(
                    "/found_pets/$foundPetKey" to foundPetValues,
                    "/users/$userId/found_pets/$foundPetKey" to foundPetValues
                )
                database.reference.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(context, "Ogłoszenie dodane", Toast.LENGTH_SHORT).show()
                    clearData()
                    findNavController().navigate(
                        FoundCreateFragmentDirections.actionFoundCreateFragmentToMainFragment()
                    )
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Error uploading form: ${e.message}", e)
                    Toast.makeText(context, "Błąd podczas dodawania postu", Toast.LENGTH_SHORT).show()
                    resetUploadUI()
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error getting download URL: ${e.message}", e)
                Toast.makeText(context, "Błąd podczas dodawania zdjęcia", Toast.LENGTH_SHORT).show()
                resetUploadUI()
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Upload failed: ${e.message}", e)
            Toast.makeText(context, "Błąd podczas uploadu obrazu", Toast.LENGTH_SHORT).show()
            resetUploadUI()
        }
    }

    private fun resetUploadUI() {
        isUploading = false
        binding.buttonFoundAccept.isEnabled = true
        binding.progressBar.visibility = View.GONE
    }

    private fun clearData() {
        foundPetViewModel.foundPetData = null
        foundPetViewModel.imageUri = null
        imageUri = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
