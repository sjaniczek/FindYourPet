package com.edu.wszib.findyourpet.lostfragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.edu.wszib.findyourpet.R
import com.edu.wszib.findyourpet.databinding.FragmentDetailsLostBinding
import com.edu.wszib.findyourpet.models.LostPetData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import java.lang.IllegalArgumentException

class LostDetailsFragment : Fragment() {

    private lateinit var editMenuItem: MenuItem
    private lateinit var deleteMenuItem: MenuItem
    private lateinit var auth: FirebaseAuth
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

        // Initialize Firebase auth
        auth = Firebase.auth
        // Initialize Database
        database = Firebase.database(databaseUrl)
        databaseRef = database.reference.child("lost_pets").child(lostPetKey)
        activity?.title = "Details";

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.menu_lost_pet_details, menu)
                editMenuItem = menu.findItem(R.id.action_edit_pet)
                deleteMenuItem = menu.findItem(R.id.action_delete_pet)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.action_edit_pet -> {
                        navigateToEditPet()
                        true
                    }

                    R.id.action_delete_pet -> {
                        deleteLostPet()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun deleteLostPet() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val lostPetRef = databaseRef

            val confirmationDialog = AlertDialog.Builder(requireContext())
                .setTitle("Delete Lost Pet")
                .setMessage("Are you sure you want to delete this lost pet?")
                .setPositiveButton("Yes") { _, _ ->
                    // User clicked "Yes," proceed with the deletion
                    // Create a map to delete the post from both locations in a single update
                    val childUpdates = HashMap<String, Any?>()
                    childUpdates["/lost_pets/$lostPetKey"] = null
                    childUpdates["/users/$userId/lost_pets/$lostPetKey"] = null

                    database.reference.updateChildren(childUpdates)
                        .addOnSuccessListener {
                            // Post deleted successfully
                            // Navigate back to the previous fragment using NavController
                            val navController = findNavController()
                            navController.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            // Failed to delete post
                            Toast.makeText(
                                requireContext(),
                                "Failed to delete post: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    // User clicked "Cancel," do nothing
                }
                .create()

            confirmationDialog.show()
        }
    }


    override fun onStart() {
        super.onStart()

        // Add value event listener to the post
        val lostPetListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                val lostPetData = dataSnapshot.getValue<LostPetData>()
                lostPetData?.let {
                    val ownerId = lostPetData.lostPetOwnerId
                    editMenuItem.isVisible = isOwner(ownerId)
                    deleteMenuItem.isVisible = isOwner(ownerId)

                    with(binding) {
                        tvLostDetailsPetName.text = lostPetData.lostPetName
                        if (lostPetData.lostPetImageUrl.isNullOrEmpty()) {
                            Picasso.get()
                                .load(DEFAULT_IMAGE_URL)
                                .into(ivPetImage)
                        } else {
                            Picasso.get()
                                .load(lostPetData.lostPetImageUrl)
                                .placeholder(R.drawable.pets)
                                .error(R.drawable.pets)
                                .into(ivPetImage)
                        }
                        tvLostDetailsPetDecodedAddress.text = lostPetData.lostPetDecodedAddress
                        tvLostDetailsPetType.text =
                            getString(R.string.details_pet_type, lostPetData.lostPetType)
                        tvLostDetailsPetDate.text =
                            getString(R.string.details_pet_date, lostPetData.lostPetDate)
                        tvLostDetailsPetHour.text =
                            getString(R.string.details_pet_hour, lostPetData.lostPetHour)
                        tvLostDetailsPetBehavior.text =
                            getString(R.string.details_pet_behavior, lostPetData.lostPetBehavior)
                        tvLostDetailsPetReact.text =
                            getString(R.string.details_pet_react, lostPetData.lostPetReact)
                        tvLostDetailsPetAdditionalInfo.text = getString(
                            R.string.details_pet_additional,
                            lostPetData.lostPetAdditionalPetInfo
                        )
                        tvLostDetailsPetOwnerName.text =
                            getString(R.string.details_pet_owner_name, lostPetData.lostPetOwnerName)
                        tvLostDetailsPetPhoneNumber.text = getString(
                            R.string.details_pet_owner_number,
                            lostPetData.lostPetPhoneNumber
                        )
                        tvLostDetailsPetEmailAddress.text = getString(
                            R.string.details_pet_owner_email,
                            lostPetData.lostPetEmailAddress
                        )
                        tvLostDetailsPetOwnerAdditionalInfo.text = getString(
                            R.string.details_pet_owner_additional,
                            lostPetData.lostPetAdditionalOwnerInfo
                        )
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
                    binding.lostPetPhoneButton.setOnClickListener {
                        val phoneNumber = lostPetData.lostPetPhoneNumber
                        val dialIntent = Intent(Intent.ACTION_DIAL)
                        dialIntent.data = Uri.parse("tel:$phoneNumber")
                        startActivity(dialIntent)
                    }
                    binding.lostDetailsSmsButton.setOnClickListener {
                        val phoneNumber = lostPetData.lostPetPhoneNumber
                        val smsUri = Uri.parse("smsto:$phoneNumber")
                        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri)
                        smsIntent.putExtra(
                            "sms_body",
                            "Dzień dobry, kontaktuję się w sprawie zagubionego zwierzaka."
                        ) // Optional message
                        startActivity(smsIntent)
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

    private fun isOwner(ownerId: String?): Boolean {
        // Get the currently logged-in user's ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid

        // Check if the ownerId matches the currently logged-in user's ID
        return ownerId == currentUserId
    }

    private fun navigateToEditPet() {
        val args = bundleOf(LostEditFragment.LOST_EDIT_POST_KEY to lostPetKey)
        val navController = requireActivity().findNavController(R.id.nav_host_fragment)
        navController.navigate(R.id.lostEditFragment, args)

    }

    companion object {
        private const val DEFAULT_IMAGE_URL = "https://i.stack.imgur.com/l60Hf.png"
        private const val databaseUrl =
            "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        private const val TAG = "LostDetailFragment"
        const val EXTRA_POST_KEY = "post_key"
    }
}