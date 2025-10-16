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
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.Category
import org.example.project.data.CategoryType
import org.example.project.viewModels.ProductsViewModel

@Composable
fun CategoryManagementScreen(
    productsViewModel: ProductsViewModel,
    onBack: () -> Unit
) {
    val categories by productsViewModel.categories
    var showAddCategory by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Header
        CategoryManagementHeader(
            onBack = onBack,
            onAddCategory = { showAddCategory = true }
        )

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Categories List
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        "Product Categories",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E2E2E)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (categories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No categories found. Add a new category to get started.",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                CategoryItem(
                                    category = category,
                                    onEdit = { editingCategory = category },
                                    onToggleActive = {
                                        // Toggle active status
                                        val updatedCategory = category.copy(isActive = !category.isActive)
                                        productsViewModel.updateCategory(updatedCategory)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Category Dialog
    if (showAddCategory || editingCategory != null) {
        AddEditCategoryDialog(
            category = editingCategory,
            onSave = { category ->
                if (editingCategory != null) {
                    productsViewModel.updateCategory(category)
                } else {
                    productsViewModel.addCategory(category)
                }
                showAddCategory = false
                editingCategory = null
            },
            onCancel = {
                showAddCategory = false
                editingCategory = null
            }
        )
    }
}

@Composable
private fun CategoryManagementHeader(
    onBack: () -> Unit,
    onAddCategory: () -> Unit
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
                "Category Management",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E2E2E)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onAddCategory,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFB8973D),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Category", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = if (category.isActive) Color.White else Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    category.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (category.isActive) Color(0xFF2E2E2E) else Color.Gray
                )
                
                Text(
                    category.description,
                    fontSize = 14.sp,
                    color = if (category.isActive) Color(0xFF666666) else Color.Gray,
                    maxLines = 1
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryTypeChip(categoryType = category.categoryType)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(isActive = category.isActive)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleActive,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (category.isActive) Icons.Default.Close else Icons.Default.Check,
                        contentDescription = if (category.isActive) "Deactivate" else "Activate",
                        tint = if (category.isActive) Color(0xFFD32F2F) else Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFFB8973D),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTypeChip(categoryType: CategoryType) {
    Box(
        modifier = Modifier
            .background(
                when (categoryType) {
                    CategoryType.RAW_GOLD -> Color(0xFFFFF8E1)
                    CategoryType.FINE_GOLD -> Color(0xFFFFF8E1)
                    CategoryType.CRYSTALS -> Color(0xFFE8F5E9)
                    CategoryType.SILVER -> Color(0xFFF3E5F5)
                    CategoryType.DIAMONDS -> Color(0xFFE3F2FD)
                    CategoryType.GEMSTONES -> Color(0xFFFFEBEE)
                    CategoryType.JEWELRY -> Color(0xFFF5F5F5)
                    CategoryType.ACCESSORIES -> Color(0xFFE0F2F1)
                    CategoryType.REPAIR -> Color(0xFFFFF3E0)
                    CategoryType.OTHER -> Color(0xFFF5F5F5)
                },
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            categoryType.name.replace("_", " "),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = when (categoryType) {
                CategoryType.RAW_GOLD -> Color(0xFFB8973D)
                CategoryType.FINE_GOLD -> Color(0xFFB8973D)
                CategoryType.CRYSTALS -> Color(0xFF4CAF50)
                CategoryType.SILVER -> Color(0xFF9C27B0)
                CategoryType.DIAMONDS -> Color(0xFF2196F3)
                CategoryType.GEMSTONES -> Color(0xFFD32F2F)
                CategoryType.JEWELRY -> Color(0xFF666666)
                CategoryType.ACCESSORIES -> Color(0xFF009688)
                CategoryType.REPAIR -> Color(0xFFFF9800)
                CategoryType.OTHER -> Color(0xFF666666)
            }
        )
    }
}

@Composable
private fun StatusChip(isActive: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            if (isActive) "Active" else "Inactive",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isActive) Color(0xFF4CAF50) else Color(0xFFD32F2F)
        )
    }
}

@Composable
private fun AddEditCategoryDialog(
    category: Category?,
    onSave: (Category) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var description by remember { mutableStateOf(category?.description ?: "") }
    var categoryType by remember { mutableStateOf(category?.categoryType ?: CategoryType.JEWELRY) }
    var isActive by remember { mutableStateOf(category?.isActive ?: true) }

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    if (category != null) "Edit Category" else "Add New Category",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E2E2E)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
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

                // Category Type Selection
                Text(
                    "Category Type",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2E2E2E)
                )

                LazyColumn(
                    modifier = Modifier.height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(CategoryType.values().toList()) { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryType = type }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = categoryType == type,
                                onClick = { categoryType = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                type.name.replace("_", " "),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Active Category", fontSize = 14.sp)
                }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF666666)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val newCategory = Category(
                                    id = category?.id ?: "",
                                    name = name.trim(),
                                    description = description.trim(),
                                    categoryType = categoryType,
                                    isActive = isActive
                                )
                                onSave(newCategory)
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFB8973D),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
