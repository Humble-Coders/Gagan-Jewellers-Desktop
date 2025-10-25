package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.Order
import org.example.project.data.PaymentStatus
import org.example.project.data.OrderStatus
import org.example.project.viewModels.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomerTransactionsScreen(
    customer: org.example.project.data.User,
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val orders by viewModel.customerOrders
    val loading by viewModel.loading
    val error by viewModel.error

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 0.dp,
                backgroundColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFFB8973D),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFFF8E1),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = customer.name.firstOrNull()?.uppercase() ?: "?",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB8973D)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = customer.name.ifBlank { "Unnamed Customer" },
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A1A1A)
                                    )
                                    Text(
                                        text = "Transaction History",
                                        fontSize = 14.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Current Balance",
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF),
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (customer.balance >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = "₹${String.format("%.2f", customer.balance)}",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (customer.balance >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }

                    Divider(color = Color(0xFFE5E7EB))

                    // Customer Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (customer.email.isNotBlank() || customer.phone.isNotBlank()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (customer.email.isNotBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "📧", fontSize = 16.sp)
                                        Text(
                                            text = customer.email,
                                            fontSize = 14.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                }
                                if (customer.phone.isNotBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "📱", fontSize = 16.sp)
                                        Text(
                                            text = customer.phone,
                                            fontSize = 14.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "📅", fontSize = 16.sp)
                                Text(
                                    text = "Joined ${dateFormat.format(Date(customer.createdAt))}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error message
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFFEE2E2),
                    elevation = 0.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "⚠️", fontSize = 20.sp)
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Content
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFB8973D),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                if (orders.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        elevation = 0.dp,
                        backgroundColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFF3F4F6),
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Receipt,
                                            contentDescription = "No transactions",
                                            modifier = Modifier.size(40.dp),
                                            tint = Color(0xFF9CA3AF)
                                        )
                                    }
                                }
                                Text(
                                    text = "No transactions found",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4B5563),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "This customer hasn't made any purchases yet",
                                    fontSize = 14.sp,
                                    color = Color(0xFF9CA3AF),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📜 Transactions",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF8E1)
                            ) {
                                Text(
                                    text = "${orders.size} orders",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 13.sp,
                                    color = Color(0xFFB8973D),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(orders) { order ->
                                TransactionCard(order = order)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Order #${order.orderId}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )

                    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                    Text(
                        text = dateFormat.format(Date(order.createdAt)),
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )

                    Text(
                        text = "${order.items.size} items",
                        fontSize = 13.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF8E1)
                    ) {
                        Text(
                            text = "₹${String.format("%.2f", order.finalAmount)}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB8973D)
                        )
                    }

                    if (order.paymentSplit != null) {
                        val dueAmount = order.paymentSplit.dueAmount
                        if (dueAmount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = "Due: ₹${String.format("%.2f", dueAmount)}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Status badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatusChip(
                    text = order.status.name,
                    color = when (order.status) {
                        OrderStatus.CONFIRMED -> Color(0xFF3B82F6)
                        OrderStatus.PROCESSING -> Color(0xFFF59E0B)
                        OrderStatus.SHIPPED -> Color(0xFFA855F7)
                        OrderStatus.DELIVERED -> Color(0xFF10B981)
                        OrderStatus.CANCELLED -> Color(0xFFEF4444)
                    }
                )

                StatusChip(
                    text = order.paymentStatus.name,
                    color = when (order.paymentStatus) {
                        PaymentStatus.PENDING -> Color(0xFFF59E0B)
                        PaymentStatus.PROCESSING -> Color(0xFF3B82F6)
                        PaymentStatus.COMPLETED -> Color(0xFF10B981)
                        PaymentStatus.FAILED -> Color(0xFFEF4444)
                        PaymentStatus.CANCELLED -> Color(0xFF6B7280)
                        PaymentStatus.REFUNDED -> Color(0xFFA855F7)
                    }
                )
            }

            // Notes if available
            if (order.notes.isNotBlank()) {
                Divider(color = Color(0xFFE5E7EB))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "📝", fontSize = 16.sp)
                    Text(
                        text = order.notes,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}