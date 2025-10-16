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
        goldPricePerGram: Double = 6080.0,
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

                    drawBorder(contentStream, margin - 10f, pageWidth, pageHeight)

                    yPosition = drawCustomerCopyHeader(contentStream, margin, yPosition, pageWidth)

                    yPosition = drawMainHeader(contentStream, companyName, companyAddress,
                        companyPhone, gstNumber, order, customer, margin, yPosition, pageWidth)

                    yPosition = drawGoldRateInfo(contentStream, margin, yPosition, pageWidth, goldPricePerGram, silverPricePerGram)

                    yPosition = drawCorrectItemsTable(
                        contentStream, order, goldPricePerGram, silverPricePerGram, margin, yPosition, pageWidth
                    )

                    yPosition = drawPaymentSection(contentStream, order, margin, yPosition, pageWidth)

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
            val invoiceDate = dateFormat.format(Date(order.timestamp))

            contentStream.setFont(helveticaBold, 10f)
            contentStream.beginText()
            contentStream.newLineAtOffset(rightColumnX, rightY)
            contentStream.showText("DOC//SAL/${order.id.takeLast(4)} Date : $invoiceDate")
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
        pageWidth: Float,
        goldPrice: Double,
        silverPrice: Double
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
            val rateText = "Gold Rate: Rs.${formatCurrency(goldPrice)}/g | Silver Rate: Rs.${formatCurrency(silverPrice)}/g | Making Charges: Rs.100/g"

            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText(rateText)
            contentStream.endText()

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

    private fun drawCorrectItemsTable(
        contentStream: PDPageContentStream,
        order: Order,
        goldPricePerGram: Double,
        silverPricePerGram: Double,
        margin: Float,
        startY: Float,
        pageWidth: Float
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
                "Making\nCharges",
                "Sub\nTotal",
                "GST\n(18%)",
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
            val totalMakingCharges = order.makingCharges
            val totalGst = order.gstAmount
            val grandTotal = order.totalAmount

            // Draw items with correct calculations
            contentStream.setFont(helvetica, 7f)

            order.items.forEach { item ->
                val itemRowHeight = rowHeight * 1.2f
                yPos -= itemRowHeight

                // Draw row border
                contentStream.addRect(margin, yPos, tableWidth, itemRowHeight)
                contentStream.stroke()

                xPos = margin + 1f
                val weight = parseWeight(item.product.weight)
                val pricePerGram = when {
                    item.product.materialType.contains("gold", ignoreCase = true) -> goldPricePerGram
                    item.product.materialType.contains("silver", ignoreCase = true) -> silverPricePerGram
                    else -> goldPricePerGram
                }
                val metalCost = weight * item.quantity * pricePerGram
                val makingCharges = weight * item.quantity * 100.0
                val subTotal = metalCost + makingCharges
                val gst = if (order.isGstIncluded) subTotal * 0.18 else 0.0
                val itemTotal = subTotal + gst

                totalQty += item.quantity
                totalWeight += weight * item.quantity

                val values = listOf(
                    truncateText(item.product.name, 20),
                    item.product.materialType,
                    "${item.quantity}",
                    String.format("%.2f", weight * item.quantity),
                    formatCurrency(pricePerGram),
                    formatCurrency(metalCost),
                    formatCurrency(makingCharges),
                    formatCurrency(subTotal),
                    if (order.isGstIncluded) formatCurrency(gst) else "0.00",
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

            val totalValues = listOf(
                "TOTAL", "", "${totalQty}", String.format("%.2f", totalWeight),
                "", formatCurrency(totalMetalCost), formatCurrency(totalMakingCharges),
                formatCurrency(totalMetalCost + totalMakingCharges),
                if (order.isGstIncluded) formatCurrency(totalGst) else "0.00",
                formatCurrency(grandTotal)
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

            // Payment breakdown using actual order values
            contentStream.setFont(helvetica, 9f)

            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText("Metal Cost:")
            contentStream.endText()
            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 120f, yPos)
            contentStream.showText("Rs.${formatCurrency(order.subtotal)}")
            contentStream.endText()
            yPos -= 15f

            contentStream.beginText()
            contentStream.newLineAtOffset(margin, yPos)
            contentStream.showText("Making Charges:")
            contentStream.endText()
            contentStream.beginText()
            contentStream.newLineAtOffset(margin + 120f, yPos)
            contentStream.showText("Rs.${formatCurrency(order.makingCharges)}")
            contentStream.endText()
            yPos -= 15f

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

            if (order.isGstIncluded && order.gstAmount > 0) {
                contentStream.beginText()
                contentStream.newLineAtOffset(margin, yPos)
                contentStream.showText("GST (18%):")
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

            // Total Amount
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
            contentStream.showText("Payment Method: ${formatPaymentMethod(order.paymentMethod)}")
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

    private fun formatPaymentMethod(paymentMethod: org.example.project.data.PaymentMethod): String {
        return when (paymentMethod) {
            org.example.project.data.PaymentMethod.CASH -> "Cash"
            org.example.project.data.PaymentMethod.CARD -> "Credit/Debit Card"
            org.example.project.data.PaymentMethod.UPI -> "UPI"
            org.example.project.data.PaymentMethod.NET_BANKING -> "Net Banking"
            org.example.project.data.PaymentMethod.BANK_TRANSFER -> "Bank Transfer"
            org.example.project.data.PaymentMethod.CASH_ON_DELIVERY -> "Cash on Delivery"
            org.example.project.data.PaymentMethod.DUE -> "Due Payment"
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
            formatter.minimumFractionDigits = 2
            formatter.format(amount)
        } catch (e: Exception) {
            String.format("%.2f", amount)
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