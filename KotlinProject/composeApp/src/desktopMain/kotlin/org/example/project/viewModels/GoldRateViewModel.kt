package org.example.project.viewModels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.example.project.data.GoldRateRepository
import org.example.project.data.GoldRates

class GoldRateViewModel(
    private val goldRateRepository: GoldRateRepository
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Current gold rates state
    private val _goldRates = mutableStateOf(GoldRates())
    val goldRates: State<GoldRates> = _goldRates

    // Loading state
    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    // Error state
    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    // Input state for 24k rate
    private val _rate24kInput = mutableStateOf("")
    val rate24kInput: State<String> = _rate24kInput

    init {
        loadCurrentGoldRates()
    }

    fun loadCurrentGoldRates() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val rates = goldRateRepository.getCurrentGoldRates()
                // Null safety check
                if (rates != null) {
                    _goldRates.value = rates
                    _rate24kInput.value = rates.rate24k.toString()
                    _error.value = null
                } else {
                    _error.value = "Failed to load gold rates: Repository returned null"
                    println("❌ GoldRateRepository returned null")
                }
            } catch (e: IllegalStateException) {
                // Recoverable: Data structure issue
                _error.value = "Invalid gold rates data: ${e.message}"
                println("⚠️ Invalid gold rates data: ${e.message}")
                e.printStackTrace()
            } catch (e: Exception) {
                // Critical: Network or unexpected errors
                _error.value = "Failed to load gold rates: ${e.message ?: "Unknown error"}"
                println("❌ Critical error loading gold rates: ${e.message}")
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateRate24kInput(input: String) {
        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*\$"))) {
            _rate24kInput.value = input
            _error.value = null
        }
    }

    fun updateGoldRates() {
        val rate24k = _rate24kInput.value.toDoubleOrNull()
        // Enhanced validation: check for null, <= 0, NaN, and Infinity
        if (rate24k == null || rate24k <= 0 || !rate24k.isFinite()) {
            _error.value = when {
                rate24k == null -> "Please enter a valid 24k gold rate"
                rate24k <= 0 -> "Gold rate must be greater than 0"
                rate24k.isNaN() -> "Invalid rate: Not a number"
                rate24k.isInfinite() -> "Invalid rate: Infinity"
                else -> "Please enter a valid 24k gold rate"
            }
            return
        }

        viewModelScope.launch {
            _loading.value = true
            try {
                val newRates = GoldRates.calculateKaratRates(rate24k)
                val success = goldRateRepository.updateGoldRates(newRates)
                
                if (success) {
                    _goldRates.value = newRates
                    _error.value = null
                } else {
                    _error.value = "Failed to update gold rates: Repository returned false"
                    println("❌ GoldRateRepository.updateGoldRates returned false")
                }
            } catch (e: IllegalStateException) {
                // Recoverable: Data structure issue
                _error.value = "Invalid gold rates data: ${e.message}"
                println("⚠️ Invalid gold rates data: ${e.message}")
                e.printStackTrace()
            } catch (e: Exception) {
                // Critical: Network or unexpected errors
                val errorMessage = e.message ?: "Unknown error"
                _error.value = "Error updating gold rates: $errorMessage"
                println("❌ Critical error updating gold rates: $errorMessage")
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun getGoldRateForKarat(karat: Int): Double {
        return when (karat) {
            24 -> _goldRates.value.rate24k
            22 -> _goldRates.value.rate22k
            20 -> _goldRates.value.rate20k
            18 -> _goldRates.value.rate18k
            14 -> _goldRates.value.rate14k
            10 -> _goldRates.value.rate10k
            else -> _goldRates.value.rate22k // Default to 22k
        }
    }

    fun calculateProductPrice(weight: Double, karat: Int, makingCharges: Double = 0.0): Double {
        val goldRate = getGoldRateForKarat(karat)
        val goldValue = weight * goldRate
        val makingChargesValue = weight * makingCharges
        return goldValue + makingChargesValue
    }

    fun clearError() {
        _error.value = null
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}
