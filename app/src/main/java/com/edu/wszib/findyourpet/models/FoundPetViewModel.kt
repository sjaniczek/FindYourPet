package com.edu.wszib.findyourpet.models

import android.net.Uri
import androidx.lifecycle.ViewModel

class FoundPetViewModel : ViewModel() {

    var foundPetData: FoundPetData? = null
    var imageUri: Uri? = null

    fun saveFormData(foundPetData: FoundPetData, imageUri: Uri?) {
        this.foundPetData = foundPetData
        this.imageUri = imageUri
    }
}