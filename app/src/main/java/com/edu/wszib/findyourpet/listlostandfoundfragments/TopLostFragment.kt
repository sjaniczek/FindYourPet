package com.edu.wszib.findyourpet.listlostandfoundfragments

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query

class TopLostFragment : LostListFragment() {
    override fun getQuery(databaseReference: DatabaseReference): Query {
        return databaseReference.child("lost_pets")
    }
}