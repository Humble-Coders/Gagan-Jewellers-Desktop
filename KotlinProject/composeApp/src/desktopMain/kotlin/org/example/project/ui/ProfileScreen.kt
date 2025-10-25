package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.User
import org.example.project.viewModels.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onCustomerClick: (User) -> Unit
) {
    val customers by viewModel.filteredCustomers
    val searchQuery by viewModel.searchQuery
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                        Column {
                            Text(
                                text = "👥 Customer Profiles",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                text = "Manage and view customer information",
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF8E1),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "${customers.size} customers",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 14.sp,
                            color = Color(0xFFB8973D),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Search Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                backgroundColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::searchCustomers,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    placeholder = {
                        Text(
                            "Search customers by name, email, or phone...",
                            color = Color(0xFF9CA3AF),
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFFB8973D),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFB8973D),
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = Color(0xFFB8973D),
                        backgroundColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        Text(
                            text = "⚠️",
                            fontSize = 20.sp
                        )
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                if (customers.isEmpty()) {
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
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "No customers",
                                            modifier = Modifier.size(40.dp),
                                            tint = Color(0xFF9CA3AF)
                                        )
                                    }
                                }
                                Text(
                                    text = if (searchQuery.isBlank()) "No customers found" else "No customers match your search",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4B5563),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (searchQuery.isBlank()) "Start adding customers to see them here" else "Try a different search term",
                                    fontSize = 14.sp,
                                    color = Color(0xFF9CA3AF),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(customers) { customer ->
                            CustomerCard(
                                customer = customer,
                                onClick = { onCustomerClick(customer) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerCard(
    customer: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar
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

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = customer.name.ifBlank { "Unnamed Customer" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )

                    if (customer.email.isNotBlank()) {
                        Text(
                            text = customer.email,
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }

                    if (customer.phone.isNotBlank()) {
                        Text(
                            text = customer.phone,
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }

                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Text(
                        text = "Member since ${dateFormat.format(Date(customer.createdAt))}",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }

            // Balance display
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Balance",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (customer.balance >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = "₹${String.format("%.2f", customer.balance)}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (customer.balance >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}