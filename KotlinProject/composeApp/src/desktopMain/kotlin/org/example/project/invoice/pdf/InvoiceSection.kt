package org.example.project.invoice.pdf

/**
 * Contract for invoice sections
 * Each section renders independently
 */
interface InvoiceSection {
    fun render(ctx: PdfContext)
}

