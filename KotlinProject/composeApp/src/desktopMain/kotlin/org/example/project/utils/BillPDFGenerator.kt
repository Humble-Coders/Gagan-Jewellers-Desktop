package org.example.project.utils

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.example.project.data.Order
import org.example.project.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class BillPDFGenerator {

    private val helveticaBold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
    private val helvetica = PDType1Font(Standard14Fonts.FontName.HELVETICA)
    private val courierBold = PDType1Font(Standard14Fonts.FontName.COURIER_BOLD)
    private val courier = PDType1Font(Standard14Fonts.FontName.COURIER)

    suspend fun generateBill(
        order: Order,
        customer: User,
        outputPath: Path,
        companyName: String = "GAGAN JEWELLERS",
        companyAddress: String = "123 Jewelry Street, Golden City, GC 12345",
        companyPhone: String = "+91 98765 43210",
        companyEmail: String = "info@gaganjewellers.com",
        gstNumber: String = "22AAAAA0000A1Z5"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            PDDocument().use { document ->
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)

                PDPageContentStream(document, page).use { contentStream ->
                    val pageWidth = page.mediaBox.width
                    val pageHeight = page.mediaBox.height
                    val margin = 30f
                    var yPosition = pageHeight - margin

                    // Draw outer border
                    drawBorder(contentStream, margin - 10f, pageWidth, pageHeight)

                    // Customer Copy Header
                    yPosition = drawCustomerCopyHeader(contentStream, margin, yPosition, pageWidth)

                    // Main Invoice Header
                    yPosition = drawMainHeader(contentStream, companyName, companyAddress,
                        companyPhone, gstNumber, order, customer, margin, yPosition, pageWidth)

                    // Gold Rate Information
                    yPosition = drawGoldRateInfo(contentStream, margin, yPosition, pageWidth)

                    // Items Table
                    yPosition = drawProfessionalItemsTable(contentStream, order, margin, yPosition, pageWidth)

                    // Payment and Total Section
                    yPosition = drawPaymentSection(contentStream, order, margin, yPosition, pageWidth)

                    // Amount in Words
                    drawAmountInWords(contentStream, order.totalAmount, margin, yPosition, pageWidth)
                }

                document.save(outputPath.toFile())
                true
            }
        } catch (e: Exception) {
            println("Error generating PDF: ${e.message}")
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

            yPos -= 20f
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
            var text = "TAX INVOICE"
            var textWidth = getTextWidth(text, helveticaBold, 16f)
            contentStream.beginText()
            contentStream.newLineAtOffset(centerX - textWidth/2, yPos)
            contentStream.showText(text)
            contentStream.endText()
            yPos -= 30f

            // Draw horizontal line
            contentStream.setLineWidth(1f)
            contentStream.moveTo(margin, yPos)
            contentStream.lineTo(pageWidth - margin, yPos)
            contentStream.stroke()
            yPos -= 15f

            // Left side - Company Details
            contentStream.setFont(helveticaBold, 14f)
            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText(companyName)
            contentStream.endText()
            yPos -= 15f

            contentStream.setFont(helvetica, 9f)
            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText(address)
            contentStream.endText()
            yPos -= 12f

            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText("Phone Number : $phone")
            contentStream.endText()
            yPos -= 12f

            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText("GSTIN : $gstNumber")
            contentStream.endText()

            // Right side - Customer Details and Invoice Info
            val rightColumnX = pageWidth * 0.6f
            var rightY = startY - 45f

            val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
            val invoiceDate = dateFormat.format(Date(order.timestamp))

            contentStream.setFont(helveticaBold, 10f)
            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText("DOC//SAL/${order.id.takeLast(4)} Date : $invoiceDate")
            contentStream.endText()
            rightY -= 20f

            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText("CUSTOMER DETAILS:")
            contentStream.endText()
            rightY -= 15f

            contentStream.setFont(helvetica, 9f)
            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText(customer.name.uppercase())
            contentStream.endText()
            rightY -= 12f

            if (customer.phone.isNotEmpty()) {
                contentStream.beginText()
                contentStream.newLineAtOffset(rightColumnX, rightY)
                contentStream.showText(customer.phone)
                contentStream.endText()
                rightY -= 12f
            }

            if (customer.address.isNotEmpty()) {
                contentStream.beginText()
                contentStream.newLineAtOffset(rightColumnX, rightY)
                contentStream.showText(customer.address.uppercase())
                contentStream.endText()
                rightY -= 12f
            }

            if (customer.email.isNotEmpty()) {
                contentStream.beginText()
                contentStream.newLineAtOffset(rightColumnX, rightY)
                contentStream.showText("Phone Number : ${customer.phone}")
                contentStream.endText()
                rightY -= 12f
            }

            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText("Encircle Id : ${order.id}")
            contentStream.endText()

            yPos -= 20f
        } catch (e: Exception) {
            println("Error in drawMainHeader: ${e.message}")
        }

        return yPos
    }

    private fun drawGoldRateInfo(
        contentStream: PDPageContentStream,
        margin: Float,
        startY: Float,
        pageWidth: Float
    ): Float {
        var yPos = startY - 10f

        try {
            // Draw line
            contentStream.setLineWidth(1f)
            contentStream.moveTo(margin, yPos)
            contentStream.lineTo(pageWidth - margin, yPos)
            contentStream.stroke()
            yPos -= 15f

            contentStream.setFont(helvetica, 8f)
            val rateText = "Standard Rate of 24 Karat/22 Karat/18 Karat/14 Karat Gold Rs: 7063.58 Rs/6475.00 Rs/5297.72 Rs/4120.43 Rs/ Standard Rate of 95.00% Purity Platinum Rs: 3,530.00"

            // Split long text into multiple lines if needed
            val maxWidth = pageWidth - (2 * margin)
            val textWidth = getTextWidth(rateText, helvetica, 8f)

            if (textWidth > maxWidth) {
                val words = rateText.split(" ")
                var currentLine = ""
                var lineY = yPos

                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    val testWidth = getTextWidth(testLine, helvetica, 8f)

                    if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                        contentStream.beginText()
                        contentStream.newLineAtOffset(margin, lineY)
                        contentStream.showText(currentLine)
                        contentStream.endText()
                        lineY -= 10f
                        currentLine = word
                    } else {
                        currentLine = testLine
                    }
                }

                if (currentLine.isNotEmpty()) {
                    contentStream.beginText()
                    contentStream.newLineAtOffset(margin, lineY)
                    contentStream.showText(currentLine)
                    contentStream.endText()
                    yPos = lineY
                }
            } else {
                contentStream.beginText()
                contentStream.newLineAtOffset(margin, yPos)
                contentStream.showText(rateText)
                contentStream.endText()
            }

            yPos -= 15f

            // Draw line
            contentStream.moveTo(margin, yPos)
            contentStream.lineTo(pageWidth - margin, yPos)
            contentStream.stroke()

        } catch (e: Exception) {
            println("Error in drawGoldRateInfo: ${e.message}")
        }

        return yPos
    }

    private fun drawProfessionalItemsTable(
        contentStream: PDPageContentStream,
        order: Order,
        margin: Float,
        startY: Float,
        pageWidth: Float
    ): Float {
        var yPos = startY - 10f
        val tableWidth = pageWidth - (2 * margin)
        val rowHeight = 25f

        try {
            // Table headers
            val headers = listOf(
                "Variant no/\nProduct description\n/Fineness",
                "Purity\nHSN",
                "Net\nQty",
                "Gross\nProduct\nWeight\n(grams)",
                "Net Stone\nWeight\n(Carats/grams)",
                "Net Metal\nWeight\n(grams)",
                "Gross\nProduct\nPrice\n(Rs.)",
                "Making Charges (Rs.)\nWastage%\nHM Charges",
                "Scheme*\nDiscount\n(Rs.)",
                "SGST\n(1.50%)",
                "CGST\n(1.50%)",
                "Product\nValue\n(Rs.)"
            )

            // Adjusted column widths to prevent overflow
            val columnWidths = listOf(0.14f, 0.08f, 0.05f, 0.08f, 0.08f, 0.08f, 0.08f, 0.12f, 0.07f, 0.07f, 0.07f, 0.08f)
            // Draw table border
            contentStream.setLineWidth(1f)
            contentStream.addRect(margin, yPos - (rowHeight * 2), tableWidth, rowHeight * 2)
            contentStream.stroke()

            // Fill header background
            contentStream.setNonStrokingColor(0.95f, 0.95f, 0.95f)
            contentStream.addRect(margin, yPos - (rowHeight * 2), tableWidth, rowHeight * 2)
            contentStream.fill()
            contentStream.setNonStrokingColor(0f, 0f, 0f)

            // Draw header text
            contentStream.setFont(helvetica, 6.5f) // Reduced font size
            var xPos = margin + 1f

            headers.forEachIndexed { index, header ->
                val columnWidth = tableWidth * columnWidths[index]
                val lines = header.split("\n")
                var textY = yPos - 6f

                lines.forEach { line ->
                    // Center align text in column
                    val lineWidth = getTextWidth(line, helvetica, 6.5f)
                    val centerOffset = (columnWidth - lineWidth) / 2

                    contentStream.beginText()
                    contentStream.newLineAtOffset(xPos + centerOffset, textY)
                    contentStream.showText(line)
                    contentStream.endText()
                    textY -= 7f
                }

                // Draw vertical line
                contentStream.moveTo(xPos + columnWidth, yPos)
                contentStream.lineTo(xPos + columnWidth, yPos - (rowHeight * 2))
                contentStream.stroke()

                xPos += columnWidth
            }

            yPos -= rowHeight * 2

            // Draw items
            contentStream.setFont(helvetica, 7f) // Reduced font size for data
            var totalQty = 0
            var totalWeight = 0.0
            var totalValue = 0.0

            order.items.forEach { item ->
                val itemRowHeight = rowHeight * 1.5f
                yPos -= itemRowHeight

                // Draw row border
                contentStream.addRect(margin, yPos, tableWidth, itemRowHeight)
                contentStream.stroke()

                xPos = margin + 1f
                val weight = parseWeight(item.product.weight)
                val makingCharges = weight * 100 // Assuming Rs 100 per gram making charges
                val itemTotal = (item.product.price * item.quantity) + makingCharges
                val sgst = itemTotal * 0.015 // 1.5% SGST
                val cgst = itemTotal * 0.015 // 1.5% CGST
                val finalValue = itemTotal + sgst + cgst

                totalQty += item.quantity
                totalWeight += weight * item.quantity
                totalValue += finalValue

                val values = listOf(
                    "${item.product.id}\n${truncateText(item.product.name, 15)}\n${item.product.materialType}",
                    "G*- 22Karat\n71131910",
                    "${item.quantity}N",
                    String.format("%.3f", weight),
                    "0.000\n0.000",
                    String.format("%.3f", weight),
                    formatCurrencyShort(item.product.price),
                    String.format("%.0f", makingCharges) + "\n20.00 %\n45.00",
                    formatCurrencyShort(0.0),
                    formatCurrencyShort(sgst),
                    formatCurrencyShort(cgst),
                    formatCurrencyShort(finalValue)
                )

                values.forEachIndexed { index, value ->
                    val columnWidth = tableWidth * columnWidths[index]
                    val lines = value.split("\n")
                    var textY = yPos + itemRowHeight - 8f

                    lines.forEach { line ->
                        // For numerical columns (price columns), right align
                        val isNumericColumn = index >= 6 // Price columns
                        val lineWidth = getTextWidth(line, helvetica, 7f)
                        val textOffset = if (isNumericColumn) {
                            maxOf(1f, columnWidth - lineWidth - 3f) // Right align with minimum 1f offset
                        } else {
                            2f // Left align with small margin
                        }

                        contentStream.beginText()
                        contentStream.newLineAtOffset(xPos + textOffset, textY)
                        contentStream.showText(line)
                        contentStream.endText()
                        textY -= 8f
                    }

                    // Draw vertical line
                    contentStream.moveTo(xPos + columnWidth, yPos)
                    contentStream.lineTo(xPos + columnWidth, yPos + itemRowHeight)
                    contentStream.stroke()

                    xPos += columnWidth
                }
            }

            // Draw total row
            yPos -= rowHeight
            contentStream.setNonStrokingColor(0.9f, 0.9f, 0.9f)
            contentStream.addRect(margin, yPos, tableWidth, rowHeight)
            contentStream.fill()
            contentStream.setNonStrokingColor(0f, 0f, 0f)

            contentStream.addRect(margin, yPos, tableWidth, rowHeight)
            contentStream.stroke()

            contentStream.setFont(helveticaBold, 8f)
            xPos = margin + 1f

            val totalValues = listOf(
                "Total", "", "${totalQty}N", String.format("%.3f", totalWeight),
                "0.000", String.format("%.3f", totalWeight), "", "",
                "", "", "", formatCurrencyShort(totalValue)
            )

            totalValues.forEachIndexed { index, value ->
                val columnWidth = tableWidth * columnWidths[index]

                // Right align for numeric columns
                val isNumericColumn = index >= 6
                val textWidth = getTextWidth(value, helveticaBold, 8f)
                val textOffset = if (isNumericColumn && value.isNotEmpty()) {
                    maxOf(1f, columnWidth - textWidth - 3f) // Right align with minimum 1f offset
                } else {
                    2f
                }

                contentStream.beginText()
                contentStream.newLineAtOffset(xPos + textOffset, yPos + 8f)
                contentStream.showText(value)
                contentStream.endText()

                // Draw vertical line
                contentStream.moveTo(xPos + columnWidth, yPos)
                contentStream.lineTo(xPos + columnWidth, yPos + rowHeight)
                contentStream.stroke()

                xPos += columnWidth
            }

        } catch (e: Exception) {
            println("Error in drawProfessionalItemsTable: ${e.message}")
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

            // Left side - Payment Details
            contentStream.setFont(helveticaBold, 10f)
            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText("Total Qty Purchased")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 150f, yPos)
            contentStream.showText("${order.items.sumOf { it.quantity }}N")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(centerX + 50f, yPos)
            contentStream.showText("Product Total Value")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(pageWidth - margin - 80f, yPos)
            contentStream.showText(formatCurrency(order.totalAmount))
            contentStream.endText()
            yPos -= 20f

            // Payment Details section
            contentStream.setFont(helveticaBold, 9f)
            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText("Payment Details")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(centerX + 50f, yPos)
            contentStream.showText("Additional Other Charges")
            contentStream.endText()
            yPos -= 15f

            // Draw table for payment details with proper spacing
            val paymentTableHeight = 85f // Increased height
            contentStream.addRect(margin, yPos - paymentTableHeight, centerX - margin - 10f, paymentTableHeight)
            contentStream.stroke()

            contentStream.addRect(centerX + 10f, yPos - paymentTableHeight, pageWidth - centerX - margin - 10f, paymentTableHeight)
            contentStream.stroke()

            // Payment mode details with better spacing
            contentStream.setFont(helvetica, 8f)
            var paymentY = yPos - 15f

            // Header row
            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 5f, paymentY)
            contentStream.showText("Payment Mode")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 5f + 100f, paymentY) // Fixed position
            contentStream.showText("Amount (Rs)")
            contentStream.endText()
            paymentY -= 18f

            // Payment entries
            val paymentMethod = when (order.paymentMethod.name) {
                "CARD" -> "HDFC"
                "CASH_ON_DELIVERY" -> "CASH"
                else -> order.paymentMethod.name
            }

            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 5f, paymentY)
            contentStream.showText(paymentMethod)
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 5f + 100f, paymentY)
            contentStream.showText(formatCurrency(order.totalAmount - 10.0))
            contentStream.endText()
            paymentY -= 18f

            if (order.totalAmount > 10.0) {
                contentStream.beginText()
                contentStream.newLineAtOffset(margin + 5f, paymentY)
                contentStream.showText("CASH")
                contentStream.endText()

                contentStream.beginText()
                contentStream.newLineAtOffset(margin + 5f + 100f, paymentY)
                contentStream.showText("10.00")
                contentStream.endText()
                paymentY -= 18f
            }

            // Total Amount Paid with proper spacing
            contentStream.setFont(helveticaBold, 9f)
            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 5f, paymentY)
            contentStream.showText("Total Amount Paid")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 5f + 100f, paymentY)
            contentStream.showText(formatCurrency(order.totalAmount))
            contentStream.endText()

            // Right side - Other charges and totals with proper spacing
            var rightY = yPos - 15f
            contentStream.setFont(helvetica, 8f)

            contentStream.beginText()
            contentStream.newLineAtOffset(centerX + 15f, rightY)
            contentStream.showText("Other charges:")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(pageWidth - margin - 60f, rightY) // Fixed right position
            contentStream.showText("0.00")
            contentStream.endText()
            rightY -= 18f

            contentStream.beginText()
            contentStream.newLineAtOffset(centerX + 15f, rightY)
            contentStream.showText("Total Other charges value")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(pageWidth - margin - 60f, rightY)
            contentStream.showText("0.00")
            contentStream.endText()
            rightY -= 18f

            contentStream.beginText()
            contentStream.newLineAtOffset(centerX + 15f, rightY)
            contentStream.showText("Net invoice values")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(pageWidth - margin - 60f, rightY)
            contentStream.showText(formatCurrency(order.totalAmount))
            contentStream.endText()
            rightY -= 18f

            if (order.discountAmount > 0) {
                contentStream.beginText()
                contentStream.newLineAtOffset(centerX + 15f, rightY)
                contentStream.showText("Discount Details : PRODUCT LEVEL DISCOUNT: ${formatCurrency(order.discountAmount)}")
                contentStream.endText()
                rightY -= 12f
            }

            // Total Amount to be paid with proper spacing
            contentStream.setFont(helveticaBold, 9f)
            contentStream.beginText()
            contentStream.newLineAtOffset(centerX + 15f, rightY)
            contentStream.showText("Total Amount to be paid")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(pageWidth - margin - 60f, rightY)
            contentStream.showText(formatCurrency(order.totalAmount))
            contentStream.endText()

            yPos -= paymentTableHeight + 10f

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
            contentStream.showText("Value in words :- $amountInWords")
            contentStream.endText()
        } catch (e: Exception) {
            println("Error in drawAmountInWords: ${e.message}")
        }
    }

    private fun convertAmountToWords(amount: Double): String {
        val intAmount = amount.toInt()
        return when {
            intAmount < 1000 -> "$intAmount Only"
            intAmount < 100000 -> "${intAmount / 1000} thousand ${intAmount % 1000} Only"
            intAmount < 10000000 -> "${intAmount / 100000} lakh ${(intAmount % 100000) / 1000} thousand ${intAmount % 1000} Only"
            else -> "${intAmount / 10000000} crore ${(intAmount % 10000000) / 100000} lakh Only"
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
            weightStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 4.0 // Default weight
        } catch (e: Exception) {
            4.0
        }
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
            formatter.maximumFractionDigits = 2
            formatter.format(amount)
        } catch (e: Exception) {
            String.format("%.2f", amount)
        }
    }

    // Short format for table cells to prevent overflow
    private fun formatCurrencyShort(amount: Double): String {
        return try {
            if (amount >= 100000) {
                String.format("%.0fK", amount / 1000)
            } else {
                String.format("%.0f", amount)
            }
        } catch (e: Exception) {
            String.format("%.0f", amount)
        }
    }

    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength - 3) + "..."
        } else {
            text
        }
    }
}