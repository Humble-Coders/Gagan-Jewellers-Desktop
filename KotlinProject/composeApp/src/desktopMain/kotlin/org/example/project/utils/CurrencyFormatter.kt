package org.example.project.utils

/**
 * Utility object for formatting currency in Indian Rupees format
 * Provides consistent currency formatting across the entire application
 * Uses Indian numbering system: 3,59,531 (first 3 digits, then groups of 2)
 */
object CurrencyFormatter {
    
    /**
     * Formats a number as Indian Rupees with currency symbol
     * Example: formatRupees(359531.0) returns "₹3,59,531"
     * Example: formatRupees(1234567.89) returns "₹12,34,567"
     */
    fun formatRupees(amount: Double, includeDecimals: Boolean = false): String {
        val formatted = formatIndianNumber(amount, includeDecimals)
        return "₹$formatted"
    }
    
    /**
     * Formats a number as Indian Rupees without currency symbol
     * Example: formatRupeesNumber(359531.0) returns "3,59,531"
     * Example: formatRupeesNumber(1234567.89) returns "12,34,567"
     */
    fun formatRupeesNumber(amount: Double, includeDecimals: Boolean = false): String {
        return formatIndianNumber(amount, includeDecimals)
    }
    
    /**
     * Formats a number with ₹ symbol prefix (simpler version)
     * Example: formatRupees(359531.0) returns "₹3,59,531"
     */
    fun formatRupees(amount: Number): String {
        return formatRupees(amount.toDouble(), includeDecimals = false)
    }
    
    /**
     * Formats a number using Indian numbering system
     * Indian numbering: first 3 digits from right, then groups of 2
     * Example: 359531 -> "3,59,531"
     * Example: 1234567 -> "12,34,567"
     */
    private fun formatIndianNumber(amount: Double, includeDecimals: Boolean): String {
        // Round to appropriate decimal places
        val roundedAmount = if (includeDecimals) {
            String.format("%.2f", amount).toDouble()
        } else {
            amount.toLong().toDouble()
        }
        
        // Convert to string without decimals if not needed
        val amountStr = if (includeDecimals) {
            String.format("%.2f", roundedAmount)
        } else {
            roundedAmount.toLong().toString()
        }
        
        // Split into integer and decimal parts
        val parts = amountStr.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1 && includeDecimals) parts[1] else ""
        
        // Apply Indian numbering system
        val formattedInteger = formatIndianInteger(integerPart)
        
        return if (decimalPart.isNotEmpty()) {
            "$formattedInteger.$decimalPart"
        } else {
            formattedInteger
        }
    }
    
    /**
     * Formats an integer string using Indian numbering system
     * First 3 digits from right, then groups of 2
     * Example: "359531" -> "3,59,531"
     * Example: "1234567" -> "12,34,567"
     */
    private fun formatIndianInteger(numberStr: String): String {
        if (numberStr.length <= 3) {
            return numberStr
        }
        
        val parts = mutableListOf<String>()
        var remaining = numberStr
        
        // Take last 3 digits first
        if (remaining.length > 3) {
            parts.add(remaining.substring(remaining.length - 3))
            remaining = remaining.substring(0, remaining.length - 3)
            
            // Then take groups of 2 from right to left
            while (remaining.length > 0) {
                val groupSize = minOf(2, remaining.length)
                parts.add(remaining.substring(remaining.length - groupSize))
                remaining = remaining.substring(0, remaining.length - groupSize)
            }
        } else {
            return numberStr
        }
        
        // Reverse parts and join with commas
        return parts.reversed().joinToString(",")
    }
    
    /**
     * Rupee symbol constant
     */
    const val RUPEE_SYMBOL = "₹"
}

