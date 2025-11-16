package com.edu.wszib.findyourpet.models

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.wszib.findyourpet.repository.LostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LostPetViewModel(private val repository: LostRepository) : ViewModel() {

    // ---- STATE FLOWS ----
    private val _uploadState = MutableStateFlow<Result<Unit>?>(null)
    val uploadState: StateFlow<Result<Unit>?> = _uploadState

    private val _deleteState = MutableStateFlow<Result<Unit>?>(null)
    val deleteState: StateFlow<Result<Unit>?> = _deleteState

    private val _reportState = MutableStateFlow<Result<Unit>?>(null)
    val reportState: StateFlow<Result<Unit>?> = _reportState

    private val _editData = MutableLiveData<LostPetData?>()
    val editData: LiveData<LostPetData?> = _editData

    // ---- FORM DATA ----
    var lostPetData: LostPetData = LostPetData()
    var imageUri: Uri? = null

    // ---- UPLOAD / UPDATE ----
    fun uploadLostPet(data: LostPetData, imageUri: Uri) {
        viewModelScope.launch {
            _uploadState.value = repository.uploadLostPet(data, imageUri)
        }
    }

    fun updateLostPet(lostPetId: String, data: LostPetData, newImageUri: Uri?) {
        viewModelScope.launch {
            _uploadState.value = repository.updateLostPet(lostPetId, data, newImageUri)

            // Refresh data after successful update
            repository.getLostPetOnce(lostPetId) { latest ->
                latest?.let {
                    lostPetData = it.copy()
                    _editData.postValue(it)
                }
            }
        }
    }

    // ---- LOAD ----
    fun loadLostPet(lostPetId: String) {
        repository.getLostPetOnce(lostPetId) { data ->
            if (data != null) {
                _editData.value = data
                lostPetData = data.copy()
            } else {
                _editData.value = null
            }
        }
    }

    // ---- DELETE ----
    fun deleteLostPet(lostPetId: String) {
        viewModelScope.launch {
            _deleteState.value = repository.deleteLostPet(lostPetId)
        }
    }

    // ---- REPORT ----
    fun sendReport(lostPetId: String, message: String) {
        val userId = repository.getCurrentUserId() ?: return

        viewModelScope.launch {
            _reportState.value = repository.sendReport(lostPetId, message, userId)
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
        lostPetData = LostPetData()
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
