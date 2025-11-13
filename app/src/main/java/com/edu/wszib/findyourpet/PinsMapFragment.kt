package com.edu.wszib.findyourpet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentPinsMapBinding
import com.edu.wszib.findyourpet.foundfragments.FoundDetailsFragment
import com.edu.wszib.findyourpet.lostfragments.LostDetailsFragment
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
import java.util.Locale

class PinsMapFragment : Fragment(), OnMapReadyCallback, GoogleMap.InfoWindowAdapter {

    private lateinit var googleMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var database: FirebaseDatabase
    private var _binding: FragmentPinsMapBinding? = null
    private val binding get() = _binding!!
    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.uiSettings.setAllGesturesEnabled(true)
        val polandCenter = LatLng(52.051373, 19.090859)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(polandCenter, 6f))
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

            if (petData is LostPetData) {
                petData.let {
                    val args = bundleOf(LostDetailsFragment.EXTRA_POST_KEY to it.lostPetId)
                    findNavController().navigate(
                        R.id.action_mainFragment_to_lostDetailsFragment,
                        args
                    )
                }
            }
            if (petData is FoundPetData) {
                petData.let {
                    val args = bundleOf(FoundDetailsFragment.EXTRA_POST_KEY to it.foundPetId)
                    findNavController().navigate(
                        R.id.action_mainFragment_to_foundDetailsFragment,
                        args
                    )
                }
            }
        }
    }
    private fun showSearchDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Wpisz adres lub miasto"
            setPadding(50, 40, 50, 40)
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            setHintTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Wyszukaj lokalizację")
            .setView(editText)
            .setPositiveButton("Szukaj") { _, _ ->
                val address = editText.text.toString()
                if (address.isNotEmpty()) {
                    searchLocation(address)
                } else {
                    Toast.makeText(requireContext(), "Podaj adres", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .create()

        dialog.show()
    }
    private fun searchLocation(query: String) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) {
                val location = LatLng(addresses[0].latitude, addresses[0].longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 14f))
            } else {
                Toast.makeText(requireContext(), "Nie znaleziono lokalizacji", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPinsMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        binding.buttonSearchAddress.setOnClickListener {
            showSearchDialog()
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun getInfoContents(marker: Marker): View? {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.custom_info_window, null)

        // Retrieve adData from marker's tag
        val petData = marker.tag

        if (petData is FoundPetData) {

            val foundImageView = view.findViewById<ImageView>(R.id.ivPinsImage)
            val foundDateTextView = view.findViewById<TextView>(R.id.tvPinsDate)
            Log.i(TAG, petData.toString())
            foundDateTextView.text = petData.foundPetDate
            val imageUrl = petData.foundPetImageUrl

            Picasso.get()
                .load(imageUrl)
                .resize(140, 140)
                .into(foundImageView, object : Callback {
                    override fun onSuccess() {
                        if (marker.isInfoWindowShown) {
                            marker.hideInfoWindow()
                            marker.showInfoWindow()
                        }
                    }

                    override fun onError(e: Exception?) {
                        Toast.makeText(requireContext(), "Wystąpił błąd podczas ładowania zdjęcia",
                            Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Błąd ładowania zdjęcia: ${e?.message}")
                    }
                })
        } else if (petData is LostPetData) {
            // Handle LostPetData
            val petNameTextView = view.findViewById<TextView>(R.id.tvPinsName)
            val lostImageView = view.findViewById<ImageView>(R.id.ivPinsImage)
            val lostDateTextView = view.findViewById<TextView>(R.id.tvPinsDate)
            Log.i(TAG, petData.toString())
            petNameTextView.text = petData.lostPetName
            lostDateTextView.text = petData.lostPetDate
            val imageUrl = petData.lostPetImageUrl

            Picasso.get()
                .load(imageUrl)
                .resize(140, 140)
                .into(lostImageView, object : Callback {
                    override fun onSuccess() {
                        Log.i(TAG, "onsuccess")
                        if (marker.isInfoWindowShown) {
                            marker.hideInfoWindow()
                            marker.showInfoWindow()
                        }
                    }

                    override fun onError(e: Exception?) {
                        return
                    }
                })
        }

        return view
    }

    override fun getInfoWindow(p0: Marker): View? {
        return null
    }

    companion object {
        private const val TAG = "pinsMapFragment"
    }
}