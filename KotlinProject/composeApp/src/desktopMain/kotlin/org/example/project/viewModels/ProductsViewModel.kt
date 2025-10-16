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
import org.example.project.data.ProductRepository
import org.example.project.data.StonesRepository

class ProductsViewModel(internal val repository: ProductRepository, private val stonesRepository: StonesRepository? = null) {

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

    fun clearError() {
        _error.value = null
    }

    fun loadProducts() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _products.value = repository.getAllProducts()
                _error.value = null
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

    private val _stoneColors = mutableStateOf<List<String>>(emptyList())
    val stoneColors: State<List<String>> = _stoneColors

    fun loadStoneSuggestions() {
        if (stonesRepository == null) return
        viewModelScope.launch {
            try {
                _stoneNames.value = stonesRepository.getAllStoneNames()
                _stoneColors.value = stonesRepository.getAllStoneColors()
            } catch (e: Exception) {
                _error.value = "Failed to load stones: ${e.message}"
            }
        }
    }

    fun addStoneSuggestion(name: String, color: String, onComplete: (String) -> Unit) {
        if (stonesRepository == null) return
        viewModelScope.launch {
            try {
                val id = stonesRepository.addStone(name, color)
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

    fun clearCurrentProduct() {
        _currentProduct.value = null
    }

    fun createNewProduct() {
        _currentProduct.value = Product()
    }

    fun addProduct(product: Product) {
        println("=== ProductsViewModel.addProduct() called ===")
        println("Product details - Name: '${product.name}', ID: '${product.id}', BarcodeIds: ${product.barcodeIds}")
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
        return repository.generateUniqueBarcodes(quantity, digits)
    }

    fun addProductsBatch(baseProduct: org.example.project.data.Product, barcodes: List<String>, onComplete: (() -> Unit)? = null) {
        println("=== ProductsViewModel.addProductsBatch() called ===")
        println("Base product - Name: '${baseProduct.name}', Barcodes count: ${barcodes.size}")
        println("Barcodes: $barcodes")
        viewModelScope.launch {
            _loading.value = true
            try {
                println("🔄 Starting batch processing...")
                for (code in barcodes) {
                    val productForDoc = baseProduct.copy(
                        id = "", // repository will assign auto document id
                        quantity = 1,
                        barcodeIds = listOf(code)
                    )
                    println("📝 Creating product for barcode: $code")
                    repository.addProduct(productForDoc)
                    println("✅ Added product with barcode: $code")
                }
                loadProducts()
                loadFeaturedProducts() // Refresh featured products list
                _error.value = null
                println("✅ Batch products added successfully")
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

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val success = repository.deleteProduct(productId)
                if (success) {
                    loadProducts() // Refresh the list
                    _error.value = null
                } else {
                    _error.value = "Failed to delete product"
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete product: ${e.message}"
            } finally {
                _loading.value = false
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
    fun getCategoriesByType(categoryType: org.example.project.data.CategoryType): List<Category> {
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
}