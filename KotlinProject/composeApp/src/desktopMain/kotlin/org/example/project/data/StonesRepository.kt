package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Stone(
    val id: String = "",
    val name: String = "",
    val color: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

interface StonesRepository {
    suspend fun getAllStoneNames(): List<String>
    suspend fun getAllStoneColors(): List<String>
    suspend fun addStone(name: String, color: String): String
}

class FirestoreStonesRepository(private val firestore: Firestore) : StonesRepository {

    private val collection get() = firestore.collection("stones")

    override suspend fun getAllStoneNames(): List<String> = withContext(Dispatchers.IO) {
        val snapshot = collection.get().get()
        snapshot.documents.mapNotNull { it.getString("name") }.distinct().sorted()
    }

    override suspend fun getAllStoneColors(): List<String> = withContext(Dispatchers.IO) {
        val snapshot = collection.get().get()
        snapshot.documents.mapNotNull { it.getString("color") }.distinct().sorted()
    }

    override suspend fun addStone(name: String, color: String): String = withContext(Dispatchers.IO) {
        val newDoc = collection.document()
        val id = newDoc.id
        val data = mapOf(
            "name" to name,
            "color" to color,
            "created_at" to System.currentTimeMillis()
        )
        newDoc.set(data).get()
        id
    }
}





