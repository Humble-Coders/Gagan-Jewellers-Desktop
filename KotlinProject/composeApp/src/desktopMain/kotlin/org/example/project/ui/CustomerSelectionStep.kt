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
            .padding(16.dp)
    ) {
        // Error message if any
        error?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                    .padding(8.dp)
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
                placeholder = { Text("Search customers...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = { showAddCustomerDialog = true },
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Customer")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                Text("No customers found. Add a new customer to get started.")
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
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onContinue,
            enabled = selectedCustomer != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue with ${selectedCustomer?.name ?: "Selected Customer"}")
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle with initials
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = customer.name.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Customer details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = customer.name,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = customer.email,
                fontSize = 14.sp,
                color = Color.Gray
            )
            if (customer.phone.isNotEmpty()) {
                Text(
                    text = customer.phone,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // Selected indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
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

    // Validation states
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Customer") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isEmpty()
                    },
                    label = { Text("Full Name") },
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (nameError) {
                    Text(
                        "Name is required",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = !isValidEmail(it)
                    },
                    label = { Text("Email") },
                    isError = emailError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (emailError) {
                    Text(
                        "Please enter a valid email",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                // Phone field (optional)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Address field (optional)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
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
                            address = address
                        )
                        onAddCustomer(customer)
                    }
                }
            ) {
                Text("Add Customer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Email validation helper
fun isValidEmail(email: String): Boolean {
    return email.isNotEmpty() 
}