package com.edu.wszib.findyourpet.models

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.wszib.findyourpet.repository.FoundRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FoundPetViewModel(private val repository: FoundRepository) : ViewModel() {

    // ---- STATE FLOWS ----


    private val _deleteState = MutableStateFlow<Result<Unit>?>(null)
    val deleteState: StateFlow<Result<Unit>?> = _deleteState

    private val _reportState = MutableStateFlow<Result<Unit>?>(null)
    val reportState: StateFlow<Result<Unit>?> = _reportState

    // Current data for editing
    private val _editData = MutableLiveData<FoundPetData?>()
    val editData: LiveData<FoundPetData?> = _editData

    // Current form data (create/edit)
    var foundPetData: FoundPetData = FoundPetData()
    var imageUri: Uri? = null

    private val _uploadState = MutableStateFlow<Result<Unit>?>(null)
    val uploadState: StateFlow<Result<Unit>?> = _uploadState
    // ---- UPLOAD / UPDATE ----
    fun uploadFoundPet(data: FoundPetData, imageUri: Uri) {
        viewModelScope.launch {
            _uploadState.value = repository.uploadFoundPet(data, imageUri)
        }
    }

    fun updateFoundPet(foundPetId: String, data: FoundPetData, newImageUri: Uri?) {
        viewModelScope.launch {
            _uploadState.value = repository.updateFoundPet(foundPetId, data, newImageUri)

            // Refresh data after update
            repository.getFoundPetOnce(foundPetId) { latest ->
                latest?.let {
                    foundPetData = it.copy()
                    _editData.postValue(it)
                }
            }
        }
    }

    // ---- LOAD ----
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

    // ---- DELETE ----
    fun deleteFoundPet(foundPetId: String) {
        viewModelScope.launch {
            _deleteState.value = repository.deleteFoundPet(foundPetId)
        }
    }

    // ---- REPORT ----
    fun sendReport(foundPetId: String, message: String) {
        val userId = repository.getCurrentUserId() ?: return

        viewModelScope.launch {
            _reportState.value = repository.sendReport(foundPetId, message, userId)
        }
    }

    // ---- RESET FUNCTIONS ----
    fun resetUploadState() {
        _uploadState.value = null
    }

    fun resetDeleteState() {
        _deleteState.value = null
    }

    fun resetReportState() {
        _reportState.value = null
    }

    fun resetEditData() {
        _editData.value = null
    }

    fun clearData() {
        foundPetData = FoundPetData()
        imageUri = null
    }

    fun resetAllStates() {
        resetUploadState()
        resetDeleteState()
        resetReportState()
    }

    // ---- AUTH ----
    fun getCurrentUserId(): String? = repository.getCurrentUserId()
}
