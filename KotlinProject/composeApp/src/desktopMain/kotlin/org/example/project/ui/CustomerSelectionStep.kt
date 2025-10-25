// CustomerSelectionScreen.kt
package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.User
import org.example.project.viewModels.CustomerViewModel

@Composable
fun CustomerSelectionStep(
    viewModel: CustomerViewModel,
    onCustomerSelected: (User) -> Unit,
    onContinue: () -> Unit
) {
    val customers by viewModel.customers
    val selectedCustomer by viewModel.selectedCustomer
    val isLoading by viewModel.loading
    val error by viewModel.error
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)  // Reduced padding
    ) {
        // Error message if any
        error?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)  // Reduced padding
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                    .padding(6.dp)  // Reduced padding
            )
        }

        // Search and Add Customer Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search customers...",
//                        fontSize = 13.sp  // Reduced font size
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .weight(1f),
//                    .height(42.dp),  // Reduced height
                singleLine = true,
                //textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)  // Reduced text size
            )

            Spacer(modifier = Modifier.width(12.dp))  // Reduced spacing

            Button(
                onClick = { showAddCustomerDialog = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                modifier = Modifier.height(42.dp)  // Match text field height
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Customer",
                    modifier = Modifier.size(16.dp)  // Reduced icon size
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "New Customer",
                    fontSize = 12.sp  // Reduced font size
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))  // Reduced spacing

        // Customer List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (customers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No customers found. Add a new customer to get started.",
                    fontSize = 14.sp  // Reduced font size
                )
            }
        } else {
            val filteredCustomers = if (searchQuery.isEmpty()) {
                customers
            } else {
                customers.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.email.contains(searchQuery, ignoreCase = true) ||
                            it.phone.contains(searchQuery, ignoreCase = true)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                elevation = 4.dp
            ) {
                LazyColumn {
                    items(filteredCustomers) { customer ->
                        CustomerListItem(
                            customer = customer,
                            isSelected = selectedCustomer?.id == customer.id,
                            onClick = {
                                viewModel.selectCustomer(customer)
                                onCustomerSelected(customer)
                            }
                        )
                        Divider()
                    }
                }
            }
        }

        // Continue button at bottom
        Spacer(modifier = Modifier.height(12.dp))  // Reduced spacing
        Button(
            onClick = onContinue,
            enabled = selectedCustomer != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Continue with ${selectedCustomer?.name ?: "Selected Customer"}",
                fontSize = 14.sp  // Reduced font size
            )
        }
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onAddCustomer = { customer ->
                viewModel.addCustomer(customer)
                showAddCustomerDialog = false
            },
            onDismiss = { showAddCustomerDialog = false }
        )
    }
}

@Composable
fun CustomerListItem(
    customer: User,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) Color(0xFFE3F2FD) else Color.Transparent)
            .padding(12.dp),  // Reduced padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle with initials
        Box(
            modifier = Modifier
                .size(36.dp)  // Reduced size
                .clip(CircleShape)
                .background(MaterialTheme.colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = customer.name.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp  // Reduced font size
            )
        }

        Spacer(modifier = Modifier.width(12.dp))  // Reduced spacing

        // Customer details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = customer.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp  // Reduced font size
            )
            Text(
                text = customer.email,
                fontSize = 12.sp,  // Reduced font size
                color = Color.Gray
            )
            if (customer.phone.isNotEmpty()) {
                Text(
                    text = customer.phone,
                    fontSize = 12.sp,  // Reduced font size
                    color = Color.Gray
                )
            }
        }

        // Selected indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)  // Reduced size
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)  // Reduced icon size
                )
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    onAddCustomer: (User) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Validation states
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add New Customer",
                fontSize = 16.sp  // Reduced font size
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),  // Reduced padding
                verticalArrangement = Arrangement.spacedBy(12.dp)  // Reduced spacing
            ) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isEmpty()
                    },
                    label = {
                        Text(
                            "Full Name",
                            fontSize = 12.sp  // Reduced font size
                        )
                    },
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)  // Reduced text size
                )
                if (nameError) {
                    Text(
                        "Name is required",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 12.dp),  // Reduced padding
                        fontSize = 11.sp  // Reduced font size
                    )
                }

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = !isValidEmail(it)
                    },
                    label = {
                        Text(
                            "Email",
                            fontSize = 12.sp  // Reduced font size
                        )
                    },
                    isError = emailError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)  // Reduced text size
                )
                if (emailError) {
                    Text(
                        "Please enter a valid email",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 12.dp),  // Reduced padding
                        fontSize = 11.sp  // Reduced font size
                    )
                }

                // Phone field (optional)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = {
                        Text(
                            "Phone Number (Optional)",
                            fontSize = 12.sp  // Reduced font size
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)  // Reduced text size
                )

                // Address field (optional)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = {
                        Text(
                            "Address (Optional)",
                            fontSize = 12.sp  // Reduced font size
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)  // Reduced text size
                )

                // Notes field (optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = {
                        Text(
                            "Notes (Optional)",
                            fontSize = 12.sp  // Reduced font size
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)  // Reduced text size
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameError = name.isEmpty()
                    emailError = !isValidEmail(email)

                    if (!nameError && !emailError) {
                        val customer = User(
                            email = email,
                            name = name,
                            phone = phone,
                            address = address,
                            notes = notes,
                            balance = 0.0, // Initialize balance to 0
                            customerId = "" // Will be set by the repository
                        )
                        onAddCustomer(customer)
                    }
                }
            ) {
                Text(
                    "Add Customer",
                    fontSize = 12.sp  // Reduced font size
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    fontSize = 12.sp  // Reduced font size
                )
            }
        }
    )
}

// Email validation helper
fun isValidEmail(email: String): Boolean {
    return email.isNotEmpty() && email.contains("@") && email.contains(".")
}