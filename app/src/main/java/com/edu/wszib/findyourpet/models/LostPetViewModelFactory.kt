package com.edu.wszib.findyourpet.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.edu.wszib.findyourpet.repository.LostRepository

class LostPetViewModelFactory(
    private val repository: LostRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LostPetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LostPetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}