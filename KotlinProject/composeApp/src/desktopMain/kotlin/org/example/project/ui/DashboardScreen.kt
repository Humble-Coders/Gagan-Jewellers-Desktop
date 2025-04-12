package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.utils.ImageLoader
import org.example.project.data.Product
import org.example.project.viewModels.ProductsViewModel
import org.jetbrains.skia.Image

@Composable
fun DashboardScreen(
    viewModel: ProductsViewModel,
    imageLoader: ImageLoader,
    onAddProduct: () -> Unit,
    onViewProductDetails: (String) -> Unit  // Add this parameter

) {
    val products by remember { viewModel.products }
    val showDeleteDialog = remember { mutableStateOf(false) }
    val productToDelete = remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header with title and add button
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Products",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onAddProduct,
                colors = ButtonDefaults.buttonColors(MaterialTheme.colors.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add New Product")
            }
        }

        // Search bar
        OutlinedTextField(
            value = "",
            onValueChange = {  },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Search products...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )

        // Products table
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
        ) {
            // Table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(0.15f), contentAlignment = Alignment.CenterStart) {
                    Text("Image", fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.weight(0.2f), contentAlignment = Alignment.CenterStart) {
                    Text("Name", fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.weight(0.15f), contentAlignment = Alignment.CenterStart) {
                    Text("Category", fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.weight(0.15f), contentAlignment = Alignment.CenterStart) {
                    Text("Material", fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.weight(0.1f), contentAlignment = Alignment.CenterStart) {
                    Text("Price", fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.weight(0.1f), contentAlignment = Alignment.CenterStart) {
                    Text("Available", fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.weight(0.15f), contentAlignment = Alignment.CenterStart) {
                    Text("Actions", fontWeight = FontWeight.Bold)
                }
            }

            Divider()

            // Table content
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No products found. Add a new product to get started.",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn {
                    items(products) { product ->
                        ProductRow(
                            product = product,
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onDelete = {
                                productToDelete.value = product.id
                                showDeleteDialog.value = true
                            },
                            // Add this new parameter
                            onClick = { onViewProductDetails(product.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog.value = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this product? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete.value?.let { viewModel.deleteProduct(it) }
                        showDeleteDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun ProductRow(
    product: Product,
    viewModel: ProductsViewModel,
    imageLoader: ImageLoader,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var productImage by remember { mutableStateOf<ImageBitmap?>(null) }

    // Load the first image if available
    LaunchedEffect(product.images) {
        if (product.images.isNotEmpty()) {
            val imageUrl = product.images.first()
            coroutineScope.launch {
                val imageBytes = imageLoader.loadImage(imageUrl)
                imageBytes?.let {
                    val image = withContext(Dispatchers.IO) {
                        Image.makeFromEncoded(it).toComposeImageBitmap()
                    }
                    productImage = image
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick=onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product image
        Box(
            modifier = Modifier.weight(0.15f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (productImage != null) {
                Image(
                    bitmap = productImage!!,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        // Name
        Box(modifier = Modifier.weight(0.2f), contentAlignment = Alignment.CenterStart) {
            Text(
                text = product.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Category
        Box(modifier = Modifier.weight(0.15f), contentAlignment = Alignment.CenterStart) {
            Text(
                text = viewModel.getCategoryName(product.categoryId),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Material
        Box(modifier = Modifier.weight(0.15f), contentAlignment = Alignment.CenterStart) {
            Text(
                text = viewModel.getMaterialName(product.materialId),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Price
        Box(modifier = Modifier.weight(0.1f), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "$${product.price}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Availability status
        Box(
            modifier = Modifier.weight(0.1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(width = 70.dp, height = 30.dp)
                    .background(
                        if (product.available) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (product.available) "Yes" else "No",
                    color = if (product.available) Color(0xFF388E3C) else Color(0xFFD32F2F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Action buttons
        Box(
            modifier = Modifier.weight(0.15f),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {


                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFEBEE), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}