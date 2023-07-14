package com.edu.wszib.findyourpet.models

import android.net.Uri
import com.google.firebase.database.Exclude

data class FoundPetData(
    var foundPetId: String? = "",
    var foundPetType: String? = "",
    var foundPetDate: String? = "",
    var foundPetFinderName: String? = "",
    var foundPetPhoneNumber: String? = "",
    var foundPetEmailAddress: String? = "",
    var foundPetDecodedAddress: String? = "",
    var foundPetBehavior: String? = "",
    var foundPetAdditionalPetInfo: String? = "",
    var foundPetAdditionalFinderInfo: String? = "",
    var foundPetDateAdded: String? = "",
    var foundPetImageUrl: String? = "",
    var foundPetImageUri: Uri? = null,
    var foundPetLocation: FoundLocation? = null
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "foundPetId" to foundPetId,
            "foundPetType" to foundPetType,
            "foundPetDate" to foundPetDate,
            "foundPetFinderName" to foundPetFinderName,
            "foundPetPhoneNumber" to foundPetPhoneNumber,
            "foundPetEmailAddress" to foundPetEmailAddress,
            "foundPetBehavior" to foundPetBehavior,
            "foundPetDecodedAddress" to foundPetDecodedAddress,
            "foundPetAdditionalPetInfo" to foundPetAdditionalPetInfo,
            "foundPetAdditionalFinderInfo" to foundPetAdditionalFinderInfo,
            "foundPetDateAdded" to foundPetDateAdded,
            "foundPetImageUrl" to foundPetImageUrl,
            "foundPetLocation" to foundPetLocation
        )
    }

    data class FoundLocation(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    ) {
        constructor() : this(0.0, 0.0)
    }
}