package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Represents a themed collection in the jewelry store
 * Collections group related products together for display and marketing
 */
data class ThemedCollection(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val productIds: List<String> = emptyList(), // Array of product IDs that belong to this collection
    val createdBy: String = "system",
    // New fields for customization
    val order: Int = 0,
    val images: List<CollectionImage> = emptyList(),
    val metadata: CollectionMetadata = CollectionMetadata()
)

/**
 * Repository interface for managing themed collections
 */
interface ThemedCollectionRepository {
    suspend fun getAllCollections(): List<ThemedCollection>
    suspend fun getCollectionById(id: String): ThemedCollection?
    suspend fun addCollection(collection: ThemedCollection): String
    suspend fun updateCollection(collection: ThemedCollection): Boolean
    suspend fun deleteCollection(id: String): Boolean
    suspend fun addProductToCollection(collectionId: String, productId: String): Boolean
    suspend fun removeProductFromCollection(collectionId: String, productId: String): Boolean
    suspend fun getCollectionsByProductId(productId: String): List<ThemedCollection>
}

/**
 * Firestore implementation of ThemedCollectionRepository
 */
class FirestoreThemedCollectionRepository(private val firestore: Firestore) : ThemedCollectionRepository {

    override suspend fun getAllCollections(): List<ThemedCollection> = withContext(Dispatchers.IO) {
        try {
            val collectionsCollection = firestore.collection("themed_collections")
            val future = collectionsCollection.get()
            val snapshot = future.get()
            
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                ThemedCollection(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    imageUrl = data["imageUrl"] as? String ?: "",
                    isActive = when (val value = data["isActive"]) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> true
                    },
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    productIds = (data["productIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    createdBy = data["createdBy"] as? String ?: "system"
                )
            }
        } catch (e: Exception) {
            println("Error fetching themed collections: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getCollectionById(id: String): ThemedCollection? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("themed_collections").document(id).get().get()
            if (doc.exists()) {
                val data = doc.data ?: return@withContext null
                
                ThemedCollection(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    imageUrl = data["imageUrl"] as? String ?: "",
                    isActive = when (val value = data["isActive"]) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> true
                    },
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    productIds = (data["productIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    createdBy = data["createdBy"] as? String ?: "system"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error fetching collection by ID: ${e.message}")
            null
        }
    }

    override suspend fun addCollection(collection: ThemedCollection): String = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val collectionData = mapOf(
                "name" to collection.name,
                "description" to collection.description,
                "imageUrl" to collection.imageUrl,
                "isActive" to collection.isActive,
                "createdAt" to currentTime,
                "updatedAt" to currentTime,
                "productIds" to collection.productIds,
                "createdBy" to collection.createdBy
            )
            
            val docRef = firestore.collection("themed_collections").add(collectionData).get()
            docRef.id
        } catch (e: Exception) {
            println("Error adding collection: ${e.message}")
            throw e
        }
    }

    override suspend fun updateCollection(collection: ThemedCollection): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val collectionData = mapOf(
                "name" to collection.name,
                "description" to collection.description,
                "imageUrl" to collection.imageUrl,
                "isActive" to collection.isActive,
                "updatedAt" to currentTime,
                "productIds" to collection.productIds,
                "createdBy" to collection.createdBy
            )
            
            firestore.collection("themed_collections").document(collection.id).set(collectionData).get()
            true
        } catch (e: Exception) {
            println("Error updating collection: ${e.message}")
            false
        }
    }

    override suspend fun deleteCollection(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("themed_collections").document(id).delete().get()
            true
        } catch (e: Exception) {
            println("Error deleting collection: ${e.message}")
            false
        }
    }

    override suspend fun addProductToCollection(collectionId: String, productId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val collectionRef = firestore.collection("themed_collections").document(collectionId)
            val doc = collectionRef.get().get()
            
            if (doc.exists()) {
                val data = doc.data ?: return@withContext false
                val currentProductIds = (data["productIds"] as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                
                if (!currentProductIds.contains(productId)) {
                    currentProductIds.add(productId)
                    
                    collectionRef.update(
                        "productIds", currentProductIds,
                        "updatedAt", System.currentTimeMillis()
                    ).get()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Error adding product to collection: ${e.message}")
            false
        }
    }

    override suspend fun removeProductFromCollection(collectionId: String, productId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val collectionRef = firestore.collection("themed_collections").document(collectionId)
            val doc = collectionRef.get().get()
            
            if (doc.exists()) {
                val data = doc.data ?: return@withContext false
                val currentProductIds = (data["productIds"] as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                
                currentProductIds.remove(productId)
                
                collectionRef.update(
                    "productIds", currentProductIds,
                    "updatedAt", System.currentTimeMillis()
                ).get()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Error removing product from collection: ${e.message}")
            false
        }
    }

    override suspend fun getCollectionsByProductId(productId: String): List<ThemedCollection> = withContext(Dispatchers.IO) {
        try {
            val collectionsCollection = firestore.collection("themed_collections")
            val future = collectionsCollection.whereArrayContains("productIds", productId).get()
            val snapshot = future.get()
            
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                ThemedCollection(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    imageUrl = data["imageUrl"] as? String ?: "",
                    isActive = when (val value = data["isActive"]) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> true
                    },
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    productIds = (data["productIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    createdBy = data["createdBy"] as? String ?: "system"
                )
            }
        } catch (e: Exception) {
            println("Error fetching collections by product ID: ${e.message}")
            emptyList()
        }
    }
}
