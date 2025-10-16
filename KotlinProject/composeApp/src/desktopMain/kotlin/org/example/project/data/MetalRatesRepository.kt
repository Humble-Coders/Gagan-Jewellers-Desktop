package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MetalRatesRepository {
    suspend fun getCurrentMetalRates(): MetalRates
    suspend fun updateGoldRates(goldRates: GoldRates): Boolean
    suspend fun updateSilverRates(silverRates: SilverRates): Boolean
    suspend fun updateMetalRates(metalRates: MetalRates): Boolean
    suspend fun getGoldRateForKarat(karat: Int): Double
    suspend fun getSilverRateForPurity(purity: Int): Double
}

class FirestoreMetalRatesRepository(private val firestore: Firestore) : MetalRatesRepository {

    override suspend fun getCurrentMetalRates(): MetalRates = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("metal_rates").document("current")
            val future = docRef.get()
            val snapshot = future.get()

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
                
                MetalRates(
                    goldRates = goldRates,
                    silverRates = silverRates,
                    lastUpdated = (data?.get("lastUpdated") as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } else {
                // Return default rates if no data exists
                MetalRates(
                    goldRates = GoldRates.calculateKaratRates(6080.0),
                    silverRates = SilverRates.calculateSilverRates(75.0)
                )
            }
        } catch (e: Exception) {
            // Return default rates on error
            MetalRates(
                goldRates = GoldRates.calculateKaratRates(6080.0),
                silverRates = SilverRates.calculateSilverRates(75.0)
            )
        }
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
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateMetalRates(metalRates: MetalRates): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("metal_rates").document("current")
            val ratesMap = mapOf(
                "gold_rate24k" to metalRates.goldRates.rate24k,
                "gold_rate22k" to metalRates.goldRates.rate22k,
                "gold_rate20k" to metalRates.goldRates.rate20k,
                "gold_rate18k" to metalRates.goldRates.rate18k,
                "gold_rate14k" to metalRates.goldRates.rate14k,
                "gold_rate10k" to metalRates.goldRates.rate10k,
                "gold_lastUpdated" to metalRates.goldRates.lastUpdated,
                "silver_rate999" to metalRates.silverRates.rate999,
                "silver_rate925" to metalRates.silverRates.rate925,
                "silver_rate900" to metalRates.silverRates.rate900,
                "silver_rate800" to metalRates.silverRates.rate800,
                "silver_lastUpdated" to metalRates.silverRates.lastUpdated,
                "lastUpdated" to System.currentTimeMillis()
            )
            docRef.set(ratesMap).get()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getGoldRateForKarat(karat: Int): Double = withContext(Dispatchers.IO) {
        val currentRates = getCurrentMetalRates()
        currentRates.getGoldRateForKarat(karat)
    }

    override suspend fun getSilverRateForPurity(purity: Int): Double = withContext(Dispatchers.IO) {
        val currentRates = getCurrentMetalRates()
        currentRates.getSilverRateForPurity(purity)
    }
}

