package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface GoldRateRepository {
    suspend fun getCurrentGoldRates(): GoldRates
    suspend fun updateGoldRates(goldRates: GoldRates): Boolean
}

class FirestoreGoldRateRepository(private val firestore: Firestore) : GoldRateRepository {

    override suspend fun getCurrentGoldRates(): GoldRates = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("gold_rates").document("current")
            val future = docRef.get()
            val snapshot = future.get()

            if (snapshot.exists()) {
                val data = snapshot.data
                GoldRates(
                    rate24k = (data?.get("rate24k") as? Number)?.toDouble() ?: 6080.0,
                    rate22k = (data?.get("rate22k") as? Number)?.toDouble() ?: 0.0,
                    rate20k = (data?.get("rate20k") as? Number)?.toDouble() ?: 0.0,
                    rate18k = (data?.get("rate18k") as? Number)?.toDouble() ?: 0.0,
                    rate14k = (data?.get("rate14k") as? Number)?.toDouble() ?: 0.0,
                    rate10k = (data?.get("rate10k") as? Number)?.toDouble() ?: 0.0,
                    lastUpdated = (data?.get("lastUpdated") as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } else {
                // Return default rates if no data exists
                GoldRates.calculateKaratRates(6080.0)
            }
        } catch (e: Exception) {
            // Return default rates on error
            GoldRates.calculateKaratRates(6080.0)
        }
    }

    override suspend fun updateGoldRates(goldRates: GoldRates): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("gold_rates").document("current")
            val ratesMap = mapOf(
                "rate24k" to goldRates.rate24k,
                "rate22k" to goldRates.rate22k,
                "rate20k" to goldRates.rate20k,
                "rate18k" to goldRates.rate18k,
                "rate14k" to goldRates.rate14k,
                "rate10k" to goldRates.rate10k,
                "lastUpdated" to goldRates.lastUpdated
            )
            docRef.set(ratesMap).get()
            true
        } catch (e: Exception) {
            false
        }
    }

}
