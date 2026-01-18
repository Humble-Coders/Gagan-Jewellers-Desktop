package org.example.project.viewModels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import org.example.project.data.FirestoreCustomerRepository
import org.example.project.data.OrderRepository
import org.example.project.data.CashAmountRepository
import org.example.project.data.User
import org.example.project.data.UnifiedTransaction
import org.example.project.data.toUnifiedTransaction

class ProfileViewModel(
    private val orderRepository: OrderRepository,
    private val cashAmountRepository: CashAmountRepository
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Use StateFlow from repository (shared across all ViewModels - same instance as CustomerViewModel)
    val customers: StateFlow<List<User>> = FirestoreCustomerRepository.customers
    val loading: StateFlow<Boolean> = FirestoreCustomerRepository.loading
    val error: StateFlow<String?> = FirestoreCustomerRepository.error
    
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
    
    // Local loading state for transactions (separate from customer loading)
    private val _transactionsLoading = mutableStateOf(false)
    val transactionsLoading: State<Boolean> = _transactionsLoading
    
    // Local error state for transactions (separate from customer error)
    private val _transactionsError = mutableStateOf<String?>(null)
    val transactionsError: State<String?> = _transactionsError
    
    init {
        println("=".repeat(80))
        println("🏗️ PROFILE VIEWMODEL: Initializing")
        println("   - ViewModel instance: ${this.hashCode()}")
        println("   - Using shared StateFlow from FirestoreCustomerRepository")
        println("   - Repository instance: ${FirestoreCustomerRepository.hashCode()}")
        println("   - Customers StateFlow: ${customers.hashCode()}")
        println("   - Current customers count: ${customers.value.size}")
        println("   - This ViewModel will receive updates automatically via StateFlow")
        println("   - NO separate listener - using shared data from repository")
        println("   - Same StateFlow instance as CustomerViewModel")
        println("=".repeat(80))
        
        // Observe customers and update filtered list
        println("👂 PROFILE VIEWMODEL: Starting to observe customers StateFlow")
        println("   - ViewModel instance: ${this.hashCode()}")
        println("   - Will receive updates when repository listener updates StateFlow")
        viewModelScope.launch {
            customers.collect { customerList ->
                println("📥 PROFILE VIEWMODEL: Received customers update from shared StateFlow")
                println("   - ViewModel instance: ${this@ProfileViewModel.hashCode()}")
                println("   - Customers count: ${customerList.size}")
                println("   - This update came from the SINGLE repository listener")
                println("   - CustomerViewModel will also receive this same update")
                
                if (_searchQuery.value.isBlank()) {
                    _filteredCustomers.value = customerList
                } else {
                    _filteredCustomers.value = customerList.filter { customer ->
                        customer.name.contains(_searchQuery.value, ignoreCase = true) ||
                        customer.email.contains(_searchQuery.value, ignoreCase = true) ||
                        customer.phone.contains(_searchQuery.value, ignoreCase = true)
                    }
                }
            }
        }
    }
    
    // No need for loadCustomers() - data comes from repository listener automatically
    
    fun searchCustomers(query: String) {
        println("🔍 PROFILE VIEWMODEL: Searching customers")
        println("   - ViewModel instance: ${this.hashCode()}")
        println("   - Query: $query")
        println("   - Accessing customers from shared StateFlow (no new fetch)")
        
        _searchQuery.value = query
        val customerList = customers.value
        println("   - Customers from shared StateFlow: ${customerList.size}")
        
        if (query.isBlank()) {
            _filteredCustomers.value = customerList
        } else {
            _filteredCustomers.value = customerList.filter { customer ->
                customer.name.contains(query, ignoreCase = true) ||
                customer.email.contains(query, ignoreCase = true) ||
                customer.phone.contains(query, ignoreCase = true)
            }
        }
        println("   - Filtered customers: ${_filteredCustomers.value.size}")
    }
    
    fun selectCustomer(customer: User?) {
        println("👤 PROFILE VIEWMODEL: Selecting customer")
        println("   - ViewModel instance: ${this.hashCode()}")
        println("   - Customer: ${customer?.name ?: "null"}")
        println("   - Using data from shared StateFlow (no new fetch)")
        
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
            _transactionsLoading.value = true
            _transactionsError.value = null
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
                _transactionsError.value = null
            } catch (e: Exception) {
                _transactionsError.value = "Failed to load customer transactions: ${e.message}"
                println("❌ PROFILE VIEWMODEL: Error loading customer transactions: ${e.message}")
            } finally {
                _transactionsLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _transactionsError.value = null
    }
    
    fun onCleared() {
        viewModelScope.cancel()
    }
}
