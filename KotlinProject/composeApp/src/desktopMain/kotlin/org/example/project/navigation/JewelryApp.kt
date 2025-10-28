package org.example.project.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.EaseInQuad
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.JewelryAppInitializer
import org.example.project.viewModels.ProductsViewModel
import org.example.project.viewModels.CustomizationViewModel
import org.example.project.viewModels.ProfileViewModel
import org.example.project.data.FirestoreCustomizationRepository
import org.example.project.data.OrderRepository
import org.example.project.data.FirestoreOrderRepository
import org.example.project.data.CashAmountRepository
import org.example.project.data.FirestoreCashAmountRepository
import org.example.project.ui.AddEditProductScreen
import org.example.project.ui.BarcodeEditScreen
import org.example.project.ui.BillingScreen
import org.example.project.ui.DashboardScreen
import org.example.project.ui.ProfileScreen
import org.example.project.ui.CustomerTransactionsScreen
import org.example.project.ui.ProductDetailScreen
import org.example.project.ui.SettingsScreen
import org.example.project.ui.GoldRateScreen
import org.example.project.ui.CategoryManagementScreen
import org.example.project.ui.CustomizationScreen
import org.example.project.ui.AppointmentScreen
import org.jetbrains.compose.resources.painterResource
import kotlinproject.composeapp.generated.resources.Res

