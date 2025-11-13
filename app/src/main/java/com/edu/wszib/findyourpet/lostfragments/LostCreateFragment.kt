package com.edu.wszib.findyourpet.lostfragments

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
import com.edu.wszib.findyourpet.databinding.FragmentCreateLostBinding
import com.edu.wszib.findyourpet.inputmasks.DateInputMask
import com.edu.wszib.findyourpet.inputmasks.TimeInputMask
import com.edu.wszib.findyourpet.models.LostPetData
import com.edu.wszib.findyourpet.models.LostPetViewModel
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

class LostCreateFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var _binding: FragmentCreateLostBinding? = null
    private val binding get() = _binding!!
    private var imageUri: Uri? = null
    private val lostPetViewModel: LostPetViewModel by activityViewModels()

    companion object {
        private const val TAG = "LostCreateFragment"
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
                binding.ivLostPet.setImageURI(imageUri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateLostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        database = Firebase.database("https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/")
        storage = Firebase.storage

        DateInputMask(binding.etLostPetDate).listen()
        TimeInputMask(binding.etLostPetHour).listen()

        restoreDataFromViewModel()

        binding.buttonChooseLostPic.setOnClickListener { checkPermissionAndPickImage() }

        binding.buttonGoToMap.setOnClickListener {
            saveFormDataToViewModel()
            findNavController().navigate(
                LostCreateFragmentDirections.actionLostCreateFragmentToLostMapsFragment(
                    false,
                    LatLng(52.06, 19.25),
                    "lostpetkey"
                )
            )
        }

        binding.buttonLostAccept.setOnClickListener { uploadImageAndForm() }
    }

    private fun restoreDataFromViewModel() {
        lostPetViewModel.lostPetData?.let { data ->
            with(binding) {
                rgLostType.findViewWithTag<RadioButton>(data.lostPetType)?.isChecked = true
                rgLostBehavior.findViewWithTag<RadioButton>(data.lostPetBehavior)?.isChecked = true
                rgLostReacts.findViewWithTag<RadioButton>(data.lostPetReact)?.isChecked = true
                etLostPetName.setText(data.lostPetName)
                etLostAddress.setText(data.lostPetDecodedAddress)
                etLostPetDate.setText(data.lostPetDate)
                etLostPetHour.setText(data.lostPetHour)
                etLostPetAdditionalInfo.setText(data.lostPetAdditionalPetInfo)
                etLostOwnerName.setText(data.lostPetOwnerName)
                etLostOwnerNumber.setText(data.lostPetPhoneNumber)
                etLostOwnerEmail.setText(data.lostPetEmailAddress)
                etLostOwnerAdditionalInfo.setText(data.lostPetAdditionalOwnerInfo)
                imageUri = lostPetViewModel.imageUri
                if (imageUri != null) ivLostPet.setImageURI(imageUri)
            }
        }
    }

    private fun saveFormDataToViewModel() {
        val data = LostPetData(
            lostPetName = binding.etLostPetName.text.toString(),
            lostPetDate = binding.etLostPetDate.text.toString(),
            lostPetHour = binding.etLostPetHour.text.toString(),
            lostPetDecodedAddress = binding.etLostAddress.text.toString(),
            lostPetAdditionalPetInfo = binding.etLostPetAdditionalInfo.text.toString(),
            lostPetOwnerName = binding.etLostOwnerName.text.toString(),
            lostPetPhoneNumber = binding.etLostOwnerNumber.text.toString(),
            lostPetEmailAddress = binding.etLostOwnerEmail.text.toString(),
            lostPetAdditionalOwnerInfo = binding.etLostOwnerAdditionalInfo.text.toString(),
            lostPetType = binding.rgLostType.findViewById<RadioButton>(binding.rgLostType.checkedRadioButtonId)?.text.toString(),
            lostPetReact = binding.rgLostReacts.findViewById<RadioButton>(binding.rgLostReacts.checkedRadioButtonId)?.text.toString(),
            lostPetBehavior = binding.rgLostBehavior.findViewById<RadioButton>(binding.rgLostBehavior.checkedRadioButtonId)?.text.toString(),
            lostPetLocation = lostPetViewModel.lostPetData?.lostPetLocation
        )
        lostPetViewModel.saveFormData(data, imageUri)
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
        return !(binding.etLostPetName.text.isNullOrEmpty() ||
                binding.etLostAddress.text.isNullOrEmpty() ||
                binding.etLostPetDate.text.isNullOrEmpty() ||
                binding.etLostPetHour.text.isNullOrEmpty() ||
                binding.etLostOwnerName.text.isNullOrEmpty() ||
                binding.etLostOwnerEmail.text.isNullOrEmpty() ||
                binding.etLostOwnerNumber.text.isNullOrEmpty() ||
                binding.rgLostType.checkedRadioButtonId == -1 ||
                binding.rgLostBehavior.checkedRadioButtonId == -1 ||
                binding.rgLostReacts.checkedRadioButtonId == -1 ||
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
        binding.buttonLostAccept.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(context, "Brak zalogowanego użytkownika", Toast.LENGTH_SHORT).show()
            resetUploadUI()
            return
        }

        val databaseRef = database.reference
        val lostPetKey = databaseRef.child("lost_pets").push().key ?: run {
            Log.w(TAG, "Nie udało się otrzymać klucza lostPetKey")
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
            setCustomMetadata("postId", lostPetKey)
        }

        val fileRef = storage.reference.child("images/${UUID.randomUUID()}")
        fileRef.putFile(imageUri!!, metadata).addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                val formData = lostPetViewModel.lostPetData!!
                val lostPetData = formData.copy(
                    lostPetId = lostPetKey,
                    lostPetOwnerId = userId,
                    lostPetImageUrl = imageUrl,
                    lostPetDateAdded = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
                val lostPetValues = lostPetData.toMap()
                val updates = hashMapOf<String, Any>(
                    "/lost_pets/$lostPetKey" to lostPetValues,
                    "/users/$userId/lost_pets/$lostPetKey" to lostPetValues
                )
                database.reference.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(context, "Ogłoszenie dodane", Toast.LENGTH_SHORT).show()
                    clearData()
                    findNavController().navigate(
                        LostCreateFragmentDirections.actionLostCreateFragmentToMainFragment()
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
        binding.buttonLostAccept.isEnabled = true
        binding.progressBar.visibility = View.GONE
    }

    private fun clearData() {
        lostPetViewModel.lostPetData = null
        lostPetViewModel.imageUri = null
        imageUri = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
