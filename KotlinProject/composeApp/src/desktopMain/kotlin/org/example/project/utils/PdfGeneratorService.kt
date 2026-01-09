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
        
        // Fetch store info from Firestore
        val storeInfoRepository = StoreInfoRepository()
        val storeInfo = kotlinx.coroutines.runBlocking {
            storeInfoRepository.getStoreInfo()
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val orderDate = dateFormat.format(Date(order.createdAt))
        val ackDate = dateFormat.format(Date(order.createdAt))

        val irn = generateIRN(order.orderId, order.createdAt)
        val ackNo = order.orderId.take(15)
        
        val subtotal = order.totalProductValue
        val gstAmount = order.gstAmount
        val discountAmount = order.discountAmount
        val totalAmount = order.totalAmount
        val gstPercentage = order.gstPercentage.toInt()

        val mainStore = storeInfo.mainStore
        val bankInfo = storeInfo.bankInfo

        val companyName = if (mainStore.companyName.isNotEmpty()) mainStore.companyName else invoiceConfig.companyName
        val companyAddress = if (mainStore.address.isNotEmpty()) mainStore.address else invoiceConfig.companyAddress
        val companyPhone = if (mainStore.phone.isNotEmpty()) mainStore.phone else invoiceConfig.companyPhone
        val companyEmail = if (mainStore.email.isNotEmpty()) mainStore.email else invoiceConfig.companyEmail
        val companyGSTIN = if (mainStore.gstin.isNotEmpty()) mainStore.gstin else invoiceConfig.companyGST
        val companyPAN = mainStore.pan
        val companyCertification = mainStore.certification
        val stateCode = if (mainStore.stateCode.isNotEmpty()) mainStore.stateCode else if (companyGSTIN.length >= 2) companyGSTIN.substring(0, 2) else ""
        val stateName = mainStore.stateName

        val companyLogoBase64 = loadLogoAsBase64("gagan_jewellers_logo.png")
        val bisLogoBase64 = loadLogoAsBase64("bis_logo.png")

        // Customer details in table format
        val customerStateName = extractStateNameFromAddress(customer.address)
        val customerStateCode = extractStateCodeFromAddress(customer.address)
        val city = extractCityFromAddress(customer.address)
        val customerDetails = buildString {
            append("<tr><td class=\"label\">Name</td><td class=\"colon\">:</td><td>${escapeHtml(customer.name.uppercase())}</td></tr>")
            append("<tr><td class=\"label\">Address</td><td class=\"colon\">:</td><td>${escapeHtml(customer.address.uppercase())}</td></tr>")
            append("<tr><td class=\"label\">City</td><td class=\"colon\">:</td><td>${escapeHtml(city.uppercase())} State: ${escapeHtml(customerStateName.uppercase())} (${escapeHtml(customerStateCode)})</td></tr>")
            append("<tr><td class=\"label\">Ph No.</td><td class=\"colon\">:</td><td>${escapeHtml(customer.phone)}</td></tr>")
        }

        // Invoice details in table format
        val invoiceDetails = buildString {
            append("<tr><td class=\"label\">Memo No.</td><td class=\"colon\">:</td><td>GST/${escapeHtml(order.orderId.takeLast(8))} Date</td><td class=\"colon\">:</td><td>${escapeHtml(orderDate)}</td></tr>")
            append("<tr><td class=\"label\">State Name</td><td class=\"colon\">:</td><td>${escapeHtml(stateName.uppercase())}</td><td class=\"label\">State Code</td><td>: ${escapeHtml(stateCode)}</td></tr>")
            append("<tr><td class=\"label\">Place of Delivery</td><td class=\"colon\">:</td><td>${escapeHtml(customerStateName.ifEmpty { stateName }.uppercase())}</td><td></td><td></td></tr>")
        }

        val productTableData = generateProductTableRows(order.items, products, order)

        val productTableTotals = """
            <td colspan="6" class="text-right"><strong>Total</strong></td>
            <td><strong>${String.format("%.3f", productTableData.totalGrossWeight)}</strong></td>
            <td><strong>${String.format("%.3f", productTableData.totalNetWeight)}</strong></td>
            <td><strong>${String.format("%.3f", productTableData.totalPolish)}</strong></td>
            <td colspan="2"><strong>${CurrencyFormatter.formatRupeesNumber(productTableData.totalAmount, includeDecimals = true)}</strong></td>
            <td></td>
        """.trimIndent()

        val bankDetails = buildString {
            if (bankInfo.accountHolder.isNotEmpty()) {
                append("<p><strong>Account holder:</strong> ${escapeHtml(bankInfo.accountHolder)}</p>")
            }
            if (bankInfo.accountNumber.isNotEmpty()) {
                append("<p><strong>Account no.:</strong> ${escapeHtml(bankInfo.accountNumber)}</p>")
            }
            if (bankInfo.ifscCode.isNotEmpty()) {
                append("<p><strong>IFSC code:</strong> ${escapeHtml(bankInfo.ifscCode)}</p>")
            }
            if (bankInfo.branch.isNotEmpty()) {
                append("<p><strong>BRANCH:</strong> ${escapeHtml(bankInfo.branch)}</p>")
            }
            if (bankInfo.accountType.isNotEmpty()) {
                append("<p><strong>Account Type:</strong> ${escapeHtml(bankInfo.accountType)}</p>")
            }
        }

        val gstinDetails = buildString {
            append("<p><strong>Company's GSTIN No.</strong> :${escapeHtml(companyGSTIN)}</p>")
            if (companyPAN.isNotEmpty()) {
                append("<p><strong>Company PAN No.</strong> :${escapeHtml(companyPAN)}</p>")
            }
            append("<p><strong>Buyer's GSTIN No.</strong> :</p>")
            append("<p><strong>Buyer's PAN</strong></p>")
        }

        val taxableAmount = subtotal - discountAmount
        val sgstAmount = gstAmount / 2
        val cgstAmount = gstAmount / 2
        val grossTotal = taxableAmount + gstAmount
        val roundedTotal = grossTotal.roundToInt().toDouble()
        val roundOff = roundedTotal - grossTotal

        val summaryRows = buildString {
            append("""
            <tr>
                <td class="label">Amount</td>
                <td class="value">${CurrencyFormatter.formatRupeesNumber(subtotal, includeDecimals = true)}</td>
            </tr>
            <tr>
                <td class="label">Less: Discount</td>
                <td class="value">${CurrencyFormatter.formatRupeesNumber(discountAmount, includeDecimals = true)}</td>
            </tr>
            <tr>
                <td class="label">Taxable Amount</td>
                <td class="value">${CurrencyFormatter.formatRupeesNumber(taxableAmount, includeDecimals = true)}</td>
            </tr>
            <tr>
                <td class="label">SGST ${String.format("%.3f", gstPercentage / 2.0)} %</td>
                <td class="value">${CurrencyFormatter.formatRupeesNumber(sgstAmount, includeDecimals = true)}</td>
            </tr>
            <tr>
                <td class="label">CGST ${String.format("%.3f", gstPercentage / 2.0)} %</td>
                <td class="value">${CurrencyFormatter.formatRupeesNumber(cgstAmount, includeDecimals = true)}</td>
            </tr>
            <tr>
                <td class="label"><strong>Gross Total</strong></td>
                <td class="value"><strong>${CurrencyFormatter.formatRupeesNumber(grossTotal, includeDecimals = true)}</strong></td>
            </tr>
            <tr>
                <td class="label">Round Off</td>
                <td class="value">${CurrencyFormatter.formatRupeesNumber(roundOff, includeDecimals = true)}</td>
            </tr>
            <tr class="net-amount">
                <td class="label"><strong>Net Amount</strong></td>
                <td class="value"><strong>${CurrencyFormatter.formatRupeesNumber(roundedTotal, includeDecimals = true)}</strong></td>
            </tr>
        """.trimIndent())
        }

        val paymentDetails = if (order.paymentSplit != null) {
            buildString {
                if (order.paymentSplit.cash > 0) {
                    append("<p>Cash: ${CurrencyFormatter.formatRupeesNumber(order.paymentSplit.cash, includeDecimals = true)}</p>")
                }
                if (order.paymentSplit.bank > 0) {
                    append("<p>Bank/Card/Online: ${CurrencyFormatter.formatRupeesNumber(order.paymentSplit.bank, includeDecimals = true)}</p>")
                }
                if (order.paymentSplit.dueAmount > 0) {
                    append("<p>Due Amount: ${CurrencyFormatter.formatRupeesNumber(order.paymentSplit.dueAmount, includeDecimals = true)}</p>")
                }
            }
        } else {
            "<p>Cash: ${CurrencyFormatter.formatRupeesNumber(totalAmount, includeDecimals = true)}</p>"
        }

        val termsAndConditions = """
        <ol>
            <li>No encashment or exchange without presentation of this Invoice.</li>
            <li>Any exchange of gold ornaments can be made within 48 hours, only if it is not tampered, repaired or used.</li>
            <li>The company is not liable for colours, damage, loss and breakage of ornaments.</li>
            <li>In case of payment by cheque, delivery of goods will be made only after realization of cheque.</li>
            <li>Terms &amp; conditions are subject to change without prior notice.</li>
            <li>Including Hallmarking Charge</li>
            <li>Disputes if any, are subject to SAHARSA jurisdiction</li>
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
            .replace("{{GSTIN_DETAILS}}", gstinDetails)
            .replace("{{SUMMARY_ROWS}}", summaryRows)
            .replace("{{AMOUNT_IN_WORDS}}", convertAmountToWords(roundedTotal))
            .replace("{{PAYMENT_DETAILS}}", paymentDetails)
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

    private data class ProductTableData(
        val rows: String,
        val totalQty: Int,
        val totalGrossWeight: Double,
        val totalNetWeight: Double,
        val totalPolish: Double,
        val totalAmount: Double
    )

    private fun generateProductTableRows(
        items: List<org.example.project.data.OrderItem>,
        products: List<org.example.project.data.Product>,
        order: Order
    ): ProductTableData {
        var totalQty = 0
        var totalGrossWeight = 0.0
        var totalNetWeight = 0.0
        var totalPolish = 0.0
        var totalAmount = 0.0
        var slNo = 1

        val rows = items.joinToString("") { item ->
            val product = products.find { it.id == item.productId } ?: return@joinToString ""

            val quantity = item.quantity
            val grossWeight = product.totalWeight * quantity
            val netWeight = (product.materialWeight.takeIf { it > 0 } ?: product.totalWeight) * quantity
            val stoneWeight = product.stones.sumOf { it.weight } * quantity
            val polish = stoneWeight

            val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
            val metalKarat = extractKaratFromMaterialType(product.materialType)
            val rate = try {
                ratesVM.calculateRateForMaterial(product.materialId, product.materialType, metalKarat)
            } catch (e: Exception) { 0.0 }

            val labourPerGram = item.labourRate
            // Calculate amount: (netWeight * rate) + labourCharges
            val amount = (netWeight * rate) + item.labourCharges

            totalQty += quantity
            totalGrossWeight += grossWeight
            totalNetWeight += netWeight
            totalPolish += polish
            totalAmount += amount

            val hsnCode = "71131910"
            val purityDisplay = when {
                product.materialType.contains("99.99", ignoreCase = true) -> "99.99"
                product.materialType.contains("22", ignoreCase = true) -> "22K"
                product.materialType.contains("18", ignoreCase = true) -> "18K"
                product.materialType.contains("14", ignoreCase = true) -> "14K"
                product.materialType.contains("24", ignoreCase = true) -> "24K"
                else -> product.materialType.uppercase()
            }
            
            """
            <tr>
                <td>${slNo++}.</td>
                <td>${escapeHtml(product.name.uppercase())}</td>
                <td>${escapeHtml(product.id.takeLast(8))}</td>
                <td>${quantity}</td>
                <td>${hsnCode}</td>
                <td>${escapeHtml(purityDisplay)}</td>
                <td>${String.format("%.3f", grossWeight)}</td>
                <td>${String.format("%.3f", netWeight)}</td>
                <td>${String.format("%.2f", polish)}</td>
                <td>${if (rate > 0) CurrencyFormatter.formatRupeesNumber(rate, includeDecimals = false) else ""}</td>
                <td>${if (labourPerGram > 0) CurrencyFormatter.formatRupeesNumber(labourPerGram, includeDecimals = false) else ""}</td>
                <td>${CurrencyFormatter.formatRupeesNumber(amount, includeDecimals = true)}</td>
            </tr>
            """.trimIndent()
        }

        return ProductTableData(
            rows = rows,
            totalQty = totalQty,
            totalGrossWeight = totalGrossWeight,
            totalNetWeight = totalNetWeight,
            totalPolish = totalPolish,
            totalAmount = totalAmount
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