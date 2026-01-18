package org.example.project.invoice.pdf

import com.lowagie.text.Element
import com.lowagie.text.Document
import com.lowagie.text.pdf.PdfWriter

/**
 * Holds document state and manages page lifecycle
 */
open class PdfContext(
    val document: Document,
    val writer: PdfWriter
) {
    open fun add(element: Element) {
        document.add(element)
    }
}

