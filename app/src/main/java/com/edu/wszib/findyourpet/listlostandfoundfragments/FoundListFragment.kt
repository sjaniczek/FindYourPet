package com.edu.wszib.findyourpet.listlostandfoundfragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edu.wszib.findyourpet.foundfragments.FoundDetailsFragment
import com.edu.wszib.findyourpet.R
import com.edu.wszib.findyourpet.models.FoundPetData
import com.edu.wszib.findyourpet.viewholders.FoundPetViewHolder
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

abstract class FoundListFragment : Fragment(){
    private lateinit var database: DatabaseReference
    // [END define_database_reference]
    private lateinit var auth: FirebaseAuth
    private lateinit var recycler: RecyclerView
    private lateinit var manager: LinearLayoutManager
    private lateinit var adapter: FirebaseRecyclerAdapter<FoundPetData, FoundPetViewHolder>

    val uid: String
        get() = Firebase.auth.currentUser!!.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val rootView = inflater.inflate(R.layout.fragment_recycler_found, container, false)

        // [START create_database_reference]

        // [END create_database_reference]

        recycler = rootView.findViewById(R.id.recyclerViewFound)
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

        val options = FirebaseRecyclerOptions.Builder<FoundPetData>()
            .setQuery(postsQuery, FoundPetData::class.java)
            .build()

        adapter = object : FirebaseRecyclerAdapter<FoundPetData, FoundPetViewHolder>(options) {

            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): FoundPetViewHolder {
                val inflater = LayoutInflater.from(viewGroup.context)
                return FoundPetViewHolder(inflater.inflate(R.layout.found_list_item, viewGroup, false))
            }



            override fun onBindViewHolder(viewHolder: FoundPetViewHolder, position: Int, model: FoundPetData) {
                val postRef = getRef(position)

                // Set click listener for the whole post view
                val postKey = postRef.key
                viewHolder.itemView.setOnClickListener {
                    // Launch PostDetailFragment
                    val navController = requireActivity().findNavController(R.id.nav_host_fragment)
                    val args = bundleOf(FoundDetailsFragment.EXTRA_POST_KEY to postKey)
                    navController.navigate(R.id.foundDetailsFragment, args)
                }

                viewHolder.bindToLostPet(model)
            }
            override fun onDataChanged() {
                super.onDataChanged()
                val textEmpty = view.findViewById<TextView>(R.id.tvFoundPetRecyclerEmpty)
                // Check if the adapter has data or not
                val isEmpty = itemCount == 0
                if (isEmpty) {
                    // Show the empty state TextView
                    textEmpty.visibility = View.VISIBLE
                } else {
                    // Hide the empty state TextView
                    textEmpty.visibility = View.GONE
                }
            }
        }



        recycler.adapter = adapter
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onStart() {
        super.onStart()
        adapter.notifyDataSetChanged()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }
    abstract fun getQuery(databaseReference: DatabaseReference): Query

}