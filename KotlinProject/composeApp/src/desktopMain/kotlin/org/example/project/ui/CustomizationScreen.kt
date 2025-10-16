package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.*
import org.example.project.viewModels.CustomizationViewModel

@Composable
fun CustomizationScreen(
    viewModel: CustomizationViewModel,
    onBack: () -> Unit
) {
    val collections by viewModel.draftCollections.collectAsState()
    val carouselItems by viewModel.draftCarouselItems.collectAsState()
    val appConfig by viewModel.draftAppConfig.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedCollectionId by viewModel.selectedCollectionId.collectAsState()
    val selectedCarouselId by viewModel.selectedCarouselId.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var showAddCollectionDialog by remember { mutableStateOf(false) }
    var showAddCarouselDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Header
        CustomizationHeader(
            onBack = onBack,
            onSaveDraft = { viewModel.saveDraft() },
            onPublish = { viewModel.publishChanges() },
            onDiscard = { viewModel.discardChanges() },
            onLoadDummyData = { viewModel.loadDummyData() },
            loading = loading
        )

        // Error display
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                backgroundColor = Color(0xFFFFEBEE),
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        errorMessage,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFFD32F2F))
                    }
                }
            }
        }

        // Main content - Split view
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel - Control & Edit Panel (60%)
            ControlPanel(
                modifier = Modifier.weight(0.6f),
                collections = collections,
                carouselItems = carouselItems,
                appConfig = appConfig,
                currentTab = currentTab,
                selectedCollectionId = selectedCollectionId,
                selectedCarouselId = selectedCarouselId,
                onTabChange = { viewModel.setCurrentTab(it) },
                onCollectionUpdate = { viewModel.updateDraftCollection(it) },
                onCarouselUpdate = { viewModel.updateDraftCarouselItem(it) },
                onToggleCollectionActive = { viewModel.toggleCollectionActive(it) },
                onToggleCarouselActive = { viewModel.toggleCarouselItemActive(it) },
                onToggleTitleActive = { viewModel.toggleCarouselItemTitleActive(it) },
                onToggleSubtitleActive = { viewModel.toggleCarouselItemSubtitleActive(it) },
                onToggleCollectionsEnabled = { viewModel.toggleCollectionsEnabled() },
                onToggleCarouselsEnabled = { viewModel.toggleCarouselsEnabled() },
                onAddCollection = { showAddCollectionDialog = true },
                onAddCarousel = { showAddCarouselDialog = true }
            )

            // Right Panel - Live Preview (40%)
            PreviewPanel(
                modifier = Modifier.weight(0.4f),
                collections = viewModel.getActiveCollectionsForPreview(),
                carouselItems = viewModel.getActiveCarouselItemsForPreview(),
                onCollectionClick = { viewModel.selectCollection(it) },
                onCarouselClick = { viewModel.selectCarousel(it) }
            )
        }
    }
    
    // Dialogs
    if (showAddCollectionDialog) {
        AddCollectionDialog(
            onDismiss = { showAddCollectionDialog = false },
            onAdd = { name, description, imageUrls ->
                viewModel.addNewCollection(name, description, imageUrls)
                showAddCollectionDialog = false
            }
        )
    }
    
    if (showAddCarouselDialog) {
        AddCarouselDialog(
            onDismiss = { showAddCarouselDialog = false },
            onAdd = { title, subtitle, imageUrl ->
                viewModel.addNewCarouselItem(title, subtitle, imageUrl)
                showAddCarouselDialog = false
            }
        )
    }
}

