package org.example.project.data

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreException
import com.google.cloud.firestore.ListenerRegistration
import com.google.cloud.firestore.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.data.normalizeMaterialType

interface MetalRateRepository {
    // StateFlow properties
    val metalRates: StateFlow<List<MetalRate>>
    val currentMetalRates: StateFlow<MetalRates>
    val loading: StateFlow<Boolean>
    val error: StateFlow<String?>
    
    // Material-specific rates (from materials/stones collections)
    suspend fun getAllMetalRates(): List<MetalRate>
    suspend fun getMetalRateById(id: String): MetalRate?
    suspend fun getMetalRateByMaterialAndType(materialId: String, materialType: String): MetalRate?
    suspend fun addMetalRate(metalRate: MetalRate): String
    suspend fun updateMetalRate(metalRate: MetalRate): Boolean
    suspend fun deleteMetalRate(id: String): Boolean
    suspend fun calculateRateForMaterial(materialId: String, materialType: String, karat: Int): Double
    suspend fun updateOrCreateMetalRate(materialId: String, materialName: String, materialType: String, pricePerGram: Double): String
    suspend fun updateMetalRateWithHistory(materialId: String, materialName: String, materialType: String, newPricePerGram: Double): Boolean
    
    // Global market rates (from metal_rates/current document)
    suspend fun getCurrentMetalRates(): MetalRates
    suspend fun updateGoldRates(goldRates: GoldRates): Boolean
    suspend fun updateSilverRates(silverRates: SilverRates): Boolean
    suspend fun getGoldRateForKarat(karat: Int): Double
    suspend fun getSilverRateForPurity(purity: Int): Double
    
    // Lifecycle methods
    fun startListening()
    fun stopListening()
}

object FirestoreMetalRateRepository : MetalRateRepository {
    private lateinit var firestore: Firestore
    private var materialsListenerRegistration: ListenerRegistration? = null
    private var stonesListenerRegistration: ListenerRegistration? = null
    private var metalRatesListenerRegistration: ListenerRegistration? = null
    
    // Coroutine scope for listener callback processing
    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // StateFlow for reactive data
    private val _metalRates = MutableStateFlow<List<MetalRate>>(emptyList())
    override val metalRates: StateFlow<List<MetalRate>> = _metalRates.asStateFlow()
    
    private val _currentMetalRates = MutableStateFlow<MetalRates>(
        MetalRates(
            goldRates = GoldRates.calculateKaratRates(6080.0),
            silverRates = SilverRates.calculateSilverRates(75.0)
        )
    )
    override val currentMetalRates: StateFlow<MetalRates> = _currentMetalRates.asStateFlow()
    
