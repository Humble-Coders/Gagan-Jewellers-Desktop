package org.example.project.viewModels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import org.example.project.data.CustomerRepository
import org.example.project.data.OrderRepository
import org.example.project.data.CashAmountRepository
import org.example.project.data.FirestoreCashAmountRepository
import org.example.project.data.User
import org.example.project.data.Order
import org.example.project.data.UnifiedTransaction
import org.example.project.data.toUnifiedTransaction
import org.example.project.JewelryAppInitializer

class ProfileViewModel(
    private val customerRepository: CustomerRepository,
    private val orderRepository: OrderRepository,
    private val cashAmountRepository: CashAmountRepository
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State for customers list
    private val _customers = mutableStateOf<List<User>>(emptyList())
    val customers: State<List<User>> = _customers

    // State for filtered customers (search results)
    private val _filteredCustomers = mutableStateOf<List<User>>(emptyList())
    val filteredCustomers: State<List<User>> = _filteredCustomers

    // State for search query
    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    // State for selected customer
    private val _selectedCustomer = mutableStateOf<User?>(null)
    val selectedCustomer: State<User?> = _selectedCustomer

    // State for customer orders
    // Orders for selected customer - now unified transactions
    private val _customerTransactions = mutableStateOf<List<UnifiedTransaction>>(emptyList())
    val customerTransactions: State<List<UnifiedTransaction>> = _customerTransactions

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
                _customers.value = customerRepository.getAllCustomers()
                _filteredCustomers.value = _customers.value
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load customers: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun searchCustomers(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _filteredCustomers.value = _customers.value
        } else {
            _filteredCustomers.value = _customers.value.filter { customer ->
                customer.name.contains(query, ignoreCase = true) ||
                customer.email.contains(query, ignoreCase = true) ||
                customer.phone.contains(query, ignoreCase = true)
            }
        }
    }

    fun selectCustomer(customer: User?) {
        _selectedCustomer.value = customer
        if (customer != null) {
            println("👤 PROFILE VIEWMODEL: Selected customer - ID: ${customer.id}, CustomerID: ${customer.customerId}")
            loadCustomerTransactions(customer.customerId)
        } else {
            _customerTransactions.value = emptyList()
        }
    }

    fun loadCustomerTransactions(customerId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                println("🔄 PROFILE VIEWMODEL: Loading transactions for customer $customerId")
                
                // Fetch orders and cash transactions in parallel
                val ordersDeferred = async { orderRepository.getOrdersByCustomerId(customerId) }
                val cashTransactionsDeferred = async { cashAmountRepository.getCashTransactionsByCustomerId(customerId) }
                
                val orders = ordersDeferred.await()
                val cashTransactions = cashTransactionsDeferred.await()
                
                println("📊 PROFILE VIEWMODEL: Found ${orders.size} orders and ${cashTransactions.size} cash transactions")
                
                // Convert to unified transactions
                val unifiedOrders = orders.map { it.toUnifiedTransaction() }
                val unifiedCashTransactions = cashTransactions.map { it.toUnifiedTransaction() }
                
                // Combine and sort by creation date (newest first)
                val allTransactions = (unifiedOrders + unifiedCashTransactions)
                    .sortedByDescending { it.createdAt }
                
                println("🔄 PROFILE VIEWMODEL: Total unified transactions: ${allTransactions.size}")
                
                _customerTransactions.value = allTransactions
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load customer transactions: ${e.message}"
                println("❌ PROFILE VIEWMODEL: Error loading customer transactions: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}
