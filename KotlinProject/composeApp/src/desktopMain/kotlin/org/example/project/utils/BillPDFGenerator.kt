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
        gstNumber: String = "22AAAAA0000A1Z5",
        goldPricePerGram: Double = 7063.58,
        silverPricePerGram: Double = 75.0
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
                    val tableResult = drawProfessionalItemsTable(
                        contentStream, order, goldPricePerGram, silverPricePerGram, margin, yPosition, pageWidth
                    )
                    yPosition = tableResult.first
                    val calculatedTotal = tableResult.second

                    // Payment and Total Section
                    yPosition = drawPaymentSection(contentStream, order, calculatedTotal, margin, yPosition, pageWidth)

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
            var text = "TAX INVOICE"
            var textWidth = getTextWidth(text, helveticaBold, 16f)
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
            val invoiceDate = dateFormat.format(Date(order.timestamp))

            contentStream.setFont(helveticaBold, 10f)
            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText("DOC//SAL/${order.id.takeLast(4)} Date : $invoiceDate")
            contentStream.endText()
            rightY -= 25f

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
            contentStream.showText("Order Id : ${order.id}")
            contentStream.endText()

            yPos -= 30f
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
        goldPricePerGram: Double,
        silverPricePerGram: Double,
        margin: Float,
        startY: Float,
        pageWidth: Float
    ): Pair<Float, Double> {
        var yPos = startY - 10f
        val tableWidth = pageWidth - (2 * margin)
        val rowHeight = 25f

        try {
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
            contentStream.setFont(helvetica, 6.5f)
            var xPos = margin + 1f

            headers.forEachIndexed { index, header ->
                val columnWidth = tableWidth * columnWidths[index]
                val lines = header.split("\n")
                var textY = yPos - 6f

                lines.forEach { line ->
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

            // Calculate totals from actual order data
            var totalQty = 0
            var totalWeight = 0.0
            var totalGrossValue = 0.0
            var totalMakingCharges = 0.0
            var totalSGST = 0.0
            var totalCGST = 0.0

            // Draw items
            contentStream.setFont(helvetica, 7f)

            order.items.forEach { item ->
                val itemRowHeight = rowHeight * 1.5f
                yPos -= itemRowHeight

                // Draw row border
                contentStream.addRect(margin, yPos, tableWidth, itemRowHeight)
                contentStream.stroke()

                xPos = margin + 1f
                val weight = parseWeight(item.product.weight)
                val grossPrice = item.product.price * item.quantity
                val makingCharges = weight * 100 // Rs 100 per gram making charges
                val grossTotal = grossPrice + makingCharges
                val sgst = grossTotal * 0.015 // 1.5% SGST
                val cgst = grossTotal * 0.015 // 1.5% CGST
                val finalValue = grossTotal + sgst + cgst

                totalQty += item.quantity
                totalWeight += weight * item.quantity
                totalGrossValue += grossPrice
                totalMakingCharges += makingCharges
                totalSGST += sgst
                totalCGST += cgst
                var totalFinalValue = finalValue

                val values = listOf(
                    "${item.product.id}\n${truncateText(item.product.name, 15)}\n${item.product.materialType}",
                    "G*- 22Karat\n71131910",
                    "${item.quantity}N",
                    String.format("%.3f", weight),
                    "0.000\n0.000",
                    String.format("%.3f", weight),
                    formatCurrencyShort(grossPrice),
                    String.format("%.0f", makingCharges) + "\n20.00 %\n45.00",
                    formatCurrencyShort(sgst),
                    formatCurrencyShort(cgst),
                    formatCurrencyShort(finalValue)
                )

                values.forEachIndexed { index, value ->
                    val columnWidth = tableWidth * columnWidths[index]
                    val lines = value.split("\n")
                    var textY = yPos + itemRowHeight - 8f

                    lines.forEach { line ->
                        val isNumericColumn = index >= 6
                        val lineWidth = getTextWidth(line, helvetica, 7f)
                        val textOffset = if (isNumericColumn) {
                            maxOf(1f, columnWidth - lineWidth - 3f)
                        } else {
                            2f
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
                "0.000", String.format("%.3f", totalWeight),
                formatCurrencyShort(totalGrossValue), formatCurrencyShort(totalMakingCharges),
                "", formatCurrencyShort(totalSGST), formatCurrencyShort(totalCGST),
                formatCurrencyShort(order.totalAmount)
            )

            totalValues.forEachIndexed { index, value ->
                val columnWidth = tableWidth * columnWidths[index]

                val isNumericColumn = index >= 6
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

                // Draw vertical line
                contentStream.moveTo(xPos + columnWidth, yPos)
                contentStream.lineTo(xPos + columnWidth, yPos + rowHeight)
                contentStream.stroke()

                xPos += columnWidth
            }

        } catch (e: Exception) {
            println("Error in drawProfessionalItemsTable: ${e.message}")
        }

        return Pair(yPos, order.totalAmount)
    }

    private fun drawPaymentSection(
        contentStream: PDPageContentStream,
        order: Order,
        calculatedTotal: Double,
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

            // Draw table for payment details
            val paymentTableHeight = 85f
            contentStream.addRect(margin, yPos - paymentTableHeight, centerX - margin - 10f, paymentTableHeight)
            contentStream.stroke()

            contentStream.addRect(centerX + 10f, yPos - paymentTableHeight, pageWidth - centerX - margin - 10f, paymentTableHeight)
            contentStream.stroke()

            // Payment mode details
            contentStream.setFont(helvetica, 8f)
            var paymentY = yPos - 15f

            // Header row
            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 5f, paymentY)
            contentStream.showText("Payment Mode")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 105f, paymentY)
            contentStream.showText("Amount (Rs)")
            contentStream.endText()
            paymentY -= 18f

            // Use actual payment method from order
            val paymentMethod = when (order.paymentMethod.name) {
                "UPI" -> "UPI"
                "CARD" -> "HDFC"
                "NET_BANKING" -> "NET_BANKING"
                "CASH_ON_DELIVERY" -> "CASH"
                else -> order.paymentMethod.name
            }

            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 5f, paymentY)
            contentStream.showText(paymentMethod)
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 105f, paymentY)
            contentStream.showText(formatCurrency(order.totalAmount))
            contentStream.endText()
            paymentY -= 25f

            // Total Amount Paid
            contentStream.setFont(helveticaBold, 9f)
            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 5f, paymentY)
            contentStream.showText("Total Amount Paid")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 105f, paymentY)
            contentStream.showText(formatCurrency(order.totalAmount))
            contentStream.endText()

            // Right side - Other charges and totals
            var rightY = yPos - 15f
            contentStream.setFont(helvetica, 8f)

            contentStream.beginText()
            contentStream.newLineAtOffset(centerX + 15f, rightY)
            contentStream.showText("Other charges:")
            contentStream.endText()

            contentStream.beginText()
            contentStream.newLineAtOffset(pageWidth - margin - 60f, rightY)
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

            // Total Amount to be paid
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
            weightStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 4.0
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