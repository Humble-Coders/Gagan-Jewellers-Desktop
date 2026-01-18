package org.example.project.invoice.pdf.sections

import com.lowagie.text.*
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import org.example.project.invoice.model.Invoice
import org.example.project.invoice.pdf.InvoiceSection
import org.example.project.invoice.pdf.PdfContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Renders customer and invoice details in a two-column layout
 */
class PartySection(private val invoice: Invoice) : InvoiceSection {
    
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
        val partyTable = PdfPTable(2)
        partyTable.widthPercentage = 100f
        partyTable.setWidths(floatArrayOf(50f, 50f))
        partyTable.setSpacingAfter(10f)

        // Left: Customer Details
        val customerCell = PdfPCell()
        customerCell.border = Rectangle.BOX
        customerCell.setPadding(5f)

        val customerTable = PdfPTable(2)
        customerTable.widthPercentage = 100f
        customerTable.setWidths(floatArrayOf(30f, 70f))

        addTableRow(customerTable, "Name", ": ${invoice.buyer.name.uppercase()}")
        addTableRow(customerTable, "Address", ": ${invoice.buyer.address.uppercase()}")
        
        // Use editable city from invoice, or extract from address
        val city = invoice.city.ifEmpty { extractCityFromAddress(invoice.buyer.address) }
        addTableRow(customerTable, "City", ": ${city.uppercase()}")
        addTableRow(customerTable, "Ph No.", ": ${invoice.buyer.phone.uppercase()}")

        customerCell.addElement(customerTable)
        partyTable.addCell(customerCell)

        // Right: Invoice Details
        val invoiceCell = PdfPCell()
        invoiceCell.border = Rectangle.BOX
        invoiceCell.setPadding(5f)

        val invoiceTable = PdfPTable(2)
        invoiceTable.widthPercentage = 100f
        invoiceTable.setWidths(floatArrayOf(35f, 65f))

        // Use editable memoNo from invoice, or default
        val memoNo = invoice.memoNo.ifEmpty { "GST/${invoice.invoiceNo.takeLast(8)}" }
        val invoiceDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
            java.util.Date(java.time.ZonedDateTime.of(invoice.issueDate, java.time.LocalTime.MIDNIGHT, java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        )

        addTableRow(invoiceTable, "Order No.", ": ${invoice.invoiceNo}")
        addTableRow(invoiceTable, "Memo No.", ": $memoNo")
        addTableRow(invoiceTable, "Date", ": $invoiceDate")

        // State Name and State Code in one row (use buyer's state)
        val stateLabelCell = PdfPCell(Phrase("State Name & Code", getArialFont(9f, Font.BOLD)))
        stateLabelCell.border = Rectangle.NO_BORDER
        stateLabelCell.setPadding(3f)
        invoiceTable.addCell(stateLabelCell)

        val stateValue = if (invoice.buyer.stateName.isNotEmpty() && invoice.buyer.stateCode.isNotEmpty()) {
            ": ${invoice.buyer.stateName.uppercase()} (${invoice.buyer.stateCode})"
        } else if (invoice.buyer.stateName.isNotEmpty()) {
            ": ${invoice.buyer.stateName.uppercase()}"
        } else if (invoice.buyer.stateCode.isNotEmpty()) {
            ": (${invoice.buyer.stateCode})"
        } else {
            ":"
        }
        val stateValueCell = PdfPCell(Phrase(stateValue, getArialFont(9f)))
        stateValueCell.border = Rectangle.NO_BORDER
        stateValueCell.setPadding(3f)
        invoiceTable.addCell(stateValueCell)

        // Use editable placeOfDelivery from invoice, or default to buyer state
        val placeOfDelivery = invoice.placeOfDelivery.ifEmpty { 
            invoice.buyer.stateName.ifEmpty { invoice.seller.stateName }
        }.uppercase()
        val placeCell1 = PdfPCell(Phrase("Place of Delivery", getArialFont(9f, Font.BOLD)))
        placeCell1.border = Rectangle.NO_BORDER
        placeCell1.setPadding(3f)
        invoiceTable.addCell(placeCell1)

        val placeCell2 = PdfPCell(Phrase(": $placeOfDelivery", getArialFont(9f)))
        placeCell2.border = Rectangle.NO_BORDER
        placeCell2.colspan = 1
        placeCell2.setPadding(3f)
        invoiceTable.addCell(placeCell2)

        invoiceCell.addElement(invoiceTable)
        partyTable.addCell(invoiceCell)

        ctx.add(partyTable)
    }

    private fun addTableRow(table: PdfPTable, label: String, value: String) {
        val labelCell = PdfPCell(Phrase(label, getArialFont(9f, Font.BOLD)))
        labelCell.border = Rectangle.NO_BORDER
        labelCell.setPadding(3f)
        table.addCell(labelCell)

        val valueCell = PdfPCell(Phrase(value, getArialFont(9f)))
        valueCell.border = Rectangle.NO_BORDER
        valueCell.setPadding(3f)
        table.addCell(valueCell)
    }
    
    /**
     * Extract city from address (usually before state or after comma)
     */
    private fun extractCityFromAddress(address: String): String {
        if (address.isEmpty()) return ""
        
        // Try to extract city - usually the first part before comma
        val parts = address.split(",")
        if (parts.size > 1) {
            return parts[0].trim()
        }
        // If no comma, try to get first word (assuming it's city)
        val words = address.split(" ").filter { it.isNotEmpty() }
        if (words.isNotEmpty()) {
            return words[0]
        }
        return ""
    }
}
