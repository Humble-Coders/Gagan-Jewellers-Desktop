package org.example.project.viewModels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.example.project.data.CustomerRepository
import org.example.project.data.OrderRepository
import org.example.project.data.User
import org.example.project.data.Order

class ProfileViewModel(
    private val customerRepository: CustomerRepository,
    private val orderRepository: OrderRepository
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
    private val _customerOrders = mutableStateOf<List<Order>>(emptyList())
    val customerOrders: State<List<Order>> = _customerOrders

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
            loadCustomerOrders(customer.id)
        } else {
            _customerOrders.value = emptyList()
        }
    }

    fun loadCustomerOrders(customerId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _customerOrders.value = orderRepository.getOrdersByCustomerId(customerId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load customer orders: ${e.message}"
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
