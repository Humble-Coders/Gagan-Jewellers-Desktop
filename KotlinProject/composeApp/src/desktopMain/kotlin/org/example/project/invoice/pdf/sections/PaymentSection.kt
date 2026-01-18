package org.example.project.invoice.pdf.sections

import com.lowagie.text.*
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import org.example.project.invoice.model.PaymentSplit
import org.example.project.invoice.pdf.InvoiceSection
import org.example.project.invoice.pdf.PdfContext
import org.example.project.utils.CurrencyFormatter

/**
 * Renders payment details table
 */
class PaymentSection(private val paymentSplit: PaymentSplit) : InvoiceSection {
    
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
        table.setWidths(floatArrayOf(50f, 50f))
        table.setSpacingAfter(0f)
        table.setSpacingBefore(0f)


        // Heading row - "Payment Details" spanning both columns (reduced size)
        val headingCell = PdfPCell(Phrase("Payment Details", getArialFont(7.5f, Font.BOLD))) // Reduced font size
        headingCell.border = Rectangle.NO_BORDER // No inner border - outer cell has border
        headingCell.colspan = 2
        headingCell.setPaddingTop(2f)
        headingCell.setPaddingBottom(2f)
        headingCell.setPaddingLeft(3f)
        headingCell.setPaddingRight(3f)
        // Reduced padding
        headingCell.backgroundColor = java.awt.Color(240, 240, 240)
        table.addCell(headingCell)
        
        // Payment details in table format (matching Bank Details layout)
        if (paymentSplit.cash > 0) {
            addPaymentRow(table, "Cash", CurrencyFormatter.formatRupeesNumber(paymentSplit.cash, includeDecimals = true))
        }
        
        if (paymentSplit.bank > 0) {
            addPaymentRow(table, "BANK/CARD/ONLINE", CurrencyFormatter.formatRupeesNumber(paymentSplit.bank, includeDecimals = true))
        }
        
        // Calculate total paid amount
        val totalPaid = paymentSplit.cash + paymentSplit.bank
        if (totalPaid > 0) {
            addPaymentRow(table, "Total Paid Amount", CurrencyFormatter.formatRupeesNumber(totalPaid, includeDecimals = true))
        }
        
        // Due amount (always show)
        addPaymentRow(table, "Due Amount", CurrencyFormatter.formatRupeesNumber(paymentSplit.dueAmount, includeDecimals = true))
        
        ctx.add(table)
    }
    
    private fun addPaymentRow(table: PdfPTable, label: String, value: String) {
        val labelCell = PdfPCell(Phrase(label, getArialFont(7f))) // Reduced font size
        labelCell.border = Rectangle.NO_BORDER // No inner border - outer cell has border
        labelCell.setPadding(2.5f) // Reduced padding
        table.addCell(labelCell)
        
        val valueCell = PdfPCell(Phrase(value, getArialFont(7f))) // Reduced font size
        valueCell.border = Rectangle.NO_BORDER // No inner border - outer cell has border
        valueCell.setPadding(2.5f) // Reduced padding
        table.addCell(valueCell)
    }
}
