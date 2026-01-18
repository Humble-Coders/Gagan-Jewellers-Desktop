package org.example.project.data

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreException
import com.google.cloud.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface InventoryRepository {
    val inventoryItems: StateFlow<List<InventoryItem>>
    val loading: StateFlow<Boolean>
    val error: StateFlow<String?>
    
    suspend fun getInventoryItemById(id: String): InventoryItem?
    suspend fun getInventoryItemByBarcodeId(barcodeId: String): InventoryItem?
    suspend fun getInventoryItemsByProductId(productId: String): List<InventoryItem>
    suspend fun getAvailableInventoryItemsByProductId(productId: String): List<InventoryItem>
    suspend fun addInventoryItem(inventoryItem: InventoryItem): String
    suspend fun updateInventoryItem(inventoryItem: InventoryItem): Boolean
    suspend fun deleteInventoryItem(id: String): Boolean
    suspend fun generateUniqueBarcodes(quantity: Int, digits: Int): List<String>
    fun generateBarcode(digits: Int): String
    
    // Lifecycle methods
    fun startListening()
    fun stopListening()
}

object FirestoreInventoryRepository : InventoryRepository {
    private lateinit var firestore: Firestore
    private var listenerRegistration: ListenerRegistration? = null
    
    // Coroutine scope for listener callback processing
    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // StateFlow for reactive data
    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    override val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()
    
