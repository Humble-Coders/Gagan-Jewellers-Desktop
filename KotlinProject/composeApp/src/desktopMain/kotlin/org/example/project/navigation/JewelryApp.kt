package org.example.project.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.lightColors
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.JewelryAppInitializer
import org.example.project.viewModels.ProductsViewModel
import org.example.project.ui.AddEditProductScreen
import org.example.project.ui.BillingScreen
import org.example.project.ui.DashboardScreen
import org.example.project.ui.ProductDetailScreen
import org.example.project.ui.SettingsScreen


@Composable
fun JewelryApp(viewModel: ProductsViewModel) {
    // State for navigation
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val imageLoader = JewelryAppInitializer.getImageLoader()
    val cartViewModel = JewelryAppInitializer.getCartViewModel()
    val customerViewModel = JewelryAppInitializer.getCustomerViewModel()
    val paymentViewModel = JewelryAppInitializer.getPaymentViewModel() // Add this line

    // Material Theme customization for jewelry store
    MaterialTheme(
        colors = lightColors(
            primary = Color(0xFFC9AD6E),        // Gold
            primaryVariant = Color(0xFFB8973D),  // Darker gold
            secondary = Color(0xFF7D7D7D),      // Silver
            background = Color(0xFFF9F9F9),     // Light background
            surface = Color.White
        )
    ) {
        // Use a Row layout to create a permanent drawer effect
        Row {
            // Sidebar navigation - fixed width, always visible
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(260.dp)  // Reduced drawer width
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFEEEEEE)))
            ) {
                // App logo header - reduced height
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)  // Reduced from 150dp
                        .background(MaterialTheme.colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // App logo placeholder
                        Box(
                            modifier = Modifier
                                .size(48.dp)  // Reduced from 64dp
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)  // Reduced from 40dp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))  // Reduced spacing
                        Text(
                            "Jewelry Inventory",
                            color = Color.White,
                            fontSize = 16.sp,  // Reduced from 18sp
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Navigation items
                NavigationItem(
                    icon = Icons.Default.AccountBox,
                    title = "Dashboard",
                    selected = currentScreen == Screen.DASHBOARD,
                    onClick = { currentScreen = Screen.DASHBOARD }
                )

                NavigationItem(
                    icon = Icons.Default.AddCircle,
                    title = "Add Product",
                    selected = currentScreen == Screen.ADD_PRODUCT,
                    onClick = {
                        viewModel.createNewProduct()
                        currentScreen = Screen.ADD_PRODUCT
                    }
                )

                NavigationItem(
                    icon = Icons.Default.MailOutline,
                    title = "Billing",
                    selected = currentScreen == Screen.BILLING,
                    onClick = {
                        // Dismiss any active snackbars when switching to billing
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                        }
                        currentScreen = Screen.BILLING
                    }
                )

                NavigationItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    selected = currentScreen == Screen.SETTINGS,
                    onClick = { currentScreen = Screen.SETTINGS }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Version info
                Text(
                    "Version 1.0",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // Main content area
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar - reduced height and font size
                TopAppBar(
                    title = {
                        Text(
                            when (currentScreen) {
                                Screen.DASHBOARD -> "Dashboard"
                                Screen.ADD_PRODUCT -> "Add Product"
                                Screen.EDIT_PRODUCT -> "Edit Product"
                                Screen.PRODUCT_DETAIL -> "Product Details"
                                Screen.SETTINGS -> "Settings"
                                Screen.BILLING -> "Billing"
                            },
                            fontSize = 16.sp  // Reduced from default
                        )
                    },
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = Color.White,
                    elevation = 2.dp,
                    modifier = Modifier.height(48.dp),  // Reduced from default 56dp
                    actions = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                viewModel.loadProducts()
                                snackbarHostState.showSnackbar("Data refreshed")
                            }
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.size(20.dp)  // Reduced icon size
                            )
                        }
                    }
                )

                // Content with SnackbarHost
                Box(modifier = Modifier.fillMaxSize()) {
                    // Screen content based on navigation
                    when (currentScreen) {
                        Screen.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onAddProduct = {
                                viewModel.createNewProduct()
                                currentScreen = Screen.ADD_PRODUCT
                            },
                            onViewProductDetails = { productId ->
                                viewModel.selectProduct(productId)
                                currentScreen = Screen.PRODUCT_DETAIL
                            }
                        )

                        Screen.ADD_PRODUCT -> AddEditProductScreen(
                            viewModel = viewModel,
                            onSave = {
                                currentScreen = Screen.DASHBOARD
                            },
                            onCancel = {
                                viewModel.clearCurrentProduct()
                                currentScreen = Screen.DASHBOARD
                            }
                        )

                        Screen.EDIT_PRODUCT -> AddEditProductScreen(
                            viewModel = viewModel,
                            onSave = {
                                currentScreen = Screen.DASHBOARD
                            },
                            onCancel = {
                                viewModel.clearCurrentProduct()
                                currentScreen = Screen.DASHBOARD
                            },
                            isEditing = true
                        )

                        Screen.PRODUCT_DETAIL -> ProductDetailScreen(
                            viewModel = viewModel,
                            imageLoader = imageLoader,
                            onEdit = {
                                currentScreen = Screen.EDIT_PRODUCT
                            },
                            onBack = {
                                currentScreen = Screen.DASHBOARD
                            }
                        )

                        Screen.SETTINGS -> SettingsScreen()

                        Screen.BILLING -> BillingScreen(
                            customerViewModel = customerViewModel,
                            cartViewModel = cartViewModel,
                            productsViewModel = viewModel,
                            imageLoader = imageLoader,
                            paymentViewModel = paymentViewModel
                        )
                    }

                    // Error message display
                    viewModel.error.value?.let { errorMessage ->
                        if (errorMessage.isNotEmpty()) {
                            LaunchedEffect(errorMessage) {
                                snackbarHostState.showSnackbar(
                                    message = errorMessage,
                                    actionLabel = "Dismiss"
                                )
                            }
                        }
                    }

                    // Loading indicator
                    if (viewModel.loading.value) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    // Snackbar host
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

// Navigation item composable - reduced height
@Composable
fun NavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) Color(0xFFEFEFEF) else Color.Transparent
    val textColor = if (selected) MaterialTheme.colors.primary else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)  // Reduced from 56dp
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = textColor,
            modifier = Modifier.size(20.dp)  // Reduced icon size
        )
        Spacer(modifier = Modifier.width(12.dp))  // Reduced spacing
        Text(
            text = title,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp  // Reduced font size
        )
    }
}

// Screen enum for navigation - no changes needed
enum class Screen {
    DASHBOARD,
    ADD_PRODUCT,
    EDIT_PRODUCT,
    PRODUCT_DETAIL,
    SETTINGS,
    BILLING
}