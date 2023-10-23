package com.edu.wszib.findyourpet.listlostandfoundfragments

import com.google.firebase.database.*

class TopFoundFragment : FoundListFragment() {
    override fun getQuery(databaseReference: DatabaseReference): Query {
        return databaseReference.child("found_pets")
    }
}
