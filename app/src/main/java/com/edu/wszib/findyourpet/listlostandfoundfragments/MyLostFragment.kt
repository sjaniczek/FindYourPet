package com.edu.wszib.findyourpet.listlostandfoundfragments

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.ktx.Firebase

class MyLostFragment : LostListFragment() {
    private lateinit var auth: FirebaseAuth
    override fun getQuery(databaseReference: DatabaseReference): Query {
        auth = Firebase.auth
        val currentUser = auth.currentUser?.uid
        return databaseReference.child("users/$currentUser/lost_pets")
    }
}