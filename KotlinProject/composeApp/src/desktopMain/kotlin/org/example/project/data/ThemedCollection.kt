package org.example.project.data

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Image data for a collection
 */
data class CollectionImage(
    val url: String = "",
    val isActive: Boolean = true,
    val order: Int = 0
)

/**
 * Metadata for a collection
 */
data class CollectionMetadata(
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "admin"
)

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
                
                // Debug: Print all fields in the document
                println("🔍 Collection '${data["name"]}' Firestore fields: ${data.keys.joinToString(", ")}")
                
                // Parse images - support multiple formats
                val imagesList = when {
                    // Format 1: imageUrls (array of strings) - current Firestore format
                    data.containsKey("imageUrls") -> {
                        val urls = (data["imageUrls"] as? List<*>)?.mapIndexedNotNull { index, url ->
                            CollectionImage(
                                url = url as? String ?: "",
                                isActive = true,
                                order = index
                            )
                        } ?: emptyList()
                        println("   📸 Parsed ${urls.size} images from 'imageUrls' field")
                        urls
                    }
                    // Format 2: images (array of objects with url, isActive, order)
                    data.containsKey("images") -> {
                        val imgs = (data["images"] as? List<*>)?.mapNotNull { imageData ->
                            val imgMap = imageData as? Map<*, *> ?: return@mapNotNull null
                            CollectionImage(
                                url = imgMap["url"] as? String ?: "",
                                isActive = when (val value = imgMap["isActive"]) {
                                    is Boolean -> value
                                    is String -> value.toBoolean()
                                    else -> true
                                },
                                order = (imgMap["order"] as? Number)?.toInt() ?: 0
                            )
                        } ?: emptyList()
                        println("   📸 Parsed ${imgs.size} images from 'images' field")
                        imgs
                    }
                    else -> {
                        println("   ⚠️ No 'imageUrls' or 'images' field found")
                        emptyList()
                    }
                }
                
                // Get first imageUrl (for backward compatibility)
                val mainImageUrl = when {
                    data.containsKey("imageUrl") -> data["imageUrl"] as? String ?: ""
                    imagesList.isNotEmpty() -> imagesList.first().url
                    else -> ""
                }
                
                // Parse metadata
                val metadataMap = data["metadata"] as? Map<*, *>
                val metadata = if (metadataMap != null) {
                    CollectionMetadata(
                        createdAt = (metadataMap["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (metadataMap["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdBy = metadataMap["createdBy"] as? String ?: "admin"
                    )
                } else {
                    CollectionMetadata()
                }
                
                ThemedCollection(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    imageUrl = mainImageUrl,
                    isActive = when (val value = data["isActive"]) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> true
                    },
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    productIds = (data["productIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    createdBy = data["createdBy"] as? String ?: "system",
                    order = (data["order"] as? Number)?.toInt() ?: 0,
                    images = imagesList,
                    metadata = metadata
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
                
                // Parse images - support multiple formats
                val imagesList = when {
                    // Format 1: imageUrls (array of strings) - current Firestore format
                    data.containsKey("imageUrls") -> {
                        (data["imageUrls"] as? List<*>)?.mapIndexedNotNull { index, url ->
                            CollectionImage(
                                url = url as? String ?: "",
                                isActive = true,
                                order = index
                            )
                        } ?: emptyList()
                    }
                    // Format 2: images (array of objects with url, isActive, order)
                    data.containsKey("images") -> {
                        (data["images"] as? List<*>)?.mapNotNull { imageData ->
                            val imgMap = imageData as? Map<*, *> ?: return@mapNotNull null
                            CollectionImage(
                                url = imgMap["url"] as? String ?: "",
                                isActive = when (val value = imgMap["isActive"]) {
                                    is Boolean -> value
                                    is String -> value.toBoolean()
                                    else -> true
                                },
                                order = (imgMap["order"] as? Number)?.toInt() ?: 0
                            )
                        } ?: emptyList()
                    }
                    else -> emptyList()
                }
                
                // Get first imageUrl (for backward compatibility)
                val mainImageUrl = when {
                    data.containsKey("imageUrl") -> data["imageUrl"] as? String ?: ""
                    imagesList.isNotEmpty() -> imagesList.first().url
                    else -> ""
                }
                
                // Parse metadata
                val metadataMap = data["metadata"] as? Map<*, *>
                val metadata = if (metadataMap != null) {
                    CollectionMetadata(
                        createdAt = (metadataMap["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (metadataMap["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdBy = metadataMap["createdBy"] as? String ?: "admin"
                    )
                } else {
                    CollectionMetadata()
                }
                
                ThemedCollection(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    imageUrl = mainImageUrl,
                    isActive = when (val value = data["isActive"]) {
                        is Boolean -> value
                        is String -> value.toBoolean()
                        else -> true
                    },
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    productIds = (data["productIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    createdBy = data["createdBy"] as? String ?: "system",
                    order = (data["order"] as? Number)?.toInt() ?: 0,
                    images = imagesList,
                    metadata = metadata
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
            
            // Convert images to map format for Firestore
            val imagesData = collection.images.map { image ->
                mapOf(
                    "url" to image.url,
                    "isActive" to image.isActive,
                    "order" to image.order
                )
            }
            
            // Convert metadata to map format
            val metadataData = mapOf(
                "createdAt" to collection.metadata.createdAt,
                "updatedAt" to currentTime,
                "createdBy" to collection.metadata.createdBy
            )
            
            val collectionData = mapOf(
                "name" to collection.name,
                "description" to collection.description,
                "imageUrl" to collection.imageUrl,
                "isActive" to collection.isActive,
                "createdAt" to currentTime,
                "updatedAt" to currentTime,
                "productIds" to collection.productIds,
                "createdBy" to collection.createdBy,
                "order" to collection.order,
                "images" to imagesData,
                "metadata" to metadataData
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
            
            // Convert images to map format for Firestore
            val imagesData = collection.images.map { image ->
                mapOf(
                    "url" to image.url,
                    "isActive" to image.isActive,
                    "order" to image.order
                )
            }
            
            // Convert metadata to map format
            val metadataData = mapOf(
                "createdAt" to collection.metadata.createdAt,
                "updatedAt" to currentTime,
                "createdBy" to collection.metadata.createdBy
            )
            
            val collectionData = mapOf(
                "name" to collection.name,
                "description" to collection.description,
                "imageUrl" to collection.imageUrl,
                "isActive" to collection.isActive,
                "updatedAt" to currentTime,
                "productIds" to collection.productIds,
                "createdBy" to collection.createdBy,
                "order" to collection.order,
                "images" to imagesData,
                "metadata" to metadataData
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
            
            // Use transaction to atomically read and update productIds array
            firestore.runTransaction { transaction: Transaction ->
                val docSnapshot = transaction.getDocument(collectionRef)
            
                if (!docSnapshot.exists()) {
                    throw Exception("Collection not found: $collectionId")
                }
                
                val data = docSnapshot.data ?: throw Exception("Collection document has no data")
                val currentProductIds = (data["productIds"] as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                
                if (!currentProductIds.contains(productId)) {
                    currentProductIds.add(productId)
                    
                    transaction.update(
                        collectionRef,
                        "productIds", currentProductIds,
                        "updatedAt", System.currentTimeMillis()
                    )
                }
                Unit
            }.get()
            
                true
        } catch (e: Exception) {
            println("Error adding product to collection: ${e.message}")
            false
        }
    }

    override suspend fun removeProductFromCollection(collectionId: String, productId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val collectionRef = firestore.collection("themed_collections").document(collectionId)
            
            // Use transaction to atomically read and update productIds array
            firestore.runTransaction { transaction: Transaction ->
                val docSnapshot = transaction.getDocument(collectionRef)
            
                if (!docSnapshot.exists()) {
                    throw Exception("Collection not found: $collectionId")
                }
                
                val data = docSnapshot.data ?: throw Exception("Collection document has no data")
                val currentProductIds = (data["productIds"] as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                
                currentProductIds.remove(productId)
                
                transaction.update(
                    collectionRef,
                    "productIds", currentProductIds,
                    "updatedAt", System.currentTimeMillis()
                )
                Unit
            }.get()
            
                true
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
