package com.edu.wszib.findyourpet

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.edu.wszib.findyourpet.databinding.FragmentPinsMapBinding
import com.edu.wszib.findyourpet.models.FoundPetData
import com.edu.wszib.findyourpet.models.LostPetData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception
import javax.sql.DataSource

class PinsMapFragment : Fragment(), OnMapReadyCallback, GoogleMap.InfoWindowAdapter {

    private lateinit var googleMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var database: FirebaseDatabase
    private var _binding: FragmentPinsMapBinding? = null
    private val binding get() = _binding!!
    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.setAllGesturesEnabled(true)

        googleMap.animateCamera(CameraUpdateFactory.zoomTo(5.0F))
        val databaseUrl =
            "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        database = Firebase.database(databaseUrl)

        val foundPetsRef = database.reference.child("found_pets")
        foundPetsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
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
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

                        val marker = googleMap.addMarker(markerOptions)
                        marker?.tag = foundPetData
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching found pets data: ${error.message}")
            }
        })

        val lostPetsRef = database.reference.child("lost_pets")
        lostPetsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {
                    val lostPetData = dataSnapshot.getValue(LostPetData::class.java)
                    val locationData = lostPetData?.lostPetLocation
                    if (locationData != null) {
                        val latitude = locationData.latitude
                        val longitude = locationData.longitude
                        val markerOptions = MarkerOptions()
                            .position(LatLng(latitude, longitude))
                            .title(lostPetData.lostPetName)
                            .snippet(lostPetData.lostPetDate)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

                        val marker = googleMap.addMarker(markerOptions)
                        marker?.tag = lostPetData
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching lost pets data: ${error.message}")
            }
        })

        googleMap.setInfoWindowAdapter(this)
        googleMap.setOnInfoWindowClickListener { marker ->
            // Retrieve adData from marker's tag
            val petData = marker.tag
            // Perform any necessary actions or navigation here
            // Example: Navigating to AdDetailsFragment with adData

            if (petData is LostPetData) {
                petData.let { val args = bundleOf(LostDetailsFragment.EXTRA_POST_KEY to it.lostPetId)
                findNavController().navigate(R.id.action_mainFragment_to_lostDetailsFragment, args) }
            }
            if (petData is FoundPetData) {
                petData.let { val args = bundleOf(FoundDetailsFragment.EXTRA_POST_KEY to it.foundPetId)
                    findNavController().navigate(R.id.action_mainFragment_to_foundDetailsFragment, args) }
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
        //fetchLocationDataFromDatabase()
        Picasso.get().isLoggingEnabled = true
    }
    @SuppressLint("MissingInflatedId")
    override fun getInfoContents(marker: Marker): View? {
        Log.i(TAG,"getInfoSTART")
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.custom_info_window, null)

        // Retrieve adData from marker's tag
        val petData = marker.tag

        if (petData is FoundPetData) {
            val imageUrl = petData.foundPetImageUrl
            val foundDateTextView = view.findViewById<TextView>(R.id.tvPinsDate)
            val foundImageView = view.findViewById<ImageView>(R.id.ivPinsImage)

            foundDateTextView.text = petData.foundPetDate

            Picasso.get()
                .load(imageUrl)
                .resize(200,200)
                .into(foundImageView, object : Callback {
                    override fun onSuccess() {
                        Log.i(TAG, "onsuccess")
                        if (marker.isInfoWindowShown) {
                            marker.hideInfoWindow()
                            marker.showInfoWindow()
                        }
                    }

                    override fun onError(e: Exception?) {
                        TODO("Not yet implemented")
                    }
                })

        } else if (petData is LostPetData) {
            // Handle LostPetData
            val petNameTextView = view.findViewById<TextView>(R.id.tvPinsName)
            val lostImageView = view.findViewById<ImageView>(R.id.ivPinsImage)
            val lostDateTextView = view.findViewById<TextView>(R.id.tvPinsDate)
            Log.i(TAG,petData.toString())
            petNameTextView.text = petData.lostPetName
            val imageUrl = petData.lostPetImageUrl

            Picasso.get()
                .load(imageUrl)
                .resize(140,140)
                .into(lostImageView, object : Callback {
                    override fun onSuccess() {
                        Log.i(TAG, "onsuccess")
                        if (marker.isInfoWindowShown) {
                            marker.hideInfoWindow()
                            marker.showInfoWindow()
                        }
                    }

                    override fun onError(e: Exception?) {
                        TODO("Not yet implemented")
                    }
                })


                    lostDateTextView.text = petData.lostPetDate
        }

        Log.i(TAG,"getInfoEND")
        return view
    }

    override fun getInfoWindow(p0: Marker): View? {
        return null
    }



//    private fun fetchLocationDataFromDatabase() {
//        val databaseUrl =
//            "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
//        database = Firebase.database(databaseUrl)
//        Log.i(TAG,"fetchLocationDataFromDatabase")
//        val databaseRef = database.reference.child("found_pets")
//        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                Log.i(TAG,"fetchLocationDataFromDatabase - onDataChange")
//                val locationDataList = mutableListOf<FoundPetData.FoundLocation>()
//
//                for (snapshot in dataSnapshot.children) {
//                    val foundPetData = snapshot.getValue(FoundPetData::class.java)
//                    foundPetData?.foundPetLocation?.let { locationData ->
//                        locationDataList.add(locationData)
//
//                    }
//                }
//                Log.d(TAG, locationDataList.toString())
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e(TAG, "Error fetching location data: ${error.message}")
//            }
//        })
//    }
    companion object {
        private const val TAG = "pinsMapFragment"
    }
}