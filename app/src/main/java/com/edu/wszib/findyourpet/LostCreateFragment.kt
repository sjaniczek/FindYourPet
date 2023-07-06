package com.edu.wszib.findyourpet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentCreateLostBinding
import com.edu.wszib.findyourpet.models.LostPetData
import com.edu.wszib.findyourpet.models.LostPetViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.util.*

class LostCreateFragment : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var _binding: FragmentCreateLostBinding? = null
    private val binding: FragmentCreateLostBinding
        get() = _binding!!
    private var imageUrl: String? = null
    private var imageUri: Uri? = null
    private val lostPetViewModel: LostPetViewModel by activityViewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.i("permission", "Permission granted")
                launchImagePicker()
            } else {
                Log.i("permission", "Permission denied")
                // Handle permission denied case
            }
        }
    private val getImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                imageUri = result.data?.data
                imageUrl = imageUri.toString()
                binding.ivLostPet.setImageURI(imageUri)
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateLostBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonChooseLostPic.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For API 33 and above
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    Log.i("permission", "Requesting permission")
                } else {
                    Log.i("permission", "Permission already granted")
                    launchImagePicker()
                }
            } else {
                // For API below 33
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    Log.i("permission", "Requesting permission")
                } else {
                    Log.i("permission", "Permission already granted")
                    launchImagePicker()
                }
            }
        }
        binding.buttonGoToMap.setOnClickListener {
            saveFormData()
            findNavController().navigate(LostCreateFragmentDirections.actionLostCreateFragmentToLostMapsFragment())
        }
        binding.buttonLostAccept.setOnClickListener {
            uploadImageAndForm()
        }
        val lostPetData = lostPetViewModel.lostPetData
        if (lostPetData != null) {
            binding.rgLostType.findViewWithTag<RadioButton>(lostPetData.lostPetType)?.isChecked = true
            binding.etLostPetName.setText(lostPetData.lostPetName)
            binding.etLostAddress.setText(lostPetData.lostPetDecodedAddress)
        }
    }
    private fun uploadImageAndForm() {
        storage = Firebase.storage
        val databaseUrl = "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        database = Firebase.database(databaseUrl)
        val databaseRef = database.getReference("test")
        val storageRef = storage.reference
        val fileName = UUID.randomUUID().toString()
        val petId = databaseRef.push().key!!
        val fileRef = storageRef.child("images/$fileName")
        val imageUri = imageUri
//            Log.d("TAG", "Image URI is null")
//            return
//        }
        if (binding.etLostPetName.text.isNullOrEmpty() ||
            binding.etLostAddress.text.isNullOrEmpty() ||
            binding.etLostAge.text.isNullOrEmpty() ||
            binding.etLostPetDate.text.isNullOrEmpty() ||
            binding.etLostOwnerName.text.isNullOrEmpty() ||
            binding.etLostOwnerEmail.text.isNullOrEmpty() ||
            binding.etLostOwnerNumber.text.isNullOrEmpty() ||
            binding.rgLostType.checkedRadioButtonId == -1 ||
            binding.rgLostBehavior.checkedRadioButtonId == -1 ||
            binding.rgLostReacts.checkedRadioButtonId == -1 ||
            imageUri == null

        ) {
            Toast.makeText(context, "Wypełnij lub zaznacz wszystkie pola oraz dodaj zdjęcie", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val petName = binding.etLostPetName.text.toString()
        val petType =
            binding.rgLostType.findViewById<RadioButton>(binding.rgLostType.checkedRadioButtonId).text.toString()
        val petAge = binding.etLostAge.text.toString()
        val lostDate = binding.etLostPetDate.text.toString()
        val ownerName = binding.etLostOwnerName.text.toString()
        val phoneNumber = binding.etLostOwnerNumber.text.toString()
        val emailAddress = binding.etLostOwnerEmail.text.toString()
        val decodedAddress = binding.etLostAddress.text.toString()
        val additionalPetInfo = binding.etLostPetAdditionalInfo.text.toString()
        val additionalOwnerInfo = binding.etLostOwnerAdditionalInfo.text.toString()
        val petReact =
            binding.rgLostReacts.findViewById<RadioButton>(binding.rgLostReacts.checkedRadioButtonId).text.toString()
        val petBehavior =
            binding.rgLostBehavior.findViewById<RadioButton>(binding.rgLostBehavior.checkedRadioButtonId).text.toString()
        val locationData = lostPetViewModel.lostPetData?.lostPetLocation
        val uploadTask = fileRef.putFile(imageUri)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                imageUrl = uri.toString()
                val petAd = LostPetData(
                    petId,
                    petName,
                    petType,
                    petAge,
                    lostDate,
                    ownerName,
                    phoneNumber,
                    emailAddress,
                    decodedAddress,
                    petBehavior,
                    petReact,
                    additionalPetInfo,
                    additionalOwnerInfo,
                    imageUrl
                )

                databaseRef.child(petId).setValue(petAd.toMap())
                    .addOnSuccessListener {
                        // Form uploaded successfully
                        Toast.makeText(context, "Form submitted", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(LostCreateFragmentDirections.actionLostCreateFragmentToMainFragment())
                    }
                    .addOnFailureListener { e ->
                        // Form upload failed
                        Log.e(TAG, "Error uploading form: ${e.message}", e)
                        Toast.makeText(context, "Error submitting form", Toast.LENGTH_SHORT).show()
                    }
            }
        }
            .addOnFailureListener { e ->
                // Image upload failed
                Log.e(TAG, "Error uploading image: ${e.message}", e)
                Toast.makeText(context, "Error uploading image", Toast.LENGTH_SHORT).show()
            }
    }
    private fun saveFormData() {
        val lostPetData = LostPetData(
            lostPetName = binding.etLostPetName.text.toString(),
            lostPetType = binding.rgLostType.findViewById<RadioButton>(binding.rgLostType.checkedRadioButtonId)?.text.toString(),
            lostPetReact = binding.rgLostReacts.findViewById<RadioButton>(binding.rgLostReacts.checkedRadioButtonId)?.text.toString(),
            lostPetBehavior = binding.rgLostBehavior.findViewById<RadioButton>(binding.rgLostBehavior.checkedRadioButtonId)?.text.toString(),
            lostPetAge = binding.etLostAge.text.toString(),
            lostPetDecodedAddress = binding.etLostAddress.text.toString(),
            lostPetAdditionalPetInfo = binding.tvLostPetAdditionalInfo.text.toString()
        )
        lostPetViewModel.saveFormData(lostPetData)
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getImageLauncher.launch(intent)
    }

    companion object {
        private const val TAG = "lostCreateFragment"
    }

}