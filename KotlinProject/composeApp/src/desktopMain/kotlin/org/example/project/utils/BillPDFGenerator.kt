package org.example.project.utils

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.example.project.data.Order
import org.example.project.data.User
import org.example.project.ui.ProductPriceInputs
import org.example.project.ui.calculateProductPrice
import org.example.project.data.MetalRatesManager
import org.example.project.data.extractKaratFromMaterialType
import org.example.project.JewelryAppInitializer
import org.example.project.utils.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

class BillPDFGenerator {

    private val helveticaBold = PDType1Font.HELVETICA_BOLD
    private val helvetica = PDType1Font.HELVETICA
    private val courierBold = PDType1Font.COURIER_BOLD
    private val courier = PDType1Font.COURIER

    suspend fun generateBill(
        order: Order,
        customer: User,
        outputPath: Path,
        companyName: String = "Gagan Jewellers Pvt.Ltd.",
        companyAddress: String = "123 Jewelry Street, Golden City, GC 12345",
        companyPhone: String = "+91 98765 43210",
        companyEmail: String = "info@vishalgems.com",
        gstNumber: String = "22AAAAA0000A1Z5",
        goldPricePerGram: Double = 6080.0,
        silverPricePerGram: Double = 75.0,
        cartItems: List<org.example.project.data.CartItem>? = null // Optional CartItem data for accurate pricing
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            println("🔄 ========== PDFBOX PDF GENERATION STARTED ==========")
            println("📋 Order ID: ${order.orderId}")
            println("👤 Customer: ${customer.name} (ID: ${customer.id})")
            println("📄 Output path: $outputPath")
            println("💰 Order total: ₹${String.format("%.2f", order.totalAmount)}")
            println("📦 Items count: ${order.items.size}")
            println("🏢 Company: $companyName")
            println("🥇 Gold rate: ₹${String.format("%.2f", goldPricePerGram)}/g")
            println("🥈 Silver rate: ₹${String.format("%.2f", silverPricePerGram)}/g")
            
            // Validate inputs
            if (order.orderId.isBlank()) {
                throw IllegalArgumentException("Order ID is blank")
            }
            if (customer.name.isBlank()) {
                throw IllegalArgumentException("Customer name is blank")
            }
            if (order.items.isEmpty()) {
                throw IllegalArgumentException("Order has no items")
            }
            
            // Check output directory
            val outputFile = outputPath.toFile()
            val outputDir = outputFile.parentFile
            
            if (!outputDir.exists()) {
                val created = outputDir.mkdirs()
                println("📁 Created output directory: $created")
            }
            
            if (!outputDir.canWrite()) {
                throw SecurityException("Cannot write to output directory: ${outputDir.absolutePath}")
            }
            
            println("✅ Output directory validated")
            
            println("🔧 Creating PDDocument...")
            PDDocument().use { document ->
                // Fetch product details for PDF generation
                val products = fetchProductDetails(order.items.map { it.productId })
                println("📦 Fetched ${products.size} product details for PDF generation")
                
                println("🔧 Adding A4 page...")
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)
                println("✅ Page added successfully")

                PDPageContentStream(document, page).use { contentStream ->
                    println("🔧 Setting up page dimensions...")
                    val pageWidth = page.mediaBox.width
                    val pageHeight = page.mediaBox.height
                    val margin = 30f
                    var yPosition = pageHeight - margin
                    
                    println("📐 Page dimensions: ${pageWidth}x${pageHeight}, margin: ${margin}")

                    println("🔧 Drawing border...")
                    drawBorder(contentStream, margin - 10f, pageWidth, pageHeight)
                    println("✅ Border drawn")

                    println("🔧 Drawing customer copy header...")
                    yPosition = drawCustomerCopyHeader(contentStream, margin, yPosition, pageWidth)
                    println("✅ Customer copy header drawn, yPosition: $yPosition")

                    println("🔧 Drawing main header...")
                    yPosition = drawMainHeader(contentStream, companyName, companyAddress,
                        companyPhone, gstNumber, order, customer, margin, yPosition, pageWidth)
                    println("✅ Main header drawn, yPosition: $yPosition")


                    println("🔧 Drawing items table...")
                    yPosition = drawCorrectItemsTable(
                        contentStream, order, products, goldPricePerGram, silverPricePerGram, margin, yPosition, pageWidth, cartItems
                    )
                    println("✅ Items table drawn, yPosition: $yPosition")

                    println("🔧 Drawing payment section...")
                    yPosition = drawPaymentSection(contentStream, order, margin, yPosition, pageWidth)
                    println("✅ Payment section drawn, yPosition: $yPosition")

                    println("🔧 Drawing amount in words...")
                    drawAmountInWords(contentStream, order.totalAmount, margin, yPosition, pageWidth)
                    println("✅ Amount in words drawn")
                }
                
                println("🔧 Saving document...")
                document.save(outputPath.toFile())
                
                // Verify the file was created
                if (outputFile.exists() && outputFile.length() > 0) {
                    println("✅ ========== PDFBOX PDF GENERATION SUCCESSFUL ==========")
                    println("📄 PDF saved at: $outputPath")
                    println("📊 PDF file size: ${outputFile.length()} bytes")
                    true
                } else {
                    println("❌ PDF file verification failed")
                    false
                }
            }
        } catch (e: Exception) {
            println("❌ ========== PDFBOX PDF GENERATION FAILED ==========")
            println("💥 Exception: ${e.javaClass.simpleName}")
            println("💥 Message: ${e.message}")
            println("💥 Stack trace:")
            e.printStackTrace()
            false
        }
    }

    private fun drawBorder(
        contentStream: PDPageContentStream,
        margin: Float,
        pageWidth: Float,
        pageHeight: Float
    ) {
        try {
            contentStream.setLineWidth(2f)
            contentStream.addRect(margin, margin, pageWidth - (2 * margin), pageHeight - (2 * margin))
            contentStream.stroke()
        } catch (e: Exception) {
            println("Error drawing border: ${e.message}")
        }
    }

    private fun drawCustomerCopyHeader(
        contentStream: PDPageContentStream,
        margin: Float,
        startY: Float,
        pageWidth: Float
    ): Float {
        var yPos = startY - 20f

        try {
            contentStream.setFont(helveticaBold, 12f)
            val customerCopyText = "CUSTOMER COPY"
            val textWidth = getTextWidth(customerCopyText, helveticaBold, 12f)

            contentStream.beginText()
            contentStream.newLineAtOffset(pageWidth - margin - textWidth, yPos)
            contentStream.showText(customerCopyText)
            contentStream.endText()

            yPos -= 30f
        } catch (e: Exception) {
            println("Error in drawCustomerCopyHeader: ${e.message}")
        }

        return yPos
    }

    private fun drawMainHeader(
        contentStream: PDPageContentStream,
        companyName: String,
        address: String,
        phone: String,
        gstNumber: String,
        order: Order,
        customer: User,
        margin: Float,
        startY: Float,
        pageWidth: Float
    ): Float {
        var yPos = startY
        val centerX = pageWidth / 2

        try {
            // TAX INVOICE header
            contentStream.setFont(helveticaBold, 16f)
            val text = "TAX INVOICE"
            val textWidth = getTextWidth(text, helveticaBold, 16f)
            contentStream.beginText()
            contentStream.newLineAtOffset(centerX - textWidth/2, yPos)
            contentStream.showText(text)
            contentStream.endText()
            yPos -= 40f

            // Draw horizontal line
            contentStream.setLineWidth(1f)
            contentStream.moveTo(margin, yPos)
            contentStream.lineTo(pageWidth - margin, yPos)
            contentStream.stroke()
            yPos -= 20f

            // Left side - Company Details
            contentStream.setFont(helveticaBold, 14f)
            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText(companyName)
            contentStream.endText()
            yPos -= 18f

            contentStream.setFont(helvetica, 9f)
            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText(address)
            contentStream.endText()
            yPos -= 15f

            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText("Phone Number : $phone")
            contentStream.endText()
            yPos -= 15f

            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText("GSTIN : $gstNumber")
            contentStream.endText()

            // Right side - Customer Details and Invoice Info
            val rightColumnX = pageWidth * 0.55f
            var rightY = startY - 20f

            val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
            val invoiceDate = dateFormat.format(Date(order.createdAt))

            contentStream.setFont(helveticaBold, 10f)
            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText("DOC//SAL/${order.orderId.takeLast(4)} Date : $invoiceDate")
            contentStream.endText()
            rightY -= 25f

            rightY -= 5f // Add space above customer details
            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText("CUSTOMER DETAILS:")
            contentStream.endText()
            rightY -= 18f

            contentStream.setFont(helvetica, 9f)
            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText(customer.name.uppercase())
            contentStream.endText()
            rightY -= 15f

            if (customer.phone.isNotEmpty()) {
                contentStream.beginText()
                contentStream.newLineAtOffset(rightColumnX, rightY)
                contentStream.showText(customer.phone)
                contentStream.endText()
                rightY -= 15f
            }

            if (customer.address.isNotEmpty()) {
                contentStream.beginText()
                contentStream.newLineAtOffset(rightColumnX, rightY)
                contentStream.showText(customer.address.uppercase())
                contentStream.endText()
                rightY -= 15f
            }

            if (customer.email.isNotEmpty()) {
                contentStream.beginText()
                contentStream.newLineAtOffset(rightColumnX, rightY)
                contentStream.showText("Email : ${customer.email}")
                contentStream.endText()
                rightY -= 15f
            }

            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText("Order Id : ${order.orderId}")
            contentStream.endText()

            yPos -= 30f
        } catch (e: Exception) {
            println("Error in drawMainHeader: ${e.message}")
        }

        return yPos
    }


    private fun drawCorrectItemsTable(
        contentStream: PDPageContentStream,
        order: Order,
        products: List<org.example.project.data.Product>,
        goldPricePerGram: Double,
        silverPricePerGram: Double,
        margin: Float,
        startY: Float,
        pageWidth: Float,
        cartItems: List<org.example.project.data.CartItem>? = null
    ): Float {
        var yPos = startY - 10f
        val tableWidth = pageWidth - (2 * margin)
        val rowHeight = 25f

        try {
            val headers = listOf(
                "Product\nDescription",
                "Material\nType",
                "Qty",
                "Weight\n(grams)",
                "Rate\n(per gram)",
                "Metal\nCost",
                "Labour\nCharges",
                "Sub\nTotal",
                "GST",
                "Item\nTotal"
            )

            val columnWidths = listOf(0.20f, 0.08f, 0.05f, 0.10f, 0.10f, 0.10f, 0.10f, 0.10f, 0.08f, 0.09f)

            // Draw table header
            contentStream.setLineWidth(1f)
            contentStream.addRect(margin, yPos - rowHeight, tableWidth, rowHeight)
            contentStream.stroke()

            // Fill header background
            contentStream.setNonStrokingColor(0.95f, 0.95f, 0.95f)
            contentStream.addRect(margin, yPos - rowHeight, tableWidth, rowHeight)
            contentStream.fill()
            contentStream.setNonStrokingColor(0f, 0f, 0f)

            // Draw header text
            contentStream.setFont(helvetica, 7f)
            var xPos = margin + 1f

            headers.forEachIndexed { index, header ->
                val columnWidth = tableWidth * columnWidths[index]
                val lines = header.split("\n")
                var textY = yPos - 8f

                lines.forEach { line ->
                    val lineWidth = getTextWidth(line, helvetica, 7f)
                    val centerOffset = (columnWidth - lineWidth) / 2

                    contentStream.beginText()
                    contentStream.newLineAtOffset(xPos + centerOffset, textY)
                    contentStream.showText(line)
                    contentStream.endText()
                    textY -= 8f
                }

                // Draw vertical line after text operations
                if (index < headers.size - 1) {
                    contentStream.moveTo(xPos + columnWidth, yPos)
                    contentStream.lineTo(xPos + columnWidth, yPos - rowHeight)
                    contentStream.stroke()
                }

                xPos += columnWidth
            }

            yPos -= rowHeight

            // Calculate totals using actual order values
            var totalQty = 0
            var totalWeight = 0.0
            val totalMetalCost = order.subtotal
            val totalGst = order.gstAmount
            val grandTotal = order.totalAmount

            // Draw items with correct calculations
            contentStream.setFont(helvetica, 7f)

            // Get metal rates for ProductPriceCalculator
            val metalRates = MetalRatesManager.metalRates.value
            val ratesVM = JewelryAppInitializer.getMetalRateViewModel()

            // Display each item separately (no grouping)
            order.items.forEachIndexed { itemIndex, item ->
                val itemRowHeight = rowHeight * 1.2f
                yPos -= itemRowHeight

                // Draw row border
                contentStream.addRect(margin, yPos, tableWidth, itemRowHeight)
                contentStream.stroke()

                xPos = margin + 1f
                
                // Get product details for this item
                val product = products.find { it.id == item.productId }
                if (product == null) {
                    // Skip if product not found
                    totalQty += item.quantity
                    return@forEachIndexed
                }
                
                // Get corresponding CartItem if available (for accurate pricing with overrides)
                val cartItem = cartItems?.find { it.productId == item.productId && it.selectedBarcodeIds.contains(item.barcodeId) }
                
                // Use ProductPriceCalculator logic (same as ReceiptScreen)
                // Use cartItem overrides if available, otherwise use product defaults
                val grossWeight = if (cartItem != null && cartItem.grossWeight > 0) cartItem.grossWeight else product.totalWeight
                val makingPercentage = product.makingPercent
                val labourRatePerGram = product.labourRate
                
                // Extract kundan and jarkan from stones array
                val kundanStones = product.stones.filter { it.name.equals("Kundan", ignoreCase = true) }
                val jarkanStones = product.stones.filter { it.name.equals("Jarkan", ignoreCase = true) }
                
                // Sum all Kundan prices and weights
                val kundanPrice = kundanStones.sumOf { it.amount }
                val kundanWeight = kundanStones.sumOf { it.weight }
                
                // Sum all Jarkan prices and weights
                val jarkanPrice = jarkanStones.sumOf { it.amount }
                val jarkanWeight = jarkanStones.sumOf { it.weight }
                
                // Get material rate (fetched from metal rates, same as ReceiptScreen)
                // Use cartItem.metal if available, otherwise use item.materialType or product.materialType
                val materialType = if (cartItem != null && cartItem.metal.isNotEmpty()) {
                    cartItem.metal
                } else if (item.materialType.isNotBlank()) {
                    item.materialType
                } else {
                    product.materialType
                }
                
                // Extract metal karat (same as ReceiptScreen)
                val metalKarat = if (cartItem != null && cartItem.metal.isNotEmpty()) {
                    cartItem.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(product.materialType)
                } else {
                    extractKaratFromMaterialType(materialType)
                }
                
                val collectionRate = try {
                    ratesVM.calculateRateForMaterial(product.materialId, product.materialType, metalKarat)
                } catch (e: Exception) { 0.0 }
                val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
                
                // Use cartItem.customGoldRate if available (same as ReceiptScreen)
                val goldRate = if (cartItem != null && cartItem.customGoldRate > 0) {
                    cartItem.customGoldRate
                } else {
                    defaultGoldRate
                }
                
                // Extract silver purity from material type
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
                    product.materialType.contains("gold", ignoreCase = true) -> goldRate
                    product.materialType.contains("silver", ignoreCase = true) -> silverRate
                    else -> goldRate
                }
                
                // Build ProductPriceInputs (same structure as ProductPriceCalculator)
                val priceInputs = ProductPriceInputs(
                    grossWeight = grossWeight,
                    goldPurity = materialType,
                    goldWeight = product.materialWeight.takeIf { it > 0 } ?: grossWeight,
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
                
                // Calculate per-item total, then multiply by quantity
                val perItemTotal = result.totalProductPrice
                val itemSubTotal = perItemTotal * item.quantity
                
                // For display in table, show per-item values
                val metalCost = result.goldPrice
                val makingCharges = result.labourCharges
                val stoneAmount = result.kundanPrice + result.jarkanPrice
                val weight = result.newWeight
                
                // GST is calculated on (subtotal - discountAmount), same as ReceiptScreen
                // For individual items, calculate GST proportionally based on item's share
                val taxableAmount = order.subtotal - order.discountAmount
                val itemGst = if (order.isGstIncluded && taxableAmount > 0) {
                    (itemSubTotal / order.subtotal) * order.gstAmount
                } else {
                    0.0
                }
                val itemTotal = itemSubTotal + itemGst

                totalQty += item.quantity
                totalWeight += weight * item.quantity

                val values = listOf(
                    product.name,
                    materialType,
                    "${item.quantity}",
                    String.format("%.2f", weight * item.quantity),
                    formatCurrency(goldRatePerGram),
                    formatCurrency(metalCost * item.quantity),
                    formatCurrency(makingCharges * item.quantity),
                    formatCurrency(itemSubTotal),
                    if (order.isGstIncluded) formatCurrency(itemGst) else "0.00",
                    formatCurrency(itemTotal)
                )

                values.forEachIndexed { index, value ->
                    val columnWidth = tableWidth * columnWidths[index]
                    val isNumericColumn = index >= 2
                    val lineWidth = getTextWidth(value, helvetica, 7f)
                    val textOffset = if (isNumericColumn) {
                        maxOf(1f, columnWidth - lineWidth - 3f)
                    } else {
                        2f
                    }

                    contentStream.beginText()
                    contentStream.newLineAtOffset(xPos + textOffset, yPos + itemRowHeight/2)
                    contentStream.showText(value)
                    contentStream.endText()

                    // Draw vertical line after text operations
                    if (index < values.size - 1) {
                        contentStream.moveTo(xPos + columnWidth, yPos)
                        contentStream.lineTo(xPos + columnWidth, yPos + itemRowHeight)
                        contentStream.stroke()
                    }

                    xPos += columnWidth
                }
            }

            // Draw total row with actual order totals
            yPos -= rowHeight
            contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f)
            contentStream.addRect(margin, yPos, tableWidth, rowHeight)
            contentStream.fill()
            contentStream.setNonStrokingColor(0f, 0f, 0f)

            contentStream.addRect(margin, yPos, tableWidth, rowHeight)
            contentStream.stroke()

            contentStream.setFont(helveticaBold, 8f)
            xPos = margin + 1f

            // Use stored order values for totals (same as ReceiptScreen)
            // Note: order.subtotal already includes all item prices calculated correctly
            val totalValues = listOf(
                "TOTAL", "", "${totalQty}", String.format("%.2f", totalWeight),
                "", formatCurrency(order.subtotal), "",
                formatCurrency(order.subtotal),
                if (order.isGstIncluded) formatCurrency(order.gstAmount) else "0.00",
                formatCurrency(order.totalAmount)
            )

            totalValues.forEachIndexed { index, value ->
                val columnWidth = tableWidth * columnWidths[index]

                val isNumericColumn = index >= 2
                val textWidth = getTextWidth(value, helveticaBold, 8f)
                val textOffset = if (isNumericColumn && value.isNotEmpty()) {
                    maxOf(1f, columnWidth - textWidth - 3f)
                } else {
                    2f
                }

                contentStream.beginText()
                contentStream.newLineAtOffset(xPos + textOffset, yPos + 8f)
                contentStream.showText(value)
                contentStream.endText()

                // Draw vertical line after text operations
                if (index < totalValues.size - 1) {
                    contentStream.moveTo(xPos + columnWidth, yPos)
                    contentStream.lineTo(xPos + columnWidth, yPos + rowHeight)
                    contentStream.stroke()
                }

                xPos += columnWidth
            }

        } catch (e: Exception) {
            println("Error in drawCorrectItemsTable: ${e.message}")
        }

        return yPos
    }

    private fun drawPaymentSection(
        contentStream: PDPageContentStream,
        order: Order,
        margin: Float,
        startY: Float,
        pageWidth: Float
    ): Float {
        var yPos = startY - 20f
        val centerX = pageWidth / 2

        try {
            // Draw line
            contentStream.setLineWidth(1f)
            contentStream.moveTo(margin, yPos)
            contentStream.lineTo(pageWidth - margin, yPos)
            contentStream.stroke()
            yPos -= 15f

            // Left side - Payment Summary
            contentStream.setFont(helveticaBold, 10f)
            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText("Payment Summary")
            contentStream.endText()
            yPos -= 20f

            // Payment breakdown using stored order values (same as ReceiptScreen)
            // ReceiptScreen uses: itemSubtotal + gstAmount - discountAmount = finalTotal
            // But displays stored transaction values
            contentStream.setFont(helvetica, 9f)

            // Subtotal (use stored order.subtotal, same as ReceiptScreen displays calculated itemSubtotal)
            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText("Subtotal:")
            contentStream.endText()
            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 120f, yPos)
            contentStream.showText("Rs.${formatCurrency(order.subtotal)}")
            contentStream.endText()
            yPos -= 15f

            // Discount (use stored order.discountAmount, same as ReceiptScreen)
            if (order.discountAmount > 0) {
                contentStream.beginText()
                contentStream.newLineAtOffset(margin, yPos)
                contentStream.showText("Discount:")
                contentStream.endText()
                contentStream.beginText()
                contentStream.newLineAtOffset(margin + 120f, yPos)
                contentStream.showText("-Rs.${formatCurrency(order.discountAmount)}")
                contentStream.endText()
                yPos -= 15f
            }

            // GST (use stored order.gstAmount, same as ReceiptScreen)
            // Calculate GST percentage from stored gstAmount (same as ReceiptScreen line 745-746)
            if (order.isGstIncluded && order.gstAmount > 0) {
                val taxableAmount = order.subtotal - order.discountAmount
                val gstPercentage = if (taxableAmount > 0 && order.gstAmount > 0) {
                    ((order.gstAmount / taxableAmount) * 100).toInt()
                } else {
                    0
                }
                
                contentStream.beginText()
                contentStream.newLineAtOffset(margin, yPos)
                contentStream.showText("GST (${gstPercentage}%):")
                contentStream.endText()
                contentStream.beginText()
                contentStream.newLineAtOffset(margin + 120f, yPos)
                contentStream.showText("Rs.${formatCurrency(order.gstAmount)}")
                contentStream.endText()
                yPos -= 15f
            }

            // Draw line before total
            contentStream.setLineWidth(0.5f)
            contentStream.moveTo(margin, yPos)
            contentStream.lineTo(margin + 200f, yPos)
            contentStream.stroke()
            yPos -= 10f

            // Total Amount (use stored order.totalAmount, same as ReceiptScreen finalTotal)
            // ReceiptScreen: finalTotal = itemSubtotal + gstAmount - discountAmount
            // But we use stored order.totalAmount which should match
            contentStream.setFont(helveticaBold, 10f)
            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText("Total Amount:")
            contentStream.endText()
            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 120f, yPos)
            contentStream.showText("Rs.${formatCurrency(order.totalAmount)}")
            contentStream.endText()
            yPos -= 25f

            // Right side - Payment Method Details
            val rightColumnX = centerX + 50f
            var rightY = startY - 35f

            contentStream.setFont(helveticaBold, 10f)
            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText("Payment Details")
            contentStream.endText()
            rightY -= 20f

            contentStream.setFont(helvetica, 9f)
            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText("Payment Status: ${order.paymentStatus.name}")
            contentStream.endText()
            rightY -= 15f

            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText("Amount Paid: Rs.${formatCurrency(order.totalAmount)}")
            contentStream.endText()
            rightY -= 15f

            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText("Status: PAID")
            contentStream.endText()

        } catch (e: Exception) {
            println("Error in drawPaymentSection: ${e.message}")
        }

        return yPos
    }

    private fun drawAmountInWords(
        contentStream: PDPageContentStream,
        amount: Double,
        margin: Float,
        yPos: Float,
        pageWidth: Float
    ) {
        try {
            val amountInWords = convertAmountToWords(amount)

            contentStream.setFont(helvetica, 9f)
            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos - 15f)
            contentStream.showText("Amount in Words: $amountInWords")
            contentStream.endText()
        } catch (e: Exception) {
            println("Error in drawAmountInWords: ${e.message}")
        }
    }

    private fun convertAmountToWords(amount: Double): String {
        val intAmount = amount.toInt()
        return when {
            intAmount < 1000 -> "Rupees $intAmount Only"
            intAmount < 100000 -> "Rupees ${intAmount / 1000} Thousand ${intAmount % 1000} Only"
            intAmount < 10000000 -> "Rupees ${intAmount / 100000} Lakh ${(intAmount % 100000) / 1000} Thousand Only"
            else -> "Rupees ${intAmount / 10000000} Crore ${(intAmount % 10000000) / 100000} Lakh Only"
        }
    }

    private fun getTextWidth(text: String, font: PDType1Font, fontSize: Float): Float {
        return try {
            font.getStringWidth(text) * fontSize / 1000f
        } catch (e: Exception) {
            text.length * fontSize * 0.6f
        }
    }

    private fun parseWeight(weightStr: String): Double {
        return try {
            weightStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 4.0
        } catch (e: Exception) {
            4.0
        }
    }

    private fun formatCurrency(amount: Double): String {
        // Use CurrencyFormatter for consistent Indian currency formatting
        return CurrencyFormatter.formatRupeesNumber(amount, includeDecimals = true)
    }

    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength - 3) + "..."
        } else {
            text
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