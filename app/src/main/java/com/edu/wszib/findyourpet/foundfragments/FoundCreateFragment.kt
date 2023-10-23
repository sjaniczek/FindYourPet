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
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
import java.text.SimpleDateFormat
import java.util.*

class FoundCreateFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
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

        auth = Firebase.auth
        val dateEditText: EditText = binding.etFoundPetDate
        DateInputMask(dateEditText).listen()
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
            findNavController().navigate(
                com.edu.wszib.findyourpet.foundfragments.FoundCreateFragmentDirections.actionFoundCreateFragmentToFoundMapsFragment(
                    false,
                    LatLng(52.06, 19.25),
                    "foundpetkey"
                )
            )
        }
        binding.buttonFoundAccept.setOnClickListener {
            uploadImageAndForm()
        }
        val foundPetData = foundPetViewModel.foundPetData
        if (foundPetData != null && foundPetViewModel.imageUri != null) {
            binding.rgFoundType.findViewWithTag<RadioButton>(foundPetData.foundPetType)?.isChecked =
                true
            binding.rgFoundBehavior.findViewWithTag<RadioButton>(foundPetData.foundPetBehavior)?.isChecked =
                true
            binding.ivFoundPet.setImageURI(foundPetViewModel.imageUri)
            binding.etFoundAddress.setText(foundPetData.foundPetDecodedAddress)
            binding.etFoundPetDate.setText(foundPetData.foundPetDate)
            binding.etFoundPetAdditionalInfo.setText(foundPetData.foundPetAdditionalPetInfo)
            binding.etFoundFinderName.setText(foundPetData.foundPetFinderName)
            binding.etFoundFinderNumber.setText(foundPetData.foundPetPhoneNumber)
            binding.etFoundFinderEmail.setText(foundPetData.foundPetEmailAddress)
            binding.etFoundFinderAdditionalInfo.setText(foundPetData.foundPetAdditionalFinderInfo)
            imageUri = foundPetViewModel.imageUri
        }
    }

    private fun uploadImageAndForm() {
        val databaseUrl =
            "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        database = Firebase.database(databaseUrl)
        storage = Firebase.storage
        val storageRef = storage.reference
        val userId = auth.currentUser?.uid
        val fileName = UUID.randomUUID().toString()
        val databaseRef = database.reference
        val foundPetKey = databaseRef.child("found_pets").push().key
        if (foundPetKey == null) {
            Log.w(TAG, "Couldn't get push key for foundPetKey")
            return
        }
        val fileRef = storageRef.child("images/$fileName")
        val imageUri = imageUri

        if (!validateFieldsAndImage(imageUri)) {
            Toast.makeText(
                context,
                "Wypełnij lub zaznacz wszystkie pola oraz dodaj zdjęcie",
                Toast.LENGTH_SHORT
            )
                .show()
            return
        }
        binding.buttonFoundAccept.isVisible = false
        val uploadTask = fileRef.putFile(imageUri!!)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                val foundPetData = createFoundPetData(imageUrl, foundPetKey)
                val foundPetValues = foundPetData.toMap()
                val foundPetUpdates = hashMapOf<String, Any>(
                    "/found_pets/$foundPetKey" to foundPetValues,
                    "/users/$userId/found_pets/$foundPetKey" to foundPetValues,
                )
                databaseRef.updateChildren(foundPetUpdates).addOnSuccessListener {
                    // Form uploaded successfully
                    Toast.makeText(context, "Ogłoszenie dodane", Toast.LENGTH_SHORT).show()
                    clearData()
                    //findNavController().popBackStack()
                    findNavController().navigate(com.edu.wszib.findyourpet.foundfragments.FoundCreateFragmentDirections.actionFoundCreateFragmentToMainFragment())
                }
                    .addOnFailureListener { e ->
                        // Form upload failed
                        Log.e(TAG, "Error uploading form: ${e.message}", e)
                        Toast.makeText(context, "Error submitting form", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
                .addOnFailureListener { e ->
                    // Image upload failed
                    Log.e(TAG, "Error uploading image: ${e.message}", e)
                    Toast.makeText(context, "Error uploading image", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveFormData() {
        val foundPetData = FoundPetData(
            foundPetDate = binding.etFoundPetDate.text.toString(),
            foundPetDecodedAddress = binding.etFoundAddress.text.toString(),
            foundPetAdditionalPetInfo = binding.etFoundPetAdditionalInfo.text.toString(),
            foundPetFinderName = binding.etFoundFinderName.text.toString(),
            foundPetPhoneNumber = binding.etFoundFinderName.text.toString(),
            foundPetEmailAddress = binding.etFoundFinderName.text.toString(),
            foundPetAdditionalFinderInfo = binding.etFoundFinderName.text.toString(),
            //foundPetImageUri = ((if (imageUri != null) imageUri else foundPetViewModel.foundPetData?.foundPetImageUri)),
            foundPetType = binding.rgFoundType.findViewById<RadioButton>(binding.rgFoundType.checkedRadioButtonId)?.text.toString(),
            foundPetBehavior = binding.rgFoundBehavior.findViewById<RadioButton>(binding.rgFoundBehavior.checkedRadioButtonId)?.text.toString()
        )
        foundPetViewModel.saveFormData(foundPetData, imageUri)
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getImageLauncher.launch(intent)
    }

    private fun validateFieldsAndImage(imageUri: Uri?): Boolean {
        val isAnyFieldEmpty = binding.etFoundAddress.text.isNullOrEmpty() ||
                binding.etFoundPetDate.text.isNullOrEmpty() ||
                binding.etFoundFinderName.text.isNullOrEmpty() ||
                binding.etFoundFinderEmail.text.isNullOrEmpty() ||
                binding.etFoundFinderNumber.text.isNullOrEmpty() ||
                binding.rgFoundType.checkedRadioButtonId == -1 ||
                binding.rgFoundBehavior.checkedRadioButtonId == -1 ||
                imageUri == null

        return !isAnyFieldEmpty
    }

    private fun createFoundPetData(imageUrl: String?, foundPetKey: String?): FoundPetData {

        val loggedUser = auth.currentUser?.uid
        val petType =
            binding.rgFoundType.findViewById<RadioButton>(binding.rgFoundType.checkedRadioButtonId).text.toString()
        val foundDate = binding.etFoundPetDate.text.toString()
        val finderName = binding.etFoundFinderName.text.toString()
        val phoneNumber = binding.etFoundFinderNumber.text.toString()
        val emailAddress = binding.etFoundFinderEmail.text.toString()
        val decodedAddress = binding.etFoundAddress.text.toString()
        val additionalPetInfo = binding.etFoundPetAdditionalInfo.text.toString()
        val additionalFinderInfo = binding.etFoundFinderAdditionalInfo.text.toString()
        val petBehavior =
            binding.rgFoundBehavior.findViewById<RadioButton>(binding.rgFoundBehavior.checkedRadioButtonId).text.toString()
        val locationData = foundPetViewModel.foundPetData?.foundPetLocation
        val dateAdded = getCurrentDateTime()
        // Perform validation or handle errors if necessary
        // Return the created FoundPetData instance
        return FoundPetData(
            loggedUser,
            foundPetKey,
            petType,
            foundDate,
            finderName,
            phoneNumber,
            emailAddress,
            decodedAddress,
            petBehavior,
            additionalPetInfo,
            additionalFinderInfo,
            dateAdded,
            imageUrl,
            locationData
        )
    }

    private fun getCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun clearData() {
        foundPetViewModel.foundPetData = null
        foundPetViewModel.imageUri = null
    }

    override fun onDestroy() {
        clearData()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "foundCreateFragment"
    }

}