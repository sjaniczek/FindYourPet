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
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentCreateFoundBinding
import com.edu.wszib.findyourpet.inputmasks.DateInputMask
import com.edu.wszib.findyourpet.models.FoundPetData
import com.edu.wszib.findyourpet.models.FoundPetViewModel
import com.edu.wszib.findyourpet.models.FoundPetViewModelFactory
import com.edu.wszib.findyourpet.models.ReportData
import com.edu.wszib.findyourpet.repository.FoundRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FoundCreateFragment : Fragment() {

    private var _binding: FragmentCreateFoundBinding? = null
    private val binding get() = _binding!!
    private var imageUri: Uri? = null
    private val viewModel: FoundPetViewModel by activityViewModels {
        FoundPetViewModelFactory(FoundRepository())
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
                binding.ivFoundPet.setImageURI(imageUri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        FragmentCreateFoundBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DateInputMask(binding.etFoundPetDate).listen()
        populateFieldsFromViewModel()
        binding.buttonChooseFoundPic.setOnClickListener { requestImagePermission() }
        binding.buttonFoundAccept.setOnClickListener { validateAndUpload() }
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
                        binding.buttonFoundAccept.isEnabled = true
                        if (it.isSuccess) {
                            Toast.makeText(context, "Ogłoszenie dodane", Toast.LENGTH_SHORT).show()
                            viewModel.resetUploadState()
                            viewModel.clearData()
                            findNavController().navigate(
                                FoundCreateFragmentDirections.actionFoundCreateFragmentToMainFragment()
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
            FoundCreateFragmentDirections.actionFoundCreateFragmentToFoundMapsFragment(
                false,
                LatLng(52.06, 19.25),
                "foundpetkey"
            )
        )
    }

    private fun populateFieldsFromViewModel() = with(binding) {

        viewModel.foundPetData.let { data ->
            etFoundPetDate.setText(data.foundPetDate)
            etFoundAddress.setText(data.foundPetDecodedAddress)
            etFoundFinderName.setText(data.foundPetFinderName)
            etFoundFinderNumber.setText(data.foundPetPhoneNumber)
            etFoundFinderEmail.setText(data.foundPetEmailAddress)
            etFoundPetAdditionalInfo.setText(data.foundPetAdditionalPetInfo)
            etFoundFinderAdditionalInfo.setText(data.foundPetAdditionalFinderInfo)
            imageUri = viewModel.imageUri
            if (imageUri != null) ivFoundPet.setImageURI(imageUri)
            rgFoundType.children.forEach { rb ->
                if (rb is RadioButton && rb.text.toString() == data.foundPetType) rb.isChecked =
                    true
            }
            rgFoundBehavior.children.forEach { rb ->
                if (rb is RadioButton && rb.text.toString() == data.foundPetBehavior) rb.isChecked =
                    true
            }
        }
    }

    private fun saveFieldsToViewModel() = with(binding) {
        viewModel.foundPetData.apply {
            foundPetDate = etFoundPetDate.text.toString()
            foundPetDecodedAddress = etFoundAddress.text.toString()
            foundPetFinderName = etFoundFinderName.text.toString()
            foundPetPhoneNumber = etFoundFinderNumber.text.toString()
            foundPetEmailAddress = etFoundFinderEmail.text.toString()
            foundPetBehavior =
                rgFoundBehavior.findViewById<RadioButton>(rgFoundBehavior.checkedRadioButtonId)?.text.toString()
            foundPetType =
                rgFoundType.findViewById<RadioButton>(rgFoundType.checkedRadioButtonId)?.text.toString()
            foundPetAdditionalPetInfo = etFoundPetAdditionalInfo.text.toString()
            foundPetAdditionalFinderInfo = etFoundFinderAdditionalInfo.text.toString()
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

    private fun validateAndUpload() {
        val location = viewModel.foundPetData.foundPetLocation
        if (!validateFields() || imageUri == null || location == null) {
            Log.d("uploadtest", "validateAndUpload: "+imageUri.toString()+location.toString())
            Toast.makeText(
                context,
                "Wypełnij pola, wybierz zdjęcie i lokalizację",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        binding.buttonFoundAccept.isEnabled = false
        val data = with(binding) {
            FoundPetData(
                foundPetType = rgFoundType.findViewById<RadioButton>(rgFoundType.checkedRadioButtonId)?.text.toString(),
                foundPetDate = etFoundPetDate.text.toString(),
                foundPetDecodedAddress = viewModel.foundPetData.foundPetDecodedAddress ?: "",
                foundPetFinderName = etFoundFinderName.text.toString(),
                foundPetPhoneNumber = etFoundFinderNumber.text.toString(),
                foundPetEmailAddress = etFoundFinderEmail.text.toString(),
                foundPetBehavior = rgFoundBehavior.findViewById<RadioButton>(rgFoundBehavior.checkedRadioButtonId)?.text.toString(),
                foundPetAdditionalPetInfo = etFoundPetAdditionalInfo.text.toString(),
                foundPetAdditionalFinderInfo = etFoundFinderAdditionalInfo.text.toString(),
                foundPetLocation = location,
                foundPetDateAdded = getCurrentDateTime()
            )
        }
        viewModel.uploadFoundPet(data, imageUri!!)
    }

    private fun getCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun validateFields(): Boolean = with(binding){
        return  etFoundPetDate.text.isNotEmpty() &&
                etFoundAddress.text.isNotEmpty() &&
                etFoundFinderName.text.isNotEmpty() &&
                etFoundFinderNumber.text.isNotEmpty() &&
                etFoundFinderEmail.text.isNotEmpty() &&
                rgFoundType.checkedRadioButtonId != -1 &&
                rgFoundBehavior.checkedRadioButtonId != -1
    }

    companion object {
        private const val TAG = "FoundCreateFragment"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}