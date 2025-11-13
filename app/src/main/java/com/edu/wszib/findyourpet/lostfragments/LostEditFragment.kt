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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentLostEditBinding
import com.edu.wszib.findyourpet.inputmasks.DateInputMask
import com.edu.wszib.findyourpet.inputmasks.TimeInputMask
import com.edu.wszib.findyourpet.models.LostPetData
import com.edu.wszib.findyourpet.models.LostPetViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import java.util.*

class LostEditFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseRef: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private var _binding: FragmentLostEditBinding? = null
    private val binding get() = _binding!!
    private var imageUrl: String? = null
    private var imageUri: Uri? = null
    private lateinit var currentLocation: LatLng
    private lateinit var dateAdded: String
    private lateinit var lostPetKey: String
    private val lostPetViewModel: LostPetViewModel by activityViewModels()
    private var isUploading = false

    companion object {
        private const val TAG = "LostEditFragment"
        const val LOST_EDIT_POST_KEY = "post_key"
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
                binding.ivLostEditPet.setImageURI(imageUri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLostEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DateInputMask(binding.etLostEditPetDate).listen()
        TimeInputMask(binding.etLostEditPetHour).listen()

        lostPetKey = requireArguments().getString(LOST_EDIT_POST_KEY)
            ?: throw IllegalArgumentException("Must pass LOST_EDIT_POST_KEY")
        auth = Firebase.auth
        database = Firebase.database(databaseUrl)
        databaseRef = database.reference.child("lost_pets").child(lostPetKey)
        storage = Firebase.storage

        if (lostPetViewModel.lostPetData != null) {
            restoreDataFromViewModel()
        } else {
            fetchDataAndUpdateUI()
        }

        binding.buttonChooseLostEditPic.setOnClickListener { checkPermissionAndPickImage() }

        binding.buttonGoToMap.setOnClickListener {
            saveFormDataToViewModel()
            findNavController().navigate(
                LostEditFragmentDirections.actionLostEditFragmentToLostMapsFragment(
                    true,
                    currentLocation,
                    lostPetKey
                )
            )
        }

        binding.buttonLostEditAccept.setOnClickListener { updateImageAndForm() }
    }

    private fun restoreDataFromViewModel() {
        lostPetViewModel.lostPetData?.let { data ->
            binding.rgLostEditType.findViewWithTag<RadioButton>(data.lostPetType)?.isChecked = true
            binding.rgLostEditBehavior.findViewWithTag<RadioButton>(data.lostPetBehavior)?.isChecked = true
            binding.rgLostEditReacts.findViewWithTag<RadioButton>(data.lostPetReact)?.isChecked = true
            binding.etLostEditPetName.setText(data.lostPetName)
            binding.etLostEditAddress.setText(data.lostPetDecodedAddress)
            binding.etLostEditPetDate.setText(data.lostPetDate)
            binding.etLostEditPetHour.setText(data.lostPetHour)
            binding.etLostEditPetAdditionalInfo.setText(data.lostPetAdditionalPetInfo)
            binding.etLostEditOwnerName.setText(data.lostPetOwnerName)
            binding.etLostEditOwnerNumber.setText(data.lostPetPhoneNumber)
            binding.etLostEditOwnerEmail.setText(data.lostPetEmailAddress)
            binding.etLostEditOwnerAdditionalInfo.setText(data.lostPetAdditionalOwnerInfo)
            imageUri = lostPetViewModel.imageUri
            imageUrl = lostPetViewModel.lostPetData?.lostPetImageUrl
            if (imageUri != null) binding.ivLostEditPet.setImageURI(imageUri)
            currentLocation = data.lostPetLocation?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(0.0, 0.0)
            dateAdded = data.lostPetDateAdded.toString()
        }
    }

    private fun fetchDataAndUpdateUI() {
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue<LostPetData>()
                data?.let {
                    binding.rgLostEditType.findViewWithTag<RadioButton>(it.lostPetType)?.isChecked = true
                    binding.rgLostEditBehavior.findViewWithTag<RadioButton>(it.lostPetBehavior)?.isChecked = true
                    binding.rgLostEditReacts.findViewWithTag<RadioButton>(it.lostPetReact)?.isChecked = true
                    binding.etLostEditPetName.setText(it.lostPetName)
                    binding.etLostEditAddress.setText(it.lostPetDecodedAddress)
                    binding.etLostEditPetDate.setText(it.lostPetDate)
                    binding.etLostEditPetHour.setText(it.lostPetHour)
                    binding.etLostEditPetAdditionalInfo.setText(it.lostPetAdditionalPetInfo)
                    binding.etLostEditOwnerName.setText(it.lostPetOwnerName)
                    binding.etLostEditOwnerNumber.setText(it.lostPetPhoneNumber)
                    binding.etLostEditOwnerEmail.setText(it.lostPetEmailAddress)
                    binding.etLostEditOwnerAdditionalInfo.setText(it.lostPetAdditionalOwnerInfo)
                    imageUrl = it.lostPetImageUrl
                    currentLocation = it.lostPetLocation?.let { loc -> LatLng(loc.latitude, loc.longitude) } ?: LatLng(0.0, 0.0)
                    dateAdded = it.lostPetDateAdded.toString()
                    Picasso.get().load(imageUrl).into(binding.ivLostEditPet)
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
        val data = LostPetData(
            lostPetName = binding.etLostEditPetName.text.toString(),
            lostPetType = binding.rgLostEditType.findViewById<RadioButton>(binding.rgLostEditType.checkedRadioButtonId)?.text.toString(),
            lostPetDate = binding.etLostEditPetDate.text.toString(),
            lostPetHour = binding.etLostEditPetHour.text.toString(),
            lostPetDecodedAddress = binding.etLostEditAddress.text.toString(),
            lostPetAdditionalPetInfo = binding.etLostEditPetAdditionalInfo.text.toString(),
            lostPetOwnerName = binding.etLostEditOwnerName.text.toString(),
            lostPetPhoneNumber = binding.etLostEditOwnerNumber.text.toString(),
            lostPetEmailAddress = binding.etLostEditOwnerEmail.text.toString(),
            lostPetAdditionalOwnerInfo = binding.etLostEditOwnerAdditionalInfo.text.toString(),
            lostPetBehavior = binding.rgLostEditBehavior.findViewById<RadioButton>(binding.rgLostEditBehavior.checkedRadioButtonId)?.text.toString(),
            lostPetReact = binding.rgLostEditReacts.findViewById<RadioButton>(binding.rgLostEditReacts.checkedRadioButtonId)?.text.toString(),
            lostPetDateAdded = dateAdded,
            lostPetImageUrl = imageUrl,
            lostPetLocation = LostPetData.LostLocation(currentLocation)
        )
        lostPetViewModel.saveFormData(data, imageUri)
    }

    private fun validateFields(): Boolean {
        return !(binding.etLostEditPetName.text.isNullOrEmpty() ||
                binding.etLostEditAddress.text.isNullOrEmpty() ||
                binding.etLostEditPetDate.text.isNullOrEmpty() ||
                binding.etLostEditPetHour.text.isNullOrEmpty() ||
                binding.etLostEditOwnerName.text.isNullOrEmpty() ||
                binding.etLostEditOwnerEmail.text.isNullOrEmpty() ||
                binding.etLostEditOwnerNumber.text.isNullOrEmpty() ||
                binding.rgLostEditType.checkedRadioButtonId == -1 ||
                binding.rgLostEditBehavior.checkedRadioButtonId == -1 ||
                binding.rgLostEditReacts.checkedRadioButtonId == -1)
    }

    private fun validateFieldsAndImage(uri: Uri?): Boolean {
        return validateFields() && (uri != null || imageUrl != null)
    }

    private fun updateImageAndForm() {
        if (isUploading) return
        isUploading = true
        binding.buttonLostEditAccept.isEnabled = false
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
                .setCustomMetadata("postId", lostPetKey)
                .build()

            fileRef.putFile(imageUri!!, metadata).addOnSuccessListener { task ->
                task.storage.downloadUrl.addOnSuccessListener { uri ->
                    val data = createLostPetData(uri.toString(), lostPetKey)
                    val values = data.toMap()
                    val updates = hashMapOf<String, Any>(
                        "/lost_pets/$lostPetKey" to values,
                        "/users/$userId/lost_pets/$lostPetKey" to values
                    )
                    dbRef.updateChildren(updates).addOnSuccessListener {
                        Toast.makeText(context, "Ogłoszenie edytowane", Toast.LENGTH_SHORT).show()
                        clearData()
                        resetUploadUI()
                        findNavController().navigate(
                            LostEditFragmentDirections.actionLostEditFragmentToMainFragment()
                        )
                    }
                }
            }
        } else {
            if (!validateFields()) {
                Toast.makeText(context, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                resetUploadUI()
                return
            }
            val data = createLostPetData(imageUrl, lostPetKey)
            val values = data.toMap()
            val updates = hashMapOf<String, Any>(
                "/lost_pets/$lostPetKey" to values,
                "/users/$userId/lost_pets/$lostPetKey" to values
            )
            dbRef.updateChildren(updates).addOnSuccessListener {
                Toast.makeText(context, "Ogłoszenie edytowane", Toast.LENGTH_SHORT).show()
                clearData()
                resetUploadUI()
                findNavController().navigate(
                    LostEditFragmentDirections.actionLostEditFragmentToMainFragment()
                )
            }
        }
    }

    private fun createLostPetData(imageUrl: String?, key: String?): LostPetData {
        return LostPetData(
            lostPetOwnerId = auth.currentUser?.uid,
            lostPetId = key,
            lostPetName = binding.etLostEditPetName.text.toString(),
            lostPetType = binding.rgLostEditType.findViewById<RadioButton>(binding.rgLostEditType.checkedRadioButtonId)?.text.toString(),
            lostPetDate = binding.etLostEditPetDate.text.toString(),
            lostPetHour = binding.etLostEditPetHour.text.toString(),
            lostPetOwnerName = binding.etLostEditOwnerName.text.toString(),
            lostPetPhoneNumber = binding.etLostEditOwnerNumber.text.toString(),
            lostPetEmailAddress = binding.etLostEditOwnerEmail.text.toString(),
            lostPetDecodedAddress = binding.etLostEditAddress.text.toString(),
            lostPetBehavior = binding.rgLostEditBehavior.findViewById<RadioButton>(binding.rgLostEditBehavior.checkedRadioButtonId)?.text.toString(),
            lostPetReact = binding.rgLostEditReacts.findViewById<RadioButton>(binding.rgLostEditReacts.checkedRadioButtonId)?.text.toString(),
            lostPetAdditionalPetInfo = binding.etLostEditPetAdditionalInfo.text.toString(),
            lostPetAdditionalOwnerInfo = binding.etLostEditOwnerAdditionalInfo.text.toString(),
            lostPetDateAdded = dateAdded,
            lostPetImageUrl = imageUrl,
            lostPetLocation = lostPetViewModel.lostPetData?.lostPetLocation ?: LostPetData.LostLocation(currentLocation)
        )
    }

    private fun clearData() {
        lostPetViewModel.lostPetData = null
        lostPetViewModel.imageUri = null
    }

    private fun resetUploadUI() {
        isUploading = false
        binding.buttonLostEditAccept.isEnabled = true
        binding.progressBar.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
