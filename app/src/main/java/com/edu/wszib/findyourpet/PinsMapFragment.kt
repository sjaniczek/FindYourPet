package com.edu.wszib.findyourpet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.edu.wszib.findyourpet.databinding.FragmentPinsMapBinding
import com.edu.wszib.findyourpet.models.FoundPetData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class PinsMapFragment : Fragment(), OnMapReadyCallback, GoogleMap.InfoWindowAdapter {

    private lateinit var googleMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var database: FirebaseDatabase
    private var _binding: FragmentPinsMapBinding? = null
    private val binding get() = _binding!!


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        Log.i(TAG,"insideOnMapReady")
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.setAllGesturesEnabled(true)

        googleMap.animateCamera(CameraUpdateFactory.zoomTo(5.0F))
        val databaseUrl =
            "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        database = Firebase.database(databaseUrl)

        val databaseRef = database.reference.child("found_pets")
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i(TAG,"onDataChange")
                for (dataSnapshot in snapshot.children) {
                    val foundPetData = dataSnapshot.getValue(FoundPetData::class.java)
                    val locationData = foundPetData?.foundPetLocation
                    if (locationData != null) {
                        val latitude = locationData.latitude
                        val longitude = locationData.longitude
                        val markerOptions = MarkerOptions()
                            .position(LatLng(latitude, longitude))
                            .title(foundPetData.foundPetFinderName)
                            .snippet(foundPetData.foundPetDate)


                        val marker = googleMap.addMarker(markerOptions)
                        marker?.tag = foundPetData
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG,"onCancel")
            }
        })

        googleMap.setInfoWindowAdapter(this)
        googleMap.setOnInfoWindowClickListener { marker ->
            // Retrieve adData from marker's tag
            val foundPetData = marker.tag as FoundPetData?
            // Perform any necessary actions or navigation here
            // Example: Navigating to AdDetailsFragment with adData
            Log.d(TAG,"onMapReadyclick")

            Log.d(TAG,"onMapReadyclickBUTTON")

            foundPetData?.let {
//                val args = bundleOf(FoundDetailsFragment.EXTRA_POST_KEY to it.adId)
//                findNavController().navigate(R.id.action_mainFragment_to_adDetailsFragment, args)
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPinsMapBinding.inflate(inflater, container, false)
        Log.i(TAG,"onCreateView")
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        Log.i(TAG,"onViewCreated")
        fetchLocationDataFromDatabase()
    }

    override fun getInfoContents(marker: Marker): View? {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.custom_info_window, null)
        Log.d(TAG,"getInfoContents")

        // Retrieve adData from marker's tag
        val foundPetData = marker.tag as FoundPetData?
        val finderNameTextView = view.findViewById<TextView>(R.id.petNameTextView)
        val foundDateTextView = view.findViewById<TextView>(R.id.lostDateTextView)
        finderNameTextView.text = foundPetData?.foundPetFinderName
        foundDateTextView.text = foundPetData?.foundPetDate

        return view
    }

    override fun getInfoWindow(p0: Marker): View? {
        return null
    }



    private fun fetchLocationDataFromDatabase() {
        val databaseUrl =
            "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        database = Firebase.database(databaseUrl)
        Log.i(TAG,"fetchLocationDataFromDatabase")
        val databaseRef = database.reference.child("found_pets")
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.i(TAG,"fetchLocationDataFromDatabase - onDataChange")
                val locationDataList = mutableListOf<FoundPetData.FoundLocation>()

                for (snapshot in dataSnapshot.children) {
                    val foundPetData = snapshot.getValue(FoundPetData::class.java)
                    foundPetData?.foundPetLocation?.let { locationData ->
                        locationDataList.add(locationData)

                    }
                }
                Log.d(TAG, locationDataList.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching location data: ${error.message}")
            }
        })
    }
    companion object {
        private const val TAG = "pinsMapFragment"
    }
}