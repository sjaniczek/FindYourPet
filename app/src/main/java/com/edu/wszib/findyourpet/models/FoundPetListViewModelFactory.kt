package com.edu.wszib.findyourpet.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.edu.wszib.findyourpet.repository.FoundRepository

class FoundPetListViewModelFactory(private val repository: FoundRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FoundPetListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FoundPetListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}