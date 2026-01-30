package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.JewelryAppInitializer
import org.example.project.data.ThemedCollection
import org.example.project.data.FirestoreThemedCollectionRepository
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URL
import java.nio.file.Paths

@Composable
fun CollectionsSection(
    collections: List<ThemedCollection>,
    products: List<org.example.project.data.Product>,
    collectionRepository: FirestoreThemedCollectionRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onCollectionsChanged: (List<ThemedCollection>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCollection by remember { mutableStateOf<ThemedCollection?>(null) }
    var deletingCollection by remember { mutableStateOf<ThemedCollection?>(null) }
    
    val productCountByCollection = remember(collections, products) {
        collections.associate { collection ->
            collection.id to collection.productIds.size
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add Collection Button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFD4AF37),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Collection")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Collection", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        if (collections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Collections,
                        contentDescription = null,
                        tint = Color(0xFFBBBBBB),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "No collections yet",
                        color = Color(0xFF999999),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Collections Grid - 2 per row
            val activeCollections = collections.filter { it.isActive }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeCollections.chunked(2).forEach { rowCollections ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCollections.forEach { collection ->
                            CollectionItem(
                                collection = collection,
                                productCount = productCountByCollection[collection.id] ?: 0,
                                onEdit = { editingCollection = it },
                                onDelete = { deletingCollection = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Add spacer if odd number of collections in last row
                        if (rowCollections.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCollectionDialog(
            onSave = { name, description, imageUrl ->
                scope.launch {
                    val newCollection = ThemedCollection(
                        name = name,
                        description = description,
                        imageUrl = imageUrl,
                        isActive = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        productIds = emptyList()
                    )
                    collectionRepository.addCollection(newCollection)
                    onCollectionsChanged(collectionRepository.getAllCollections())
                }
                showAddDialog = false
            },
            onCancel = { showAddDialog = false }
        )
    }

    // Edit Collection Dialog
    editingCollection?.let { collection ->
        AddCollectionDialog(
            initialCollection = collection,
            onSave = { name, description, imageUrl ->
                scope.launch {
                    val updatedCollection = collection.copy(
                        name = name,
                        description = description,
                        imageUrl = imageUrl,
                        updatedAt = System.currentTimeMillis()
                    )
                    collectionRepository.updateCollection(updatedCollection)
                    onCollectionsChanged(collectionRepository.getAllCollections())
                }
                editingCollection = null
            },
            onCancel = { editingCollection = null }
        )
    }

    // Delete Confirmation Dialog
    deletingCollection?.let { collection ->
        AlertDialog(
            onDismissRequest = { deletingCollection = null },
            title = { Text("Delete Collection", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete \"${collection.name}\"?")
                    val productCount = productCountByCollection[collection.id] ?: 0
                    if (productCount > 0) {
                        Text(
                            "⚠️ This collection has $productCount product(s). Products will not be deleted but will be removed from this collection.",
                            color = Color(0xFFFF6B00),
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            collectionRepository.deleteCollection(collection.id)
                            onCollectionsChanged(collectionRepository.getAllCollections())
                        }
                        deletingCollection = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Red,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCollection = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CollectionItem(
    collection: ThemedCollection,
    productCount: Int,
    onEdit: (ThemedCollection) -> Unit,
    onDelete: (ThemedCollection) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val storageService = remember { JewelryAppInitializer.getStorageService() }
    
    // Collect all image URLs from both imageUrl and images list
    val allImageUrls = remember(collection) {
        buildList {
            // Add main imageUrl if not empty
            if (collection.imageUrl.isNotEmpty()) {
                add(collection.imageUrl)
            }
            // Add all active images from images list
            collection.images.filter { it.isActive }
                .sortedBy { it.order }
                .forEach { add(it.url) }
        }.distinct().also { urls ->
            println("📋 Collection '${collection.name}' has ${urls.size} image URLs:")
            urls.forEachIndexed { index, url -> 
                println("   [$index] $url")
            }
            if (urls.isEmpty()) {
                println("⚠️ No images found for collection '${collection.name}'")
                println("   - imageUrl: '${collection.imageUrl}'")
                println("   - images list size: ${collection.images.size}")
                collection.images.forEachIndexed { i, img ->
                    println("   - images[$i]: url='${img.url}', isActive=${img.isActive}, order=${img.order}")
                }
            }
        } // Remove duplicates
    }
    
    // State for carousel
    var currentImageIndex by remember { mutableStateOf(0) }
    var loadedImages by remember { mutableStateOf<List<ImageBitmap?>>(emptyList()) }
    
    // Load all images
    LaunchedEffect(allImageUrls) {
        if (allImageUrls.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                println("🎨 Starting to load ${allImageUrls.size} images for collection '${collection.name}'")
                val bitmaps = allImageUrls.mapIndexed { index, imageUrl ->
                    try {
                        println("🖼️ [$index/${allImageUrls.size}] Loading: $imageUrl")
                        
                        val imageBytes = when {
                            // If it's an HTTPS URL, load directly
                            imageUrl.startsWith("https://") || imageUrl.startsWith("http://") -> {
                                try {
                                    println("   🌐 Loading via HTTP...")
                                    val bytes = URL(imageUrl).openStream().readBytes()
                                    println("   ✅ HTTP load successful: ${bytes.size} bytes")
                                    bytes
                                } catch (e: Exception) {
                                    println("   ⚠️ HTTP load failed: ${e.message}")
                                    e.printStackTrace()
                                    null
                                }
                            }
                            // If it's a gs:// URL, extract path and use storage service
                            imageUrl.startsWith("gs://") -> {
                                val path = imageUrl.substringAfter("gs://").substringAfter("/")
                                println("   📁 Extracted path from gs:// URL: $path")
                                storageService.downloadFileBytes(path)
                            }
                            // Otherwise, assume it's a storage path
                            else -> {
                                println("   📁 Using as storage path: $imageUrl")
                                storageService.downloadFileBytes(imageUrl)
                            }
                        }
                        
                        if (imageBytes != null && imageBytes.isNotEmpty()) {
                            try {
                                val bitmap = decodeAndResizeCategoryImage(imageBytes, maxSize = 100)
                                if (bitmap != null) {
                                    println("   ✅ Image decoded successfully: ${bitmap.width}x${bitmap.height}")
                                } else {
                                    println("   ❌ Failed to decode image bytes")
                                }
                                bitmap
                            } catch (e: Exception) {
                                println("   ❌ Error decoding image: ${e.message}")
                                e.printStackTrace()
                                null
                            }
                        } else {
                            println("   ⚠️ No image bytes received")
                            null
                        }
                    } catch (e: Exception) {
                        println("   ❌ Error loading image: ${e.message}")
                        e.printStackTrace()
                        null
                    }
                }
                
                val successCount = bitmaps.count { it != null }
                println("📊 Collection '${collection.name}': Loaded $successCount/${allImageUrls.size} images successfully")
                
                withContext(Dispatchers.Main) {
                    loadedImages = bitmaps
                }
            }
        }
    }
    
    // Auto-rotate carousel every 3 seconds
    LaunchedEffect(loadedImages) {
        if (loadedImages.size > 1) {
            while (true) {
                kotlinx.coroutines.delay(3000) // 3 seconds
                currentImageIndex = (currentImageIndex + 1) % loadedImages.size
            }
        }
    }

    Card(
        modifier = modifier
            .height(80.dp),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color(0xFFFAFAFA),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Collection Image Carousel
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                val currentImage = loadedImages.getOrNull(currentImageIndex)
                
                if (currentImage != null) {
                    Image(
                        bitmap = currentImage,
                        contentDescription = collection.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Show indicator dots if multiple images
                    if (loadedImages.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            loadedImages.indices.forEach { index ->
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(
                                            if (index == currentImageIndex) 
                                                Color.White 
                                            else 
                                                Color.White.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }
                    }
                } else if (loadedImages.isEmpty() && allImageUrls.isNotEmpty()) {
                    // Loading indicator
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFFD4AF37),
                        strokeWidth = 2.dp
                    )
                } else {
                    // No images
                    Icon(
                        Icons.Default.Collections,
                        contentDescription = null,
                        tint = Color(0xFFBBBBBB),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Collection Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = collection.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (collection.description.isNotEmpty()) {
                    Text(
                        text = collection.description,
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Product Count Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFD4AF37),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = productCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Edit Button
            IconButton(
                onClick = { onEdit(collection) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Collection",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete Button
            IconButton(
                onClick = { onDelete(collection) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Collection",
                    tint = Color(0xFFCC0000),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddCollectionDialog(
    initialCollection: ThemedCollection? = null,
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialCollection?.name ?: "") }
    var description by remember { mutableStateOf(initialCollection?.description ?: "") }
    var selectedImageUrl by remember { mutableStateOf(initialCollection?.imageUrl ?: "") }
    var selectedImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val storageService = JewelryAppInitializer.getStorageService()

    // Load existing image if editing
    LaunchedEffect(initialCollection?.imageUrl) {
        if (initialCollection?.imageUrl?.isNotEmpty() == true) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val imageBytes = when {
                        // If it's an HTTPS URL, load directly
                        initialCollection.imageUrl.startsWith("https://") || initialCollection.imageUrl.startsWith("http://") -> {
                            try {
                                URL(initialCollection.imageUrl).openStream().readBytes()
                            } catch (e: Exception) {
                                println("⚠️ Failed to load from URL, trying Firebase Storage path...")
                                null
                            }
                        }
                        // If it's a gs:// URL, extract path and use storage service
                        initialCollection.imageUrl.startsWith("gs://") -> {
                            val path = initialCollection.imageUrl.substringAfter("gs://").substringAfter("/")
                            storageService.downloadFileBytes(path)
                        }
                        // Otherwise, assume it's a storage path
                        else -> {
                            storageService.downloadFileBytes(initialCollection.imageUrl)
                        }
                    }
                    
                    if (imageBytes != null && imageBytes.isNotEmpty()) {
                        selectedImageBitmap = decodeAndResizeCategoryImage(imageBytes, maxSize = 200)
                    }
                } catch (e: Exception) {
                    println("Error loading existing image: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .width(500.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp,
            backgroundColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    if (initialCollection != null) "Edit Collection" else "Add New Collection",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )

                // Collection Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Collection Name") },
                    placeholder = { Text("Enter collection name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFD4AF37),
                        focusedLabelColor = Color(0xFFD4AF37),
                        cursorColor = Color(0xFFD4AF37)
                    )
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Enter description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFD4AF37),
                        focusedLabelColor = Color(0xFFD4AF37),
                        cursorColor = Color(0xFFD4AF37)
                    )
                )

                // Image Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Collection Image",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )

                    // Image Preview
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .border(2.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
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
                                Icons.Default.Collections,
                                contentDescription = "No Image",
                                tint = Color(0xFFBBBBBB),
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
                                    title = "Select Collection Image"
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
                                        errorMessage = null
                                    }

                                    val directoryPath = "collections/${System.currentTimeMillis()}"
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
                                                val imageBytes = URL(imageUrl).openStream().readBytes()
                                                decodeAndResizeCategoryImage(imageBytes, maxSize = 200)
                                            }
                                        } else {
                                            errorMessage = "Failed to upload image"
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isUploading,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFD4AF37),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Image")
                    }

                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFFD4AF37)
                        )
                    }

                    errorMessage?.let {
                        Text(it, color = Color.Red, fontSize = 12.sp)
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF666666)
                        )
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name, description, selectedImageUrl)
                            }
                        },
                        enabled = name.isNotBlank() && !isUploading,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFD4AF37),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (initialCollection != null) "Save Changes" else "Add Collection")
                    }
                }
            }
        }
    }
}
