package com.edu.wszib.findyourpet.listlostandfoundfragments

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class TopLostFragment : LostListFragment() {
    override fun getDatabaseReference(): DatabaseReference {
        return FirebaseDatabase.getInstance().reference.child("lost_pets")
    }
}