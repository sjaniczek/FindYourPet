package com.edu.wszib.findyourpet.ui.lostfragments

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
import com.edu.wszib.findyourpet.databinding.FragmentLostEditBinding
import com.edu.wszib.findyourpet.inputmasks.DateInputMask
import com.edu.wszib.findyourpet.inputmasks.TimeInputMask
import com.edu.wszib.findyourpet.models.LostPetData
import com.edu.wszib.findyourpet.models.LostPetViewModel
import com.edu.wszib.findyourpet.models.LostPetViewModelFactory
import com.edu.wszib.findyourpet.repository.LostRepository
import com.google.android.gms.maps.model.LatLng
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class LostEditFragment : Fragment() {


    private var _binding: FragmentLostEditBinding? = null
    private val binding get() = _binding!!
    private var imageUrl: String? = null
    private var imageUri: Uri? = null
    private val viewModel: LostPetViewModel by activityViewModels {
        LostPetViewModelFactory(LostRepository())
    }
    private lateinit var currentLocation: LatLng
    private lateinit var lostPetKey: String

    companion object {
        const val LOST_EDIT_POST_KEY = "post_key"
        private const val TAG = "LostEditFragment"
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
        populateFieldsFromViewModel()
        lostPetKey = requireArguments().getString(LOST_EDIT_POST_KEY)
            ?: throw IllegalArgumentException("Must pass LOST_EDIT_POST_KEY")
        viewModel.loadLostPet(lostPetKey)
        viewModel.editData.observe(viewLifecycleOwner) { data ->
            if (data != null && viewModel.lostPetData.lostPetId.isNullOrEmpty()) {
                fillUiWithData(data)
                viewModel.lostPetData = data
            }
        }
        binding.buttonChooseLostEditPic.setOnClickListener { requestImagePermission() }
        binding.buttonLostEditAccept.setOnClickListener { validateAndUpdate() }
        binding.buttonGoToMap.setOnClickListener {
            saveFieldsToViewModel()
            openMap()
        }
        viewModel.resetUploadState()
        observeUpload()
    }

    private fun fillUiWithData(data: LostPetData) = with(binding) {
        etLostEditPetName.setText(data.lostPetName)
        etLostEditAddress.setText(data.lostPetDecodedAddress)
        etLostEditPetDate.setText(data.lostPetDate)
        etLostEditPetHour.setText(data.lostPetHour)
        etLostEditPetAdditionalInfo.setText(data.lostPetAdditionalPetInfo)
        etLostEditOwnerName.setText(data.lostPetOwnerName)
        etLostEditOwnerNumber.setText(data.lostPetPhoneNumber)
        etLostEditOwnerEmail.setText(data.lostPetEmailAddress)
        etLostEditOwnerAdditionalInfo.setText(data.lostPetAdditionalOwnerInfo)
        imageUrl = data.lostPetImageUrl
        rgLostEditType.children.forEach { rb ->
            if (rb is RadioButton && rb.text.toString() == data.lostPetType) rb.isChecked = true
        }
        rgLostEditBehavior.children.forEach { rb ->
            if (rb is RadioButton && rb.text.toString() == data.lostPetBehavior) rb.isChecked =
                true
        }
        rgLostEditReacts.children.forEach { rb ->
            if (rb is RadioButton && rb.text.toString() == data.lostPetReact) rb.isChecked =
                true
        }
        currentLocation =
            data.lostPetLocation?.let { loc -> LatLng(loc.latitude, loc.longitude) } ?: LatLng(
                0.0,
                0.0
            )
        Picasso.get().load(imageUrl).into(ivLostEditPet)
    }

    private fun openMap() {
        val loc = viewModel.lostPetData.lostPetLocation
        if (loc != null) currentLocation = LatLng(loc.latitude, loc.longitude)
        findNavController().navigate(
            LostEditFragmentDirections.actionLostEditFragmentToLostMapsFragment(
                true,
                currentLocation,
                lostPetKey
            )
        )
    }

    private fun observeUpload() {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uploadState.collect { result ->
                    result?.let {
                        binding.buttonLostEditAccept.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        if (it.isSuccess) {
                            Toast.makeText(context, "Ogłoszenie edytowane", Toast.LENGTH_SHORT)
                                .show()
                            viewModel.resetUploadState()
                            viewModel.clearData()
                            findNavController().navigate(
                                LostEditFragmentDirections.actionLostEditFragmentToMainFragment()
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
        viewModel.lostPetData.let { data ->

            etLostEditPetName.setText(data.lostPetName)
            etLostEditAddress.setText(data.lostPetDecodedAddress)
            etLostEditPetDate.setText(data.lostPetDate)
            etLostEditPetHour.setText(data.lostPetHour)
            etLostEditPetAdditionalInfo.setText(data.lostPetAdditionalPetInfo)
            etLostEditOwnerName.setText(data.lostPetOwnerName)
            etLostEditOwnerNumber.setText(data.lostPetPhoneNumber)
            etLostEditOwnerEmail.setText(data.lostPetEmailAddress)
            etLostEditOwnerAdditionalInfo.setText(data.lostPetAdditionalOwnerInfo)
            imageUri = viewModel.imageUri
            imageUrl = viewModel.lostPetData.lostPetImageUrl
            if (imageUri != null) {
                ivLostEditPet.setImageURI(imageUri)
            } else if (!imageUrl.isNullOrEmpty()) {

                Picasso.get().load(imageUrl).into(ivLostEditPet)
            }
            rgLostEditType.children.forEach { rb ->
                if (rb is RadioButton && rb.text.toString() == data.lostPetType) rb.isChecked = true
            }
            rgLostEditBehavior.children.forEach { rb ->
                if (rb is RadioButton && rb.text.toString() == data.lostPetBehavior) rb.isChecked =
                    true
            }
            rgLostEditReacts.children.forEach { rb ->
                if (rb is RadioButton && rb.text.toString() == data.lostPetReact) rb.isChecked =
                    true
            }
            currentLocation =
                data.lostPetLocation?.let { loc -> LatLng(loc.latitude, loc.longitude) } ?: LatLng(
                    0.0,
                    0.0
                )
        }
    }

    private fun saveFieldsToViewModel() = with(binding) {
        viewModel.lostPetData.apply {
            lostPetName = etLostEditPetName.text.toString()
            lostPetType =
                rgLostEditType.findViewById<RadioButton>(rgLostEditType.checkedRadioButtonId)?.text.toString()
            lostPetDate = etLostEditPetDate.text.toString()
            lostPetHour = etLostEditPetHour.text.toString()
            lostPetDecodedAddress = etLostEditAddress.text.toString()
            lostPetAdditionalPetInfo = etLostEditPetAdditionalInfo.text.toString()
            lostPetOwnerName = etLostEditOwnerName.text.toString()
            lostPetPhoneNumber = etLostEditOwnerNumber.text.toString()
            lostPetEmailAddress = etLostEditOwnerEmail.text.toString()
            lostPetAdditionalOwnerInfo = etLostEditOwnerAdditionalInfo.text.toString()
            lostPetBehavior =
                rgLostEditBehavior.findViewById<RadioButton>(rgLostEditBehavior.checkedRadioButtonId)?.text.toString()
            lostPetReact =
                rgLostEditReacts.findViewById<RadioButton>(rgLostEditReacts.checkedRadioButtonId)?.text.toString()
            viewModel.imageUri = imageUri
            //lostPetLocation = LostPetData.LostLocation(currentLocation)
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
        return etLostEditPetName.text.isNotEmpty() &&
                etLostEditAddress.text.isNotEmpty() &&
                etLostEditPetDate.text.isNotEmpty() &&
                etLostEditPetHour.text.isNotEmpty() &&
                etLostEditOwnerName.text.isNotEmpty() &&
                etLostEditOwnerEmail.text.isNotEmpty() &&
                etLostEditOwnerNumber.text.isNotEmpty() &&
                rgLostEditType.checkedRadioButtonId != -1 &&
                rgLostEditBehavior.checkedRadioButtonId != -1 &&
                rgLostEditReacts.checkedRadioButtonId != -1
    }

    private fun validateAndUpdate() {
        val location = viewModel.lostPetData.lostPetLocation
        if (!validateFields() || location == null) {
            Log.d("uploadtest", "validateAndUpload: " + imageUri.toString() + location.toString())
            Toast.makeText(
                context,
                "Wypełnij pola, wybierz zdjęcie i lokalizację",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        binding.buttonLostEditAccept.isEnabled = false
        val updatedData = with(binding) {
            LostPetData(
                lostPetOwnerId = viewModel.lostPetData.lostPetOwnerId,
                lostPetId = lostPetKey,
                lostPetName = etLostEditPetName.text.toString(),
                lostPetType = rgLostEditType.findViewById<RadioButton>(rgLostEditType.checkedRadioButtonId)?.text.toString(),
                lostPetDate = etLostEditPetDate.text.toString(),
                lostPetHour = etLostEditPetHour.text.toString(),
                lostPetOwnerName = etLostEditOwnerName.text.toString(),
                lostPetPhoneNumber = etLostEditOwnerNumber.text.toString(),
                lostPetEmailAddress = etLostEditOwnerEmail.text.toString(),
                lostPetDecodedAddress = etLostEditAddress.text.toString(),
                lostPetBehavior = rgLostEditBehavior.findViewById<RadioButton>(rgLostEditBehavior.checkedRadioButtonId)?.text.toString(),
                lostPetReact = rgLostEditReacts.findViewById<RadioButton>(rgLostEditReacts.checkedRadioButtonId)?.text.toString(),
                lostPetAdditionalPetInfo = etLostEditPetAdditionalInfo.text.toString(),
                lostPetAdditionalOwnerInfo = etLostEditOwnerAdditionalInfo.text.toString(),
                lostPetDateAdded = viewModel.lostPetData.lostPetDateAdded,
                lostPetImageUrl = viewModel.lostPetData.lostPetImageUrl,
                lostPetLocation = viewModel.lostPetData.lostPetLocation,
            )
        }
        viewModel.updateLostPet(lostPetKey, updatedData, imageUri)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
