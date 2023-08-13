package com.edu.wszib.findyourpet

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentDetailsLostBinding
import com.edu.wszib.findyourpet.databinding.FragmentLostEditBinding
import com.edu.wszib.findyourpet.inputmasks.DateInputMask
import com.edu.wszib.findyourpet.inputmasks.TimeInputMask
import com.edu.wszib.findyourpet.models.LostPetData
import com.edu.wszib.findyourpet.models.LostPetViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import java.lang.IllegalArgumentException
import java.net.URL
import java.util.*

class LostEditFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var lostPetKey: String
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseRef: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private var _binding: FragmentLostEditBinding? = null
    private val binding: FragmentLostEditBinding
        get() = _binding!!
    private var imageUrl: String? = null
    private var imageUri: Uri? = null
    private lateinit var currentLocation: LatLng
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
                binding.ivLostEditPet.setImageURI(imageUri)
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLostEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateEditText: EditText = binding.etLostEditPetDate
        val hourEditText: EditText = binding.etLostEditPetHour
        DateInputMask(dateEditText).listen()
        TimeInputMask(hourEditText).listen()

        lostPetKey = requireArguments().getString(LOST_EDIT_POST_KEY)
            ?: throw IllegalArgumentException("Must pass EXTRA_POST_KEY")
        auth = Firebase.auth

        // Initialize Database
        database = Firebase.database(databaseUrl)
        databaseRef = database.reference.child("lost_pets").child(lostPetKey)
        activity?.title = "Edit";
        val lostPetData = lostPetViewModel.lostPetData
        imageUri = lostPetViewModel.imageUri
        if (lostPetData != null) {
            with(binding) {
                rgLostEditType.findViewWithTag<RadioButton>(lostPetData.lostPetType)?.isChecked =
                    true
                rgLostEditBehavior.findViewWithTag<RadioButton>(lostPetData.lostPetBehavior)?.isChecked =
                    true
                rgLostEditReacts.findViewWithTag<RadioButton>(lostPetData.lostPetReact)?.isChecked =
                    true
                Picasso.get()
                    .load(lostPetData.lostPetImageUrl)
                    .into(ivLostEditPet)
                etLostEditPetName.setText(lostPetData.lostPetName)
                etLostEditAddress.setText(lostPetData.lostPetDecodedAddress)
                etLostEditPetDate.setText(lostPetData.lostPetDate)
                etLostEditPetHour.setText(lostPetData.lostPetHour)
                etLostEditPetAdditionalInfo.setText(lostPetData.lostPetAdditionalPetInfo)
                etLostEditOwnerName.setText(lostPetData.lostPetOwnerName)
                etLostEditOwnerNumber.setText(lostPetData.lostPetPhoneNumber)
                etLostEditOwnerEmail.setText(lostPetData.lostPetEmailAddress)
                etLostEditOwnerAdditionalInfo.setText(lostPetData.lostPetAdditionalOwnerInfo)

                imageUrl = lostPetData.lostPetImageUrl
            }
        }
        else
        {
            fetchDataAndUpdateUI()
        }

        binding.buttonGoToMap.setOnClickListener {
            saveFormData()
            findNavController().navigate(
                LostEditFragmentDirections.actionLostEditFragmentToLostMapsFragment(
                    true,
                    currentLocation,
                    lostPetKey
                )
            )
        }
        binding.buttonLostEditAccept.setOnClickListener{
            updateImageAndForm()
        }
        binding.buttonChooseLostEditPic.setOnClickListener {
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
                Log.i(TAG,"onDataChange")
                val lostPetData = snapshot.getValue<LostPetData>()
                lostPetData?.let {
                    with(binding) {
                        imageUrl = lostPetData.lostPetImageUrl
                        Picasso.get()
                            .load(lostPetData.lostPetImageUrl)
                            .into(ivLostEditPet)
                        rgLostEditType.findViewWithTag<RadioButton>(lostPetData.lostPetType)?.isChecked =
                            true
                        rgLostEditBehavior.findViewWithTag<RadioButton>(lostPetData.lostPetBehavior)?.isChecked =
                            true
                        rgLostEditReacts.findViewWithTag<RadioButton>(lostPetData.lostPetReact)?.isChecked =
                            true
                        //ivLostEditPet.setImageURI(lostPetViewModel.imageUri)
                        etLostEditPetName.setText(lostPetData.lostPetName)
                        etLostEditAddress.setText(lostPetData.lostPetDecodedAddress)
                        etLostEditPetDate.setText(lostPetData.lostPetDate)
                        etLostEditPetHour.setText(lostPetData.lostPetHour)
                        etLostEditPetAdditionalInfo.setText(lostPetData.lostPetAdditionalPetInfo)
                        etLostEditOwnerName.setText(lostPetData.lostPetOwnerName)
                        etLostEditOwnerNumber.setText(lostPetData.lostPetPhoneNumber)
                        etLostEditOwnerEmail.setText(lostPetData.lostPetEmailAddress)
                        etLostEditOwnerAdditionalInfo.setText(lostPetData.lostPetAdditionalOwnerInfo)
                        currentLocation = LatLng(
                            lostPetData.lostPetLocation?.latitude ?: 1.0,
                            lostPetData.lostPetLocation?.longitude ?: 1.0
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
        val databaseUrl = "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        database = Firebase.database(databaseUrl)
        storage = Firebase.storage
        val storageRef = storage.reference
        val userId = auth.currentUser?.uid
        val fileName = UUID.randomUUID().toString()
        val databaseRef = database.reference
        val fileRef = storageRef.child("images/$fileName")

        if (!validateFieldsAndImage(imageUri)) {
            Toast.makeText(
                context,
                "Wypełnij lub zaznacz wszystkie pola oraz dodaj zdjęcie",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (imageUri != null) {
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
                        Toast.makeText(context, "Form submitted", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Before nav")
                        val navController =
                            requireActivity().findNavController(R.id.nav_host_fragment)
                        navController.navigate(R.id.mainFragment) // Navigate to MainFragment
                        Log.e(TAG, "After nav")
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
        else
        {
            val lostPetData = createFoundPetData(imageUrl, lostPetKey)
            val lostPetValues = lostPetData.toMap()
            val lostPetUpdates = hashMapOf<String, Any>(
                "/lost_pets/$lostPetKey" to lostPetValues,
                "/users/$userId/lost_pets/$lostPetKey" to lostPetValues,
            )

            databaseRef.updateChildren(lostPetUpdates).addOnSuccessListener {
                // Form uploaded successfully
                Toast.makeText(context, "Form submitted", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Before nav")
                val navController = requireActivity().findNavController(R.id.nav_host_fragment)
                navController.navigate(R.id.mainFragment) // Navigate to MainFragment
                Log.e(TAG, "After nav")
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
        imageUri = ((if (imageUri != null) imageUri else lostPetViewModel.imageUri))
        val lostPetData = LostPetData(
            lostPetName = binding.etLostEditPetName.text.toString(),
            lostPetDate = binding.etLostEditPetDate.text.toString(),
            lostPetHour = binding.etLostEditPetHour.text.toString(),
            lostPetDecodedAddress = binding.etLostEditAddress.text.toString(),
            lostPetAdditionalPetInfo = binding.etLostEditPetAdditionalInfo.text.toString(),
            lostPetOwnerName = binding.etLostEditOwnerName.text.toString(),
            lostPetPhoneNumber = binding.etLostEditOwnerNumber.text.toString(),
            lostPetEmailAddress = binding.etLostEditOwnerEmail.text.toString(),
            lostPetAdditionalOwnerInfo = binding.etLostEditOwnerAdditionalInfo.text.toString(),
            //lostPetImageUri = ((if (imageUri != null) imageUri else lostPetViewModel.lostPetData?.lostPetImageUri)),
            lostPetType = binding.rgLostEditType.findViewById<RadioButton>(binding.rgLostEditType.checkedRadioButtonId)?.text.toString(),
            lostPetReact = binding.rgLostEditReacts.findViewById<RadioButton>(binding.rgLostEditReacts.checkedRadioButtonId)?.text.toString(),
            lostPetBehavior = binding.rgLostEditBehavior.findViewById<RadioButton>(binding.rgLostEditBehavior.checkedRadioButtonId)?.text.toString(),
            lostPetImageUrl = imageUrl
            )
        lostPetViewModel.saveFormData(lostPetData, imageUri)
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getImageLauncher.launch(intent)
    }

    private fun validateFieldsAndImage(imageUri: Uri?): Boolean {
        val isAnyFieldEmpty = binding.etLostEditPetName.text.isNullOrEmpty() ||
                binding.etLostEditAddress.text.isNullOrEmpty() ||
                binding.etLostEditAge.text.isNullOrEmpty() ||
                binding.etLostEditPetDate.text.isNullOrEmpty() ||
                binding.etLostEditPetHour.text.isNullOrEmpty() ||
                binding.etLostEditOwnerName.text.isNullOrEmpty() ||
                binding.etLostEditOwnerEmail.text.isNullOrEmpty() ||
                binding.etLostEditOwnerNumber.text.isNullOrEmpty() ||
                binding.rgLostEditType.checkedRadioButtonId == -1 ||
                binding.rgLostEditBehavior.checkedRadioButtonId == -1 ||
                binding.rgLostEditReacts.checkedRadioButtonId == -1 ||
                (imageUri == null || imageUrl == null)

        return !isAnyFieldEmpty
    }

    private fun createFoundPetData(imageUrl: String?, lostPetKey: String?): LostPetData {

        val loggedUser = auth.currentUser?.uid
        val petName = binding.etLostEditPetName.text.toString()
        val petType =
            binding.rgLostEditType.findViewById<RadioButton>(binding.rgLostEditType.checkedRadioButtonId).text.toString()
        val petAge = binding.etLostEditAge.text.toString()
        val lostDate = binding.etLostEditPetDate.text.toString()
        val lostHour = binding.etLostEditPetHour.text.toString()
        val ownerName = binding.etLostEditOwnerName.text.toString()
        val phoneNumber = binding.etLostEditOwnerNumber.text.toString()
        val emailAddress = binding.etLostEditOwnerEmail.text.toString()
        val decodedAddress = binding.etLostEditAddress.text.toString()
        val additionalPetInfo = binding.etLostEditPetAdditionalInfo.text.toString()
        val additionalOwnerInfo = binding.etLostEditOwnerAdditionalInfo.text.toString()
        val petReact =
            binding.rgLostEditReacts.findViewById<RadioButton>(binding.rgLostEditReacts.checkedRadioButtonId).text.toString()
        val petBehavior =
            binding.rgLostEditBehavior.findViewById<RadioButton>(binding.rgLostEditBehavior.checkedRadioButtonId).text.toString()
        val locationData = lostPetViewModel.lostPetData?.lostPetLocation
        val dateAdded = lostPetViewModel.lostPetData?.lostPetDateAdded
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

    companion object {
        private const val TAG = "LostEditFragment"
        private const val databaseUrl =
            "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        const val LOST_EDIT_POST_KEY = "post_key"
    }
}