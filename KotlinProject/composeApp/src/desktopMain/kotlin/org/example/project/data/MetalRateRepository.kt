package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            val metalRatesCollection = firestore.collection("rates")
            val future = metalRatesCollection.get()
            val snapshot = future.get()
            
            snapshot.documents.map { doc ->
                val data = doc.data
                val previousRateData = data["previous_rate"] as? Map<String, Any>
                val previousRate = if (previousRateData != null) {
                    RateHistory(
                        pricePerGram = (previousRateData["price_per_gram"] as? Number)?.toDouble() ?: 0.0,
                        updatedAt = (previousRateData["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedBy = previousRateData["updated_by"] as? String ?: "system"
                    )
                } else null
                
                MetalRate(
                    id = doc.id,
                    materialId = data["material_id"] as? String ?: "",
                    materialName = data["material_name"] as? String ?: "",
                    materialType = data["material_type"] as? String ?: "",
                    karat = (data["karat"] as? Number)?.toInt() ?: 24,
                    pricePerGram = (data["price_per_gram"] as? Number)?.toDouble() ?: 0.0,
                    isActive = (data["is_active"] as? Boolean) ?: true,
                    createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    previousRate = previousRate
                )
            }
        } catch (e: Exception) {
            println("❌ Error loading metal rates: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getMetalRateById(id: String): MetalRate? = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("rates").document(id)
            val future = docRef.get()
            val snapshot = future.get()
            
            if (snapshot.exists()) {
                val data = snapshot.data
                MetalRate(
                    id = snapshot.id,
                    materialId = data?.get("material_id") as? String ?: "",
                    materialName = data?.get("material_name") as? String ?: "",
                    materialType = data?.get("material_type") as? String ?: "",
                    karat = (data?.get("karat") as? Number)?.toInt() ?: 24,
                    pricePerGram = (data?.get("price_per_gram") as? Number)?.toDouble() ?: 0.0,
                    isActive = (data?.get("is_active") as? Boolean) ?: true,
                    createdAt = (data?.get("created_at") as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data?.get("updated_at") as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            println("❌ Error loading metal rate by ID: ${e.message}")
            null
        }
    }

    override suspend fun getMetalRateByMaterialAndType(materialId: String, materialType: String): MetalRate? = withContext(Dispatchers.IO) {
        try {
            val metalRatesCollection = firestore.collection("rates")
            val query = metalRatesCollection
                .whereEqualTo("material_id", materialId)
                .whereEqualTo("material_type", materialType)
                .whereEqualTo("is_active", true)
                .limit(1)
            
            val future = query.get()
            val snapshot = future.get()
            
            if (!snapshot.isEmpty) {
                val doc = snapshot.documents.first()
                val data = doc.data
                MetalRate(
                    id = doc.id,
                    materialId = data["material_id"] as? String ?: "",
                    materialName = data["material_name"] as? String ?: "",
                    materialType = data["material_type"] as? String ?: "",
                    karat = (data["karat"] as? Number)?.toInt() ?: 24,
                    pricePerGram = (data["price_per_gram"] as? Number)?.toDouble() ?: 0.0,
                    isActive = (data["is_active"] as? Boolean) ?: true,
                    createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            println("❌ Error loading metal rate by material and type: ${e.message}")
            null
        }
    }

    override suspend fun addMetalRate(metalRate: MetalRate): String = withContext(Dispatchers.IO) {
        try {
            val metalRatesCollection = firestore.collection("rates")
            val docRef = metalRatesCollection.document()
            val newId = docRef.id
            
            val data = mapOf(
                "material_id" to metalRate.materialId,
                "material_name" to metalRate.materialName,
                "material_type" to metalRate.materialType,
                "karat" to metalRate.karat,
                "price_per_gram" to metalRate.pricePerGram,
                "is_active" to metalRate.isActive,
                "created_at" to System.currentTimeMillis(),
                "updated_at" to System.currentTimeMillis()
            )
            
            docRef.set(data).get()
            println("✅ Metal rate added with ID: $newId")
            newId
        } catch (e: Exception) {
            println("❌ Error adding metal rate: ${e.message}")
            throw e
        }
    }

    override suspend fun updateMetalRate(metalRate: MetalRate): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("rates").document(metalRate.id)
            
            // Convert previous rate to Firestore format
            val previousRateData = metalRate.previousRate?.let { rateHistory ->
                mapOf(
                    "price_per_gram" to rateHistory.pricePerGram,
                    "updated_at" to rateHistory.updatedAt,
                    "updated_by" to rateHistory.updatedBy
                )
            }
            
            val data = mapOf(
                "material_id" to metalRate.materialId,
                "material_name" to metalRate.materialName,
                "material_type" to metalRate.materialType,
                "karat" to metalRate.karat,
                "price_per_gram" to metalRate.pricePerGram,
                "is_active" to metalRate.isActive,
                "updated_at" to System.currentTimeMillis(),
                "previous_rate" to previousRateData
            )
            
            docRef.update(data).get()
            println("✅ Metal rate updated: ${metalRate.id}")
            true
        } catch (e: Exception) {
            println("❌ Error updating metal rate: ${e.message}")
            false
        }
    }

    override suspend fun deleteMetalRate(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("rates").document(id).delete().get()
            println("✅ Metal rate deleted: $id")
            true
        } catch (e: Exception) {
            println("❌ Error deleting metal rate: ${e.message}")
            false
        }
    }

    override suspend fun calculateRateForMaterial(materialId: String, materialType: String, karat: Int): Double = withContext(Dispatchers.IO) {
        try {
            val metalRate = getMetalRateByMaterialAndType(materialId, materialType)
            if (metalRate != null) {
                val calculatedRate = metalRate.calculateRateForKarat(karat)
                println("💰 Calculated rate for $materialType $karat K: $calculatedRate (base: ${metalRate.pricePerGram} for ${metalRate.karat}K)")
                calculatedRate
            } else {
                println("⚠️ No metal rate found for material: $materialId, type: $materialType")
                0.0
            }
        } catch (e: Exception) {
            println("❌ Error calculating rate: ${e.message}")
            0.0
        }
    }

    override suspend fun updateOrCreateMetalRate(materialId: String, materialName: String, materialType: String, pricePerGram: Double): String = withContext(Dispatchers.IO) {
        try {
            // First, try to find existing rate
            val existingRate = getMetalRateByMaterialAndType(materialId, materialType)
            
            if (existingRate != null) {
                // Update existing rate
                val updatedRate = existingRate.copy(
                    pricePerGram = pricePerGram,
                    updatedAt = System.currentTimeMillis()
                )
                val success = updateMetalRate(updatedRate)
                if (success) {
                    println("✅ Updated existing metal rate: ${materialName} ${materialType}")
                    existingRate.id
                } else {
                    throw Exception("Failed to update existing metal rate")
                }
            } else {
                // Create new rate
                val newRate = MetalRate(
                    materialId = materialId,
                    materialName = materialName,
                    materialType = materialType,
                    karat = 24, // Base karat
                    pricePerGram = pricePerGram,
                    isActive = true
                )
                val newId = addMetalRate(newRate)
                println("✅ Created new metal rate: ${materialName} ${materialType}")
                newId
            }
        } catch (e: Exception) {
            println("❌ Error updating or creating metal rate: ${e.message}")
            throw e
        }
    }

    override suspend fun updateMetalRateWithHistory(materialId: String, materialName: String, materialType: String, newPricePerGram: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            println("🔄 Updating metal rate with history: $materialName $materialType -> $newPricePerGram per gram")
            
            // Get existing rate
            val existingRate = getMetalRateByMaterialAndType(materialId, materialType)
            
            if (existingRate != null) {
                // Update existing rate with history
                val currentTime = System.currentTimeMillis()
                
                // Update previous rate if price is different
                val updatedPreviousRate = if (existingRate.pricePerGram != newPricePerGram) {
                    RateHistory(
                        pricePerGram = existingRate.pricePerGram,
                        updatedAt = existingRate.updatedAt,
                        updatedBy = "system"
                    )
                } else {
                    existingRate.previousRate
                }
                
                val updatedRate = existingRate.copy(
                    pricePerGram = newPricePerGram,
                    updatedAt = currentTime,
                    previousRate = updatedPreviousRate
                )
                
                val success = updateMetalRate(updatedRate)
                if (success) {
                    println("✅ Updated metal rate with history: ${materialName} ${materialType} -> $newPricePerGram per gram")
                }
                success
            } else {
                // Create new rate
                val newRate = MetalRate(
                    materialId = materialId,
                    materialName = materialName,
                    materialType = materialType,
                    karat = 24, // Base karat for 24K
                    pricePerGram = newPricePerGram,
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    previousRate = null
                )
                
                val newId = addMetalRate(newRate)
                println("✅ Created new metal rate: ${materialName} ${materialType} -> $newPricePerGram per gram")
                newId.isNotEmpty()
            }
        } catch (e: Exception) {
            println("❌ Error updating metal rate with history: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
