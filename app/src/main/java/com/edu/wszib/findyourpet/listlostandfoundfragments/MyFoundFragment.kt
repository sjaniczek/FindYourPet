package com.edu.wszib.findyourpet.listlostandfoundfragments

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.ktx.Firebase

class MyFoundFragment : FoundListFragment() {
    override fun getDatabaseReference(): DatabaseReference {
        val uid = Firebase.auth.currentUser?.uid ?: ""
        return FirebaseDatabase.getInstance().reference.child("users/$uid/found_pets")
    }
}