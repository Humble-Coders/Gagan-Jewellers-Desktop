package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.JewelryAppInitializer

data class MainStore(
    val name: String = "",
    val address: String = "",
    val phone_primary: String = "",
    val phone_secondary: String = "",
    val email: String = "",
    val gstIn: String = "",
    val certification: String = "",
    val stateCode: String = "",
    val stateName: String = "",
    val whatsappNumber: String = "",
    val whatsappMessage: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val establishedYear: String = "",
    val logo_images: List<String> = emptyList()
)

data class BankInfo(
    val account_holder: String = "",
    val AccountNumber: String = "",
    val IFSC_Code: String = "",
    val Branch: String = "",
    val Acc_type: String = "",
    val pan_no: String = ""
)

data class DayHours(
    val openTime: String = "",
    val closeTime: String = "",
    val isClosed: Boolean = false
)

data class StoreHours(
    val monday: DayHours = DayHours(),
    val tuesday: DayHours = DayHours(),
    val wednesday: DayHours = DayHours(),
    val thursday: DayHours = DayHours(),
    val friday: DayHours = DayHours(),
    val saturday: DayHours = DayHours(),
    val sunday: DayHours = DayHours()
)

data class StoreInfo(
    val mainStore: MainStore = MainStore(),
    val bankInfo: BankInfo = BankInfo(),
    val storeHours: StoreHours = StoreHours(),
    val storeImages: List<String> = emptyList()
)

object StoreInfoRepository {
    private val firestore: Firestore by lazy { JewelryAppInitializer.getFirestore() }
    
    // In-memory cache for store info (shared across entire app)
    @Volatile
    private var cachedStoreInfo: StoreInfo? = null

