package org.example.project.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.*

class CustomizationViewModel(private val repository: CustomizationRepository) : ViewModel() {
    
    // Published state (what's in Firestore)
    private val _publishedCollections = MutableStateFlow<List<ThemedCollection>>(emptyList())
    val publishedCollections: StateFlow<List<ThemedCollection>> = _publishedCollections.asStateFlow()
    
    private val _publishedCarouselItems = MutableStateFlow<List<CarouselItem>>(emptyList())
    val publishedCarouselItems: StateFlow<List<CarouselItem>> = _publishedCarouselItems.asStateFlow()
    
    private val _publishedAppConfig = MutableStateFlow<AppConfig?>(null)
    val publishedAppConfig: StateFlow<AppConfig?> = _publishedAppConfig.asStateFlow()
    
    // Draft state (what admin is editing)
    private val _draftCollections = MutableStateFlow<List<ThemedCollection>>(emptyList())
    val draftCollections: StateFlow<List<ThemedCollection>> = _draftCollections.asStateFlow()
    
    private val _draftCarouselItems = MutableStateFlow<List<CarouselItem>>(emptyList())
    val draftCarouselItems: StateFlow<List<CarouselItem>> = _draftCarouselItems.asStateFlow()
    
    private val _draftAppConfig = MutableStateFlow<AppConfig?>(null)
    val draftAppConfig: StateFlow<AppConfig?> = _draftAppConfig.asStateFlow()
    
    // UI state
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _currentTab = MutableStateFlow(0) // 0 = Collections, 1 = Carousel
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()
    
    // Selection state
    private val _selectedCollectionId = MutableStateFlow<String?>(null)
    val selectedCollectionId: StateFlow<String?> = _selectedCollectionId.asStateFlow()
    
    private val _selectedCarouselId = MutableStateFlow<String?>(null)
    val selectedCarouselId: StateFlow<String?> = _selectedCarouselId.asStateFlow()
    
    init {
        loadPublishedData()
    }
    
    fun loadPublishedData() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val collections = repository.getAllCollections()
                val carouselItems = repository.getAllCarouselItems()
                val appConfig = repository.getAppConfig()
                
                _publishedCollections.value = collections
                _publishedCarouselItems.value = carouselItems
                _publishedAppConfig.value = appConfig
                
                // Initialize draft state with published data
                _draftCollections.value = collections
                _draftCarouselItems.value = carouselItems
                _draftAppConfig.value = appConfig ?: AppConfig()
                
                _error.value = null
                println("✅ Loaded ${collections.size} collections and ${carouselItems.size} carousel items")
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
                println("❌ Error loading published data: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun setCurrentTab(tab: Int) {
        _currentTab.value = tab
    }
    
    // Selection methods
    fun selectCollection(collectionId: String) {
        _selectedCollectionId.value = collectionId
        _selectedCarouselId.value = null
        _currentTab.value = 0 // Switch to Collections tab
        println("📌 Selected collection: $collectionId")
    }
    
    fun selectCarousel(carouselId: String) {
        _selectedCarouselId.value = carouselId
        _selectedCollectionId.value = null
        _currentTab.value = 1 // Switch to Carousel tab
        println("📌 Selected carousel: $carouselId")
    }
    
    fun clearSelection() {
        _selectedCollectionId.value = null
        _selectedCarouselId.value = null
        println("📌 Selection cleared")
    }
    
    // Collections management
    fun updateDraftCollection(collection: ThemedCollection) {
        val updated = _draftCollections.value.toMutableList()
        val index = updated.indexOfFirst { it.id == collection.id }
        if (index >= 0) {
            updated[index] = collection
            _draftCollections.value = updated
        }
    }
    
    fun toggleCollectionActive(collectionId: String) {
        val updated = _draftCollections.value.toMutableList()
        val index = updated.indexOfFirst { it.id == collectionId }
        if (index >= 0) {
            updated[index] = updated[index].copy(isActive = !updated[index].isActive)
            _draftCollections.value = updated
        }
    }
    
    fun updateCollectionImage(collectionId: String, imageIndex: Int, image: CollectionImage) {
        val updated = _draftCollections.value.toMutableList()
        val collectionIndex = updated.indexOfFirst { it.id == collectionId }
        if (collectionIndex >= 0) {
            val collection = updated[collectionIndex]
            val updatedImages = collection.images.toMutableList()
            if (imageIndex < updatedImages.size) {
                updatedImages[imageIndex] = image
                updated[collectionIndex] = collection.copy(images = updatedImages)
                _draftCollections.value = updated
            }
        }
    }
    
