package org.example.project.data

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.JewelryAppInitializer

class InvoiceConfigRepository {
    private val firestore: Firestore by lazy { JewelryAppInitializer.getFirestore() }

    suspend fun getInvoiceConfig(): InvoiceConfig? = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = firestore.collection("invoice_config")
                .document("default")
                .get()
                .get()

            if (snapshot.exists()) {
                val data = snapshot.data
                InvoiceConfig(
                    id = snapshot.id,
                    companyName = data?.get("companyName") as? String ?: "Vishal Gems and Jewels Pvt. Ltd.",
                    companyAddress = data?.get("companyAddress") as? String ?: "",
                    companyPhone = data?.get("companyPhone") as? String ?: "",
                    companyEmail = data?.get("companyEmail") as? String ?: "",
                    companyGST = data?.get("companyGST") as? String ?: "",
                    showCustomerDetails = data?.get("showCustomerDetails") as? Boolean ?: true,
                    showGoldRate = data?.get("showGoldRate") as? Boolean ?: true,
                    showMakingCharges = data?.get("showMakingCharges") as? Boolean ?: true,
                    showPaymentBreakup = data?.get("showPaymentBreakup") as? Boolean ?: true,
                    showItemDetails = data?.get("showItemDetails") as? Boolean ?: true,
                    showTermsAndConditions = data?.get("showTermsAndConditions") as? Boolean ?: true,
                    termsAndConditions = data?.get("termsAndConditions") as? String ?: "Thank you for your business!",
                    logoUrl = data?.get("logoUrl") as? String ?: "",
                    footerText = data?.get("footerText") as? String ?: "Visit us again!",
                    isActive = data?.get("isActive") as? Boolean ?: true,
                    createdAt = (data?.get("createdAt") as? Long) ?: System.currentTimeMillis(),
                    updatedAt = (data?.get("updatedAt") as? Long) ?: System.currentTimeMillis()
                )
            } else {
                // Return default config if none exists
                InvoiceConfig()
            }
        } catch (e: Exception) {
            println("❌ Error fetching invoice config: ${e.message}")
            e.printStackTrace()
            InvoiceConfig() // Return default config on error
        }
    }

    suspend fun saveInvoiceConfig(config: InvoiceConfig): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val data = mapOf(
                "companyName" to config.companyName,
                "companyAddress" to config.companyAddress,
                "companyPhone" to config.companyPhone,
                "companyEmail" to config.companyEmail,
                "companyGST" to config.companyGST,
                "showCustomerDetails" to config.showCustomerDetails,
                "showGoldRate" to config.showGoldRate,
                "showMakingCharges" to config.showMakingCharges,
                "showPaymentBreakup" to config.showPaymentBreakup,
                "showItemDetails" to config.showItemDetails,
                "showTermsAndConditions" to config.showTermsAndConditions,
                "termsAndConditions" to config.termsAndConditions,
                "logoUrl" to config.logoUrl,
                "footerText" to config.footerText,
                "isActive" to config.isActive,
                "createdAt" to config.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("invoice_config")
                .document("default")
                .set(data)
                .get()

            println("✅ Invoice config saved successfully")
            true
        } catch (e: Exception) {
            println("❌ Error saving invoice config: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun getInvoiceConfigFlow(): Flow<InvoiceConfig> = flow {
        try {
            val snapshot = firestore.collection("invoice_config")
                .document("default")
                .get()
                .get()

            val config = if (snapshot.exists()) {
                val data = snapshot.data
                InvoiceConfig(
                    id = snapshot.id,
                    companyName = data?.get("companyName") as? String ?: "Vishal Gems and Jewels Pvt. Ltd.",
                    companyAddress = data?.get("companyAddress") as? String ?: "",
                    companyPhone = data?.get("companyPhone") as? String ?: "",
                    companyEmail = data?.get("companyEmail") as? String ?: "",
                    companyGST = data?.get("companyGST") as? String ?: "",
                    showCustomerDetails = data?.get("showCustomerDetails") as? Boolean ?: true,
                    showGoldRate = data?.get("showGoldRate") as? Boolean ?: true,
                    showMakingCharges = data?.get("showMakingCharges") as? Boolean ?: true,
                    showPaymentBreakup = data?.get("showPaymentBreakup") as? Boolean ?: true,
                    showItemDetails = data?.get("showItemDetails") as? Boolean ?: true,
                    showTermsAndConditions = data?.get("showTermsAndConditions") as? Boolean ?: true,
                    termsAndConditions = data?.get("termsAndConditions") as? String ?: "Thank you for your business!",
                    logoUrl = data?.get("logoUrl") as? String ?: "",
                    footerText = data?.get("footerText") as? String ?: "Visit us again!",
                    isActive = data?.get("isActive") as? Boolean ?: true,
                    createdAt = (data?.get("createdAt") as? Long) ?: System.currentTimeMillis(),
                    updatedAt = (data?.get("updatedAt") as? Long) ?: System.currentTimeMillis()
                )
            } else {
                InvoiceConfig()
            }
            emit(config)
        } catch (e: Exception) {
            println("❌ Error in invoice config flow: ${e.message}")
            emit(InvoiceConfig())
        }
    }
}
