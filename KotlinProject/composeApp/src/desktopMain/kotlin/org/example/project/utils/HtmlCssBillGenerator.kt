package org.example.project.utils

import org.example.project.data.Order
import org.example.project.data.User
import org.example.project.data.CartItem
import org.example.project.data.InvoiceConfig
import org.example.project.data.MetalRatesManager
import org.example.project.data.extractKaratFromMaterialType
import java.io.File
import java.io.FileWriter
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

class HtmlCssBillGenerator {

    fun generateHtmlBill(
        order: Order,
        customer: User,
        outputPath: Path,
        invoiceConfig: InvoiceConfig = InvoiceConfig()
    ): Boolean {
        return try {
            println("🔄 ========== HTML/CSS BILL GENERATION STARTED ==========")
            println("📋 Order ID: ${order.id}")
            println("📄 Output path: $outputPath")
            println("👤 Customer: ${customer.name} (ID: ${customer.id})")
            println("💰 Order total: ₹${String.format("%.2f", order.totalAmount)}")
            println("📦 Items count: ${order.items.size}")
            println("💳 Payment method: ${order.paymentMethod}")
            
            // Validate inputs
            if (order.id.isBlank()) {
                throw IllegalArgumentException("Order ID is blank")
            }
            if (customer.name.isBlank()) {
                throw IllegalArgumentException("Customer name is blank")
            }
            if (order.items.isEmpty()) {
                throw IllegalArgumentException("Order has no items")
            }
            
            // Check output directory
            val htmlFile = outputPath.toFile()
            val outputDir = htmlFile.parentFile
            
            if (!outputDir.exists()) {
                val created = outputDir.mkdirs()
                println("📁 Created output directory: $created")
            }
            
            if (!outputDir.canWrite()) {
                throw SecurityException("Cannot write to output directory: ${outputDir.absolutePath}")
            }
            
            println("✅ Output directory validated")
            
            // Generate HTML content
            println("🔧 Generating HTML content...")
            val htmlContent = generateHtmlContent(order, customer, invoiceConfig)
            println("📄 Generated HTML content length: ${htmlContent.length} characters")
            
            if (htmlContent.isBlank()) {
                throw IllegalStateException("Generated HTML content is empty")
            }
            
            // Write HTML file
            println("🔧 Writing HTML file...")
            FileWriter(htmlFile).use { writer ->
                writer.write(htmlContent)
            }
            
            // Verify HTML file was created
            if (htmlFile.exists() && htmlFile.length() > 0) {
                println("✅ HTML file created successfully: ${htmlFile.length()} bytes")
            } else {
                throw IllegalStateException("HTML file was not created or is empty")
            }
            
            // Copy CSS file to the same directory
            println("🔧 Copying CSS file...")
            copyCssFile(outputPath)
            
            println("✅ ========== HTML/CSS BILL GENERATION SUCCESSFUL ==========")
            println("📄 HTML file: $outputPath")
            println("📄 CSS file: ${outputDir.absolutePath}/bill-styles.css")
            true
            
        } catch (e: Exception) {
            println("❌ ========== HTML/CSS BILL GENERATION FAILED ==========")
            println("💥 Exception: ${e.javaClass.simpleName}")
            println("💥 Message: ${e.message}")
            println("💥 Stack trace:")
            e.printStackTrace()
            false
        }
    }

    private fun generateHtmlContent(order: Order, customer: User, invoiceConfig: InvoiceConfig): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val orderDate = dateFormat.format(Date(order.timestamp))
        
        // Calculate totals
        val subtotal = order.subtotal
        val gstAmount = order.gstAmount
        val discountAmount = order.discountAmount
        val totalAmount = order.totalAmount
        
