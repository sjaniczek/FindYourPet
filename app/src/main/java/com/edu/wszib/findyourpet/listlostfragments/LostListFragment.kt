package com.edu.wszib.findyourpet.listlostfragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edu.wszib.findyourpet.LostDetailsFragment
import com.edu.wszib.findyourpet.R
import com.edu.wszib.findyourpet.R.*
import com.edu.wszib.findyourpet.models.LostPetData
import com.edu.wszib.findyourpet.viewholders.LostPetViewHolder
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

abstract class LostListFragment : Fragment() {
    private lateinit var database: DatabaseReference
    // [END define_database_reference]

    private lateinit var recycler: RecyclerView
    private lateinit var manager: LinearLayoutManager
    private var adapter: FirebaseRecyclerAdapter<LostPetData, LostPetViewHolder>? = null

    val uid: String
        get() = Firebase.auth.currentUser!!.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val rootView = inflater.inflate(layout.fragment_recycler_lost, container, false)

        // [START create_database_reference]

        // [END create_database_reference]

        recycler = rootView.findViewById(R.id.recyclerViewLost)
        recycler.setHasFixedSize(true)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manager = LinearLayoutManager(activity)
        manager.reverseLayout = true
        manager.stackFromEnd = true
        recycler.layoutManager = manager

        val databaseUrl = "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
        database = Firebase.database(databaseUrl).reference

        val postsQuery = getQuery(database)

        val options = FirebaseRecyclerOptions.Builder<LostPetData>()
            .setQuery(postsQuery, LostPetData::class.java)
            .build()

        adapter = object : FirebaseRecyclerAdapter<LostPetData, LostPetViewHolder>(options) {
            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): LostPetViewHolder {
                val inflater = LayoutInflater.from(viewGroup.context)
                return LostPetViewHolder(inflater.inflate(layout.lost_list_iem, viewGroup, false))
            }

            override fun onBindViewHolder(viewHolder: LostPetViewHolder, position: Int, model: LostPetData) {
                val postRef = getRef(position)

                // Set click listener for the whole post view
                val postKey = postRef.key
                viewHolder.itemView.setOnClickListener {
                    // Launch PostDetailFragment
                    val navController = requireActivity().findNavController(R.id.nav_host_fragment)
                    val args = bundleOf(LostDetailsFragment.EXTRA_POST_KEY to postKey)
                    Toast.makeText(context, postKey, Toast.LENGTH_SHORT).show()
                    //Toast.makeText(context,args.toString(),Toast.LENGTH_SHORT).show()
                    navController.navigate(R.id.action_mainFragment_to_lostDetailsFragment, args)
                }

                // Determine if the current user has liked this post and set UI accordingly
                // viewHolder.setLikedState(model.stars.containsKey(uid))

                // Bind Post to ViewHolder, setting OnClickListener for the star button
                viewHolder.bindToLostPet(model) {
                    // Need to write to both places the post is stored
                    //val globalPostRef = database.child("posts").child(postRef.key!!)
                    //val userPostRef = database.child("user-posts").child(model.uid!!).child(postRef.key!!)

                    // Run two transactions
                    // onStarClicked(globalPostRef)
                    //onStarClicked(userPostRef)
                }
            }
        }

        recycler.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }
    abstract fun getQuery(databaseReference: DatabaseReference): Query

}
