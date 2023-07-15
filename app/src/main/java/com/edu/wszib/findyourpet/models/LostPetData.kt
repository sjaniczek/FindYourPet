package com.edu.wszib.findyourpet.models

import android.net.Uri
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class LostPetData(
    var lostPetId: String? = "",
    var lostPetName: String? = "",
    var lostPetType: String? = "",
    var lostPetAge: String? = "",
    var lostPetDate: String? = "",
    var lostPetHour: String? = "",
    var lostPetOwnerName: String? = "",
    var lostPetPhoneNumber: String? = "",
    var lostPetEmailAddress: String? = "",
    var lostPetDecodedAddress: String? = "",
    var lostPetBehavior: String? = "",
    var lostPetReact: String? = "",
    var lostPetAdditionalPetInfo: String? = "",
    var lostPetAdditionalOwnerInfo: String? = "",
    var lostPetDateAdded: String? = "",
    var lostPetImageUrl: String? = "",
    var lostPetImageUri: Uri? = null,
    var lostPetLocation: LostLocation? = null
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "lostPetId" to lostPetId,
            "lostPetName" to lostPetName,
            "lostPetType" to lostPetType,
            "lostPetAge" to lostPetAge,
            "lostPetDate" to lostPetDate,
            "lostPetOwnerName" to lostPetOwnerName,
            "lostPetPhoneNumber" to lostPetPhoneNumber,
            "lostPetEmailAddress" to lostPetEmailAddress,
            "lostPetDecodedAddress" to lostPetDecodedAddress,
            "lostPetBehavior" to lostPetBehavior,
            "lostPetReact" to lostPetReact,
            "lostPetAdditionalPetInfo" to lostPetAdditionalPetInfo,
            "lostPetAdditionalOwnerInfo" to lostPetAdditionalOwnerInfo,
            "lostPetDateAdded" to lostPetDateAdded,
            "lostPetImageUrl" to lostPetImageUrl,
            "lostPetLocation" to lostPetLocation
        )
    }

    data class LostLocation(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    ) {
        constructor() : this(0.0, 0.0)
    }
}
