package org.example.project.invoice.pdf.sections

import com.lowagie.text.*
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import org.example.project.invoice.model.BankInfo
import org.example.project.invoice.model.Party
import org.example.project.invoice.pdf.InvoiceSection
import org.example.project.invoice.pdf.PdfContext

/**
 * Renders bank details section (all from store_info collection bank_info document)
 */
class BankSection(private val bankInfo: BankInfo, private val seller: Party, private val buyer: Party) : InvoiceSection {
    
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
        
        // Heading row - "Bank Details" spanning both columns (reduced size)
        val headingCell = PdfPCell(Phrase("Bank Details", getArialFont(7.5f, Font.BOLD)))
        headingCell.border = Rectangle.NO_BORDER // No inner border - outer cell has border
        headingCell.colspan = 2
        headingCell.setPadding(3f) // Reduced padding
        headingCell.backgroundColor = java.awt.Color(240, 240, 240)
        table.addCell(headingCell)
        
        // Bank account details in table format
        if (bankInfo.accountHolder.isNotEmpty()) {
            addBankRow(table, "Account holder", bankInfo.accountHolder)
        }
        if (bankInfo.accountNumber.isNotEmpty()) {
            addBankRow(table, "Account no.", bankInfo.accountNumber)
        }
        if (bankInfo.ifscCode.isNotEmpty()) {
            addBankRow(table, "IFSC code", bankInfo.ifscCode)
        }
        if (bankInfo.branch.isNotEmpty()) {
            addBankRow(table, "BRANCH", bankInfo.branch.uppercase())
        }
        if (bankInfo.accountType.isNotEmpty()) {
            addBankRow(table, "Account Type", bankInfo.accountType.uppercase())
        }
        
        // Company GSTIN from main_store document
        if (seller.gstin.isNotEmpty()) {
            addBankRow(table, "Company's GSTIN No.", seller.gstin)
        }
        
        // Company PAN from bank_info document
        if (bankInfo.pan.isNotEmpty()) {
            addBankRow(table, "Company PAN No.", bankInfo.pan)
        }
        
        ctx.add(table)
    }
    
    private fun addBankRow(table: PdfPTable, label: String, value: String) {
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
