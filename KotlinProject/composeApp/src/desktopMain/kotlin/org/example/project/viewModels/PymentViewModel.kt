package org.example.project.viewModels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.data.Cart
import org.example.project.data.DiscountType
import org.example.project.data.Order
import org.example.project.data.OrderStatus
import org.example.project.data.PaymentMethod
import org.example.project.data.PaymentRepository
import org.example.project.data.PaymentStatus
import org.example.project.data.PaymentTransaction

class PaymentViewModel(
    private val paymentRepository: PaymentRepository
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Payment Method State
    private val _selectedPaymentMethod = mutableStateOf<PaymentMethod?>(null)
    val selectedPaymentMethod: State<PaymentMethod?> = _selectedPaymentMethod

    // Discount State
    private val _discountType = mutableStateOf(DiscountType.AMOUNT)
    val discountType: State<DiscountType> = _discountType

    private val _discountValue = mutableStateOf("")
    val discountValue: State<String> = _discountValue

    private val _discountAmount = mutableStateOf(0.0)
    val discountAmount: State<Double> = _discountAmount

    // Loading and Error States
    private val _isProcessing = mutableStateOf(false)
    val isProcessing: State<Boolean> = _isProcessing

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    // Transaction State
    private val _lastTransaction = mutableStateOf<PaymentTransaction?>(null)
    val lastTransaction: State<PaymentTransaction?> = _lastTransaction

    fun setPaymentMethod(paymentMethod: PaymentMethod) {
        _selectedPaymentMethod.value = paymentMethod
        _errorMessage.value = null
    }

    fun setDiscountType(discountType: DiscountType) {
        _discountType.value = discountType
        _discountValue.value = ""
        _discountAmount.value = 0.0
        _errorMessage.value = null
    }

    fun setDiscountValue(value: String) {
        // Allow only numbers and single decimal point
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*\$"))) {
            _discountValue.value = value
            _errorMessage.value = null
        }
    }

    fun applyDiscount() {
        val value = _discountValue.value.toDoubleOrNull()

        if (value == null || value <= 0) {
            _errorMessage.value = "Please enter a valid discount value"
            return
        }

        when (_discountType.value) {
            DiscountType.PERCENTAGE -> {
                if (value > 100) {
                    _errorMessage.value = "Percentage discount cannot exceed 100%"
                    return
                }
                // Store percentage value - will be applied to subtotal in calculation
                _discountAmount.value = value
            }

            DiscountType.AMOUNT -> {
                _discountAmount.value = value
            }
        }

        _errorMessage.value = null
    }

    fun calculateDiscountAmount(subtotal: Double): Double {
        return when (_discountType.value) {
            DiscountType.PERCENTAGE -> {
                subtotal * (_discountAmount.value / 100)
            }

            DiscountType.AMOUNT -> {
                minOf(_discountAmount.value, subtotal) // Don't exceed subtotal
            }
        }
    }

    fun saveOrderWithPaymentMethod(
        cart: Cart,
        subtotal: Double,
        discountAmount: Double,
        gst: Double,
        finalTotal: Double,
        onSuccess: () -> Unit
    ) {
        if (_selectedPaymentMethod.value == null) {
            _errorMessage.value = "Please select a payment method"
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null

            try {
                val order = Order(
                    id = generateOrderId(),
                    customerId = cart.customerId,
                    paymentMethod = _selectedPaymentMethod.value!!,
                    subtotal = subtotal,
                    discountAmount = discountAmount,
                    gstAmount = gst,
                    totalAmount = finalTotal,
                    items = cart.items,
                    timestamp = System.currentTimeMillis(),
                    status = OrderStatus.CONFIRMED
                )

                // Save order to database
                val success = paymentRepository.saveOrder(order)

                if (success) {
                    _lastTransaction.value = PaymentTransaction(
                        id = order.id,
                        cartId = order.customerId,
                        paymentMethod = order.paymentMethod,
                        subtotal = order.subtotal,
                        discountAmount = order.discountAmount,
                        gstAmount = order.gstAmount,
                        totalAmount = order.totalAmount,
                        items = order.items,
                        timestamp = order.timestamp,
                        status = PaymentStatus.PENDING // Payment method selected but not processed
                    )
                    onSuccess()
                } else {
                    _errorMessage.value = "Failed to save order. Please try again."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error saving order: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun generateOrderId(): String {
        return "ORD_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    fun clearDiscount() {
        _discountValue.value = ""
        _discountAmount.value = 0.0
        _errorMessage.value = null
    }

    fun clearPaymentMethod() {
        _selectedPaymentMethod.value = null
    }

    fun resetPaymentState() {
        _selectedPaymentMethod.value = null
        _discountType.value = DiscountType.AMOUNT
        _discountValue.value = ""
        _discountAmount.value = 0.0
        _isProcessing.value = false
        _errorMessage.value = null
        _lastTransaction.value = null
    }

    private fun generateTransactionId(): String {
        return "TXN_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}