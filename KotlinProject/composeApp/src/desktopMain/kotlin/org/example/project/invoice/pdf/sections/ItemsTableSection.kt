package org.example.project.invoice.pdf.sections

import com.lowagie.text.*
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import org.example.project.invoice.model.InvoiceItem
import org.example.project.invoice.pdf.InvoiceSection
import org.example.project.invoice.pdf.PdfContext
import org.example.project.utils.CurrencyFormatter

/**
 * Renders the items table with product details
 * All calculations match ReceiptScreen logic
 */
class ItemsTableSection(private val items: List<InvoiceItem>) : InvoiceSection {
    
    // Arial font for all text
    private val arialBaseFont = try {
        BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED)
    } catch (e: Exception) {
        BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED)
    }
    
    private fun getArialFont(size: Float, style: Int = Font.NORMAL): Font {
        return Font(arialBaseFont, size, style)
    }
    
    override fun render(ctx: PdfContext) {
        val table = PdfPTable(13)
        table.widthPercentage = 100f
        // Adjusted column widths: removed Gross Weight, added Rate (per gm)
        table.setWidths(floatArrayOf(12f, 15f, 12f, 5f, 8f, 10f, 10f, 8f, 8f, 12f, 10f, 10f, 12f))
        table.setSpacingAfter(10f)
        
        // Header row
        val headers = listOf(
            "Variant no", "Product name", "Material with Purity",
            "Net Qty", "Gross Weight (grams)", "Net Stone Weight (grams)",
            "Net Metal Weight (grams)", "Polish", "HSN", "Rate (per gm)",
            "Labour Charges (Rs.)", "Stone Amount (Rs.)", "COST Value (Rs.)"
        )
        
        headers.forEach { header ->
            val cell = PdfPCell(Phrase(header, getArialFont(6f, Font.BOLD)))
            cell.border = Rectangle.BOX
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.verticalAlignment = Element.ALIGN_MIDDLE
            cell.setPadding(3f)
            cell.backgroundColor = java.awt.Color(240, 240, 240)
            table.addCell(cell)
        }
        
        // Data rows
        items.forEach { item ->
            addDataCell(table, item.variantNo)
            
            // Product name with diamond/solitaire weights if applicable
            // Always create a cell to allow for multi-line content (for diamond/solitaire weights)
            val productCell = PdfPCell()
            productCell.border = Rectangle.BOX
            productCell.horizontalAlignment = Element.ALIGN_LEFT
            productCell.verticalAlignment = Element.ALIGN_TOP
            productCell.setPadding(3f)
            
            val productPara = Paragraph()
            productPara.setLeading(0f, 1.2f) // Increased line spacing (1.2x font size) for better readability
            
            // Product name in bold on first line - reduce font size if too long to fit in one line
            val productNameText = item.productName.uppercase()
            val baseFontSize = 6f
            val productNameFontSize = getFontSizeForText(productNameText, baseFontSize)
            productPara.add(Phrase(productNameText, getArialFont(productNameFontSize, Font.BOLD)))
            
            // Diamond weight on separate line if present (check with small epsilon to handle floating point)
            if (item.diamondWeightInCarats > 0.001) {
                productPara.add(Chunk.NEWLINE)
                productPara.add(Phrase("Diamond Wt ${String.format("%.3f", item.diamondWeightInCarats)} Ct", getArialFont(6f)))
            }
            
            // Solitaire weight on separate line if present (check with small epsilon to handle floating point)
            if (item.solitaireWeightInCarats > 0.001) {
                productPara.add(Chunk.NEWLINE)
                productPara.add(Phrase("Solitaire Wt ${String.format("%.3f", item.solitaireWeightInCarats)} Ct", getArialFont(6f)))
            }
            
            productCell.addElement(productPara)
            productCell.setNoWrap(false) // Allow wrapping for diamond/solitaire weights, but product name fits in one line
            table.addCell(productCell)
            
            addDataCell(table, item.materialWithPurity)
            addDataCell(table, "${item.quantity}N")
            // Gross Weight column (restored)
            addDataCell(table, String.format("%.1f", item.grossWeight))
            addDataCell(table, if (item.netStoneWeight > 0) String.format("%.1f", item.netStoneWeight) else "-")
            addDataCell(table, String.format("%.1f", item.netMetalWeight))
            addDataCell(table, String.format("%.1f", item.makingPercent) + "%")
            addDataCell(table, item.barcodeId.ifEmpty { "-" }) // HSN column with barcode ID
            // Rate (per gm) column instead of Gross Product Price
            addDataCell(table, CurrencyFormatter.formatRupeesNumber(item.materialRatePerGram, includeDecimals = true))
            addDataCell(table, CurrencyFormatter.formatRupeesNumber(item.labourCharges, includeDecimals = true))
            addDataCell(table, if (item.stoneAmount > 0) CurrencyFormatter.formatRupeesNumber(item.stoneAmount, includeDecimals = true) else "-")
            addDataCell(table, CurrencyFormatter.formatRupeesNumber(item.costValue, includeDecimals = true))
        }
        
        // Total row
        val totalQty = items.sumOf { it.quantity }
        val totalGrossWeight = items.sumOf { it.grossWeight }
        val totalNetStoneWeight = items.sumOf { it.netStoneWeight }
        val totalNetMetalWeight = items.sumOf { it.netMetalWeight }
        val totalMakingCharges = items.sumOf { it.labourCharges }
        val totalStoneAmount = items.sumOf { it.stoneAmount }
        val totalCostValue = items.sumOf { it.costValue }
        
        // Total row - first 3 columns merged
        val totalCell1 = PdfPCell(Phrase("Total", getArialFont(7f, Font.BOLD)))
        totalCell1.border = Rectangle.BOX
        totalCell1.colspan = 3
        totalCell1.horizontalAlignment = Element.ALIGN_CENTER
        totalCell1.setPadding(3f)
        table.addCell(totalCell1)
        
        addTotalCell(table, "${totalQty}N")
        addTotalCell(table, String.format("%.1f", totalGrossWeight)) // Gross Weight total
        addTotalCell(table, if (totalNetStoneWeight > 0) String.format("%.1f", totalNetStoneWeight) else "-")
        addTotalCell(table, String.format("%.1f", totalNetMetalWeight))
        addTotalCell(table, "-") // Polish column total
        addTotalCell(table, "-") // HSN column total
        addTotalCell(table, "-") // Rate column total (no total for rate)
        addTotalCell(table, CurrencyFormatter.formatRupeesNumber(totalMakingCharges, includeDecimals = true))
        addTotalCell(table, if (totalStoneAmount > 0) CurrencyFormatter.formatRupeesNumber(totalStoneAmount, includeDecimals = true) else "-")
        addTotalCell(table, CurrencyFormatter.formatRupeesNumber(totalCostValue, includeDecimals = true))
        
        ctx.add(table)
    }
    
    /**
     * Calculate appropriate font size based on text length to prevent overflow
     * Returns smaller font size for longer text to ensure it fits within the cell
     */
    private fun getFontSizeForText(text: String, baseSize: Float): Float {
        val length = text.length
        return when {
            length <= 6 -> baseSize
            length <= 10 -> baseSize * 0.9f
            length <= 14 -> baseSize * 0.8f
            length <= 18 -> baseSize * 0.7f
            length <= 22 -> baseSize * 0.6f
            length <= 26 -> baseSize * 0.55f
            else -> baseSize * 0.5f.coerceAtLeast(3f) // Minimum font size of 3
        }
    }
    
    private fun addDataCell(table: PdfPTable, text: String) {
        // Calculate font size based on text length to prevent overflow
        val baseFontSize = 6f
        val fontSize = getFontSizeForText(text, baseFontSize)
        
        val cell = PdfPCell(Phrase(text, getArialFont(fontSize)))
        cell.border = Rectangle.BOX
        cell.horizontalAlignment = Element.ALIGN_CENTER
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        cell.setPadding(3f)
        // Prevent text from overflowing - reduce font size if needed, but keep single line
        cell.setNoWrap(true)
        table.addCell(cell)
    }
    
    private fun addTotalCell(table: PdfPTable, text: String) {
        // Calculate font size based on text length to prevent overflow
        val baseFontSize = 7f
        val fontSize = getFontSizeForText(text, baseFontSize)
        
        val cell = PdfPCell(Phrase(text, getArialFont(fontSize, Font.BOLD)))
        cell.border = Rectangle.BOX
        cell.horizontalAlignment = Element.ALIGN_CENTER
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        cell.setPadding(3f)
        // Prevent text from overflowing - reduce font size if needed, but keep single line
        cell.setNoWrap(true)
        table.addCell(cell)
    }
}
