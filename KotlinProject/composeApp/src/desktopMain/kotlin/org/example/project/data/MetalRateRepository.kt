package org.example.project.data

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.data.normalizeMaterialType

interface MetalRateRepository {
    suspend fun getAllMetalRates(): List<MetalRate>
    suspend fun getMetalRateById(id: String): MetalRate?
    suspend fun getMetalRateByMaterialAndType(materialId: String, materialType: String): MetalRate?
    suspend fun addMetalRate(metalRate: MetalRate): String
    suspend fun updateMetalRate(metalRate: MetalRate): Boolean
    suspend fun deleteMetalRate(id: String): Boolean
    suspend fun calculateRateForMaterial(materialId: String, materialType: String, karat: Int): Double
    suspend fun updateOrCreateMetalRate(materialId: String, materialName: String, materialType: String, pricePerGram: Double): String
    suspend fun updateMetalRateWithHistory(materialId: String, materialName: String, materialType: String, newPricePerGram: Double): Boolean
}

class FirestoreMetalRateRepository(private val firestore: Firestore) : MetalRateRepository {

    override suspend fun getAllMetalRates(): List<MetalRate> = withContext(Dispatchers.IO) {
        try {
            val metalRates = mutableListOf<MetalRate>()
            
            // Read from materials collection
            val materialsCollection = firestore.collection("materials")
            val materialsSnapshot = materialsCollection.get().get()
            
            materialsSnapshot.documents.forEach { materialDoc ->
                val materialData = materialDoc.data
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
                                            metalRates.add(
                                                MetalRate(
                                                    id = "${materialId}_${normalizedPurity}", // Generate ID from material + type
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
            
            // Read from stones collection
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
                                    val rate = (typeItem["rate"] as? String) ?: (typeItem["rate"] as? Number)?.toString() ?: ""
                                    if (purity.isNotEmpty() && rate.isNotEmpty()) {
                                        val normalizedPurity = purity.trim().uppercase()
                                        val rateValue = rate.toDoubleOrNull() ?: 0.0
                                        if (rateValue > 0) {
                                            metalRates.add(
                                                MetalRate(
                                                    id = "${stoneDoc.id}_${normalizedPurity}", // Generate ID from stone + purity
                                                    materialId = "", // Stones don't have materialId
                                                    materialName = stoneName,
                                                    materialType = normalizedPurity,
                                                    karat = 0, // Stones don't have karat
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
            
            println("✅ Loaded ${metalRates.size} rates from materials and stones collections")
            metalRates
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            // Network or Firestore-specific errors
            println("❌ Firestore error loading metal rates: ${e.message}")
            e.printStackTrace()
            emptyList()
        } catch (e: IllegalStateException) {
            // Recoverable: Data structure issues
            println("⚠️ Data structure error loading metal rates: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            // Critical: Unexpected errors
            println("❌ Critical error loading metal rates: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Helper function to extract karat from purity string
    private fun extractKaratFromPurity(purity: String): Int {
        val normalized = normalizeMaterialType(purity)
        val regex = Regex("""(\d+)K""")
        val match = regex.find(normalized)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 24
    }

    override suspend fun getMetalRateById(id: String): MetalRate? = withContext(Dispatchers.IO) {
        try {
            // ID format: "materialId_materialType" or "stoneName_purity"
            val parts = id.split("_", limit = 2)
            if (parts.size == 2) {
                val materialIdOrName = parts[0]
                val materialType = parts[1]
                return@withContext getMetalRateByMaterialAndType(materialIdOrName, materialType)
            }
            null
        } catch (e: com.google.cloud.firestore.FirestoreException) {
            println("❌ Firestore error loading metal rate by ID: ${e.message}")
            null
        } catch (e: Exception) {
            println("❌ Error loading metal rate by ID: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun getMetalRateByMaterialAndType(materialId: String, materialType: String): MetalRate? = withContext(Dispatchers.IO) {
        try {
            // Normalize material type for comparison (22, 22K, 22k should be same)
            val normalizedMaterialType = normalizeMaterialType(materialType)
            
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
                "${metalRate.materialId}_${metalRate.materialType}"
            } else {
                // Update stones collection
                updateStoneTypesArray(metalRate.materialName, metalRate.materialType, metalRate.pricePerGram.toString())
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
            } else {
                // Update stones collection
                updateStoneTypesArray(metalRate.materialName, metalRate.materialType, metalRate.pricePerGram.toString())
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
                    println("✅ Metal rate deleted from materials collection: $id")
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
                        println("✅ Stone rate deleted from stones collection: $id")
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
                println("✅ Updated/created metal rate in materials collection: ${materialName} ${normalizedPurity}")
                "${materialId}_${normalizedPurity}"
            } else {
                // Update or create in stones collection
                updateStoneTypesArray(materialName, normalizedPurity, pricePerGram.toString())
                println("✅ Updated/created stone rate in stones collection: ${materialName} ${normalizedPurity}")
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
                println("✅ Updated metal rate in materials collection: ${materialName} ${normalizedPurity} -> $newPricePerGram per gram")
                true
            } else {
                // Update stones collection
                updateStoneTypesArray(materialName, normalizedPurity, newPricePerGram.toString())
                println("✅ Updated stone rate in stones collection: ${materialName} ${normalizedPurity} -> $newPricePerGram per gram")
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
}
