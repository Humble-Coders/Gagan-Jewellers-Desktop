package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun AutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onItemSelected: (String) -> Unit,
    onAddNew: (String) -> Unit,
    suggestions: List<String>,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    maxSuggestions: Int = 5
) {
    var expanded by remember { mutableStateOf(false) }
    var filteredSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddOption by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Open dropdown when text field is focused
    LaunchedEffect(isFocused) {
        if (isFocused) {
            expanded = true
        }
    }

    // Filter suggestions based on input
    LaunchedEffect(value, suggestions) {
        if (value.isNotEmpty()) {
            filteredSuggestions = suggestions.filter { 
                it.lowercase().contains(value.lowercase()) 
            }
            showAddOption = !suggestions.any { 
                it.lowercase() == value.lowercase() 
            }
        } else {
            filteredSuggestions = suggestions.take(maxSuggestions)
            showAddOption = false
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            trailingIcon = {
                Row {
                    if (value.isNotEmpty()) {
                        IconButton(
                            onClick = { 
                                onValueChange("")
                                expanded = false
                            }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                    IconButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = if (expanded) "Close" else "Open"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Dropdown suggestions positioned below the text field
        if (expanded && (filteredSuggestions.isNotEmpty() || showAddOption)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                elevation = 8.dp
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    // Show filtered suggestions
                    items(filteredSuggestions.take(maxSuggestions)) { suggestion ->
                        SuggestionItem(
                            text = suggestion,
                            onClick = {
                                onItemSelected(suggestion)
                                onValueChange(suggestion)
                                expanded = false
                            }
                        )
                    }
                    
                    // Show "Add new" option if current value doesn't exist
                    if (showAddOption && value.isNotEmpty()) {
                        item {
                            AddNewItem(
                                text = "Add \"$value\"",
                                onClick = {
                                    onAddNew(value)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body1,
            color = Color.Black
        )
    }
}

@Composable
private fun AddNewItem(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color(0xFFE3F2FD))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add new",
            tint = Color(0xFF1976D2),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.body1,
            color = Color(0xFF1976D2)
        )
    }
}
