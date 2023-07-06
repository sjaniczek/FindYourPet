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
import com.edu.wszib.findyourpet.databinding.FragmentCreateFoundBinding
import com.edu.wszib.findyourpet.models.FoundPetData
import com.edu.wszib.findyourpet.models.FoundPetViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.util.*

class FoundCreateFragment : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var _binding: FragmentCreateFoundBinding? = null
    private val binding: FragmentCreateFoundBinding
        get() = _binding!!
    private var imageUrl: String? = null
    private var imageUri: Uri? = null
    private val foundPetViewModel: FoundPetViewModel by activityViewModels()

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
                binding.ivFoundPet.setImageURI(imageUri)
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateFoundBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonChooseFoundPic.setOnClickListener {
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
            findNavController().navigate(FoundCreateFragmentDirections.actionFoundCreateFragmentToFoundMapsFragment())
        }
        binding.buttonFoundAccept.setOnClickListener {
            uploadImageAndForm()
        }
        val foundPetData = foundPetViewModel.foundPetData
        if (foundPetData != null) {
            binding.rgFoundType.findViewWithTag<RadioButton>(foundPetData.foundPetType)?.isChecked = true
            binding.etFoundPetName.setText(foundPetData.foundPetName)
            binding.etFoundAddress.setText(foundPetData.foundPetDecodedAddress)
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
        if (binding.etFoundPetName.text.isNullOrEmpty() ||
            binding.etFoundAddress.text.isNullOrEmpty() ||
            binding.etFoundAge.text.isNullOrEmpty() ||
            binding.etFoundPetDate.text.isNullOrEmpty() ||
            binding.etFoundOwnerName.text.isNullOrEmpty() ||
            binding.etFoundOwnerEmail.text.isNullOrEmpty() ||
            binding.etFoundOwnerNumber.text.isNullOrEmpty() ||
            binding.rgFoundType.checkedRadioButtonId == -1 ||
            binding.rgFoundBehavior.checkedRadioButtonId == -1 ||
            binding.rgFoundReacts.checkedRadioButtonId == -1 ||
            imageUri == null

        ) {
            Toast.makeText(context, "Wypełnij lub zaznacz wszystkie pola oraz dodaj zdjęcie", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val petName = binding.etFoundPetName.text.toString()
        val petType =
            binding.rgFoundType.findViewById<RadioButton>(binding.rgFoundType.checkedRadioButtonId).text.toString()
        val petAge = binding.etFoundAge.text.toString()
        val foundDate = binding.etFoundPetDate.text.toString()
        val ownerName = binding.etFoundOwnerName.text.toString()
        val phoneNumber = binding.etFoundOwnerNumber.text.toString()
        val emailAddress = binding.etFoundOwnerEmail.text.toString()
        val decodedAddress = binding.etFoundAddress.text.toString()
        val additionalPetInfo = binding.etFoundPetAdditionalInfo.text.toString()
        val additionalOwnerInfo = binding.etFoundOwnerAdditionalInfo.text.toString()
        val petReact =
            binding.rgFoundReacts.findViewById<RadioButton>(binding.rgFoundReacts.checkedRadioButtonId).text.toString()
        val petBehavior =
            binding.rgFoundBehavior.findViewById<RadioButton>(binding.rgFoundBehavior.checkedRadioButtonId).text.toString()
        val locationData = foundPetViewModel.foundPetData?.foundPetLocation
        val uploadTask = fileRef.putFile(imageUri)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                imageUrl = uri.toString()
                val petAd = FoundPetData(
                    petId,
                    petName,
                    petType,
                    petAge,
                    foundDate,
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
                        findNavController().navigate(FoundCreateFragmentDirections.actionFoundCreateFragmentToMainFragment())
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
        val foundPetData = FoundPetData(
            foundPetName = binding.etFoundPetName.text.toString(),
            foundPetType = binding.rgFoundType.findViewById<RadioButton>(binding.rgFoundType.checkedRadioButtonId)?.text.toString(),
            foundPetReact = binding.rgFoundReacts.findViewById<RadioButton>(binding.rgFoundReacts.checkedRadioButtonId)?.text.toString(),
            foundPetBehavior = binding.rgFoundBehavior.findViewById<RadioButton>(binding.rgFoundBehavior.checkedRadioButtonId)?.text.toString(),
            foundPetAge = binding.etFoundAge.text.toString(),
            foundPetDecodedAddress = binding.etFoundAddress.text.toString(),
            foundPetAdditionalPetInfo = binding.tvFoundPetAdditionalInfo.text.toString()
        )
        foundPetViewModel.saveFormData(foundPetData)
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getImageLauncher.launch(intent)
    }

    companion object {
        private const val TAG = "foundCreateFragment"
    }

}