    private val _loading = MutableStateFlow<Boolean>(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()
    
    // Cache for materials and stones to combine into metal rates
    private val _materialsCache = MutableStateFlow<List<com.google.cloud.firestore.DocumentSnapshot>>(emptyList())
    private val _stonesCache = MutableStateFlow<List<com.google.cloud.firestore.DocumentSnapshot>>(emptyList())
    
    fun initialize(firestoreInstance: Firestore) {
        println("🔧 METAL RATE REPOSITORY: Initializing repository object")
        println("   - Thread: ${Thread.currentThread().name}")
        firestore = firestoreInstance
        startListening()
    }
    
    override fun startListening() {
        if (materialsListenerRegistration != null) {
            println("⚠️ METAL RATE REPOSITORY: Listeners already active - NOT creating duplicate listeners")
            println("   - Thread: ${Thread.currentThread().name}")
            println("   - Current metal rates count: ${_metalRates.value.size}")
            return
        }
        
        if (!::firestore.isInitialized) {
            println("❌ METAL RATE REPOSITORY: Firestore not initialized. Call initialize() first.")
            return
        }
        
        println("=".repeat(80))
        println("👂 METAL RATE REPOSITORY: Starting Firestore listeners on materials, stones, and metal_rates collections")
        println("   - Thread: ${Thread.currentThread().name}")
        println("   - Repository instance: ${this.hashCode()}")
        println("   - This is the ONLY set of listeners that will be created")
        println("=".repeat(80))
        _loading.value = true
        
        // Start listening to materials collection
        val materialsCollection = firestore.collection("materials")
        materialsListenerRegistration = materialsCollection.addSnapshotListener { snapshot, exception ->
            println("📡 METAL RATE REPOSITORY: Materials listener callback triggered")
            println("   - Thread: ${Thread.currentThread().name}")
            
            if (exception != null) {
                println("❌ METAL RATE REPOSITORY: Materials listener error: ${exception.message}")
                _error.value = "Failed to load materials: ${exception.message}"
                return@addSnapshotListener
            }
            
            if (snapshot == null) {
                println("⚠️ METAL RATE REPOSITORY: Materials snapshot is null")
                return@addSnapshotListener
            }
            
            // Process snapshot on IO dispatcher
            listenerScope.launch(Dispatchers.IO) {
                try {
                    println("📥 METAL RATE REPOSITORY: Processing materials snapshot from Firestore")
                    println("   - Thread: ${Thread.currentThread().name}")
                    println("   - Document count: ${snapshot.documents.size}")
                    
                    _materialsCache.value = snapshot.documents
                    updateMetalRatesFromCache()
                    
                    println("✅ METAL RATE REPOSITORY: Materials cache updated")
                    println("-".repeat(80))
                } catch (e: Exception) {
                    println("❌ METAL RATE REPOSITORY: Error processing materials documents: ${e.message}")
                    _error.value = "Error processing materials data: ${e.message}"
                }
            }
        }
        
        // Start listening to stones collection
        val stonesCollection = firestore.collection("stones")
        stonesListenerRegistration = stonesCollection.addSnapshotListener { snapshot, exception ->
            println("📡 METAL RATE REPOSITORY: Stones listener callback triggered")
            println("   - Thread: ${Thread.currentThread().name}")
            
            if (exception != null) {
                println("❌ METAL RATE REPOSITORY: Stones listener error: ${exception.message}")
                _error.value = "Failed to load stones: ${exception.message}"
                return@addSnapshotListener
            }
            
            if (snapshot == null) {
                println("⚠️ METAL RATE REPOSITORY: Stones snapshot is null")
                return@addSnapshotListener
            }
            
            // Process snapshot on IO dispatcher
            listenerScope.launch(Dispatchers.IO) {
                try {
                    println("📥 METAL RATE REPOSITORY: Processing stones snapshot from Firestore")
                    println("   - Thread: ${Thread.currentThread().name}")
                    println("   - Document count: ${snapshot.documents.size}")
                    
                    _stonesCache.value = snapshot.documents
                    updateMetalRatesFromCache()
                    
                    println("✅ METAL RATE REPOSITORY: Stones cache updated")
                    println("-".repeat(80))
                } catch (e: Exception) {
                    println("❌ METAL RATE REPOSITORY: Error processing stones documents: ${e.message}")
                    _error.value = "Error processing stones data: ${e.message}"
                }
            }
        }
        
        // Start listening to metal_rates/current document
        val metalRatesDocRef = firestore.collection("metal_rates").document("current")
        metalRatesListenerRegistration = metalRatesDocRef.addSnapshotListener { snapshot, exception ->
            println("📡 METAL RATE REPOSITORY: Metal rates document listener callback triggered")
            println("   - Thread: ${Thread.currentThread().name}")
            
            if (exception != null) {
                println("❌ METAL RATE REPOSITORY: Metal rates listener error: ${exception.message}")
                _error.value = "Failed to load metal rates: ${exception.message}"
                return@addSnapshotListener
            }
            
            if (snapshot == null) {
                println("⚠️ METAL RATE REPOSITORY: Metal rates snapshot is null")
                return@addSnapshotListener
            }
            
            // Process snapshot on IO dispatcher
            listenerScope.launch(Dispatchers.IO) {
                try {
                    println("📥 METAL RATE REPOSITORY: Processing metal rates document from Firestore")
                    println("   - Thread: ${Thread.currentThread().name}")
                    
                    if (snapshot.exists()) {
                        val data = snapshot.data
                        val goldRates = GoldRates(
                            rate24k = (data?.get("gold_rate24k") as? Number)?.toDouble() ?: 6080.0,
                            rate22k = (data?.get("gold_rate22k") as? Number)?.toDouble() ?: 0.0,
                            rate20k = (data?.get("gold_rate20k") as? Number)?.toDouble() ?: 0.0,
                            rate18k = (data?.get("gold_rate18k") as? Number)?.toDouble() ?: 0.0,
                            rate14k = (data?.get("gold_rate14k") as? Number)?.toDouble() ?: 0.0,
                            rate10k = (data?.get("gold_rate10k") as? Number)?.toDouble() ?: 0.0,
                            lastUpdated = (data?.get("gold_lastUpdated") as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                        
                        val silverRates = SilverRates(
                            rate999 = (data?.get("silver_rate999") as? Number)?.toDouble() ?: 75.0,
                            rate925 = (data?.get("silver_rate925") as? Number)?.toDouble() ?: 0.0,
                            rate900 = (data?.get("silver_rate900") as? Number)?.toDouble() ?: 0.0,
                            rate800 = (data?.get("silver_rate800") as? Number)?.toDouble() ?: 0.0,
                            lastUpdated = (data?.get("silver_lastUpdated") as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                        
                        _currentMetalRates.value = MetalRates(
                            goldRates = goldRates,
                            silverRates = silverRates,
                            lastUpdated = (data?.get("lastUpdated") as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                    } else {
                        // Return default rates if no data exists
                        _currentMetalRates.value = MetalRates(
                            goldRates = GoldRates.calculateKaratRates(6080.0),
                            silverRates = SilverRates.calculateSilverRates(75.0)
                        )
                    }
                    
                    _error.value = null
                    _loading.value = false
                    
                    println("✅ METAL RATE REPOSITORY: Current metal rates StateFlow updated successfully")
                    println("-".repeat(80))
                } catch (e: Exception) {
                    println("❌ METAL RATE REPOSITORY: Error parsing metal rates document: ${e.message}")
                    _error.value = "Error parsing metal rates data: ${e.message}"
                    _loading.value = false
                }
            }
        }
        
        println("✅ METAL RATE REPOSITORY: All listeners attached successfully")
        println("   - Materials listener registration: ${materialsListenerRegistration.hashCode()}")
        println("   - Stones listener registration: ${stonesListenerRegistration.hashCode()}")
        println("   - Metal rates listener registration: ${metalRatesListenerRegistration.hashCode()}")
    }
    
    override fun stopListening() {
        println("🛑 METAL RATE REPOSITORY: Stopping Firestore listeners")
        println("   - Thread: ${Thread.currentThread().name}")
        println("   - Repository instance: ${this.hashCode()}")
        materialsListenerRegistration?.remove()
        materialsListenerRegistration = null
        stonesListenerRegistration?.remove()
        stonesListenerRegistration = null
        metalRatesListenerRegistration?.remove()
        metalRatesListenerRegistration = null
        println("✅ METAL RATE REPOSITORY: All Firestore listeners stopped")
    }
    
    // Helper function to combine materials and stones caches into metal rates
    private fun updateMetalRatesFromCache() {
        try {
            val metalRatesList = mutableListOf<MetalRate>()
            
            // Process materials
            _materialsCache.value.forEach { materialDoc ->
                val materialData = materialDoc.data ?: return@forEach
                val materialName = materialData["name"] as? String ?: ""
                val materialId = materialDoc.id
                val typesData = materialData["types"]
                
                when (typesData) {
                    is List<*> -> {
                        typesData.forEach { typeItem ->
                            when (typeItem) {
                                is Map<*, *> -> {
                                    val purity = (typeItem["purity"] as? String) ?: (typeItem["purity"] as? Number)?.toString() ?: ""
                                    val rate = (typeItem["rate"] as? String) ?: (typeItem["rate"] as? Number)?.toString() ?: ""
                                    if (purity.isNotEmpty() && rate.isNotEmpty()) {
                                        val normalizedPurity = normalizeMaterialType(purity)
                                        val rateValue = rate.toDoubleOrNull() ?: 0.0
                                        if (rateValue > 0) {
                                            metalRatesList.add(
                                                MetalRate(
                                                    id = "${materialId}_${normalizedPurity}",
                                                    materialId = materialId,
                                                    materialName = materialName,
                                                    materialType = normalizedPurity,
                                                    karat = extractKaratFromPurity(normalizedPurity),
                                                    pricePerGram = rateValue,
                                                    isActive = true,
                                                    createdAt = (materialData["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                                    updatedAt = System.currentTimeMillis(),
                                                    previousRate = null
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Process stones
            _stonesCache.value.forEach { stoneDoc ->
                val stoneData = stoneDoc.data ?: return@forEach
                val stoneName = stoneData["name"] as? String ?: ""
                val typesData = stoneData["types"]
                
                when (typesData) {
                    is List<*> -> {
                        typesData.forEach { typeItem ->
                            when (typeItem) {
                                is Map<*, *> -> {
                                    val purity = (typeItem["purity"] as? String) ?: (typeItem["purity"] as? Number)?.toString() ?: ""
                                    val rate = (typeItem["rate"] as? String) ?: (typeItem["rate"] as? Number)?.toString() ?: ""
                                    if (purity.isNotEmpty() && rate.isNotEmpty()) {
                                        val normalizedPurity = purity.trim().uppercase()
                                        val rateValue = rate.toDoubleOrNull() ?: 0.0
                                        if (rateValue > 0) {
                                            metalRatesList.add(
                                                MetalRate(
                                                    id = "${stoneDoc.id}_${normalizedPurity}",
                                                    materialId = "",
                                                    materialName = stoneName,
                                                    materialType = normalizedPurity,
                                                    karat = 0,
                                                    pricePerGram = rateValue,
                                                    isActive = true,
                                                    createdAt = (stoneData["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                                    updatedAt = System.currentTimeMillis(),
                                                    previousRate = null
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            println("✅ METAL RATE REPOSITORY: Combined ${metalRatesList.size} metal rates from materials and stones")
            println("   - Updating StateFlow (this will notify all ViewModels)")
            println("   - Previous metal rates count: ${_metalRates.value.size}")
            
            _metalRates.value = metalRatesList
            _error.value = null
            _loading.value = false
            
            println("✅ METAL RATE REPOSITORY: Metal rates StateFlow updated successfully")
            println("   - New metal rates count: ${metalRatesList.size}")
            println("   - All ViewModels using this repository will receive this update automatically")
        } catch (e: Exception) {
            println("❌ METAL RATE REPOSITORY: Error combining metal rates: ${e.message}")
            _error.value = "Error combining metal rates: ${e.message}"
            _loading.value = false
        }
    }
    
    // Helper function to extract karat from purity string
    private fun extractKaratFromPurity(purity: String): Int {
        val normalized = normalizeMaterialType(purity)
        val regex = Regex("""(\d+)K""")
        val match = regex.find(normalized)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 24
    }

    override suspend fun getAllMetalRates(): List<MetalRate> = withContext(Dispatchers.IO) {
        // Use cached data from StateFlow
        _metalRates.value
    }

    override suspend fun getMetalRateById(id: String): MetalRate? = withContext(Dispatchers.IO) {
        try {
            // First check cached data from StateFlow
            val cachedRate = _metalRates.value.find { it.id == id }
            if (cachedRate != null) {
                return@withContext cachedRate
            }
            
            // Fallback to parsing ID and querying
            // ID format: "materialId_materialType" or "stoneName_purity"
            val parts = id.split("_", limit = 2)
            if (parts.size == 2) {
                val materialIdOrName = parts[0]
                val materialType = parts[1]
                return@withContext getMetalRateByMaterialAndType(materialIdOrName, materialType)
            }
            null
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            println("❌ METAL RATE REPOSITORY: Firestore error loading metal rate by ID: ${e.message}")
            null
        } catch (e: Exception) {
            println("❌ METAL RATE REPOSITORY: Error loading metal rate by ID: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun getMetalRateByMaterialAndType(materialId: String, materialType: String): MetalRate? = withContext(Dispatchers.IO) {
        try {
            // Normalize material type for comparison (22, 22K, 22k should be same)
            val normalizedMaterialType = normalizeMaterialType(materialType)
            
            // First check cached data from StateFlow
            val cachedRate = _metalRates.value.find { rate ->
                if (materialId.isEmpty()) {
                    // For stones (no materialId), match by materialName and materialType
                    rate.materialId.isEmpty() && 
                    normalizeMaterialType(rate.materialType) == normalizedMaterialType
                } else {
                    // For metals, match by materialId and materialType
                    rate.materialId == materialId && 
                    normalizeMaterialType(rate.materialType) == normalizedMaterialType
                }
            }
            
            if (cachedRate != null) {
                return@withContext cachedRate
            }
            
            // Fallback to direct query if not in cache
            if (materialId.isEmpty()) {
                // For stones (no materialId), search in stones collection
                val stonesCollection = firestore.collection("stones")
                val stonesSnapshot = stonesCollection.get().get()
                
                stonesSnapshot.documents.forEach { stoneDoc ->
                    val stoneData = stoneDoc.data
                    val stoneName = stoneData["name"] as? String ?: ""
                    val typesData = stoneData["types"]
                    
                    when (typesData) {
                        is List<*> -> {
                            typesData.forEach { typeItem ->
                                when (typeItem) {
                                    is Map<*, *> -> {
                                        val purity = (typeItem["purity"] as? String) ?: (typeItem["purity"] as? Number)?.toString() ?: ""
                                        val normalizedPurity = purity.trim().uppercase()
                                        if (normalizedPurity == normalizedMaterialType || 
                                            (materialType.isNotEmpty() && stoneName.equals(materialType, ignoreCase = true))) {
                                            val rate = (typeItem["rate"] as? String) ?: (typeItem["rate"] as? Number)?.toString() ?: ""
                                            val rateValue = rate.toDoubleOrNull() ?: 0.0
                                            if (rateValue > 0) {
                                                return@withContext MetalRate(
                                                    id = "${stoneDoc.id}_${normalizedPurity}",
                                                    materialId = "",
                                                    materialName = stoneName,
                                                    materialType = normalizedPurity,
                                                    karat = 0,
                                                    pricePerGram = rateValue,
                                                    isActive = true,
                                                    createdAt = (stoneData["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                                    updatedAt = System.currentTimeMillis()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // For metals, search in materials collection
                val materialDocRef = firestore.collection("materials").document(materialId)
                val materialDoc = materialDocRef.get().get()
                
                if (materialDoc.exists()) {
                    val materialData = materialDoc.data
                    val materialName = materialData?.get("name") as? String ?: ""
                    val typesData = materialData?.get("types")
                    
                    when (typesData) {
                        is List<*> -> {
                            typesData.forEach { typeItem ->
                                when (typeItem) {
                                    is Map<*, *> -> {
                                        val purity = (typeItem["purity"] as? String) ?: (typeItem["purity"] as? Number)?.toString() ?: ""
                                        val normalizedPurity = normalizeMaterialType(purity)
                                        if (normalizedPurity == normalizedMaterialType) {
                                            val rate = (typeItem["rate"] as? String) ?: (typeItem["rate"] as? Number)?.toString() ?: ""
                                            val rateValue = rate.toDoubleOrNull() ?: 0.0
                                            if (rateValue > 0) {
                                                return@withContext MetalRate(
                                                    id = "${materialId}_${normalizedPurity}",
                                                    materialId = materialId,
                                                    materialName = materialName,
                                                    materialType = normalizedPurity,
                                                    karat = extractKaratFromPurity(normalizedPurity),
                                                    pricePerGram = rateValue,
                                                    isActive = true,
                                                    createdAt = (materialData?.get("created_at") as? Number)?.toLong() ?: System.currentTimeMillis(),
                                                    updatedAt = System.currentTimeMillis()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            null
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            println("❌ Firestore error loading metal rate by material and type: ${e.message}")
            e.printStackTrace()
            null
        } catch (e: Exception) {
            println("❌ Error loading metal rate by material and type: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun addMetalRate(metalRate: MetalRate): String = withContext(Dispatchers.IO) {
        try {
            // Rates are now stored in materials/stones collections, not in rates collection
            // This method is kept for backward compatibility but now updates materials/stones collections
            if (metalRate.materialId.isNotEmpty()) {
                // Update materials collection
                updateMaterialTypesArray(metalRate.materialId, metalRate.materialType, metalRate.pricePerGram.toString())
                // Listener will automatically update _metalRates - no manual refresh needed
                println("✅ METAL RATE REPOSITORY: Metal rate added to materials collection")
                println("   - Listener will automatically detect this change and update StateFlow")
                "${metalRate.materialId}_${metalRate.materialType}"
            } else {
                // Update stones collection
                updateStoneTypesArray(metalRate.materialName, metalRate.materialType, metalRate.pricePerGram.toString())
                // Listener will automatically update _metalRates - no manual refresh needed
                println("✅ METAL RATE REPOSITORY: Stone rate added to stones collection")
                println("   - Listener will automatically detect this change and update StateFlow")
                "${metalRate.materialName}_${metalRate.materialType}"
            }
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            println("❌ Firestore error adding metal rate: ${e.message}")
            e.printStackTrace()
            throw e
        } catch (e: Exception) {
            println("❌ Error adding metal rate: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun updateMetalRate(metalRate: MetalRate): Boolean = withContext(Dispatchers.IO) {
        try {
            // Rates are now stored in materials/stones collections, not in rates collection
            // This method is kept for backward compatibility but now updates materials/stones collections
            if (metalRate.materialId.isNotEmpty()) {
                // Update materials collection
                updateMaterialTypesArray(metalRate.materialId, metalRate.materialType, metalRate.pricePerGram.toString())
                // Listener will automatically update _metalRates - no manual refresh needed
                println("✅ METAL RATE REPOSITORY: Metal rate updated in materials collection")
                println("   - Listener will automatically detect this change and update StateFlow")
            } else {
                // Update stones collection
                updateStoneTypesArray(metalRate.materialName, metalRate.materialType, metalRate.pricePerGram.toString())
                // Listener will automatically update _metalRates - no manual refresh needed
                println("✅ METAL RATE REPOSITORY: Stone rate updated in stones collection")
                println("   - Listener will automatically detect this change and update StateFlow")
            }
            println("✅ Metal rate updated: ${metalRate.materialName} ${metalRate.materialType}")
            true
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            println("❌ Firestore error updating metal rate: ${e.message}")
            e.printStackTrace()
            false
        } catch (e: Exception) {
            println("❌ Error updating metal rate: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override suspend fun deleteMetalRate(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Rates are now stored in materials/stones collections
            // Parse the ID to get materialId/stoneName and materialType
            // Format: "materialId_materialType" or "stoneName_purity"
            val parts = id.split("_", limit = 2)
            if (parts.size != 2) {
                return@withContext false
            }
            
                val materialIdOrName = parts[0]
                val materialType = parts[1]
                
            // Try materials collection first using transaction
                val materialDocRef = firestore.collection("materials").document(materialIdOrName)
            
            try {
                // Use transaction to atomically check existence and update types array
                // This prevents race condition where document could be deleted between check and transaction
                firestore.runTransaction { transaction: Transaction ->
                    val materialDocSnapshot = transaction.getDocument(materialDocRef)
                    if (!materialDocSnapshot.exists()) {
                        throw Exception("Material document not found: $materialIdOrName")
                    }
                    
                    val data = materialDocSnapshot.data ?: throw Exception("Material document has no data")
                    val existingTypes = mutableListOf<Map<String, String>>()
                    val typesData = data["types"]
                    val normalizedMaterialType = normalizeMaterialType(materialType)
                    
                    when (typesData) {
                        is List<*> -> {
                            typesData.forEach { typeItem ->
                                when (typeItem) {
                                    is Map<*, *> -> {
                                        val purity = (typeItem["purity"] as? String) ?: (typeItem["purity"] as? Number)?.toString() ?: ""
                                        val normalizedPurity = normalizeMaterialType(purity)
                                        // Only add if it's not the one we're deleting
                                        if (normalizedPurity != normalizedMaterialType) {
                                            val rate = (typeItem["rate"] as? String) ?: (typeItem["rate"] as? Number)?.toString() ?: ""
                                            existingTypes.add(mapOf("purity" to normalizedPurity, "rate" to rate))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    transaction.update(materialDocRef, "types", existingTypes)
                    // Listener will automatically update _metalRates - no manual refresh needed
                    println("✅ METAL RATE REPOSITORY: Metal rate deleted from materials collection: $id")
                    println("   - Listener will automatically detect this change and update StateFlow")
                    Unit
                }.get()
                    return@withContext true
            } catch (e: Exception) {
                // Material not found in materials collection, try stones collection
                // Continue to stones collection logic below
            }
            
            // Try stones collection - need to query first (outside transaction)
                    val stonesCollection = firestore.collection("stones")
                    val query = stonesCollection.whereEqualTo("name", materialIdOrName).limit(1)
                    val snapshot = query.get().get()
                    
                    if (!snapshot.isEmpty) {
                        val stoneDoc = snapshot.documents.first()
                val stoneDocRef = stoneDoc.reference
                
                // Use transaction to atomically read and update types array
                firestore.runTransaction { transaction: Transaction ->
                    val stoneDocSnapshot = transaction.getDocument(stoneDocRef)
                    if (!stoneDocSnapshot.exists()) {
                        throw Exception("Stone document not found: $materialIdOrName")
                    }
                    
                    val stoneData = stoneDocSnapshot.data ?: throw Exception("Stone document has no data")
                        val existingTypes = mutableListOf<Map<String, String>>()
                        val typesData = stoneData["types"]
                        val normalizedMaterialType = materialType.trim().uppercase()
                        
                        when (typesData) {
                            is List<*> -> {
                                typesData.forEach { typeItem ->
                                    when (typeItem) {
                                        is Map<*, *> -> {
                                            val purity = (typeItem["purity"] as? String) ?: (typeItem["purity"] as? Number)?.toString() ?: ""
                                            val normalizedPurity = purity.trim().uppercase()
                                            // Only add if it's not the one we're deleting
                                            if (normalizedPurity != normalizedMaterialType) {
                                                val rate = (typeItem["rate"] as? String) ?: (typeItem["rate"] as? Number)?.toString() ?: ""
                                                existingTypes.add(mapOf("purity" to normalizedPurity, "rate" to rate))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    
                    transaction.update(stoneDocRef, "types", existingTypes)
                        // Listener will automatically update _metalRates - no manual refresh needed
                        println("✅ METAL RATE REPOSITORY: Stone rate deleted from stones collection: $id")
                        println("   - Listener will automatically detect this change and update StateFlow")
                    Unit
                }.get()
                        return@withContext true
            }
            
            false
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            println("❌ Firestore error deleting metal rate: ${e.message}")
            e.printStackTrace()
            false
        } catch (e: Exception) {
            println("❌ Error deleting metal rate: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override suspend fun calculateRateForMaterial(materialId: String, materialType: String, karat: Int): Double = withContext(Dispatchers.IO) {
        try {
            val metalRate = getMetalRateByMaterialAndType(materialId, materialType)
            if (metalRate != null) {
                // Use the exact stored rate for this material/type directly to avoid
                // recalculating when Firestore already holds the per-gram price.
                println("💰 Using Firestore rate for $materialType (no recalculation): ${metalRate.pricePerGram}")
                metalRate.pricePerGram
            } else {
                println("⚠️ No metal rate found for material: $materialId, type: $materialType")
                0.0
            }
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            println("❌ Firestore error calculating rate: ${e.message}")
            e.printStackTrace()
            0.0
        } catch (e: Exception) {
            println("❌ Error calculating rate: ${e.message}")
            e.printStackTrace()
            0.0
        }
    }

    override suspend fun updateOrCreateMetalRate(materialId: String, materialName: String, materialType: String, pricePerGram: Double): String = withContext(Dispatchers.IO) {
        try {
            // Normalize material type
            val normalizedPurity = normalizeMaterialType(materialType)
            
            if (materialId.isNotEmpty()) {
                // Update or create in materials collection
                updateMaterialTypesArray(materialId, normalizedPurity, pricePerGram.toString())
                // Listener will automatically update _metalRates - no manual refresh needed
                println("✅ METAL RATE REPOSITORY: Updated/created metal rate in materials collection: ${materialName} ${normalizedPurity}")
                println("   - Listener will automatically detect this change and update StateFlow")
                "${materialId}_${normalizedPurity}"
            } else {
                // Update or create in stones collection
                updateStoneTypesArray(materialName, normalizedPurity, pricePerGram.toString())
                // Listener will automatically update _metalRates - no manual refresh needed
                println("✅ METAL RATE REPOSITORY: Updated/created stone rate in stones collection: ${materialName} ${normalizedPurity}")
                println("   - Listener will automatically detect this change and update StateFlow")
                "${materialName}_${normalizedPurity}"
            }
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            println("❌ Firestore error updating or creating metal rate: ${e.message}")
            e.printStackTrace()
            throw e
        } catch (e: Exception) {
            println("❌ Error updating or creating metal rate: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun updateMetalRateWithHistory(materialId: String, materialName: String, materialType: String, newPricePerGram: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            println("🔄 Updating metal rate: $materialName $materialType -> $newPricePerGram per gram")
            
            // Normalize purity (22, 22K, 22k should be same)
            val normalizedPurity = normalizeMaterialType(materialType)
            
            if (materialId.isNotEmpty()) {
                // Update materials collection
                updateMaterialTypesArray(materialId, normalizedPurity, newPricePerGram.toString())
                // Listener will automatically update _metalRates - no manual refresh needed
                println("✅ METAL RATE REPOSITORY: Updated metal rate in materials collection: ${materialName} ${normalizedPurity} -> $newPricePerGram per gram")
                println("   - Listener will automatically detect this change and update StateFlow")
                true
            } else {
                // Update stones collection
                updateStoneTypesArray(materialName, normalizedPurity, newPricePerGram.toString())
                // Listener will automatically update _metalRates - no manual refresh needed
                println("✅ METAL RATE REPOSITORY: Updated stone rate in stones collection: ${materialName} ${normalizedPurity} -> $newPricePerGram per gram")
                println("   - Listener will automatically detect this change and update StateFlow")
                true
            }
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            println("❌ Firestore error updating metal rate with history: ${e.message}")
            e.printStackTrace()
            false
        } catch (e: Exception) {
            println("❌ Error updating metal rate with history: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Updates the materials collection with the new types array format
     * Structure: types: [{purity: "24", rate: "5000"}, {purity: "22", rate: "4500"}]
     */
    private suspend fun updateMaterialTypesArray(materialId: String, purity: String, rate: String) = withContext(Dispatchers.IO) {
        try {
            if (materialId.isEmpty()) {
                println("⚠️ Material ID is empty, skipping materials collection update")
                return@withContext
            }
            
            val materialDocRef = firestore.collection("materials").document(materialId)
            
            // Use transaction to atomically read and update types array
            firestore.runTransaction { transaction: Transaction ->
                val materialDocSnapshot = transaction.getDocument(materialDocRef)
                if (!materialDocSnapshot.exists()) {
                    throw Exception("Material document not found: $materialId")
            }
            
                val data = materialDocSnapshot.data ?: throw Exception("Material document has no data")
            val existingTypes = mutableListOf<Map<String, String>>()
            
            // Parse existing types array
            val typesData = data["types"]
            when (typesData) {
                is List<*> -> {
                    typesData.forEach { typeItem ->
                        when (typeItem) {
                            is Map<*, *> -> {
                                val existingPurity = (typeItem["purity"] as? String) ?: (typeItem["purity"] as? Number)?.toString() ?: ""
                                val normalizedExistingPurity = normalizeMaterialType(existingPurity)
                                // Only add if it's not the purity we're updating
                                if (normalizedExistingPurity != purity) {
                                    val existingRate = (typeItem["rate"] as? String) ?: (typeItem["rate"] as? Number)?.toString() ?: ""
                                    existingTypes.add(mapOf("purity" to normalizedExistingPurity, "rate" to existingRate))
                                }
                            }
                            is String -> {
                                // Old format: just string types
                                val normalizedExistingPurity = normalizeMaterialType(typeItem)
                                if (normalizedExistingPurity != purity) {
                                    existingTypes.add(mapOf("purity" to normalizedExistingPurity, "rate" to ""))
                                }
                            }
                        }
                    }
                }
            }
            
            // Add or update the current purity/rate
            existingTypes.add(mapOf("purity" to purity, "rate" to rate))
            
            // Update the material document with the new types array
                transaction.update(materialDocRef, "types", existingTypes)
            println("✅ Updated materials collection: $materialId with purity $purity and rate $rate")
                Unit
            }.get()
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            println("❌ Firestore error updating materials collection: ${e.message}")
            e.printStackTrace()
            throw e
        } catch (e: Exception) {
            println("❌ Error updating materials collection: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Updates the stones collection with the new types array format
     * Structure: types: [{purity: "VVS", rate: "5000"}, {purity: "VS", rate: "4500"}]
     */
    suspend fun updateStoneTypesArray(stoneName: String, purity: String, rate: String) = withContext(Dispatchers.IO) {
        try {
            // Find stone document by name (query outside transaction since transactions can't query)
            val stonesCollection = firestore.collection("stones")
            val query = stonesCollection.whereEqualTo("name", stoneName).limit(1)
            val snapshot = query.get().get()
            
            if (snapshot.isEmpty) {
                println("⚠️ Stone document not found: $stoneName")
                return@withContext
            }
            
            val stoneDoc = snapshot.documents.first()
            val stoneDocRef = stoneDoc.reference
            
            // Use transaction to atomically read and update types array
            firestore.runTransaction { transaction: Transaction ->
                val stoneDocSnapshot = transaction.getDocument(stoneDocRef)
                if (!stoneDocSnapshot.exists()) {
                    throw Exception("Stone document not found: $stoneName")
                }
                
                val data = stoneDocSnapshot.data ?: throw Exception("Stone document has no data")
            val existingTypes = mutableListOf<Map<String, String>>()
            
            // Parse existing types array
            val typesData = data["types"]
                val normalizedPurity = purity.trim().uppercase()
                
            when (typesData) {
                is List<*> -> {
                    typesData.forEach { typeItem ->
                        when (typeItem) {
                            is Map<*, *> -> {
                                val existingPurity = (typeItem["purity"] as? String) ?: (typeItem["purity"] as? Number)?.toString() ?: ""
                                // For stones, normalize purity (case-insensitive comparison)
                                val normalizedExistingPurity = existingPurity.trim().uppercase()
                                // Only add if it's not the purity we're updating
                                if (normalizedExistingPurity != normalizedPurity) {
                                    val existingRate = (typeItem["rate"] as? String) ?: (typeItem["rate"] as? Number)?.toString() ?: ""
                                    existingTypes.add(mapOf("purity" to normalizedExistingPurity, "rate" to existingRate))
                                }
                            }
                            is String -> {
                                // Old format: just string types
                                val normalizedExistingPurity = typeItem.trim().uppercase()
                                if (normalizedExistingPurity != normalizedPurity) {
                                    existingTypes.add(mapOf("purity" to normalizedExistingPurity, "rate" to ""))
                                }
                            }
                        }
                    }
                }
            }
            
            // Add or update the current purity/rate (normalized)
            existingTypes.add(mapOf("purity" to normalizedPurity, "rate" to rate))
            
            // Update the stone document with the new types array
                transaction.update(stoneDocRef, "types", existingTypes)
            println("✅ Updated stones collection: $stoneName with purity $normalizedPurity and rate $rate")
                Unit
            }.get()
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            println("❌ Firestore error updating stones collection: ${e.message}")
            e.printStackTrace()
            throw e
        } catch (e: Exception) {
            println("❌ Error updating stones collection: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    // ========== Global Market Rates Methods (from MetalRatesRepository) ==========
    
    override suspend fun getCurrentMetalRates(): MetalRates = withContext(Dispatchers.IO) {
        // Use cached data from StateFlow
        _currentMetalRates.value
    }

    override suspend fun updateGoldRates(goldRates: GoldRates): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("metal_rates").document("current")
            val ratesMap = mapOf(
                "gold_rate24k" to goldRates.rate24k,
                "gold_rate22k" to goldRates.rate22k,
                "gold_rate20k" to goldRates.rate20k,
                "gold_rate18k" to goldRates.rate18k,
                "gold_rate14k" to goldRates.rate14k,
                "gold_rate10k" to goldRates.rate10k,
                "gold_lastUpdated" to goldRates.lastUpdated,
                "lastUpdated" to System.currentTimeMillis()
            )
            docRef.set(ratesMap, com.google.cloud.firestore.SetOptions.merge()).get()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateSilverRates(silverRates: SilverRates): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("metal_rates").document("current")
            val ratesMap = mapOf(
                "silver_rate999" to silverRates.rate999,
                "silver_rate925" to silverRates.rate925,
                "silver_rate900" to silverRates.rate900,
                "silver_rate800" to silverRates.rate800,
                "silver_lastUpdated" to silverRates.lastUpdated,
                "lastUpdated" to System.currentTimeMillis()
            )
            docRef.set(ratesMap, com.google.cloud.firestore.SetOptions.merge()).get()
            // Listener will automatically update _currentMetalRates - no manual refresh needed
            println("✅ METAL RATE REPOSITORY: Silver rates updated in Firestore")
            println("   - Listener will automatically detect this change and update StateFlow")
            true
        } catch (e: Exception) {
            println("❌ METAL RATE REPOSITORY: Failed to update silver rates: ${e.message}")
            false
        }
    }

    override suspend fun getGoldRateForKarat(karat: Int): Double = withContext(Dispatchers.IO) {
        // Use cached data from StateFlow
        _currentMetalRates.value.getGoldRateForKarat(karat)
    }

    override suspend fun getSilverRateForPurity(purity: Int): Double = withContext(Dispatchers.IO) {
        // Use cached data from StateFlow
        _currentMetalRates.value.getSilverRateForPurity(purity)
    }
}
