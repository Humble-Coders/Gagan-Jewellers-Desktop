package org.example.project.invoice.pdf.sections

import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import org.example.project.invoice.pdf.InvoiceSection
import org.example.project.invoice.pdf.PdfContext

/**
 * Divider section - creates a tight horizontal line with zero spacing
 */
class DividerSection : InvoiceSection {

    override fun render(ctx: PdfContext) {
        val dividerTable = PdfPTable(1)
        dividerTable.widthPercentage = 100f
        dividerTable.setSpacingBefore(0f)   // 🔥 VERY IMPORTANT
        dividerTable.setSpacingAfter(0f)    // 🔥 VERY IMPORTANT

        val dividerCell = PdfPCell()
        dividerCell.border = Rectangle.BOTTOM
        dividerCell.borderWidthBottom = 0.8f
        dividerCell.minimumHeight = 0.1f    // 🔥 VERY IMPORTANT
        dividerCell.setPadding(0f)

        dividerTable.addCell(dividerCell)
        ctx.add(dividerTable)
    }
}

