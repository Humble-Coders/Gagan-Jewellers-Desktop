package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.JewelryAppInitializer

data class MainStore(
    val companyName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val gstin: String = "",
    val pan: String = "",
    val certification: String = "",
    val stateCode: String = "",
    val stateName: String = ""
)

data class BankInfo(
    val accountHolder: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val branch: String = "",
    val accountType: String = ""
)

data class StoreInfo(
    val mainStore: MainStore = MainStore(),
    val bankInfo: BankInfo = BankInfo()
)

class StoreInfoRepository {
    private val firestore: Firestore by lazy { JewelryAppInitializer.getFirestore() }

    suspend fun getStoreInfo(): StoreInfo = withContext(Dispatchers.IO) {
        return@withContext try {
            // Fetch main_store document
            val mainStoreDoc = firestore.collection("store_info")
                .document("main_store")
                .get()
                .get()

            val mainStoreData = mainStoreDoc.data ?: emptyMap()
            val mainStore = MainStore(
                companyName = mainStoreData["companyName"] as? String ?: "",
                address = mainStoreData["address"] as? String ?: "",
                phone = mainStoreData["phone"] as? String ?: "",
                email = mainStoreData["email"] as? String ?: "",
                gstin = mainStoreData["gstin"] as? String ?: "",
                pan = mainStoreData["pan"] as? String ?: "",
                certification = mainStoreData["certification"] as? String ?: "",
                stateCode = mainStoreData["stateCode"] as? String ?: "",
                stateName = mainStoreData["stateName"] as? String ?: ""
            )

            // Fetch bank_info document
            val bankInfoDoc = firestore.collection("store_info")
                .document("bank_info")
                .get()
                .get()

            val bankInfoData = bankInfoDoc.data ?: emptyMap()
            val bankInfo = BankInfo(
                accountHolder = bankInfoData["accountHolder"] as? String ?: "",
                accountNumber = bankInfoData["accountNumber"] as? String ?: "",
                ifscCode = bankInfoData["ifscCode"] as? String ?: "",
                branch = bankInfoData["branch"] as? String ?: "",
                accountType = bankInfoData["accountType"] as? String ?: ""
            )

            StoreInfo(mainStore = mainStore, bankInfo = bankInfo)
        } catch (e: Exception) {
            println("❌ Error fetching store info: ${e.message}")
            e.printStackTrace()
            StoreInfo() // Return empty store info on error
        }
    }
}

