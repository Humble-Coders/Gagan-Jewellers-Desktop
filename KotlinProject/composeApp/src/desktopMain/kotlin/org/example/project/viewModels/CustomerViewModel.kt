// CustomerViewModel.kt
package org.example.project.viewModels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.FirestoreCustomerRepository
import org.example.project.data.User

class CustomerViewModel {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    init {
        println("=".repeat(80))
        println("🏗️ CUSTOMER VIEWMODEL: Initializing")
        println("   - ViewModel instance: ${this.hashCode()}")
        println("   - Using shared StateFlow from FirestoreCustomerRepository")
        println("   - Repository instance: ${FirestoreCustomerRepository.hashCode()}")
        println("   - Customers StateFlow: ${FirestoreCustomerRepository.customers.hashCode()}")
        println("   - This ViewModel will receive updates automatically via StateFlow")
        println("   - NO separate listener - using shared data from repository")
        println("=".repeat(80))
    }
    
    // Use StateFlow from repository (shared across all ViewModels)
    val customers: StateFlow<List<User>> = FirestoreCustomerRepository.customers.also {
        println("📊 CUSTOMER VIEWMODEL: Accessing customers StateFlow")
        println("   - ViewModel instance: ${this.hashCode()}")
        println("   - StateFlow instance: ${it.hashCode()}")
        println("   - Current customers count: ${it.value.size}")
    }
    val loading: StateFlow<Boolean> = FirestoreCustomerRepository.loading
    val error: StateFlow<String?> = FirestoreCustomerRepository.error
    
    // Selected customer state (local to ViewModel)
    private val _selectedCustomer = mutableStateOf<User?>(null)
    val selectedCustomer: State<User?> = _selectedCustomer
    
    // No need for loadCustomers() - data comes from repository listener automatically
    
    fun selectCustomer(customer: User?) {
        println("👤 CUSTOMER VIEWMODEL: Selecting customer")
        println("   - ViewModel instance: ${this.hashCode()}")
        println("   - Customer: ${customer?.name ?: "null"}")
        println("   - Using data from shared StateFlow (no new fetch)")
        
        // Validate customer data before selection
        if (customer != null) {
            // Basic validation: ensure customer has required fields
            if (customer.name.isBlank()) {
                println("⚠️ CUSTOMER VIEWMODEL: Attempted to select customer with blank name")
                return
            }
        }
        _selectedCustomer.value = customer
    }
    
    fun addCustomer(customer: User) {
        println("➕ CUSTOMER VIEWMODEL: Adding customer")
        println("   - ViewModel instance: ${this.hashCode()}")
        println("   - Customer name: ${customer.name}")
        println("   - Will use shared repository (same instance used by all ViewModels)")
        
        // Validate customer data before adding
        val validationErrors = mutableListOf<String>()
        
        if (customer.name.isBlank()) {
            validationErrors.add("Customer name is required")
        }
        
        if (customer.phone.isBlank() && customer.email.isBlank()) {
            validationErrors.add("At least one contact method (phone or email) is required")
        }
        
        if (validationErrors.isNotEmpty()) {
            println("❌ CUSTOMER VIEWMODEL: Validation failed: ${validationErrors.joinToString(", ")}")
            return
        }
        
        viewModelScope.launch {
            try {
                val id = FirestoreCustomerRepository.addCustomer(customer)
                println("✅ CUSTOMER VIEWMODEL: Customer added successfully with ID: $id")
                println("   - Repository listener will automatically update StateFlow")
                println("   - This ViewModel and ProfileViewModel will both receive the update")
                // No need to reload - listener will update automatically
            } catch (e: Exception) {
                println("❌ CUSTOMER VIEWMODEL: Failed to add customer: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    fun updateCustomer(customer: User) {
        viewModelScope.launch {
            try {
                val success = FirestoreCustomerRepository.updateCustomer(customer)
                if (!success) {
                    println("❌ CUSTOMER VIEWMODEL: Failed to update customer")
                }
                // No need to reload - listener will update automatically
            } catch (e: Exception) {
                println("❌ CUSTOMER VIEWMODEL: Failed to update customer: ${e.message}")
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
