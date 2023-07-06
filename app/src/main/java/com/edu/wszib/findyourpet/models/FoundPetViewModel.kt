package com.edu.wszib.findyourpet.models

import androidx.lifecycle.ViewModel

class FoundPetViewModel : ViewModel() {
    var foundPetData: FoundPetData? = null

    fun saveFormData(foundPetData: FoundPetData){
        this.foundPetData = foundPetData
    }
}