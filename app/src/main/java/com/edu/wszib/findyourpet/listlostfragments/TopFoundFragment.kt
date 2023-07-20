package com.edu.wszib.findyourpet.listlostfragments

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TopFoundFragment: FoundListFragment() {
    override fun getQuery(databaseReference: DatabaseReference): Query {
        return databaseReference.child("found_pets")
    }
}
