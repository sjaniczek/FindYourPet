package com.edu.wszib.findyourpet.listlostandfoundfragments

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class TopFoundFragment : FoundListFragment() {
    override fun getDatabaseReference(): DatabaseReference {
        // Return reference to all found pets in the database
        return FirebaseDatabase.getInstance().reference.child("found_pets")
    }
}
