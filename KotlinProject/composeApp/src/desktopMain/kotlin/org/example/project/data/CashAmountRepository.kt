package org.example.project.data

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

interface CashAmountRepository {
    suspend fun addCashTransaction(cashAmount: CashAmount): String
    suspend fun getCashTransactionsByCustomerId(customerId: String): List<CashAmount>
    suspend fun getAllCashTransactions(): List<CashAmount>
    suspend fun updateCustomerBalance(customerId: String, amount: Double, transactionType: CashTransactionType): Boolean
}

class FirestoreCashAmountRepository(private val firestore: Firestore) : CashAmountRepository {

    // Generate TRA ID similar to order ID format
    private fun generateCashAmountId(): String {
        return "TRA_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    override suspend fun addCashTransaction(cashAmount: CashAmount): String = withContext(Dispatchers.IO) {
        try {
            val cashAmountId = generateCashAmountId()
            val currentTime = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd MMMM yyyy 'at' HH:mm:ss z", Locale.getDefault())
            val transactionDate = dateFormat.format(Date(currentTime))

            // Create cashAmount data matching Order structure
            val cashAmountData = mapOf(
                "cashAmountId" to cashAmountId,
                "customerId" to cashAmount.customerId,
                
                // Payment Information
                "paymentSplit" to null, // Cash transactions don't have payment splits
                "paymentStatus" to PaymentStatus.COMPLETED.name,
                
                // Financial Details
                "subtotal" to cashAmount.amount,
                "discountAmount" to 0.0,
                "discountPercent" to 0.0,
                "taxableAmount" to cashAmount.amount,
                "gstAmount" to 0.0,
                "totalAmount" to cashAmount.amount,
                "finalAmount" to cashAmount.amount,
                "isGstIncluded" to false,
                
                // Cash Transaction Specific Fields
                "amount" to cashAmount.amount,
                "transactionType" to cashAmount.transactionType.name,
                
                // Order Items - Empty for cash transactions
                "items" to emptyList<Map<String, Any>>(),
                
                // Metal Rates Reference - Empty for cash transactions
                "metalRatesReference" to "",
                
                // Timestamps
                "createdAt" to currentTime,
                "updatedAt" to currentTime,
                "completedAt" to currentTime,
                "transactionDate" to transactionDate,
                
                // Additional Information
                "notes" to cashAmount.notes,
                "status" to OrderStatus.CONFIRMED.name,
                "createdBy" to cashAmount.createdBy
            )

            // Use the generated ID as the document ID
            firestore.collection("cashAmount").document(cashAmountId).set(cashAmountData).get()

            // Update customer balance
            updateCustomerBalance(cashAmount.customerId, cashAmount.amount, cashAmount.transactionType)

            cashAmountId
        } catch (e: Exception) {
            println("Error adding cash transaction: ${e.message}")
            throw e
        }
    }

