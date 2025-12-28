package org.example.project.utils

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.data.Order
import org.example.project.data.User
import org.example.project.data.CartItem
import org.example.project.data.InvoiceConfig
import org.example.project.data.MetalRatesManager
import org.example.project.data.extractKaratFromMaterialType
import org.example.project.utils.CurrencyFormatter
import org.example.project.ui.ProductPriceInputs
import org.example.project.ui.calculateProductPrice
import org.example.project.JewelryAppInitializer
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfGeneratorService {

    suspend fun generateInvoicePDF(
        order: Order,
        customer: User,
        outputFile: File,
        invoiceConfig: InvoiceConfig = InvoiceConfig()
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔄 PDF generation started → ${outputFile.absolutePath}")
                val htmlContent = generateInvoiceHtml(order, customer, invoiceConfig)

                // Ensure output directory exists and is writable
                val parentDir = outputFile.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    val created = parentDir.mkdirs()
                    println("📁 Created parent directory: $created → ${parentDir.absolutePath}")
                }
                if (parentDir != null && !parentDir.canWrite()) {
                    throw SecurityException("Cannot write to directory: ${parentDir.absolutePath}")
                }

                // Try primary engine: OpenHTMLToPDF
                var pdfGenerated = false
                try {
                    println("🛠 Using OpenHTMLToPDF (primary)...")
                    FileOutputStream(outputFile).use { os ->
                        val builder = PdfRendererBuilder()
                        builder.useFastMode()
                        builder.withHtmlContent(htmlContent, null)
                        builder.toStream(os)
                        builder.run()
                    }
                    pdfGenerated = true
                    println("✅ OpenHTMLToPDF succeeded")
                } catch (engineError: Exception) {
                    println("⚠️ OpenHTMLToPDF failed: ${engineError.message}")
                    engineError.printStackTrace()
                    
                    // Fallback: Flying Saucer
                    try {
                        println("🛠 Falling back to Flying Saucer (iTextRenderer)...")
                        val renderer = org.xhtmlrenderer.pdf.ITextRenderer()
                        renderer.setDocumentFromString(htmlContent)
                        renderer.layout()
                        FileOutputStream(outputFile).use { os ->
                            renderer.createPDF(os)
                        }
                        pdfGenerated = true
                        println("✅ Flying Saucer fallback succeeded")
                    } catch (fallbackError: Exception) {
                        println("💥 Flying Saucer fallback failed: ${fallbackError.message}")
                        fallbackError.printStackTrace()
                        throw Exception("Both PDF engines failed. OpenHTMLToPDF: ${engineError.message}, Flying Saucer: ${fallbackError.message}")
                    }
                }

                if (!outputFile.exists() || outputFile.length() <= 0) {
                    throw IllegalStateException("PDF file was not created or is empty: ${outputFile.absolutePath}")
                }

                println("✅ PDF generated → ${outputFile.absolutePath} (${outputFile.length()} bytes)")
                Result.success(outputFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun generateInvoiceHtml(order: Order, customer: User, invoiceConfig: InvoiceConfig): String {
        // Fetch product details for PDF generation
        val products = fetchProductDetails(order.items.map { it.productId })
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val orderDate = dateFormat.format(Date(order.createdAt))
        
        val subtotal = order.subtotal
        val gstAmount = order.gstAmount
        val discountAmount = order.discountAmount
        val totalAmount = order.totalAmount
        val gstPercentage = if (subtotal > 0) ((gstAmount / subtotal) * 100).toInt() else 0

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            <title>Invoice ${order.orderId}</title>
            <style>
                ${getPdfStyles()}
            </style>
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
                            <p><strong>Invoice No:</strong> ${order.orderId}</p>
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
                            ${generateDetailedItemsRows(order.items, products)}
                        </tbody>
                    </table>
                </div>

                <!-- Totals Section -->
                <div class="totals-section">
                    <div class="totals-table">
                        <div class="total-row">
                            <span class="total-label">Subtotal:</span>
                            <span class="total-value">Rs ${CurrencyFormatter.formatRupeesNumber(subtotal, includeDecimals = true)}</span>
                        </div>
                        ${if (gstAmount > 0) """
                        <div class="total-row">
                            <span class="total-label">GST (${gstPercentage}%):</span>
                            <span class="total-value">Rs ${CurrencyFormatter.formatRupeesNumber(gstAmount, includeDecimals = true)}</span>
                        </div>
                        """ else ""}
                        ${if (discountAmount > 0) """
                        <div class="total-row discount-row">
                            <span class="total-label">Discount:</span>
                            <span class="total-value">-Rs ${CurrencyFormatter.formatRupeesNumber(discountAmount, includeDecimals = true)}</span>
                        </div>
                        """ else ""}
                        <div class="total-row final-total">
                            <span class="total-label"><strong>TOTAL:</strong></span>
                            <span class="total-value"><strong>Rs ${CurrencyFormatter.formatRupeesNumber(totalAmount, includeDecimals = true)}</strong></span>
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
            val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
            val currentProduct = item.product
            
            // Use ProductPriceCalculator logic (same as ReceiptScreen)
            val grossWeight = if (item.grossWeight > 0) item.grossWeight else currentProduct.totalWeight
            val makingPercentage = currentProduct.makingPercent
            val labourRatePerGram = currentProduct.labourRate
            
            // Extract kundan and jarkan from stones array
            val kundanStones = currentProduct.stones.filter { it.name.equals("Kundan", ignoreCase = true) }
            val jarkanStones = currentProduct.stones.filter { it.name.equals("Jarkan", ignoreCase = true) }
            
            // Sum all Kundan prices and weights
            val kundanPrice = kundanStones.sumOf { it.amount }
            val kundanWeight = kundanStones.sumOf { it.weight }
            
            // Sum all Jarkan prices and weights
            val jarkanPrice = jarkanStones.sumOf { it.amount }
            val jarkanWeight = jarkanStones.sumOf { it.weight }
            
            // Get material rate (fetched from metal rates, same as ProductPriceCalculator)
            val metalKarat = if (item.metal.isNotEmpty()) {
                item.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(currentProduct.materialType)
            } else {
                extractKaratFromMaterialType(currentProduct.materialType)
            }
            val collectionRate = try {
                ratesVM.calculateRateForMaterial(currentProduct.materialId, currentProduct.materialType, metalKarat)
            } catch (e: Exception) { 0.0 }
            val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
            val goldRate = if (item.customGoldRate > 0) item.customGoldRate else defaultGoldRate
            // Extract silver purity from material type
            val materialTypeLower = currentProduct.materialType.lowercase()
            val silverPurity = when {
                materialTypeLower.contains("999") -> 999
                materialTypeLower.contains("925") || materialTypeLower.contains("92.5") -> 925
                materialTypeLower.contains("900") || materialTypeLower.contains("90.0") -> 900
                else -> {
                    val threeDigits = Regex("(\\d{3})").find(materialTypeLower)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    if (threeDigits != null && threeDigits in listOf(900, 925, 999)) threeDigits else 999
                }
            }
            val silverRate = metalRates.getSilverRateForPurity(silverPurity)
            val goldRatePerGram = if (collectionRate > 0) collectionRate else when {
                currentProduct.materialType.contains("gold", ignoreCase = true) -> goldRate
                currentProduct.materialType.contains("silver", ignoreCase = true) -> silverRate
                else -> goldRate
            }
            
            // Build ProductPriceInputs (same structure as ProductPriceCalculator)
            val priceInputs = ProductPriceInputs(
                grossWeight = grossWeight,
                goldPurity = currentProduct.materialType,
                goldWeight = currentProduct.materialWeight.takeIf { it > 0 } ?: grossWeight,
                makingPercentage = makingPercentage,
                labourRatePerGram = labourRatePerGram,
                kundanPrice = kundanPrice,
                kundanWeight = kundanWeight,
                jarkanPrice = jarkanPrice,
                jarkanWeight = jarkanWeight,
                goldRatePerGram = goldRatePerGram
            )
            
            // Use the same calculation function as ProductPriceCalculator
            val result = calculateProductPrice(priceInputs)
            
            val quantity = item.quantity
            val baseAmount = result.goldPrice * quantity
            val labourCharges = result.labourCharges * quantity
            val stoneAmount = (result.kundanPrice + result.jarkanPrice) * quantity
            val totalCharges = result.totalProductPrice * quantity
            
            // For display: use result.newWeight (same as ReceiptScreen)
            val netWeight = result.newWeight
            val lessWeight = grossWeight - netWeight
            
            println("📄 PDF SERVICE CALCULATION for ${item.product.name}:")
            println("   - Base amount: ${baseAmount}")
            println("   - Labour charges: ${labourCharges}")
            println("   - Stone amount: ${stoneAmount}")
            println("   - Total charges: ${totalCharges}")
            
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
                <td>Rs ${CurrencyFormatter.formatRupeesNumber(goldRatePerGram, includeDecimals = true)}</td>
                <td class="charges-breakdown">
                    <div class="charge-item">Base: Rs ${CurrencyFormatter.formatRupeesNumber(baseAmount, includeDecimals = true)}</div>
                    ${if (labourCharges > 0) "<div class=\"charge-item\">Labour: Rs ${CurrencyFormatter.formatRupeesNumber(labourCharges, includeDecimals = true)}</div>" else ""}
                    ${if (stoneAmount > 0) "<div class=\"charge-item\">Stone: Rs ${CurrencyFormatter.formatRupeesNumber(stoneAmount, includeDecimals = true)}</div>" else ""}
                </td>
                <td class="total-amount">Rs ${CurrencyFormatter.formatRupeesNumber(totalCharges, includeDecimals = true)}</td>
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
                append("<div class=\"payment-row\"><span class=\"payment-label\">Cash:</span><span class=\"payment-value\">Rs ${CurrencyFormatter.formatRupeesNumber(paymentSplit.cashAmount, includeDecimals = true)}</span></div>")
            }
            if (paymentSplit.cardAmount > 0) {
                append("<div class=\"payment-row\"><span class=\"payment-label\">Card:</span><span class=\"payment-value\">Rs ${CurrencyFormatter.formatRupeesNumber(paymentSplit.cardAmount, includeDecimals = true)}</span></div>")
            }
            if (paymentSplit.bankAmount > 0) {
                append("<div class=\"payment-row\"><span class=\"payment-label\">Bank Transfer:</span><span class=\"payment-value\">Rs ${CurrencyFormatter.formatRupeesNumber(paymentSplit.bankAmount, includeDecimals = true)}</span></div>")
            }
            if (paymentSplit.onlineAmount > 0) {
                append("<div class=\"payment-row\"><span class=\"payment-label\">Online:</span><span class=\"payment-value\">Rs ${CurrencyFormatter.formatRupeesNumber(paymentSplit.onlineAmount, includeDecimals = true)}</span></div>")
            }
            if (dueAmount > 0) {
                append("<div class=\"payment-row\"><span class=\"payment-label\">Due:</span><span class=\"payment-value\">Rs ${CurrencyFormatter.formatRupeesNumber(dueAmount, includeDecimals = true)}</span></div>")
            } else if (dueAmount < 0) {
                append("<div class=\"payment-row due-overpaid\"><span class=\"payment-label\">Due (Overpaid):</span><span class=\"payment-value\">Rs ${CurrencyFormatter.formatRupeesNumber(dueAmount, includeDecimals = true)}</span></div>")
            }
            
            append("<div class=\"payment-row payment-total\"><span class=\"payment-label\"><strong>Total Payment:</strong></span><span class=\"payment-value\"><strong>Rs ${CurrencyFormatter.formatRupeesNumber(totalPayment, includeDecimals = true)}</strong></span></div>")
            
            if (isDueAmountNegative) {
                append("<div class=\"payment-warning\">⚠️ Due amount is negative! Payment split amounts need adjustment.</div>")
            }
        }
    }

    private fun getPdfStyles(): String {
        return """
        @page {
            size: A4;
            margin: 20mm;
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Helvetica', 'Arial', sans-serif;
            font-size: 10pt;
            line-height: 1.4;
            color: #333;
        }

        .invoice-container {
            max-width: 100%;
            background: white;
        }

        .header {
            display: table;
            width: 100%;
            border-bottom: 3px solid #d4af37;
            padding-bottom: 15px;
            margin-bottom: 20px;
        }

        .company-info {
            display: table-cell;
            width: 60%;
            vertical-align: top;
        }

        .company-name {
            color: #d4af37;
            font-size: 20pt;
            font-weight: bold;
            margin-bottom: 5px;
        }

        .company-details {
            color: #666;
            font-size: 12pt;
            margin-bottom: 2px;
        }

        .company-address, .company-contact {
            color: #888;
            font-size: 10pt;
            margin-bottom: 2px;
        }

        .invoice-info {
            display: table-cell;
            width: 40%;
            text-align: right;
            vertical-align: top;
        }

        .invoice-title {
            color: #d4af37;
            font-size: 18pt;
            margin-bottom: 10px;
        }

        .invoice-details {
            font-size: 10pt;
        }

        .invoice-details p {
            margin-bottom: 3px;
        }

        .customer-section {
            margin-bottom: 20px;
        }

        .customer-section h3 {
            color: #d4af37;
            margin-bottom: 8px;
            font-size: 12pt;
        }

        .customer-details p {
            margin-bottom: 3px;
            font-size: 10pt;
        }

        .rate-info {
            background-color: #f8f9fa;
            padding: 8px;
            border-left: 4px solid #d4af37;
            margin-bottom: 15px;
            font-size: 9pt;
        }

        .items-section {
            margin-bottom: 20px;
        }

        .items-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 15px;
            font-size: 9pt;
        }

        .items-table th,
        .items-table td {
            padding: 6px 4px;
            text-align: left;
            border-bottom: 1px solid #ddd;
            vertical-align: top;
        }

        .items-table th {
            background-color: #d4af37;
            color: white;
            font-weight: bold;
            text-transform: uppercase;
            font-size: 8pt;
        }

        .items-table tr:nth-child(even) {
            background-color: #f9f9f9;
        }

        .item-name {
            font-weight: bold;
            color: #333;
        }

        .weight-details {
            font-size: 8pt;
        }

        .weight-breakdown {
            line-height: 1.2;
        }

        .weight-breakdown div {
            margin-bottom: 1px;
        }

        .net-weight {
            font-weight: bold;
            color: #d4af37;
        }

        .charges-breakdown {
            font-size: 8pt;
        }

        .charge-item {
            margin-bottom: 2px;
            line-height: 1.1;
        }

        .total-amount {
            font-weight: bold;
            color: #d4af37;
            text-align: right;
        }

        .totals-section {
            margin-bottom: 20px;
        }

        .totals-table {
            float: right;
            width: 250px;
        }

        .total-row {
            display: flex;
            justify-content: space-between;
            padding: 6px 0;
            border-bottom: 1px solid #eee;
            font-size: 10pt;
        }

        .total-row:last-child {
            border-bottom: none;
        }

        .discount-row {
            color: #e74c3c;
        }

        .final-total {
            font-size: 12pt;
            font-weight: bold;
            color: #d4af37;
            border-top: 2px solid #d4af37;
            margin-top: 8px;
            padding-top: 8px;
        }

        .total-label {
            font-weight: 500;
        }

        .total-value {
            font-weight: 600;
        }

        .payment-breakdown-section {
            margin: 20px 0;
            clear: both;
        }

        .payment-breakdown-section h3 {
            color: #d4af37;
            margin-bottom: 10px;
            font-size: 12pt;
            border-bottom: 2px solid #d4af37;
            padding-bottom: 3px;
        }

        .payment-breakdown-table {
            background-color: #f8f9fa;
            padding: 10px;
            border-radius: 5px;
            border-left: 4px solid #d4af37;
        }

        .payment-row {
            display: flex;
            justify-content: space-between;
            padding: 4px 0;
            border-bottom: 1px solid #e0e0e0;
            font-size: 9pt;
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
            font-size: 11pt;
            font-weight: bold;
            color: #d4af37;
            border-top: 2px solid #d4af37;
            margin-top: 8px;
            padding-top: 8px;
        }

        .due-overpaid {
            color: #e74c3c;
        }

        .payment-warning {
            background-color: #ffeaa7;
            color: #d63031;
            padding: 6px;
            border-radius: 3px;
            margin-top: 8px;
            font-size: 8pt;
            font-weight: bold;
        }

        .footer {
            text-align: center;
            margin-top: 30px;
            padding-top: 15px;
            border-top: 2px solid #d4af37;
            clear: both;
        }

        .thank-you {
            font-size: 12pt;
            color: #d4af37;
            font-weight: bold;
            margin-bottom: 8px;
        }

        .terms {
            font-size: 9pt;
            color: #888;
            font-style: italic;
        }

        /* Page break control */
        tr {
            page-break-inside: avoid;
        }

        thead {
            display: table-header-group;
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
    
    private fun generateSimplifiedItemsRows(items: List<org.example.project.data.OrderItem>): String {
        return items.joinToString("") { item ->
            """
            <tr>
                <td>Product ${item.productId}</td>
                <td>Barcode: ${item.barcodeId}</td>
                <td>${item.quantity}</td>
                <td>-</td>
                <td>-</td>
                <td>-</td>
                <td>-</td>
            </tr>
            """.trimIndent()
        }
    }
    
    private fun generateDetailedItemsRows(items: List<org.example.project.data.OrderItem>, products: List<org.example.project.data.Product>): String {
        return items.joinToString("") { item ->
            val product = products.find { it.id == item.productId }
            if (product != null) {
                // Use ProductPriceCalculator logic (same as ReceiptScreen)
                val metalRates = MetalRatesManager.metalRates.value
                val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
                val currentProduct = product
                
                // Use ProductPriceCalculator logic
                val grossWeight = currentProduct.totalWeight
                val makingPercentage = currentProduct.makingPercent
                val labourRatePerGram = currentProduct.labourRate
                
                // Extract kundan and jarkan from stones array
                val kundanStones = currentProduct.stones.filter { it.name.equals("Kundan", ignoreCase = true) }
                val jarkanStones = currentProduct.stones.filter { it.name.equals("Jarkan", ignoreCase = true) }
                
                // Sum all Kundan prices and weights
                val kundanPrice = kundanStones.sumOf { it.amount }
                val kundanWeight = kundanStones.sumOf { it.weight }
                
                // Sum all Jarkan prices and weights
                val jarkanPrice = jarkanStones.sumOf { it.amount }
                val jarkanWeight = jarkanStones.sumOf { it.weight }
                
                // Get material rate (fetched from metal rates, same as ProductPriceCalculator)
                val materialType = if (item.materialType.isNotBlank()) item.materialType else currentProduct.materialType
                val metalKarat = extractKaratFromMaterialType(materialType)
                val collectionRate = try {
                    ratesVM.calculateRateForMaterial(currentProduct.materialId, currentProduct.materialType, metalKarat)
                } catch (e: Exception) { 0.0 }
                val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
                // Extract silver purity from material type
                val materialTypeLower = currentProduct.materialType.lowercase()
                val silverPurity = when {
                    materialTypeLower.contains("999") -> 999
                    materialTypeLower.contains("925") || materialTypeLower.contains("92.5") -> 925
                    materialTypeLower.contains("900") || materialTypeLower.contains("90.0") -> 900
                    else -> {
                        val threeDigits = Regex("(\\d{3})").find(materialTypeLower)?.groupValues?.getOrNull(1)?.toIntOrNull()
                        if (threeDigits != null && threeDigits in listOf(900, 925, 999)) threeDigits else 999
                    }
                }
                val silverRate = metalRates.getSilverRateForPurity(silverPurity)
                val goldRatePerGram = if (collectionRate > 0) collectionRate else when {
                    currentProduct.materialType.contains("gold", ignoreCase = true) -> defaultGoldRate
                    currentProduct.materialType.contains("silver", ignoreCase = true) -> silverRate
                    else -> defaultGoldRate
                }
                
                // Build ProductPriceInputs (same structure as ProductPriceCalculator)
                val priceInputs = ProductPriceInputs(
                    grossWeight = grossWeight,
                    goldPurity = materialType,
                    goldWeight = currentProduct.materialWeight.takeIf { it > 0 } ?: grossWeight,
                    makingPercentage = makingPercentage,
                    labourRatePerGram = labourRatePerGram,
                    kundanPrice = kundanPrice,
                    kundanWeight = kundanWeight,
                    jarkanPrice = jarkanPrice,
                    jarkanWeight = jarkanWeight,
                    goldRatePerGram = goldRatePerGram
                )
                
                // Use the same calculation function as ProductPriceCalculator
                val result = calculateProductPrice(priceInputs)
                
                val quantity = item.quantity
                val baseAmount = result.goldPrice * quantity
                val labourCharges = result.labourCharges * quantity
                val stoneAmount = (result.kundanPrice + result.jarkanPrice) * quantity
                val totalCharges = result.totalProductPrice * quantity
                
                // For display: use result.newWeight (same as ReceiptScreen)
                val netWeight = result.newWeight
                val lessWeight = grossWeight - netWeight
                
                """
                <tr>
                    <td class="item-name">${product.name}</td>
                    <td>${materialType}</td>
                    <td class="weight-details">
                        <div class="weight-breakdown">
                            <div>Gross: ${String.format("%.2f", grossWeight)}g</div>
                            <div>Less: ${String.format("%.2f", lessWeight)}g</div>
                            <div class="net-weight">Net: ${String.format("%.2f", netWeight)}g</div>
                        </div>
                    </td>
                    <td>${quantity}</td>
                    <td>Rs ${CurrencyFormatter.formatRupeesNumber(goldRatePerGram, includeDecimals = true)}</td>
                    <td class="charges-breakdown">
                        <div class="charge-item">Base: Rs ${CurrencyFormatter.formatRupeesNumber(baseAmount, includeDecimals = true)}</div>
                        ${if (labourCharges > 0) "<div class=\"charge-item\">Labour: Rs ${CurrencyFormatter.formatRupeesNumber(labourCharges, includeDecimals = true)}</div>" else ""}
                        ${if (stoneAmount > 0) "<div class=\"charge-item\">Stone: Rs ${CurrencyFormatter.formatRupeesNumber(stoneAmount, includeDecimals = true)}</div>" else ""}
                    </td>
                    <td class="total-amount">Rs ${CurrencyFormatter.formatRupeesNumber(totalCharges, includeDecimals = true)}</td>
                </tr>
                """.trimIndent()
            } else {
                generateSimplifiedItemsRows(listOf(item))
            }
        }
    }
    
    private fun parseWeight(weightString: String): Double {
        return try {
            weightString.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
    
    private fun fetchProductDetails(productIds: List<String>): List<org.example.project.data.Product> {
        return try {
            val productRepository = org.example.project.JewelryAppInitializer.getProductRepository()
            if (productRepository != null) {
                kotlinx.coroutines.runBlocking {
                    val allProducts = productRepository.getAllProducts()
                    allProducts.filter { productIds.contains(it.id) }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