    suspend fun getStoreInfo(): StoreInfo = withContext(Dispatchers.IO) {
        // Return cached data if available
        cachedStoreInfo?.let {
            println("📦 Using cached store info")
            return@withContext it
        }
        
        // Cache is empty, fetch from Firestore
        println("🔄 Fetching store info from Firestore (cache miss)")
        return@withContext try {
            // Fetch main_store document
            val mainStoreDoc = firestore.collection("store_info")
                .document("main_store")
                .get()
                .get()

            val mainStoreData = mainStoreDoc.data ?: emptyMap()
            val mainStore = MainStore(
                name = mainStoreData["name"] as? String ?: "",
                address = mainStoreData["address"] as? String ?: "",
                phone_primary = mainStoreData["phone_primary"] as? String ?: "",
                phone_secondary = mainStoreData["phone_secondary"] as? String ?: "",
                email = mainStoreData["email"] as? String ?: "",
                gstIn = mainStoreData["gstIn"] as? String ?: "",
                certification = mainStoreData["certification"] as? String ?: "",
                stateCode = mainStoreData["stateCode"] as? String ?: "",
                stateName = mainStoreData["stateName"] as? String ?: "",
                whatsappNumber = mainStoreData["whatsappNumber"] as? String ?: "",
                whatsappMessage = mainStoreData["whatsappMessage"] as? String ?: "",
                latitude = mainStoreData["latitude"] as? String ?: "",
                longitude = mainStoreData["longitude"] as? String ?: "",
                establishedYear = mainStoreData["establishedYear"] as? String ?: "",
                logo_images = (mainStoreData["logo_images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            )

            // Fetch store_hours from main_store document (nested field) - time strings
            val storeHoursMap = mainStoreData["store_hours"] as? Map<String, Any> ?: emptyMap()
            
            // Fetch store_hours_closed from separate document for closed status
            val storeHoursClosedDoc = try {
                firestore.collection("store_info")
                    .document("store_hours_closed")
                    .get()
                    .get()
            } catch (e: Exception) {
                null
            }
            val storeHoursClosedData = storeHoursClosedDoc?.data ?: emptyMap()
            
            // Parse time strings (format: "10:00 AM - 8:00 PM") and combine with closed status
            fun parseTimeString(timeStr: String): Pair<String, String> {
                if (timeStr.isEmpty()) return "" to ""
                val parts = timeStr.split(" - ")
                return if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    "" to ""
                }
            }
            
            val storeHours = StoreHours(
                monday = run {
                    val (open, close) = parseTimeString(storeHoursMap["monday"] as? String ?: "")
                    DayHours(
                        openTime = open,
                        closeTime = close,
                        isClosed = storeHoursClosedData["monday"] as? Boolean ?: false
                    )
                },
                tuesday = run {
                    val (open, close) = parseTimeString(storeHoursMap["tuesday"] as? String ?: "")
                    DayHours(
                        openTime = open,
                        closeTime = close,
                        isClosed = storeHoursClosedData["tuesday"] as? Boolean ?: false
                    )
                },
                wednesday = run {
                    val (open, close) = parseTimeString(storeHoursMap["wednesday"] as? String ?: "")
                    DayHours(
                        openTime = open,
                        closeTime = close,
                        isClosed = storeHoursClosedData["wednesday"] as? Boolean ?: false
                    )
                },
                thursday = run {
                    val (open, close) = parseTimeString(storeHoursMap["thursday"] as? String ?: "")
                    DayHours(
                        openTime = open,
                        closeTime = close,
                        isClosed = storeHoursClosedData["thursday"] as? Boolean ?: false
                    )
                },
                friday = run {
                    val (open, close) = parseTimeString(storeHoursMap["friday"] as? String ?: "")
                    DayHours(
                        openTime = open,
                        closeTime = close,
                        isClosed = storeHoursClosedData["friday"] as? Boolean ?: false
                    )
                },
                saturday = run {
                    val (open, close) = parseTimeString(storeHoursMap["saturday"] as? String ?: "")
                    DayHours(
                        openTime = open,
                        closeTime = close,
                        isClosed = storeHoursClosedData["saturday"] as? Boolean ?: false
                    )
                },
                sunday = run {
                    val (open, close) = parseTimeString(storeHoursMap["sunday"] as? String ?: "")
                    DayHours(
                        openTime = open,
                        closeTime = close,
                        isClosed = storeHoursClosedData["sunday"] as? Boolean ?: false
                    )
                }
            )

            // Fetch store_images from main_store document (array field)
            val storeImages = (mainStoreData["store_images"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            // Fetch bank_info document
            val bankInfoDoc = firestore.collection("store_info")
                .document("bank_info")
                .get()
                .get()

            val bankInfoData = bankInfoDoc.data ?: emptyMap()
            // Fetch bank info from store_info/bank_info document with exact field names from Firestore
            // Field names in Firestore: account_holder, AccountNumber, IFSC_Code, Branch, Acc_type, pan_no
            val bankInfo = BankInfo(
                account_holder = bankInfoData["account_holder"] as? String ?: "",
                AccountNumber = bankInfoData["AccountNumber"] as? String ?: "",
                IFSC_Code = bankInfoData["IFSC_Code"] as? String ?: "",
                Branch = bankInfoData["Branch"] as? String ?: "",
                Acc_type = bankInfoData["Acc_type"] as? String ?: "",
                pan_no = bankInfoData["pan_no"] as? String ?: ""
            )
            
            println("✅ Bank Info fetched from store_info/bank_info:")
            println("   - Account Holder: ${bankInfo.account_holder}")
            println("   - Account Number: ${bankInfo.AccountNumber}")
            println("   - IFSC Code: ${bankInfo.IFSC_Code}")
            println("   - Branch: ${bankInfo.Branch}")
            println("   - Account Type: ${bankInfo.Acc_type}")

            val storeInfo = StoreInfo(mainStore = mainStore, bankInfo = bankInfo, storeHours = storeHours, storeImages = storeImages)
            
            // Cache the fetched data
            cachedStoreInfo = storeInfo
            println("💾 Store info cached in memory")
            
            return@withContext storeInfo
        } catch (e: Exception) {
            println("❌ Error fetching store info: ${e.message}")
            e.printStackTrace()
            StoreInfo() // Return empty store info on error
        }
    }

    suspend fun saveStoreInfo(storeInfo: StoreInfo): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // Save main_store document - using update to preserve existing fields
            val mainStoreData = mutableMapOf<String, Any>()
            
            // Only add fields that have values to avoid overwriting with empty strings
            if (storeInfo.mainStore.name.isNotEmpty()) {
                mainStoreData["name"] = storeInfo.mainStore.name
            }
            if (storeInfo.mainStore.address.isNotEmpty()) {
                mainStoreData["address"] = storeInfo.mainStore.address
            }
            if (storeInfo.mainStore.phone_primary.isNotEmpty()) {
                mainStoreData["phone_primary"] = storeInfo.mainStore.phone_primary
            }
            if (storeInfo.mainStore.phone_secondary.isNotEmpty()) {
                mainStoreData["phone_secondary"] = storeInfo.mainStore.phone_secondary
            }
            if (storeInfo.mainStore.email.isNotEmpty()) {
                mainStoreData["email"] = storeInfo.mainStore.email
            }
            if (storeInfo.mainStore.gstIn.isNotEmpty()) {
                mainStoreData["gstIn"] = storeInfo.mainStore.gstIn
            }
            if (storeInfo.mainStore.certification.isNotEmpty()) {
                mainStoreData["certification"] = storeInfo.mainStore.certification
            }
            if (storeInfo.mainStore.stateCode.isNotEmpty()) {
                mainStoreData["stateCode"] = storeInfo.mainStore.stateCode
            }
            if (storeInfo.mainStore.stateName.isNotEmpty()) {
                mainStoreData["stateName"] = storeInfo.mainStore.stateName
            }
            if (storeInfo.mainStore.whatsappNumber.isNotEmpty()) {
                mainStoreData["whatsappNumber"] = storeInfo.mainStore.whatsappNumber
            }
            if (storeInfo.mainStore.whatsappMessage.isNotEmpty()) {
                mainStoreData["whatsappMessage"] = storeInfo.mainStore.whatsappMessage
            }
            if (storeInfo.mainStore.latitude.isNotEmpty()) {
                mainStoreData["latitude"] = storeInfo.mainStore.latitude
            }
            if (storeInfo.mainStore.longitude.isNotEmpty()) {
                mainStoreData["longitude"] = storeInfo.mainStore.longitude
            }
            if (storeInfo.mainStore.establishedYear.isNotEmpty()) {
                mainStoreData["establishedYear"] = storeInfo.mainStore.establishedYear
            }
            
            // Save logo_images (always save, even if empty)
            mainStoreData["logo_images"] = storeInfo.mainStore.logo_images
            
            // Add store_hours as nested map in main_store document (time strings only)
            val storeHoursMap = mutableMapOf<String, String>()
            fun formatTimeString(dayHours: DayHours): String {
                return if (dayHours.openTime.isNotEmpty() && dayHours.closeTime.isNotEmpty()) {
                    "${dayHours.openTime} - ${dayHours.closeTime}"
                } else ""
            }
            
            val mondayTime = formatTimeString(storeInfo.storeHours.monday)
            if (mondayTime.isNotEmpty()) {
                storeHoursMap["monday"] = mondayTime
            }
            val tuesdayTime = formatTimeString(storeInfo.storeHours.tuesday)
            if (tuesdayTime.isNotEmpty()) {
                storeHoursMap["tuesday"] = tuesdayTime
            }
            val wednesdayTime = formatTimeString(storeInfo.storeHours.wednesday)
            if (wednesdayTime.isNotEmpty()) {
                storeHoursMap["wednesday"] = wednesdayTime
            }
            val thursdayTime = formatTimeString(storeInfo.storeHours.thursday)
            if (thursdayTime.isNotEmpty()) {
                storeHoursMap["thursday"] = thursdayTime
            }
            val fridayTime = formatTimeString(storeInfo.storeHours.friday)
            if (fridayTime.isNotEmpty()) {
                storeHoursMap["friday"] = fridayTime
            }
            val saturdayTime = formatTimeString(storeInfo.storeHours.saturday)
            if (saturdayTime.isNotEmpty()) {
                storeHoursMap["saturday"] = saturdayTime
            }
            val sundayTime = formatTimeString(storeInfo.storeHours.sunday)
            if (sundayTime.isNotEmpty()) {
                storeHoursMap["sunday"] = sundayTime
            }
            if (storeHoursMap.isNotEmpty()) {
                mainStoreData["store_hours"] = storeHoursMap
            }
            
            // Save closed status in separate document
            val storeHoursClosedData = mapOf(
                "monday" to storeInfo.storeHours.monday.isClosed,
                "tuesday" to storeInfo.storeHours.tuesday.isClosed,
                "wednesday" to storeInfo.storeHours.wednesday.isClosed,
                "thursday" to storeInfo.storeHours.thursday.isClosed,
                "friday" to storeInfo.storeHours.friday.isClosed,
                "saturday" to storeInfo.storeHours.saturday.isClosed,
                "sunday" to storeInfo.storeHours.sunday.isClosed
            )
            firestore.collection("store_info")
                .document("store_hours_closed")
                .set(storeHoursClosedData)
                .get()
            
            // Add store_images as array in main_store document
            if (storeInfo.storeImages.isNotEmpty()) {
                mainStoreData["store_images"] = storeInfo.storeImages
            }
            
            firestore.collection("store_info")
                .document("main_store")
                .update(mainStoreData)
                .get()

            // Save bank_info document - using update to preserve existing fields
            val bankInfoData = mutableMapOf<String, Any>()
            
            // Only add fields that have values, using the exact Firestore field names
            if (storeInfo.bankInfo.account_holder.isNotEmpty()) {
                bankInfoData["account_holder"] = storeInfo.bankInfo.account_holder
            }
            if (storeInfo.bankInfo.AccountNumber.isNotEmpty()) {
                bankInfoData["AccountNumber"] = storeInfo.bankInfo.AccountNumber
            }
            if (storeInfo.bankInfo.IFSC_Code.isNotEmpty()) {
                bankInfoData["IFSC_Code"] = storeInfo.bankInfo.IFSC_Code
            }
            if (storeInfo.bankInfo.Branch.isNotEmpty()) {
                bankInfoData["Branch"] = storeInfo.bankInfo.Branch
            }
            if (storeInfo.bankInfo.Acc_type.isNotEmpty()) {
                bankInfoData["Acc_type"] = storeInfo.bankInfo.Acc_type
            }
            if (storeInfo.bankInfo.pan_no.isNotEmpty()) {
                bankInfoData["pan_no"] = storeInfo.bankInfo.pan_no
            }
            
            if (bankInfoData.isNotEmpty()) {
                firestore.collection("store_info")
                    .document("bank_info")
                    .update(bankInfoData)
                    .get()
            }

            // Invalidate cache after successful save - next getStoreInfo() will fetch fresh data
            invalidateCache()
            println("✅ Store info saved successfully and cache invalidated")
            true
        } catch (e: Exception) {
            println("❌ Error saving store info: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Invalidates the cache. Next call to getStoreInfo() will fetch fresh data from Firestore.
     */
    fun invalidateCache() {
        cachedStoreInfo = null
        println("🗑️ Store info cache invalidated")
    }
    
    /**
     * Refreshes the cache by fetching latest data from Firestore.
     */
    suspend fun refreshCache() {
        invalidateCache()
        getStoreInfo() // This will fetch and cache fresh data
    }
}

