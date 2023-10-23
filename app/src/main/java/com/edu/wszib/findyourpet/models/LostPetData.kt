package com.edu.wszib.findyourpet.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class LostPetData(
    var lostPetOwnerId: String? = "",
    var lostPetId: String? = "",
    var lostPetName: String? = "",
    var lostPetType: String? = "",
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
    var lostPetLocation: LostLocation? = null
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "lostPetOwnerId" to lostPetOwnerId,
            "lostPetId" to lostPetId,
            "lostPetName" to lostPetName,
            "lostPetType" to lostPetType,
            "lostPetDate" to lostPetDate,
            "lostPetHour" to lostPetHour,
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
        constructor(latLng: LatLng) : this(latLng.latitude, latLng.longitude)
    }
}