        // Calculate GST percentage
        val gstPercentage = if (subtotal > 0) ((gstAmount / subtotal) * 100).toInt() else 0

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>Invoice ${order.id}</title>
            <link rel="stylesheet" href="bill-styles.css">
        </head>
        <body>
            <div class="invoice-container">
                <!-- Header -->
                <div class="header">
                    <div class="company-info">
                        <h1 class="company-name">${invoiceConfig.companyName}</h1>
                        <p class="company-details">Premium Gold &amp; Silver Jewelry</p>
                        ${if (invoiceConfig.companyAddress.isNotEmpty()) "<p class=\"company-address\">${invoiceConfig.companyAddress}</p>" else ""}
                        <p class="company-contact">
                            ${if (invoiceConfig.companyPhone.isNotEmpty()) "Phone: ${invoiceConfig.companyPhone}" else ""}
                            ${if (invoiceConfig.companyEmail.isNotEmpty()) " | Email: ${invoiceConfig.companyEmail}" else ""}
                            ${if (invoiceConfig.companyGST.isNotEmpty()) " | GST: ${invoiceConfig.companyGST}" else ""}
                        </p>
                    </div>
                    <div class="invoice-info">
                        <h2 class="invoice-title">INVOICE</h2>
                        <div class="invoice-details">
                            <p><strong>Invoice No:</strong> ${order.id}</p>
                            <p><strong>Date:</strong> $orderDate</p>
                        </div>
                    </div>
                </div>

                <!-- Customer Information -->
                <div class="customer-section">
                    <h3>Bill To:</h3>
                    <div class="customer-details">
                        <p><strong>${customer.name}</strong></p>
                        ${if (customer.phone.isNotEmpty()) "<p>Phone: ${customer.phone}</p>" else ""}
                        ${if (customer.email.isNotEmpty()) "<p>Email: ${customer.email}</p>" else ""}
                        ${if (customer.address.isNotEmpty()) "<p>Address: ${customer.address}</p>" else ""}
                    </div>
                </div>


                <!-- Items Table -->
                <div class="items-section">
                    <table class="items-table">
                        <thead>
                            <tr>
                                <th>Item</th>
                                <th>Material</th>
                                <th>Weight Details</th>
                                <th>Qty</th>
                                <th>Rate</th>
                                <th>Charges Breakdown</th>
                                <th>Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${generateItemsRows(order.items)}
                        </tbody>
                    </table>
                </div>

                <!-- Totals Section -->
                <div class="totals-section">
                    <div class="totals-table">
                        <div class="total-row">
                            <span class="total-label">Subtotal:</span>
                            <span class="total-value">Rs ${String.format("%.2f", subtotal)}</span>
                        </div>
                        ${if (gstAmount > 0) """
                        <div class="total-row">
                            <span class="total-label">GST (${gstPercentage}%):</span>
                            <span class="total-value">Rs ${String.format("%.2f", gstAmount)}</span>
                        </div>
                        """ else ""}
                        ${if (discountAmount > 0) """
                        <div class="total-row discount-row">
                            <span class="total-label">Discount:</span>
                            <span class="total-value">-Rs ${String.format("%.2f", discountAmount)}</span>
                        </div>
                        """ else ""}
                        <div class="total-row final-total">
                            <span class="total-label"><strong>TOTAL:</strong></span>
                            <span class="total-value"><strong>Rs ${String.format("%.2f", totalAmount)}</strong></span>
                        </div>
                    </div>
                </div>

                ${if (order.paymentSplit != null) """
                <!-- Payment Breakdown Section -->
                <div class="payment-breakdown-section">
                    <h3>Payment Breakdown</h3>
                    <div class="payment-breakdown-table">
                        ${generatePaymentBreakdownRows(order.paymentSplit)}
                    </div>
                </div>
                """ else ""}