@Composable
private fun CustomizationHeader(
    onBack: () -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    onDiscard: () -> Unit,
    onLoadDummyData: () -> Unit,
    loading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Customization Manager",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLoadDummyData,
                    enabled = !loading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load Dummy Data")
                }
                OutlinedButton(
                    onClick = onDiscard,
                    enabled = !loading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF666666))
                ) {
                    Text("Discard")
                }
                OutlinedButton(
                    onClick = onSaveDraft,
                    enabled = !loading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2196F3))
                ) {
                    Text("Save Draft")
                }
                Button(
                    onClick = onPublish,
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Publish")
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlPanel(
    modifier: Modifier = Modifier,
    collections: List<ThemedCollection>,
    carouselItems: List<CarouselItem>,
    appConfig: AppConfig?,
    currentTab: Int,
    selectedCollectionId: String?,
    selectedCarouselId: String?,
    onTabChange: (Int) -> Unit,
    onCollectionUpdate: (ThemedCollection) -> Unit,
    onCarouselUpdate: (CarouselItem) -> Unit,
    onToggleCollectionActive: (String) -> Unit,
    onToggleCarouselActive: (String) -> Unit,
    onToggleTitleActive: (String) -> Unit,
    onToggleSubtitleActive: (String) -> Unit,
    onToggleCollectionsEnabled: () -> Unit,
    onToggleCarouselsEnabled: () -> Unit,
    onAddCollection: () -> Unit,
    onAddCarousel: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxSize(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Tab bar
            TabRow(
                selectedTabIndex = currentTab,
                backgroundColor = Color(0xFFF5F5F5)
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { onTabChange(0) },
                    text = { Text("Collections") }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { onTabChange(1) },
                    text = { Text("Carousel") }
                )
            }

            // Content based on selected tab
            when (currentTab) {
                0 -> CollectionsManager(
                    collections = collections,
                    appConfig = appConfig,
                    selectedCollectionId = selectedCollectionId,
                    onCollectionUpdate = onCollectionUpdate,
                    onToggleCollectionActive = onToggleCollectionActive,
                    onToggleCollectionsEnabled = onToggleCollectionsEnabled,
                    onAddCollection = onAddCollection
                )
                1 -> CarouselManager(
                    carouselItems = carouselItems,
                    appConfig = appConfig,
                    selectedCarouselId = selectedCarouselId,
                    onCarouselUpdate = onCarouselUpdate,
                    onToggleCarouselActive = onToggleCarouselActive,
                    onToggleTitleActive = onToggleTitleActive,
                    onToggleSubtitleActive = onToggleSubtitleActive,
                    onToggleCarouselsEnabled = onToggleCarouselsEnabled,
                    onAddCarousel = onAddCarousel
                )
            }
        }
    }
}

@Composable
private fun CollectionsManager(
    collections: List<ThemedCollection>,
    appConfig: AppConfig?,
    selectedCollectionId: String?,
    onCollectionUpdate: (ThemedCollection) -> Unit,
    onToggleCollectionActive: (String) -> Unit,
    onToggleCollectionsEnabled: () -> Unit,
    onAddCollection: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Global toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFE3F2FD),
                elevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Collections Enabled",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = appConfig?.collectionsEnabled ?: true,
                        onCheckedChange = { onToggleCollectionsEnabled() }
                    )
                }
            }
        }

        // Collections list
        items(collections) { collection ->
            CollectionItem(
                collection = collection,
                isActiveInConfig = appConfig?.activeCollectionIds?.contains(collection.id) ?: false,
                isSelected = selectedCollectionId == collection.id,
                onToggleActive = { onToggleCollectionActive(collection.id) },
                onUpdate = onCollectionUpdate
            )
        }

        // Add new collection button
        item {
            OutlinedButton(
                onClick = onAddCollection,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2196F3))
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Collection")
            }
        }
    }
}

