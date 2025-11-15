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
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentFoundEditBinding
import com.edu.wszib.findyourpet.inputmasks.DateInputMask
import com.edu.wszib.findyourpet.models.FoundPetData
import com.edu.wszib.findyourpet.models.FoundPetViewModel
import com.edu.wszib.findyourpet.models.FoundPetViewModelFactory
import com.edu.wszib.findyourpet.repository.FoundRepository
import com.edu.wszib.findyourpet.ui.LoginFragmentDirections
import com.google.android.gms.maps.model.LatLng
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class FoundEditFragment : Fragment() {

    private var _binding: FragmentFoundEditBinding? = null
    private val binding get() = _binding!!
    private var imageUri: Uri? = null
    private var imageUrl: String? = null
    private lateinit var dateAdded: String
    private val viewModel: FoundPetViewModel by activityViewModels {
        FoundPetViewModelFactory(FoundRepository())
    }
    private lateinit var currentLocation: LatLng
    private lateinit var foundPetKey: String

    companion object {
        const val FOUND_EDIT_POST_KEY = "post_key"
        const val TAG = "FoundEditFragment"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchImagePicker()
        else Toast.makeText(context, "Brak uprawnień do zdjęć", Toast.LENGTH_SHORT).show()
    }

    private val getImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            imageUri = result.data?.data
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
        populateFieldsFromViewModel()
        foundPetKey = requireArguments().getString(FOUND_EDIT_POST_KEY)
            ?: throw IllegalArgumentException("Must pass post_key")
        viewModel.loadFoundPet(foundPetKey)
        viewModel.editData.observe(viewLifecycleOwner) { data ->
            if (data != null && viewModel.foundPetData.foundPetId.isNullOrEmpty()) {
                fillUIWithData(data)
                viewModel.foundPetData = data
            }
        }
        binding.buttonChooseFoundEditPic.setOnClickListener { requestImagePermission() }
        binding.buttonFoundEditAccept.setOnClickListener { validateAndUpdate() }
        binding.buttonGoToMap.setOnClickListener {
            saveFieldsToViewModel()
            openMap()
        }
        viewModel.resetUploadState()
        observeUpdate()
    }

    private fun fillUIWithData(data: FoundPetData) = with(binding) {
        rgFoundEditType.findViewWithTag<RadioButton>(data.foundPetType)?.isChecked = true
        rgFoundEditBehavior.findViewWithTag<RadioButton>(data.foundPetBehavior)?.isChecked = true
        etFoundEditAddress.setText(data.foundPetDecodedAddress)
        etFoundEditPetDate.setText(data.foundPetDate)
        etFoundEditPetAdditionalInfo.setText(data.foundPetAdditionalPetInfo)
        etFoundEditFinderName.setText(data.foundPetFinderName)
        etFoundEditFinderNumber.setText(data.foundPetPhoneNumber)
        etFoundEditFinderEmail.setText(data.foundPetEmailAddress)
        etFoundEditFinderAdditionalInfo.setText(data.foundPetAdditionalFinderInfo)
        imageUrl = data.foundPetImageUrl
        Picasso.get().load(imageUrl).into(binding.ivFoundEditPet)
        currentLocation =
            data.foundPetLocation?.let { loc -> LatLng(loc.latitude, loc.longitude) } ?: LatLng(
                0.0,
                0.0
            )

    }

    private fun openMap() {
        val loc = viewModel.foundPetData.foundPetLocation
        if (loc != null) currentLocation = LatLng(loc.latitude, loc.longitude)
        findNavController().navigate(
            FoundEditFragmentDirections.actionFoundEditFragmentToFoundMapsFragment(
                true,
                currentLocation,
                foundPetKey
            )
        )
    }

    private fun observeUpdate() {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uploadState.collect { result ->
                    result?.let {
                        binding.buttonFoundEditAccept.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        if (it.isSuccess) {
                            Toast.makeText(context, "Ogłoszenie edytowane", Toast.LENGTH_SHORT)
                                .show()
                            //Log.d(TAG, "Before clear data"+viewModel.foundPetData.toString())
                            viewModel.resetUploadState()
                            viewModel.clearData()
                            //Log.d(TAG, "After clear data"+viewModel.foundPetData.toString())
                            findNavController().navigate(
                                FoundEditFragmentDirections.actionFoundEditFragmentToMainFragment()
                            )
                        } else {
                            Toast.makeText(
                                context,
                                "Błąd: ${it.exceptionOrNull()?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun populateFieldsFromViewModel() = with(binding) {
        viewModel.foundPetData.let { data ->

            etFoundEditPetDate.setText(data.foundPetDate)
            etFoundEditAddress.setText(data.foundPetDecodedAddress)
            etFoundEditFinderName.setText(data.foundPetFinderName)
            etFoundEditFinderNumber.setText(data.foundPetPhoneNumber)
            etFoundEditFinderEmail.setText(data.foundPetEmailAddress)
            etFoundEditPetAdditionalInfo.setText(data.foundPetAdditionalPetInfo)
            etFoundEditFinderAdditionalInfo.setText(data.foundPetAdditionalFinderInfo)
            imageUri = viewModel.imageUri
            imageUrl = viewModel.foundPetData.foundPetImageUrl
            if (imageUri != null) {
                ivFoundEditPet.setImageURI(imageUri)
            } else if (!imageUrl.isNullOrEmpty()) {

                Picasso.get().load(imageUrl).into(ivFoundEditPet)
            }

            rgFoundEditType.children.forEach { rb ->
                if (rb is RadioButton && rb.text.toString() == data.foundPetType) rb.isChecked =
                    true
            }
            rgFoundEditBehavior.children.forEach { rb ->
                if (rb is RadioButton && rb.text.toString() == data.foundPetBehavior) rb.isChecked =
                    true
            }
            currentLocation =
                data.foundPetLocation?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(0.0, 0.0)
        }
    }

    private fun saveFieldsToViewModel() = with(binding) {
        viewModel.foundPetData.apply {
            foundPetDate = etFoundEditPetDate.text.toString()
            foundPetDecodedAddress = etFoundEditAddress.text.toString()
            foundPetFinderName = etFoundEditFinderName.text.toString()
            foundPetPhoneNumber = etFoundEditFinderNumber.text.toString()
            foundPetEmailAddress = etFoundEditFinderEmail.text.toString()
            foundPetBehavior = rgFoundEditBehavior.findViewById<RadioButton>(rgFoundEditBehavior.checkedRadioButtonId)?.text.toString()
            foundPetType = rgFoundEditType.findViewById<RadioButton>(rgFoundEditType.checkedRadioButtonId)?.text.toString()
            foundPetAdditionalPetInfo = etFoundEditPetAdditionalInfo.text.toString()
            foundPetAdditionalFinderInfo = etFoundEditFinderAdditionalInfo.text.toString()
            viewModel.imageUri = imageUri
        }
    }

    private fun requestImagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            )
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            else launchImagePicker()
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            )
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            else launchImagePicker()
        }
    }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getImageLauncher.launch(intent)
    }

    private fun validateFields(): Boolean = with(binding) {
        return etFoundEditPetDate.text.isNotEmpty() &&
                etFoundEditAddress.text.isNotEmpty() &&
                etFoundEditFinderName.text.isNotEmpty() &&
                etFoundEditFinderNumber.text.isNotEmpty() &&
                etFoundEditFinderEmail.text.isNotEmpty() &&
                rgFoundEditType.checkedRadioButtonId != -1 &&
                rgFoundEditBehavior.checkedRadioButtonId != -1
    }

    private fun validateAndUpdate() {
        val location = viewModel.foundPetData.foundPetLocation
        if (!validateFields() || location == null) {
            Toast.makeText(
                context,
                "Wypełnij pola, wybierz zdjęcie i lokalizację",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        binding.buttonFoundEditAccept.isEnabled = false
        val updatedData = with(binding) {
            FoundPetData(
                foundPetId = foundPetKey,
                foundPetOwnerId = viewModel.foundPetData.foundPetOwnerId,
                foundPetType = rgFoundEditType.findViewById<RadioButton>(rgFoundEditType.checkedRadioButtonId)?.text.toString(),
                foundPetBehavior = rgFoundEditBehavior.findViewById<RadioButton>(rgFoundEditBehavior.checkedRadioButtonId)?.text.toString(),
                foundPetDecodedAddress = etFoundEditAddress.text.toString(),
                foundPetDate = etFoundEditPetDate.text.toString(),
                foundPetAdditionalPetInfo = etFoundEditPetAdditionalInfo.text.toString(),
                foundPetFinderName = etFoundEditFinderName.text.toString(),
                foundPetPhoneNumber = etFoundEditFinderNumber.text.toString(),
                foundPetEmailAddress = etFoundEditFinderEmail.text.toString(),
                foundPetAdditionalFinderInfo = etFoundEditFinderAdditionalInfo.text.toString(),
                foundPetDateAdded = viewModel.foundPetData.foundPetDateAdded,
                foundPetImageUrl = viewModel.foundPetData.foundPetImageUrl,
                foundPetLocation = viewModel.foundPetData.foundPetLocation,
            )
        }
        viewModel.updateFoundPet(foundPetKey, updatedData, imageUri)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
