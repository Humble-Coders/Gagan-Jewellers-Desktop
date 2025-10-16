package org.example.project.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
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
import org.example.project.viewModels.CustomizationViewModel
import org.example.project.data.FirestoreCustomizationRepository
import org.example.project.ui.AddEditProductScreen
import org.example.project.ui.BillingScreen
import org.example.project.ui.DashboardScreen
import org.example.project.ui.ProductDetailScreen
import org.example.project.ui.SettingsScreen
import org.example.project.ui.GoldRateScreen
import org.example.project.ui.CategoryManagementScreen
import org.example.project.ui.InvoiceConfigScreen
import org.example.project.ui.CustomizationScreen

@Composable
fun JewelryApp(viewModel: ProductsViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var isSidebarExpanded by remember { mutableStateOf(true) }
    var isSidebarVisible by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val imageLoader = JewelryAppInitializer.getImageLoader()
    val cartViewModel = JewelryAppInitializer.getCartViewModel()
    val customerViewModel = JewelryAppInitializer.getCustomerViewModel()
    val paymentViewModel = JewelryAppInitializer.getPaymentViewModel()
    val customizationViewModel = CustomizationViewModel(
        FirestoreCustomizationRepository(JewelryAppInitializer.getFirestore())
    )

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
                // Sidebar with hover detection and smooth animations
                if (isSidebarVisible) {
                    val sidebarInteractionSource = remember { MutableInteractionSource() }
                    val isSidebarHovered by sidebarInteractionSource.collectIsHoveredAsState()

                    // Automatically expand sidebar on hover
                    LaunchedEffect(isSidebarHovered) {
                        if (isSidebarHovered && !isSidebarExpanded) {
                            isSidebarExpanded = true
                        }
                    }

                    // Animated width for smooth transitions
                    val animatedWidth by animateDpAsState(
                        targetValue = if (isSidebarExpanded) 280.dp else 72.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "sidebar_width"
                    )

                    // Animated header height for smooth transitions
                    val animatedHeaderHeight by animateDpAsState(
                        targetValue = if (isSidebarExpanded) 150.dp else 150.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "header_height"
                    )

                    // Animated icon size
                    val animatedIconSize by animateDpAsState(
                        targetValue = if (isSidebarExpanded) 64.dp else 40.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "icon_size"
                    )

                    val animatedLogoIconSize by animateDpAsState(
                        targetValue = if (isSidebarExpanded) 40.dp else 24.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "logo_icon_size"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(animatedWidth)
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFEEEEEE)))
                            .hoverable(sidebarInteractionSource)
                    ) {
                        // Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(animatedHeaderHeight)
                                .background(MaterialTheme.colors.primary)
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isSidebarExpanded,
                                enter = fadeIn(
                                    animationSpec = tween(300)
                                ),
                                exit = fadeOut(
                                    animationSpec = tween(200)
                                ),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                IconButton(
                                    onClick = { isSidebarExpanded = false }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Collapse sidebar",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(animatedIconSize)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Build,
                                        contentDescription = "Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(animatedLogoIconSize)
                                    )
                                }
                                AnimatedVisibility(
                                    visible = isSidebarExpanded,
                                    enter = fadeIn(
                                        animationSpec = tween(300, delayMillis = 100)
                                    ) + expandHorizontally(
                                        animationSpec = tween(300)
                                    ),
                                    exit = fadeOut(
                                        animationSpec = tween(200)
                                    ) + shrinkHorizontally(
                                        animationSpec = tween(200)
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Jewelry Inventory",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        NavigationItem(
                            icon = Icons.Default.AccountBox,
                            title = "Dashboard",
                            selected = currentScreen == Screen.DASHBOARD,
                            onClick = { currentScreen = Screen.DASHBOARD },
                            isExpanded = isSidebarExpanded
                        )

                        NavigationItem(
                            icon = Icons.Default.AddCircle,
                            title = "Add Product",
                            selected = currentScreen == Screen.ADD_PRODUCT,
                            onClick = {
                                viewModel.createNewProduct()
                                currentScreen = Screen.ADD_PRODUCT
                            },
                            isExpanded = isSidebarExpanded
                        )

                        NavigationItem(
                            icon = Icons.Default.MailOutline,
                            title = "Billing",
                            selected = currentScreen == Screen.BILLING,
                            onClick = {
                                currentScreen = Screen.BILLING
                            },
                            isExpanded = isSidebarExpanded
                        )

                        NavigationItem(
                            icon = Icons.Default.AccountBox,
                            title = "Gold Rates",
                            selected = currentScreen == Screen.GOLD_RATES,
                            onClick = { currentScreen = Screen.GOLD_RATES },
                            isExpanded = isSidebarExpanded
                        )

                        NavigationItem(
                            icon = Icons.Default.AccountBox,
                            title = "Categories",
                            selected = currentScreen == Screen.CATEGORIES,
                            onClick = { currentScreen = Screen.CATEGORIES },
                            isExpanded = isSidebarExpanded
                        )

                        NavigationItem(
                            icon = Icons.Default.AccountBox,
                            title = "Invoice Config",
                            selected = currentScreen == Screen.INVOICE_CONFIG,
                            onClick = { currentScreen = Screen.INVOICE_CONFIG },
                            isExpanded = isSidebarExpanded
                        )

                        NavigationItem(
                            icon = Icons.Default.Build,
                            title = "Customization",
                            selected = currentScreen == Screen.CUSTOMIZATION,
                            onClick = { currentScreen = Screen.CUSTOMIZATION },
                            isExpanded = isSidebarExpanded
                        )

                        NavigationItem(
                            icon = Icons.Default.Settings,
                            title = "Settings",
                            selected = currentScreen == Screen.SETTINGS,
                            onClick = { currentScreen = Screen.SETTINGS },
                            isExpanded = isSidebarExpanded
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        AnimatedVisibility(
                            visible = isSidebarExpanded,
                            enter = fadeIn(
                                animationSpec = tween(300, delayMillis = 100)
                            ),
                            exit = fadeOut(
                                animationSpec = tween(200)
                            )
                        ) {
                            Text(
                                "Version 1.0",
                                modifier = Modifier.padding(16.dp),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

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
                    TopAppBar(
                        title = { Text("Jewelry Inventory Management") },
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White,
                        navigationIcon = if (!isSidebarExpanded) {
                            {
                                IconButton(onClick = {
                                    isSidebarVisible = true
                                    isSidebarExpanded = true
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open sidebar")
                                }
                            }
                        } else null,
                        actions = {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    viewModel.loadProducts()
                                }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
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
                                onSave = { currentScreen = Screen.DASHBOARD },
                                onCancel = {
                                    viewModel.clearCurrentProduct()
                                    currentScreen = Screen.DASHBOARD
                                }
                            )

                            Screen.EDIT_PRODUCT -> AddEditProductScreen(
                                viewModel = viewModel,
                                onSave = { currentScreen = Screen.DASHBOARD },
                                onCancel = {
                                    viewModel.clearCurrentProduct()
                                    currentScreen = Screen.DASHBOARD
                                },
                                isEditing = true
                            )

                            Screen.PRODUCT_DETAIL -> ProductDetailScreen(
                                viewModel = viewModel,
                                imageLoader = imageLoader,
                                onEdit = { currentScreen = Screen.EDIT_PRODUCT },
                                onBack = { currentScreen = Screen.DASHBOARD }
                            )

                            Screen.SETTINGS -> SettingsScreen()

                            Screen.GOLD_RATES -> GoldRateScreen(
                                onBack = { currentScreen = Screen.DASHBOARD }
                            )

                            Screen.CATEGORIES -> CategoryManagementScreen(
                                productsViewModel = viewModel,
                                onBack = { currentScreen = Screen.DASHBOARD }
                            )

                            Screen.INVOICE_CONFIG -> InvoiceConfigScreen(
                                onBack = { currentScreen = Screen.DASHBOARD }
                            )

                            Screen.CUSTOMIZATION -> CustomizationScreen(
                                viewModel = customizationViewModel,
                                onBack = { currentScreen = Screen.DASHBOARD }
                            )

                            Screen.BILLING -> BillingScreen(
                                customerViewModel = customerViewModel,
                                cartViewModel = cartViewModel,
                                productsViewModel = viewModel,
                                imageLoader = imageLoader,
                                paymentViewModel = paymentViewModel
                            )
                        }

                        if (viewModel.loading.value) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    isExpanded: Boolean
) {
    val backgroundColor = if (selected) Color(0xFFEFEFEF) else Color.Transparent
    val textColor = if (selected) MaterialTheme.colors.primary else Color.Black

    // Consistent height container for both expanded and collapsed states
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        if (isExpanded) {
            // Full navigation item with text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    color = textColor,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        } else {
            // Collapsed navigation item with only icon centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

enum class Screen {
    DASHBOARD,
    ADD_PRODUCT,
    EDIT_PRODUCT,
    PRODUCT_DETAIL,
    SETTINGS,
    GOLD_RATES,
    CATEGORIES,
    INVOICE_CONFIG,
    BILLING,
    CUSTOMIZATION
}