    // Carousel management
    fun updateDraftCarouselItem(item: CarouselItem) {
        val updated = _draftCarouselItems.value.toMutableList()
        val index = updated.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            updated[index] = item
            _draftCarouselItems.value = updated
        }
    }
    
    fun toggleCarouselItemActive(itemId: String) {
        val updated = _draftCarouselItems.value.toMutableList()
        val index = updated.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            updated[index] = updated[index].copy(isActive = !updated[index].isActive)
            _draftCarouselItems.value = updated
        }
    }
    
    fun toggleCarouselItemTitleActive(itemId: String) {
        val updated = _draftCarouselItems.value.toMutableList()
        val index = updated.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            updated[index] = updated[index].copy(titleActive = !updated[index].titleActive)
            _draftCarouselItems.value = updated
        }
    }
    
    fun toggleCarouselItemSubtitleActive(itemId: String) {
        val updated = _draftCarouselItems.value.toMutableList()
        val index = updated.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            updated[index] = updated[index].copy(subtitleActive = !updated[index].subtitleActive)
            _draftCarouselItems.value = updated
        }
    }
    
    // App config management
    fun updateDraftAppConfig(config: AppConfig) {
        _draftAppConfig.value = config
    }
    
    fun toggleCollectionsEnabled() {
        val current = _draftAppConfig.value ?: AppConfig()
        _draftAppConfig.value = current.copy(collectionsEnabled = !current.collectionsEnabled)
    }
    
    fun toggleCarouselsEnabled() {
        val current = _draftAppConfig.value ?: AppConfig()
        _draftAppConfig.value = current.copy(carouselsEnabled = !current.carouselsEnabled)
    }
    
    // Save operations
    fun saveDraft() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Save collections
                for (collection in _draftCollections.value) {
                    if (collection.id.isNotEmpty()) {
                        repository.updateCollection(collection)
                    } else {
                        repository.saveCollection(collection)
                    }
                }
                
                // Save carousel items
                for (item in _draftCarouselItems.value) {
                    if (item.id.isNotEmpty()) {
                        repository.updateCarouselItem(item)
                    } else {
                        repository.saveCarouselItem(item)
                    }
                }
                
                // Save app config
                _draftAppConfig.value?.let { config ->
                    repository.updateAppConfig(config)
                }
                
                // Reload published data
                loadPublishedData()
                
                _error.value = null
                println("✅ Draft saved successfully")
            } catch (e: Exception) {
                _error.value = "Failed to save draft: ${e.message}"
                println("❌ Error saving draft: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun publishChanges() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Save all changes
                saveDraft()
                
                // Update app config to publish changes
                val config = _draftAppConfig.value ?: AppConfig()
                val publishedConfig = config.copy(lastPublished = System.currentTimeMillis())
                repository.updateAppConfig(publishedConfig)
                
                // Reload published data
                loadPublishedData()
                
                _error.value = null
                println("✅ Changes published successfully")
            } catch (e: Exception) {
                _error.value = "Failed to publish changes: ${e.message}"
                println("❌ Error publishing changes: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun discardChanges() {
        // Reset draft state to published state
        _draftCollections.value = _publishedCollections.value
        _draftCarouselItems.value = _publishedCarouselItems.value
        _draftAppConfig.value = _publishedAppConfig.value ?: AppConfig()
        _error.value = null
        println("🔄 Changes discarded, reverted to published state")
    }
    
    fun clearError() {
        _error.value = null
    }
    
    // Add new collection
    fun addNewCollection(name: String, description: String, imageUrls: List<String> = emptyList()) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val order = _draftCollections.value.size
                val images = imageUrls.mapIndexed { index, url ->
                    CollectionImage(url = url, isActive = true, order = index)
                }
                
                val newCollection = ThemedCollection(
                    name = name,
                    description = description,
                    isActive = true,
                    order = order,
                    images = images
                )
                
                val newId = repository.saveCollection(newCollection)
                
                // Update draft collections
                _draftCollections.value = _draftCollections.value + newCollection.copy(id = newId)
                
                // Add to active collection IDs
                val config = _draftAppConfig.value ?: AppConfig()
                _draftAppConfig.value = config.copy(
                    activeCollectionIds = config.activeCollectionIds + newId
                )
                
                // Reload published data
                loadPublishedData()
                
                _error.value = null
                println("✅ New collection added: $name")
            } catch (e: Exception) {
                _error.value = "Failed to add collection: ${e.message}"
                println("❌ Error adding collection: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
    
    // Add new carousel item
    fun addNewCarouselItem(title: String, subtitle: String, imageUrl: String = "") {
        viewModelScope.launch {
            _loading.value = true
            try {
                val order = _draftCarouselItems.value.size
                val newItem = CarouselItem(
                    title = title,
                    titleActive = true,
                    subtitle = subtitle,
                    subtitleActive = true,
                    image = CarouselImage(url = imageUrl, isActive = true),
                    isActive = true,
                    order = order
                )
                
                val newId = repository.saveCarouselItem(newItem)
                
                // Update draft carousel items
                _draftCarouselItems.value = _draftCarouselItems.value + newItem.copy(id = newId)
                
                // Add to active carousel IDs
                val config = _draftAppConfig.value ?: AppConfig()
                _draftAppConfig.value = config.copy(
                    activeCarouselIds = config.activeCarouselIds + newId
                )
                
                // Reload published data
                loadPublishedData()
                
                _error.value = null
                println("✅ New carousel item added: $title")
            } catch (e: Exception) {
                _error.value = "Failed to add carousel item: ${e.message}"
                println("❌ Error adding carousel item: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
    
    // Helper methods for preview
    fun getActiveCollectionsForPreview(): List<ThemedCollection> {
        val config = _draftAppConfig.value ?: AppConfig()
        return if (config.collectionsEnabled) {
            _draftCollections.value
                .filter { it.isActive && it.id in config.activeCollectionIds }
                .sortedBy { it.order }
        } else emptyList()
    }
    
    fun getActiveCarouselItemsForPreview(): List<CarouselItem> {
        val config = _draftAppConfig.value ?: AppConfig()
        return if (config.carouselsEnabled) {
            _draftCarouselItems.value
                .filter { it.isActive && it.id in config.activeCarouselIds }
                .sortedBy { it.order }
        } else emptyList()
    }
    
    // Load dummy data for testing
    fun loadDummyData() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Create dummy collections
                val dummyCollections = listOf(
                    ThemedCollection(
                        id = "dummy_col_1",
                        name = "Wedding Collection",
                        description = "Beautiful wedding jewelry pieces",
                        isActive = true,
                        order = 0,
                        images = listOf(
                            CollectionImage(url = "wedding_1.jpg", isActive = true, order = 0),
                            CollectionImage(url = "wedding_2.jpg", isActive = true, order = 1),
                            CollectionImage(url = "wedding_3.jpg", isActive = true, order = 2)
                        )
                    ),
                    ThemedCollection(
                        id = "dummy_col_2",
                        name = "Traditional Collection",
                        description = "Classic traditional designs",
                        isActive = true,
                        order = 1,
                        images = listOf(
                            CollectionImage(url = "traditional_1.jpg", isActive = true, order = 0),
                            CollectionImage(url = "traditional_2.jpg", isActive = true, order = 1)
                        )
                    ),
                    ThemedCollection(
                        id = "dummy_col_3",
                        name = "Modern Collection",
                        description = "Contemporary jewelry styles",
                        isActive = false,
                        order = 2,
                        images = listOf(
                            CollectionImage(url = "modern_1.jpg", isActive = true, order = 0)
                        )
                    )
                )
                
                // Create dummy carousel items
                val dummyCarouselItems = listOf(
                    CarouselItem(
                        id = "dummy_car_1",
                        title = "New Arrivals",
                        titleActive = true,
                        subtitle = "Check out our latest collection",
                        subtitleActive = true,
                        image = CarouselImage(url = "carousel_1.jpg", isActive = true),
                        isActive = true,
                        order = 0
                    ),
                    CarouselItem(
                        id = "dummy_car_2",
                        title = "Special Offer",
                        titleActive = true,
                        subtitle = "Up to 30% off on selected items",
                        subtitleActive = true,
                        image = CarouselImage(url = "carousel_2.jpg", isActive = true),
                        isActive = true,
                        order = 1
                    ),
                    CarouselItem(
                        id = "dummy_car_3",
                        title = "Premium Collection",
                        titleActive = false,
                        subtitle = "Exclusive designs",
                        subtitleActive = true,
                        image = CarouselImage(url = "carousel_3.jpg", isActive = true),
                        isActive = true,
                        order = 2
                    )
                )
                
                // Update draft state
                _draftCollections.value = dummyCollections
                _draftCarouselItems.value = dummyCarouselItems
                
                // Update app config
                val config = AppConfig(
                    activeCollectionIds = listOf("dummy_col_1", "dummy_col_2", "dummy_col_3"),
                    activeCarouselIds = listOf("dummy_car_1", "dummy_car_2", "dummy_car_3"),
                    collectionsEnabled = true,
                    carouselsEnabled = true
                )
                _draftAppConfig.value = config
                
                _error.value = null
                println("✅ Dummy data loaded successfully")
            } catch (e: Exception) {
                _error.value = "Failed to load dummy data: ${e.message}"
                println("❌ Error loading dummy data: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
}
