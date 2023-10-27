package com.edu.wszib.findyourpet.models

import android.net.Uri
import androidx.lifecycle.ViewModel

class LostPetViewModel : ViewModel() {

    var lostPetData: LostPetData? = null
    var imageUri: Uri? = null
    fun saveFormData(lostPetData: LostPetData, imageUri: Uri?) {
        this.lostPetData = lostPetData
        this.imageUri = imageUri
    }
}