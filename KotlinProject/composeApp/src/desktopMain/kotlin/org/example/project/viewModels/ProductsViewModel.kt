package org.example.project.viewModels

// ViewModel.kt
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.example.project.data.Category
import org.example.project.data.Material
import org.example.project.data.Product
import org.example.project.data.GroupedProduct
import org.example.project.data.ProductRepository
import org.example.project.data.StonesRepository
import org.example.project.data.Stone
import org.example.project.data.InventoryRepository
import org.example.project.data.InventoryItem

// Data class to hold cached inventory information for a product
data class InventoryData(
    val availableCount: Int = 0,
    val barcodeIds: List<String> = emptyList()
)

class ProductsViewModel(
    internal val repository: ProductRepository,
    private val stonesRepository: StonesRepository? = null,
    private val inventoryRepository: InventoryRepository? = null
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State for products list
    private val _products = mutableStateOf<List<Product>>(emptyList())
    val products: State<List<Product>> = _products

    // State for categories
    private val _categories = mutableStateOf<List<Category>>(emptyList())
    val categories: State<List<Category>> = _categories

    // State for materials
    private val _materials = mutableStateOf<List<Material>>(emptyList())
    val materials: State<List<Material>> = _materials

    // Loading state
    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    // Error state
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    // Refresh trigger for inventory changes
    private val _inventoryRefreshTrigger = mutableStateOf(0)
    val inventoryRefreshTrigger: State<Int> = _inventoryRefreshTrigger

    // Inventory data cache
    private val _inventoryData = mutableStateOf<Map<String, InventoryData>>(emptyMap())
    val inventoryData: State<Map<String, InventoryData>> = _inventoryData

    // Inventory loading state
    private val _inventoryLoading = mutableStateOf(false)
    val inventoryLoading: State<Boolean> = _inventoryLoading

    // Selected product state
    private val _currentProduct = mutableStateOf<Product?>(null)
    val currentProduct: State<Product?> = _currentProduct

    // Featured products state
    private val _featuredProducts = mutableStateOf<List<Product>>(emptyList())
    val featuredProducts: State<List<Product>> = _featuredProducts

    init {
        loadProducts()
        loadCategories()
        loadMaterials()
        loadStoneSuggestions()
        loadFeaturedProducts()
    }

    /**
     * Load inventory data for all products asynchronously
     * This prevents UI blocking by loading inventory data in the background
     */
    fun loadInventoryData() {
        if (inventoryRepository == null) return

        viewModelScope.launch {
            _inventoryLoading.value = true
            try {
                val allProducts = _products.value
                val inventoryDataMap = mutableMapOf<String, InventoryData>()

                // Load inventory data for each product
                allProducts.forEach { product ->
                    try {
                        val availableItems = inventoryRepository.getAvailableInventoryItemsByProductId(product.id)
                        val allItems = inventoryRepository.getInventoryItemsByProductId(product.id)
                        val barcodeIds = allItems.map { it.barcodeId }

                        inventoryDataMap[product.id] = InventoryData(
                            availableCount = availableItems.size,
                            barcodeIds = barcodeIds
                        )
                    } catch (e: Exception) {
                        println("Error loading inventory for product ${product.id}: ${e.message}")
                        inventoryDataMap[product.id] = InventoryData()
                    }
                }

                _inventoryData.value = inventoryDataMap
                println("✅ Loaded inventory data for ${inventoryDataMap.size} products")
            } catch (e: Exception) {
                println("❌ Error loading inventory data: ${e.message}")
                _error.value = "Failed to load inventory data: ${e.message}"
            } finally {
                _inventoryLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Trigger refresh of inventory-based data (like grouped products)
     * This should be called whenever inventory changes occur
     */
    fun triggerInventoryRefresh() {
        _inventoryRefreshTrigger.value = _inventoryRefreshTrigger.value + 1
        println("🔄 Inventory refresh triggered: ${_inventoryRefreshTrigger.value}")

        // Reload inventory data asynchronously
        loadInventoryData()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _products.value = repository.getAllProducts()
                _error.value = null

                // Load inventory data after products are loaded
                loadInventoryData()
            } catch (e: Exception) {
                _error.value = "Failed to load products: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Stones suggestions
    private val _stoneNames = mutableStateOf<List<String>>(emptyList())
    val stoneNames: State<List<String>> = _stoneNames

    private val _stonePurities = mutableStateOf<List<String>>(emptyList())
    val stonePurities: State<List<String>> = _stonePurities
    // Keep stoneColors for backward compatibility (maps to stonePurities)
    val stoneColors: State<List<String>> = _stonePurities

    private val _stones = mutableStateOf<List<Stone>>(emptyList())
    val stones: State<List<Stone>> = _stones

    fun loadStoneSuggestions() {
        if (stonesRepository == null) return
        viewModelScope.launch {
            try {
                _stoneNames.value = stonesRepository.getAllStoneNames()
                _stonePurities.value = stonesRepository.getAllStonePurities()
                _stones.value = stonesRepository.getAllStones()
            } catch (e: Exception) {
                _error.value = "Failed to load stones: ${e.message}"
            }
        }
    }

    fun addStoneSuggestion(name: String, onComplete: (String) -> Unit) {
        if (stonesRepository == null) return
        viewModelScope.launch {
            try {
                val id = stonesRepository.addStone(name)
                loadStoneSuggestions()
                onComplete(id)
            } catch (e: Exception) {
                _error.value = "Failed to add stone: ${e.message}"
            }
        }
    }

    fun updateProductQuantity(productId: String, newQuantity: Int) {
        viewModelScope.launch {
            try {
                val product = repository.getProductById(productId)
                if (product != null) {
                    val updatedProduct = product.copy(quantity = newQuantity)
                    val success = repository.updateProduct(updatedProduct)
                    if (success) {
                        loadProducts() // Refresh the list
                        _error.value = null
                    } else {
                        _error.value = "Failed to update quantity"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to update quantity: ${e.message}"
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                _categories.value = repository.getCategories()
            } catch (e: Exception) {
                _error.value = "Failed to load categories: ${e.message}"
            }
        }
    }

    private fun loadMaterials() {
        viewModelScope.launch {
            try {
                _materials.value = repository.getMaterials()
            } catch (e: Exception) {
                _error.value = "Failed to load materials: ${e.message}"
            }
        }
    }

    private fun loadFeaturedProducts() {
        viewModelScope.launch {
            try {
                _featuredProducts.value = repository.getFeaturedProducts()
            } catch (e: Exception) {
                _error.value = "Failed to load featured products: ${e.message}"
            }
        }
    }

    /**
     * Group products by product ID for dashboard display
     * Each product is shown individually with its inventory quantity
     * Note: Quantity is now fetched from inventory collection using where condition on product ID
     */
    fun getGroupedProducts(): List<GroupedProduct> {
        val allProducts = _products.value

        // Each product is shown individually (no grouping by commonId anymore)
        return allProducts.map { product ->
                    val inventoryQuantity = getAvailableInventoryCountForProduct(product.id)
                    GroupedProduct(
                        baseProduct = product,
                        quantity = inventoryQuantity, // Quantity from inventory collection
                        individualProducts = listOf(product),
                        barcodeIds = getBarcodeIdsForProduct(product.id), // Get barcodes from inventory
                commonId = null // commonId removed from Product model
                )
            }
    }

    /**
     * Get all grouped products including sold items (for administrative purposes)
     * Note: This method now works with inventory items instead of barcodeIds
     */
    fun getAllGroupedProductsIncludingSold(): List<GroupedProduct> {
        val allProducts = _products.value

        // Each product is shown individually (no grouping by commonId anymore)
        return allProducts.map { product ->
                    GroupedProduct(
                        baseProduct = product,
                        quantity = 1,
                        individualProducts = listOf(product),
                        barcodeIds = emptyList(), // Barcodes are now in inventory collection
                commonId = null // commonId removed from Product model
                )
            }
    }

    // New: add suggestion helpers used by UI
    fun addCategorySuggestion(name: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val id = repository.addCategory(name)
                loadCategories()
                onComplete(id)
            } catch (e: Exception) {
                _error.value = "Failed to add category: ${e.message}"
            }
        }
    }

    fun addCompleteCategory(category: Category, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val id = repository.addCompleteCategory(category)
                loadCategories()
                onComplete(id)
            } catch (e: Exception) {
                _error.value = "Failed to add category: ${e.message}"
            }
        }
    }

    fun updateCategory(category: Category, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.updateCategory(category)
                if (success) {
                    loadCategories() // Refresh the list after updating
                }
                onComplete(success)
            } catch (e: Exception) {
                _error.value = "Failed to update category: ${e.message}"
                onComplete(false)
            }
        }
    }

    fun deleteCategory(categoryId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.deleteCategory(categoryId)
                if (success) {
                    loadCategories() // Refresh the list after deleting
                }
                onComplete(success)
            } catch (e: Exception) {
                _error.value = "Failed to delete category: ${e.message}"
                onComplete(false)
            }
        }
    }

    fun addMaterialSuggestion(name: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val id = repository.addMaterial(name)
                loadMaterials()
                onComplete(id)
            } catch (e: Exception) {
                _error.value = "Failed to add material: ${e.message}"
            }
        }
    }

    fun addMaterialTypeSuggestion(materialId: String, type: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val ok = repository.addMaterialType(materialId, type)
                loadMaterials()
                onComplete(ok)
            } catch (e: Exception) {
                _error.value = "Failed to add material type: ${e.message}"
            }
        }
    }

    fun selectProduct(productId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val product = repository.getProductById(productId)
                _currentProduct.value = product
                println("Product loaded for editing: $productId - ${product?.name}")
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to get product: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun getProductByBarcodeId(barcodeId: String): Product? {
        return try {
            // First get the inventory item by barcode
            val inventoryItem = inventoryRepository?.getInventoryItemByBarcodeId(barcodeId)
            if (inventoryItem != null) {
                // Then get the product by product ID
                repository.getProductById(inventoryItem.productId)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Failed to get product by barcode ID: $barcodeId - ${e.message}")
            null
        }
    }

    fun clearCurrentProduct() {
        _currentProduct.value = null
    }

    fun createNewProduct() {
        _currentProduct.value = Product()
    }

    fun addProduct(product: Product) {
        println("=== ProductsViewModel.addProduct() called ===")
        println("Product details - Name: '${product.name}', ID: '${product.id}'")
        viewModelScope.launch {
            _loading.value = true
            try {
                println("📤 Calling repository.addProduct...")
                val result = repository.addProduct(product)
                println("📥 Repository.addProduct completed")
                loadProducts() // Refresh the list
                loadFeaturedProducts() // Refresh featured products list
                _error.value = null
                println("✅ Product added successfully, loading products...")
            } catch (e: Exception) {
                println("💥 Exception in addProduct: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to add product: ${e.message}"
            } finally {
                _loading.value = false
                println("🏁 addProduct operation completed")
            }
        }
    }

    suspend fun generateUniqueBarcodes(quantity: Int, digits: Int): List<String> {
        return try {
            inventoryRepository?.generateUniqueBarcodes(quantity, digits) ?: emptyList()
        } catch (e: Exception) {
            println("Failed to generate unique barcodes: ${e.message}")
            emptyList()
        }
    }

    fun generateBarcode(digits: Int): String {
        return inventoryRepository?.generateBarcode(digits) ?: ""
    }

    fun duplicateProductWithBarcode(productId: String, barcodeId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Verify the product exists (but don't modify products collection)
                val productToDuplicate = repository.getProductById(productId)
                if (productToDuplicate == null) {
                    _error.value = "Product not found for duplication"
                    return@launch
                }

                println("🔄 Duplicating product: ${productToDuplicate.name}")
                println("   - Original Product ID: $productId")
                println("   - New Barcode ID: $barcodeId")
                println("   - ⚠️ NO PRODUCT DOCUMENT WILL BE CREATED - ONLY INVENTORY ITEM")

                // Create ONLY a new inventory item with the same product ID and new barcode ID
                // NO product document is created in products collection
                val inventoryItem = InventoryItem(
                    productId = productId, // Use the same product ID
                    barcodeId = barcodeId // Use the new barcode ID
                )

                val inventoryId = inventoryRepository?.addInventoryItem(inventoryItem)
                if (inventoryId != null && inventoryId.isNotEmpty()) {
                    println("✅ Inventory item created successfully")
                    println("   - Inventory ID: $inventoryId")
                    println("   - Product ID: $productId (same as original)")
                    println("   - Barcode ID: $barcodeId (new)")
                    println("   - Products collection: UNCHANGED")

                    // Trigger immediate refresh of dashboard to show updated quantity
                    triggerInventoryRefresh()

                    _error.value = null
                    println("✅ Product duplicated successfully - ONLY inventory item added")
                } else {
                    _error.value = "Failed to create inventory item for duplicated product"
                    println("❌ Failed to create inventory item")
                }
            } catch (e: Exception) {
                println("💥 Exception in duplicateProductWithBarcode: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to duplicate product: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Generate a common ID for grouping multiple products
     */
    fun generateCommonId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "GRP_${timestamp}_${random}"
    }

    fun addProductsBatch(baseProduct: org.example.project.data.Product, barcodes: List<String>, onComplete: (() -> Unit)? = null) {
        println("=== ProductsViewModel.addProductsBatch() called ===")
        println("Base product - Name: '${baseProduct.name}', Barcodes count: ${barcodes.size}")
        println("Barcodes: $barcodes")
        viewModelScope.launch {
            _loading.value = true
            try {
                println("🔄 Starting batch processing...")

                // Create the base product first
                val productForDoc = baseProduct.copy(
                    id = "", // repository will assign auto document id
                    quantity = 1
                )
                println("📝 Creating base product")
                val productId = repository.addProduct(productForDoc)
                println("✅ Base product created with ID: $productId")

                // Create inventory items for each barcode
                for (code in barcodes) {
                    val inventoryItem = InventoryItem(
                        productId = productId,
                        barcodeId = code
                    )
                    inventoryRepository?.addInventoryItem(inventoryItem)
                    println("✅ Added inventory item for barcode: $code")
                }

                loadProducts()
                loadFeaturedProducts() // Refresh featured products list
                _error.value = null
                println("✅ Batch products and inventory items added successfully")
                onComplete?.let { it() }
            } catch (e: Exception) {
                println("💥 Exception in addProductsBatch: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to add products: ${e.message}"
            } finally {
                _loading.value = false
                println("🏁 addProductsBatch operation completed")
            }
        }
    }

    suspend fun addProductSync(product: Product): String? {
        return try {
            repository.addProduct(product)
        } catch (e: Exception) {
            println("Failed to add product: ${e.message}")
            null
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val success = repository.updateProduct(product)
                if (success) {
                    loadProducts() // Refresh the list
                    loadFeaturedProducts() // Refresh featured products list
                    _error.value = null
                } else {
                    _error.value = "Failed to update product"
                }
            } catch (e: Exception) {
                _error.value = "Failed to update product: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Update product (commonId removed, so this just updates the single product)
     */
    fun updateProductsWithCommonId(product: Product) {
        // Since commonId is removed, just update the single product
                    updateProduct(product)
    }

    fun deleteProduct(productId: String) {
        println("🗑️ VIEWMODEL: deleteProduct called")
        println("   - Product ID: $productId")
        println("   - Timestamp: ${System.currentTimeMillis()}")

        viewModelScope.launch {
            _loading.value = true
            try {
                println("🗑️ PRODUCT DELETION START")
                println("   - Product ID: $productId")

                // First, delete all inventory items (barcode documents) for this product
                val inventoryDeleted = deleteAllInventoryItemsForProduct(productId)
                println("   - Inventory deletion result: $inventoryDeleted")

                // Then delete the product document
                println("🗑️ DELETING PRODUCT DOCUMENT")
                println("   - Product ID: $productId")
                val success = repository.deleteProduct(productId)
                println("   - Product deletion result: $success")

                if (success) {
                    println("✅ Product and all related inventory items deleted successfully")
                    loadProducts() // Refresh the list
                    triggerInventoryRefresh() // Trigger dashboard refresh
                    _error.value = null
                } else {
                    println("❌ Failed to delete product document")
                    _error.value = "Failed to delete product"
                }
            } catch (e: Exception) {
                println("💥 Exception in deleteProduct: ${e.message}")
                _error.value = "Failed to delete product: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Delete grouped products properly.
     * For grouped products, keeps only the parent/base product and deletes all others.
     * For single products, deletes the product directly.
     */
    fun deleteGroupedProduct(groupedProduct: GroupedProduct) {
        println("🗑️ VIEWMODEL: deleteGroupedProduct called")
        println("   - Product Name: ${groupedProduct.baseProduct.name}")
        println("   - Product ID: ${groupedProduct.baseProduct.id}")
        println("   - Common ID: ${groupedProduct.commonId}")
        println("   - Timestamp: ${System.currentTimeMillis()}")

        viewModelScope.launch {
            _loading.value = true
            try {
                if (groupedProduct.commonId == null) {
                    // Single product - delete directly
                    println("🗑️ SINGLE PRODUCT DELETION")
                    println("   - Product ID: ${groupedProduct.baseProduct.id}")
                    println("   - Product Name: ${groupedProduct.baseProduct.name}")

                    // Delete all inventory items for this product first
                    val inventoryDeleted = deleteAllInventoryItemsForProduct(groupedProduct.baseProduct.id)
                    println("   - Inventory deletion result: $inventoryDeleted")

                    // Then delete the product document
                    val success = repository.deleteProduct(groupedProduct.baseProduct.id)
                    if (success) {
                        println("   - ✅ Single product and all inventory items deleted successfully")
                        loadProducts() // Refresh the list
                        triggerInventoryRefresh() // Trigger dashboard refresh
                        _error.value = null
                    } else {
                        println("   - ❌ Failed to delete single product")
                        _error.value = "Failed to delete product"
                    }
                } else {
                    // Grouped product - keep parent, delete others
                    println("🗑️ GROUPED PRODUCT DELETION")
                    println("   - Common ID: ${groupedProduct.commonId}")
                    println("   - Total products in group: ${groupedProduct.individualProducts.size}")
                    println("   - Parent Product ID: ${groupedProduct.baseProduct.id}")
                    println("   - Parent Product Name: ${groupedProduct.baseProduct.name}")

                    val parentProduct = groupedProduct.baseProduct
                    val productsToDelete = groupedProduct.individualProducts.filter { it.id != parentProduct.id }

                    println("   - Products to delete: ${productsToDelete.size}")

                    var deletedCount = 0
                    var inventoryDeletedCount = 0

                    // Delete each product and its inventory items
                    productsToDelete.forEach { productToDelete ->
                        try {
                            // First delete all inventory items for this product
                            val inventoryDeleted = deleteAllInventoryItemsForProduct(productToDelete.id)
                            if (inventoryDeleted) {
                                inventoryDeletedCount++
                            }

                            // Then delete the product document
                            val success = repository.deleteProduct(productToDelete.id)
                            if (success) {
                                deletedCount++
                                println("   - ✅ Deleted product and inventory: ${productToDelete.name} (${productToDelete.id})")
                            } else {
                                println("   - ❌ Failed to delete product: ${productToDelete.name} (${productToDelete.id})")
                            }
                        } catch (e: Exception) {
                            println("   - ❌ Error deleting product ${productToDelete.id}: ${e.message}")
                        }
                    }

                    println("🗑️ GROUPED PRODUCT DELETION COMPLETE")
                    println("   - Common ID: ${groupedProduct.commonId}")
                    println("   - Parent Product Preserved: ${parentProduct.name}")
                    println("   - Products Deleted: $deletedCount/${productsToDelete.size}")
                    println("   - Inventory Items Deleted: $inventoryDeletedCount")

                    loadProducts() // Refresh the list
                    triggerInventoryRefresh() // Trigger dashboard refresh
                    _error.value = null
                }
            } catch (e: Exception) {
                println("❌ Error in grouped product deletion: ${e.message}")
                _error.value = "Failed to delete grouped product: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteProductByBarcodeId(barcodeId: String) {
        println("🗑️ BARCODE DELETE REQUEST START")
        println("   - Barcode ID: $barcodeId")
        println("   - Timestamp: ${System.currentTimeMillis()}")

        viewModelScope.launch {
            _loading.value = true
            try {
                println("🔍 Searching for inventory item with barcode: $barcodeId")
                val inventoryItem = inventoryRepository?.getInventoryItemByBarcodeId(barcodeId)

                if (inventoryItem != null) {
                    println("✅ Inventory item found for barcode deletion:")
                    println("   - Inventory ID: ${inventoryItem.id}")
                    println("   - Product ID: ${inventoryItem.productId}")

                    // Actually delete the inventory item document
                    val success = inventoryRepository?.deleteInventoryItem(inventoryItem.id) ?: false

                    if (success) {
                        println("✅ Inventory item deleted successfully!")
                        println("   - Inventory ID: ${inventoryItem.id}")
                        println("   - Barcode: $barcodeId")
                        println("   - Document removed from Firestore")

                        // Trigger dashboard refresh to show updated quantities
                        triggerInventoryRefresh()
                        _error.value = null
                    } else {
                        println("❌ Failed to delete inventory item!")
                        _error.value = "Failed to delete barcode $barcodeId"
                    }
                } else {
                    println("❌ No inventory item found with barcode: $barcodeId")
                    _error.value = "No inventory item found with barcode $barcodeId"
                }
            } catch (e: Exception) {
                println("💥 Exception during barcode deletion:")
                println("   - Barcode: $barcodeId")
                println("   - Error: ${e.message}")
                _error.value = "Failed to delete product with barcode $barcodeId: ${e.message}"
            } finally {
                _loading.value = false
                println("🏁 BARCODE DELETE REQUEST END")
                println("   - Barcode ID: $barcodeId")
                println("   - Loading state: false")
            }
        }
    }

    // Helper function to get category name by ID
    fun getCategoryName(categoryId: String): String {
        return categories.value.find { it.id == categoryId }?.name ?: "Unknown Category"
    }

    // Helper function to get material name by ID
    fun getMaterialName(materialId: String): String {
        return materials.value.find { it.id == materialId }?.name ?: "Unknown Material"
    }

    // Category management methods
    fun addCategory(category: Category) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Add category to repository (you'll need to implement this in ProductRepository)
                // For now, just add to local state
                _categories.value = _categories.value + category
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to add category: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Update category in repository (you'll need to implement this in ProductRepository)
                // For now, just update local state
                _categories.value = _categories.value.map {
                    if (it.id == category.id) category else it
                }
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to update category: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Delete category from repository (you'll need to implement this in ProductRepository)
                // For now, just remove from local state
                _categories.value = _categories.value.filter { it.id != categoryId }
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to delete category: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Get categories by type
    fun getCategoriesByType(categoryType: String): List<Category> {
        return categories.value.filter { it.categoryType == categoryType && it.isActive }
    }

    // Get active categories only
    fun getActiveCategories(): List<Category> {
        return categories.value.filter { it.isActive }
    }

    // Clean up resources
    fun onCleared() {
        viewModelScope.cancel()
    }

    // Inventory Management Methods
    suspend fun addInventoryItem(inventoryItem: InventoryItem): String? {
        return try {
            println("🔍 DEBUG: inventoryRepository is ${if (inventoryRepository != null) "NOT NULL" else "NULL"}")
            if (inventoryRepository == null) {
                println("❌ ERROR: inventoryRepository is null!")
                return null
            }
            val result = inventoryRepository?.addInventoryItem(inventoryItem)
            println("🔍 DEBUG: addInventoryItem result: $result")
            result
        } catch (e: Exception) {
            println("Failed to add inventory item: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun updateInventoryItem(inventoryItem: InventoryItem): String? {
        return try {
            val success = inventoryRepository?.updateInventoryItem(inventoryItem) ?: false
            if (success) inventoryItem.id else null
        } catch (e: Exception) {
            println("Failed to update inventory item: ${e.message}")
            null
        }
    }

    suspend fun getInventoryItemByBarcodeId(barcodeId: String): InventoryItem? {
        return try {
            inventoryRepository?.getInventoryItemByBarcodeId(barcodeId)
        } catch (e: Exception) {
            println("Failed to get inventory item by barcode ID: ${e.message}")
            null
        }
    }

    /**
     * Get available inventory count for a product from cached data
     * This method uses cached inventory data to avoid blocking the UI thread
     */
    private fun getAvailableInventoryCountForProduct(productId: String): Int {
        return _inventoryData.value[productId]?.availableCount ?: 0
    }

    /**
     * Get all barcode IDs for a product from cached data
     * This method uses cached inventory data to avoid blocking the UI thread
     */
    private fun getBarcodeIdsForProduct(productId: String): List<String> {
        return _inventoryData.value[productId]?.barcodeIds ?: emptyList()
    }

    /**
     * Delete all inventory items (barcode documents) for a product
     * This method removes all inventory documents that reference the given product ID
     */
    private suspend fun deleteAllInventoryItemsForProduct(productId: String): Boolean {
        return try {
            if (inventoryRepository == null) {
                println("⚠️ Inventory repository is null, cannot delete inventory items for product: $productId")
                return false
            }

            println("🗑️ DELETING ALL INVENTORY ITEMS FOR PRODUCT")
            println("   - Product ID: $productId")

            // Get all inventory items for this product
            val inventoryItems = inventoryRepository.getInventoryItemsByProductId(productId)
            println("   - Found ${inventoryItems.size} inventory items to delete")

            if (inventoryItems.isEmpty()) {
                println("   - No inventory items found for this product")
                return true // Return true since there's nothing to delete
            }

            var deletedCount = 0
            var failedCount = 0

            // Delete each inventory item
            inventoryItems.forEach { inventoryItem ->
                try {
                    println("   - Attempting to delete inventory item: ${inventoryItem.id} (barcode: ${inventoryItem.barcodeId})")
                    val success = inventoryRepository.deleteInventoryItem(inventoryItem.id)
                    if (success) {
                        deletedCount++
                        println("   - ✅ Deleted inventory item: ${inventoryItem.barcodeId}")
                    } else {
                        failedCount++
                        println("   - ❌ Failed to delete inventory item: ${inventoryItem.barcodeId}")
                    }
                } catch (e: Exception) {
                    failedCount++
                    println("   - ❌ Error deleting inventory item ${inventoryItem.barcodeId}: ${e.message}")
                }
            }

            println("🗑️ INVENTORY DELETION COMPLETE")
            println("   - Product ID: $productId")
            println("   - Inventory Items Deleted: $deletedCount")
            println("   - Inventory Items Failed: $failedCount")

            // Return true if at least some items were deleted successfully
            deletedCount > 0
        } catch (e: Exception) {
            println("❌ Error deleting inventory items for product $productId: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Test method to verify delete functionality works
     * This bypasses all UI and directly tests the repository methods
     */
    fun testDeleteFunctionality(productId: String) {
        println("🧪 TEST DELETE FUNCTIONALITY START")
        println("   - Product ID: $productId")

        viewModelScope.launch {
            try {
                // Test 1: Check if product exists
                println("🧪 TEST 1: Checking if product exists")
                val product = repository.getProductById(productId)
                if (product != null) {
                    println("   - ✅ Product found: ${product.name}")
                } else {
                    println("   - ❌ Product not found")
                    return@launch
                }

                // Test 2: Check inventory items
                println("🧪 TEST 2: Checking inventory items")
                val inventoryItems = inventoryRepository?.getInventoryItemsByProductId(productId) ?: emptyList()
                println("   - Found ${inventoryItems.size} inventory items")

                // Test 3: Try to delete inventory items
                println("🧪 TEST 3: Testing inventory deletion")
                inventoryItems.forEach { item ->
                    println("   - Attempting to delete inventory item: ${item.barcodeId}")
                    val success = inventoryRepository?.deleteInventoryItem(item.id) ?: false
                    println("   - Result: $success")
                }

                // Test 4: Try to delete product
                println("🧪 TEST 4: Testing product deletion")
                val productDeleteSuccess = repository.deleteProduct(productId)
                println("   - Product deletion result: $productDeleteSuccess")

                // Refresh data
                loadProducts()
                triggerInventoryRefresh()

                println("🧪 TEST DELETE FUNCTIONALITY COMPLETE")

            } catch (e: Exception) {
                println("🧪 TEST DELETE ERROR: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}