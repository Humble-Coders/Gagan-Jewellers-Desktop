package org.example.project.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.InvoiceConfig
import org.example.project.data.InvoiceConfigRepository

class InvoiceConfigViewModel : ViewModel() {
    private val repository by lazy { InvoiceConfigRepository() }

    private val _invoiceConfig = MutableStateFlow(InvoiceConfig())
    val invoiceConfig: StateFlow<InvoiceConfig> = _invoiceConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadInvoiceConfig()
    }

    fun loadInvoiceConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val config = repository.getInvoiceConfig()
                if (config != null) {
                    _invoiceConfig.value = config
                    println("✅ Invoice config loaded successfully")
                } else {
                    _errorMessage.value = "Failed to load invoice configuration"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading invoice configuration: ${e.message}"
                println("❌ Error loading invoice config: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateInvoiceConfig(config: InvoiceConfig) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val success = repository.saveInvoiceConfig(config)
                if (success) {
                    _invoiceConfig.value = config
                    _successMessage.value = "Invoice configuration saved successfully"
                    println("✅ Invoice config updated successfully")
                } else {
                    _errorMessage.value = "Failed to save invoice configuration"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error saving invoice configuration: ${e.message}"
                println("❌ Error saving invoice config: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateCompanyInfo(
        name: String,
        address: String,
        phone: String,
        email: String,
        gst: String
    ) {
        val currentConfig = _invoiceConfig.value
        val updatedConfig = currentConfig.copy(
            companyName = name,
            companyAddress = address,
            companyPhone = phone,
            companyEmail = email,
            companyGST = gst
        )
        updateInvoiceConfig(updatedConfig)
    }

    fun updateDisplaySettings(
        showCustomerDetails: Boolean,
        showGoldRate: Boolean,
        showMakingCharges: Boolean,
        showPaymentBreakup: Boolean,
        showItemDetails: Boolean,
        showTermsAndConditions: Boolean
    ) {
        val currentConfig = _invoiceConfig.value
        val updatedConfig = currentConfig.copy(
            showCustomerDetails = showCustomerDetails,
            showGoldRate = showGoldRate,
            showMakingCharges = showMakingCharges,
            showPaymentBreakup = showPaymentBreakup,
            showItemDetails = showItemDetails,
            showTermsAndConditions = showTermsAndConditions
        )
        updateInvoiceConfig(updatedConfig)
    }

    fun updateFooterSettings(
        footerText: String,
        termsAndConditions: String
    ) {
        val currentConfig = _invoiceConfig.value
        val updatedConfig = currentConfig.copy(
            footerText = footerText,
            termsAndConditions = termsAndConditions
        )
        updateInvoiceConfig(updatedConfig)
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
