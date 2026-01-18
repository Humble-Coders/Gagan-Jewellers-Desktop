package org.example.project.viewModels

// ViewModel.kt
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
        // Observe StateFlow from repository instead of calling load methods
        // The repository listener will automatically update these StateFlows
        viewModelScope.launch {
            repository.products.collect { productsList ->
                try {
                    _products.value = productsList
                    // Load inventory data after products are updated (only if products are not empty)
                    if (productsList.isNotEmpty() && inventoryRepository != null) {
                    loadInventoryData()
                    }
                } catch (e: Exception) {
                    println("⚠️ PRODUCTS VIEWMODEL: Failed to update products from StateFlow: ${e.message}")
                }
            }
        }
        
        viewModelScope.launch {
            repository.categories.collect { categoriesList ->
                try {
                    _categories.value = categoriesList
                } catch (e: Exception) {
                    println("⚠️ PRODUCTS VIEWMODEL: Failed to update categories from StateFlow: ${e.message}")
                }
            }
        }
        
        viewModelScope.launch {
            repository.materials.collect { materialsList ->
                try {
                    _materials.value = materialsList
                } catch (e: Exception) {
                    println("⚠️ PRODUCTS VIEWMODEL: Failed to update materials from StateFlow: ${e.message}")
                }
            }
        }
        
        viewModelScope.launch {
            repository.loading.collect { isLoading ->
                try {
                    _loading.value = isLoading
                } catch (e: Exception) {
                    println("⚠️ PRODUCTS VIEWMODEL: Failed to update loading from StateFlow: ${e.message}")
                }
            }
        }
        
        viewModelScope.launch {
            repository.error.collect { errorMessage ->
                try {
                    _error.value = errorMessage
                } catch (e: Exception) {
                    println("⚠️ PRODUCTS VIEWMODEL: Failed to update error from StateFlow: ${e.message}")
                }
            }
        }
        
        // Observe StateFlow from stones repository
        stonesRepository?.let { repo ->
            viewModelScope.launch {
                repo.stones.collect { stonesList ->
                    try {
                        _stones.value = stonesList
                        _stoneNames.value = stonesList.map { it.name }.distinct().sorted()
                        _stonePurities.value = stonesList
                            .flatMap { it.types.map { stoneType -> stoneType.purity } }
                            .distinct()
                            .filter { it.isNotEmpty() }
                            .sorted()
                    } catch (e: Exception) {
                        println("⚠️ PRODUCTS VIEWMODEL: Failed to update stones from StateFlow: ${e.message}")
                    }
                }
            }
        }
        
        // Still need to load featured products as it doesn't have a listener yet
        loadFeaturedProducts()
    }

    /**
     * Load inventory data for all products asynchronously
     * This prevents UI blocking by loading inventory data in the background
     */
    fun loadInventoryData() {
        val repo = inventoryRepository ?: run {
            println("⚠️ PRODUCTS VIEWMODEL: inventoryRepository is null, cannot load inventory data")
            return
        }

        val allProducts = _products.value
        if (allProducts.isEmpty()) {
            println("⚠️ PRODUCTS VIEWMODEL: Products list is empty, skipping inventory load")
            return
        }

        println("🔄 PRODUCTS VIEWMODEL: Loading inventory data for ${allProducts.size} products")

        viewModelScope.launch {
            try {
                _inventoryLoading.value = true
            } catch (e: Exception) {
                // ViewModel may be disposed, silently return
                println("⚠️ Failed to set inventory loading state: ${e.message}")
                return@launch
            }
            
            try {
                val inventoryDataMap = mutableMapOf<String, InventoryData>()

                // Load inventory data in parallel for better performance
                val deferredResults = allProducts.map { product ->
                    async(Dispatchers.IO) {
                        try {
                            val availableItems = repo.getAvailableInventoryItemsByProductId(product.id)
                            val allItems = repo.getInventoryItemsByProductId(product.id)
                            val barcodeIds = allItems.map { it.barcodeId }

                            product.id to InventoryData(
                                availableCount = availableItems.size,
                                barcodeIds = barcodeIds
                            )
                        } catch (e: Exception) {
                            // Silently fail - will use default InventoryData
                            product.id to InventoryData()
                        }
                    }
                }

                // Wait for all parallel operations to complete
                val results = deferredResults.awaitAll()
                results.forEach { (productId, data) ->
                    inventoryDataMap[productId] = data
                }

                // Update state with null safety check
                try {
                    _inventoryData.value = inventoryDataMap
                    println("✅ PRODUCTS VIEWMODEL: Inventory data loaded successfully - ${inventoryDataMap.size} products")
                    _inventoryLoading.value = false
                } catch (e: Exception) {
                    // ViewModel may be disposed, log and continue
                    println("⚠️ Failed to update inventory data: ${e.message}")
                    // Silently fail if ViewModel is disposed
                }
            } catch (e: Exception) {
                println("❌ PRODUCTS VIEWMODEL: Error loading inventory data: ${e.message}")
                e.printStackTrace()
                // Only update error state if ViewModel is still active
                try {
                    _error.value = "Failed to load inventory data: ${e.message}"
                    _inventoryLoading.value = false
                } catch (ex: Exception) {
                    println("⚠️ Failed to update error state: ${ex.message}")
                    // ViewModel may be disposed, silently ignore
                }
            } finally {
                // Only update loading state if ViewModel is still active
                try {
                    _inventoryLoading.value = false
                } catch (e: Exception) {
                    // ViewModel may be disposed, silently ignore
                    println("⚠️ Failed to update inventory loading state: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        try {
            _error.value = null
        } catch (e: Exception) {
            println("⚠️ Failed to clear error: ${e.message}")
        }
    }

    /**
     * Refresh inventory data for a single product (optimized for single product updates)
     */
    fun refreshInventoryDataForProduct(productId: String) {
        val repo = inventoryRepository ?: return
        
        viewModelScope.launch {
            try {
                val availableItems = repo.getAvailableInventoryItemsByProductId(productId)
                val allItems = repo.getInventoryItemsByProductId(productId)
                val barcodeIds = allItems.map { it.barcodeId }
                
                val newInventoryData = InventoryData(
                    availableCount = availableItems.size,
                    barcodeIds = barcodeIds
                )
                
                // Update state with null safety check
                try {
                    // Update only this product's inventory data in the map
                    try {
                        _inventoryData.value = _inventoryData.value.toMutableMap().apply {
                            put(productId, newInventoryData)
                        }
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _inventoryData: ${e.message}")
                    }
                    
                    // Trigger UI refresh for grouped products
                    try {
                        _inventoryRefreshTrigger.value = _inventoryRefreshTrigger.value + 1
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _inventoryRefreshTrigger: ${e.message}")
                    }
                    println("✅ Inventory data refreshed for product: $productId")
                } catch (e: Exception) {
                    println("⚠️ Failed to update inventory state: ${e.message}")
                }
            } catch (e: Exception) {
                println("⚠️ Failed to refresh inventory data for product $productId: ${e.message}")
            }
        }
    }

    /**
     * Trigger refresh of inventory-based data (like grouped products)
     * This should be called whenever inventory changes occur
     */
    fun triggerInventoryRefresh() {
        try {
            _inventoryRefreshTrigger.value = _inventoryRefreshTrigger.value + 1
            println("🔄 Inventory refresh triggered: ${_inventoryRefreshTrigger.value}")

            // Reload inventory data asynchronously
            loadInventoryData()
        } catch (e: Exception) {
            println("⚠️ Failed to trigger inventory refresh: ${e.message}")
            // Silently fail if ViewModel is disposed
        }
    }

    fun loadProducts() {
        // No longer needed - products are automatically updated via StateFlow listener
        // This method is kept for backward compatibility but does nothing
        println("ℹ️ PRODUCTS VIEWMODEL: loadProducts() called but products are now managed by repository listener")
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
                try {
                    _stonePurities.value = stonesRepository.getAllStonePurities()
                } catch (e: Exception) {
                    println("⚠️ Failed to set _stonePurities: ${e.message}")
                }
                try {
                    _stones.value = stonesRepository.getAllStones()
                } catch (e: Exception) {
                    println("⚠️ Failed to set _stones: ${e.message}")
                }
            } catch (e: Exception) {
                try {
                    _error.value = "Failed to load stones: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
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
                try {
                    _error.value = "Failed to add stone: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
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
                        try {
                            _error.value = null
                        } catch (e: Exception) {
                            println("⚠️ Failed to set _error to null: ${e.message}")
                        }
                    } else {
                        try {
                            _error.value = "Failed to update quantity"
                        } catch (e: Exception) {
                            println("⚠️ Failed to set _error value: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                try {
                    _error.value = "Failed to update quantity: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            }
        }
    }

    private fun loadCategories() {
        // No longer needed - categories are automatically updated via StateFlow listener
        // This method is kept for backward compatibility but does nothing
        println("ℹ️ PRODUCTS VIEWMODEL: loadCategories() called but categories are now managed by repository listener")
    }

    private fun loadMaterials() {
        // No longer needed - materials are automatically updated via StateFlow listener
        // This method is kept for backward compatibility but does nothing
        println("ℹ️ PRODUCTS VIEWMODEL: loadMaterials() called but materials are now managed by repository listener")
    }

    private fun loadFeaturedProducts() {
        viewModelScope.launch {
            try {
                _featuredProducts.value = repository.getFeaturedProducts()
            } catch (e: Exception) {
                try {
                    _error.value = "Failed to load featured products: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
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


    // New: add suggestion helpers used by UI
    fun addCategorySuggestion(name: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val id = repository.addCategory(name)
                loadCategories()
                onComplete(id)
            } catch (e: Exception) {
                try {
                    _error.value = "Failed to add category: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
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
                try {
                    _error.value = "Failed to add category: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
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
                try {
                    _error.value = "Failed to update category: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
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
                try {
                    _error.value = "Failed to delete category: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
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
                try {
                    _error.value = "Failed to add material: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
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
                try {
                    _error.value = "Failed to add material type: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            }
        }
    }

    fun selectProduct(productId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
            } catch (e: Exception) {
                println("⚠️ Failed to set _loading to true: ${e.message}")
                return@launch
            }
            try {
                val product = repository.getProductById(productId)
                try {
                    _currentProduct.value = product
                } catch (e: Exception) {
                    println("⚠️ Failed to set _currentProduct: ${e.message}")
                }
                println("Product loaded for editing: $productId - ${product?.name}")
                try {
                    _error.value = null
                } catch (e: Exception) {
                    println("⚠️ Failed to set _error to null: ${e.message}")
                }
            } catch (e: Exception) {
                try {
                    _error.value = "Failed to get product: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            } finally {
                try {
                    _loading.value = false
                } catch (e: Exception) {
                    println("⚠️ Failed to set _loading to false in finally: ${e.message}")
                }
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
        try {
            _currentProduct.value = Product()
        } catch (e: Exception) {
            println("⚠️ Failed to create new product: ${e.message}")
        }
    }

    fun addProduct(product: Product) {
        println("=== ProductsViewModel.addProduct() called ===")
        println("Product details - Name: '${product.name}', ID: '${product.id}'")
        viewModelScope.launch {
            try {
                _loading.value = true
            } catch (e: Exception) {
                println("⚠️ Failed to set _loading to true: ${e.message}")
                return@launch
            }
            try {
                println("📤 Calling repository.addProduct...")
                val result = repository.addProduct(product)
                println("📥 Repository.addProduct completed")
                loadProducts() // Refresh the list
                loadFeaturedProducts() // Refresh featured products list
                try {
                    _error.value = null
                } catch (e: Exception) {
                    println("⚠️ Failed to set _error to null: ${e.message}")
                }
                println("✅ Product added successfully, loading products...")
            } catch (e: Exception) {
                println("💥 Exception in addProduct: ${e.message}")
                e.printStackTrace()
                try {
                    _error.value = "Failed to add product: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            } finally {
                try {
                    _loading.value = false
                } catch (e: Exception) {
                    println("⚠️ Failed to set _loading to false in finally: ${e.message}")
                }
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
                // Verify the product exists
                val productToDuplicate = repository.getProductById(productId)
                if (productToDuplicate == null) {
                    _error.value = "Product not found for duplication"
                    return@launch
                }

                println("🔄 Duplicating product: ${productToDuplicate.name}")
                println("   - Original Product ID: $productId")
                println("   - New Barcode ID: $barcodeId")

                // Create a new inventory item with the same product ID and new barcode ID
                val inventoryItem = InventoryItem(
                    productId = productId, // Use the same product ID
                    barcodeId = barcodeId // Use the new barcode ID
                )

                val inventoryId = inventoryRepository?.addInventoryItem(inventoryItem)
                if (inventoryId != null && inventoryId.isNotEmpty()) {
                    println("✅ Inventory item created successfully")
                    println("   - Inventory ID: $inventoryId")
                    println("   - Product ID: $productId")
                    println("   - Barcode ID: $barcodeId")

                    // Update product quantity - increment by 1
                    val updatedProduct = productToDuplicate.copy(quantity = productToDuplicate.quantity + 1)
                    
                    // Update local state immediately (same as delete barcode - instant UI update)
                    _products.value = _products.value.map { product ->
                        if (product.id == productId) {
                            updatedProduct
                        } else {
                            product
                        }
                    }
                    
                    // Update inventory data immediately with new barcode (same as delete barcode)
                    val currentInventoryData = try {
                        _inventoryData.value[productId] ?: InventoryData()
                    } catch (e: Exception) {
                        println("⚠️ Failed to get current inventory data: ${e.message}")
                        InventoryData()
                    }
                    
                    try {
                    val newBarcodeIds = currentInventoryData.barcodeIds + barcodeId
                    val newInventoryData = InventoryData(
                        availableCount = currentInventoryData.availableCount + 1,
                        barcodeIds = newBarcodeIds
                    )
                    try {
                        _inventoryData.value = _inventoryData.value.toMutableMap().apply {
                            put(productId, newInventoryData)
                        }
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _inventoryData: ${e.message}")
                    }
                    
                    // Trigger UI refresh for grouped products
                    try {
                        _inventoryRefreshTrigger.value = _inventoryRefreshTrigger.value + 1
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _inventoryRefreshTrigger: ${e.message}")
                    }
                    } catch (e: Exception) {
                        println("⚠️ Failed to update inventory data: ${e.message}")
                    }
                    
                    // Update product in Firestore (same as delete barcode - synchronous)
                    val updateSuccess = repository.updateProduct(updatedProduct)
                    if (updateSuccess) {
                        println("✅ Product quantity updated: ${productToDuplicate.quantity} -> ${updatedProduct.quantity}")
                    } else {
                        println("⚠️ Failed to update product quantity, but inventory item was created")
                        // Revert optimistic update on failure
                        try {
                            try {
                                _products.value = _products.value.map { product ->
                                    if (product.id == productId) {
                                        productToDuplicate
                                    } else {
                                        product
                                    }
                                }
                            } catch (e: Exception) {
                                println("⚠️ Failed to revert _products: ${e.message}")
                            }
                            // Revert inventory data on failure
                            try {
                                _inventoryData.value = _inventoryData.value.toMutableMap().apply {
                                    put(productId, currentInventoryData)
                                }
                            } catch (e: Exception) {
                                println("⚠️ Failed to revert _inventoryData: ${e.message}")
                            }
                        } catch (e: Exception) {
                            println("⚠️ Failed to revert inventory data: ${e.message}")
                        }
                    }

                    try {
                        _error.value = null
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error to null: ${e.message}")
                    }
                    println("✅ Product duplicated successfully - inventory item added and quantity incremented")
                } else {
                    try {
                        _loading.value = false
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _loading to false: ${e.message}")
                    }
                    try {
                        _error.value = "Failed to create inventory item for duplicated product"
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error value: ${e.message}")
                    }
                    println("❌ Failed to create inventory item")
                }
            } catch (e: Exception) {
                println("💥 Exception in duplicateProductWithBarcode: ${e.message}")
                e.printStackTrace()
                try {
                    _error.value = "Failed to duplicate product: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            } finally {
                try {
                    _loading.value = false
                } catch (e: Exception) {
                    println("⚠️ Failed to set _loading to false in finally: ${e.message}")
                }
            }
        }
    }

    fun addProductsBatch(baseProduct: org.example.project.data.Product, barcodes: List<String>, onComplete: (() -> Unit)? = null) {
        println("=== ProductsViewModel.addProductsBatch() called ===")
        println("Base product - Name: '${baseProduct.name}', Barcodes count: ${barcodes.size}")
        println("Barcodes: $barcodes")
        viewModelScope.launch {
            try {
                _loading.value = true
            } catch (e: Exception) {
                println("⚠️ Failed to set _loading to true: ${e.message}")
                return@launch
            }
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
                try {
                    _error.value = null
                } catch (e: Exception) {
                    println("⚠️ Failed to set _error to null: ${e.message}")
                }
                println("✅ Batch products and inventory items added successfully")
                onComplete?.let { it() }
            } catch (e: Exception) {
                println("💥 Exception in addProductsBatch: ${e.message}")
                e.printStackTrace()
                try {
                    _error.value = "Failed to add products: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            } finally {
                try {
                    _loading.value = false
                } catch (e: Exception) {
                    println("⚠️ Failed to set _loading to false in finally: ${e.message}")
                }
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
            try {
                _loading.value = true
            } catch (e: Exception) {
                println("⚠️ Failed to set _loading to true: ${e.message}")
                return@launch
            }
            try {
                val success = repository.updateProduct(product)
                if (success) {
                    loadProducts() // Refresh the list
                    loadFeaturedProducts() // Refresh featured products list
                    try {
                        _error.value = null
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error to null: ${e.message}")
                    }
                } else {
                    try {
                        _error.value = "Failed to update product"
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error value: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                try {
                    _error.value = "Failed to update product: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            } finally {
                try {
                    _loading.value = false
                } catch (e: Exception) {
                    println("⚠️ Failed to set _loading to false in finally: ${e.message}")
                }
            }
        }
    }

    fun deleteProduct(productId: String) {
        println("🗑️ VIEWMODEL: deleteProduct called")
        println("   - Product ID: $productId")
        println("   - Timestamp: ${System.currentTimeMillis()}")

        viewModelScope.launch {
            try {
                _loading.value = true
            } catch (e: Exception) {
                println("⚠️ Failed to set _loading to true: ${e.message}")
                return@launch
            }
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
                    try {
                        _error.value = null
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error to null: ${e.message}")
                    }
                } else {
                    println("❌ Failed to delete product document")
                    try {
                        _error.value = "Failed to delete product"
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error value: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("💥 Exception in deleteProduct: ${e.message}")
                try {
                    _error.value = "Failed to delete product: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            } finally {
                try {
                    _loading.value = false
                } catch (e: Exception) {
                    println("⚠️ Failed to set _loading to false in finally: ${e.message}")
                }
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
            try {
                _loading.value = true
            } catch (e: Exception) {
                println("⚠️ Failed to set _loading to true: ${e.message}")
                return@launch
            }
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
                        try {
                            _error.value = null
                        } catch (e: Exception) {
                            println("⚠️ Failed to set _error to null: ${e.message}")
                        }
                    } else {
                        println("   - ❌ Failed to delete single product")
                        try {
                            _error.value = "Failed to delete product"
                        } catch (e: Exception) {
                            println("⚠️ Failed to set _error value: ${e.message}")
                        }
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
                    try {
                        _error.value = null
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error to null: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("❌ Error in grouped product deletion: ${e.message}")
                try {
                    _error.value = "Failed to delete grouped product: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            } finally {
                try {
                    _loading.value = false
                } catch (e: Exception) {
                    println("⚠️ Failed to set _loading to false in finally: ${e.message}")
                }
            }
        }
    }

    fun deleteProductByBarcodeId(barcodeId: String) {
        println("🗑️ BARCODE DELETE REQUEST START")
        println("   - Barcode ID: $barcodeId")
        println("   - Timestamp: ${System.currentTimeMillis()}")

        viewModelScope.launch {
            try {
                _loading.value = true
            } catch (e: Exception) {
                println("⚠️ Failed to set _loading to true: ${e.message}")
                return@launch
            }
            try {
                println("🔍 Searching for inventory item with barcode: $barcodeId")
                val inventoryItem = inventoryRepository?.getInventoryItemByBarcodeId(barcodeId)

                if (inventoryItem != null) {
                    println("✅ Inventory item found for barcode deletion:")
                    println("   - Inventory ID: ${inventoryItem.id}")
                    println("   - Product ID: ${inventoryItem.productId}")

                    // Get the product to update its quantity
                    val product = repository.getProductById(inventoryItem.productId)

                    // Actually delete the inventory item document
                    val success = inventoryRepository?.deleteInventoryItem(inventoryItem.id) ?: false

                    if (success) {
                        println("✅ Inventory item deleted successfully!")
                        println("   - Inventory ID: ${inventoryItem.id}")
                        println("   - Barcode: $barcodeId")
                        println("   - Document removed from Firestore")

                        // Update product quantity - decrement by 1 (but don't go below 0)
                        if (product != null) {
                            val newQuantity = (product.quantity - 1).coerceAtLeast(0)
                            val updatedProduct = product.copy(quantity = newQuantity)
                            val updateSuccess = repository.updateProduct(updatedProduct)
                            if (updateSuccess) {
                                println("✅ Product quantity updated: ${product.quantity} -> ${updatedProduct.quantity}")
                            } else {
                                println("⚠️ Failed to update product quantity, but inventory item was deleted")
                            }
                        }

                        // Trigger dashboard refresh to show updated quantities
                        triggerInventoryRefresh()
                        loadProducts() // Refresh product list to show updated quantity
                        try {
                            _error.value = null
                        } catch (e: Exception) {
                            println("⚠️ Failed to set _error to null: ${e.message}")
                        }
                    } else {
                        println("❌ Failed to delete inventory item!")
                        try {
                            _error.value = "Failed to delete barcode $barcodeId"
                        } catch (e: Exception) {
                            println("⚠️ Failed to set _error value: ${e.message}")
                        }
                    }
                } else {
                    println("❌ No inventory item found with barcode: $barcodeId")
                    try {
                        _error.value = "No inventory item found with barcode $barcodeId"
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error value: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("💥 Exception during barcode deletion:")
                println("   - Barcode: $barcodeId")
                println("   - Error: ${e.message}")
                try {
                    _error.value = "Failed to delete product with barcode $barcodeId: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            } finally {
                try {
                    _loading.value = false
                } catch (e: Exception) {
                    println("⚠️ Failed to set _loading to false in finally: ${e.message}")
                }
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
            try {
                _loading.value = true
            } catch (e: Exception) {
                println("⚠️ Failed to set _loading to true: ${e.message}")
                return@launch
            }
            try {
                val id = repository.addCompleteCategory(category)
                if (id.isNotEmpty()) {
                    loadCategories() // Refresh the list after adding
                    try {
                        _error.value = null
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error to null: ${e.message}")
                    }
                } else {
                    try {
                        _error.value = "Failed to add category to Firestore"
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error value: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                try {
                    _error.value = "Failed to add category: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            } finally {
                try {
                    _loading.value = false
                } catch (e: Exception) {
                    println("⚠️ Failed to set _loading to false in finally: ${e.message}")
                }
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                _loading.value = true
            } catch (e: Exception) {
                println("⚠️ Failed to set _loading to true: ${e.message}")
                return@launch
            }
            try {
                val success = repository.updateCategory(category)
                if (success) {
                    loadCategories() // Refresh the list after updating
                    try {
                        _error.value = null
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error to null: ${e.message}")
                    }
                } else {
                    try {
                        _error.value = "Failed to update category in Firestore"
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error value: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                try {
                    _error.value = "Failed to update category: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            } finally {
                try {
                    _loading.value = false
                } catch (e: Exception) {
                    println("⚠️ Failed to set _loading to false in finally: ${e.message}")
                }
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
            } catch (e: Exception) {
                println("⚠️ Failed to set _loading to true: ${e.message}")
                return@launch
            }
            try {
                val success = repository.deleteCategory(categoryId)
                if (success) {
                    loadCategories() // Refresh the list after deleting
                    try {
                        _error.value = null
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error to null: ${e.message}")
                    }
                } else {
                    try {
                        _error.value = "Failed to delete category from Firestore"
                    } catch (e: Exception) {
                        println("⚠️ Failed to set _error value: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                try {
                    _error.value = "Failed to delete category: ${e.message}"
                } catch (e2: Exception) {
                    println("⚠️ Failed to set _error value: ${e2.message}")
                }
            } finally {
                try {
                    _loading.value = false
                } catch (e: Exception) {
                    println("⚠️ Failed to set _loading to false in finally: ${e.message}")
                }
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
            if (inventoryRepository == null) {
                println("❌ ERROR: inventoryRepository is null!")
                return null
            }
            val result = inventoryRepository.addInventoryItem(inventoryItem)
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
            if (success) {
                println("✅ PRODUCTS VIEWMODEL: Inventory item updated successfully")
                println("   - Inventory ID: ${inventoryItem.id}")
                println("   - Product ID: ${inventoryItem.productId}")
                println("   - Barcode ID: ${inventoryItem.barcodeId}")
                
                // Trigger inventory refresh to update UI with new barcode
                triggerInventoryRefresh()
                
                inventoryItem.id
            } else {
                println("❌ PRODUCTS VIEWMODEL: Failed to update inventory item")
                null
            }
        } catch (e: Exception) {
            println("❌ PRODUCTS VIEWMODEL: Exception updating inventory item: ${e.message}")
            e.printStackTrace()
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

            // Update product quantity - deduct the number of deleted inventory items
            if (deletedCount > 0) {
                try {
                    val product = repository.getProductById(productId)
                    if (product != null) {
                        val newQuantity = (product.quantity - deletedCount).coerceAtLeast(0)
                        val updatedProduct = product.copy(quantity = newQuantity)
                        val updateSuccess = repository.updateProduct(updatedProduct)
                        if (updateSuccess) {
                            println("✅ Product quantity updated: ${product.quantity} -> ${updatedProduct.quantity} (deducted $deletedCount)")
                        } else {
                            println("⚠️ Failed to update product quantity after deleting inventory items")
                        }
                    }
                } catch (e: Exception) {
                    println("⚠️ Error updating product quantity: ${e.message}")
                }
            }

            // Return true if at least some items were deleted successfully
            deletedCount > 0
        } catch (e: Exception) {
            println("❌ Error deleting inventory items for product $productId: ${e.message}")
            e.printStackTrace()
            false
        }
    }

}