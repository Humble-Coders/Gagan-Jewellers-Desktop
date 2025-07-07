package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CartRepository {
    suspend fun getMetalPrices(): MetalPrices
    suspend fun updateMetalPrices(prices: MetalPrices): Boolean
}

class FirestoreCartRepository(private val firestore: Firestore) : CartRepository {

    override suspend fun getMetalPrices(): MetalPrices = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("settings").document("metal_prices")
            val future = docRef.get()
            val doc = future.get()

            if (doc.exists()) {
                val data = doc.data ?: return@withContext MetalPrices()

                MetalPrices(
                    goldPricePerGram = (data["goldPricePerGram"] as? Number)?.toDouble() ?: 6080.0,
                    silverPricePerGram = (data["silverPricePerGram"] as? Number)?.toDouble() ?: 75.0,
                    lastUpdated = (data["lastUpdated"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } else {
                // Create default prices document if it doesn't exist
                val defaultPrices = MetalPrices()
                updateMetalPrices(defaultPrices)
                defaultPrices
            }
        } catch (e: Exception) {
            println("Error fetching metal prices: ${e.message}")
            MetalPrices() // Return default prices
        }
    }

    override suspend fun updateMetalPrices(prices: MetalPrices): Boolean = withContext(Dispatchers.IO) {
        try {
            val pricesMap = mapOf(
                "goldPricePerGram" to prices.goldPricePerGram,
                "silverPricePerGram" to prices.silverPricePerGram,
                "lastUpdated" to System.currentTimeMillis()
            )

            firestore.collection("settings")
                .document("metal_prices")
                .set(pricesMap)
                .get()

            true
        } catch (e: Exception) {
            println("Error updating metal prices: ${e.message}")
            false
        }
    }
}