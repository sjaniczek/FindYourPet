package com.edu.wszib.findyourpet.listlostandfoundfragments

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edu.wszib.findyourpet.R
import com.edu.wszib.findyourpet.adapters.LostPetListAdapter
import com.edu.wszib.findyourpet.models.LostPetData
import com.edu.wszib.findyourpet.ui.lostfragments.LostDetailsFragment
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

abstract class LostListFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var recycler: RecyclerView
    private lateinit var manager: LinearLayoutManager
    private lateinit var adapter: LostPetListAdapter
    private var referenceLocation: LatLng? = null

    private val list = mutableListOf<LostPetData>()

    open fun getDatabaseReference(): DatabaseReference {
        return database.child("lost_pets")
    }

    val uid: String
        get() = Firebase.auth.currentUser!!.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recycler_lost, container, false)
        recycler = rootView.findViewById(R.id.recyclerViewLost)
        recycler.setHasFixedSize(true)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manager = LinearLayoutManager(activity)
        manager.reverseLayout = false
        manager.stackFromEnd = false
        recycler.layoutManager = manager

        val etSearch = view.findViewById<EditText>(R.id.etSearchAddress)
        val btnSearch = view.findViewById<ImageButton>(R.id.btnSearchAddress)
        val btnLocate = view.findViewById<ImageButton>(R.id.btnGetLocation)
        val btnReset = view.findViewById<ImageButton>(R.id.btnResetSearch)

        adapter = LostPetListAdapter(list) { pet ->
            val args = bundleOf(LostDetailsFragment.EXTRA_POST_KEY to pet.lostPetId)
            findNavController().navigate(R.id.lostDetailsFragment, args)
        }
        recycler.adapter = adapter

        val databaseUrl =
            "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        database = Firebase.database(databaseUrl).reference
        etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val address = etSearch.text.toString()
                if (address.isNotEmpty()) {
                    setReferenceLocationFromAddress(address)
                } else {
                    Toast.makeText(context, "Wpisz adres", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
        // obsługa przycisków
        btnSearch.setOnClickListener {
            val address = etSearch.text.toString()
            if (address.isNotEmpty()) {
                setReferenceLocationFromAddress(address)
            } else {
                Toast.makeText(context, "Wpisz adres", Toast.LENGTH_SHORT).show()
            }
        }

        btnLocate.setOnClickListener {
            requestUserLocationAndSort()
        }

        btnReset.setOnClickListener {
            resetSearch(etSearch)
        }

        loadLostPets()
    }

    private fun resetSearch(etSearch: EditText) {
        referenceLocation = null
        etSearch.text.clear()
        loadLostPets()
        recycler.smoothScrollToPosition(0)
    }

    private fun setReferenceLocationFromAddress(address: String) {
        val geocoder = Geocoder(requireContext())
        try {
            val addresses = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                val loc = addresses[0]
                referenceLocation = LatLng(loc.latitude, loc.longitude)
                sortListByDistance()
            } else {
                Toast.makeText(context, "Nie znaleziono lokalizacji", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Błąd przy geokodowaniu: ${e.message}", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }
    }

    private fun sortListByDistance() {
        val ref = referenceLocation ?: return

        list.sortBy { pet ->
            pet.lostPetLocation?.let {
                haversineDistance(ref, LatLng(it.latitude, it.longitude))
            } ?: Double.MAX_VALUE
        }
        adapter.notifyDataSetChanged()
        recycler.smoothScrollToPosition(0)
    }

    private fun haversineDistance(from: LatLng, to: LatLng): Double {
        val R = 6371000.0 // promień Ziemi w metrach
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(from.latitude)) *
                cos(Math.toRadians(to.latitude)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    @SuppressLint("MissingPermission")
    private fun requestUserLocationAndSort() {
        val fused = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                referenceLocation = LatLng(loc.latitude, loc.longitude)
                sortListByDistance()
            } else {
                Toast.makeText(context, "Nie udało się pobrać lokalizacji", Toast.LENGTH_SHORT)
                    .show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(
                context,
                "Błąd przy pobieraniu lokalizacji: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadLostPets() {
        val query = getDatabaseReference()
        Log.d("LostListFragment", "Query path: ${query.ref.key}")

        query.get().addOnSuccessListener { snapshot ->
            list.clear()
            for (child in snapshot.children) {
                val pet = child.getValue(LostPetData::class.java)
                pet?.lostPetId = child.key
                if (pet != null) list.add(pet)
            }

            // sortowanie domyślne po dacie dodania
            list.sortByDescending { it.lostPetDateAdded }

            val textEmpty = view?.findViewById<TextView>(R.id.tvLostPetRecyclerEmpty)
            textEmpty?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

            adapter.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            Log.e("LostListFragment", "Failed to load pets", e)
        }
    }
}