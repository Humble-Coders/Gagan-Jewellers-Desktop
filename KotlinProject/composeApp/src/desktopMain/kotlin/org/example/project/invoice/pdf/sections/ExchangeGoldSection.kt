package org.example.project.invoice.pdf.sections

import com.lowagie.text.*
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import org.example.project.invoice.model.ExchangeGoldInfo
import org.example.project.invoice.pdf.InvoiceSection
import org.example.project.invoice.pdf.PdfContext
import org.example.project.utils.CurrencyFormatter

/**
 * Renders exchange gold details section
 */
class ExchangeGoldSection(private val exchangeGold: ExchangeGoldInfo) : InvoiceSection {
    
    override fun render(ctx: PdfContext) {
        val table = PdfPTable(2)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(70f, 30f))
        table.setSpacingAfter(5f)
        
        // Details row
        val detailsCell = PdfPCell()
        detailsCell.border = Rectangle.NO_BORDER
        detailsCell.setPadding(5f)
        
        val detailsPara = Paragraph()
        if (exchangeGold.productName.isNotEmpty()) {
            detailsPara.add(Phrase("Product: ${exchangeGold.productName}", Font(Font.NORMAL, 8f)))
            detailsPara.add(Chunk.NEWLINE)
        }
        if (exchangeGold.goldWeight > 0) {
            // Display weight with total product weight and percentage on the same line
            val weightText = if (exchangeGold.totalProductWeight > 0 && exchangeGold.percentage > 0) {
                "Weight: ${String.format("%.2f", exchangeGold.goldWeight)}g (Total: ${String.format("%.2f", exchangeGold.totalProductWeight)}g, ${String.format("%.2f", exchangeGold.percentage)}%)"
            } else {
                "Weight: ${String.format("%.2f", exchangeGold.goldWeight)}g"
            }
            detailsPara.add(Phrase(weightText, Font(Font.NORMAL, 8f)))
            detailsPara.add(Chunk.NEWLINE)
        }
        if (exchangeGold.goldPurity.isNotEmpty()) {
            detailsPara.add(Phrase("Purity: ${exchangeGold.goldPurity}", Font(Font.NORMAL, 8f)))
            detailsPara.add(Chunk.NEWLINE)
        }
        if (exchangeGold.goldRate > 0) {
            detailsPara.add(Phrase("Rate: ₹${String.format("%.2f", exchangeGold.goldRate)}/g", Font(Font.NORMAL, 8f)))
        }
        
        detailsCell.addElement(detailsPara)
        table.addCell(detailsCell)
        
        // Empty cell for alignment
        val emptyCell = PdfPCell()
        emptyCell.border = Rectangle.NO_BORDER
        table.addCell(emptyCell)
        
        ctx.add(table)
    }
}

