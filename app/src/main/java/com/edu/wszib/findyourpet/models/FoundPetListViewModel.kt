package com.edu.wszib.findyourpet.models

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.edu.wszib.findyourpet.repository.FoundRepository
import com.google.android.gms.maps.model.LatLng

class FoundPetListViewModel(private val repository: FoundRepository) : ViewModel() {

    private val _allPets = MutableLiveData<List<FoundPetData>>()
    val allPets: LiveData<List<FoundPetData>> = _allPets

    private var referenceLocation: LatLng? = null

    fun setReferenceLocation(location: LatLng) {
        referenceLocation = location
        loadAllFoundPets()
    }

    fun loadAllFoundPets() {
        repository.getAllFoundPets { list ->
            val sorted = list.sortedBy { pet ->
                referenceLocation?.let { ref ->
                    distanceInMeters(ref, pet.foundPetLocation)
                } ?: Double.MAX_VALUE
            }
            _allPets.postValue(sorted)
        }
    }

    private fun distanceInMeters(from: LatLng, to: FoundPetData.FoundLocation?): Double {
        if (to == null) return Double.MAX_VALUE
        val results = FloatArray(1)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
        return results[0].toDouble()
    }
}
