package org.example.project.invoice.pdf.sections

import com.lowagie.text.*
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import org.example.project.invoice.model.Invoice
import org.example.project.invoice.pdf.InvoiceSection
import org.example.project.invoice.pdf.PdfContext

class TotalsSection(private val invoice: Invoice) : InvoiceSection {

    private val arialBaseFont = BaseFont.createFont(
        BaseFont.HELVETICA,
        BaseFont.CP1252,
        BaseFont.NOT_EMBEDDED
    )

    private fun font(size: Float, style: Int = Font.NORMAL) =
        Font(arialBaseFont, size, style)

    override fun render(ctx: PdfContext) {

        /* ---------------- In Words ---------------- */

        val wordsTable = PdfPTable(1).apply {
            widthPercentage = 100f
            setSpacingBefore(0f)
            setSpacingAfter(6f)
            isSplitLate = false
            isSplitRows = false
            keepTogether = true
        }

        val wordsCell = PdfPCell().apply {
            border = Rectangle.BOX
            borderWidthTop = 0f
            setPadding(6f)
            setPaddingTop(4f)
        }

        wordsCell.addElement(
            Paragraph().apply {
                add(Phrase("In Words: ", font(8f, Font.BOLD)))
                add(Phrase(convertAmountToWords(invoice.netAmount), font(8f, Font.BOLD)))
                spacingAfter = 0f
            }
        )

        wordsTable.addCell(wordsCell)
        ctx.add(wordsTable)

        /* ---------------- Terms & Conditions ---------------- */

        val termsTable = PdfPTable(2).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(70f, 30f))
            setSpacingAfter(6f)
            isSplitLate = false
            isSplitRows = false
            keepTogether = true
        }

        // LEFT CELL
        val leftCell = PdfPCell().apply {
            border = Rectangle.NO_BORDER
            setPadding(0f)
        }

        val leftPara = Paragraph().apply {
            leading = 9f
            add(Phrase("TERMS & CONDITIONS:", font(8f, Font.BOLD)))
            add(Chunk.NEWLINE)
        }

        val terms = listOf(
            "No encashment or exchange without presentation of this Invoice.",
            "Any exchange of sold ornaments can be made within 48 hours, only if it is not tampered, repaired or used.",
            "The company is not liable for colours, damage, loss and breakage of ornaments.",
            "In case of payment by cheque, delivery of goods will be made only after realization of cheque.",
            "Terms & conditions are subject to change without prior notice.",
            "Including Hallmarking Charge",
            "Disputes if any, are subject to SAHARSA jurisdiction"
        )

        terms.forEachIndexed { i, text ->
            leftPara.add(Phrase("${i + 1}. $text", font(7.5f)))
            if (i != terms.lastIndex) leftPara.add(Chunk.NEWLINE)
        }

        leftCell.addElement(leftPara)
        termsTable.addCell(leftCell)

        // RIGHT CELL
        val rightCell = PdfPCell().apply {
            border = Rectangle.NO_BORDER
            horizontalAlignment = Element.ALIGN_RIGHT
            setPadding(0f)
            setPaddingLeft(10f)
        }

        val rightPara = Paragraph().apply {
            // Top
            add(Phrase("E. & O.E.", font(8f)))
            add(Chunk.NEWLINE)
            add(Chunk.NEWLINE)
            add(Chunk.NEWLINE)
            add(Chunk.NEWLINE)
            add(Chunk.NEWLINE) // spacer
            // Bottom
            add(Phrase("For ${invoice.seller.name.uppercase()}", font(8.5f, Font.BOLD)))
        }

        rightCell.addElement(rightPara)
        termsTable.addCell(rightCell)

        ctx.add(termsTable)

        /* ---------------- Signatures ---------------- */

        val footerTable = PdfPTable(2).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(50f, 50f))
            setSpacingBefore(10f)
            isSplitLate = false
            isSplitRows = false
            keepTogether = true
        }

        fun signatureCell(label: String): PdfPCell =
            PdfPCell().apply {
                border = Rectangle.NO_BORDER
                horizontalAlignment = Element.ALIGN_CENTER
                addElement(
                    Paragraph().apply {
                        add(Chunk("_________________", font(8f)))
                        add(Chunk.NEWLINE)
                        add(Chunk.NEWLINE)
                        add(Phrase(label, font(8f)))
                    }
                )
            }

        footerTable.addCell(signatureCell("Customer's Signature"))
        footerTable.addCell(signatureCell("Authorised Signatory"))

        ctx.add(footerTable)
    }

    /* -------- Amount to Words -------- */

    private fun convertAmountToWords(amount: Double): String {
        val intPart = amount.toInt()
        val paise = ((amount - intPart) * 100).toInt()

        fun words(n: Int): String {
            val o = arrayOf("", "one","two","three","four","five","six","seven","eight","nine","ten",
                "eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen")
            val t = arrayOf("","","twenty","thirty","forty","fifty","sixty","seventy","eighty","ninety")

            return when {
                n < 20 -> o[n]
                n < 100 -> t[n / 10] + if (n % 10 != 0) " ${o[n % 10]}" else ""
                n < 1000 -> "${o[n / 100]} hundred" + if (n % 100 != 0) " ${words(n % 100)}" else ""
                n < 100000 -> "${words(n / 1000)} thousand" + if (n % 1000 != 0) " ${words(n % 1000)}" else ""
                n < 10000000 -> "${words(n / 100000)} lakh" + if (n % 100000 != 0) " ${words(n % 100000)}" else ""
                else -> "${words(n / 10000000)} crore" + if (n % 10000000 != 0) " ${words(n % 10000000)}" else ""
            }
        }

        val main = words(intPart).replaceFirstChar { it.uppercaseChar() }
        val suffix = if (paise > 0) " and $paise/100" else ""
        return "Rupees $main$suffix Only"
    }
}