    override suspend fun getCashTransactionsByCustomerId(customerId: String): List<CashAmount> = withContext(Dispatchers.IO) {
        try {
            println("💰 CASH REPOSITORY: Fetching cash transactions for customer $customerId")
            
            // First, let's check if there are any cash transactions at all
            val allSnapshot = firestore.collection("cashAmount").get().get()
            println("💰 CASH REPOSITORY: Total cash transactions in collection: ${allSnapshot.documents.size}")
            
            if (allSnapshot.documents.isNotEmpty()) {
                println("💰 CASH REPOSITORY: Sample document IDs: ${allSnapshot.documents.take(3).map { it.id }}")
                val sampleDoc = allSnapshot.documents.first()
                val sampleData = sampleDoc.data
                println("💰 CASH REPOSITORY: Sample document data keys: ${sampleData?.keys}")
                println("💰 CASH REPOSITORY: Sample customerId: ${sampleData?.get("customerId")}")
            }
            
            val snapshot = firestore.collection("cashAmount")
                .whereEqualTo("customerId", customerId)
                .get()
                .get()

            println("💰 CASH REPOSITORY: Found ${snapshot.documents.size} cash transaction documents for customer $customerId")

            val cashTransactions = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                println("💰 CASH REPOSITORY: Processing document ${doc.id} with data: ${data.keys}")
                
                CashAmount(
                    cashAmountId = data["cashAmountId"] as? String ?: doc.id,
                    customerId = data["customerId"] as? String ?: "",
                    
                    // Payment Information
                    paymentSplit = null, // Cash transactions don't have payment splits
                    paymentStatus = PaymentStatus.valueOf(data["paymentStatus"] as? String ?: PaymentStatus.COMPLETED.name),
                    
                    // Financial Details
                    subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                    discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                    discountPercent = (data["discountPercent"] as? Number)?.toDouble() ?: 0.0,
                    taxableAmount = (data["taxableAmount"] as? Number)?.toDouble() ?: 0.0,
                    gstAmount = (data["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                    totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                    finalAmount = (data["finalAmount"] as? Number)?.toDouble() ?: 0.0,
                    isGstIncluded = data["isGstIncluded"] as? Boolean ?: false,
                    
                    // Cash Transaction Specific Fields
                    amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                    transactionType = CashTransactionType.valueOf(data["transactionType"] as? String ?: CashTransactionType.GIVE.name),
                    
                    // Order Items - Empty for cash transactions
                    items = emptyList(),
                    
                    // Metal Rates Reference
                    metalRatesReference = data["metalRatesReference"] as? String ?: "",
                    
                    // Timestamps
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    completedAt = (data["completedAt"] as? Number)?.toLong(),
                    transactionDate = data["transactionDate"] as? String ?: "",
                    
                    // Additional Information
                    notes = data["notes"] as? String ?: "",
                    status = OrderStatus.valueOf(data["status"] as? String ?: OrderStatus.CONFIRMED.name),
                    createdBy = data["createdBy"] as? String ?: "system"
                )
            }
            
            println("💰 CASH REPOSITORY: Successfully processed ${cashTransactions.size} cash transactions")
            cashTransactions
        } catch (e: Exception) {
            println("❌ CASH REPOSITORY: Error getting cash transactions: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getAllCashTransactions(): List<CashAmount> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("cashAmount")
                .get()
                .get()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                CashAmount(
                    cashAmountId = data["cashAmountId"] as? String ?: doc.id,
                    customerId = data["customerId"] as? String ?: "",
                    
                    // Payment Information
                    paymentSplit = null,
                    paymentStatus = PaymentStatus.valueOf(data["paymentStatus"] as? String ?: PaymentStatus.COMPLETED.name),
                    
                    // Financial Details
                    subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                    discountAmount = (data["discountAmount"] as? Number)?.toDouble() ?: 0.0,
                    discountPercent = (data["discountPercent"] as? Number)?.toDouble() ?: 0.0,
                    taxableAmount = (data["taxableAmount"] as? Number)?.toDouble() ?: 0.0,
                    gstAmount = (data["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                    totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                    finalAmount = (data["finalAmount"] as? Number)?.toDouble() ?: 0.0,
                    isGstIncluded = data["isGstIncluded"] as? Boolean ?: false,
                    
                    // Cash Transaction Specific Fields
                    amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                    transactionType = CashTransactionType.valueOf(data["transactionType"] as? String ?: CashTransactionType.GIVE.name),
                    
                    // Order Items
                    items = emptyList(),
                    
                    // Metal Rates Reference
                    metalRatesReference = data["metalRatesReference"] as? String ?: "",
                    
                    // Timestamps
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    completedAt = (data["completedAt"] as? Number)?.toLong(),
                    transactionDate = data["transactionDate"] as? String ?: "",
                    
                    // Additional Information
                    notes = data["notes"] as? String ?: "",
                    status = OrderStatus.valueOf(data["status"] as? String ?: OrderStatus.CONFIRMED.name),
                    createdBy = data["createdBy"] as? String ?: "system"
                )
            }
        } catch (e: Exception) {
            println("Error getting all cash transactions: ${e.message}")
            emptyList()
        }
    }

    override suspend fun updateCustomerBalance(customerId: String, amount: Double, transactionType: CashTransactionType): Boolean = withContext(Dispatchers.IO) {
        try {
            val userDoc = firestore.collection("users").document(customerId).get().get()
            if (!userDoc.exists()) {
                println("Customer not found: $customerId")
                return@withContext false
            }

            val currentBalance = (userDoc.getDouble("balance") ?: 0.0)
            val balanceChange = when (transactionType) {
                CashTransactionType.GIVE -> amount      // Give cash = increase balance
                CashTransactionType.RECEIVE -> -amount  // Receive cash = decrease balance
            }
            val newBalance = currentBalance + balanceChange

            firestore.collection("users").document(customerId)
                .update("balance", newBalance, "updatedAt", System.currentTimeMillis())
                .get()

            println("Updated customer $customerId balance: $currentBalance -> $newBalance (${transactionType.name} $amount)")
            true
        } catch (e: Exception) {
            println("Error updating customer balance: ${e.message}")
            false
        }
    }
}
