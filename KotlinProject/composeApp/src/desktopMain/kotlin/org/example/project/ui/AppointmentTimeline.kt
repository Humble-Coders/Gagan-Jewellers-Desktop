package org.example.project.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.cloud.firestore.QuerySnapshot
import com.google.cloud.firestore.DocumentSnapshot
import org.example.project.data.AppointmentWithUser
import org.example.project.data.BookingStatus
import org.example.project.data.User
import org.example.project.data.Product

import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppointmentTimeline(
    appointments: List<AppointmentWithUser>,
    onStatusUpdate: (String, BookingStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    if (appointments.isEmpty()) {
        EmptyAppointmentsView(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(appointments.sortedBy { it.booking.startTime }) { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    onStatusUpdate = onStatusUpdate
                )
            }
        }
    }
}

@Composable
private fun EmptyAppointmentsView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.EventAvailable,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Appointments",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "No appointments scheduled for this date",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: AppointmentWithUser,
    onStatusUpdate: (String, BookingStatus) -> Unit
) {
    var isStatusExpanded by remember { mutableStateOf(false) }
    var showWishlistDialog by remember { mutableStateOf(false) }

    val statusColor = getStatusColor(appointment.booking.status)
    val statusIcon = getStatusIcon(appointment.booking.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeline indicator
                TimelineIndicator(
                    statusColor = statusColor,
                    statusIcon = statusIcon
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Appointment content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                     // Time and expandable status with overlay
                     Box {
                         Row(
                             modifier = Modifier.fillMaxWidth(),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.Top
                         ) {
                             Text(
                                 text = formatTimeRange(appointment.booking.startTime, appointment.booking.endTime),
                                 style = MaterialTheme.typography.subtitle1,
                                 fontWeight = FontWeight.Bold,
                                 color = MaterialTheme.colors.onSurface
                             )

                             ExpandableStatusChip(
                                 currentStatus = appointment.booking.status,
                                 isExpanded = isStatusExpanded,
                                 onToggle = { isStatusExpanded = !isStatusExpanded },
                                 onStatusSelected = { newStatus ->
                                     onStatusUpdate(appointment.booking.id, newStatus)
                                 }
                             )
                         }
                         
                         // Status dropdown overlay
                         if (isStatusExpanded) {
                             StatusDropdownOverlay(
                                 currentStatus = appointment.booking.status,
                                 onStatusSelected = { newStatus ->
                                     onStatusUpdate(appointment.booking.id, newStatus)
                                     isStatusExpanded = false
                                 },
                                 onDismiss = { isStatusExpanded = false }
                             )
                         }
                     }

                    // Rest of the card content...
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomerInfo(user = appointment.user, userId = appointment.booking.userId)

                    if (appointment.booking.serviceType.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ServiceInfo(serviceType = appointment.booking.serviceType)
                    }

                    if (appointment.booking.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        NotesSection(notes = appointment.booking.notes)
                    }

                    if (appointment.booking.wishlistProductIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        WishlistSection(productIds = appointment.booking.wishlistProductIds)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (appointment.user != null) {
                        OutlinedButton(
                            onClick = { showWishlistDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE91E63)
                            ),
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.3f))
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Wishlist",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    AppointmentActions(
                        status = appointment.booking.status,
                        onConfirm = { onStatusUpdate(appointment.booking.id, BookingStatus.CONFIRMED) },
                        onCancel = { onStatusUpdate(appointment.booking.id, BookingStatus.CANCELLED) },
                        onComplete = { onStatusUpdate(appointment.booking.id, BookingStatus.COMPLETED) }
                    )
                }
            }
        }

        if (showWishlistDialog && appointment.user != null) {
            WishlistDialog(
                user = appointment.user,
                onDismiss = { showWishlistDialog = false }
            )
        }
    }
}

@Composable
private fun TimelineIndicator(
    statusColor: Color,
    statusIcon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.1f))
                .border(
                    width = 2.dp,
                    color = statusColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Timeline line (hidden for last item)
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(20.dp)
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
        )
    }
}



