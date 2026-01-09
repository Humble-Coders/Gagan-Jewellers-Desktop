// CustomerRepository.kt
package org.example.project.data

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

interface CustomerRepository {
    suspend fun getAllCustomers(): List<User>
    suspend fun getCustomerById(id: String): User?
    suspend fun addCustomer(user: User): String
    suspend fun updateCustomer(user: User): Boolean
    suspend fun addToCustomerBalance(customerId: String, dueAmount: Double): Boolean
    
    // Subcollection methods for recentlyViewed
    suspend fun addToRecentlyViewed(userId: String, productId: String): Boolean
    suspend fun getRecentlyViewed(userId: String): List<RecentlyViewedItem>
    suspend fun removeFromRecentlyViewed(userId: String, itemId: String): Boolean
    
    // Subcollection methods for wishlist
    suspend fun addToWishlist(userId: String, productId: String): Boolean
    suspend fun getWishlist(userId: String): List<WishlistItem>
    suspend fun removeFromWishlist(userId: String, itemId: String): Boolean
}

class FirestoreCustomerRepository(private val firestore: Firestore) : CustomerRepository {
    override suspend fun getAllCustomers(): List<User> = withContext(Dispatchers.IO) {
        val customersCollection = firestore.collection("users")
        val future = customersCollection.get()

        val snapshot = future.get()
        snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null

            User(
                id = doc.id, // This will be the auto-generated document ID
                email = data["email"] as? String ?: "",
                name = data["name"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                address = data["address"] as? String ?: "",
                notes = data["notes"] as? String ?: "",
                balance = (data["balance"] as? Number)?.toDouble() ?: 0.0,
                customerId = doc.id, // Set customer_id to the document ID
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                // Additional fields from the image (keeping as null initially)
                googleId = data["googleId"] as? String,
                fcmToken = data["fcmToken"] as? String,
                isGoogleSignIn = data["isGoogleSignIn"] as? Boolean,
                lastTokenUpdate = (data["lastTokenUpdate"] as? Number)?.toLong(),
                profilePictureUrl = data["profilePictureUrl"] as? String
                // Note: recentlyViewed and wishlist are now subcollections
            )
        }
    }

    override suspend fun getCustomerById(id: String): User? = withContext(Dispatchers.IO) {
        val docRef = firestore.collection("users").document(id)
        val future = docRef.get()
        val doc = future.get()

        if (doc.exists()) {
            val data = doc.data ?: return@withContext null

            User(
                id = doc.id, // This will be the auto-generated document ID
                email = data["email"] as? String ?: "",
                name = data["name"] as? String ?: "",
                phone = data["phone"] as? String ?: "",
                address = data["address"] as? String ?: "",
                notes = data["notes"] as? String ?: "",
                balance = (data["balance"] as? Number)?.toDouble() ?: 0.0,
                customerId = doc.id, // Set customer_id to the document ID
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                // Additional fields from the image (keeping as null initially)
                googleId = data["googleId"] as? String,
                fcmToken = data["fcmToken"] as? String,
                isGoogleSignIn = data["isGoogleSignIn"] as? Boolean,
                lastTokenUpdate = (data["lastTokenUpdate"] as? Number)?.toLong(),
                profilePictureUrl = data["profilePictureUrl"] as? String
                // Note: recentlyViewed and wishlist are now subcollections
            )
        } else null
    }

    override suspend fun addCustomer(user: User): String = withContext(Dispatchers.IO) {
        try {
            println("👤 CUSTOMER REPOSITORY: Adding new customer")
            println("   - Name: ${user.name}")
            println("   - Email: ${user.email}")
            println("   - Phone: ${user.phone}")
            println("   - Address: ${user.address}")
            println("   - Notes: ${user.notes}")
            
            // For new customers, use auto-generated document ID
            val docRef = firestore.collection("users").document()
            val docId = docRef.id

            val userMap = mapOf(
                "email" to user.email,
                "name" to user.name,
                "phone" to user.phone,
                "address" to user.address,
                "notes" to user.notes,
                "balance" to 0.0, // Initialize balance to 0
                "customerId" to docId, // Set customer_id to the auto-generated document ID
                "createdAt" to System.currentTimeMillis(),
                // Additional fields from the image (keeping as null initially)
                "googleId" to null,
                "fcmToken" to null,
                "isGoogleSignIn" to null,
                "lastTokenUpdate" to null,
                "profilePictureUrl" to null
                // Note: recentlyViewed and wishlist are now subcollections
            )

            docRef.set(userMap).get()
            
            println("✅ CUSTOMER REPOSITORY: Customer added successfully")
            println("   - Document ID: $docId")
            println("   - Customer ID: $docId")

            return@withContext docId // Return the auto-generated document ID
        } catch (e: Exception) {
            println("❌ CUSTOMER REPOSITORY: Failed to add customer")
            println("   - Error: ${e.message}")
            throw e
        }
    }