@Composable
fun JewelryApp(viewModel: ProductsViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var isSidebarExpanded by remember { mutableStateOf(true) }
    var isSidebarVisible by remember { mutableStateOf(true) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var snackbarAction by remember { mutableStateOf<String?>(null) }
    var snackbarActionCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var currentBarcodeId by remember { mutableStateOf("") }
    var showBarcodeDeleteDialog by remember { mutableStateOf(false) }
    var barcodeToDelete by remember { mutableStateOf("") }
    var showBarcodeEditDialog by remember { mutableStateOf(false) }
    var barcodeToEdit by remember { mutableStateOf("") }
    var newBarcodeId by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    // Dashboard view preferences to control what is shown when returning
    var dashboardStartInProductsView by remember { mutableStateOf(false) }
    var dashboardInitialSelectedCategoryId by remember { mutableStateOf<String?>(null) }
    var lastViewedCategoryId by remember { mutableStateOf<String?>(null) }
    val imageLoader = JewelryAppInitializer.getImageLoader()
    val cartViewModel = JewelryAppInitializer.getCartViewModel()
    val customerViewModel = JewelryAppInitializer.getCustomerViewModel()
    val paymentViewModel = JewelryAppInitializer.getPaymentViewModel()
    val appointmentViewModel = JewelryAppInitializer.getAppointmentViewModel()
    val availabilityRepository = JewelryAppInitializer.getAvailabilityRepository()
    val customizationViewModel = CustomizationViewModel(
        FirestoreCustomizationRepository(JewelryAppInitializer.getFirestore())
    )
    val profileViewModel = ProfileViewModel(
        JewelryAppInitializer.getCustomerRepository(),
        FirestoreOrderRepository(JewelryAppInitializer.getFirestore()),
        FirestoreCashAmountRepository(JewelryAppInitializer.getFirestore())
    )

    // Function to show snackbar
    fun showSnackbar(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        snackbarMessage = message
        snackbarAction = actionLabel
        snackbarActionCallback = onAction
        showSnackbar = true
    }

    MaterialTheme(
        colors = lightColors(
            primary = Color(0xFFC9AD6E),
            primaryVariant = Color(0xFFB8973D),
            secondary = Color(0xFF7D7D7D),
            background = Color(0xFFF9F9F9),
            surface = Color.White
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row {
                // Enhanced Sidebar with fixed icons and professional styling
                EnhancedSidebar(
                    isSidebarExpanded = isSidebarExpanded,
                    isSidebarVisible = isSidebarVisible,
                    currentScreen = currentScreen,
                    onScreenChange = { screen ->
                        when (screen) {
                            Screen.ADD_PRODUCT -> viewModel.createNewProduct()
                            else -> {}
                        }
                        currentScreen = screen
                    },
                    onSidebarToggle = { expanded ->
                        isSidebarExpanded = expanded
                    },
                    onSidebarVisibilityChange = { visible ->
                        isSidebarVisible = visible
                    }
                )

                // Main content area with hover detection
                val contentInteractionSource = remember { MutableInteractionSource() }
                val isContentHovered by contentInteractionSource.collectIsHoveredAsState()

                // Collapse sidebar when hovering over content
                LaunchedEffect(isContentHovered) {
                    if (isContentHovered && isSidebarExpanded && isSidebarVisible) {
                        isSidebarExpanded = false
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .hoverable(contentInteractionSource)
                ) {
                    EnhancedTopAppBar(
                        title = {
                            Text(
                                "Gagan Jewellers Pvt Ltd",
                                fontWeight = FontWeight.Medium
                            )
                        },
                        isSidebarExpanded = isSidebarExpanded,
                        isSidebarVisible = isSidebarVisible,
                        onToggleSidebar = {
                            isSidebarVisible = true
                            isSidebarExpanded = true
                        },
                        onRefresh = {
                            coroutineScope.launch {
                                viewModel.loadProducts()
                                showSnackbar("Data refreshed successfully", "PROFILE", {
                                    currentScreen = Screen.PROFILE
                                })
                            }
                        }
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Content screens
                        when (currentScreen) {
                            Screen.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                imageLoader = imageLoader,
                                onAddProduct = {
                                    viewModel.createNewProduct()
                                    currentScreen = Screen.ADD_PRODUCT
                                    showSnackbar("Ready to add new product")
                                },
                                onViewProductDetails = { productId ->
                                    // Capture the category for the selected product so we can return to it
                                    val product = viewModel.products.value.find { it.id == productId }
                                    lastViewedCategoryId = product?.categoryId
                                    viewModel.selectProduct(productId)
                                    // Back should return to products list filtered by this category
                                    dashboardStartInProductsView = true
                                    dashboardInitialSelectedCategoryId = lastViewedCategoryId
                                    currentScreen = Screen.PRODUCT_DETAIL
                                },
                                startInProductsView = dashboardStartInProductsView,
                                initialSelectedCategoryId = dashboardInitialSelectedCategoryId,
            onEditBarcode = { barcodeId ->
                println("✏️ NAVIGATION: Edit Barcode Request")
                println("   - Barcode ID: $barcodeId")
                println("   - Timestamp: ${System.currentTimeMillis()}")
                barcodeToEdit = barcodeId
                newBarcodeId = barcodeId
                showBarcodeEditDialog = true
                println("   - Showing barcode edit dialog")
            },
                                onDeleteBarcode = { barcodeId ->
                                    println("🗑️ NAVIGATION: Delete Barcode Request")
                                    println("   - Barcode ID: $barcodeId")
                                    println("   - Timestamp: ${System.currentTimeMillis()}")
                                    // Show confirmation dialog for barcode deletion
                                    barcodeToDelete = barcodeId
                                    showBarcodeDeleteDialog = true
                                    println("   - Showing delete confirmation dialog")
                                },
                                onDuplicateProduct = { productId, barcodeId ->
                                    // Create a duplicate product with the new barcode
                                    viewModel.duplicateProductWithBarcode(productId, barcodeId)
                                    showSnackbar("Product duplicated successfully")
                                }
                            )

                            Screen.ADD_PRODUCT -> AddEditProductScreen(
                                viewModel = viewModel,
                                onSave = {
                                    coroutineScope.launch {
                                        // Ensure dashboard shows the new product immediately
                                        viewModel.loadProducts()
                                        viewModel.triggerInventoryRefresh()
                                        currentScreen = Screen.DASHBOARD
                                        showSnackbar("Product saved successfully", "VIEW", {
                                            currentScreen = Screen.PRODUCT_DETAIL
                                        })
                                    }
                                },
                                onCancel = {
                                    viewModel.clearCurrentProduct()
                                    currentScreen = Screen.DASHBOARD
                                    showSnackbar("Product creation canceled")
                                },
                                onEditBarcode = { barcodeId ->
                                    println("✏️ NAVIGATION: Edit Barcode Request (from Add Product)")
                                    println("   - Barcode ID: $barcodeId")
                                    println("   - Timestamp: ${System.currentTimeMillis()}")
                                    barcodeToEdit = barcodeId
                                    newBarcodeId = barcodeId
                                    showBarcodeEditDialog = true
                                }
                            )

                            Screen.EDIT_PRODUCT -> AddEditProductScreen(
                                viewModel = viewModel,
                                onSave = {
                                    coroutineScope.launch {
                                        // Ensure dashboard reflects updates immediately
                                        viewModel.loadProducts()
                                        viewModel.triggerInventoryRefresh()
                                        currentScreen = Screen.DASHBOARD
                                        showSnackbar("Product updated successfully", "VIEW", {
                                            currentScreen = Screen.PRODUCT_DETAIL
                                        })
                                    }
                                },
                                onCancel = {
                                    viewModel.clearCurrentProduct()
                                    currentScreen = Screen.DASHBOARD
                                    showSnackbar("Editing canceled")
                                },
                                onEditBarcode = { barcodeId ->
                                    println("✏️ NAVIGATION: Edit Barcode Request (from Edit Product)")
                                    println("   - Barcode ID: $barcodeId")
                                    println("   - Timestamp: ${System.currentTimeMillis()}")
                                    barcodeToEdit = barcodeId
                                    newBarcodeId = barcodeId
                                    showBarcodeEditDialog = true
                                },
                                isEditing = true
                            )

                            Screen.PRODUCT_DETAIL -> ProductDetailScreen(
                                viewModel = viewModel,
                                imageLoader = imageLoader,
                                onEdit = { currentScreen = Screen.EDIT_PRODUCT },
                                onBack = {
                                    // Return to products list filtered by last viewed category
                                    dashboardStartInProductsView = true
                                    dashboardInitialSelectedCategoryId = lastViewedCategoryId
                                    currentScreen = Screen.DASHBOARD
                                }
                            )

                            Screen.BARCODE_EDIT -> BarcodeEditScreen(
                                barcodeId = currentBarcodeId,
                                viewModel = viewModel,
                                onBack = { 
                                    dashboardStartInProductsView = false
                                    currentScreen = Screen.DASHBOARD 
                                },
                                onSave = {
                                    dashboardStartInProductsView = false
                                    currentScreen = Screen.DASHBOARD
                                    showSnackbar("Barcode document updated successfully")
                                }
                            )

                            Screen.SETTINGS -> SettingsScreen()

                            Screen.GOLD_RATES -> GoldRateScreen(
                                onBack = { 
                                    // Always reset to category cards when coming back from Metal Rates
                                    dashboardStartInProductsView = false
                                    dashboardInitialSelectedCategoryId = null
                                    currentScreen = Screen.DASHBOARD 
                                }
                            )

                            Screen.CATEGORIES -> CategoryManagementScreen(
                                productsViewModel = viewModel,
                                onBack = { 
                                    dashboardStartInProductsView = false
                                    currentScreen = Screen.DASHBOARD 
                                }
                            )

                            Screen.CUSTOMIZATION -> CustomizationScreen(
                                viewModel = customizationViewModel,
                                onBack = { 
                                    dashboardStartInProductsView = false
                                    currentScreen = Screen.DASHBOARD 
                                }
                            )

                            Screen.APPOINTMENTS -> AppointmentScreen(
                                viewModel = appointmentViewModel,
                                availabilityRepository = availabilityRepository,
                                onBack = { 
                                    dashboardStartInProductsView = false
                                    currentScreen = Screen.DASHBOARD 
                                }
                            )

                            Screen.BILLING -> BillingScreen(
                                customerViewModel = customerViewModel,
                                cartViewModel = cartViewModel,
                                productsViewModel = viewModel,
                                imageLoader = imageLoader,
                                paymentViewModel = paymentViewModel
                            )

                            Screen.PROFILE -> ProfileScreen(
                                viewModel = profileViewModel,
                                onBack = { 
                                    dashboardStartInProductsView = false
                                    currentScreen = Screen.DASHBOARD 
                                },
                                onCustomerClick = { customer ->
                                    profileViewModel.selectCustomer(customer)
                                    currentScreen = Screen.CUSTOMER_TRANSACTIONS
                                }
                            )

                            Screen.CUSTOMER_TRANSACTIONS -> {
                                val selectedCustomer = profileViewModel.selectedCustomer.value
                                if (selectedCustomer != null) {
                                    CustomerTransactionsScreen(
                                        customer = selectedCustomer,
                                        viewModel = profileViewModel,
                                        onBack = { currentScreen = Screen.PROFILE }
                                    )
                                } else {
                                    // Fallback to profile screen if no customer selected
                                    ProfileScreen(
                                        viewModel = profileViewModel,
                                        onBack = { currentScreen = Screen.DASHBOARD },
                                        onCustomerClick = { customer ->
                                            profileViewModel.selectCustomer(customer)
                                            currentScreen = Screen.CUSTOMER_TRANSACTIONS
                                        }
                                    )
                                }
                            }
                        }

                        // Loading indicator overlay
                        if (viewModel.loading.value) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    backgroundColor = Color.White,
                                    modifier = Modifier.size(120.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colors.primary,
                                            strokeWidth = 3.dp
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Loading...",
                                            style = MaterialTheme.typography.body2
                                        )
                                    }
                                }
                            }
        }

        // Barcode Edit Dialog
        if (showBarcodeEditDialog) {
            println("✏️ BARCODE EDIT DIALOG SHOWN")
            println("   - Barcode to edit: $barcodeToEdit")
            println("   - New barcode ID: $newBarcodeId")
            println("   - Timestamp: ${System.currentTimeMillis()}")

            androidx.compose.material.AlertDialog(
                onDismissRequest = {
                    println("✏️ BARCODE EDIT DIALOG DISMISSED")
                    println("   - Barcode: $barcodeToEdit")
                    showBarcodeEditDialog = false
                },
                title = {
                    Text(
                        "Edit Barcode ID",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text("Current barcode ID:")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = barcodeToEdit,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("New barcode ID:")
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = newBarcodeId,
                            onValueChange = { newBarcodeId = it },
                            placeholder = { Text("Enter new barcode ID") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colors.primary,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "⚠️ Please match the barcode ID on the product",
                            color = Color(0xFFFFA500),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            println("✏️ BARCODE EDIT CONFIRMED - Proceeding with update")
                            println("   - Old barcode: $barcodeToEdit")
                            println("   - New barcode: $newBarcodeId")
                            println("   - Timestamp: ${System.currentTimeMillis()}")
                            
                            if (newBarcodeId.isNotBlank() && newBarcodeId != barcodeToEdit) {
                                // TODO: Implement barcode update logic
                                coroutineScope.launch {
                                    try {
                                        // Find the inventory item with the old barcode
                                        val inventoryItem = viewModel.getInventoryItemByBarcodeId(barcodeToEdit)
                                        if (inventoryItem != null) {
                                            // Update the barcode in the inventory item
                                            val updatedInventoryItem = inventoryItem.copy(barcodeId = newBarcodeId)
                                            val inventoryId = viewModel.updateInventoryItem(updatedInventoryItem)
                                            if (inventoryId != null) {
                                                showSnackbar("Barcode updated successfully")
                                                println("✅ Barcode updated successfully")
                                            } else {
                                                showSnackbar("Failed to update barcode")
                                                println("❌ Failed to update barcode")
                                            }
                                        } else {
                                            showSnackbar("Inventory item not found")
                                            println("❌ Inventory item not found for barcode: $barcodeToEdit")
                                        }
                                    } catch (e: Exception) {
                                        showSnackbar("Failed to update barcode: ${e.message}")
                                        println("❌ Failed to update barcode: ${e.message}")
                                    }
                                }
                            } else {
                                showSnackbar("Please enter a valid new barcode ID")
                                println("❌ Invalid new barcode ID")
                            }
                            
                            showBarcodeEditDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            println("✏️ BARCODE EDIT CANCELLED")
                            println("   - Barcode: $barcodeToEdit")
                            println("   - Timestamp: ${System.currentTimeMillis()}")
                            showBarcodeEditDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Barcode Delete Confirmation Dialog
        if (showBarcodeDeleteDialog) {
                            println("🗑️ DELETE CONFIRMATION DIALOG SHOWN")
                            println("   - Barcode to delete: $barcodeToDelete")
                            println("   - Timestamp: ${System.currentTimeMillis()}")
                            
                            androidx.compose.material.AlertDialog(
                                onDismissRequest = { 
                                    println("🗑️ DELETE CONFIRMATION DIALOG DISMISSED")
                                    println("   - Barcode: $barcodeToDelete")
                                    showBarcodeDeleteDialog = false 
                                },
                                title = {
                                    Text(
                                        "Delete Product",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Column {
                                        Text("Are you sure you want to delete the product with barcode:")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = barcodeToDelete,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colors.error
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "⚠️ Please match the barcode ID on the product",
                                            color = Color(0xFFFFA500),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            println("🗑️ DELETE CONFIRMED - Proceeding with deletion")
                                            println("   - Barcode to delete: $barcodeToDelete")
                                            println("   - Timestamp: ${System.currentTimeMillis()}")
                                            viewModel.deleteProductByBarcodeId(barcodeToDelete)
                                            showBarcodeDeleteDialog = false
                                            showSnackbar("Barcode removed successfully")
                                            println("   - Delete request sent to ViewModel")
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colors.error
                                        )
                                    ) {
                                        Text("Delete")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { 
                                            println("🗑️ DELETE CANCELLED")
                                            println("   - Barcode: $barcodeToDelete")
                                            println("   - Timestamp: ${System.currentTimeMillis()}")
                                            showBarcodeDeleteDialog = false 
                                        }
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        // Professional SnackBar
                        SnackBar(
                            visible = showSnackbar,
                            message = snackbarMessage,
                            actionLabel = snackbarAction,
                            onAction = snackbarActionCallback,
                            onDismiss = { showSnackbar = false }
                        )
                    }
                }
            }
        }
    }
}

enum class Screen {
    DASHBOARD,
    ADD_PRODUCT,
    EDIT_PRODUCT,
    PRODUCT_DETAIL,
    BARCODE_EDIT,
    SETTINGS,
    GOLD_RATES,
    CATEGORIES,
    BILLING,
    CUSTOMIZATION,
    APPOINTMENTS,
    PROFILE,
    CUSTOMER_TRANSACTIONS
}

@Composable
fun SnackBar(
    visible: Boolean,
    message: String,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short,
    onAction: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val backgroundGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFC9AD6E),
            Color(0xFFB8973D)
        )
    )
    val textColor = Color.White
    val actionButtonColor = Color(0xFF1A1A1A) // Dark button background
    val actionTextColor = Color.White
    val buttonShape = RoundedCornerShape(4.dp)

    val offsetY = animateDpAsState(
        targetValue = if (visible) 0.dp else 100.dp,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "snackbar_offset"
    )

    val alpha = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "snackbar_alpha"
    )

    // Auto-dismiss after duration
    LaunchedEffect(visible) {
        if (visible) {
            delay(when (duration) {
                SnackbarDuration.Short -> 4000L
                SnackbarDuration.Long -> 8000L
                SnackbarDuration.Indefinite -> Long.MAX_VALUE
            })
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .offset(y = offsetY.value)
                .alpha(alpha.value),
            shape = RoundedCornerShape(8.dp),
            elevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundGradient)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        color = textColor,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium
                    )

                    if (actionLabel != null && onAction != null) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Surface(
                            color = actionButtonColor,
                            shape = buttonShape,
                            elevation = 0.dp
                        ) {
                            TextButton(
                                onClick = {
                                    onAction()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = actionTextColor
                                )
                            ) {
                                Text(
                                    text = actionLabel.uppercase(),
                                    style = MaterialTheme.typography.button,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class SnackbarDuration {
    Short, Long, Indefinite
}

@Composable
fun EnhancedSidebar(
    isSidebarExpanded: Boolean,
    isSidebarVisible: Boolean,
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    onSidebarToggle: (Boolean) -> Unit,
    onSidebarVisibilityChange: (Boolean) -> Unit
) {
    if (isSidebarVisible) {
        val sidebarInteractionSource = remember { MutableInteractionSource() }
        val isSidebarHovered by sidebarInteractionSource.collectIsHoveredAsState()

        // Automatically expand sidebar on hover
        LaunchedEffect(isSidebarHovered) {
            if (isSidebarHovered && !isSidebarExpanded) {
                onSidebarToggle(true)
            }
        }

        // Animated width for smooth transitions
        val animatedWidth by animateDpAsState(
            targetValue = if (isSidebarExpanded) 280.dp else 72.dp,
            animationSpec = tween(durationMillis = 300, easing = EaseInOutQuart),
            label = "sidebar_width"
        )

        // Animated header height: compact when collapsed (to fit diamond), larger when expanded (to fit shop name)
        val headerHeight by animateDpAsState(
            targetValue = if (isSidebarExpanded) 150.dp else 64.dp,
            animationSpec = tween(durationMillis = 300, easing = EaseInOutQuart),
            label = "sidebar_header_height"
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(animatedWidth)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFC9AD6E), // Lighter gold at top
                            Color(0xFFB8973D)  // Slightly darker gold at bottom
                        )
                    )
                )
                .border(BorderStroke(1.dp, Color(0xFFB8973D)))
                .hoverable(sidebarInteractionSource)
        ) {
            // Main content container
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with brand info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFC9AD6E),
                                    Color(0xFFB8973D)
                                )
                            )
                        )
                ) {
                    if (!isSidebarExpanded) {
                        // Show diamond icon only when sidebar is collapsed - positioned to align with hamburger menu
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .align(Alignment.TopCenter)
                                .padding(top = 19.dp), // Center in 64dp top bar (32dp - 24dp icon size / 2 = 8dp)
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Diamond,
                                contentDescription = "Jewelry Logo",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Logo and brand information
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(horizontal = 8.dp)
                    ) {

                        // Brand name with animation when expanded
                        val brandTextAlpha by animateFloatAsState(
                            targetValue = if (isSidebarExpanded) 1f else 0f,
                            animationSpec = tween(
                                durationMillis = 400,
                                delayMillis = 200,
                                easing = FastOutSlowInEasing
                            ),
                            label = "brand_text_alpha"
                        )

                        if (isSidebarExpanded) {
                            Box(
                                modifier = Modifier.alpha(brandTextAlpha)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Brand name with multiple styles
                                    Text(
                                        "Gagan",
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        // Add shadow for better visibility
                                        style = TextStyle(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.3f),
                                                blurRadius = 4f
                                            )
                                        )
                                    )
                                    Text(
                                        "Jewellers",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.5.sp,
                                        style = TextStyle(
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.3f),
                                                blurRadius = 3f
                                            )
                                        )
                                    )
                                    Text(
                                        "Pvt Ltd",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal,
                                        letterSpacing = 0.25.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Navigation items
                NavigationItems(
                    currentScreen = currentScreen,
                    isSidebarExpanded = isSidebarExpanded,
                    onScreenChange = onScreenChange
                )

                Spacer(modifier = Modifier.weight(1f))

                // Footer with version info
                if (isSidebarExpanded) {
                    val alphaAnim by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = tween(300, delayMillis = 100),
                        label = "footer_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .alpha(alphaAnim)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Version 1.0",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationItems(
    currentScreen: Screen,
    isSidebarExpanded: Boolean,
    onScreenChange: (Screen) -> Unit
) {
    val navigationItems = listOf(
        NavigationItemData(
            icon = Icons.Default.AccountBox,
            title = "Dashboard",
            screen = Screen.DASHBOARD
        ),
        NavigationItemData(
            icon = Icons.Default.AddCircle,
            title = "Add Product",
            screen = Screen.ADD_PRODUCT
        ),
        NavigationItemData(
            icon = Icons.Default.MailOutline,
            title = "Billing",
            screen = Screen.BILLING
        ),
        NavigationItemData(
            icon = Icons.Default.Person,
            title = "Customer Profiles",
            screen = Screen.PROFILE
        ),
        NavigationItemData(
            icon = Icons.Default.Star,
            title = "Metal Rates",
            screen = Screen.GOLD_RATES
        ),
        NavigationItemData(
            icon = Icons.Default.AccountBox,
            title = "Categories",
            screen = Screen.CATEGORIES
        ),
        // NavigationItemData(
        //     icon = Icons.Default.Build,
        //     title = "Customization",
        //     screen = Screen.CUSTOMIZATION
        // ),
        NavigationItemData(
            icon = Icons.Default.Event,
            title = "Appointments",
            screen = Screen.APPOINTMENTS
        ),
        NavigationItemData(
            icon = Icons.Default.Settings,
            title = "Settings",
            screen = Screen.SETTINGS
        )
    )

    Column {
        navigationItems.forEach { item ->
            EnhancedNavigationItem(
                icon = item.icon,
                title = item.title,
                selected = currentScreen == item.screen,
                onClick = { onScreenChange(item.screen) },
                isExpanded = isSidebarExpanded
            )
        }
    }
}

@Composable
fun EnhancedNavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    isExpanded: Boolean
) {
    // Dark semi-transparent background for selected items
    val backgroundColor = if (selected) Color.White else Color.Transparent

    // Pure white text for maximum contrast
    val textColor = if (selected) Color.Black else Color.White

    // Icon colors - bright white for selected, slightly dimmer for unselected
    val iconColor = if (selected) Color.Black else Color.White.copy(alpha = 0.9f)

    val iconPadding by animateDpAsState(
        targetValue = if (isExpanded) 16.dp else 24.dp,
        animationSpec = tween(durationMillis = 300, easing = EaseInOutQuart),
        label = "icon_padding"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick),
    ) {
        // Fixed position icon container with subtle highlight
        Box(
            modifier = Modifier
                .size(56.dp)
                .padding(start = iconPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            // Icon with shadow for better visibility
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier
                    .size(26.dp) // Slightly larger icons
            )
        }

        // Animated text container
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(300, delayMillis = 150)) +
                    expandHorizontally(
                        animationSpec = tween(300, easing = EaseOutQuad),
                        expandFrom = Alignment.Start
                    ),
            exit = fadeOut(animationSpec = tween(200)) +
                    shrinkHorizontally(
                        animationSpec = tween(200, easing = EaseInQuad),
                        shrinkTowards = Alignment.Start
                    ),
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 56.dp)
                .align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    color = textColor,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 15.sp, // Slightly larger text
                    letterSpacing = 0.25.sp // Better readability
                )
            }
        }
    }
}

private data class NavigationItemData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val screen: Screen
)

@Composable
fun EnhancedTopAppBar(
    title: @Composable () -> Unit,
    isSidebarExpanded: Boolean,
    isSidebarVisible: Boolean,
    onToggleSidebar: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.primary,
    contentColor: Color = Color.White,
    elevation: Dp = 4.dp,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = backgroundColor,
        elevation = elevation,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp) // Slightly taller than standard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu icon to re-open sidebar
                if (!isSidebarExpanded || !isSidebarVisible) {
                    IconButton(onClick = onToggleSidebar) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Open sidebar",
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                // Just title - no logo
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        title()
                    }
                }

                // Right-aligned action buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions()

                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = contentColor
                        )
                    }
                }
            }
        }
    }
}