                <!-- Footer -->
                <div class="footer">
                    <p class="thank-you">${invoiceConfig.termsAndConditions}</p>
                    ${if (invoiceConfig.footerText.isNotEmpty()) "<p class=\"terms\">${invoiceConfig.footerText}</p>" else ""}
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun generateItemsRows(items: List<CartItem>): String {
        return items.joinToString("\n") { item ->
            val metalRates = MetalRatesManager.metalRates.value
            
            // Use the same calculation logic as CartTable.kt
            val metalKarat = if (item.metal.isNotEmpty()) {
                item.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(item.product.materialType)
            } else {
                extractKaratFromMaterialType(item.product.materialType)
            }
            
            // Get rates using the same logic as cart screen
            val goldRate = metalRates.getGoldRateForKarat(metalKarat)
            val silverPurity = extractSilverPurityFromMaterialType(item.product.materialType)
            val silverRate = metalRates.getSilverRateForPurity(silverPurity)
            
            // Try to get collection-specific rate first (same as cart)
            val ratesVM = org.example.project.JewelryAppInitializer.getMetalRateViewModel()
            val collectionRate = try {
                ratesVM.calculateRateForMaterial(item.product.materialId, item.product.materialType, metalKarat)
            } catch (e: Exception) { 0.0 }
            
            val metalRate = if (collectionRate > 0) collectionRate else when {
                item.product.materialType.contains("gold", ignoreCase = true) -> goldRate
                item.product.materialType.contains("silver", ignoreCase = true) -> silverRate
                else -> goldRate
            }
            
            // Use the same weight calculation as cart screen
            val grossWeight = if (item.grossWeight > 0) item.grossWeight else item.product.totalWeight
            val lessWeight = if (item.lessWeight > 0) item.lessWeight else item.product.lessWeight
            val netWeight = grossWeight - lessWeight
            val quantity = item.quantity
            
            // Use the same charge calculations as cart screen
            val makingChargesPerGram = if (item.makingCharges > 0) item.makingCharges else item.product.defaultMakingRate
            val cwWeight = if (item.cwWeight > 0) item.cwWeight else item.product.cwWeight
            val stoneRate = if (item.stoneRate > 0) item.stoneRate else item.product.stoneRate
            val stoneQuantity = if (item.stoneQuantity > 0) item.stoneQuantity else item.product.stoneQuantity
            val vaCharges = if (item.va > 0) item.va else item.product.vaCharges
            
            // Calculate amounts using the same logic as cart screen
            val baseAmount = netWeight * metalRate * quantity
            val makingCharges = netWeight * makingChargesPerGram * quantity
            val stoneAmount = stoneRate * stoneQuantity * cwWeight
            val totalCharges = baseAmount + makingCharges + stoneAmount + vaCharges
            
            """
            <tr>
                <td class="item-name">${item.product.name}</td>
                <td>${item.metal.ifEmpty { item.product.materialType }}</td>
                <td class="weight-details">
                    <div class="weight-breakdown">
                        <div>Gross: ${String.format("%.2f", grossWeight)}g</div>
                        <div>Less: ${String.format("%.2f", lessWeight)}g</div>
                        <div class="net-weight">Net: ${String.format("%.2f", netWeight)}g</div>
                    </div>
                </td>
                <td>${quantity}</td>
                <td>Rs ${String.format("%.2f", metalRate)}</td>
                <td class="charges-breakdown">
                    <div class="charge-item">Base: Rs ${String.format("%.2f", baseAmount)}</div>
                    ${if (makingCharges > 0) "<div class=\"charge-item\">Making: Rs ${String.format("%.2f", makingCharges)}</div>" else ""}
                    ${if (stoneAmount > 0) "<div class=\"charge-item\">Stone: Rs ${String.format("%.2f", stoneAmount)}</div>" else ""}
                    ${if (vaCharges > 0) "<div class=\"charge-item\">VA: Rs ${String.format("%.2f", vaCharges)}</div>" else ""}
                </td>
                <td class="total-amount">Rs ${String.format("%.2f", totalCharges)}</td>
            </tr>
            """.trimIndent()
        }
    }

