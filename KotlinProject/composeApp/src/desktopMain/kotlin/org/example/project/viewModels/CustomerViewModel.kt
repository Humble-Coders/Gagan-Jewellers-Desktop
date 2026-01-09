// CustomerViewModel.kt
package org.example.project.viewModels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.example.project.data.CustomerRepository
import org.example.project.data.User

class CustomerViewModel(private val repository: CustomerRepository) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State for customers list
    private val _customers = mutableStateOf<List<User>>(emptyList())
    val customers: State<List<User>> = _customers

    // Selected customer state
    private val _selectedCustomer = mutableStateOf<User?>(null)
    val selectedCustomer: State<User?> = _selectedCustomer

    // Loading state
    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    // Error state
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        loadCustomers()
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _customers.value = repository.getAllCustomers()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load customers: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun selectCustomer(customer: User?) {
        // Validate customer data before selection
        if (customer != null) {
            // Basic validation: ensure customer has required fields
            if (customer.name.isBlank()) {
                println("⚠️ CUSTOMER VIEWMODEL: Attempted to select customer with blank name")
                _error.value = "Cannot select customer: name is required"
                return
            }
        }
        _selectedCustomer.value = customer
        _error.value = null
    }

    fun addCustomer(customer: User) {
        // Validate customer data before adding
        val validationErrors = mutableListOf<String>()
        
        if (customer.name.isBlank()) {
            validationErrors.add("Customer name is required")
        }
        
        if (customer.phone.isBlank() && customer.email.isBlank()) {
            validationErrors.add("At least one contact method (phone or email) is required")
        }
        
        if (validationErrors.isNotEmpty()) {
            _error.value = "Validation failed: ${validationErrors.joinToString(", ")}"
            return
        }
        
        viewModelScope.launch {
            _loading.value = true
            try {
                val id = repository.addCustomer(customer)
                println("✅ CUSTOMER VIEWMODEL: Customer added successfully with ID: $id")
                loadCustomers()
                _error.value = null
            } catch (e: Exception) {
                println("❌ CUSTOMER VIEWMODEL: Failed to add customer: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to add customer: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateCustomer(customer: User) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val success = repository.updateCustomer(customer)
                if (success) {
                    loadCustomers()
                    _error.value = null
                } else {
                    _error.value = "Failed to update customer"
                }
            } catch (e: Exception) {
                _error.value = "Failed to update customer: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearSelectedCustomer() {
        _selectedCustomer.value = null
    }

    // Clean up resources
    fun onCleared() {
        viewModelScope.cancel()
    }
}