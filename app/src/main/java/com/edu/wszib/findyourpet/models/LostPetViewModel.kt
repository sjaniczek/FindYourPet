package com.edu.wszib.findyourpet.models

import androidx.lifecycle.ViewModel

class LostPetViewModel : ViewModel() {
    var lostPetData: LostPetData? = null

    fun saveFormData(lostPetData: LostPetData){
        this.lostPetData = lostPetData
    }
}