package org.example.project.data

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreException
import com.google.cloud.firestore.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

interface PaymentRepository {
    suspend fun saveOrder(order: Order, inventoryItemIds: List<String>): Boolean
}

class FirestorePaymentRepository(private val firestore: Firestore) : PaymentRepository {
    
    override suspend fun saveOrder(order: Order, inventoryItemIds: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            println("💾 PAYMENT REPOSITORY: Saving order to Firestore")
            println("   - Order ID: ${order.orderId}")
            println("   - Customer ID: ${order.customerId}")
            println("   - Total Amount: ₹${String.format("%.2f", order.totalAmount)}")
            
            val orderMap = mapOf(
                "orderId" to order.orderId,
                "customerId" to order.customerId, // Reference to users collection
                "totalProductValue" to order.totalProductValue, // Stored as finalAmount in Firestore for backward compatibility
                "finalAmount" to order.totalProductValue, // Backward compatibility
                "discountAmount" to order.discountAmount,
                "discountPercent" to (order.discountPercent ?: 0.0), // Store 0.0 if null
                "gstAmount" to order.gstAmount,
                "gstPercentage" to order.gstPercentage,
                "totalAmount" to order.totalAmount, // Total payable amount after GST and discount applied
                "isGstIncluded" to order.isGstIncluded,
                "createdAt" to order.createdAt,
                "updatedAt" to order.updatedAt,
                "invoiceUrl" to order.invoiceUrl,
                "transactionDate" to order.transactionDate,
                "notes" to order.notes,
                "items" to order.items.map { item ->
                    mapOf(
                        "barcodeId" to item.barcodeId,
                        "productId" to item.productId,
                        "quantity" to item.quantity,
                        "makingPercentage" to item.makingPercentage,
                        "labourCharges" to item.labourCharges,
                        "labourRate" to item.labourRate
                    )
                },
                "paymentSplit" to order.paymentSplit?.let { split ->
                    mapOf(
                        "bank" to split.bank, // Sum of bankAmount + cardAmount + onlineAmount
                        "cash" to split.cash,
                        "dueAmount" to split.dueAmount
                    )
                },
                "exchange_gold" to order.exchangeGold?.let { exchange ->
                    mapOf(
                        "productName" to exchange.productName,
                        "goldWeight" to exchange.goldWeight,
                        "goldPurity" to exchange.goldPurity,
                        "goldRate" to exchange.goldRate,
                        "finalGoldExchangePrice" to exchange.finalGoldExchangePrice,
                        "totalProductWeight" to exchange.totalProductWeight,
                        "percentage" to exchange.percentage
                    )
                }
            )
            
            val orderRef = firestore.collection("orders").document(order.orderId)
            val customerRef = firestore.collection("users").document(order.customerId)
            val dueAmount = order.paymentSplit?.dueAmount ?: 0.0
            val hasInventoryUpdates = inventoryItemIds.isNotEmpty()
            
            // Use Firebase transaction to atomically:
            // 1. Create order document
            // 2. Update customer balance (if due amount > 0)
            // 3. Delete inventory items (if any)
            val needsTransaction = dueAmount > 0 || hasInventoryUpdates
            
            if (needsTransaction) {
                // Retry transaction up to 3 times for concurrent order conflicts
                var retries = 3
                var success = false
                var lastError: Exception? = null
                
                while (retries > 0 && !success) {
                    try {
                        firestore.runTransaction { transaction: Transaction ->
                            // STEP 1: ALL READS FIRST (Firebase requirement)
                            // Validate inventory items exist before deletion and get product IDs
                            val productQuantityMap = mutableMapOf<String, Int>() // productId -> count of items to delete
                            
                            if (hasInventoryUpdates) {
                                inventoryItemIds.forEach { inventoryItemId ->
                                    val inventoryRef = firestore.collection("inventory").document(inventoryItemId)
                                    val inventoryDoc = transaction.getDocument(inventoryRef)
                                    if (!inventoryDoc.exists()) {
                                        throw Exception("Inventory item $inventoryItemId not found - may have been sold to another customer")
                                    }
                                    
                                    // Get product_id from inventory document
                                    val productId = inventoryDoc.getString("product_id") ?: ""
                                    if (productId.isNotEmpty()) {
                                        productQuantityMap[productId] = productQuantityMap.getOrDefault(productId, 0) + 1
                                    }
                                }
                                println("✅ PAYMENT REPOSITORY: All inventory items validated within transaction")
                                println("   - Products to update: ${productQuantityMap.keys.size}")
                            }
                            
                            // Read product documents to get current quantities
                            val productQuantities = mutableMapOf<String, Int>()
                            
                            productQuantityMap.keys.forEach { productId ->
                                val productRef = firestore.collection("products").document(productId)
                                val productDoc = transaction.getDocument(productRef)
                                if (productDoc.exists()) {
                                    val quantityStr = productDoc.getString("quantity") ?: "0"
                                    val currentQuantity = quantityStr.toIntOrNull() ?: 0
                                    productQuantities[productId] = currentQuantity
                                } else {
                                    productQuantities[productId] = 0
                                }
                            }
                            
                            var currentBalance = 0.0
                            if (dueAmount > 0) {
                                val customerDoc = transaction.getDocument(customerRef)
                                if (!customerDoc.exists()) {
                                    throw Exception("Customer not found: ${order.customerId}")
                                }
                                currentBalance = (customerDoc.getDouble("balance") ?: 0.0)
                            }
                            
                            // STEP 2: ALL WRITES AFTER READS
                            // 1. Create order document
                            transaction.set(orderRef, orderMap)
                            
                            // 2. Update customer balance if due amount > 0
                            if (dueAmount > 0) {
                                val newBalance = currentBalance + dueAmount
                                // Firestore supports multiple fields in single update call
                                transaction.update(customerRef, mapOf(
                                    "balance" to newBalance,
                                    "updatedAt" to System.currentTimeMillis()
                                ))
                                println("✅ PAYMENT REPOSITORY: Customer balance updated atomically")
                                println("   - Customer Balance: $currentBalance -> $newBalance")
                            }
                            
                            // 3. Delete inventory items atomically
                            if (hasInventoryUpdates) {
                                inventoryItemIds.forEach { inventoryItemId ->
                                    val inventoryRef = firestore.collection("inventory").document(inventoryItemId)
                                    transaction.delete(inventoryRef)
                                }
                                println("✅ PAYMENT REPOSITORY: ${inventoryItemIds.size} inventory items deleted atomically")
                            }
                            
                            // 4. Update product quantities atomically (deduct deleted inventory items)
                            productQuantityMap.forEach { (productId, deleteCount) ->
                                val productRef = firestore.collection("products").document(productId)
                                val currentQuantity = productQuantities[productId] ?: 0
                                val newQuantity = (currentQuantity - deleteCount).coerceAtLeast(0)
                                
                                transaction.update(productRef, mapOf(
                                    "quantity" to newQuantity.toString(),
                                    "updated_at" to System.currentTimeMillis()
                                ))
                                println("✅ PAYMENT REPOSITORY: Product quantity updated atomically")
                                println("   - Product ID: $productId")
                                println("   - Quantity: $currentQuantity -> $newQuantity (deducted $deleteCount)")
                            }
                            
                            println("✅ PAYMENT REPOSITORY: Order, balance, inventory, and product quantities updated atomically")
                            println("   - Order ID: ${order.orderId}")
                            Unit
                        }.get()
                        success = true
                    } catch (e: Exception) {
                        lastError = e
                        retries--
                        
                        // Check if error is retryable (inventory conflict or transaction conflict)
                        // Check both exception type and error message for Firestore exceptions
                        val isRetryable = when {
                            e is FirestoreException -> {
                                // Check Firestore exception message for retryable conditions
                                val message = e.message ?: ""
                                message.contains("ABORTED", ignoreCase = true) ||
                                message.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
                                message.contains("not found", ignoreCase = true)
                            }
                            else -> {
                                // For non-Firestore exceptions, check message
                                e.message?.contains("not found", ignoreCase = true) == true ||
                                e.message?.contains("ABORTED", ignoreCase = true) == true
                            }
                        }
                        
                        if (retries == 0 || !isRetryable) {
                            // Not retryable or out of retries - throw the error
                            throw e
                        }
                        
                        // Wait before retry (exponential backoff)
                        val delayMs = 100L * (3 - retries)
                        println("⚠️ PAYMENT REPOSITORY: Transaction conflict, retrying in ${delayMs}ms (${retries} retries left)")
                        delay(delayMs)
                }
                }
                
                if (!success) {
                    throw lastError ?: Exception("Failed to save order after retries")
                }
            } else {
                // If no due amount and no inventory updates, still use transaction for consistency
                // This ensures atomic order creation even if no other updates are needed
                firestore.runTransaction { transaction: Transaction ->
                    transaction.set(orderRef, orderMap)
                    println("✅ PAYMENT REPOSITORY: Order created atomically")
                    Unit
                }.get()
                println("✅ PAYMENT REPOSITORY: Order saved successfully (no balance/inventory update needed)")
            }
            
            println("✅ PAYMENT REPOSITORY: Order saved successfully")
            println("   - Document ID: ${order.orderId}")
            true
        } catch (e: FirestoreException) {
            // Handle Firestore-specific errors
            val errorMsg = when {
                e.message?.contains("not found", ignoreCase = true) == true -> {
                    "Inventory item not found - may have been sold to another customer"
                }
                e.message?.contains("permission", ignoreCase = true) == true || 
                e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true -> {
                    "Permission denied - check Firestore security rules"
                }
                e.message?.contains("deadline", ignoreCase = true) == true ||
                e.message?.contains("DEADLINE_EXCEEDED", ignoreCase = true) == true -> {
                    "Request timeout - please try again"
                }
                e.message?.contains("ABORTED", ignoreCase = true) == true -> {
                    "Transaction conflict - inventory may have been modified. Please try again"
                }
                else -> "Database error: ${e.message ?: "Unknown error"}"
            }
            println("❌ PAYMENT REPOSITORY: $errorMsg")
            false
        } catch (e: Exception) {
            // Handle other errors
            val errorMsg = when {
                e.message?.contains("not found", ignoreCase = true) == true -> {
                    "One or more items are no longer available - may have been sold to another customer"
                }
                e.message?.contains("Customer not found", ignoreCase = true) == true -> {
                    "Customer not found: ${e.message}"
                }
                else -> "Failed to save order: ${e.message ?: "Unknown error"}"
            }
            println("❌ PAYMENT REPOSITORY: $errorMsg")
            e.printStackTrace()
            false
        }
    }
}
