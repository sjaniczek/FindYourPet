package com.edu.wszib.findyourpet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.edu.wszib.findyourpet.databinding.FragmentDetailsLostBinding
import com.edu.wszib.findyourpet.databinding.FragmentFoundDetailsBinding
import com.edu.wszib.findyourpet.models.FoundPetData
import com.edu.wszib.findyourpet.models.LostPetData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import java.lang.IllegalArgumentException


class FoundDetailsFragment : Fragment() {
        private lateinit var editMenuItem: MenuItem
        private lateinit var deleteMenuItem: MenuItem
        private lateinit var auth: FirebaseAuth
        private lateinit var foundPetKey: String
        private lateinit var database: FirebaseDatabase
        private lateinit var databaseRef: DatabaseReference
        private var foundPetListener: ValueEventListener? = null
        private var _binding: FragmentFoundDetailsBinding? = null
        private val binding: FragmentFoundDetailsBinding
            get() = _binding!!


        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            _binding = FragmentFoundDetailsBinding.inflate(inflater, container, false)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            foundPetKey = requireArguments().getString(EXTRA_POST_KEY)
                ?: throw IllegalArgumentException("Must pass EXTRA_POST_KEY")

            // Initialize Firebase auth
            auth = Firebase.auth

            // Initialize Database
            database = Firebase.database(databaseUrl)
            databaseRef = database.reference.child("found_pets").child(foundPetKey)
            activity?.title = "Details";

            val menuHost: MenuHost = requireActivity()
            menuHost.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    // Add menu items here
                    menuInflater.inflate(R.menu.menu_found_pet_details, menu)
                    editMenuItem = menu.findItem(R.id.action_edit_pet)
                    deleteMenuItem = menu.findItem(R.id.action_delete_pet)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    // Handle the menu selection
                    return when (menuItem.itemId) {
                        R.id.action_edit_pet -> {
                            // todo menu1
                            true
                        }
                        R.id.action_delete_pet -> {
                            // todo menu2
                            true
                        }
                        else -> false
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }

        override fun onStart() {
            super.onStart()

            // Add value event listener to the post
            val foundPetListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Get Post object and use the values to update the UI
                    val foundPetData = dataSnapshot.getValue<FoundPetData>()
                    foundPetData?.let {
                        val ownerId = foundPetData.foundPetOwnerId
                        editMenuItem.isVisible = isOwner(ownerId)
                        deleteMenuItem.isVisible = isOwner(ownerId)

                        with(binding) {
                            if (foundPetData.foundPetImageUrl.isNullOrEmpty()) {
                                Picasso.get()
                                    .load(DEFAULT_IMAGE_URL)
                                    .into(ivPetImage)
                            } else {
                                Picasso.get()
                                    .load(foundPetData.foundPetImageUrl)
                                    .into(ivPetImage)
                            }
                            tvFoundDetailsPetDecodedAddress.text =
                                foundPetData.foundPetDecodedAddress
                            tvFoundDetailsPetType.text =
                                getString(R.string.details_pet_type, foundPetData.foundPetType)
                            tvFoundDetailsPetDate.text =
                                getString(R.string.details_pet_date, foundPetData.foundPetDate)
                            tvFoundDetailsPetBehavior.text = getString(
                                R.string.details_pet_behavior,
                                foundPetData.foundPetBehavior
                            )
                            tvFoundDetailsPetAdditionalInfo.text = getString(
                                R.string.details_pet_additional,
                                foundPetData.foundPetAdditionalPetInfo
                            )
                            tvFoundDetailsPetFinderName.text = getString(
                                R.string.details_pet_owner_name,
                                foundPetData.foundPetFinderName
                            )
                            tvFoundDetailsPetPhoneNumber.text = getString(
                                R.string.details_pet_owner_number,
                                foundPetData.foundPetPhoneNumber
                            )
                            tvFoundDetailsPetEmailAddress.text = getString(
                                R.string.details_pet_owner_email,
                                foundPetData.foundPetEmailAddress
                            )
                            tvFoundDetailsPetOwnerAdditionalInfo.text = getString(
                                R.string.details_pet_owner_additional,
                                foundPetData.foundPetAdditionalFinderInfo
                            )
                        }
                        binding.foundDetailsMapButton.setOnClickListener {
                            val longitude = foundPetData.foundPetLocation?.longitude
                            val latitude = foundPetData.foundPetLocation?.latitude
                            val uri = Uri.parse("geo:,$longitude?q=$latitude,$longitude")

                            // Start the map application
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            mapIntent.setPackage("com.google.android.apps.maps") // This will ensure it opens in Google Maps
                            startActivity(mapIntent)
                        }
                        binding.foundPetPhoneButton.setOnClickListener {
                            val phoneNumber = foundPetData.foundPetPhoneNumber
                            val dialIntent = Intent(Intent.ACTION_DIAL)
                            dialIntent.data = Uri.parse("tel:$phoneNumber")
                            startActivity(dialIntent)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                    Toast.makeText(
                        context, "Failed to load post.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            databaseRef.addValueEventListener(foundPetListener)

            // Keep copy of post listener so we can remove it when app stops
            this.foundPetListener = foundPetListener


        }

        override fun onStop() {
            super.onStop()

            // Remove post value event listener
            foundPetListener?.let {
                databaseRef.removeEventListener(it)
            }

            // Clean up comments listener
            //adapter?.cleanupListener()
        }

        private fun isOwner(ownerId: String?): Boolean {
            // Get the currently logged-in user's ID
            val currentUser = FirebaseAuth.getInstance().currentUser
            val currentUserId = currentUser?.uid

            // Check if the ownerId matches the currently logged-in user's ID
            return ownerId == currentUserId
        }

        companion object {
            private const val DEFAULT_IMAGE_URL = "https://i.stack.imgur.com/l60Hf.png"
            private const val databaseUrl =
                "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
            private const val TAG = "FoundDetailFragment"
            const val EXTRA_POST_KEY = "post_key"
        }
    }