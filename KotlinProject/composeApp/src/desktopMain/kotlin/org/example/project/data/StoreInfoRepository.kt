package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.JewelryAppInitializer

data class MainStore(
    val companyName: String = "",
    val address: String = "",
    val phone: String = "",
    val phonePrimary: String = "",
    val phoneSecondary: String = "",
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
    val accountType: String = "",
    val pan: String = "" // PAN from bank_info document (pan_no field)
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
                companyName = mainStoreData["companyName"] as? String ?: mainStoreData["name"] as? String ?: "",
                address = mainStoreData["address"] as? String ?: "",
                phone = mainStoreData["phone"] as? String ?: "",
                phonePrimary = mainStoreData["phone_primary"] as? String ?: mainStoreData["phonePrimary"] as? String ?: "",
                phoneSecondary = mainStoreData["phone_secondary"] as? String ?: mainStoreData["phoneSecondary"] as? String ?: "",
                email = mainStoreData["email"] as? String ?: "",
                gstin = mainStoreData["gstIn"] as? String ?: mainStoreData["gstin"] as? String ?: mainStoreData["GSTIN"] as? String ?: "",
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
            // Fetch bank info from store_info/bank_info document with exact field names from Firestore
            // Field names in Firestore: acc_holder, acc_number, IFSC_Code, Branch, Acc_type, pan_no
            val bankInfo = BankInfo(
                accountHolder = bankInfoData["acc_holder"] as? String 
                    ?: bankInfoData["accountHolder"] as? String 
                    ?: bankInfoData["account_holder"] as? String ?: "",
                accountNumber = bankInfoData["acc_number"] as? String 
                    ?: bankInfoData["accountNumber"] as? String 
                    ?: bankInfoData["account_number"] as? String ?: "",
                ifscCode = bankInfoData["IFSC_Code"] as? String 
                    ?: bankInfoData["ifscCode"] as? String 
                    ?: bankInfoData["ifsc_code"] as? String ?: "",
                branch = bankInfoData["Branch"] as? String 
                    ?: bankInfoData["branch"] as? String ?: "",
                accountType = bankInfoData["Acc_type"] as? String 
                    ?: bankInfoData["accountType"] as? String 
                    ?: bankInfoData["account_type"] as? String ?: "",
                pan = bankInfoData["pan_no"] as? String 
                    ?: bankInfoData["panNo"] as? String 
                    ?: bankInfoData["pan"] as? String ?: ""
            )
            
            println("✅ Bank Info fetched from store_info/bank_info:")
            println("   - Account Holder: ${bankInfo.accountHolder}")
            println("   - Account Number: ${bankInfo.accountNumber}")
            println("   - IFSC Code: ${bankInfo.ifscCode}")
            println("   - Branch: ${bankInfo.branch}")
            println("   - Account Type: ${bankInfo.accountType}")

            StoreInfo(mainStore = mainStore, bankInfo = bankInfo)
        } catch (e: Exception) {
            println("❌ Error fetching store info: ${e.message}")
            e.printStackTrace()
            StoreInfo() // Return empty store info on error
        }
    }
}