@Composable
private fun CustomerInfo(
    user: org.example.project.data.User?,
    userId: String
) {
    if (user != null) {
        Column {
            Text(
                text = user.name.ifEmpty { "Unknown Customer" },
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface
            )
            
            if (user.phone.isNotEmpty()) {
                Text(
                    text = user.phone,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
            
            if (user.email.isNotEmpty()) {
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        Text(
            text = "Customer ID: $userId",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ServiceInfo(serviceType: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Build,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = serviceType,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun NotesSection(notes: String) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Notes,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = notes,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun WishlistSection(productIds: List<String>) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Wishlist: ${productIds.size} items",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun AppointmentActions(
    status: BookingStatus,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit
) {
    when (status) {
        BookingStatus.PENDING -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Confirm",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        BookingStatus.CONFIRMED -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onComplete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2196F3)
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFF2196F3))
                ) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Mark Complete",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        BookingStatus.CANCELLED, BookingStatus.NO_SHOW, BookingStatus.COMPLETED -> {
            // No action buttons for these statuses - use dropdown only
        }
    }
}

@Composable
private fun StatusUpdateDropdown(
    currentStatus: BookingStatus,
    onStatusSelected: (BookingStatus) -> Unit,
    onDismiss: () -> Unit
) {
    val allStatuses = BookingStatus.values().filter { it != currentStatus }
    
    Card(
        modifier = Modifier
            .width(180.dp)
            .wrapContentHeight()
            .clickable { }, // Prevent click through
        elevation = 16.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            allStatuses.forEach { status ->
                val (backgroundColor, textColor) = getStatusColors(status)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            onStatusSelected(status)
                            onDismiss()
                        }
                        .padding(vertical = 2.dp),
                    backgroundColor = backgroundColor,
                    shape = RoundedCornerShape(4.dp),
                    elevation = 0.dp
                ) {
                    Text(
                        text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: BookingStatus,
    onClick: () -> Unit
) {
    val (backgroundColor, textColor) = getStatusColors(status)

    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        backgroundColor = backgroundColor,
        elevation = 0.dp,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Change status",
                modifier = Modifier.size(12.dp),
                tint = textColor
            )
        }
    }
}

@Composable
private fun ExpandableStatusChip(
    currentStatus: BookingStatus,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onStatusSelected: (BookingStatus) -> Unit
) {
    val (currentBgColor, currentTextColor) = getStatusColors(currentStatus)

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    // Collapsed/Base Chip
    Card(
        modifier = Modifier.width(110.dp),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = currentBgColor,
        elevation = 0.dp,
        border = BorderStroke(1.dp, currentTextColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(currentTextColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentStatus.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = currentTextColor,
                    fontSize = 11.sp
                )
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Change status",
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = rotation },
                tint = currentTextColor
            )
        }
    }
}

