package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.example.project.data.InvoiceConfig
import org.example.project.data.InvoiceField

@Composable
fun InvoiceConfigScreen(
    onBack: () -> Unit
) {
    var invoiceConfig by remember { mutableStateOf(InvoiceConfig()) }
    var showPreview by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Header
        InvoiceConfigHeader(
            onBack = onBack,
            onPreview = { showPreview = true },
            onSave = { /* Save configuration */ }
        )

        // Main Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Side - Configuration (60%)
            Column(
                modifier = Modifier.weight(0.6f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Company Information
                CompanyInfoCard(
                    config = invoiceConfig,
                    onConfigChange = { invoiceConfig = it }
                )

                // Invoice Fields Configuration
                InvoiceFieldsCard(
                    config = invoiceConfig,
                    onConfigChange = { invoiceConfig = it }
                )
            }

            // Right Side - Preview (40%)
            Column(
                modifier = Modifier.weight(0.4f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InvoicePreviewCard(config = invoiceConfig)
            }
        }
    }

    // Preview Dialog
    if (showPreview) {
        InvoicePreviewDialog(
            config = invoiceConfig,
            onClose = { showPreview = false }
        )
    }
}

@Composable
private fun InvoiceConfigHeader(
    onBack: () -> Unit,
    onPreview: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(0.dp),
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF2E2E2E),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                "Invoice Configuration",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E2E2E)
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFB8973D)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Preview", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview", fontSize = 14.sp)
                }

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFB8973D),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Done, contentDescription = "Save", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun CompanyInfoCard(
    config: InvoiceConfig,
    onConfigChange: (InvoiceConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Company Information",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            OutlinedTextField(
                value = config.companyName,
                onValueChange = { onConfigChange(config.copy(companyName = it)) },
                label = { Text("Company Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = config.companyAddress,
                onValueChange = { onConfigChange(config.copy(companyAddress = it)) },
                label = { Text("Company Address") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = config.companyPhone,
                    onValueChange = { onConfigChange(config.copy(companyPhone = it)) },
                    label = { Text("Phone") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = config.companyEmail,
                    onValueChange = { onConfigChange(config.copy(companyEmail = it)) },
                    label = { Text("Email") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }

            OutlinedTextField(
                value = config.companyGST,
                onValueChange = { onConfigChange(config.copy(companyGST = it)) },
                label = { Text("GST Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun InvoiceFieldsCard(
    config: InvoiceConfig,
    onConfigChange: (InvoiceConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Invoice Fields",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            Text(
                "Select which fields to display on the invoice",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )

            val fields = listOf(
                InvoiceField("showCustomerDetails", "Customer Details", config.showCustomerDetails),
                InvoiceField("showGoldRate", "Gold Rate Information", config.showGoldRate),
                InvoiceField("showMakingCharges", "Making Charges", config.showMakingCharges),
                InvoiceField("showPaymentBreakup", "Payment Breakup", config.showPaymentBreakup),
                InvoiceField("showItemDetails", "Item Details", config.showItemDetails),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(fields) { field ->
                    InvoiceFieldItem(
                        field = field,
                        onToggle = { isEnabled ->
                            val updatedConfig = when (field.fieldName) {
                                "showCustomerDetails" -> config.copy(showCustomerDetails = isEnabled)
                                "showGoldRate" -> config.copy(showGoldRate = isEnabled)
                                "showMakingCharges" -> config.copy(showMakingCharges = isEnabled)
                                "showPaymentBreakup" -> config.copy(showPaymentBreakup = isEnabled)
                                "showItemDetails" -> config.copy(showItemDetails = isEnabled)
                                else -> config
                            }
                            onConfigChange(updatedConfig)
                        }
                    )
                }
            }


            // Footer Text
            OutlinedTextField(
                value = config.footerText,
                onValueChange = { onConfigChange(config.copy(footerText = it)) },
                label = { Text("Footer Text") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun InvoiceFieldItem(
    field: InvoiceField,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!field.isEnabled) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = field.isEnabled,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFB8973D),
                uncheckedColor = Color(0xFFE0E0E0)
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            field.displayName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2E2E2E)
        )
    }
}

@Composable
private fun InvoicePreviewCard(config: InvoiceConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Invoice Preview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E2E2E)
            )

            // Preview content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Company Header
                    Text(
                        config.companyName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E2E2E)
                    )
                    
                    if (config.companyAddress.isNotEmpty()) {
                        Text(
                            config.companyAddress,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                    
                    if (config.companyPhone.isNotEmpty()) {
                        Text(
                            "Phone: ${config.companyPhone}",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                    
                    if (config.companyEmail.isNotEmpty()) {
                        Text(
                            "Email: ${config.companyEmail}",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                    
                    if (config.companyGST.isNotEmpty()) {
                        Text(
                            "GST: ${config.companyGST}",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sample invoice content
                    Text(
                        "INVOICE #INV-001",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E2E2E)
                    )

                    if (config.showCustomerDetails) {
                        Text(
                            "Customer: John Doe",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }

                    if (config.showGoldRate) {
                        Text(
                            "Gold Rate: ₹6,080/gm",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Items:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E2E2E)
                    )

                    Text(
                        "• Gold Ring (22k) - 5.5g - ₹33,440",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )

                    Text(
                        "• Silver Chain - 10g - ₹750",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Total: ₹34,190",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E2E2E)
                    )


                    if (config.footerText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            config.footerText,
                            fontSize = 10.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoicePreviewDialog(
    config: InvoiceConfig,
    onClose: () -> Unit
) {
    // Create a list of available fields based on InvoiceConfig properties
    val availableFields = listOf(
        InvoiceField("companyName", "Company Name", config.companyName.isNotEmpty()),
        InvoiceField("companyAddress", "Company Address", config.companyAddress.isNotEmpty()),
        InvoiceField("companyPhone", "Phone", config.companyPhone.isNotEmpty()),
        InvoiceField("companyEmail", "Email", config.companyEmail.isNotEmpty()),
        InvoiceField("companyGST", "GST Number", config.companyGST.isNotEmpty()),
        InvoiceField("showCustomerDetails", "Customer Details", config.showCustomerDetails),
        InvoiceField("showGoldRate", "Gold Rate", config.showGoldRate),
        InvoiceField("showMakingCharges", "Making Charges", config.showMakingCharges),
        InvoiceField("showPaymentBreakup", "Payment Breakup", config.showPaymentBreakup),
        InvoiceField("showItemDetails", "Item Details", config.showItemDetails),
    )
    
    var selectedFields by remember { mutableStateOf(availableFields.filter { it.isEnabled }.toSet()) }
    
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Invoice Preview & Field Selection",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E2E2E)
                    )
                    
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left side - Field selection
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            "Select Fields to Display",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E2E2E)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(availableFields) { field ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            selectedFields = if (selectedFields.contains(field)) {
                                                selectedFields - field
                                            } else {
                                                selectedFields + field
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedFields.contains(field),
                                        onCheckedChange = { 
                                            selectedFields = if (it) {
                                                selectedFields + field
                                            } else {
                                                selectedFields - field
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        field.displayName,
                                        fontSize = 14.sp,
                                        color = Color(0xFF2E2E2E)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Right side - Invoice preview
                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Invoice Preview",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E2E2E)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Invoice preview content
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 2.dp,
                            shape = RoundedCornerShape(8.dp),
                            backgroundColor = Color.White
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Company header
                                if (selectedFields.any { field -> field.fieldName == "companyName" }) {
                                    Text(
                                        config.companyName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                if (selectedFields.any { field -> field.fieldName == "companyAddress" }) {
                                    Text(
                                        config.companyAddress,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                if (selectedFields.any { field -> field.fieldName == "companyPhone" }) {
                                    Text(
                                        "Phone: ${config.companyPhone}",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                if (selectedFields.any { field -> field.fieldName == "companyEmail" }) {
                                    Text(
                                        "Email: ${config.companyEmail}",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                if (selectedFields.any { field -> field.fieldName == "companyGST" }) {
                                    Text(
                                        "GST: ${config.companyGST}",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Invoice details
                                Text(
                                    "TAX INVOICE",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                // Sample invoice content
                                Text(
                                    "Invoice #: INV-001",
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Text(
                                    "Date: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())}",
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Sample product table
                                Text(
                                    "Product Details:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Text(
                                    "Sample Product - ₹1,000.00",
                                    fontSize = 12.sp
                                )
                                
                                Text(
                                    "Total: ₹1,000.00",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF666666)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close")
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = {
                            // Update config with selected fields
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFB8973D),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Apply Changes")
                    }
                }
            }
        }
    }
}
