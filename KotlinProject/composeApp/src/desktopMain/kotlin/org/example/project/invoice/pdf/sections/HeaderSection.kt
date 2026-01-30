package org.example.project.invoice.pdf.sections

import com.lowagie.text.*
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import org.example.project.JewelryAppInitializer
import org.example.project.data.StoreInfoRepository
import org.example.project.invoice.model.Invoice
import org.example.project.invoice.pdf.InvoiceSection
import org.example.project.invoice.pdf.PdfContext
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * Header section with company logo, company info, and BIS logo
 * ERP-grade deterministic layout
 */
class HeaderSection(private val invoice: Invoice) : InvoiceSection {

    private val arialBaseFont = BaseFont.createFont(
        BaseFont.HELVETICA,
        BaseFont.CP1252,
        BaseFont.NOT_EMBEDDED
    )

    private fun font(size: Float, style: Int = Font.NORMAL): Font {
        return Font(arialBaseFont, size, style)
    }

    private fun loadImage(filename: String): Image? {
        return try {
            val resourceStream =
                javaClass.getResourceAsStream("/drawable/$filename")
                    ?: javaClass.classLoader.getResourceAsStream("drawable/$filename")

            if (resourceStream != null) {
                val bytes = resourceStream.readBytes()
                resourceStream.close()
                return Image.getInstance(bytes)
            }

            val root = findProjectRoot() ?: return null
            val file = File("$root/composeApp/src/commonMain/composeResources/drawable/$filename")

            if (file.exists()) Image.getInstance(file.readBytes()) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun loadGaganLogoFromFirebase(): Image? {
        return try {
            // Fetch logo URLs from Firestore store_info/main_store/logo_images
            val storeInfo = runBlocking {
                StoreInfoRepository.getStoreInfo()
            }
            
            val logoUrls = storeInfo.mainStore.logo_images
            
            if (logoUrls.isEmpty()) {
                println("⚠️ No logo images found in store_info/main_store/logo_images")
                return null
            }
            
            // Try to load the first logo image
            val storage = JewelryAppInitializer.getStorageService()
            for (logoUrl in logoUrls) {
                try {
                    println("📸 Attempting to load logo from: $logoUrl")
                    
                    // Handle different URL formats
                    val bytes = when {
                        logoUrl.startsWith("https://") -> {
                            // Download from HTTPS URL
                            java.net.URL(logoUrl).readBytes()
                        }
                        logoUrl.startsWith("gs://") -> {
                            // Extract path from gs:// URL and download from Storage
                            val path = logoUrl.removePrefix("gs://").substringAfter("/")
                            storage.downloadFileBytes(path)
                        }
                        else -> {
                            // Assume it's a direct Storage path
                            storage.downloadFileBytes(logoUrl)
                        }
                    }
                    
                    if (bytes != null && bytes.isNotEmpty()) {
                        println("✅ Successfully loaded logo (${bytes.size} bytes)")
                        return Image.getInstance(bytes)
                    }
                } catch (e: Exception) {
                    println("⚠️ Failed to load logo from $logoUrl: ${e.message}")
                }
            }
            
            println("⚠️ Could not load any logo from the ${logoUrls.size} URL(s) in logo_images")
            null
        } catch (e: Exception) {
            println("❌ Error loading logo from Firestore: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun findProjectRoot(): String? {
        var dir = File(System.getProperty("user.dir"))
        repeat(8) {
            if (
                File(dir, "settings.gradle.kts").exists() ||
                File(dir, ".git").exists()
            ) return dir.absolutePath
            dir = dir.parentFile ?: return null
        }
        return null
    }

    override fun render(ctx: PdfContext) {

        val headerTable = PdfPTable(3)
        headerTable.widthPercentage = 100f
        headerTable.setWidths(floatArrayOf(20f, 60f, 20f))
        headerTable.setSpacingAfter(12f)

        /* ---------------- LEFT : COMPANY LOGO ---------------- */

        val logoCell = PdfPCell()
        logoCell.border = Rectangle.NO_BORDER
        logoCell.fixedHeight = 100f
        logoCell.horizontalAlignment = Element.ALIGN_LEFT
        logoCell.verticalAlignment = Element.ALIGN_TOP
        logoCell.setPaddingLeft(0f)
        logoCell.setPaddingTop(6f)

        loadGaganLogoFromFirebase()?.let {
            it.scaleToFit(90f, 90f)
            it.alignment = Image.ALIGN_LEFT
            logoCell.addElement(it)
        } ?: run {
            logoCell.addElement(Phrase("COMPANY LOGO", font(9f, Font.BOLD)))
        }

        headerTable.addCell(logoCell)

        /* ---------------- CENTER : COMPANY INFO ---------------- */

        val companyCell = PdfPCell()
        companyCell.border = Rectangle.NO_BORDER
        companyCell.horizontalAlignment = Element.ALIGN_CENTER
        companyCell.verticalAlignment = Element.ALIGN_MIDDLE
        companyCell.setPaddingTop(6f)

        val p = Paragraph()
        p.alignment = Element.ALIGN_CENTER

        p.add(Phrase(invoice.seller.name.uppercase(), font(18f, Font.BOLD)))
        p.add(Chunk.NEWLINE)

        p.add(Phrase("Govt. approved BIS Certified hallmark Gold Jewellery", font(9f)))
        p.add(Chunk.NEWLINE)

        if (invoice.seller.address.isNotBlank()) {
            p.add(Phrase(invoice.seller.address, font(9f)))
            p.add(Chunk.NEWLINE)
        }

        if (invoice.seller.phonePrimary.isNotBlank()) {
            p.add(Phrase("Phone: ${invoice.seller.phonePrimary}", font(9f)))
            p.add(Chunk.NEWLINE)
        }

        if (invoice.seller.email.isNotBlank()) {
            p.add(Phrase("Email: ${invoice.seller.email}", font(9f)))
            p.add(Chunk.NEWLINE)
        }

        if (invoice.seller.gstin.isNotBlank()) {
            p.add(Phrase("GSTIN: ${invoice.seller.gstin}", font(9f)))
        }

        companyCell.addElement(p)
        headerTable.addCell(companyCell)

        /* ---------------- RIGHT : BIS LOGO ---------------- */

        val bisCell = PdfPCell()
        bisCell.border = Rectangle.NO_BORDER
        bisCell.fixedHeight = 100f
        bisCell.horizontalAlignment = Element.ALIGN_RIGHT
        bisCell.verticalAlignment = Element.ALIGN_TOP
        bisCell.setPaddingRight(6f)
        bisCell.setPaddingTop(6f)

        loadImage("bis_logo.png")?.let {
            it.scaleToFit(75f, 75f)
            it.alignment = Image.ALIGN_RIGHT
            bisCell.addElement(it)
        } ?: run {
            bisCell.addElement(
                Phrase(
                    "BIS\nBureau of Indian\nStandards",
                    font(8f, Font.BOLD)
                )
            )
        }

        headerTable.addCell(bisCell)

        ctx.add(headerTable)

        /* ---------------- TITLE ---------------- */

        val title = Paragraph("TAX INVOICE", font(16f, Font.BOLD))
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 10f
        ctx.add(title)
    }
}

