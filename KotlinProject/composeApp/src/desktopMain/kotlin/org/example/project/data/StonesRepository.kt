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
    val stones: StateFlow<List<Stone>>
    val loading: StateFlow<Boolean>
    val error: StateFlow<String?>
    
    suspend fun getAllStoneNames(): List<String>
    suspend fun getAllStonePurities(): List<String>
    suspend fun getAllStones(): List<Stone>
    suspend fun addStone(name: String): String
    
    // Lifecycle methods
    fun startListening()
    fun stopListening()
}

object FirestoreStonesRepository : StonesRepository {
    private lateinit var firestore: Firestore
    private var listenerRegistration: ListenerRegistration? = null
    
    // Coroutine scope for listener callback processing
    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // StateFlow for reactive data
    private val _stones = MutableStateFlow<List<Stone>>(emptyList())
    override val stones: StateFlow<List<Stone>> = _stones.asStateFlow()
    
    private val _loading = MutableStateFlow<Boolean>(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()
    
    fun initialize(firestoreInstance: Firestore) {
        println("🔧 STONES REPOSITORY: Initializing repository object")
        println("   - Thread: ${Thread.currentThread().name}")
        firestore = firestoreInstance
        startListening()
    }
    
    override fun startListening() {
        if (listenerRegistration != null) {
            println("⚠️ STONES REPOSITORY: Listener already active - NOT creating duplicate listener")
            println("   - Thread: ${Thread.currentThread().name}")
            println("   - Current stones count: ${_stones.value.size}")
            return
        }
        
        if (!::firestore.isInitialized) {
            println("❌ STONES REPOSITORY: Firestore not initialized. Call initialize() first.")
            return
        }
        
        println("=".repeat(80))
        println("👂 STONES REPOSITORY: Starting Firestore listener on stones collection")
        println("   - Thread: ${Thread.currentThread().name}")
        println("   - Repository instance: ${this.hashCode()}")
        println("   - This is the ONLY listener that will be created")
        println("=".repeat(80))
        _loading.value = true
        
        val stonesCollection = firestore.collection("stones")
        
        listenerRegistration = stonesCollection.addSnapshotListener { snapshot, exception ->
            println("📡 STONES REPOSITORY: Listener callback triggered")
            println("   - Thread: ${Thread.currentThread().name}")
            println("   - Repository instance: ${this.hashCode()}")
            
            if (exception != null) {
                println("❌ STONES REPOSITORY: Listener error: ${exception.message}")
                _error.value = "Failed to load stones: ${exception.message}"
                _loading.value = false
                return@addSnapshotListener
            }
            
            if (snapshot == null) {
                println("⚠️ STONES REPOSITORY: Snapshot is null")
                _loading.value = false
                return@addSnapshotListener
            }
            
            // Process snapshot on IO dispatcher for consistency with other repository methods
            listenerScope.launch(Dispatchers.IO) {
                try {
                    println("📥 STONES REPOSITORY: Processing snapshot from Firestore")
                    println("   - Thread: ${Thread.currentThread().name}")
                    println("   - Document count: ${snapshot.documents.size}")
                    
                    val stonesList = snapshot.documents.mapNotNull { doc ->
                        parseStoneDocument(doc)
                    }
                    
                    println("✅ STONES REPOSITORY: Parsed ${stonesList.size} stones from snapshot")
                    println("   - Updating StateFlow (this will notify all ViewModels)")
                    println("   - Previous stones count: ${_stones.value.size}")
                    
                    _stones.value = stonesList
                    _error.value = null
                    _loading.value = false
                    
                    println("✅ STONES REPOSITORY: StateFlow updated successfully")
                    println("   - New stones count: ${stonesList.size}")
                    println("   - All ViewModels using this repository will receive this update automatically")
                    println("   - Repository instance: ${this.hashCode()}")
                    println("-".repeat(80))
                } catch (e: Exception) {
                    println("❌ STONES REPOSITORY: Error parsing documents: ${e.message}")
                    _error.value = "Error parsing stone data: ${e.message}"
                    _loading.value = false
                }
            }
        }
        
        println("✅ STONES REPOSITORY: Listener attached successfully")
        println("   - Listener registration: ${listenerRegistration.hashCode()}")
    }
    
    override fun stopListening() {
        println("🛑 STONES REPOSITORY: Stopping Firestore listener")
        println("   - Thread: ${Thread.currentThread().name}")
        println("   - Repository instance: ${this.hashCode()}")
        listenerRegistration?.remove()
        listenerRegistration = null
        println("✅ STONES REPOSITORY: Firestore listener stopped")
    }
    
    private fun parseStoneDocument(doc: com.google.cloud.firestore.DocumentSnapshot): Stone? {
        val data = doc.data ?: return null
        
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
        
        return Stone(
            id = doc.id,
            name = data["name"] as? String ?: "",
            imageUrl = data["image_url"] as? String ?: "",
            types = typesList,
            createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
    
    override suspend fun getAllStoneNames(): List<String> = withContext(Dispatchers.IO) {
        // Use cached data from StateFlow
        _stones.value.map { it.name }.distinct().sorted()
    }

    override suspend fun getAllStonePurities(): List<String> = withContext(Dispatchers.IO) {
        // Extract purities from cached stones
        _stones.value
            .flatMap { it.types.map { stoneType -> stoneType.purity } }
            .distinct()
            .filter { it.isNotEmpty() }
            .sorted()
    }

    override suspend fun getAllStones(): List<Stone> = withContext(Dispatchers.IO) {
        // Return cached data from StateFlow
        _stones.value
    }

    override suspend fun addStone(name: String): String = withContext(Dispatchers.IO) {
        try {
            val newDoc = firestore.collection("stones").document()
            val id = newDoc.id
            val data = mapOf(
                "name" to name,
                "created_at" to System.currentTimeMillis()
            )
            newDoc.set(data).get()
            // Listener will automatically update _stones - no manual refresh needed
            println("✅ STONES REPOSITORY: Stone added to Firestore")
            println("   - Document ID: $id")
            println("   - Listener will automatically detect this change and update StateFlow")
            id
        } catch (e: Exception) {
            println("❌ STONES REPOSITORY: Failed to add stone: ${e.message}")
            throw e
        }
    }
}





