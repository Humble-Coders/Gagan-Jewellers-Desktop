package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CustomizationRepository {
    suspend fun getAllCollections(): List<ThemedCollection>
    suspend fun getCollectionById(id: String): ThemedCollection?
    suspend fun saveCollection(collection: ThemedCollection): String
    suspend fun updateCollection(collection: ThemedCollection): Boolean
    suspend fun deleteCollection(id: String): Boolean
    
    suspend fun getAllCarouselItems(): List<CarouselItem>
    suspend fun getCarouselItemById(id: String): CarouselItem?
    suspend fun saveCarouselItem(item: CarouselItem): String
    suspend fun updateCarouselItem(item: CarouselItem): Boolean
    suspend fun deleteCarouselItem(id: String): Boolean
    
    suspend fun getAppConfig(): AppConfig?
    suspend fun updateAppConfig(config: AppConfig): Boolean
}

class FirestoreCustomizationRepository(private val firestore: Firestore) : CustomizationRepository {

    // Collections
    override suspend fun getAllCollections(): List<ThemedCollection> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("themedCollections")
                .orderBy("order")
                .get().get()
            
            snapshot.documents.map { doc ->
                docToCollection(doc)
            }
        } catch (e: Exception) {
            println("❌ Error loading collections: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getCollectionById(id: String): ThemedCollection? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("themedCollections").document(id).get().get()
            if (doc.exists()) {
                docToCollection(doc)
            } else null
        } catch (e: Exception) {
            println("❌ Error loading collection: ${e.message}")
            null
        }
    }

    override suspend fun saveCollection(collection: ThemedCollection): String = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("themedCollections").document()
            val newId = docRef.id
            docRef.set(collectionToMap(collection.copy(id = newId))).get()
            println("✅ Collection saved with ID: $newId")
            newId
        } catch (e: Exception) {
            println("❌ Error saving collection: ${e.message}")
            throw e
        }
    }

    override suspend fun updateCollection(collection: ThemedCollection): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("themedCollections").document(collection.id)
                .update(collectionToMap(collection)).get()
            println("✅ Collection updated: ${collection.id}")
            true
        } catch (e: Exception) {
            println("❌ Error updating collection: ${e.message}")
            false
        }
    }

    override suspend fun deleteCollection(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("themedCollections").document(id).delete().get()
            println("✅ Collection deleted: $id")
            true
        } catch (e: Exception) {
            println("❌ Error deleting collection: ${e.message}")
            false
        }
    }

    // Carousel Items
    override suspend fun getAllCarouselItems(): List<CarouselItem> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("carouselItems")
                .orderBy("order")
                .get().get()
            
            snapshot.documents.map { doc ->
                docToCarouselItem(doc)
            }
        } catch (e: Exception) {
            println("❌ Error loading carousel items: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getCarouselItemById(id: String): CarouselItem? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("carouselItems").document(id).get().get()
            if (doc.exists()) {
                docToCarouselItem(doc)
            } else null
        } catch (e: Exception) {
            println("❌ Error loading carousel item: ${e.message}")
            null
        }
    }

    override suspend fun saveCarouselItem(item: CarouselItem): String = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("carouselItems").document()
            val newId = docRef.id
            docRef.set(carouselItemToMap(item.copy(id = newId))).get()
            println("✅ Carousel item saved with ID: $newId")
            newId
        } catch (e: Exception) {
            println("❌ Error saving carousel item: ${e.message}")
            throw e
        }
    }

    override suspend fun updateCarouselItem(item: CarouselItem): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("carouselItems").document(item.id)
                .update(carouselItemToMap(item)).get()
            println("✅ Carousel item updated: ${item.id}")
            true
        } catch (e: Exception) {
            println("❌ Error updating carousel item: ${e.message}")
            false
        }
    }

    override suspend fun deleteCarouselItem(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("carouselItems").document(id).delete().get()
            println("✅ Carousel item deleted: $id")
            true
        } catch (e: Exception) {
            println("❌ Error deleting carousel item: ${e.message}")
            false
        }
    }

    // App Config
    override suspend fun getAppConfig(): AppConfig? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("appConfig").document("displaySettings").get().get()
            if (doc.exists()) {
                val data = doc.data
                AppConfig(
                    id = doc.id,
                    activeCollectionIds = (data?.get("activeCollectionIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    activeCarouselIds = (data?.get("activeCarouselIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    collectionsEnabled = data?.get("collectionsEnabled") as? Boolean ?: true,
                    carouselsEnabled = data?.get("carouselsEnabled") as? Boolean ?: true,
                    lastPublished = data?.get("lastPublished") as? Long ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            println("❌ Error loading app config: ${e.message}")
            null
        }
    }

    override suspend fun updateAppConfig(config: AppConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "activeCollectionIds" to config.activeCollectionIds,
                "activeCarouselIds" to config.activeCarouselIds,
                "collectionsEnabled" to config.collectionsEnabled,
                "carouselsEnabled" to config.carouselsEnabled,
                "lastPublished" to System.currentTimeMillis()
            )
            firestore.collection("appConfig").document("displaySettings").set(data).get()
            println("✅ App config updated")
            true
        } catch (e: Exception) {
            println("❌ Error updating app config: ${e.message}")
            false
        }
    }

    // Helper methods
    private fun docToCollection(doc: com.google.cloud.firestore.DocumentSnapshot): ThemedCollection {
        val data = doc.data ?: return ThemedCollection()
        val imagesData = data["images"] as? List<Map<String, Any>> ?: emptyList()
        val metadataData = data["metadata"] as? Map<String, Any> ?: emptyMap()
        
        return ThemedCollection(
            id = doc.id,
            name = data["name"] as? String ?: "",
            description = data["description"] as? String ?: "",
            imageUrl = data["imageUrl"] as? String ?: "",
            isActive = data["isActive"] as? Boolean ?: true,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            productIds = (data["productIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            createdBy = data["createdBy"] as? String ?: "system",
            order = (data["order"] as? Number)?.toInt() ?: 0,
            images = imagesData.map { imageData ->
                CollectionImage(
                    url = imageData["url"] as? String ?: "",
                    isActive = imageData["isActive"] as? Boolean ?: true,
                    order = (imageData["order"] as? Number)?.toInt() ?: 0
                )
            },
            metadata = CollectionMetadata(
                createdAt = (metadataData["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (metadataData["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                createdBy = metadataData["createdBy"] as? String ?: "admin"
            )
        )
    }

    private fun collectionToMap(collection: ThemedCollection): Map<String, Any> {
        return mapOf(
            "name" to collection.name,
            "description" to collection.description,
            "imageUrl" to collection.imageUrl,
            "isActive" to collection.isActive,
            "createdAt" to collection.createdAt,
            "updatedAt" to System.currentTimeMillis(),
            "productIds" to collection.productIds,
            "createdBy" to collection.createdBy,
            "order" to collection.order,
            "images" to collection.images.map { image ->
                mapOf(
                    "url" to image.url,
                    "isActive" to image.isActive,
                    "order" to image.order
                )
            },
            "metadata" to mapOf(
                "createdAt" to collection.metadata.createdAt,
                "updatedAt" to System.currentTimeMillis(),
                "createdBy" to collection.metadata.createdBy
            )
        )
    }

    private fun docToCarouselItem(doc: com.google.cloud.firestore.DocumentSnapshot): CarouselItem {
        val data = doc.data ?: return CarouselItem()
        val imageData = data["image"] as? Map<String, Any> ?: emptyMap()
        val metadataData = data["metadata"] as? Map<String, Any> ?: emptyMap()
        
        return CarouselItem(
            id = doc.id,
            title = data["title"] as? String ?: "",
            titleActive = data["titleActive"] as? Boolean ?: true,
            subtitle = data["subtitle"] as? String ?: "",
            subtitleActive = data["subtitleActive"] as? Boolean ?: true,
            image = CarouselImage(
                url = imageData["url"] as? String ?: "",
                isActive = imageData["isActive"] as? Boolean ?: true
            ),
            isActive = data["isActive"] as? Boolean ?: true,
            order = (data["order"] as? Number)?.toInt() ?: 0,
            metadata = CarouselMetadata(
                createdAt = (metadataData["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (metadataData["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                createdBy = metadataData["createdBy"] as? String ?: "admin"
            )
        )
    }

    private fun carouselItemToMap(item: CarouselItem): Map<String, Any> {
        return mapOf(
            "title" to item.title,
            "titleActive" to item.titleActive,
            "subtitle" to item.subtitle,
            "subtitleActive" to item.subtitleActive,
            "image" to mapOf(
                "url" to item.image.url,
                "isActive" to item.image.isActive
            ),
            "isActive" to item.isActive,
            "order" to item.order,
            "metadata" to mapOf(
                "createdAt" to item.metadata.createdAt,
                "updatedAt" to System.currentTimeMillis(),
                "createdBy" to item.metadata.createdBy
            )
        )
    }
}
