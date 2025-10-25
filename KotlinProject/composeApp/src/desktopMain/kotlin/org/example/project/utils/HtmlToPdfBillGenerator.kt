package org.example.project.utils

import org.example.project.data.Order
import org.example.project.data.User
import org.example.project.data.CartItem
import java.io.FileOutputStream
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import org.xhtmlrenderer.pdf.ITextRenderer

class HtmlToPdfBillGenerator {

    fun generatePdfBill(order: Order, customer: User, outputPath: Path): Boolean {
        return try {
            val outputFile = outputPath.toFile()
            outputFile.parentFile?.mkdirs()

            val htmlContent = generatePdfCompatibleHtml(order, customer)
            val renderer = ITextRenderer()
            renderer.setDocumentFromString(htmlContent)
            renderer.layout()

            FileOutputStream(outputFile).use { outputStream ->
                renderer.createPDF(outputStream)
            }

            outputFile.exists() && outputFile.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun generatePdfCompatibleHtml(order: Order, customer: User): String {
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
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
        <html xmlns="http://www.w3.org/1999/xhtml">
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <title>Invoice ${escapeXml(order.orderId)}</title>
            <style type="text/css">
                ${getPdfCompatibleStyles()}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="company">
                        <h1>Vishal Gems and Jewels Pvt. Ltd.</h1>
                        <p class="tagline">Premium Gold and Silver Jewelry</p>
                        <p>Your Store Address Here</p>
                        <p>Phone: +91-XXXXXXXXXX</p>
                        <p>Email: info@vishalgems.com</p>
                    </div>
                    <div class="invoice-meta">
                        <h2>INVOICE</h2>
                        <table class="meta-table">
                            <tr><td><strong>Invoice No:</strong></td><td>${escapeXml(order.orderId)}</td></tr>
                            <tr><td><strong>Date:</strong></td><td>${escapeXml(orderDate)}</td></tr>
                        </table>
                    </div>
                </div>
                
                <div class="customer">
                    <h3>Bill To:</h3>
                    <p><strong>${escapeXml(customer.name)}</strong></p>
                    ${if (customer.phone.isNotEmpty()) "<p>Phone: ${escapeXml(customer.phone)}</p>" else ""}
                    ${if (customer.email.isNotEmpty()) "<p>Email: ${escapeXml(customer.email)}</p>" else ""}
                    ${if (customer.address.isNotEmpty()) "<p>Address: ${escapeXml(customer.address)}</p>" else ""}
                </div>
                
                <table class="items">
                    <thead>
                        <tr>
                            <th>Item</th>
                            <th>Material</th>
                            <th>Weight</th>
                            <th>Qty</th>
                            <th>Rate</th>
                            <th>Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${generateDetailedItemRows(order.items, products)}
                    </tbody>
                </table>
                
                <div class="totals">
                    <table class="totals-table">
                        <tr><td>Subtotal:</td><td>Rs. ${String.format("%.2f", subtotal)}</td></tr>
                        ${if (gstAmount > 0) "<tr><td>GST (${gstPercentage}%):</td><td>Rs. ${String.format("%.2f", gstAmount)}</td></tr>" else ""}
                        ${if (discountAmount > 0) "<tr><td>Discount:</td><td class=\"discount\">-Rs. ${String.format("%.2f", discountAmount)}</td></tr>" else ""}
                        <tr class="total"><td><strong>TOTAL:</strong></td><td><strong>Rs. ${String.format("%.2f", totalAmount)}</strong></td></tr>
                    </table>
                </div>
                
                <div class="footer">
                    <p class="thanks"><strong>Thank you for your business!</strong></p>
                    <p>Terms and Conditions apply. Please keep this invoice for your records.</p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun generateItemRows(items: List<CartItem>): String {
        return items.joinToString("") { item ->
            val weight = if (item.selectedWeight > 0) item.selectedWeight else parseWeight(item.product.weight)
            val rate = getMetalRate(item)
            val amount = weight * item.quantity * rate

            """
            <tr>
                <td>${escapeXml(item.product.name)}</td>
                <td>${escapeXml(item.product.materialType)}</td>
                <td>${String.format("%.2f", weight)}g</td>
                <td>${item.quantity}</td>
                <td>Rs. ${String.format("%.2f", rate)}</td>
                <td>Rs. ${String.format("%.2f", amount)}</td>
            </tr>
            """
        }
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun parseWeight(weightStr: String): Double {
        return weightStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    }

    private fun getMetalRate(cartItem: CartItem): Double {
        return when {
            cartItem.product.materialType.contains("gold", ignoreCase = true) -> 5500.0
            cartItem.product.materialType.contains("silver", ignoreCase = true) -> 75.0
            else -> 5500.0
        }
    }

    private fun getPdfCompatibleStyles(): String {
        return """
        * { margin: 0; padding: 0; }
        body { font-family: Arial, sans-serif; font-size: 12pt; color: #333; }
        .container { padding: 20px; }
        .header { border-bottom: 3px solid #B8973D; padding-bottom: 15px; margin-bottom: 20px; }
        .company h1 { color: #B8973D; font-size: 24pt; margin-bottom: 5px; }
        .tagline { color: #666; font-size: 10pt; margin-bottom: 10px; }
        .company p { font-size: 9pt; color: #888; margin-bottom: 3px; }
        .invoice-meta { text-align: right; margin-top: -80px; }
        .invoice-meta h2 { color: #B8973D; font-size: 20pt; margin-bottom: 10px; }
        .meta-table { margin-left: auto; font-size: 10pt; }
        .meta-table td { padding: 2px 5px; }
        .customer { margin-bottom: 20px; padding: 10px; background: #F9F9F9; border-left: 3px solid #B8973D; }
        .customer h3 { color: #B8973D; font-size: 12pt; margin-bottom: 8px; }
        .customer p { font-size: 10pt; margin-bottom: 3px; }
        .items { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
        .items th { background: #B8973D; color: white; padding: 10px; text-align: left; font-size: 10pt; }
        .items td { padding: 8px; border-bottom: 1px solid #DDD; font-size: 10pt; }
        .items tbody tr:nth-child(even) { background: #F9F9F9; }
        .totals { margin-bottom: 20px; }
        .totals-table { float: right; width: 250px; border-collapse: collapse; }
        .totals-table td { padding: 8px; border-bottom: 1px solid #EEE; font-size: 11pt; }
        .totals-table td:last-child { text-align: right; font-weight: bold; }
        .discount { color: #4CAF50; }
        .total { border-top: 2px solid #B8973D; font-size: 13pt; color: #B8973D; }
        .footer { clear: both; text-align: center; margin-top: 30px; padding-top: 20px; border-top: 2px solid #B8973D; }
        .thanks { color: #B8973D; font-size: 12pt; margin-bottom: 8px; }
        .footer p { font-size: 9pt; color: #888; }
        """.trimIndent()
    }
    
    private fun generateSimplifiedItemRows(items: List<org.example.project.data.OrderItem>): String {
        return items.joinToString("") { item ->
            """
            <tr>
                <td>Product ${item.productId}</td>
                <td>Barcode: ${item.barcodeId}</td>
                <td>${item.quantity}</td>
                <td>-</td>
                <td>-</td>
            </tr>
            """.trimIndent()
        }
    }
    
    private fun generateDetailedItemRows(items: List<org.example.project.data.OrderItem>, products: List<org.example.project.data.Product>): String {
        return items.joinToString("") { item ->
            val product = products.find { it.id == item.productId }
            if (product != null) {
                """
                <tr>
                    <td>${product.name}</td>
                    <td>${product.materialType}</td>
                    <td>${item.quantity}</td>
                    <td>₹${String.format("%.2f", parseWeight(product.weight) * item.quantity)}</td>
                    <td>₹${String.format("%.2f", parseWeight(product.weight) * item.quantity * 100.0)}</td>
                </tr>
                """.trimIndent()
            } else {
                generateSimplifiedItemRows(listOf(item))
            }
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