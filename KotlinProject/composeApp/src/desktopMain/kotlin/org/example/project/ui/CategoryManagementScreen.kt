package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.data.Category
import org.example.project.viewModels.ProductsViewModel
import org.example.project.JewelryAppInitializer
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import javax.imageio.ImageIO

// Data classes for Unsplash API response
data class UnsplashImageData(
    val urls: UnsplashUrls?
)

data class UnsplashUrls(
    val small: String?,
    val regular: String?,
    val thumb: String?
)

// Helper function to get suggested images from Unsplash API
suspend fun getSuggestedImages(categoryName: String): List<String> = withContext(Dispatchers.IO) {
    try {
        // Clean and prepare search query
        val searchQuery = prepareSearchQuery(categoryName)
        
        // Unsplash API endpoint (using demo access key - in production, use your own key)
        val apiKey = "g48LebMsx_K5EW7JWeUe-KiSqWKqsSBOj0vhwqoOLWk" // Replace with your Unsplash access key
        val encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.toString())
        val url = "https://api.unsplash.com/search/photos?query=$encodedQuery&per_page=6&orientation=squarish"
        
        println("🔍 Searching for: '$searchQuery' -> '$encodedQuery'")
        println("🌐 API URL: $url")
        
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Client-ID $apiKey")
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        println("📡 Response Code: ${connection.responseCode}")
        
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = parseUnsplashResponse(response)
            val imageUrls = jsonResponse.map { imageData ->
                // Get small/medium size image URL
                imageData.urls?.small ?: imageData.urls?.regular ?: imageData.urls?.thumb
            }.filterNotNull()
            println("✅ Found ${imageUrls.size} images from Unsplash")
            imageUrls
        } else {
            println("Unsplash API error: ${connection.responseCode}")
            getFallbackImages(categoryName)
        }
    } catch (e: Exception) {
        println("Error fetching images from Unsplash: ${e.message}")
        getFallbackImages(categoryName)
    }
}

// Prepare search query for better results
fun prepareSearchQuery(categoryName: String): String {
    val cleanName = categoryName.lowercase().trim()
    
    // Map common jewelry terms to better search queries
    val searchMappings = mapOf(
        "ring" to "jewelry ring",
        "rings" to "jewelry rings",
        "necklace" to "jewelry necklace",
        "necklaces" to "jewelry necklaces", 
        "earring" to "jewelry earrings",
        "earrings" to "jewelry earrings",
        "bracelet" to "jewelry bracelet",
        "bracelets" to "jewelry bracelets",
        "chain" to "jewelry chain",
        "chains" to "jewelry chains",
        "pendant" to "jewelry pendant",
        "pendants" to "jewelry pendants",
        "watch" to "luxury watch",
        "watches" to "luxury watches",
        "brooch" to "jewelry brooch",
        "brooches" to "jewelry brooches"
    )
    
    return searchMappings[cleanName] ?: "jewelry $cleanName"
}

// Parse Unsplash API response
fun parseUnsplashResponse(json: String): List<UnsplashImageData> {
    try {
        // Simple JSON parsing (in production, use a proper JSON library like kotlinx.serialization)
        val imageUrls = mutableListOf<String>()
        
        // Extract URLs from JSON response using regex (simple approach)
        val urlPattern = "\"small\":\\s*\"([^\"]+)\"".toRegex()
        val matches = urlPattern.findAll(json)
        
        matches.forEach { matchResult ->
            val url = matchResult.groupValues[1]
            if (url.isNotEmpty()) {
                imageUrls.add(url)
            }
        }
        
        // Return as UnsplashImageData objects
        return imageUrls.take(6).map { url ->
            UnsplashImageData(
                urls = UnsplashUrls(
                    small = url,
                    regular = url.replace("&w=400", "&w=800"),
                    thumb = url.replace("&w=400", "&w=200")
                )
            )
        }
    } catch (e: Exception) {
        println("Error parsing Unsplash response: ${e.message}")
        return emptyList()
    }
}

// Fallback images when API fails
fun getFallbackImages(categoryName: String): List<String> {
    return when (categoryName.lowercase()) {
        "rings", "ring" -> listOf(
            "https://images.unsplash.com/photo-1605100804763-247f67b3557e?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1603561596112-db0b0b0b0b0b?w=400&h=400&fit=crop"
        )
        "necklaces", "necklace" -> listOf(
            "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1605100804763-247f67b3557e?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1603561596112-db0b0b0b0b0b?w=400&h=400&fit=crop"
        )
        "earrings", "earring" -> listOf(
            "https://images.unsplash.com/photo-1603561596112-db0b0b0b0b0b?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1605100804763-247f67b3557e?w=400&h=400&fit=crop"
        )
        else -> listOf(
            "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1605100804763-247f67b3557e?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1603561596112-db0b0b0b0b0b?w=400&h=400&fit=crop"
        )
    }
}