    private fun generatePaymentBreakdownRows(paymentSplit: org.example.project.data.PaymentSplit): String {
        val dueAmount = paymentSplit.dueAmount
        val isDueAmountNegative = dueAmount < 0
        val totalPayment = paymentSplit.cashAmount + paymentSplit.cardAmount + paymentSplit.bankAmount + paymentSplit.onlineAmount + dueAmount
        
        return buildString {
            if (paymentSplit.cashAmount > 0) {
                append("<div class=\"payment-row\"><span class=\"payment-label\">Cash:</span><span class=\"payment-value\">Rs ${String.format("%.2f", paymentSplit.cashAmount)}</span></div>")
            }
            if (paymentSplit.cardAmount > 0) {
                append("<div class=\"payment-row\"><span class=\"payment-label\">Card:</span><span class=\"payment-value\">Rs ${String.format("%.2f", paymentSplit.cardAmount)}</span></div>")
            }
            if (paymentSplit.bankAmount > 0) {
                append("<div class=\"payment-row\"><span class=\"payment-label\">Bank Transfer:</span><span class=\"payment-value\">Rs ${String.format("%.2f", paymentSplit.bankAmount)}</span></div>")
            }
            if (paymentSplit.onlineAmount > 0) {
                append("<div class=\"payment-row\"><span class=\"payment-label\">Online:</span><span class=\"payment-value\">Rs ${String.format("%.2f", paymentSplit.onlineAmount)}</span></div>")
            }
            if (dueAmount > 0) {
                append("<div class=\"payment-row\"><span class=\"payment-label\">Due:</span><span class=\"payment-value\">Rs ${String.format("%.2f", dueAmount)}</span></div>")
            } else if (dueAmount < 0) {
                append("<div class=\"payment-row due-overpaid\"><span class=\"payment-label\">Due (Overpaid):</span><span class=\"payment-value\">Rs ${String.format("%.2f", dueAmount)}</span></div>")
            }
            
            append("<div class=\"payment-row payment-total\"><span class=\"payment-label\"><strong>Total Payment:</strong></span><span class=\"payment-value\"><strong>Rs ${String.format("%.2f", totalPayment)}</strong></span></div>")
            
            if (isDueAmountNegative) {
                append("<div class=\"payment-warning\">⚠️ Due amount is negative! Payment split amounts need adjustment.</div>")
            }
        }
    }

    private fun getMetalRate(cartItem: CartItem): Double {
        // This is a simplified version - you might want to use actual metal rates
        return when {
            cartItem.product.materialType.contains("gold", ignoreCase = true) -> 5500.0
            cartItem.product.materialType.contains("silver", ignoreCase = true) -> 75.0
            else -> 5500.0
        }
    }

    private fun copyCssFile(outputPath: Path) {
        try {
            println("🔧 Copying CSS file to output directory...")
            val outputDir = outputPath.parent
            val cssFile = outputDir.resolve("bill-styles.css").toFile()
            
            println("📄 CSS file path: $cssFile")
            
            val cssContent = getCssStyles()
            println("📄 CSS content length: ${cssContent.length} characters")
            
            if (cssContent.isBlank()) {
                throw IllegalStateException("CSS content is empty")
            }
            
            FileWriter(cssFile).use { writer ->
                writer.write(cssContent)
            }
            
            // Verify CSS file was created
            if (cssFile.exists() && cssFile.length() > 0) {
                println("✅ CSS file created successfully: ${cssFile.length()} bytes")
            } else {
                throw IllegalStateException("CSS file was not created or is empty")
            }
            
        } catch (e: Exception) {
            println("❌ ========== CSS FILE COPY FAILED ==========")
            println("💥 Exception: ${e.javaClass.simpleName}")
            println("💥 Message: ${e.message}")
            println("💥 Stack trace:")
            e.printStackTrace()
            throw e // Re-throw to indicate failure
        }
    }