    override suspend fun updateCustomer(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val userMap = mapOf(
                "email" to user.email,
                "name" to user.name,
                "phone" to user.phone,
                "address" to user.address,
                "notes" to user.notes,
                "balance" to user.balance
            )

            firestore.collection("users").document(user.id).update(userMap).get()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Adds due amount to customer's balance
     * This is called after a bill is processed to update the customer's outstanding balance
     * Uses Firebase transaction to ensure atomicity
     */
    override suspend fun addToCustomerBalance(customerId: String, dueAmount: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            println("💰 CUSTOMER REPOSITORY: Adding to customer balance")
            println("   - Customer ID: $customerId")
            println("   - Due Amount: $dueAmount")
            
            val customerRef = firestore.collection("users").document(customerId)
            
            firestore.runTransaction { transaction: Transaction ->
                val customerDoc = transaction.getDocument(customerRef)
            
                if (!customerDoc.exists()) {
                    throw Exception("Customer not found with ID: $customerId")
                }
                
                val currentBalance = (customerDoc.getDouble("balance") ?: 0.0)
                val newBalance = currentBalance + dueAmount
                
                println("   - Current Balance: $currentBalance")
                println("   - New Balance: $newBalance")
                
                // Update the balance atomically
                transaction.update(customerRef, "balance", newBalance, "updatedAt", System.currentTimeMillis())
                
                println("✅ Customer balance updated successfully")
                Unit // Return Unit for transaction function
            }.get()
            
                true
        } catch (e: Exception) {
            println("❌ Error updating customer balance: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    // Recently Viewed Subcollection Methods
    override suspend fun addToRecentlyViewed(userId: String, productId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            println("👁️ CUSTOMER REPOSITORY: Adding to recently viewed")
            println("   - User ID: $userId")
            println("   - Product ID: $productId")
            
            val docRef = firestore.collection("users").document(userId).collection("recentlyViewed").document()
            val item = RecentlyViewedItem(
                id = docRef.id,
                productId = productId,
                viewedAt = System.currentTimeMillis()
            )
            
            val itemMap = mapOf(
                "productId" to item.productId,
                "viewedAt" to item.viewedAt
            )
            
            docRef.set(itemMap).get()
            
            println("✅ CUSTOMER REPOSITORY: Added to recently viewed successfully")
            println("   - Item ID: ${item.id}")
            true
        } catch (e: Exception) {
            println("❌ CUSTOMER REPOSITORY: Failed to add to recently viewed")
            println("   - Error: ${e.message}")
            false
        }
    }
    
    override suspend fun getRecentlyViewed(userId: String): List<RecentlyViewedItem> = withContext(Dispatchers.IO) {
        try {
            println("👁️ CUSTOMER REPOSITORY: Getting recently viewed")
            println("   - User ID: $userId")
            
            val future = firestore.collection("users").document(userId).collection("recentlyViewed")
                .orderBy("viewedAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
                .limit(20) // Limit to 20 most recent items
                .get()
            
            val snapshot = future.get()
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                RecentlyViewedItem(
                    id = doc.id,
                    productId = data["productId"] as? String ?: "",
                    viewedAt = (data["viewedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
            
            println("✅ CUSTOMER REPOSITORY: Retrieved ${items.size} recently viewed items")
            items
        } catch (e: Exception) {
            println("❌ CUSTOMER REPOSITORY: Failed to get recently viewed")
            println("   - Error: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun removeFromRecentlyViewed(userId: String, itemId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            println("👁️ CUSTOMER REPOSITORY: Removing from recently viewed")
            println("   - User ID: $userId")
            println("   - Item ID: $itemId")
            
            firestore.collection("users").document(userId).collection("recentlyViewed").document(itemId).delete().get()
            
            println("✅ CUSTOMER REPOSITORY: Removed from recently viewed successfully")
            true
        } catch (e: Exception) {
            println("❌ CUSTOMER REPOSITORY: Failed to remove from recently viewed")
            println("   - Error: ${e.message}")
            false
        }
    }
    
    // Wishlist Subcollection Methods
    override suspend fun addToWishlist(userId: String, productId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            println("❤️ CUSTOMER REPOSITORY: Adding to wishlist")
            println("   - User ID: $userId")
            println("   - Product ID: $productId")
            
            val docRef = firestore.collection("users").document(userId).collection("wishlist").document()
            val item = WishlistItem(
                id = docRef.id,
                productId = productId,
                addedAt = System.currentTimeMillis()
            )
            
            val itemMap = mapOf(
                "productId" to item.productId,
                "addedAt" to item.addedAt
            )
            
            docRef.set(itemMap).get()
            
            println("✅ CUSTOMER REPOSITORY: Added to wishlist successfully")
            println("   - Item ID: ${item.id}")
            true
        } catch (e: Exception) {
            println("❌ CUSTOMER REPOSITORY: Failed to add to wishlist")
            println("   - Error: ${e.message}")
            false
        }
    }
    
    override suspend fun getWishlist(userId: String): List<WishlistItem> = withContext(Dispatchers.IO) {
        try {
            println("❤️ CUSTOMER REPOSITORY: Getting wishlist")
            println("   - User ID: $userId")
            
            val future = firestore.collection("users").document(userId).collection("wishlist")
                .orderBy("addedAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
                .get()
            
            val snapshot = future.get()
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                WishlistItem(
                    id = doc.id,
                    productId = data["productId"] as? String ?: "",
                    addedAt = (data["addedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
            
            println("✅ CUSTOMER REPOSITORY: Retrieved ${items.size} wishlist items")
            items
        } catch (e: Exception) {
            println("❌ CUSTOMER REPOSITORY: Failed to get wishlist")
            println("   - Error: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun removeFromWishlist(userId: String, itemId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            println("❤️ CUSTOMER REPOSITORY: Removing from wishlist")
            println("   - User ID: $userId")
            println("   - Item ID: $itemId")
            
            firestore.collection("users").document(userId).collection("wishlist").document(itemId).delete().get()
            
            println("✅ CUSTOMER REPOSITORY: Removed from wishlist successfully")
            true
        } catch (e: Exception) {
            println("❌ CUSTOMER REPOSITORY: Failed to remove from wishlist")
            println("   - Error: ${e.message}")
            false
        }
    }
}