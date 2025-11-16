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
import com.edu.wszib.findyourpet.databinding.FragmentCreateLostBinding
import com.edu.wszib.findyourpet.inputmasks.DateInputMask
import com.edu.wszib.findyourpet.inputmasks.TimeInputMask
import com.edu.wszib.findyourpet.models.LostPetData
import com.edu.wszib.findyourpet.models.LostPetViewModel
import com.edu.wszib.findyourpet.models.LostPetViewModelFactory
import com.edu.wszib.findyourpet.repository.LostRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LostCreateFragment : Fragment() {

    private var _binding: FragmentCreateLostBinding? = null
    private val binding get() = _binding!!
    private var imageUri: Uri? = null
    private val viewModel: LostPetViewModel by activityViewModels {
        LostPetViewModelFactory(LostRepository())
    }
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) launchImagePicker()
            else Toast.makeText(context, "Brak uprawnień do zdjęć", Toast.LENGTH_SHORT).show()
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

        DateInputMask(binding.etLostPetDate).listen()
        TimeInputMask(binding.etLostPetHour).listen()
        populateFieldsFromViewModel()
        binding.buttonChooseLostPic.setOnClickListener { requestImagePermission() }
        binding.buttonLostAccept.setOnClickListener { validateAndUpload() }
        binding.buttonGoToMap.setOnClickListener {
            saveFieldsToViewModel()
            openMap()
        }
        observeUpload()
    }

    private fun observeUpload() {
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uploadState.collectLatest { result ->
                    result?.let {
                        binding.buttonLostAccept.isEnabled = true
                        if (it.isSuccess) {
                            Toast.makeText(context, "Ogłoszenie dodane", Toast.LENGTH_SHORT).show()
                            viewModel.resetUploadState()
                            viewModel.clearData()
                            findNavController().navigate(
                                LostCreateFragmentDirections.actionLostCreateFragmentToMainFragment()
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

    private fun openMap() {
        findNavController().navigate(
            LostCreateFragmentDirections.actionLostCreateFragmentToLostMapsFragment(
                false,
                LatLng(52.06, 19.25),
                "lostpetkey"
            )
        )
    }

    private fun populateFieldsFromViewModel() = with(binding) {
        viewModel.lostPetData.let { data ->
            etLostPetName.setText(data.lostPetName)
            etLostAddress.setText(data.lostPetDecodedAddress)
            etLostPetDate.setText(data.lostPetDate)
            etLostPetHour.setText(data.lostPetHour)
            etLostPetAdditionalInfo.setText(data.lostPetAdditionalPetInfo)
            etLostOwnerName.setText(data.lostPetOwnerName)
            etLostOwnerNumber.setText(data.lostPetPhoneNumber)
            etLostOwnerEmail.setText(data.lostPetEmailAddress)
            etLostOwnerAdditionalInfo.setText(data.lostPetAdditionalOwnerInfo)
            imageUri = viewModel.imageUri
            if (imageUri != null) ivLostPet.setImageURI(imageUri)
            rgLostType.children.forEach { rb ->
                if (rb is RadioButton && rb.text.toString() == data.lostPetType) rb.isChecked = true
            }
            rgLostBehavior.children.forEach { rb ->
                if (rb is RadioButton && rb.text.toString() == data.lostPetBehavior) rb.isChecked =
                    true
            }
            rgLostReacts.children.forEach { rb ->
                if (rb is RadioButton && rb.text.toString() == data.lostPetReact) rb.isChecked =
                    true
            }
        }
    }

    private fun saveFieldsToViewModel() = with(binding) {
        viewModel.lostPetData.apply {
            lostPetName = etLostPetName.text.toString()
            lostPetDate = etLostPetDate.text.toString()
            lostPetHour = etLostPetHour.text.toString()
            lostPetDecodedAddress = etLostAddress.text.toString()
            lostPetAdditionalPetInfo = etLostPetAdditionalInfo.text.toString()
            lostPetOwnerName = etLostOwnerName.text.toString()
            lostPetPhoneNumber = etLostOwnerNumber.text.toString()
            lostPetEmailAddress = etLostOwnerEmail.text.toString()
            lostPetAdditionalOwnerInfo = etLostOwnerAdditionalInfo.text.toString()
            lostPetType =
                rgLostType.findViewById<RadioButton>(rgLostType.checkedRadioButtonId)?.text.toString()
            lostPetReact =
                rgLostReacts.findViewById<RadioButton>(rgLostReacts.checkedRadioButtonId)?.text.toString()
            lostPetBehavior =
                rgLostBehavior.findViewById<RadioButton>(rgLostBehavior.checkedRadioButtonId)?.text.toString()
            //lostPetLocation = lostPetViewModel.lostPetData?.lostPetLocation
            viewModel.imageUri = imageUri
        }
        //  lostPetViewModel.saveFormData(data, imageUri)
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

    private fun validateAndUpload() {
        val location = viewModel.lostPetData.lostPetLocation
        if (!validateFields() || imageUri == null || location == null) {
            Log.d("uploadtest", "validateAndUpload: " + imageUri.toString() + location.toString())
            Toast.makeText(context, "Wypełnij wszystkie pola i dodaj zdjęcie", Toast.LENGTH_SHORT)
                .show()
            return
        }
        binding.buttonLostAccept.isEnabled = false
        val data = with(binding) {
            LostPetData(
                lostPetType = rgLostType.findViewById<RadioButton>(rgLostType.checkedRadioButtonId)?.text.toString(),
                lostPetDate = etLostPetDate.text.toString(),
                lostPetName = etLostPetName.text.toString(),
                lostPetOwnerName = etLostOwnerName.text.toString(),
                lostPetDecodedAddress = viewModel.lostPetData.lostPetDecodedAddress ?: "",
                lostPetHour = etLostPetHour.text.toString(),
                lostPetPhoneNumber = etLostOwnerNumber.text.toString(),
                lostPetEmailAddress = etLostOwnerEmail.text.toString(),
                lostPetBehavior = rgLostBehavior.findViewById<RadioButton>(rgLostBehavior.checkedRadioButtonId)?.text.toString(),
                lostPetReact = rgLostReacts.findViewById<RadioButton>(rgLostReacts.checkedRadioButtonId)?.text.toString(),
                lostPetAdditionalPetInfo = etLostPetAdditionalInfo.text.toString(),
                lostPetAdditionalOwnerInfo = etLostOwnerAdditionalInfo.text.toString(),
                lostPetLocation = location,
                lostPetDateAdded = getCurrentDateTime()
            )
        }
        viewModel.uploadLostPet(data, imageUri!!)
    }

    private fun validateFields(): Boolean = with(binding) {
        return etLostPetName.text.isNotEmpty() &&
                etLostAddress.text.isNotEmpty() &&
                etLostPetDate.text.isNotEmpty() &&
                etLostPetHour.text.isNotEmpty() &&
                etLostOwnerName.text.isNotEmpty() &&
                etLostOwnerEmail.text.isNotEmpty() &&
                etLostOwnerNumber.text.isNotEmpty() &&
                rgLostType.checkedRadioButtonId != -1 &&
                rgLostBehavior.checkedRadioButtonId != -1 &&
                rgLostReacts.checkedRadioButtonId != -1

    }

    private fun getCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    companion object {
        private const val TAG = "LostCreateFragment"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