@Composable
private fun CollectionItem(
    collection: ThemedCollection,
    isActiveInConfig: Boolean,
    isSelected: Boolean,
    onToggleActive: () -> Unit,
    onUpdate: (ThemedCollection) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = Color(0xFF2196F3),
                shape = RoundedCornerShape(8.dp)
            ),
        elevation = if (isSelected) 4.dp else 2.dp,
        backgroundColor = when {
            isSelected -> Color(0xFFE3F2FD)
            collection.isActive && isActiveInConfig -> Color.White
            else -> Color(0xFFF5F5F5)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Collection header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Switch(
                        checked = collection.isActive,
                        onCheckedChange = { onToggleActive() }
                    )
                    Text(
                        collection.name.ifEmpty { "Untitled Collection" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (collection.isActive) Color.Black else Color.Gray
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Order: ${collection.order}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            // Collection details (when expanded)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Name field
                CollectionField(
                    label = "Name",
                    value = collection.name,
                    isActive = collection.isActive,
                    onValueChange = { onUpdate(collection.copy(name = it)) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description field
                CollectionField(
                    label = "Description",
                    value = collection.description,
                    isActive = collection.isActive,
                    onValueChange = { onUpdate(collection.copy(description = it)) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Images section
                Text(
                    "Images (${collection.images.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (collection.isActive) Color.Black else Color.Gray
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(collection.images) { image ->
                        ImageSlot(
                            image = image,
                            isCollectionActive = collection.isActive,
                            onUpdate = { updatedImage ->
                                val updatedImages = collection.images.toMutableList()
                                val index = updatedImages.indexOf(image)
                                if (index >= 0) {
                                    updatedImages[index] = updatedImage
                                    onUpdate(collection.copy(images = updatedImages))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionField(
    label: String,
    value: String,
    isActive: Boolean,
    onValueChange: (String) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) Color.Black else Color.Gray
            )
            Switch(
                checked = isActive,
                onCheckedChange = { /* TODO: Implement field-level active toggle */ },
                modifier = Modifier.size(24.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = isActive,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledTextColor = Color.Gray,
                disabledBorderColor = Color(0xFFE0E0E0)
            )
        )
    }
}

@Composable
private fun ImageSlot(
    image: CollectionImage,
    isCollectionActive: Boolean,
    onUpdate: (CollectionImage) -> Unit
) {
    Card(
        modifier = Modifier.size(80.dp),
        elevation = 2.dp,
        backgroundColor = if (image.isActive && isCollectionActive) Color.White else Color(0xFFF5F5F5)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (image.url.isNotEmpty()) {
                // TODO: Load actual image
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("IMG", fontSize = 10.sp, color = Color.Gray)
                }
            } else {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add image",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Switch(
                checked = image.isActive,
                onCheckedChange = { onUpdate(image.copy(isActive = it)) },
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CarouselManager(
    carouselItems: List<CarouselItem>,
    appConfig: AppConfig?,
    selectedCarouselId: String?,
    onCarouselUpdate: (CarouselItem) -> Unit,
    onToggleCarouselActive: (String) -> Unit,
    onToggleTitleActive: (String) -> Unit,
    onToggleSubtitleActive: (String) -> Unit,
    onToggleCarouselsEnabled: () -> Unit,
    onAddCarousel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Global toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFE8F5E8),
                elevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Carousels Enabled",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = appConfig?.carouselsEnabled ?: true,
                        onCheckedChange = { onToggleCarouselsEnabled() }
                    )
                }
            }
        }

        // Carousel items list
        items(carouselItems) { item ->
            CarouselItem(
                item = item,
                isActiveInConfig = appConfig?.activeCarouselIds?.contains(item.id) ?: false,
                isSelected = selectedCarouselId == item.id,
                onToggleActive = { onToggleCarouselActive(item.id) },
                onToggleTitleActive = { onToggleTitleActive(item.id) },
                onToggleSubtitleActive = { onToggleSubtitleActive(item.id) },
                onUpdate = onCarouselUpdate
            )
        }

        // Add new carousel item button
        item {
            OutlinedButton(
                onClick = onAddCarousel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Carousel Item")
            }
        }
    }
}

@Composable
private fun CarouselItem(
    item: CarouselItem,
    isActiveInConfig: Boolean,
    isSelected: Boolean,
    onToggleActive: () -> Unit,
    onToggleTitleActive: () -> Unit,
    onToggleSubtitleActive: () -> Unit,
    onUpdate: (CarouselItem) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = Color(0xFF4CAF50),
                shape = RoundedCornerShape(8.dp)
            ),
        elevation = if (isSelected) 4.dp else 2.dp,
        backgroundColor = when {
            isSelected -> Color(0xFFE8F5E8)
            item.isActive && isActiveInConfig -> Color.White
            else -> Color(0xFFF5F5F5)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Carousel item header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = { onToggleActive() }
                    )
                    Text(
                        item.title.ifEmpty { "Untitled Carousel" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (item.isActive) Color.Black else Color.Gray
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Order: ${item.order}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            // Carousel item details (when expanded)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Image
                CarouselImageField(
                    image = item.image,
                    isItemActive = item.isActive,
                    onUpdate = { onUpdate(item.copy(image = it)) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Title field
                CarouselTextField(
                    label = "Title",
                    value = item.title,
                    isActive = item.titleActive && item.isActive,
                    onValueChange = { onUpdate(item.copy(title = it)) },
                    onToggleActive = onToggleTitleActive
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtitle field
                CarouselTextField(
                    label = "Subtitle",
                    value = item.subtitle,
                    isActive = item.subtitleActive && item.isActive,
                    onValueChange = { onUpdate(item.copy(subtitle = it)) },
                    onToggleActive = onToggleSubtitleActive
                )
            }
        }
    }
}

@Composable
private fun CarouselImageField(
    image: CarouselImage,
    isItemActive: Boolean,
    onUpdate: (CarouselImage) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Image",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (image.isActive && isItemActive) Color.Black else Color.Gray
            )
            Switch(
                checked = image.isActive,
                onCheckedChange = { onUpdate(image.copy(isActive = it)) },
                modifier = Modifier.size(24.dp)
            )
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            elevation = 2.dp,
            backgroundColor = if (image.isActive && isItemActive) Color.White else Color(0xFFF5F5F5)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (image.url.isNotEmpty()) {
                    // TODO: Load actual image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Carousel Image", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add image",
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Text("Add Image", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun CarouselTextField(
    label: String,
    value: String,
    isActive: Boolean,
    onValueChange: (String) -> Unit,
    onToggleActive: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) Color.Black else Color.Gray
            )
            Switch(
                checked = isActive,
                onCheckedChange = { onToggleActive() },
                modifier = Modifier.size(24.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = isActive,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledTextColor = Color.Gray,
                disabledBorderColor = Color(0xFFE0E0E0)
            )
        )
    }
}

@Composable
private fun PreviewPanel(
    modifier: Modifier = Modifier,
    collections: List<ThemedCollection>,
    carouselItems: List<CarouselItem>,
    onCollectionClick: (String) -> Unit,
    onCarouselClick: (String) -> Unit
) {
    Card(
        modifier = modifier.fillMaxSize(),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Preview header
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF2D2D2D),
                elevation = 0.dp,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Mobile Preview - Click to Select",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Virtual phone frame
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Phone frame
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .height(440.dp),
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = Color(0xFF2D2D2D)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Phone screen
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            elevation = 0.dp,
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Color(0xFFF8F8F8)
                        ) {
                            // Screen content - Complete Home Screen Preview
                            PreviewHomeScreen(
                                modifier = Modifier.fillMaxSize(),
                                categories = emptyList(), // TODO: Pass actual categories from Firestore
                                featuredProducts = emptyList(), // TODO: Pass actual featured products from Firestore
                                collections = collections,
                                carouselItems = carouselItems,
                                onProductClick = { /* Empty for now */ },
                                onCollectionClick = onCollectionClick,
                                onCategoryClick = { /* Empty for now */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCollectionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl1 by remember { mutableStateOf("") }
    var imageUrl2 by remember { mutableStateOf("") }
    var imageUrl3 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Collection", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Collection Name") },
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
                Text("Image URLs (optional)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = imageUrl1,
                    onValueChange = { imageUrl1 = it },
                    label = { Text("Image 1 URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = imageUrl2,
                    onValueChange = { imageUrl2 = it },
                    label = { Text("Image 2 URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = imageUrl3,
                    onValueChange = { imageUrl3 = it },
                    label = { Text("Image 3 URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val imageUrls = listOf(imageUrl1, imageUrl2, imageUrl3).filter { it.isNotEmpty() }
                    onAdd(name, description, imageUrls)
                },
                enabled = name.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2196F3))
            ) {
                Text("Add Collection")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddCarouselDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Carousel Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(title, subtitle, imageUrl) },
                enabled = title.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Text("Add Carousel Item")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
