package org.example.project.invoice.pdf

import com.lowagie.text.*
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import org.example.project.invoice.model.Invoice
import org.example.project.invoice.pdf.sections.*
import java.io.File
import java.io.FileOutputStream

/**
 * Main invoice renderer using OpenPDF
 * Deterministic, section-based rendering
 */
class InvoiceRenderer {
    
    fun render(invoice: Invoice, outputFile: File) {
        // A4 with 0.5 cm margins (0.5 cm = 14.17 points, approximately 14f)
        val document = Document(PageSize.A4, 14f, 14f, 14f, 14f)
        val writer = PdfWriter.getInstance(document, FileOutputStream(outputFile))
        
        document.open()
        val ctx = PdfContext(document, writer)
        
        try {
            // Render sections in order
            HeaderSection(invoice).render(ctx)
            PartySection(invoice).render(ctx)
            ItemsTableSection(invoice.items).render(ctx)
            
            // Two column layout for bank/payment (left) and summary with exchange gold (right)
            // Right column (tax summary) should be smaller to give more space to items table
            val twoColumnTable = PdfPTable(2)
            twoColumnTable.widthPercentage = 100f
            twoColumnTable.setWidths(floatArrayOf(70f, 30f)) // Increased left column, reduced right column
            twoColumnTable.setSpacingAfter(3f) // Reduced spacing before TotalsSection
            
            // Left column: Bank and Payment (reduced size)
            val leftCell = PdfPCell()
            leftCell.border = Rectangle.BOX
            leftCell.setPadding(4f) // Reduced padding for left column
            
            // Render bank section and payment section directly into left cell
            val leftElements = mutableListOf<Element>()
            val bankSection = BankSection(invoice.bankInfo, invoice.seller, invoice.buyer)
            val bankCtx = object : PdfContext(ctx.document, ctx.writer) {
                override fun add(element: Element) {
                    leftElements.add(element)
                }
            }
            bankSection.render(bankCtx)
            
            // Divider between Bank Details and Payment Details (using DividerSection)
            val dividerSection = DividerSection()
            dividerSection.render(bankCtx)
            
            if (invoice.paymentSplit != null) {
                val paymentSection = PaymentSection(invoice.paymentSplit)
                paymentSection.render(bankCtx)
            }
            
            leftElements.forEach { leftCell.addElement(it) }
            // Cells in same row will automatically have same height
            twoColumnTable.addCell(leftCell)
            
            // Right column: Tax Summary (includes exchange gold at top) - reduced size
            val rightCell = PdfPCell()
            rightCell.border = Rectangle.BOX // Add border like left cell
            rightCell.setPadding(4f) // Reduced padding to save space
            
            val rightElements = mutableListOf<Element>()
            val taxSection = TaxSummarySection(invoice)
            val taxCtx = object : PdfContext(ctx.document, ctx.writer) {
                override fun add(element: Element) {
                    rightElements.add(element)
                }
            }
            taxSection.render(taxCtx)
            
            rightElements.forEach { rightCell.addElement(it) }
            // Cells in same row will automatically have same height
            twoColumnTable.addCell(rightCell)
            
            ctx.add(twoColumnTable)
            
            // Amount in words and Terms & Conditions
            TotalsSection(invoice).render(ctx)
            
        } finally {
            document.close()
        }
    }

}