@Composable
fun AddCategoryDialog(
    initialCategory: Category? = null,
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var description by remember { mutableStateOf(initialCategory?.description ?: "") }
    var selectedImageUrl by remember { mutableStateOf(initialCategory?.imageUrl ?: "") }
    var selectedImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var suggestedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val storageService = JewelryAppInitializer.getStorageService()

    // Load existing image if editing
    LaunchedEffect(initialCategory?.imageUrl) {
        if (initialCategory?.imageUrl?.isNotEmpty() == true) {
            try {
                selectedImageBitmap = withContext(Dispatchers.IO) {
                    ImageIO.read(URL(initialCategory.imageUrl))?.toComposeImageBitmap()
                }
            } catch (e: Exception) {
                println("Error loading existing image: ${e.message}")
            }
        }
    }

    // Load suggested images when name changes
    LaunchedEffect(name) {
        if (name.isNotEmpty() && name.length > 2) {
            isLoadingSuggestions = true
            coroutineScope.launch {
                try {
                    suggestedImages = getSuggestedImages(name)
                } catch (e: Exception) {
                    println("Error loading suggested images: ${e.message}")
                    suggestedImages = emptyList()
                }
                isLoadingSuggestions = false
            }
        } else {
            suggestedImages = emptyList()
        }
    }

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    if (initialCategory != null) "Edit Category" else "Add New Category",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E2E2E)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    // Image Selection Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Category Image",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E2E2E)
                        )

                        // Image Preview
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0E0E0))
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageBitmap != null) {
                                Image(
                                    bitmap = selectedImageBitmap!!,
                                    contentDescription = "Selected Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "No Image Selected",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        // Select Image Button
                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val fileDialog = FileDialog(Frame()).apply {
                                        mode = FileDialog.LOAD
                                        title = "Select Image"
                                        file = "*.jpg;*.jpeg;*.png;*.gif"
                                        isVisible = true
                                    }

                                    val selectedFile = if (fileDialog.directory != null && fileDialog.file != null) {
                                        File(fileDialog.directory, fileDialog.file).takeIf { it.exists() }
                                    } else {
                                        null
                                    }

                                    if (selectedFile != null) {
                                        withContext(Dispatchers.Main) {
                                            isUploading = true
                                            uploadProgress = 0f
                                            errorMessage = null
                                        }

                                        val directoryPath = "categories/${System.currentTimeMillis()}"
                                        val progressFlow = MutableStateFlow(0f)
                                        val imageUrl = storageService.uploadFile(
                                            Paths.get(selectedFile.absolutePath),
                                            directoryPath,
                                            progressFlow
                                        )

                                        withContext(Dispatchers.Main) {
                                            isUploading = false
                                            if (imageUrl != null) {
                                                selectedImageUrl = imageUrl
                                                selectedImageBitmap = withContext(Dispatchers.IO) {
                                                    ImageIO.read(URL(imageUrl))?.toComposeImageBitmap()
                                                }
                                            } else {
                                                errorMessage = "Failed to upload image."
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isUploading
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Select Image")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Image")
                        }

                        if (isUploading) {
                            LinearProgressIndicator(
                                progress = uploadProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Uploading... ${(uploadProgress * 100).toInt()}%")
                        }
                        errorMessage?.let {
                            Text(it, color = Color.Red)
                        }
                    }

                    // Suggested Images Section
                    if (name.isNotEmpty() && name.length > 2) {
                        Text(
                            "Suggested Images",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E2E2E)
                        )
                        
                        if (isLoadingSuggestions) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else if (suggestedImages.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(suggestedImages) { imageUrl ->
                                    SuggestedImageItem(
                                        imageUrl = imageUrl,
                                        onSelect = { url ->
                                            selectedImageUrl = url
                                            coroutineScope.launch {
                                                try {
                                                    val bitmap = withContext(Dispatchers.IO) {
                                                        val url = URL(imageUrl)
                                                        val bufferedImage = ImageIO.read(url)
                                                        bufferedImage?.toComposeImageBitmap()
                                                    }
                                                    selectedImageBitmap = bitmap
                                                } catch (e: Exception) {
                                                    println("Error loading suggested image: ${e.message}")
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            Text(
                                "No suggested images found",
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(name, description, selectedImageUrl)
                        },
                        enabled = name.isNotEmpty()
                    ) {
                        Text(if (initialCategory != null) "Update Category" else "Add Category")
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedImageItem(
    imageUrl: String,
    onSelect: (String) -> Unit
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(imageUrl) {
        try {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val url = URL(imageUrl)
                    val bufferedImage = ImageIO.read(url)
                    bufferedImage?.toComposeImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            imageBitmap = bitmap
        } catch (e: Exception) {
            println("Error loading suggested image: ${e.message}")
        }
        isLoading = false
    }

    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable { onSelect(imageUrl) },
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                }
                imageBitmap != null -> {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = "Suggested Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "No Image",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryManagementScreen(
    productsViewModel: ProductsViewModel,
    onBack: () -> Unit
) {
    val categories by productsViewModel.categories
    val loading by productsViewModel.loading
    val error by productsViewModel.error
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Header
        CategoryScreenHeader(
            onBack = onBack,
            onAddCategory = { showAddCategoryDialog = true }
        )

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Categories Grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        "Product Categories",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E2E2E)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        loading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colors.primary
                                )
                            }
                        }
                        error != null -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = Color.Red,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        error ?: "Unknown error",
                                        color = Color.Red,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        categories.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Category,
                                        contentDescription = "No Categories",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                            Text(
                                        "No categories found",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                                }
                            }
                        }
                        else -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(200.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.height(600.dp)
                            ) {
                                items(categories.filter { it.isActive }) { category ->
                                    CategoryCard(
                                    category = category,
                                        onEdit = { editingCategory = it },
                                        onDelete = { deletingCategory = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onSave = { name, description, imageUrl ->
                // Create category with all required fields
                val newCategory = Category(
                    name = name,
                    description = description,
                    imageUrl = imageUrl,
                    categoryType = "JEWELRY", // Default category type
                    hasGenderVariants = false, // Default value
                    order = categories.size, // Set order to next available
                    isActive = true, // Default to active
                    createdAt = System.currentTimeMillis()
                )
                // Add complete category through viewModel
                productsViewModel.addCompleteCategory(newCategory) { categoryId ->
                    println("✅ Category created successfully with ID: $categoryId")
                    // Category added successfully, refresh the list
                    // Note: loadCategories() is private, the viewModel will refresh automatically
                }
                showAddCategoryDialog = false
            },
            onCancel = { showAddCategoryDialog = false }
        )
    }

        // Edit Category Dialog
        editingCategory?.let { category ->
            AddCategoryDialog(
                initialCategory = category,
                onSave = { name, description, imageUrl ->
                    // Update the existing category
                    val updatedCategory = category.copy(
                        name = name,
                        description = description,
                        imageUrl = imageUrl
                    )
                    // Call the functional update method
                    productsViewModel.updateCategory(updatedCategory) { success ->
                        if (success) {
                            println("✅ Category updated successfully: ${updatedCategory.name}")
                        } else {
                            println("❌ Failed to update category: ${updatedCategory.name}")
                        }
                    }
                    editingCategory = null
                },
                onCancel = { editingCategory = null }
            )
        }

        // Delete Category Warning Dialog
        deletingCategory?.let { category ->
            AlertDialog(
                onDismissRequest = { deletingCategory = null },
                title = {
                    Text(
                        "Delete Category",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                },
                text = {
                    Column {
                        Text(
                            "Are you sure you want to delete \"${category.name}\"?",
                            fontSize = 16.sp,
                            color = Color(0xFF2E2E2E)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "⚠️ Warning: This category might have products associated with it. Deleting this category will not delete the products, but they will lose their category association.",
                            fontSize = 14.sp,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            productsViewModel.deleteCategory(category.id) { success ->
                                if (success) {
                                    println("✅ Category deleted successfully: ${category.name}")
                                } else {
                                    println("❌ Failed to delete category: ${category.name}")
                                }
                            }
                            deletingCategory = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { deletingCategory = null }
                    ) {
                        Text("Cancel")
                    }
                }
        )
    }
}

@Composable
fun CategoryScreenHeader(
    onBack: () -> Unit,
    onAddCategory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                tint = Color(0xFF2E2E2E)
                )
            }

            Text(
            "Categories",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            Button(
                onClick = onAddCategory,
                colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.White,
                contentColor = Color(0xFF2E2E2E)
            ),
            elevation = ButtonDefaults.elevation(defaultElevation = 2.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Category")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Category")
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Load image asynchronously
    LaunchedEffect(category.imageUrl) {
        if (category.imageUrl.isNotEmpty()) {
            try {
                imageBitmap = withContext(Dispatchers.IO) {
                    ImageIO.read(URL(category.imageUrl))?.toComposeImageBitmap()
                }
                hasError = false
            } catch (e: Exception) {
                println("Error loading category image: ${e.message}")
                hasError = true
            }
        } else {
            hasError = true
        }
        isLoading = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Action buttons in top-right corner
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .zIndex(10f) // Ensure buttons are on top
            ) {
                // Delete button - red theme for danger
                IconButton(
                    onClick = { 
                        println("Delete button clicked for category: ${category.name}")
                        onDelete(category) 
                    },
                    modifier = Modifier
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFFB71C1C).copy(alpha = 0.3f),
                            spotColor = Color(0xFFB71C1C).copy(alpha = 0.2f)
                        )
                        .background(
                            Color(0xFFD32F2F), // Red color for delete action
                            CircleShape
                        )
                        .size(32.dp)
                        .padding(end = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Category",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
                
                // Edit button - golden theme
                IconButton(
                    onClick = { 
                        println("Edit button clicked for category: ${category.name}")
                        onEdit(category) 
                    },
                    modifier = Modifier
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFFB8860B).copy(alpha = 0.3f),
                            spotColor = Color(0xFFB8860B).copy(alpha = 0.2f)
                        )
                        .background(
                            Color(0xFFDAA520), // Golden color matching app theme
                            CircleShape
                        )
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Category",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }
            
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Image section - larger to focus on image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF5F5F5))
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colors.primary
                                )
                            }
                        }
                        hasError || imageBitmap == null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "No Image",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        else -> {
                            imageBitmap?.let { bitmap ->
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap,
                                    contentDescription = "Category Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // Category name section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E2E2E),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}