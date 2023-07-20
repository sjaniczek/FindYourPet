package com.edu.wszib.findyourpet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.edu.wszib.findyourpet.databinding.FragmentDetailsLostBinding
import com.edu.wszib.findyourpet.models.LostPetData
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import java.lang.IllegalArgumentException

class LostDetailsFragment : Fragment() {

    private lateinit var lostPetKey: String
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseRef: DatabaseReference
    private var lostPetListener: ValueEventListener? = null
    private var _binding: FragmentDetailsLostBinding? = null
    private val binding: FragmentDetailsLostBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDetailsLostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lostPetKey = requireArguments().getString(EXTRA_POST_KEY)
            ?: throw IllegalArgumentException("Must pass EXTRA_POST_KEY")

        // Initialize Database
        database = Firebase.database(databaseUrl)
        databaseRef = database.reference.child("lost_pets").child(lostPetKey)


    }
    override fun onStart() {
        super.onStart()

        // Add value event listener to the post
        val lostPetListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                val lostPetData = dataSnapshot.getValue<LostPetData>()
                lostPetData?.let {
                    with(binding){
                        tvLostDetailsPetName.text = lostPetData.lostPetName
                        if(lostPetData.lostPetImageUrl.isNullOrEmpty()){
                            Picasso.get()
                                .load(DEFAULT_IMAGE_URL)
                                .into(ivPetImage)
                        }
                        else {
                            Picasso.get()
                                .load(lostPetData.lostPetImageUrl)
                                .into(ivPetImage)
                        }
                        tvLostDetailsPetDecodedAddress.text = lostPetData.lostPetDecodedAddress
                        tvLostDetailsPetType.text = getString(R.string.details_pet_type, lostPetData.lostPetType)
                        tvLostDetailsPetDate.text = getString(R.string.details_pet_date, lostPetData.lostPetDate)
                        tvLostDetailsPetHour.text = getString(R.string.details_pet_hour, lostPetData.lostPetHour)
                        tvLostDetailsPetBehavior.text = getString(R.string.details_pet_behavior, lostPetData.lostPetBehavior)
                        tvLostDetailsPetReact.text = getString(R.string.details_pet_react, lostPetData.lostPetReact)
                        tvLostDetailsPetAdditionalInfo.text = getString(R.string.details_pet_additional, lostPetData.lostPetAdditionalPetInfo)
                        tvLostDetailsPetOwnerName.text = getString(R.string.details_pet_owner_name, lostPetData.lostPetOwnerName)
                        tvLostDetailsPetPhoneNumber.text = getString(R.string.details_pet_owner_number, lostPetData.lostPetPhoneNumber)
                        tvLostDetailsPetEmailAddress.text = getString(R.string.details_pet_owner_email, lostPetData.lostPetEmailAddress)
                        tvLostDetailsPetOwnerAdditionalInfo.text = getString(R.string.details_pet_owner_additional, lostPetData.lostPetAdditionalOwnerInfo)
                    }
                    binding.lostDetailsMapButton.setOnClickListener {
                        val longitude = lostPetData.lostPetLocation?.longitude
                        val latitude = lostPetData.lostPetLocation?.latitude
                        val uri = Uri.parse("geo:,$longitude?q=$latitude,$longitude")

                        // Start the map application
                        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                        mapIntent.setPackage("com.google.android.apps.maps") // This will ensure it opens in Google Maps
                        startActivity(mapIntent)
                    }
                    binding.lostPetPhoneButton.setOnClickListener{
                        val phoneNumber = lostPetData.lostPetPhoneNumber
                        val dialIntent = Intent(Intent.ACTION_DIAL)
                        dialIntent.data = Uri.parse("tel:$phoneNumber")
                        startActivity(dialIntent)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                Toast.makeText(context, "Failed to load post.",
                    Toast.LENGTH_SHORT).show()
            }
        }

        databaseRef.addValueEventListener(lostPetListener)

        // Keep copy of post listener so we can remove it when app stops
        this.lostPetListener = lostPetListener


    }

    override fun onStop() {
        super.onStop()

        // Remove post value event listener
        lostPetListener?.let {
            databaseRef.removeEventListener(it)
        }

        // Clean up comments listener
        //adapter?.cleanupListener()
    }
    companion object {
        private const val DEFAULT_IMAGE_URL = "https://i.stack.imgur.com/l60Hf.png"
        private const val databaseUrl =
            "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        private const val TAG = "LostDetailFragment"
        const val EXTRA_POST_KEY = "post_key"
    }
}