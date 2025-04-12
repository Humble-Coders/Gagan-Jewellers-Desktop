package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.example.project.JewelryAppInitializer
import org.example.project.utils.ImageLoader
import org.jetbrains.skia.Image
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Paths

/**
 * A composable for managing product images in Add/Edit screens.
 * This component handles displaying existing images and uploading new ones.
 */
@Composable
fun ProductImageManager(
    existingImages: List<String>,
    onImagesChanged: (List<String>) -> Unit,
    imageLoader: ImageLoader,
    productId: String = ""
) {
    val coroutineScope = rememberCoroutineScope()
    val storageService = JewelryAppInitializer.getStorageService()
    var images by remember { mutableStateOf(existingImages) }
    var imageDisplayList by remember { mutableStateOf<List<Pair<String, ImageBitmap?>>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var imageToDelete by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Create a progress flow that we'll observe
    val progressFlow = remember { MutableStateFlow(0f) }

    // Observe the progress flow
    LaunchedEffect(progressFlow) {
        progressFlow.collect { progress ->
            uploadProgress = progress
        }
    }

    // Initialize the list of image display data
    LaunchedEffect(images) {
        // Create a list of image URLs paired with null bitmaps initially
        imageDisplayList = images.map { it to null }

        // Load each image asynchronously
        images.forEachIndexed { index, imageUrl ->
            coroutineScope.launch {
                try {
                    println("Loading image: $imageUrl")
                    val imageBytes = imageLoader.loadImage(imageUrl)
                    if (imageBytes != null) {
                        println("Image loaded, size: ${imageBytes.size} bytes")
                        val bitmap = withContext(Dispatchers.IO) {
                            Image.makeFromEncoded(imageBytes).asImageBitmap()
                        }
                        // Update just this image in the list
                        val updatedList = imageDisplayList.toMutableList()
                        if (index < updatedList.size) {
                            updatedList[index] = imageUrl to bitmap
                            imageDisplayList = updatedList
                        }
                    } else {
                        println("Failed to load image: $imageUrl")
                    }
                } catch (e: Exception) {
                    println("Error loading image: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Product Images",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Error message display
        errorMessage?.let { error ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Error",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    error,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { errorMessage = null }) {
                    Text("✕", color = Color.Red)
                }
            }
        }

        // Image gallery
        if (imageDisplayList.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(140.dp).padding(bottom = 16.dp)
            ) {
                itemsIndexed(imageDisplayList) { index, (imageUrl, bitmap) ->
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Product image ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }

                        // Delete button overlay
                        IconButton(
                            onClick = {
                                imageToDelete = imageUrl
                                showDeleteDialog = true
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(32.dp)
                                .padding(4.dp)
                                .background(Color(0x80000000), RoundedCornerShape(4.dp))
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete image",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // No images placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5))
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No images added yet", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Upload button & progress indicator
        Button(
            onClick = {
                // Use a safer approach to file selection
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        println("Opening file dialog...")
                        // Create the file dialog on the IO dispatcher
                        val fileDialog = FileDialog(Frame()).apply {
                            mode = FileDialog.LOAD
                            title = "Select Image"
                            file = "*.jpg;*.jpeg;*.png;*.gif"
                            isVisible = true
                        }

                        val selectedFile = if (fileDialog.directory != null && fileDialog.file != null) {
                            println("Selected file: ${fileDialog.directory}${fileDialog.file}")
                            File(fileDialog.directory, fileDialog.file).takeIf { it.exists() }
                        } else {
                            println("No file selected or dialog cancelled")
                            null
                        }

                        // If a file was selected, upload it
                        if (selectedFile != null) {
                            withContext(Dispatchers.Main) {
                                isUploading = true
                                progressFlow.value = 0f
                                errorMessage = null
                            }

                            // Determine directory path based on product ID
                            val directoryPath = if (productId.isNotEmpty()) {
                                "products/$productId"
                            } else {
                                "products/${System.currentTimeMillis()}"
                            }

                            println("Starting upload to $directoryPath")
                            val imageUrl = storageService.uploadFile(
                                Paths.get(selectedFile.absolutePath),
                                directoryPath,
                                progressFlow
                            )

                            withContext(Dispatchers.Main) {
                                isUploading = false

                                if (imageUrl != null) {
                                    println("Upload successful: $imageUrl")
                                    val updatedImages = images + imageUrl
                                    images = updatedImages
                                    onImagesChanged(updatedImages)
                                } else {
                                    errorMessage = "Failed to upload image. Please try again."
                                    println("Upload failed, no URL returned")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Error selecting/uploading image: ${e.message}")
                        e.printStackTrace()

                        withContext(Dispatchers.Main) {
                            isUploading = false
                            errorMessage = "Error: ${e.message ?: "Unknown error"}"
                        }
                    }
                }
            },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add image")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Image")
        }

        if (isUploading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = uploadProgress,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Uploading image... ${(uploadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    // Confirmation dialog for deleting images
    if (showDeleteDialog && imageToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                imageToDelete = null
            },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this image?") },
            confirmButton = {
                Button(
                    onClick = {
                        imageToDelete?.let { urlToDelete ->
                            // Remove from list first
                            val updatedImages = images.filter { it != urlToDelete }
                            images = updatedImages
                            onImagesChanged(updatedImages)

                            // Then attempt to delete from Firebase Storage
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    println("Deleting image: $urlToDelete")
                                    val deleted = storageService.deleteFile(urlToDelete)
                                    if (deleted) {
                                        println("Successfully deleted image: $urlToDelete")
                                    } else {
                                        println("Failed to delete image: $urlToDelete")
                                    }
                                } catch (e: Exception) {
                                    println("Error deleting image: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                        }
                        showDeleteDialog = false
                        imageToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDeleteDialog = false
                    imageToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}