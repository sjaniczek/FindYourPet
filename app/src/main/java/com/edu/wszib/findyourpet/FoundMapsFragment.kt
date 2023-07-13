package com.edu.wszib.findyourpet

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.databinding.FragmentFoundMapsBinding
import com.edu.wszib.findyourpet.models.FoundPetData
import com.edu.wszib.findyourpet.models.FoundPetViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import java.util.*

class FoundMapsFragment : Fragment(), OnMapReadyCallback {
    private val foundPetViewModel: FoundPetViewModel by activityViewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private var _binding: FragmentFoundMapsBinding? = null
    private val binding get() = _binding!!

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            // Permission denied, show a message or do something else
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFoundMapsBinding.inflate(inflater, container, false)

        val foundPetViewModel = foundPetViewModel.foundPetData
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_found_layout) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.setAllGesturesEnabled(true)
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(5.0F))

        getCurrentLocation()
        binding.buttonLostMapSend.setOnClickListener {
            val currentLocation = googleMap.cameraPosition.target
            decodeLocation(currentLocation)
        }
    }

    private fun decodeLocation(currentLocation: LatLng) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val addresses = geocoder.getFromLocation(
                currentLocation.latitude,
                currentLocation.longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val decodedAddress = addresses[0].getAddressLine(0)
                        foundPetViewModel.foundPetData?.foundPetDecodedAddress = decodedAddress
                        foundPetViewModel.foundPetData?.foundPetLocation = FoundPetData.FoundLocation(
                            currentLocation.latitude,
                            currentLocation.longitude
                        )
                        activity?.runOnUiThread {
                            Toast.makeText(
                                context,
                                foundPetViewModel.foundPetData?.foundPetDecodedAddress.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        findNavController().navigate(FoundMapsFragmentDirections.actionFoundMapsFragmentToFoundCreateFragment())
                    }

                    override fun onError(errorMessage: String?) {
                        super.onError(errorMessage)
                        Toast.makeText(
                            context,
                            "Błąd w pobieraniu adresu. Spróbuj ponownie później lub wpisz adres ręcznie.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                })
        } else {
            @Suppress("DEPRECATION")
            val addresses =
                geocoder.getFromLocation(currentLocation.latitude, currentLocation.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                // Process the retrieved addresses
                val decodedAddress = addresses[0].getAddressLine(0)
                foundPetViewModel.foundPetData?.foundPetDecodedAddress = decodedAddress
                foundPetViewModel.foundPetData?.foundPetLocation =
                    FoundPetData.FoundLocation(currentLocation.latitude, currentLocation.longitude)

                findNavController().navigate(FoundMapsFragmentDirections.actionFoundMapsFragmentToFoundCreateFragment())
            } else {
                Toast.makeText(
                    context,
                    "Błąd w pobieraniu adresu. Spróbuj ponownie później lub wpisz adres ręcznie.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getCurrentLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                object : CancellationToken() {
                    override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                        CancellationTokenSource().token

                    override fun isCancellationRequested() = false
                }).addOnSuccessListener { location: Location? ->
                Log.i("userloc", location.toString())
                if (location == null)
                    Toast.makeText(context, "Cannot get location.", Toast.LENGTH_SHORT).show()
                else {
                    val lat = location.latitude
                    val lng = location.longitude
                    val latLng = LatLng(lat, lng)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(6F))
                }
            }
        }//WHAT IF DENIED?
    }
}