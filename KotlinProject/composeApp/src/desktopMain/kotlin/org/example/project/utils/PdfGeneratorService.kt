package org.example.project.utils

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.data.Order
import org.example.project.data.User
import org.example.project.data.InvoiceConfig
import org.example.project.data.MetalRatesManager
import org.example.project.data.extractKaratFromMaterialType
import org.example.project.data.Material
import org.example.project.utils.CurrencyFormatter
import org.example.project.ui.ProductPriceInputs
import org.example.project.ui.calculateProductPrice
import org.example.project.JewelryAppInitializer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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

                val parentDir = outputFile.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    val created = parentDir.mkdirs()
                    println("📁 Created parent directory: $created → ${parentDir.absolutePath}")
                }
                if (parentDir != null && !parentDir.canWrite()) {
                    throw SecurityException("Cannot write to directory: ${parentDir.absolutePath}")
                }

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
        val products = fetchProductDetails(order.items.map { it.productId })
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
        val orderDate = dateFormat.format(Date(order.createdAt))
        
        val subtotal = order.totalProductValue
        val gstAmount = order.gstAmount
        val discountAmount = order.discountAmount
        val totalAmount = order.totalAmount
        val taxableAmount = order.totalProductValue - order.discountAmount
        val gstPercentage = order.gstPercentage.toInt()

        val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
        val allMetalRates = ratesVM.metalRates.value

        fun findGoldRate(karat: Int): Double {
            val goldRates = allMetalRates.filter {
                it.materialType.contains("gold", ignoreCase = true) && it.isActive
            }

            val exactMatch = goldRates.find { it.karat == karat }
            if (exactMatch != null) return exactMatch.pricePerGram

            val typeMatch = goldRates.find {
                val typeLower = it.materialType.lowercase()
                (typeLower.contains("$karat") && (typeLower.contains("k") || typeLower.contains("karat"))) ||
                        (it.karat > 0 && it.karat != karat && it.materialType.contains("$karat", ignoreCase = true))
            }
            if (typeMatch != null) {
                return if (typeMatch.karat > 0 && typeMatch.karat != karat) {
                    typeMatch.pricePerGram * (karat.toDouble() / typeMatch.karat.toDouble())
                } else {
                    typeMatch.pricePerGram
                }
            }

            return 0.0
        }

        fun findSilverRate(purity: Int): Double {
            val silverRates = allMetalRates.filter {
                it.materialType.contains("silver", ignoreCase = true) && it.isActive
            }

            val exactMatch = silverRates.find {
                it.karat == purity || it.materialType.contains("$purity", ignoreCase = true)
            }
            if (exactMatch != null) return exactMatch.pricePerGram

            val typeMatch = silverRates.find {
                it.materialType.contains("$purity", ignoreCase = true)
            }
            if (typeMatch != null) {
                val basePurity = when {
                    typeMatch.materialType.contains("999", ignoreCase = true) -> 999
                    typeMatch.materialType.contains("925", ignoreCase = true) -> 925
                    typeMatch.materialType.contains("900", ignoreCase = true) -> 900
                    typeMatch.karat in listOf(999, 925, 900) -> typeMatch.karat
                    else -> 999
                }
                return if (basePurity > 0 && basePurity != purity) {
                    typeMatch.pricePerGram * (purity.toDouble() / basePurity.toDouble())
                } else {
                    typeMatch.pricePerGram
                }
            }

            return 0.0
        }

        // Fetch materials to get gold and silver rates from types array
        val materials = try {
            val productRepository = JewelryAppInitializer.getProductRepository()
            kotlinx.coroutines.runBlocking {
                productRepository.getMaterials()
            }
        } catch (e: Exception) {
            emptyList()
        }

        // Extract gold and silver rates from materials collection
        val goldRatesMap = mutableMapOf<Int, Double>()
        val silverRatesMap = mutableMapOf<Int, Double>()

        materials.forEach { material ->
            if (material.name.contains("gold", ignoreCase = true)) {
                material.types.forEach { type ->
                    val purity = type.purity.replace("K", "").replace("k", "").toIntOrNull()
                    val rate = type.rate.replace(",", "").toDoubleOrNull()
                    if (purity != null && rate != null && rate > 0) {
                        goldRatesMap[purity] = rate
                    }
                }
            } else if (material.name.contains("silver", ignoreCase = true)) {
                material.types.forEach { type ->
                    val purity = type.purity.replace("%", "").toIntOrNull()
                    val rate = type.rate.replace(",", "").toDoubleOrNull()
                    if (purity != null && rate != null && rate > 0) {
                        silverRatesMap[purity] = rate
                    }
                }
            }
        }

        // Use materials rates if available, otherwise fallback to metal rates
        val gold24kRate = goldRatesMap[24] ?: findGoldRate(24)
        val gold22kRate = goldRatesMap[22] ?: findGoldRate(22)
        val gold18kRate = goldRatesMap[18] ?: findGoldRate(18)
        val gold14kRate = goldRatesMap[14] ?: findGoldRate(14)

        val silver999Rate = silverRatesMap[999] ?: findSilverRate(999)
        val silver925Rate = silverRatesMap[925] ?: findSilverRate(925)
        val silver900Rate = silverRatesMap[900] ?: findSilverRate(900)

        val productTableData = generateTanishqProductTableRows(order.items, products, order, subtotal, discountAmount, gstAmount)
        val totalQty = productTableData.totalQty
        val totalGrossWeight = productTableData.totalGrossWeight
        val totalNetStoneWeight = productTableData.totalNetStoneWeight
        val totalNetMetalWeight = productTableData.totalNetMetalWeight
        val totalGrossProductPrice = productTableData.totalGrossProductPrice
        val totalMakingCharges = productTableData.totalMakingCharges
        val totalGst = productTableData.totalGst
        val totalProductValue = productTableData.totalProductValue
        val totalStoneAmount = productTableData.totalStoneAmount

        val stateCode = if (invoiceConfig.companyGST.length >= 4) {
            invoiceConfig.companyGST.substring(0, 2)
        } else ""
        
        // Extract pincode from address (look for 6-digit number)
        val pincodeRegex = Regex("\\b\\d{6}\\b")
        val pincodeMatch = pincodeRegex.find(invoiceConfig.companyAddress)
        val pincode = pincodeMatch?.value ?: ""
        
        // Try to get CIN from Firestore if available (for now, leave empty if not in model)
        val companyCIN = "" // Will be populated if CIN field exists in InvoiceConfig

        // Build dynamic content sections
        // Seller details: shop name, address, pincode, phone, GSTIN, state code, CIN (each on new line)
        val sellerDetails = buildString {
            append("<p class=\"detail-line\"><strong>${invoiceConfig.companyName.uppercase()}</strong></p>")
            append(if (invoiceConfig.companyAddress.isNotEmpty()) "<p class=\"detail-line\">${invoiceConfig.companyAddress.uppercase()}</p>" else "<p class=\"detail-line\"></p>")
            append(if (pincode.isNotEmpty()) "<p class=\"detail-line\">$pincode</p>" else "<p class=\"detail-line\"></p>")
            append(if (invoiceConfig.companyPhone.isNotEmpty()) "<p class=\"detail-line\">Phone Number: ${invoiceConfig.companyPhone}</p>" else "<p class=\"detail-line\"></p>")
            append(if (invoiceConfig.companyGST.isNotEmpty()) "<p class=\"detail-line\">GSTIN: ${invoiceConfig.companyGST}</p>" else "<p class=\"detail-line\"></p>")
            append(if (stateCode.isNotEmpty()) "<p class=\"detail-line\">State Code: $stateCode</p>" else "<p class=\"detail-line\"></p>")
            append(if (companyCIN.isNotEmpty()) "<p class=\"detail-line\">CIN: $companyCIN</p>" else "<p class=\"detail-line\"></p>")
        }

        // Customer details: customer name, address, phone number, customer ID (each on new line)
        val customerDetails = buildString {
            append("<p class=\"detail-line\"><strong>${customer.name.uppercase()}</strong></p>")
            append(if (customer.address.isNotEmpty()) "<p class=\"detail-line\">${customer.address.uppercase()}</p>" else "<p class=\"detail-line\"></p>")
            append(if (customer.phone.isNotEmpty()) "<p class=\"detail-line\">Phone Number: ${customer.phone}</p>" else "<p class=\"detail-line\"></p>")
            append("<p class=\"detail-line\">Customer ID: ${customer.customerId.ifEmpty { customer.id }}</p>")
        }

        // Standard rates: One line for Gold, One line for Silver
        val standardRates = buildString {
            append("<p class=\"rates-line\"><strong>Gold Rates:</strong> 24K: Rs ${String.format("%.2f", gold24kRate)} | 22K: Rs ${String.format("%.2f", gold22kRate)} | 18K: Rs ${String.format("%.2f", gold18kRate)} | 14K: Rs ${String.format("%.2f", gold14kRate)}</p>")
            append("<p class=\"rates-line\"><strong>Silver Rates:</strong> 999: Rs ${String.format("%.2f", silver999Rate)} | 925: Rs ${String.format("%.2f", silver925Rate)} | 900: Rs ${String.format("%.2f", silver900Rate)}</p>")
        }

        val productTableTotals = """
            <tr class="totals-row">
                <td colspan="3"><strong>Total</strong></td>
                <td><strong>${totalQty}N</strong></td>
                <td><strong>${String.format("%.3f", totalGrossWeight)}</strong></td>
                <td><strong>${if (totalNetStoneWeight > 0) String.format("%.3f", totalNetStoneWeight) else "-"}</strong></td>
                <td><strong>${String.format("%.3f", totalNetMetalWeight)}</strong></td>
                <td><strong>-</strong></td>
                <td><strong>${CurrencyFormatter.formatRupeesNumber(totalGrossProductPrice, includeDecimals = true)}</strong></td>
                <td><strong>${CurrencyFormatter.formatRupeesNumber(totalMakingCharges, includeDecimals = true)}</strong></td>
                <td><strong>${if (totalStoneAmount > 0) CurrencyFormatter.formatRupeesNumber(totalStoneAmount, includeDecimals = true) else "-"}</strong></td>
                <td><strong>${CurrencyFormatter.formatRupeesNumber(totalProductValue, includeDecimals = true)}</strong></td>
            </tr>
        """.trimIndent()

        val paymentTableRows = if (order.paymentSplit != null) {
            generatePaymentTableRows(order.paymentSplit, customer.name)
        } else {
            """
            <tr>
                <td>CASH</td>
                <td>-</td>
                <td>-</td>
                <td>${CurrencyFormatter.formatRupeesNumber(totalAmount, includeDecimals = true)}</td>
            </tr>
            """.trimIndent()
        }

        val paymentTableTotals = buildString {
            append("""
                <tr class="payment-total-row">
                    <td colspan="3"><strong>Total Amount Paid</strong></td>
                    <td><strong>${CurrencyFormatter.formatRupeesNumber(order.paymentSplit?.getTotalPaid() ?: totalAmount, includeDecimals = true)}</strong></td>
                </tr>
            """.trimIndent())
            if (order.paymentSplit?.dueAmount ?: 0.0 > 0) {
                append("""
                <tr class="payment-total-row">
                    <td colspan="3"><strong>Due Amount</strong></td>
                    <td><strong>${CurrencyFormatter.formatRupeesNumber(order.paymentSplit?.dueAmount ?: 0.0, includeDecimals = true)}</strong></td>
                </tr>
                """.trimIndent())
            }
        }

        // Summary rows: Total Product Value, GST Applied and Amount After GST, Discount Applied and Amount After Discount, Total Amount to be Paid (bold)
        val summaryRows = buildString {
            append("""
                <div class="summary-item-row">
                    <span class="summary-label">Total Product Value:</span>
                    <span class="summary-value">${CurrencyFormatter.formatRupeesNumber(totalProductValue, includeDecimals = true)}</span>
                </div>
                <div class="summary-item-row">
                    <span class="summary-label">GST Applied (${gstPercentage}%):</span>
                    <span class="summary-value">${CurrencyFormatter.formatRupeesNumber(gstAmount, includeDecimals = true)}</span>
                </div>
                <div class="summary-item-row">
                    <span class="summary-label">Amount After GST:</span>
                    <span class="summary-value">${CurrencyFormatter.formatRupeesNumber(totalProductValue + gstAmount, includeDecimals = true)}</span>
                </div>
                <div class="summary-item-row">
                    <span class="summary-label">Discount Applied:</span>
                    <span class="summary-value">${CurrencyFormatter.formatRupeesNumber(discountAmount, includeDecimals = true)}</span>
                </div>
                <div class="summary-item-row">
                    <span class="summary-label">Amount After Discount (Net Invoice Value):</span>
                    <span class="summary-value">${CurrencyFormatter.formatRupeesNumber(totalProductValue + gstAmount - discountAmount, includeDecimals = true)}</span>
                </div>
                <div class="summary-item-row total-payable">
                    <span class="summary-label"><strong>Total Amount to be Paid:</strong></span>
                    <span class="summary-value"><strong>${CurrencyFormatter.formatRupeesNumber(totalAmount, includeDecimals = true)}</strong></span>
                </div>
            """.trimIndent())
        }

        val termsAndConditions = """
            <p class="terms-line">1. Net Invoice Value includes Gold value, Product Making charge, Wastage, Vat / Sales tax, and Stone cost (as applicable).</p>
            <p class="terms-line">2. Received above products in good condition.</p>
        """.trimIndent()

        // Load template files
        val htmlTemplate = loadTemplateFile("invoice-template.html")
        val cssContent = loadTemplateFile("invoice-styles.css")

        // Replace placeholders
        return htmlTemplate
            .replace("{{CSS_CONTENT}}", cssContent)
            .replace("{{ORDER_ID}}", order.orderId)
            .replace("{{DOC_ID}}", order.orderId.takeLast(8))
            .replace("{{ORDER_DATE}}", orderDate)
            .replace("{{SELLER_DETAILS}}", sellerDetails)
            .replace("{{CUSTOMER_DETAILS}}", customerDetails)
            .replace("{{STANDARD_RATES}}", standardRates)
            .replace("{{PRODUCT_TABLE_ROWS}}", productTableData.rows)
            .replace("{{PRODUCT_TABLE_TOTALS}}", productTableTotals)
            .replace("{{PAYMENT_TABLE_ROWS}}", paymentTableRows)
            .replace("{{PAYMENT_TABLE_TOTALS}}", paymentTableTotals)
            .replace("{{SUMMARY_ROWS}}", summaryRows)
            .replace("{{AMOUNT_IN_WORDS}}", convertAmountToWords(totalAmount))
            .replace("{{TERMS_AND_CONDITIONS}}", termsAndConditions)
    }

    private data class ProductTableData(
        val rows: String,
        val totalQty: Int,
        val totalGrossWeight: Double,
        val totalNetStoneWeight: Double,
        val totalNetMetalWeight: Double,
        val totalGrossProductPrice: Double,
        val totalMakingCharges: Double,
        val totalGst: Double,
        val totalProductValue: Double,
        val totalStoneAmount: Double = 0.0
    )

    private fun generateTanishqProductTableRows(
        items: List<org.example.project.data.OrderItem>,
        products: List<org.example.project.data.Product>,
        order: Order,
        orderSubtotal: Double,
        orderDiscountAmount: Double,
        orderGstAmount: Double
    ): ProductTableData {
            val metalRates = MetalRatesManager.metalRates.value
        val ratesVM = JewelryAppInitializer.getMetalRateViewModel()

        var totalQty = 0
        var totalGrossWeight = 0.0
        var totalNetStoneWeight = 0.0
        var totalNetMetalWeight = 0.0
        var totalGrossProductPrice = 0.0
        var totalMakingCharges = 0.0
        var totalGst = 0.0
        var totalProductValue = 0.0
        var totalStoneAmount = 0.0

        val rows = items.joinToString("") { item ->
            val product = products.find { it.id == item.productId }
            if (product == null) {
                return@joinToString ""
            }
            
            // Use making % and labour charges from OrderItem (stored in orders collection)
            val makingPercentage = item.makingPercentage
            val labourCharges = item.labourCharges

            val grossWeight = product.totalWeight
            val kundanStones = product.stones.filter { it.name.equals("Kundan", ignoreCase = true) }
            val jarkanStones = product.stones.filter { it.name.equals("Jarkan", ignoreCase = true) }

            val kundanPrice = kundanStones.sumOf { it.amount }
            val kundanWeight = kundanStones.sumOf { it.weight }
            val jarkanPrice = jarkanStones.sumOf { it.amount }
            val jarkanWeight = jarkanStones.sumOf { it.weight }

            // OrderItem no longer has materialType, use product materialType
            val materialType = product.materialType
            val metalKarat = extractKaratFromMaterialType(materialType)
            val collectionRate = try {
                ratesVM.calculateRateForMaterial(product.materialId, product.materialType, metalKarat)
            } catch (e: Exception) { 0.0 }
            val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)

            val materialTypeLower = product.materialType.lowercase()
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
                product.materialType.contains("gold", ignoreCase = true) -> defaultGoldRate
                product.materialType.contains("silver", ignoreCase = true) -> silverRate
                else -> defaultGoldRate
            }

            val priceInputs = ProductPriceInputs(
                grossWeight = grossWeight,
                goldPurity = materialType,
                goldWeight = product.materialWeight.takeIf { it > 0 } ?: grossWeight,
                makingPercentage = makingPercentage,
                labourRatePerGram = item.labourRate, // Use from OrderItem
                kundanPrice = kundanPrice,
                kundanWeight = kundanWeight,
                jarkanPrice = jarkanPrice,
                jarkanWeight = jarkanWeight,
                goldRatePerGram = goldRatePerGram
            )

            val result = calculateProductPrice(priceInputs)

            val quantity = item.quantity
            val netWeight = result.newWeight

            val netMetalWeight = (product.materialWeight.takeIf { it > 0 } ?: grossWeight) * quantity
            val netStoneWeight = (kundanWeight + jarkanWeight) * quantity

            // Base amount (same as receipt screen)
            val baseAmount = result.goldPrice * quantity
            // Use labour charges from OrderItem (stored in orders collection)
            val makingCharges = labourCharges * quantity
            // Item total (same as receipt screen)
            val itemTotal = result.totalProductPrice * quantity

            val taxableAmount = orderSubtotal - orderDiscountAmount
            val itemGst = if (order.isGstIncluded && taxableAmount > 0 && orderSubtotal > 0) {
                (itemTotal / orderSubtotal) * orderGstAmount
            } else {
                0.0
            }

            // Cost value = item total (same as receipt screen)
            val costValue = itemTotal

            totalQty += quantity
            totalGrossWeight += grossWeight * quantity
            totalNetStoneWeight += netStoneWeight
            totalNetMetalWeight += netMetalWeight
            totalGrossProductPrice += baseAmount // Use baseAmount instead of grossProductPrice
            totalMakingCharges += makingCharges
            totalGst += itemGst
            totalProductValue += costValue

            val hsnCode = "71131910"

            val purityDisplay = when {
                materialType.contains("22", ignoreCase = true) -> "G-22Karat"
                materialType.contains("18", ignoreCase = true) -> "G-18Karat"
                materialType.contains("14", ignoreCase = true) -> "G-14Karat"
                materialType.contains("24", ignoreCase = true) -> "G-24Karat"
                materialType.contains("silver", ignoreCase = true) -> "S-${silverPurity}"
                else -> materialType.uppercase()
            }

            val makingPercentDisplay = if (makingPercentage > 0) String.format("%.2f", makingPercentage) else "0.00"
            val stoneAmount = (kundanPrice + jarkanPrice) * quantity
            val stoneAmountDisplay = if (stoneAmount > 0) CurrencyFormatter.formatRupeesNumber(stoneAmount, includeDecimals = true) else "-"
            val netStoneWeightDisplay = if (netStoneWeight > 0) String.format("%.3f", netStoneWeight) else "-"
            
            totalStoneAmount += stoneAmount
            
            """
            <tr>
                <td>${item.barcodeId.ifEmpty { product.id.takeLast(12) }}</td>
                <td>${product.name.uppercase()}</td>
                <td>$purityDisplay</td>
                <td>${quantity}N</td>
                <td>${String.format("%.3f", grossWeight * quantity)}</td>
                <td>$netStoneWeightDisplay</td>
                <td>${String.format("%.3f", netMetalWeight)}</td>
                <td>$makingPercentDisplay%</td>
                <td>${CurrencyFormatter.formatRupeesNumber(baseAmount, includeDecimals = true)}</td>
                <td>${CurrencyFormatter.formatRupeesNumber(makingCharges, includeDecimals = true)}</td>
                <td>$stoneAmountDisplay</td>
                <td>${CurrencyFormatter.formatRupeesNumber(costValue, includeDecimals = true)}</td>
            </tr>
            """.trimIndent()
        }

        return ProductTableData(
            rows = rows,
            totalQty = totalQty,
            totalGrossWeight = totalGrossWeight,
            totalNetStoneWeight = totalNetStoneWeight,
            totalNetMetalWeight = totalNetMetalWeight,
            totalGrossProductPrice = totalGrossProductPrice,
            totalMakingCharges = totalMakingCharges,
            totalGst = totalGst,
            totalProductValue = totalProductValue,
            totalStoneAmount = totalStoneAmount
        )
    }

    private fun generatePaymentTableRows(paymentSplit: org.example.project.data.PaymentSplit, customerName: String): String {
        return buildString {
            if (paymentSplit.cash > 0) {
                append("""
                <tr>
                    <td>CASH</td>
                    <td>-</td>
                    <td>-</td>
                    <td>${CurrencyFormatter.formatRupeesNumber(paymentSplit.cash, includeDecimals = true)}</td>
                </tr>
                """.trimIndent())
            }
            if (paymentSplit.bank > 0) {
                append("""
                <tr>
                    <td>BANK/CARD/ONLINE</td>
                    <td>-</td>
                    <td>-</td>
                    <td>${CurrencyFormatter.formatRupeesNumber(paymentSplit.bank, includeDecimals = true)}</td>
                </tr>
                """.trimIndent())
            }
            if (paymentSplit.dueAmount > 0) {
                append("""
                <tr>
                    <td>DUE</td>
                    <td>-</td>
                    <td>-</td>
                    <td>${CurrencyFormatter.formatRupeesNumber(paymentSplit.dueAmount, includeDecimals = true)}</td>
                </tr>
                """.trimIndent())
            }
        }
    }

    private fun loadTemplateFile(filename: String): String {
        return try {
            // Try as resource first (if packaged in JAR)
            val resourceContent = try {
                val resourceStream = javaClass.getResourceAsStream("/org/example/project/utils/$filename")
                    ?: javaClass.classLoader.getResourceAsStream("org/example/project/utils/$filename")
                resourceStream?.use { it.bufferedReader().readText() }
            } catch (e: Exception) { null }
            
            if (resourceContent != null) {
                return resourceContent
            }
            
            // Try multiple file system paths
            val userDir = System.getProperty("user.dir")
            println("🔍 Looking for template file: $filename")
            println("   Current working directory: $userDir")
            
            val possiblePaths = mutableListOf<File?>()
            
            // Determine project root - handle case where working dir is inside composeApp
            val projectRoot = when {
                userDir.endsWith("/composeApp") || userDir.endsWith("\\composeApp") -> {
                    File(userDir).parentFile?.absolutePath ?: userDir
                }
                userDir.contains("/composeApp/") || userDir.contains("\\composeApp\\") -> {
                    // Extract project root by going up from composeApp
                    val composeAppIndex = userDir.indexOf("/composeApp/")
                    if (composeAppIndex > 0) {
                        userDir.substring(0, composeAppIndex)
                    } else {
                        userDir
                    }
                }
                else -> userDir
            }
            
            // 1. Try from project root (most reliable)
            val fromProjectRoot = File("$projectRoot/composeApp/src/desktopMain/kotlin/org/example/project/utils/$filename")
            possiblePaths.add(fromProjectRoot)
            println("   Checking: ${fromProjectRoot.absolutePath} (exists: ${fromProjectRoot.exists()})")
            
            // 2. Try relative to current working directory (if we're in project root)
            val relativePath = File("composeApp/src/desktopMain/kotlin/org/example/project/utils/$filename")
            possiblePaths.add(relativePath)
            println("   Checking: ${relativePath.absolutePath} (exists: ${relativePath.exists()})")
            
            // 3. Try if we're already in composeApp directory
            val fromComposeApp = File("src/desktopMain/kotlin/org/example/project/utils/$filename")
            possiblePaths.add(fromComposeApp)
            println("   Checking: ${fromComposeApp.absolutePath} (exists: ${fromComposeApp.exists()})")
            
            // 4. Try absolute from user directory (only if not already in composeApp to avoid duplicate)
            if (!userDir.endsWith("/composeApp") && !userDir.contains("/composeApp/")) {
                val absolutePath = File("$userDir/composeApp/src/desktopMain/kotlin/org/example/project/utils/$filename")
                possiblePaths.add(absolutePath)
                println("   Checking: ${absolutePath.absolutePath} (exists: ${absolutePath.exists()})")
            }
            
            // 6. Try to find from class location (for development/build)
            try {
                val codeSource = javaClass.protectionDomain?.codeSource?.location
                if (codeSource != null) {
                    val classFile = File(codeSource.toURI().path)
                    if (classFile.absolutePath.contains("/build/")) {
                        val buildProjectRoot = classFile.absolutePath.substringBefore("/build/")
                        val classPath = File("$buildProjectRoot/composeApp/src/desktopMain/kotlin/org/example/project/utils/$filename")
                        possiblePaths.add(classPath)
                        println("   Checking: ${classPath.absolutePath} (exists: ${classPath.exists()})")
                    }
                }
            } catch (e: Exception) { possiblePaths.add(null) }
            
            // 7. Try to find utils directory relative to this class file
            try {
                val classResource = javaClass.getResource("${javaClass.simpleName}.class")
                if (classResource != null && classResource.protocol == "file") {
                    val classPath = File(classResource.toURI().path)
                    val utilsDir = classPath.parentFile
                    if (utilsDir != null && utilsDir.name == "utils") {
                        val utilsPath = File(utilsDir, filename)
                        possiblePaths.add(utilsPath)
                        println("   Checking: ${utilsPath.absolutePath} (exists: ${utilsPath.exists()})")
                    }
                }
            } catch (e: Exception) { possiblePaths.add(null) }
            
            // 8. Try to find project root using findProjectRoot helper
            val foundProjectRoot = findProjectRoot()
            if (foundProjectRoot != null) {
                val foundPath = File("$foundProjectRoot/composeApp/src/desktopMain/kotlin/org/example/project/utils/$filename")
                possiblePaths.add(foundPath)
                println("   Checking: ${foundPath.absolutePath} (exists: ${foundPath.exists()})")
            }
            
            // Find first existing file
            for (path in possiblePaths) {
                if (path != null && path.exists() && path.isFile) {
                    println("✅ Found template file: ${path.absolutePath}")
                    return path.readText()
                }
            }
            
            val checkedPaths = possiblePaths.filterNotNull().joinToString(", ") { it.absolutePath }
            throw IllegalStateException("Template file not found: $filename. Checked paths: $checkedPaths")
        } catch (e: Exception) {
            println("❌ Error loading template file: $filename")
            println("   Error: ${e.message}")
            e.printStackTrace()
            throw IllegalStateException("Could not load template file: $filename - ${e.message}", e)
        }
    }
    
    private fun findProjectRoot(): String? {
        var currentDir = File(System.getProperty("user.dir"))
        val maxDepth = 10
        var depth = 0
        
        while (depth < maxDepth && currentDir.exists()) {
            // Check for common project markers
            val markers = listOf("build.gradle.kts", "build.gradle", "settings.gradle.kts", "settings.gradle", ".git")
            if (markers.any { File(currentDir, it).exists() }) {
                return currentDir.absolutePath
            }
            currentDir = currentDir.parentFile ?: break
            depth++
        }
        return null
    }
    
    private fun convertAmountToWords(amount: Double): String {
        val intAmount = amount.toInt()
        val decimalPart = ((amount - intAmount) * 100).toInt()

        fun convertNumberToWords(num: Int): String {
            val ones = arrayOf("", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
                "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
            val tens = arrayOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

            return when {
                num == 0 -> ""
                num < 20 -> ones[num]
                num < 100 -> {
                    val ten = num / 10
                    val one = num % 10
                    if (one == 0) tens[ten] else "${tens[ten]} ${ones[one]}"
                }
                num < 1000 -> {
                    val hundred = num / 100
                    val remainder = num % 100
                    if (remainder == 0) "${ones[hundred]} hundred"
                    else "${ones[hundred]} hundred ${convertNumberToWords(remainder)}"
                }
                num < 100000 -> {
                    val thousand = num / 1000
                    val remainder = num % 1000
                    if (remainder == 0) "${convertNumberToWords(thousand)} thousand"
                    else "${convertNumberToWords(thousand)} thousand ${convertNumberToWords(remainder)}"
                }
                num < 10000000 -> {
                    val lakh = num / 100000
                    val remainder = num % 100000
                    if (remainder == 0) "${convertNumberToWords(lakh)} lakh"
                    else "${convertNumberToWords(lakh)} lakh ${convertNumberToWords(remainder)}"
                }
                else -> {
                    val crore = num / 10000000
                    val remainder = num % 10000000
                    if (remainder == 0) "${convertNumberToWords(crore)} crore"
                    else "${convertNumberToWords(crore)} crore ${convertNumberToWords(remainder)}"
            }
        }
    }
    
        val words = convertNumberToWords(intAmount)
        val result = if (words.isEmpty()) "Zero" else words.split(" ").joinToString(" ") {
            it.replaceFirstChar { char -> char.uppercaseChar() }
        }
        val paise = if (decimalPart > 0) " and ${decimalPart}/100" else ""
        return "Rupees $result$paise Only"
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