@Composable
private fun StatusDropdownOverlay(
    currentStatus: BookingStatus,
    onStatusSelected: (BookingStatus) -> Unit,
    onDismiss: () -> Unit
) {
    val allStatuses = BookingStatus.values().filter { it != BookingStatus.NO_SHOW }
    
    Card(
        modifier = Modifier
            .width(110.dp)
            .offset(x = 0.dp, y = 36.dp)
            .zIndex(1000f),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(durationMillis = 300)
                )
                .padding(vertical = 6.dp, horizontal = 6.dp)
        ) {
            allStatuses.filter { it != currentStatus }.forEach { status ->
                val (_, txtColor) = getStatusColors(status)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onStatusSelected(status)
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(txtColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun formatTimeRange(startTime: Long, endTime: Long): String {
    val startDate = Date(startTime)
    val endDate = Date(endTime)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    return "${timeFormat.format(startDate)} - ${timeFormat.format(endDate)}"
}

private fun getStatusColor(status: BookingStatus): Color {
    return when (status) {
        BookingStatus.PENDING -> Color(0xFF9C27B0) // Purple
        BookingStatus.CONFIRMED -> Color(0xFF4CAF50) // Green
        BookingStatus.COMPLETED -> Color(0xFF2196F3) // Blue
        BookingStatus.CANCELLED -> Color(0xFFF44336) // Red
        BookingStatus.NO_SHOW -> Color(0xFFFF9800) // Orange
    }
}

private fun getStatusColors(status: BookingStatus): Pair<Color, Color> {
    val statusColor = getStatusColor(status)
    return Pair(
        statusColor.copy(alpha = 0.1f),
        statusColor
    )
}

private fun getStatusIcon(status: BookingStatus): ImageVector {
    return when (status) {
        BookingStatus.PENDING -> Icons.Default.Schedule
        BookingStatus.CONFIRMED -> Icons.Default.CheckCircle
        BookingStatus.COMPLETED -> Icons.Default.Done
        BookingStatus.CANCELLED -> Icons.Default.Cancel
        BookingStatus.NO_SHOW -> Icons.Default.Warning
    }
}

@Composable
fun WishlistDialog(
    user: User,
    onDismiss: () -> Unit
) {
    var wishlistProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user.id) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                val wishlistProductIds = fetchUserWishlist(user.id)
                println("📋 Found ${wishlistProductIds.size} items in wishlist for user ${user.id}")

                val products = mutableListOf<Product>()
                for (productId in wishlistProductIds) {
                    try {
                        val product = fetchProductDetails(productId)
                        if (product != null) {
                            products.add(product)
                        }
                    } catch (e: Exception) {
                        println("❌ Error fetching product $productId: ${e.message}")
                    }
                }

                wishlistProducts = products
                println("✅ Loaded ${products.size} wishlist products for ${user.name}")

            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load wishlist"
                println("❌ Error loading wishlist: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(650.dp)
                .height(600.dp),
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 8.dp,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFE91E63),
                                    Color(0xFFD81B60)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "${user.name}'s Wishlist",
                                    style = MaterialTheme.typography.h5,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Color.White.copy(alpha = 0.2f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${wishlistProducts.size} items",
                                        style = MaterialTheme.typography.caption,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(10.dp)
                                )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Content Section
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFE91E63),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = "Loading wishlist...",
                                        style = MaterialTheme.typography.body1,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        errorMessage != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(
                                                Color(0xFFF44336).copy(alpha = 0.1f),
                                                RoundedCornerShape(40.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            tint = Color(0xFFF44336),
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Error Loading Wishlist",
                                        style = MaterialTheme.typography.h6,
                                        color = MaterialTheme.colors.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = errorMessage ?: "Unknown error",
                                        style = MaterialTheme.typography.body2,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                        wishlistProducts.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .background(
                                                Color(0xFFE91E63).copy(alpha = 0.1f),
                                                RoundedCornerShape(50.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = Color(0xFFE91E63),
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "No Items Yet",
                                        style = MaterialTheme.typography.h5,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "This wishlist is empty. Products added to\nthe wishlist will appear here.",
                                        style = MaterialTheme.typography.body1,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(wishlistProducts) { product ->
                                    WishlistProductItem(product = product)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WishlistProductItem(product: Product) {
    var productImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoadingImage by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val imageLoader = org.example.project.JewelryAppInitializer.getImageLoader()

    LaunchedEffect(product.images) {
        if (product.images.isNotEmpty()) {
            isLoadingImage = true
            coroutineScope.launch {
                try {
                    val imageUrl = product.images.first()
                    println("🖼️ Loading wishlist product image: $imageUrl")

                    val imageBytes = imageLoader.loadImage(imageUrl)
                    if (imageBytes != null && imageBytes.isNotEmpty()) {
                        productImage = withContext(Dispatchers.IO) {
                            Image.makeFromEncoded(imageBytes).toComposeImageBitmap()
                        }
                        println("✅ Successfully loaded wishlist product image")
                    } else {
                        println("❌ Failed to load wishlist product image - no data")
                    }
                } catch (e: Exception) {
                    println("❌ Failed to decode wishlist product image: ${e.message}")
                } finally {
                    isLoadingImage = false
                }
            }
        } else {
            isLoadingImage = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            Card(
                modifier = Modifier.size(88.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 0.dp,
                backgroundColor = MaterialTheme.colors.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoadingImage -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFFE91E63),
                                strokeWidth = 2.dp
                            )
                        }
                        productImage != null -> {
                            androidx.compose.foundation.Image(
                                bitmap = productImage!!,
                                contentDescription = product.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        else -> {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = product.name,
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Product Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    if (product.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${String.format("%.2f", product.price)}",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE91E63)
                    )

                    if (product.weight.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Scale,
                                contentDescription = null,
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = product.weight,
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Wishlist Indicator
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Color(0xFFE91E63).copy(alpha = 0.1f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "In Wishlist",
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Helper functions for fetching data
private suspend fun fetchUserWishlist(userId: String): List<String> = withContext(Dispatchers.IO) {
    try {
        println("📋 Fetching wishlist for user $userId")
        val firestore = org.example.project.JewelryAppInitializer.getFirestore()
        val wishlistCollection = firestore.collection("users").document(userId).collection("wishlist")
        
        val future = wishlistCollection.get()
        val snapshot = future.get()
        
        val productIds = snapshot.documents.mapNotNull { doc ->
            doc.getString("productId")
        }
        
        println("📋 Found ${productIds.size} items in wishlist for user $userId")
        productIds
    } catch (e: Exception) {
        println("❌ Error fetching wishlist for user $userId: ${e.message}")
        emptyList()
    }
}

private suspend fun fetchProductDetails(productId: String): Product? = withContext(Dispatchers.IO) {
    try {
        println("📦 Fetching product details for $productId")
        val firestore = org.example.project.JewelryAppInitializer.getFirestore()
        
        val docRef = firestore.collection("products").document(productId)
        val future = docRef.get()
        val doc = future.get()
        
        if (doc.exists()) {
            val data = doc.data ?: return@withContext null
            
            val product = Product(
                id = doc.id,
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                images = (data["images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                categoryId = data["categoryId"] as? String ?: "",
                materialId = data["materialId"] as? String ?: "",
                weight = data["weight"] as? String ?: "",
                createdAt = (data["createdAt"] as? com.google.cloud.Timestamp)?.seconds?.times(1000)
                    ?: (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
            
            println("📦 Successfully loaded product: ${product.name}")
            product
        } else {
            println("❌ Product $productId does not exist")
            null
        }
    } catch (e: Exception) {
        println("❌ Error fetching product $productId: ${e.message}")
        null
    }
}