    private val _loading = MutableStateFlow<Boolean>(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()
    
    fun initialize(firestoreInstance: Firestore) {
        println("🔧 INVENTORY REPOSITORY: Initializing repository object")
        println("   - Thread: ${Thread.currentThread().name}")
        firestore = firestoreInstance
        startListening()
    }
    
    override fun startListening() {
        if (listenerRegistration != null) {
            println("⚠️ INVENTORY REPOSITORY: Listener already active - NOT creating duplicate listener")
            println("   - Thread: ${Thread.currentThread().name}")
            println("   - Current inventory items count: ${_inventoryItems.value.size}")
            return
        }
        
        if (!::firestore.isInitialized) {
            println("❌ INVENTORY REPOSITORY: Firestore not initialized. Call initialize() first.")
            return
        }
        
        println("=".repeat(80))
        println("👂 INVENTORY REPOSITORY: Starting Firestore listener on inventory collection")
        println("   - Thread: ${Thread.currentThread().name}")
        println("   - Repository instance: ${this.hashCode()}")
        println("   - This is the ONLY listener that will be created")
        println("=".repeat(80))
        _loading.value = true
        
        val inventoryCollection = firestore.collection("inventory")
        
        listenerRegistration = inventoryCollection.addSnapshotListener { snapshot, exception ->
            println("📡 INVENTORY REPOSITORY: Listener callback triggered")
            println("   - Thread: ${Thread.currentThread().name}")
            println("   - Repository instance: ${this.hashCode()}")
            
            if (exception != null) {
                println("❌ INVENTORY REPOSITORY: Listener error: ${exception.message}")
                _error.value = "Failed to load inventory: ${exception.message}"
                _loading.value = false
                return@addSnapshotListener
            }
            
            if (snapshot == null) {
                println("⚠️ INVENTORY REPOSITORY: Snapshot is null")
                _loading.value = false
                return@addSnapshotListener
            }
            
            // Process snapshot on IO dispatcher for consistency with other repository methods
            listenerScope.launch(Dispatchers.IO) {
                try {
                    println("📥 INVENTORY REPOSITORY: Processing snapshot from Firestore")
                    println("   - Thread: ${Thread.currentThread().name}")
                    println("   - Document count: ${snapshot.documents.size}")
                    
                    val inventoryList = snapshot.documents.mapNotNull { doc ->
                        parseInventoryItemDocument(doc)
                    }
                    
                    println("✅ INVENTORY REPOSITORY: Parsed ${inventoryList.size} inventory items from snapshot")
                    println("   - Updating StateFlow (this will notify all ViewModels)")
                    println("   - Previous inventory items count: ${_inventoryItems.value.size}")
                    
                    _inventoryItems.value = inventoryList
                    _error.value = null
                    _loading.value = false
                    
                    println("✅ INVENTORY REPOSITORY: StateFlow updated successfully")
                    println("   - New inventory items count: ${inventoryList.size}")
                    println("   - All ViewModels using this repository will receive this update automatically")
                    println("   - Repository instance: ${this.hashCode()}")
                    println("-".repeat(80))
                } catch (e: Exception) {
                    println("❌ INVENTORY REPOSITORY: Error parsing documents: ${e.message}")
                    _error.value = "Error parsing inventory data: ${e.message}"
                    _loading.value = false
                }
            }
        }
        
        println("✅ INVENTORY REPOSITORY: Listener attached successfully")
        println("   - Listener registration: ${listenerRegistration.hashCode()}")
    }
    
    override fun stopListening() {
        println(" INVENTORY REPOSITORY: Stopping Firestore listener")
        println("   - Thread: ${Thread.currentThread().name}")
        println("   - Repository instance: ${this.hashCode()}")
        listenerRegistration?.remove()
        listenerRegistration = null
        println("✅ INVENTORY REPOSITORY: Firestore listener stopped")
    }
    
    private fun parseInventoryItemDocument(doc: com.google.cloud.firestore.DocumentSnapshot): InventoryItem? {
        val data = doc.data ?: return null
        
        return InventoryItem(
            id = doc.id,
            productId = data["product_id"] as? String ?: "",
            barcodeId = data["barcode_id"] as? String ?: "",
            createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
    
    override suspend fun getInventoryItemById(id: String): InventoryItem? = withContext(Dispatchers.IO) {
        // First check cached data
        _inventoryItems.value.find { it.id == id }
            ?: run {
                // Fallback to direct query if not in cache
                try {
                    val docRef = firestore.collection("inventory").document(id)
                    val doc = docRef.get().get()
                    if (doc.exists()) parseInventoryItemDocument(doc) else null
                } catch (e: Exception) {
                    println("❌ INVENTORY REPOSITORY: Error fetching inventory item by ID: ${e.message}")
                    null
                }
            }
    }


    override suspend fun getInventoryItemByBarcodeId(barcodeId: String): InventoryItem? = withContext(Dispatchers.IO) {
        try {
            println("🔍 INVENTORY: Searching for barcode in cached inventory")
            println("   - Barcode ID: $barcodeId")
            
            // First check cached data
            val cachedItem = _inventoryItems.value.find { it.barcodeId == barcodeId }
            if (cachedItem != null) {
                println("✅ INVENTORY: Found inventory item in cache")
                return@withContext cachedItem
            }
            
            // Fallback to direct query if not in cache
            println("🔍 INVENTORY: Barcode not in cache, querying Firestore")
            val inventoryCollection = firestore.collection("inventory")
            val query = inventoryCollection.whereEqualTo("barcode_id", barcodeId)
            val snapshot = query.get().get()

            if (!snapshot.isEmpty) {
                val document = snapshot.documents.first()
                val item = parseInventoryItemDocument(document)
                println("✅ INVENTORY: Found inventory item in Firestore")
                item
            } else {
                println("❌ INVENTORY: No inventory item found with barcode")
                null
            }
        } catch (e: Exception) {
            println("💥 INVENTORY: Exception during barcode search: ${e.message}")
            null
        }
    }

    override suspend fun getInventoryItemsByProductId(productId: String): List<InventoryItem> = withContext(Dispatchers.IO) {
        // Use cached data from StateFlow
        _inventoryItems.value.filter { it.productId == productId }
    }

    override suspend fun getAvailableInventoryItemsByProductId(productId: String): List<InventoryItem> = withContext(Dispatchers.IO) {
        // Since status field is removed, return all inventory items for the product from cache
        _inventoryItems.value.filter { it.productId == productId }
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
            
            docRef.set(inventoryMap).get()
            // Listener will automatically update _inventoryItems - no manual refresh needed
            println("✅ INVENTORY REPOSITORY: Inventory item added to Firestore")
            println("   - Document ID: $newInventoryId")
            println("   - Listener will automatically detect this change and update StateFlow")
            newInventoryId
        } catch (e: Exception) {
            println("❌ INVENTORY REPOSITORY: Failed to add inventory item: ${e.message}")
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
            // Listener will automatically update _inventoryItems - no manual refresh needed
            println("✅ INVENTORY REPOSITORY: Inventory item updated in Firestore")
            println("   - Listener will automatically detect this change and update StateFlow")
            true
        } catch (e: Exception) {
            println("❌ INVENTORY REPOSITORY: Failed to update inventory item: ${e.message}")
            false
        }
    }

    override suspend fun deleteInventoryItem(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("inventory").document(id).delete().get()
            // Listener will automatically update _inventoryItems - no manual refresh needed
            println("✅ INVENTORY REPOSITORY: Inventory item deleted from Firestore")
            println("   - Listener will automatically detect this change and update StateFlow")
            true
        } catch (e: Exception) {
            println("❌ INVENTORY REPOSITORY: Failed to delete inventory item: ${e.message}")
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

    private suspend fun isBarcodeUnique(barcode: String): Boolean = withContext(Dispatchers.IO) {
        // Check cached data first
        val existsInCache = _inventoryItems.value.any { it.barcodeId == barcode }
        if (existsInCache) {
            return@withContext false
        }
        
        // Fallback to direct query if not in cache (for race conditions)
        val inventoryCollection = firestore.collection("inventory")
        val query = inventoryCollection.whereEqualTo("barcode_id", barcode)
        val snapshot = query.get().get()
        snapshot.isEmpty
    }

    override suspend fun generateUniqueBarcodes(quantity: Int, digits: Int): List<String> = withContext(Dispatchers.IO) {
        val result = mutableSetOf<String>()
        val maxIterations = quantity * 100 // Prevent infinite loop: try up to 100x the quantity
        var iterations = 0
        
        while (result.size < quantity && iterations < maxIterations) {
            iterations++
            val code = generateRandomBarcode(digits)
            if (isBarcodeUnique(code)) {
                result.add(code)
            }
        }
        
        if (result.size < quantity) {
            println("⚠️ WARNING: Could not generate $quantity unique barcodes after $iterations attempts")
            println("   - Generated: ${result.size} unique barcodes")
            println("   - This may indicate that most barcodes with $digits digits are already taken")
        }
        
        result.toList()
    }
}
