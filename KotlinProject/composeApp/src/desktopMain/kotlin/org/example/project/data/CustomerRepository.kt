// CustomerRepository.kt
package org.example.project.data

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreException
import com.google.cloud.firestore.ListenerRegistration
import com.google.cloud.firestore.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

interface CustomerRepository {
    val customers: StateFlow<List<User>>
    val loading: StateFlow<Boolean>
    val error: StateFlow<String?>
    
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
    
    // Lifecycle methods
    fun startListening()
    fun stopListening()
}

object FirestoreCustomerRepository : CustomerRepository {
    private lateinit var firestore: Firestore
    private var listenerRegistration: ListenerRegistration? = null
    
    // Coroutine scope for listener callback processing
    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // StateFlow for reactive data
    private val _customers = MutableStateFlow<List<User>>(emptyList())
    override val customers: StateFlow<List<User>> = _customers.asStateFlow()
    
    private val _loading = MutableStateFlow<Boolean>(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()
    
    fun initialize(firestoreInstance: Firestore) {
        println("🔧 CUSTOMER REPOSITORY: Initializing repository object")
        println("   - Thread: ${Thread.currentThread().name}")
        firestore = firestoreInstance
        startListening()
    }
    
    override fun startListening() {
        if (listenerRegistration != null) {
            println("⚠️ CUSTOMER REPOSITORY: Listener already active - NOT creating duplicate listener")
            println("   - Thread: ${Thread.currentThread().name}")
            println("   - Current customers count: ${_customers.value.size}")
            return
        }
        
        if (!::firestore.isInitialized) {
            println("❌ CUSTOMER REPOSITORY: Firestore not initialized. Call initialize() first.")
            return
        }
        
        println("=".repeat(80))
        println("👂 CUSTOMER REPOSITORY: Starting Firestore listener on users collection")
        println("   - Thread: ${Thread.currentThread().name}")
        println("   - Repository instance: ${this.hashCode()}")
        println("   - This is the ONLY listener that will be created")
        println("=".repeat(80))
        _loading.value = true
        
        val usersCollection = firestore.collection("users")
        
        listenerRegistration = usersCollection.addSnapshotListener { snapshot, exception ->
            println("📡 CUSTOMER REPOSITORY: Listener callback triggered")
            println("   - Thread: ${Thread.currentThread().name}")
            println("   - Repository instance: ${this.hashCode()}")
            
            if (exception != null) {
                println("❌ CUSTOMER REPOSITORY: Listener error: ${exception.message}")
                _error.value = "Failed to load customers: ${exception.message}"
                _loading.value = false
                return@addSnapshotListener
            }
            
            if (snapshot == null) {
                println("⚠️ CUSTOMER REPOSITORY: Snapshot is null")
                _loading.value = false
                return@addSnapshotListener
            }
            
            // Process snapshot on IO dispatcher for consistency with other repository methods
            listenerScope.launch(Dispatchers.IO) {
                try {
                    println("📥 CUSTOMER REPOSITORY: Processing snapshot from Firestore")
                    println("   - Thread: ${Thread.currentThread().name}")
                    println("   - Document count: ${snapshot.documents.size}")
                    
                    val customersList = snapshot.documents.mapNotNull { doc ->
                        parseUserDocument(doc)
                    }
                    
                    println("✅ CUSTOMER REPOSITORY: Parsed ${customersList.size} customers from snapshot")
                    println("   - Updating StateFlow (this will notify all ViewModels)")
                    println("   - Previous customers count: ${_customers.value.size}")
                    
                    _customers.value = customersList
                    _error.value = null
                    _loading.value = false
                    
                    println("✅ CUSTOMER REPOSITORY: StateFlow updated successfully")
                    println("   - New customers count: ${customersList.size}")
                    println("   - All ViewModels using this repository will receive this update automatically")
                    println("   - Repository instance: ${this.hashCode()}")
                    println("-".repeat(80))
                } catch (e: Exception) {
                    println("❌ CUSTOMER REPOSITORY: Error parsing documents: ${e.message}")
                    _error.value = "Error parsing customer data: ${e.message}"
                    _loading.value = false
                }
            }
        }
        
        println("✅ CUSTOMER REPOSITORY: Listener attached successfully")
        println("   - Listener registration: ${listenerRegistration.hashCode()}")
    }
    
    override fun stopListening() {
        println("🛑 CUSTOMER REPOSITORY: Stopping Firestore listener")
        println("   - Thread: ${Thread.currentThread().name}")
        println("   - Repository instance: ${this.hashCode()}")
        listenerRegistration?.remove()
        listenerRegistration = null
        // Note: listenerScope is not cancelled here to allow pending operations to complete
        println("✅ CUSTOMER REPOSITORY: Firestore listener stopped")
    }
    
    private fun parseUserDocument(doc: DocumentSnapshot): User? {
        val data = doc.data ?: return null
        
        return User(
            id = doc.id,
            email = data["email"] as? String ?: "",
            name = data["name"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            address = data["address"] as? String ?: "",
            notes = data["notes"] as? String ?: "",
            balance = (data["balance"] as? Number)?.toDouble() ?: 0.0,
            customerId = doc.id,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            googleId = data["googleId"] as? String,
            fcmToken = data["fcmToken"] as? String,
            isGoogleSignIn = data["isGoogleSignIn"] as? Boolean,
            lastTokenUpdate = (data["lastTokenUpdate"] as? Number)?.toLong(),
            profilePictureUrl = data["profilePictureUrl"] as? String
        )
    }
    
    override suspend fun getCustomerById(id: String): User? = withContext(Dispatchers.IO) {
        // First check cached data
        _customers.value.find { it.id == id || it.customerId == id }
            ?: run {
                // Fallback to direct query if not in cache
                try {
                    val docRef = firestore.collection("users").document(id)
                    val doc = docRef.get().get()
                    if (doc.exists()) parseUserDocument(doc) else null
                } catch (e: Exception) {
                    println("❌ CUSTOMER REPOSITORY: Error fetching customer by ID: ${e.message}")
                    null
                }
            }
    }
    
    override suspend fun addCustomer(user: User): String = withContext(Dispatchers.IO) {
        try {
            println("👤 CUSTOMER REPOSITORY: Adding new customer")
            println("   - Repository instance: ${this.hashCode()}")
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
            // Listener will automatically update _customers - no manual refresh needed
            
            println("✅ CUSTOMER REPOSITORY: Customer added to Firestore")
            println("   - Document ID: $docId")
            println("   - Customer ID: $docId")
            println("   - Listener will automatically detect this change and update StateFlow")
            println("   - All ViewModels will receive the update via StateFlow")
            
            return@withContext docId // Return the auto-generated document ID
        } catch (e: Exception) {
            println("❌ CUSTOMER REPOSITORY: Failed to add customer")
            println("   - Error: ${e.message}")
            throw e
        }
    }
    
    override suspend fun updateCustomer(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            println("🔄 CUSTOMER REPOSITORY: Updating customer")
            println("   - Repository instance: ${this.hashCode()}")
            println("   - Customer ID: ${user.id}")
            println("   - Customer name: ${user.name}")
            
            val userMap = mapOf(
                "email" to user.email,
                "name" to user.name,
                "phone" to user.phone,
                "address" to user.address,
                "notes" to user.notes,
                "balance" to user.balance,
                "updatedAt" to System.currentTimeMillis()
            )
            
            firestore.collection("users").document(user.id).update(userMap).get()
            // Listener will automatically update _customers - no manual refresh needed
            
            println("✅ CUSTOMER REPOSITORY: Customer updated in Firestore")
            println("   - Listener will automatically detect this change and update StateFlow")
            println("   - All ViewModels will receive the update via StateFlow")
            
            true
        } catch (e: Exception) {
            println("❌ CUSTOMER REPOSITORY: Failed to update customer: ${e.message}")
            false
        }
    }
    
    /**
     * Adds due amount to customer's balance
     * This is called after a bill is processed to update the customer's outstanding balance
     * Uses Firebase transaction to ensure atomicity
     */
    override suspend fun addToCustomerBalance(customerId: String, dueAmount: Double): Boolean = 
        withContext(Dispatchers.IO) {
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
                    transaction.update(
                        customerRef, 
                        "balance", newBalance, 
                        "updatedAt", System.currentTimeMillis()
                    )
                    
                println("✅ Customer balance updated successfully")
                Unit // Return Unit for transaction function
            }.get()
            
            println("✅ CUSTOMER REPOSITORY: Balance updated in Firestore")
            println("   - Listener will automatically detect this change and update StateFlow")
            println("   - All ViewModels will receive the update via StateFlow")
                
            true
            } catch (e: Exception) {
                println("❌ Error updating customer balance: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    
    // Recently Viewed Subcollection Methods
    override suspend fun addToRecentlyViewed(userId: String, productId: String): Boolean = 
        withContext(Dispatchers.IO) {
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
    
    override suspend fun getRecentlyViewed(userId: String): List<RecentlyViewedItem> = 
        withContext(Dispatchers.IO) {
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
    
    override suspend fun removeFromRecentlyViewed(userId: String, itemId: String): Boolean = 
        withContext(Dispatchers.IO) {
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
    override suspend fun addToWishlist(userId: String, productId: String): Boolean = 
        withContext(Dispatchers.IO) {
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
    
    override suspend fun getWishlist(userId: String): List<WishlistItem> = 
        withContext(Dispatchers.IO) {
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
    
    override suspend fun removeFromWishlist(userId: String, itemId: String): Boolean = 
        withContext(Dispatchers.IO) {
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
