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
import org.example.project.data.StoreInfoRepository
import org.example.project.utils.CurrencyFormatter
import org.example.project.ui.ProductPriceInputs
import org.example.project.ui.calculateProductPrice
import org.example.project.JewelryAppInitializer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.Base64
import java.security.MessageDigest
import kotlin.math.roundToInt

class PdfGeneratorService {

    suspend fun generateInvoicePDF(
        order: Order,
        customer: User,
        outputFile: File,
        invoiceConfig: InvoiceConfig = InvoiceConfig(),
        cartItems: List<org.example.project.data.CartItem>? = null // Optional: use CartItems from receipt screen
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔄 PDF generation started (OpenPDF) → ${outputFile.absolutePath}")

                val parentDir = outputFile.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    val created = parentDir.mkdirs()
                    println("📁 Created parent directory: $created → ${parentDir.absolutePath}")
                }
                if (parentDir != null && !parentDir.canWrite()) {
                    throw SecurityException("Cannot write to directory: ${parentDir.absolutePath}")
                }

                       // Fetch required data
                       val storeInfo = StoreInfoRepository.getStoreInfo()
                       val products = fetchProductDetails(order.items.map { it.productId })
                           .associateBy { it.id }

                       // Calculate invoice (freeze all numbers)
                       // IRN and ACK are generated inside InvoiceCalculator
                       // If cartItems are provided, use them for accurate item details (from receipt screen)
                       val calculator = org.example.project.invoice.calculation.InvoiceCalculator()
                       val invoice = calculator.calculate(order, customer, storeInfo, products, cartItems)
                
                // Render PDF using OpenPDF
                val renderer = org.example.project.invoice.pdf.InvoiceRenderer()
                renderer.render(invoice, outputFile)

                if (!outputFile.exists() || outputFile.length() <= 0) {
                    throw IllegalStateException("PDF file was not created or is empty: ${outputFile.absolutePath}")
                }

