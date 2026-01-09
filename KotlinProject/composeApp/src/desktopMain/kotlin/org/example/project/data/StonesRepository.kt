package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Stone type with purity and rate
data class StoneType(
    val purity: String = "",
    val rate: String = "" // Stored as string in Firestore
)

data class Stone(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val types: List<StoneType> = emptyList(), // Updated to support purity and rate
    val createdAt: Long = System.currentTimeMillis()
)

interface StonesRepository {
    suspend fun getAllStoneNames(): List<String>
    suspend fun getAllStonePurities(): List<String>
    suspend fun getAllStones(): List<Stone>
    suspend fun addStone(name: String): String
}

class FirestoreStonesRepository(private val firestore: Firestore) : StonesRepository {

    private val collection get() = firestore.collection("stones")

    override suspend fun getAllStoneNames(): List<String> = withContext(Dispatchers.IO) {
        val snapshot = collection.get().get()
        snapshot.documents.mapNotNull { it.getString("name") }.distinct().sorted()
    }

    override suspend fun getAllStonePurities(): List<String> = withContext(Dispatchers.IO) {
        // Extract purities from stones collection's types array
        // Each stone document has a types array: [{purity: "VVS", rate: "5000"}, {purity: "VS", rate: "4500"}]
        getAllStones()
            .flatMap { it.types.map { stoneType -> stoneType.purity } }
            .distinct()
            .filter { it.isNotEmpty() }
            .sorted()
    }

    override suspend fun getAllStones(): List<Stone> = withContext(Dispatchers.IO) {
        val snapshot = collection.get().get()
        snapshot.documents.map { doc ->
            val data = doc.data
            // Parse types array - can be List<String> (old format) or List<Map> (new format)
            val typesList = mutableListOf<StoneType>()
            val typesData = data["types"]
            
            when (typesData) {
                is List<*> -> {
                    typesData.forEach { typeItem ->
                        when (typeItem) {
                            is String -> {
                                // Old format: just string types, convert to StoneType with empty rate
                                // Normalize purity (uppercase for consistency)
                                val normalizedPurity = typeItem.trim().uppercase()
                                typesList.add(StoneType(purity = normalizedPurity, rate = ""))
                            }
                            is Map<*, *> -> {
                                // New format: map with purity and rate
                                val purity = (typeItem["purity"] as? String) ?: (typeItem["purity"] as? Number)?.toString() ?: ""
                                val rate = (typeItem["rate"] as? String) ?: (typeItem["rate"] as? Number)?.toString() ?: ""
                                // Normalize purity (uppercase for consistency)
                                val normalizedPurity = purity.trim().uppercase()
                                typesList.add(StoneType(purity = normalizedPurity, rate = rate))
                            }
                        }
                    }
                }
            }
            
            Stone(
                id = doc.id,
                name = data["name"] as? String ?: "",
                imageUrl = data["image_url"] as? String ?: "",
                types = typesList,
                createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }

    override suspend fun addStone(name: String): String = withContext(Dispatchers.IO) {
        val newDoc = collection.document()
        val id = newDoc.id
        val data = mapOf(
            "name" to name,
            "created_at" to System.currentTimeMillis()
        )
        newDoc.set(data).get()
        id
    }
}





