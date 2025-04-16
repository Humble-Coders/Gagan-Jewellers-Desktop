// CustomerRepository.kt
package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

interface CustomerRepository {
    suspend fun getAllCustomers(): List<User>
    suspend fun getCustomerById(id: String): User?
    suspend fun addCustomer(user: User): String
    suspend fun updateCustomer(user: User): Boolean
}

class FirestoreCustomerRepository(private val firestore: Firestore) : CustomerRepository {
    override suspend fun getAllCustomers(): List<User> = withContext(Dispatchers.IO) {
        val customersCollection = firestore.collection("users")
        val future = customersCollection.get()

        val snapshot = future.get()
        snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null

            User(
                id = doc.id,
                email = data["email"] as? String ?: "",
                name = data["name"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                address = data["address"] as? String ?: "",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }

    override suspend fun getCustomerById(id: String): User? = withContext(Dispatchers.IO) {
        val docRef = firestore.collection("users").document(id)
        val future = docRef.get()
        val doc = future.get()

        if (doc.exists()) {
            val data = doc.data ?: return@withContext null

            User(
                id = doc.id,
                email = data["email"] as? String ?: "",
                name = data["name"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                address = data["address"] as? String ?: "",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        } else null
    }

    override suspend fun addCustomer(user: User): String = withContext(Dispatchers.IO) {
        try {
            // Use email as document ID for temporary users
            val docId =
                user.email.trim().lowercase(Locale.getDefault()) // Use lowercase email for consistency

            val userMap = mapOf(
                "email" to user.email,
                "name" to user.name,
                "phone" to user.phone,
                "address" to user.address,
                "createdAt" to System.currentTimeMillis(),
                "isTemporary" to true // Flag to identify temporary users
            )

            // Create/update the document with email as ID
            firestore.collection("users")
                .document(docId)
                .set(userMap)
                .get()

            return@withContext docId
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun updateCustomer(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val userMap = mapOf(
                "email" to user.email,
                "name" to user.name,
                "phone" to user.phone,
                "address" to user.address
            )

            firestore.collection("users").document(user.id).update(userMap).get()
            true
        } catch (e: Exception) {
            false
        }
    }
}