                println("✅ PDF generated → ${outputFile.absolutePath} (${outputFile.length()} bytes)")
                Result.success(outputFile)
            } catch (e: Exception) {
                println("❌ PDF generation failed: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    fun generateInvoiceHtml(order: Order, customer: User, invoiceConfig: InvoiceConfig): String {
        val products = fetchProductDetails(order.items.map { it.productId })

        // Fetch store info from Firestore
        val storeInfo = kotlinx.coroutines.runBlocking {
            StoreInfoRepository.getStoreInfo()
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val orderDate = dateFormat.format(Date(order.createdAt))
        val ackDate = dateFormat.format(Date(order.createdAt))

        val irn = generateIRN(order.orderId, order.createdAt)
        val ackNo = order.orderId.take(15)

        val subtotal = order.totalProductValue
        val exchangeGoldValue = order.exchangeGold?.finalGoldExchangePrice ?: 0.0
        val gstAmount = order.gstAmount
        val discountAmount = order.discountAmount
        val totalAmount = order.totalAmount
        val gstPercentage = order.gstPercentage.toInt()

        val mainStore = storeInfo.mainStore
        val bankInfo = storeInfo.bankInfo

        val companyName = if (mainStore.name.isNotEmpty()) mainStore.name else invoiceConfig.companyName
        val companyAddress = if (mainStore.address.isNotEmpty()) mainStore.address else invoiceConfig.companyAddress
        val companyPhone = if (mainStore.phone_primary.isNotEmpty()) mainStore.phone_primary else invoiceConfig.companyPhone
        val companyEmail = if (mainStore.email.isNotEmpty()) mainStore.email else invoiceConfig.companyEmail
        val companyGSTIN = if (mainStore.gstIn.isNotEmpty()) mainStore.gstIn else invoiceConfig.companyGST
        val companyPAN = "" // PAN removed from main_store
        val companyCertification = mainStore.certification
        val stateCode = if (mainStore.stateCode.isNotEmpty()) mainStore.stateCode else if (companyGSTIN.length >= 2) companyGSTIN.substring(0, 2) else ""
        val stateName = mainStore.stateName

        val companyLogoBase64 = loadGaganLogoFromFirebase()
        val bisLogoBase64 = loadLogoAsBase64("bis_logo.png")

        // Customer details in table format
        val customerStateName = extractStateNameFromAddress(customer.address)
        val customerStateCode = extractStateCodeFromAddress(customer.address)
        val city = extractCityFromAddress(customer.address)
        val customerDetails = buildString {
            append("""
        <tr class="border-b border-black">
            <td class="p-2 w-24 font-semibold">Name</td>
            <td class="p-2">: ${escapeHtml(customer.name.uppercase())}</td>
        </tr>
        <tr class="border-b border-black">
            <td class="p-2 font-semibold">Address</td>
            <td class="p-2">: ${escapeHtml(customer.address.uppercase())}</td>
        </tr>
        <tr class="border-b border-black">
            <td class="p-2 font-semibold">City</td>
            <td class="p-2">: ${escapeHtml(city.uppercase())} State: ${escapeHtml(customerStateName.uppercase())} (${escapeHtml(customerStateCode)})</td>
        </tr>
        <tr>
            <td class="p-2 font-semibold">Ph No.</td>
            <td class="p-2">:</td>
        </tr>
    """.trimIndent())
        }

        // Invoice details in table format
        val invoiceDetails = buildString {
            append("<tr class=\"border-b border-black\"><td class=\"p-2 w-28 font-semibold\">Memo No.</td><td class=\"p-2\">: GST/${escapeHtml(order.orderId.takeLast(8))}</td><td class=\"p-2 w-20 font-semibold\">Date</td><td class=\"p-2\">: ${escapeHtml(orderDate)}</td></tr>")
            append("<tr class=\"border-b border-black\"><td class=\"p-2 font-semibold\">State Name</td><td class=\"p-2\">: ${escapeHtml(stateName.uppercase())}</td><td class=\"p-2 font-semibold\">State Code</td><td class=\"p-2\">: ${escapeHtml(stateCode)}</td></tr>")
            append("<tr><td class=\"p-2 font-semibold\">Place of Delivery</td><td class=\"p-2\" colspan=\"3\">: ${escapeHtml(customerStateName.ifEmpty { stateName }.uppercase())}</td></tr>")
        }

        val productTableData = generateTanishqProductTableRows(order.items, products, order, subtotal, discountAmount, gstAmount)

        val productTableTotals = """
    <td class="border-r border-black p-1" colspan="3"><strong>Total</strong></td>
    <td class="border-r border-black p-1 text-center"><strong>${productTableData.totalQty}N</strong></td>
    <td class="border-r border-black p-1 text-center"><strong>${String.format("%.3f", productTableData.totalGrossWeight)}</strong></td>
    <td class="border-r border-black p-1 text-center"><strong>${if (productTableData.totalNetStoneWeight > 0) String.format("%.3f", productTableData.totalNetStoneWeight) else "-"}</strong></td>
    <td class="border-r border-black p-1 text-center"><strong>${String.format("%.3f", productTableData.totalNetMetalWeight)}</strong></td>
    <td class="border-r border-black p-1 text-center"><strong>-</strong></td>
    <td class="border-r border-black p-1 text-right"><strong>${CurrencyFormatter.formatRupeesNumber(productTableData.totalGrossProductPrice, includeDecimals = true)}</strong></td>
    <td class="border-r border-black p-1 text-right"><strong>${CurrencyFormatter.formatRupeesNumber(productTableData.totalMakingCharges, includeDecimals = true)}</strong></td>
    <td class="border-r border-black p-1 text-center"><strong>${if (productTableData.totalStoneAmount > 0) CurrencyFormatter.formatRupeesNumber(productTableData.totalStoneAmount, includeDecimals = true) else "-"}</strong></td>
    <td class="p-1 text-right"><strong>${CurrencyFormatter.formatRupeesNumber(productTableData.totalProductValue, includeDecimals = true)}</strong></td>
""".trimIndent()
        val bankDetails = buildString {
            if (bankInfo.account_holder.isNotEmpty()) {
                append("<p><span class=\"font-semibold\">Account Holder :</span> ${escapeHtml(bankInfo.account_holder)}</p>")
            }
            if (bankInfo.AccountNumber.isNotEmpty()) {
                append("<p><span class=\"font-semibold\">Account No. :</span> ${escapeHtml(bankInfo.AccountNumber)}</p>")
            }
            if (bankInfo.IFSC_Code.isNotEmpty()) {
                append("<p><span class=\"font-semibold\">IFSC Code :</span> ${escapeHtml(bankInfo.IFSC_Code)}</p>")
            }
            if (bankInfo.Branch.isNotEmpty()) {
                append("<p><span class=\"font-semibold\">Branch :</span> ${escapeHtml(bankInfo.Branch)}</p>")
            }
            if (bankInfo.Acc_type.isNotEmpty()) {
                append("<p><span class=\"font-semibold\">Account Type :</span> ${escapeHtml(bankInfo.Acc_type)}</p>")
            }
            if (companyGSTIN.isNotEmpty()) {
                append("<p><span class=\"font-semibold\">Company's GSTIN No.</span> : ${escapeHtml(companyGSTIN)}</p>")
            }
            if (companyPAN.isNotEmpty()) {
                append("<p><span class=\"font-semibold\">Company PAN No.</span> : ${escapeHtml(companyPAN)}</p>")
            }
        }

        val paymentTableRows = if (order.paymentSplit != null) {
            buildString {
                if (order.paymentSplit.cash > 0) {
                    append("""
                        <tr class="border-b border-black">
                            <td class="border-r border-black p-1">CASH</td>
                            <td class="border-r border-black p-1 text-center">-</td>
                            <td class="border-r border-black p-1">${escapeHtml(customer.name)}</td>
                            <td class="p-1 text-right">${CurrencyFormatter.formatRupeesNumber(order.paymentSplit.cash, includeDecimals = true)}</td>
                        </tr>
                    """.trimIndent())
                }
                if (order.paymentSplit.bank > 0) {
                    append("""
                        <tr class="border-b border-black">
                            <td class="border-r border-black p-1">BANK/CARD/ONLINE</td>
                            <td class="border-r border-black p-1 text-center">-</td>
                            <td class="border-r border-black p-1">${escapeHtml(customer.name)}</td>
                            <td class="p-1 text-right">${CurrencyFormatter.formatRupeesNumber(order.paymentSplit.bank, includeDecimals = true)}</td>
                        </tr>
                    """.trimIndent())
                }
                if (order.paymentSplit.dueAmount > 0) {
                    append("""
                        <tr class="border-b border-black">
                            <td class="border-r border-black p-1">DUE</td>
                            <td class="border-r border-black p-1 text-center">-</td>
                            <td class="border-r border-black p-1">${escapeHtml(customer.name)}</td>
                            <td class="p-1 text-right">${CurrencyFormatter.formatRupeesNumber(order.paymentSplit.dueAmount, includeDecimals = true)}</td>
                        </tr>
                    """.trimIndent())
                }
                val totalPaid = order.paymentSplit.cash + order.paymentSplit.bank
                append("""
                    <tr class="border-b border-black font-semibold">
                        <td class="p-1" colspan="3">Total Amount Paid</td>
                        <td class="p-1 text-right">${CurrencyFormatter.formatRupeesNumber(totalPaid, includeDecimals = true)}</td>
                    </tr>
                    <tr class="font-semibold">
                        <td class="p-1" colspan="3">Due Amount</td>
                        <td class="p-1 text-right">${CurrencyFormatter.formatRupeesNumber(order.paymentSplit.dueAmount, includeDecimals = true)}</td>
                    </tr>
                """.trimIndent())
            }
        } else {
            ""
        }

        // Taxable amount is calculated after exchange gold and discount
        val amountAfterExchangeGold = (subtotal - exchangeGoldValue).coerceAtLeast(0.0)
        val taxableAmount = amountAfterExchangeGold - discountAmount
        val sgstAmount = gstAmount / 2
        val cgstAmount = gstAmount / 2
        val grossTotal = taxableAmount + gstAmount
        val roundedTotal = grossTotal.roundToInt().toDouble()
        val roundOff = roundedTotal - grossTotal

        val summaryRows = buildString {
            append("""
            <tr class="border-b border-black">
                <td class="p-2 font-semibold">Amount</td>
                <td class="p-2 text-right">${CurrencyFormatter.formatRupeesNumber(subtotal, includeDecimals = true)}</td>
            </tr>
            """.trimIndent())
            // Display Exchange Gold if present (shown after Amount, before Discount)
            if (exchangeGoldValue > 0) {
                append("""
            <tr class="border-b border-black">
                <td class="p-2 font-semibold">Less: Exchange Gold</td>
                <td class="p-2 text-right">${CurrencyFormatter.formatRupeesNumber(exchangeGoldValue, includeDecimals = true)}</td>
            </tr>
                """.trimIndent())
                // Show exchange gold details if available
                order.exchangeGold?.let { exchangeGold ->
                    if (exchangeGold.productName.isNotEmpty() || exchangeGold.goldWeight > 0 || exchangeGold.goldPurity.isNotEmpty()) {
                        val details = buildString {
                            if (exchangeGold.productName.isNotEmpty()) {
                                append("Product: ${escapeHtml(exchangeGold.productName)}<br>")
                            }
                            if (exchangeGold.goldWeight > 0) {
                                append("Weight: ${String.format("%.2f", exchangeGold.goldWeight)}g<br>")
                            }
                            if (exchangeGold.goldPurity.isNotEmpty()) {
                                append("Purity: ${escapeHtml(exchangeGold.goldPurity)}<br>")
                            }
                            if (exchangeGold.goldRate > 0) {
                                append("Rate: ₹${String.format("%.2f", exchangeGold.goldRate)}/g")
                            }
                        }
                        append("""
            <tr class="border-b border-black">
                <td class="p-2 pl-6" colspan="2" style="font-size: 10px; color: #666;">
                            $details
                </td>
            </tr>
                        """.trimIndent())
                    }
                }
            }
            append("""
            <tr class="border-b border-black">
                <td class="p-2 font-semibold">Less: Discount</td>
                <td class="p-2 text-right">${CurrencyFormatter.formatRupeesNumber(discountAmount, includeDecimals = true)}</td>
            </tr>
            <tr class="border-b border-black">
                <td class="p-2 font-semibold">Taxable Amount</td>
                <td class="p-2 text-right">${CurrencyFormatter.formatRupeesNumber(taxableAmount, includeDecimals = true)}</td>
            </tr>
            <tr class="border-b border-black">
                <td class="p-2 font-semibold">SGST ${String.format("%.3f", gstPercentage / 2.0)} % + CGST ${String.format("%.3f", gstPercentage / 2.0)} %</td>
                <td class="p-2 text-right">${CurrencyFormatter.formatRupeesNumber(gstAmount, includeDecimals = true)}</td>
            </tr>
            <tr class="border-b border-black">
                <td class="p-2 font-semibold">Gross Total</td>
                <td class="p-2 text-right">${CurrencyFormatter.formatRupeesNumber(grossTotal, includeDecimals = true)}</td>
            </tr>
            <tr class="border-b border-black">
                <td class="p-2 font-semibold">Round Off</td>
                <td class="p-2 text-right">${CurrencyFormatter.formatRupeesNumber(roundOff, includeDecimals = true)}</td>
            </tr>
            <tr class="bg-gray-100">
                <td class="p-2 font-bold">Net Amount</td>
                <td class="p-2 text-right font-bold">${CurrencyFormatter.formatRupeesNumber(roundedTotal, includeDecimals = true)}</td>
            </tr>
            """.trimIndent())
        }

        val termsAndConditions = """
        <ol class="text-10px space-y-1 list-decimal pl-5">
            <li>No encashment or exchange without presentation of this Invoice.</li>
            <li>Any exchange of gold ornaments can be made within 48 hours, only if it is not tampered.</li>
            <li>The company is not liable for colours, damage, loss and breakage of ornaments.</li>
            <li>In case of payment by cheque, delivery of goods will be made only after realization of cheque.</li>
            <li>All term &amp; conditions are subject to change without prior notice.</li>
            <li>Including Hallmarking Charge</li>
            <li>Disputes if any, are subject to SAHANSA jurisdiction</li>
        </ol>
        """.trimIndent()

        val htmlTemplate = loadTemplateFile("invoice-template.html")
        val cssContent = loadTemplateFile("invoice-styles.css")

        // Convert HTML to XHTML-compliant format
        val finalHtml = htmlTemplate
            .replace("{{CSS_CONTENT}}", cssContent)
            .replace("{{COMPANY_LOGO_PATH}}", companyLogoBase64)
            .replace("{{BIS_LOGO_PATH}}", bisLogoBase64)
            .replace("{{COMPANY_NAME}}", escapeHtml(companyName))
            .replace("{{COMPANY_NAME_SHORT}}", escapeHtml(companyName.split(" ").firstOrNull() ?: companyName))
            .replace("{{COMPANY_ADDRESS}}", escapeHtml(companyAddress))
            .replace("{{COMPANY_CERTIFICATION}}", escapeHtml(companyCertification))
            .replace("{{COMPANY_PHONE}}", escapeHtml(companyPhone))
            .replace("{{COMPANY_EMAIL}}", escapeHtml(companyEmail))
            .replace("{{COMPANY_GSTIN}}", escapeHtml(companyGSTIN))
            .replace("{{IRN}}", escapeHtml(irn))
            .replace("{{ACK_NO}}", escapeHtml(ackNo))
            .replace("{{ACK_DATE}}", escapeHtml(ackDate))
            .replace("{{ORDER_ID}}", escapeHtml(order.orderId))
            .replace("{{CUSTOMER_DETAILS}}", customerDetails)
            .replace("{{INVOICE_DETAILS}}", invoiceDetails)
            .replace("{{PRODUCT_TABLE_ROWS}}", productTableData.rows)
            .replace("{{PRODUCT_TABLE_TOTALS}}", productTableTotals)
            .replace("{{BANK_DETAILS}}", bankDetails)
            .replace("{{PAYMENT_TABLE_ROWS}}", paymentTableRows)
            .replace("{{SUMMARY_ROWS}}", summaryRows)
            .replace("{{AMOUNT_IN_WORDS}}", convertAmountToWords(roundedTotal))
            .replace("{{TERMS_AND_CONDITIONS}}", termsAndConditions)

        // Convert to XHTML-compliant (fix self-closing tags)
        return convertToXhtmlCompliant(finalHtml)
    }

    /**
     * Convert HTML to XHTML-compliant format
     */
    private fun convertToXhtmlCompliant(html: String): String {
        return html
            .replace("<br>", "<br />")
            .replace("<hr>", "<hr />")
            .replace("<img ", "<img ", ignoreCase = false)
            .replace(Regex("""<img([^>]*[^/])>"""), "<img$1 />")
            .replace(Regex("""<input([^>]*[^/])>"""), "<input$1 />")
            .replace(Regex("""<meta([^>]*[^/])>"""), "<meta$1 />")
    }

    private data class TanishqProductTableData(
        val rows: String,
        val totalQty: Int,
        val totalGrossWeight: Double,
        val totalNetStoneWeight: Double,
        val totalNetMetalWeight: Double,
        val totalGrossProductPrice: Double,
        val totalMakingCharges: Double,
        val totalStoneAmount: Double,
        val totalProductValue: Double
    )

    private fun generateTanishqProductTableRows(
        items: List<org.example.project.data.OrderItem>,
        products: List<org.example.project.data.Product>,
        order: Order,
        subtotal: Double,
        discountAmount: Double,
        gstAmount: Double
    ): TanishqProductTableData {
        var totalQty = 0
        var totalGrossWeight = 0.0
        var totalNetStoneWeight = 0.0
        var totalNetMetalWeight = 0.0
        var totalGrossProductPrice = 0.0
        var totalMakingCharges = 0.0
        var totalStoneAmount = 0.0
        var totalProductValue = 0.0

        val rows = items.joinToString("") { item ->
            val product = products.find { it.id == item.productId } ?: return@joinToString ""

            val quantity = item.quantity
            val grossWeight = product.totalWeight * quantity
            val stoneWeight = product.stones.sumOf { it.weight } * quantity
            val metalWeight = (product.materialWeight.takeIf { it > 0 } ?: product.totalWeight) * quantity

            val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
            val metalKarat = extractKaratFromMaterialType(product.materialType)
            val rate = try {
                ratesVM.calculateRateForMaterial(product.materialId, product.materialType, metalKarat)
            } catch (e: Exception) { 0.0 }

            val grossProductPrice = metalWeight * rate
            val makingPercent = if (metalWeight > 0) (item.labourCharges / metalWeight) * 100 else 0.0
            val labourCharges = item.labourCharges
            val stoneAmount = 0.0 // Add stone calculation if needed
            val costValue = grossProductPrice + labourCharges + stoneAmount

            totalQty += quantity
            totalGrossWeight += grossWeight
            totalNetStoneWeight += stoneWeight
            totalNetMetalWeight += metalWeight
            totalGrossProductPrice += grossProductPrice
            totalMakingCharges += labourCharges
            totalStoneAmount += stoneAmount
            totalProductValue += costValue

            val materialDisplay = when {
                product.materialType.contains("24", ignoreCase = true) -> "G-24Karat"
                product.materialType.contains("22", ignoreCase = true) -> "G-22Karat"
                product.materialType.contains("18", ignoreCase = true) -> "G-18Karat"
                else -> product.materialType.uppercase()
            }

            """
            <tr class="border-b border-black">
                <td class="border-r border-black p-1 text-center">${escapeHtml(product.id.takeLast(12))}</td>
                <td class="border-r border-black p-1">${escapeHtml(product.name.uppercase())}</td>
                <td class="border-r border-black p-1 text-center">${escapeHtml(materialDisplay)}</td>
                <td class="border-r border-black p-1 text-center">${quantity}N</td>
                <td class="border-r border-black p-1 text-center">${String.format("%.3f", grossWeight)}</td>
                <td class="border-r border-black p-1 text-center">${if (stoneWeight > 0) String.format("%.3f", stoneWeight) else "-"}</td>
                <td class="border-r border-black p-1 text-center">${String.format("%.3f", metalWeight)}</td>
                <td class="border-r border-black p-1 text-center">${String.format("%.2f", makingPercent)}%</td>
                <td class="border-r border-black p-1 text-right">${CurrencyFormatter.formatRupeesNumber(grossProductPrice, includeDecimals = true)}</td>
                <td class="border-r border-black p-1 text-right">${CurrencyFormatter.formatRupeesNumber(labourCharges, includeDecimals = true)}</td>
                <td class="border-r border-black p-1 text-center">${if (stoneAmount > 0) CurrencyFormatter.formatRupeesNumber(stoneAmount, includeDecimals = true) else "-"}</td>
                <td class="p-1 text-right">${CurrencyFormatter.formatRupeesNumber(costValue, includeDecimals = true)}</td>
            </tr>
            """.trimIndent()
        }

        return TanishqProductTableData(
            rows = rows,
            totalQty = totalQty,
            totalGrossWeight = totalGrossWeight,
            totalNetStoneWeight = totalNetStoneWeight,
            totalNetMetalWeight = totalNetMetalWeight,
            totalGrossProductPrice = totalGrossProductPrice,
            totalMakingCharges = totalMakingCharges,
            totalStoneAmount = totalStoneAmount,
            totalProductValue = totalProductValue
        )
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

            // 5. Try to find from class location (for development/build)
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

            // 6. Try to find utils directory relative to this class file
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

            // 7. Try to find project root using findProjectRoot helper
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

    /**
     * Escape HTML entities to prevent XML parsing errors
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    /**
     * Generate IRN (Invoice Reference Number) using SHA-256 hash
     */
    private fun generateIRN(orderId: String, timestamp: Long): String {
        val input = "$orderId$timestamp"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Load Gagan Jewellers logo from Firebase Storage (logos folder) as base64 data URI.
     */
    private fun loadGaganLogoFromFirebase(): String {
        val paths = listOf(
            "logos/gagan jewellers logo",
            "logos/gagan jewellers logo.png",
            "logos/gagan jewellers logo.jpg",
            "logos/gagan jewellers logo.jpeg"
        )
        return try {
            val storage = JewelryAppInitializer.getStorageService()
            var bytes: ByteArray? = null
            for (path in paths) {
                val b = storage.downloadFileBytes(path)
                if (b != null && b.isNotEmpty()) {
                    bytes = b
                    break
                }
            }
            bytes ?: return ""
            val base64 = Base64.getEncoder().encodeToString(bytes)
            val mimeType = when {
                bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "image/png"
                bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "image/jpeg"
                else -> "image/png"
            }
            "data:$mimeType;base64,$base64"
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Load logo from composeResources and convert to base64 data URI
     */
    private fun loadLogoAsBase64(filename: String): String {
        return try {
            // Try to load from composeResources
            val resourceStream = javaClass.getResourceAsStream("/drawable/$filename")
                ?: javaClass.classLoader.getResourceAsStream("drawable/$filename")

            if (resourceStream != null) {
                val imageBytes = resourceStream.readBytes()
                val base64 = Base64.getEncoder().encodeToString(imageBytes)
                val mimeType = when {
                    filename.endsWith(".png", ignoreCase = true) -> "image/png"
                    filename.endsWith(".jpg", ignoreCase = true) || filename.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    else -> "image/png"
                }
                return "data:$mimeType;base64,$base64"
            }

            // Try to load from file system (composeResources folder)
            val userDir = System.getProperty("user.dir")
            val projectRoot = findProjectRoot() ?: userDir
            val logoPath = File("$projectRoot/composeApp/src/commonMain/composeResources/drawable/$filename")

            if (logoPath.exists()) {
                val imageBytes = logoPath.readBytes()
                val base64 = Base64.getEncoder().encodeToString(imageBytes)
                val mimeType = when {
                    filename.endsWith(".png", ignoreCase = true) -> "image/png"
                    filename.endsWith(".jpg", ignoreCase = true) || filename.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    else -> "image/png"
                }
                return "data:$mimeType;base64,$base64"
            }

            println("⚠️ Logo not found: $filename")
            "" // Return empty string if logo not found
        } catch (e: Exception) {
            println("⚠️ Error loading logo $filename: ${e.message}")
            "" // Return empty string on error
        }
    }

    /**
     * Extract state code from address (look for state code patterns)
     */
    private fun extractStateCodeFromAddress(address: String): String {
        // Common state codes in India
        val stateCodePattern = Regex("""\b([0-9]{2})\b""")
        val match = stateCodePattern.find(address)
        return match?.groupValues?.get(1) ?: ""
    }

    /**
     * Extract state name from address
     */
    private fun extractStateNameFromAddress(address: String): String {
        val states = listOf("Punjab", "Haryana", "Delhi", "Uttar Pradesh", "Rajasthan", "Himachal Pradesh", "Bihar", "West Bengal")
        for (state in states) {
            if (address.contains(state, ignoreCase = true)) {
                return state
            }
        }
        return ""
    }

    /**
     * Extract city from address (usually before state or after comma)
     */
    private fun extractCityFromAddress(address: String): String {
        // Try to extract city - usually the last word before state or after first comma
        val parts = address.split(",")
        if (parts.size > 1) {
            return parts[0].trim()
        }
        // If no comma, try to get first word
        val words = address.split(" ")
        if (words.isNotEmpty()) {
            return words[0]
        }
        return ""
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