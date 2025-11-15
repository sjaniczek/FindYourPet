package com.edu.wszib.findyourpet.models

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.wszib.findyourpet.repository.FoundRepository
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FoundPetViewModel(private val repository: FoundRepository) : ViewModel() {

    private val _uploadState = MutableStateFlow<Result<Unit>?>(null)
    val uploadState: StateFlow<Result<Unit>?> = _uploadState

    private val _editData = MutableLiveData<FoundPetData?>()
    val editData: LiveData<FoundPetData?> = _editData

    var foundPetData: FoundPetData = FoundPetData()
    var imageUri: Uri? = null

    fun uploadFoundPet(data: FoundPetData, imageUri: Uri) {
        viewModelScope.launch {
            _uploadState.value = repository.uploadFoundPet(data, imageUri)
        }
    }

    // Standardowe pobranie danych
    fun loadFoundPet(foundPetId: String) {
        repository.getFoundPetOnce(foundPetId) { data ->
            if (data != null) {
                _editData.value = data
                foundPetData = data.copy()
            } else {
                _editData.value = null
            }
        }
    }

    // Update danych + pobranie najnowszych po udanym update
    fun updateFoundPet(foundPetId: String, data: FoundPetData, newImageUri: Uri?) {
        viewModelScope.launch {
            _uploadState.value = repository.updateFoundPet(foundPetId, data, newImageUri)
            // Po udanym update wymuś pobranie świeżych danych
            repository.getFoundPetOnce(foundPetId) { latest ->
                latest?.let {
                    foundPetData = it.copy()
                    _editData.postValue(it)
                }
            }
        }
    }

    fun saveFormData(data: FoundPetData, imageUri: Uri?) {
        foundPetData = data
        this.imageUri = imageUri
    }

    fun resetUploadState() {
        _uploadState.value = null
    }

    fun clearData() {
        foundPetData = FoundPetData()
        imageUri = null
    }
}
