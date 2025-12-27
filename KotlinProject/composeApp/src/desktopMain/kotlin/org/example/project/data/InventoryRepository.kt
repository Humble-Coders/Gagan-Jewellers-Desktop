package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface InventoryRepository {
    suspend fun getAllInventoryItems(): List<InventoryItem>
    suspend fun getInventoryItemById(id: String): InventoryItem?
    suspend fun getInventoryItemByBarcodeId(barcodeId: String): InventoryItem?
    suspend fun getInventoryItemsByProductId(productId: String): List<InventoryItem>
    suspend fun getAvailableInventoryItemsByProductId(productId: String): List<InventoryItem>
    suspend fun addInventoryItem(inventoryItem: InventoryItem): String
    suspend fun updateInventoryItem(inventoryItem: InventoryItem): Boolean
    suspend fun deleteInventoryItem(id: String): Boolean
    suspend fun markAsSold(inventoryItemId: String, customerId: String): Boolean
    suspend fun markAsAvailable(inventoryItemId: String): Boolean
    suspend fun generateUniqueBarcodes(quantity: Int, digits: Int): List<String>
    fun generateBarcode(digits: Int): String
}

class FirestoreInventoryRepository(private val firestore: Firestore) : InventoryRepository {

    override suspend fun getAllInventoryItems(): List<InventoryItem> = withContext(Dispatchers.IO) {
        try {
            val inventoryCollection = firestore.collection("inventory")
            val future = inventoryCollection.get()
            val snapshot = future.get()
            
            snapshot.documents.map { doc ->
                val data = doc.data
                InventoryItem(
                    id = doc.id,
                    productId = data["product_id"] as? String ?: "",
                    barcodeId = data["barcode_id"] as? String ?: "",
                    createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            println("Error fetching inventory items: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getInventoryItemById(id: String): InventoryItem? = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("inventory").document(id)
            val future = docRef.get()
            val snapshot = future.get()

            if (snapshot.exists()) {
                val data = snapshot.data
                InventoryItem(
                    id = snapshot.id,
                    productId = data?.get("product_id") as? String ?: "",
                    barcodeId = data?.get("barcode_id") as? String ?: "",
                    createdAt = (data?.get("created_at") as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data?.get("updated_at") as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            println("Error fetching inventory item by ID: ${e.message}")
            null
        }
    }

    override suspend fun getInventoryItemByBarcodeId(barcodeId: String): InventoryItem? = withContext(Dispatchers.IO) {
        try {
            println("🔍 INVENTORY: Searching for barcode in inventory collection")
            println("   - Barcode ID: $barcodeId")
            
            val inventoryCollection = firestore.collection("inventory")
            val query = inventoryCollection.whereEqualTo("barcode_id", barcodeId)
            val future = query.get()
            val snapshot = future.get()

            println("🔍 INVENTORY: Query executed")
            println("   - Snapshot size: ${snapshot.size()}")
            println("   - Is empty: ${snapshot.isEmpty}")

            if (!snapshot.isEmpty) {
                val document = snapshot.documents.first()
                val data = document.data
                
                println("✅ INVENTORY: Found inventory item")
                println("   - Document ID: ${document.id}")
                println("   - Product ID: ${data?.get("product_id")}")
                
                InventoryItem(
                    id = document.id,
                    productId = data?.get("product_id") as? String ?: "",
                    barcodeId = data?.get("barcode_id") as? String ?: "",
                    createdAt = (data?.get("created_at") as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data?.get("updated_at") as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } else {
                println("❌ INVENTORY: No inventory item found with barcode")
                null
            }
        } catch (e: Exception) {
            println("💥 INVENTORY: Exception during barcode search")
            println("   - Barcode: $barcodeId")
            println("   - Error: ${e.message}")
            null
        }
    }

    override suspend fun getInventoryItemsByProductId(productId: String): List<InventoryItem> = withContext(Dispatchers.IO) {
        try {
            val inventoryCollection = firestore.collection("inventory")
            val query = inventoryCollection.whereEqualTo("product_id", productId)
            val future = query.get()
            val snapshot = future.get()
            
            snapshot.documents.map { doc ->
                val data = doc.data
                InventoryItem(
                    id = doc.id,
                    productId = data["product_id"] as? String ?: "",
                    barcodeId = data["barcode_id"] as? String ?: "",
                    createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            println("Error fetching inventory items by product ID: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getAvailableInventoryItemsByProductId(productId: String): List<InventoryItem> = withContext(Dispatchers.IO) {
        try {
            // Since status field is removed, return all inventory items for the product
            val inventoryCollection = firestore.collection("inventory")
            val query = inventoryCollection.whereEqualTo("product_id", productId)
            val future = query.get()
            val snapshot = future.get()
            
            snapshot.documents.map { doc ->
                val data = doc.data
                InventoryItem(
                    id = doc.id,
                    productId = data["product_id"] as? String ?: "",
                    barcodeId = data["barcode_id"] as? String ?: "",
                    createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            println("Error fetching inventory items by product ID: ${e.message}")
            emptyList()
        }
    }

    override suspend fun addInventoryItem(inventoryItem: InventoryItem): String = withContext(Dispatchers.IO) {
        try {
            println("🔍 INVENTORY REPOSITORY: addInventoryItem called")
            println("   - Product ID: ${inventoryItem.productId}")
            println("   - Barcode ID: ${inventoryItem.barcodeId}")
            
            val inventoryCollection = firestore.collection("inventory")
            val docRef = inventoryCollection.document()
            val newInventoryId = docRef.id

            val inventoryMap = mapOf(
                "product_id" to inventoryItem.productId,
                "barcode_id" to inventoryItem.barcodeId,
                "created_at" to (inventoryItem.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()),
                "updated_at" to (inventoryItem.updatedAt.takeIf { it > 0 } ?: System.currentTimeMillis())
            )

            println("🔍 INVENTORY REPOSITORY: About to write to Firestore")
            println("   - Collection: inventory")
            println("   - Document ID: $newInventoryId")
            println("   - Data: $inventoryMap")
            
            docRef.set(inventoryMap).get()
            println("✅ Inventory item created with ID: $newInventoryId")
            newInventoryId
        } catch (e: Exception) {
            println("Error adding inventory item: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun updateInventoryItem(inventoryItem: InventoryItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("inventory").document(inventoryItem.id)

            val inventoryMap = mapOf(
                "product_id" to inventoryItem.productId,
                "barcode_id" to inventoryItem.barcodeId,
                "updated_at" to System.currentTimeMillis()
            )

            docRef.update(inventoryMap).get()
            true
        } catch (e: Exception) {
            println("Error updating inventory item: ${e.message}")
            false
        }
    }

    override suspend fun deleteInventoryItem(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("inventory").document(id).delete().get()
            true
        } catch (e: Exception) {
            println("Error deleting inventory item: ${e.message}")
            false
        }
    }

    override suspend fun markAsSold(inventoryItemId: String, customerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Since status field is removed, just update the timestamp
            val docRef = firestore.collection("inventory").document(inventoryItemId)
            val updateMap = mapOf(
                "updated_at" to System.currentTimeMillis()
            )
            docRef.update(updateMap).get()
            true
        } catch (e: Exception) {
            println("Error marking inventory item as sold: ${e.message}")
            false
        }
    }

    override suspend fun markAsAvailable(inventoryItemId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Since status field is removed, just update the timestamp
            val docRef = firestore.collection("inventory").document(inventoryItemId)
            val updateMap = mapOf(
                "updated_at" to System.currentTimeMillis()
            )
            docRef.update(updateMap).get()
            true
        } catch (e: Exception) {
            println("Error marking inventory item as available: ${e.message}")
            false
        }
    }

    private fun generateRandomBarcode(digits: Int): String {
        val sb = StringBuilder(digits)
        repeat(digits) {
            val d = (0..9).random()
            sb.append(d)
        }
        return sb.toString()
    }
    
    override fun generateBarcode(digits: Int): String {
        return generateRandomBarcode(digits)
    }

    private fun isBarcodeUnique(barcode: String): Boolean {
        val inventoryCollection = firestore.collection("inventory")
        val query = inventoryCollection.whereEqualTo("barcode_id", barcode)
        val snapshot = query.get().get()
        return snapshot.isEmpty
    }

    override suspend fun generateUniqueBarcodes(quantity: Int, digits: Int): List<String> = withContext(Dispatchers.IO) {
        val result = mutableSetOf<String>()
        while (result.size < quantity) {
            val code = generateRandomBarcode(digits)
            if (isBarcodeUnique(code)) {
                result.add(code)
            }
        }
        result.toList()
    }
}
