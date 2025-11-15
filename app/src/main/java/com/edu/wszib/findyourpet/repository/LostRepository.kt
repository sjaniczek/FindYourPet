package com.edu.wszib.findyourpet.repository

import android.net.Uri
import com.edu.wszib.findyourpet.models.LostPetData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.google.firebase.database.ktx.database
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class LostRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = Firebase.database(
        "https://findyourpet-e77a8-default-rtdb.europe-west1.firebasedatabase.app/"
    )
    private val storage = FirebaseStorage.getInstance()

    // Pobranie pojedynczego wpisu (once)
    fun getLostPetOnce(lostPetId: String, callback: (LostPetData?) -> Unit) {
        database.reference.child("lost_pets").child(lostPetId)
            .get()
            .addOnSuccessListener { snapshot ->
                val data = snapshot.getValue(LostPetData::class.java)
                callback(data)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    suspend fun uploadLostPet(data: LostPetData, imageUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("Brak użytkownika"))
            val key = database.reference.child("lost_pets").push().key
                ?: return@withContext Result.failure(Exception("Nie udało się wygenerować klucza"))

            val fileRef = storage.reference.child("images/${UUID.randomUUID()}")
            fileRef.putFile(imageUri).await()
            val imageUrl = fileRef.downloadUrl.await().toString()

            val updatedData = data.copy(lostPetOwnerId = userId, lostPetId = key, lostPetImageUrl = imageUrl)
            val values = updatedData.toMap()
            val updates = mapOf(
                "/lost_pets/$key" to values,
                "/users/$userId/lost_pets/$key" to values
            )
            database.reference.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLostPet(lostPetId: String, data: LostPetData, newImageUri: Uri?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("Brak użytkownika"))

            val imageUrl = if (newImageUri != null) {
                val fileRef = storage.reference.child("images/${UUID.randomUUID()}")
                fileRef.putFile(newImageUri).await()
                fileRef.downloadUrl.await().toString()
            } else data.lostPetImageUrl ?: ""

            val updatedData = data.copy(lostPetOwnerId = userId, lostPetImageUrl = imageUrl)
            val values = updatedData.toMap()

            val updates = mapOf(
                "/lost_pets/$lostPetId" to values,
                "/users/$userId/lost_pets/$lostPetId" to values
            )

            database.reference.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun deleteLostPet(lostPetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: return@withContext Result.failure(Exception("Brak użytkownika"))

            val updates = mapOf<String, Any?>(
                "/lost_pets/$lostPetId" to null,
                "/users/$userId/lost_pets/$lostPetId" to null
            )

            FirebaseDatabase.getInstance().reference.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendReport(lostPetId: String, message: String, userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val reportRef = database.reference.child("reports").push()

                val reportData = mapOf(
                    "postId" to lostPetId,
                    "userId" to userId,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis()
                )

                reportRef.setValue(reportData).await()


                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    fun getCurrentUserId(): String? = auth.currentUser?.uid
}
