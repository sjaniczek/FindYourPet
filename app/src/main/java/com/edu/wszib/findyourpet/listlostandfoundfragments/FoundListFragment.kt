package com.edu.wszib.findyourpet.listlostandfoundfragments

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edu.wszib.findyourpet.FoundPetListAdapter
import com.edu.wszib.findyourpet.R
import com.edu.wszib.findyourpet.models.FoundPetData
import com.edu.wszib.findyourpet.ui.foundfragments.FoundDetailsFragment
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

abstract class FoundListFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var recycler: RecyclerView
    private lateinit var manager: LinearLayoutManager
    private lateinit var adapter: FoundPetListAdapter
    private var referenceLocation: LatLng? = null

    private val list = mutableListOf<FoundPetData>()

    open fun getDatabaseReference(): DatabaseReference {
        return database.child("found_pets")
    }

    val uid: String
        get() = Firebase.auth.currentUser!!.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recycler_found, container, false)
        recycler = rootView.findViewById(R.id.recyclerViewFound)
        recycler.setHasFixedSize(true)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manager = LinearLayoutManager(activity)

        // ⬇⬇⬇ NAJWAŻNIEJSZA ZMIANA — WYŁĄCZ reverseLayout !
        manager.reverseLayout = false
        manager.stackFromEnd = false
        // ⬆⬆⬆
        val etSearch = view.findViewById<EditText>(R.id.etSearchAddress)
        val btnSearch = view.findViewById<ImageButton>(R.id.btnSearchAddress)
        val btnLocate = view.findViewById<ImageButton>(R.id.btnGetLocation)
        val btnReset = view.findViewById<ImageButton>(R.id.btnResetSearch)
        recycler.layoutManager = manager

        adapter = FoundPetListAdapter(list) { pet ->
            val args = bundleOf(FoundDetailsFragment.EXTRA_POST_KEY to pet.foundPetId)
            findNavController().navigate(R.id.foundDetailsFragment, args)
        }
        recycler.adapter = adapter

        val databaseUrl = "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        database = Firebase.database(databaseUrl).reference



        btnSearch.setOnClickListener {
            val address = etSearch.text.toString()
            if (address.isNotEmpty()) {
                setReferenceLocationFromAddress(address)
            } else {
                Toast.makeText(context, "Wpisz adres", Toast.LENGTH_SHORT).show()
            }
        }
        btnLocate.setOnClickListener {
            getUserLocation()
        }
        btnReset.setOnClickListener {
            resetSearch(etSearch)
        }
        loadFoundPets()
    }
    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        val fused = LocationServices.getFusedLocationProviderClient(requireActivity())

        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                referenceLocation = LatLng(loc.latitude, loc.longitude)
                //etSearch.setText("Moja lokalizacja")
                sortListByDistance()
                recycler.scrollToPosition(0)
            } else {
                Toast.makeText(context, "Nie udało się pobrać lokalizacji", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun resetSearch(etSearch: EditText) {
        referenceLocation = null
        etSearch.text.clear()
        loadFoundPets()              // wczytaj oryginalną kolejność
        recycler.smoothScrollToPosition(0) // przewiń na górę
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
            Toast.makeText(context, "Błąd przy geokodowaniu: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun sortListByDistance() {
        val ref = referenceLocation ?: return

        list.sortBy { pet ->
            pet.foundPetLocation?.let {
                val dx = it.latitude - ref.latitude
                val dy = it.longitude - ref.longitude
                dx * dx + dy * dy
            } ?: Double.MAX_VALUE
        }
        adapter.notifyDataSetChanged()
        recycler.smoothScrollToPosition(0)
    }

    private fun loadFoundPets() {
        val query = getDatabaseReference()
        Log.d("FoundListFragment", "Query path: ${query.ref.key}")

        query.get().addOnSuccessListener { snapshot ->
            list.clear()
            for (child in snapshot.children) {
                val pet = child.getValue(FoundPetData::class.java)
                pet?.foundPetId = child.key
                if (pet != null) list.add(pet)
            }

            // 🔥 Sortowanie domyślne po dacie dodania
            list.sortByDescending { it.foundPetDateAdded }

            val textEmpty = view?.findViewById<TextView>(R.id.tvFoundPetRecyclerEmpty)
            textEmpty?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

            adapter.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            Log.e("FoundListFragment", "Failed to load pets", e)
        }
    }
}
