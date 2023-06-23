package com.edu.wszib.findyourpet.models

import android.net.Uri
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class LostPetData(
    var adId: String? = "",
    var petName: String? = "",
    var petType: String? = "",
    var petAge: String? = "",
    var lostDate: String? = "",
    var ownerName: String? = "",
    var phoneNumber: String? = "",
    var emailAddress: String? = "",
    var decodedAddress: String? = "",
    var petBehavior: String? = "",
    var petReact: String? = "",
    var additionalPetInfo: String? = "",
    var additionalOwnerInfo: String? = "",
    var dateAdded: String? = "",
    var imageUrl: String? = "",
    var imageUri: Uri? = null,
    var lostLocation: LostLocation? = null
){

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "adId" to adId,
            "petName" to petName,
            "petType" to petType,
            "petAge" to petAge,
            "lostDate" to lostDate,
            "ownerName" to ownerName,
            "phoneNumber" to phoneNumber,
            "emailAddress" to emailAddress,
            "decodedAddress" to decodedAddress,
            "petBehavior" to petBehavior,
            "petReact" to petReact,
            "additionalPetInfo" to additionalPetInfo,
            "additionalOwnerInfo" to additionalOwnerInfo,
            "dateAdded" to dateAdded,
            "imageUrl" to imageUrl,
            "lostLocation" to lostLocation
        )
    }

    //    fun getImageUriObject(): Uri? {
//        return imageUri?.let {
//            Uri.parse(it)
//        }
//    }
    data class LostLocation(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    ) {
        constructor() : this(0.0, 0.0)
    }
}