    private fun getCssStyles(): String {
        return """
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Arial', sans-serif;
            line-height: 1.6;
            color: #333;
            background-color: #f8f9fa;
        }

        .invoice-container {
            max-width: 800px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            box-shadow: 0 0 20px rgba(0,0,0,0.1);
        }

        .header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            border-bottom: 3px solid #d4af37;
            padding-bottom: 20px;
            margin-bottom: 30px;
        }

        .company-name {
            color: #d4af37;
            font-size: 28px;
            font-weight: bold;
            margin-bottom: 5px;
        }

        .company-details {
            color: #666;
            font-size: 14px;
            margin-bottom: 2px;
        }

        .company-address, .company-contact {
            color: #888;
            font-size: 12px;
            margin-bottom: 2px;
        }

        .invoice-title {
            color: #d4af37;
            font-size: 24px;
            text-align: right;
            margin-bottom: 10px;
        }

        .invoice-details {
            text-align: right;
            font-size: 14px;
        }

        .invoice-details p {
            margin-bottom: 3px;
        }

        .customer-section {
            margin-bottom: 30px;
        }

        .customer-section h3 {
            color: #d4af37;
            margin-bottom: 10px;
            font-size: 16px;
        }

        .customer-details p {
            margin-bottom: 3px;
        }

        .items-section {
            margin-bottom: 30px;
        }

        .items-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 20px;
        }

        .items-table th,
        .items-table td {
            padding: 12px 8px;
            text-align: left;
            border-bottom: 1px solid #ddd;
            vertical-align: top;
        }

        .items-table th {
            background-color: #d4af37;
            color: white;
            font-weight: bold;
            text-transform: uppercase;
            font-size: 12px;
        }

        .items-table tr:nth-child(even) {
            background-color: #f9f9f9;
        }

        .items-table tr:hover {
            background-color: #f5f5f5;
        }

        .item-name {
            font-weight: bold;
            color: #333;
        }

        .weight-details {
            font-size: 11px;
        }

        .weight-breakdown {
            line-height: 1.3;
        }

        .weight-breakdown div {
            margin-bottom: 2px;
        }

        .net-weight {
            font-weight: bold;
            color: #d4af37;
        }

        .charges-breakdown {
            font-size: 11px;
        }

        .charge-item {
            margin-bottom: 3px;
            line-height: 1.2;
        }

        .total-amount {
            font-weight: bold;
            color: #d4af37;
            text-align: right;
        }

        .payment-breakdown-section {
            margin: 30px 0;
            clear: both;
        }

        .payment-breakdown-section h3 {
            color: #d4af37;
            margin-bottom: 15px;
            font-size: 16px;
            border-bottom: 2px solid #d4af37;
            padding-bottom: 5px;
        }

        .payment-breakdown-table {
            background-color: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            border-left: 4px solid #d4af37;
        }

        .payment-row {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px solid #e0e0e0;
        }

        .payment-row:last-child {
            border-bottom: none;
        }

        .payment-label {
            font-weight: 500;
            color: #333;
        }

        .payment-value {
            font-weight: 600;
            color: #333;
        }

        .payment-total {
            font-size: 16px;
            font-weight: bold;
            color: #d4af37;
            border-top: 2px solid #d4af37;
            margin-top: 10px;
            padding-top: 10px;
        }

        .due-overpaid {
            color: #e74c3c;
        }

        .payment-warning {
            background-color: #ffeaa7;
            color: #d63031;
            padding: 10px;
            border-radius: 5px;
            margin-top: 10px;
            font-size: 12px;
            font-weight: bold;
        }

        .totals-section {
            margin-bottom: 30px;
        }

        .totals-table {
            float: right;
            width: 300px;
        }

        .total-row {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px solid #eee;
        }

        .total-row:last-child {
            border-bottom: none;
        }

        .discount-row {
            color: #e74c3c;
        }

        .final-total {
            font-size: 18px;
            font-weight: bold;
            color: #d4af37;
            border-top: 2px solid #d4af37;
            margin-top: 10px;
            padding-top: 10px;
        }

        .total-label {
            font-weight: 500;
        }

        .total-value {
            font-weight: 600;
        }

        .footer {
            text-align: center;
            margin-top: 40px;
            padding-top: 20px;
            border-top: 2px solid #d4af37;
            clear: both;
        }

        .thank-you {
            font-size: 16px;
            color: #d4af37;
            font-weight: bold;
            margin-bottom: 10px;
        }

        .terms {
            font-size: 12px;
            color: #888;
            font-style: italic;
        }

        @media print {
            body {
                background-color: white;
            }
            
            .invoice-container {
                box-shadow: none;
                margin: 0;
                padding: 20px;
            }
        }
        """.trimIndent()
    }
    
    private fun extractKaratFromMaterialType(materialType: String): Int {
        val s = materialType.lowercase()
        if (s.contains("22")) return 22
        if (s.contains("18")) return 18
        if (s.contains("14")) return 14
        if (s.contains("24")) return 24
        val twoDigits = Regex("(\\d{2})").find(s)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (twoDigits != null && twoDigits in listOf(14, 18, 22, 24)) return twoDigits
        return 22
    }
    
    private fun extractSilverPurityFromMaterialType(materialType: String): Int {
        val s = materialType.lowercase()
        if (s.contains("999")) return 999
        if (s.contains("925") || s.contains("92.5")) return 925
        if (s.contains("900") || s.contains("90.0")) return 900
        val threeDigits = Regex("(\\d{3})").find(s)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (threeDigits != null && threeDigits in listOf(900, 925, 999)) return threeDigits
        return 999
    }
}
