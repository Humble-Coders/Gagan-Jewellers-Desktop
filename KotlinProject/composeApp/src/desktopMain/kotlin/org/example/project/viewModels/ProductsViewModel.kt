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

class ProductsViewModel(private val repository: ProductRepository) {

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

    init {
        loadProducts()
        loadCategories()
        loadMaterials()
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
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.addProduct(product)
                loadProducts() // Refresh the list
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to add product: ${e.message}"
            } finally {
                _loading.value = false
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

    // Clean up resources
    fun onCleared() {
        viewModelScope.cancel()
    }
}