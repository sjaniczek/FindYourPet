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
import androidx.core.view.isVisible
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
import java.util.UUID

class FoundEditFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var foundPetKey: String
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseRef: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private var _binding: FragmentFoundEditBinding? = null
    private val binding: FragmentFoundEditBinding
        get() = _binding!!
    private var imageUrl: String? = null
    private var imageUri: Uri? = null
    private lateinit var currentLocation: LatLng
    private lateinit var dateAdded: String
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
                binding.ivFoundEditPet.setImageURI(imageUri)
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentFoundEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateEditText: EditText = binding.etFoundEditPetDate
        DateInputMask(dateEditText).listen()

        foundPetKey = requireArguments().getString(FOUND_EDIT_POST_KEY)
            ?: throw IllegalArgumentException("Must pass EXTRA_POST_KEY")
        auth = Firebase.auth

        // Initialize Database
        database = Firebase.database(databaseUrl)
        databaseRef = database.reference.child("found_pets").child(foundPetKey)
        activity?.title = "Edit";
        val foundPetData = foundPetViewModel.foundPetData
        imageUri = foundPetViewModel.imageUri
        if (foundPetData != null) {
            with(binding) {
                rgFoundEditType.findViewWithTag<RadioButton>(foundPetData.foundPetType)?.isChecked =
                    true
                rgFoundEditBehavior.findViewWithTag<RadioButton>(foundPetData.foundPetBehavior)?.isChecked =
                    true
                Picasso.get()
                    .load(foundPetData.foundPetImageUrl)
                    .into(ivFoundEditPet)
                etFoundEditAddress.setText(foundPetData.foundPetDecodedAddress)
                etFoundEditPetDate.setText(foundPetData.foundPetDate)
                etFoundEditPetAdditionalInfo.setText(foundPetData.foundPetAdditionalPetInfo)
                etFoundEditFinderName.setText(foundPetData.foundPetFinderName)
                etFoundEditFinderNumber.setText(foundPetData.foundPetPhoneNumber)
                etFoundEditFinderEmail.setText(foundPetData.foundPetEmailAddress)
                etFoundEditFinderAdditionalInfo.setText(foundPetData.foundPetAdditionalFinderInfo)
                dateAdded = foundPetData.foundPetDateAdded.toString()
                imageUrl = foundPetData.foundPetImageUrl
            }
        } else {
            fetchDataAndUpdateUI()
        }

        binding.buttonGoToMap.setOnClickListener {
            saveFormData()
            findNavController().navigate(
                com.edu.wszib.findyourpet.foundfragments.FoundEditFragmentDirections.actionFoundEditFragmentToFoundMapsFragment(
                    true,
                    currentLocation,
                    foundPetKey
                )
            )
        }
        binding.buttonFoundEditAccept.setOnClickListener {
            updateImageAndForm()
        }
        binding.buttonChooseFoundEditPic.setOnClickListener {
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
    }

    private fun fetchDataAndUpdateUI() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i(TAG, "onDataChange")
                val foundPetData = snapshot.getValue<FoundPetData>()
                foundPetData?.let {
                    with(binding) {
                        imageUrl = foundPetData.foundPetImageUrl
                        Picasso.get()
                            .load(foundPetData.foundPetImageUrl)
                            .into(ivFoundEditPet)
                        rgFoundEditType.findViewWithTag<RadioButton>(foundPetData.foundPetType)?.isChecked =
                            true
                        rgFoundEditBehavior.findViewWithTag<RadioButton>(foundPetData.foundPetBehavior)?.isChecked =
                            true

                        //ivFoundEditPet.setImageURI(foundPetViewModel.imageUri)
                        etFoundEditAddress.setText(foundPetData.foundPetDecodedAddress)
                        etFoundEditPetDate.setText(foundPetData.foundPetDate)
                        etFoundEditPetAdditionalInfo.setText(foundPetData.foundPetAdditionalPetInfo)
                        etFoundEditFinderName.setText(foundPetData.foundPetFinderName)
                        etFoundEditFinderNumber.setText(foundPetData.foundPetPhoneNumber)
                        etFoundEditFinderEmail.setText(foundPetData.foundPetEmailAddress)
                        etFoundEditFinderAdditionalInfo.setText(foundPetData.foundPetAdditionalFinderInfo)
                        dateAdded = foundPetData.foundPetDateAdded.toString()
                        currentLocation = LatLng(
                            foundPetData.foundPetLocation?.latitude ?: 1.0,
                            foundPetData.foundPetLocation?.longitude ?: 1.0
                        )

                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                Toast.makeText(
                    context, "Failed to load post.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateImageAndForm() {
        val databaseUrl =
            "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        database = Firebase.database(databaseUrl)
        storage = Firebase.storage
        val storageRef = storage.reference
        val userId = auth.currentUser?.uid
        val fileName = UUID.randomUUID().toString()
        val databaseRef = database.reference
        val fileRef = storageRef.child("images/$fileName")

        if (imageUri != null) {
            if (!validateFieldsAndImage(imageUri)) {
                Toast.makeText(
                    context,
                    "Wypełnij lub zaznacz wszystkie pola oraz dodaj zdjęcie",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        } else {
            if (!validateFields()) {
                Toast.makeText(
                    context,
                    "Wypełnij lub zaznacz wszystkie pola",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }
        binding.buttonFoundEditAccept.isVisible = false
        if (imageUri != null) {
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
                        findNavController().navigate(com.edu.wszib.findyourpet.foundfragments.FoundEditFragmentDirections.actionFoundEditFragmentToMainFragment())
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
        } else {
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
                findNavController().navigate(FoundEditFragmentDirections.actionFoundEditFragmentToMainFragment())
            }
                .addOnFailureListener { e ->
                    // Form upload failed
                    Log.e(TAG, "Error uploading form: ${e.message}", e)
                    Toast.makeText(context, "Error submitting form", Toast.LENGTH_SHORT)
                        .show()
                }
            return
        }

    }

    private fun saveFormData() {
        imageUri = ((if (imageUri != null) imageUri else foundPetViewModel.imageUri))
        val foundPetData = FoundPetData(
            foundPetDate = binding.etFoundEditPetDate.text.toString(),
            foundPetDecodedAddress = binding.etFoundEditAddress.text.toString(),
            foundPetAdditionalPetInfo = binding.etFoundEditPetAdditionalInfo.text.toString(),
            foundPetFinderName = binding.etFoundEditFinderName.text.toString(),
            foundPetPhoneNumber = binding.etFoundEditFinderNumber.text.toString(),
            foundPetEmailAddress = binding.etFoundEditFinderEmail.text.toString(),
            foundPetAdditionalFinderInfo = binding.etFoundEditFinderAdditionalInfo.text.toString(),
            foundPetType = binding.rgFoundEditType.findViewById<RadioButton>(binding.rgFoundEditType.checkedRadioButtonId)?.text.toString(),
            foundPetBehavior = binding.rgFoundEditBehavior.findViewById<RadioButton>(binding.rgFoundEditBehavior.checkedRadioButtonId)?.text.toString(),
            foundPetImageUrl = imageUrl,
            foundPetDateAdded = dateAdded
        )
        foundPetViewModel.saveFormData(foundPetData, imageUri)
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getImageLauncher.launch(intent)
    }

    private fun validateFieldsAndImage(imageUri: Uri?): Boolean {
        val isAnyFieldEmpty = binding.etFoundEditAddress.text.isNullOrEmpty() ||
                binding.etFoundEditPetDate.text.isNullOrEmpty() ||
                binding.etFoundEditFinderName.text.isNullOrEmpty() ||
                binding.etFoundEditFinderEmail.text.isNullOrEmpty() ||
                binding.etFoundEditFinderNumber.text.isNullOrEmpty() ||
                binding.rgFoundEditType.checkedRadioButtonId == -1 ||
                binding.rgFoundEditBehavior.checkedRadioButtonId == -1 ||
                (imageUri == null || imageUrl == null)

        return !isAnyFieldEmpty
    }

    private fun validateFields(): Boolean {
        val isAnyFieldEmpty = binding.etFoundEditAddress.text.isNullOrEmpty() ||
                binding.etFoundEditPetDate.text.isNullOrEmpty() ||
                binding.etFoundEditFinderName.text.isNullOrEmpty() ||
                binding.etFoundEditFinderEmail.text.isNullOrEmpty() ||
                binding.etFoundEditFinderNumber.text.isNullOrEmpty() ||
                binding.rgFoundEditType.checkedRadioButtonId == -1 ||
                binding.rgFoundEditBehavior.checkedRadioButtonId == -1

        return !isAnyFieldEmpty
    }

    private fun createFoundPetData(imageUrl: String?, foundPetKey: String?): FoundPetData {

        val loggedUser = auth.currentUser?.uid
        val petType =
            binding.rgFoundEditType.findViewById<RadioButton>(binding.rgFoundEditType.checkedRadioButtonId).text.toString()
        val foundDate = binding.etFoundEditPetDate.text.toString()
        val ownerName = binding.etFoundEditFinderName.text.toString()
        val phoneNumber = binding.etFoundEditFinderNumber.text.toString()
        val emailAddress = binding.etFoundEditFinderEmail.text.toString()
        val decodedAddress = binding.etFoundEditAddress.text.toString()
        val additionalPetInfo = binding.etFoundEditPetAdditionalInfo.text.toString()
        val additionalOwnerInfo = binding.etFoundEditFinderAdditionalInfo.text.toString()
        val petBehavior =
            binding.rgFoundEditBehavior.findViewById<RadioButton>(binding.rgFoundEditBehavior.checkedRadioButtonId).text.toString()
        val locationData =
            (if (foundPetViewModel.foundPetData?.foundPetLocation != null) foundPetViewModel.foundPetData?.foundPetLocation else FoundPetData.FoundLocation(
                currentLocation
            ))
        val dateAdded = (if (foundPetViewModel.foundPetData?.foundPetDateAdded != null) foundPetViewModel.foundPetData?.foundPetDateAdded else dateAdded)
        return FoundPetData(
            foundPetOwnerId = loggedUser,
            foundPetId = foundPetKey,
            foundPetType = petType,
            foundPetDate = foundDate,
            foundPetFinderName = ownerName,
            foundPetPhoneNumber = phoneNumber,
            foundPetEmailAddress = emailAddress,
            foundPetDecodedAddress = decodedAddress,
            foundPetBehavior = petBehavior,
            foundPetAdditionalPetInfo = additionalPetInfo,
            foundPetAdditionalFinderInfo = additionalOwnerInfo,
            foundPetDateAdded = dateAdded,
            foundPetImageUrl = imageUrl,
            foundPetLocation = locationData
        )

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
        private const val TAG = "FoundEditFragment"
        private const val databaseUrl =
            "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        const val FOUND_EDIT_POST_KEY = "post_key"
    }
}