package com.example.myapplication.data.repositories

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SnapRepository {

    private val storage = Firebase.storage
    private val firestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun uploadSnap(localUri: Uri): Result<Uri> = try {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No user"))
        val snapId = UUID.randomUUID().toString()
        val ref = storage.reference.child("snaps/$uid/$snapId.jpg")
        ref.putFile(localUri).await()
        val downloadUrl = ref.downloadUrl.await()

        // Store metadata (minimal)
        val data = mapOf(
            "id" to snapId,
            "sender" to uid,
            "mediaUrl" to downloadUrl.toString(),
            "createdAt" to com.google.firebase.Timestamp.now(),
            "recipients" to listOf(uid) // TODO replace with actual recipients
        )
        firestore.collection("snaps").document(snapId).set(data).await()

        Result.success(downloadUrl)
    } catch (e: Exception) {
        Result.failure(e)
    }
} 