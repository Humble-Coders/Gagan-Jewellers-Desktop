package org.example.project.invoice.pdf.sections

import com.lowagie.text.*
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import org.example.project.invoice.model.Invoice
import org.example.project.invoice.pdf.InvoiceSection
import org.example.project.invoice.pdf.PdfContext
import org.example.project.utils.CurrencyFormatter

/**
 * Renders tax summary section with exchange gold details at top, then tax breakdown
 */
class TaxSummarySection(private val invoice: Invoice) : InvoiceSection {
    
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
        val table = PdfPTable(2)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(70f, 30f))
        table.setSpacingAfter(0f) // Reduced spacing
        
        // Amount - should be before Exchange Gold
        addSummaryRow(table, "Amount", invoice.subtotal, isBoldLabel = true)
        
        // Exchange Gold details (if present) - reduced size
        if (invoice.exchangeGold != null && invoice.exchangeGoldValue > 0) {
            val exchangeLabelCell = PdfPCell(Phrase("Less: Exchange Gold", getArialFont(7f, Font.BOLD))) // Reduced font
            exchangeLabelCell.border = Rectangle.NO_BORDER // No inner border - outer cell has border
            exchangeLabelCell.borderWidthTop = 0f
            exchangeLabelCell.borderWidthBottom = 0f
            exchangeLabelCell.borderWidthLeft = 0f
            exchangeLabelCell.borderWidthRight = 0f
            exchangeLabelCell.setPadding(2f) // Reduced padding
            table.addCell(exchangeLabelCell)
            
            val exchangeValueCell = PdfPCell(Phrase(CurrencyFormatter.formatRupeesNumber(invoice.exchangeGoldValue, includeDecimals = true), getArialFont(7f))) // Reduced font
            exchangeValueCell.border = Rectangle.NO_BORDER // No inner border - outer cell has border
            exchangeValueCell.borderWidthTop = 0f
            exchangeValueCell.borderWidthBottom = 0f
            exchangeValueCell.borderWidthLeft = 0f
            exchangeValueCell.borderWidthRight = 0f
            exchangeValueCell.horizontalAlignment = Element.ALIGN_RIGHT
            exchangeValueCell.setPadding(2f) // Reduced padding
            exchangeValueCell.setNoWrap(true)
            table.addCell(exchangeValueCell)
            
            // Exchange Gold details row
            val exchangeDetailsCell = PdfPCell()
            exchangeDetailsCell.border = Rectangle.NO_BORDER // No inner border - outer cell has border
            exchangeDetailsCell.borderWidthTop = 0f
            exchangeDetailsCell.borderWidthBottom = 0f
            exchangeDetailsCell.borderWidthLeft = 0f
            exchangeDetailsCell.borderWidthRight = 0f
            exchangeDetailsCell.colspan = 2
            exchangeDetailsCell.setPadding(2f) // Reduced padding
            exchangeDetailsCell.setPaddingLeft(8f) // Reduced left padding

            val exchangeDetailsPara = Paragraph()
            exchangeDetailsPara.leading = 7f

            var first = true
            fun addLine(text: String) {
                if (!first) exchangeDetailsPara.add(Chunk.NEWLINE)
                exchangeDetailsPara.add(Phrase(text, getArialFont(6f)))
                first = false
            }

            if (invoice.exchangeGold.goldWeight > 0) {
                addLine("Weight: ${String.format("%.2f", invoice.exchangeGold.goldWeight)}g")
            }
            if (invoice.exchangeGold.goldPurity.isNotEmpty()) {
                addLine("Purity: ${invoice.exchangeGold.goldPurity}")
            }
            if (invoice.exchangeGold.goldRate > 0) {
                addLine("Rate: ₹${String.format("%.2f", invoice.exchangeGold.goldRate)}/g")
            }
            
            exchangeDetailsCell.addElement(exchangeDetailsPara)
            table.addCell(exchangeDetailsCell)
        }
        
        // Discount - always show (even if 0) matching image format
        addSummaryRow(table, "Less: Discount", invoice.discountAmount, isBoldLabel = true)
        
        // Taxable Amount - matching image format
        addSummaryRow(table, "Taxable Amount", invoice.taxableAmount, isBoldLabel = true)
        
        // SGST and CGST as separate rows (matching image format)
        val sgstPercentage = invoice.gstPercentage / 2.0
        val cgstPercentage = invoice.gstPercentage / 2.0
        val sgstAmount = invoice.gstAmount / 2.0
        val cgstAmount = invoice.gstAmount / 2.0
        
        addSummaryRow(table, "SGST ${String.format("%.3f", sgstPercentage)} %", sgstAmount, isBoldLabel = true)
        addSummaryRow(table, "CGST ${String.format("%.3f", cgstPercentage)} %", cgstAmount, isBoldLabel = true)
        
        // Gross Total - matching image format
        addSummaryRow(table, "Gross Total", invoice.grossTotal, isBoldLabel = true)
        
        // Round Off - matching image format
        addSummaryRow(table, "Round Off", invoice.roundOff, isBoldLabel = true)
        
        // Net Amount (bold) - matching image format (reduced size)
        val netLabelCell = PdfPCell(Phrase("Net Amount", getArialFont(7.5f, Font.BOLD))) // Reduced font
        netLabelCell.border = Rectangle.NO_BORDER // No inner border - outer cell has border
        netLabelCell.borderWidthTop = 0f
        netLabelCell.borderWidthBottom = 0f
        netLabelCell.borderWidthLeft = 0f
        netLabelCell.borderWidthRight = 0f
        netLabelCell.setPadding(2.5f) // Reduced padding
        netLabelCell.backgroundColor = java.awt.Color(240, 240, 240)
        table.addCell(netLabelCell)
        
        val netValueCell = PdfPCell(Phrase(CurrencyFormatter.formatRupeesNumber(invoice.netAmount, includeDecimals = true), getArialFont(7.5f, Font.BOLD))) // Reduced font
        netValueCell.border = Rectangle.NO_BORDER // No inner border - outer cell has border
        netValueCell.borderWidthTop = 0f
        netValueCell.borderWidthBottom = 0f
        netValueCell.borderWidthLeft = 0f
        netValueCell.borderWidthRight = 0f
        netValueCell.horizontalAlignment = Element.ALIGN_RIGHT
        netValueCell.setPadding(2.5f) // Reduced padding
        netValueCell.backgroundColor = java.awt.Color(240, 240, 240)
        netValueCell.setNoWrap(true)
        table.addCell(netValueCell)
        
        ctx.add(table)
    }
    
    private fun addSummaryRow(table: PdfPTable, label: String, value: Double, isBoldLabel: Boolean = false) {
        val labelCell = PdfPCell(Phrase(label, getArialFont(7f, if (isBoldLabel) Font.BOLD else Font.NORMAL))) // Reduced font size
        labelCell.border = Rectangle.NO_BORDER // No inner border - outer cell has border
        labelCell.borderWidthTop = 0f
        labelCell.borderWidthBottom = 0f
        labelCell.borderWidthLeft = 0f
        labelCell.borderWidthRight = 0f
        labelCell.setPadding(2f) // Reduced padding
        table.addCell(labelCell)
        
        val valueCell = PdfPCell(Phrase(CurrencyFormatter.formatRupeesNumber(value, includeDecimals = true), getArialFont(7f))) // Reduced font size
        valueCell.border = Rectangle.NO_BORDER // No inner border - outer cell has border
        valueCell.borderWidthTop = 0f
        valueCell.borderWidthBottom = 0f
        valueCell.borderWidthLeft = 0f
        valueCell.borderWidthRight = 0f
        valueCell.horizontalAlignment = Element.ALIGN_RIGHT
        valueCell.setPadding(2f) // Reduced padding
        valueCell.setNoWrap(true)
        table.addCell(valueCell)
    }
}
