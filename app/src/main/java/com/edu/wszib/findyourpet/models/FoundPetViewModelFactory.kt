package com.edu.wszib.findyourpet.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.edu.wszib.findyourpet.repository.FoundRepository

// Factory to create FoundPetViewModel with repository injection
class FoundPetViewModelFactory(
    private val repository: FoundRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FoundPetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FoundPetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
