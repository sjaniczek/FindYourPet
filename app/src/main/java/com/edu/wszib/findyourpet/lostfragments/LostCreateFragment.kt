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
import java.text.SimpleDateFormat
import java.util.*

class LostCreateFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
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
    ): View {
        _binding = FragmentCreateLostBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
        val dateEditText: EditText = binding.etLostPetDate
        val hourEditText: EditText = binding.etLostPetHour
        DateInputMask(dateEditText).listen()
        TimeInputMask(hourEditText).listen()
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
            binding.buttonGoToMap.setOnClickListener {
                saveFormData()
                findNavController().navigate(
                    com.edu.wszib.findyourpet.lostfragments.LostCreateFragmentDirections.actionLostCreateFragmentToLostMapsFragment(
                        false,
                        LatLng(52.06, 19.25),
                        "lostpetkey"
                    )
                )
            }
        }
        binding.buttonLostAccept.setOnClickListener {
            uploadImageAndForm()
        }
        val lostPetData = lostPetViewModel.lostPetData
        if (lostPetData != null && lostPetViewModel.imageUri != null) {
            with(binding) {
                rgLostType.findViewWithTag<RadioButton>(lostPetData.lostPetType)?.isChecked =
                    true
                rgLostBehavior.findViewWithTag<RadioButton>(lostPetData.lostPetBehavior)?.isChecked =
                    true
                rgLostReacts.findViewWithTag<RadioButton>(lostPetData.lostPetReact)?.isChecked =
                    true
                ivLostPet.setImageURI(lostPetViewModel.imageUri)
                etLostPetName.setText(lostPetData.lostPetName)
                etLostAddress.setText(lostPetData.lostPetDecodedAddress)
                etLostPetDate.setText(lostPetData.lostPetDate)
                etLostPetHour.setText(lostPetData.lostPetHour)
                etLostPetAdditionalInfo.setText(lostPetData.lostPetAdditionalPetInfo)
                etLostOwnerName.setText(lostPetData.lostPetOwnerName)
                etLostOwnerNumber.setText(lostPetData.lostPetPhoneNumber)
                etLostOwnerEmail.setText(lostPetData.lostPetEmailAddress)
                etLostOwnerAdditionalInfo.setText(lostPetData.lostPetAdditionalOwnerInfo)
                imageUri = lostPetViewModel.imageUri
            }
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
        val lostPetKey = databaseRef.child("lost_pets").push().key
        if (lostPetKey == null) {
            Log.w(TAG, "Couldn't get push key for lostPetKey")
            return
        }
        val fileRef = storageRef.child("images/$fileName")

        if (!validateFieldsAndImage(imageUri)) {
            Toast.makeText(
                context,
                "Wypełnij lub zaznacz wszystkie pola oraz dodaj zdjęcie",
                Toast.LENGTH_SHORT
            )
                .show()
            return
        }
        binding.buttonLostAccept.isVisible = false
        val uploadTask = fileRef.putFile(imageUri!!)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                val lostPetData = createFoundPetData(imageUrl, lostPetKey)
                val lostPetValues = lostPetData.toMap()
                val lostPetUpdates = hashMapOf<String, Any>(
                    "/lost_pets/$lostPetKey" to lostPetValues,
                    "/users/$userId/lost_pets/$lostPetKey" to lostPetValues,
                )
                databaseRef.updateChildren(lostPetUpdates).addOnSuccessListener {
                    // Form uploaded successfully
                    Toast.makeText(context, "Ogłoszenie dodane", Toast.LENGTH_SHORT).show()
                    clearData()
                    //findNavController().popBackStack()
                    findNavController().navigate(com.edu.wszib.findyourpet.lostfragments.LostCreateFragmentDirections.actionLostCreateFragmentToMainFragment())
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
        val lostPetData = LostPetData(
            lostPetName = binding.etLostPetName.text.toString(),
            lostPetDate = binding.etLostPetDate.text.toString(),
            lostPetHour = binding.etLostPetHour.text.toString(),
            lostPetDecodedAddress = binding.etLostAddress.text.toString(),
            lostPetAdditionalPetInfo = binding.etLostPetAdditionalInfo.text.toString(),
            lostPetOwnerName = binding.etLostOwnerName.text.toString(),
            lostPetPhoneNumber = binding.etLostOwnerNumber.text.toString(),
            lostPetEmailAddress = binding.etLostOwnerEmail.text.toString(),
            lostPetAdditionalOwnerInfo = binding.etLostOwnerAdditionalInfo.text.toString(),
            //lostPetImageUri = ((if (imageUri != null) imageUri else lostPetViewModel.lostPetData?.lostPetImageUri)),
            lostPetType = binding.rgLostType.findViewById<RadioButton>(binding.rgLostType.checkedRadioButtonId)?.text.toString(),
            lostPetReact = binding.rgLostReacts.findViewById<RadioButton>(binding.rgLostReacts.checkedRadioButtonId)?.text.toString(),
            lostPetBehavior = binding.rgLostBehavior.findViewById<RadioButton>(binding.rgLostBehavior.checkedRadioButtonId)?.text.toString(),

            )
        lostPetViewModel.saveFormData(lostPetData, imageUri)
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getImageLauncher.launch(intent)
    }

    private fun validateFieldsAndImage(imageUri: Uri?): Boolean {
        val isAnyFieldEmpty = binding.etLostPetName.text.isNullOrEmpty() ||
                binding.etLostAddress.text.isNullOrEmpty() ||
                binding.etLostPetDate.text.isNullOrEmpty() ||
                binding.etLostPetHour.text.isNullOrEmpty() ||
                binding.etLostOwnerName.text.isNullOrEmpty() ||
                binding.etLostOwnerEmail.text.isNullOrEmpty() ||
                binding.etLostOwnerNumber.text.isNullOrEmpty() ||
                binding.rgLostType.checkedRadioButtonId == -1 ||
                binding.rgLostBehavior.checkedRadioButtonId == -1 ||
                binding.rgLostReacts.checkedRadioButtonId == -1 ||
                imageUri == null

        return !isAnyFieldEmpty
    }

    private fun createFoundPetData(imageUrl: String?, lostPetKey: String?): LostPetData {

        val loggedUser = auth.currentUser?.uid
        val petName = binding.etLostPetName.text.toString()
        val petType =
            binding.rgLostType.findViewById<RadioButton>(binding.rgLostType.checkedRadioButtonId).text.toString()
        val lostDate = binding.etLostPetDate.text.toString()
        val lostHour = binding.etLostPetHour.text.toString()
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
        val dateAdded = getCurrentDateTime()
        return LostPetData(
            loggedUser,
            lostPetKey,
            petName,
            petType,
            lostDate,
            lostHour,
            ownerName,
            phoneNumber,
            emailAddress,
            decodedAddress,
            petBehavior,
            petReact,
            additionalPetInfo,
            additionalOwnerInfo,
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
        lostPetViewModel.lostPetData = null
        lostPetViewModel.imageUri = null
    }

    override fun onDestroy() {
        clearData()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "lostCreateFragment"
    }

}