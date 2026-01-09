package org.example.project.utils

import org.example.project.data.OrderItem
import org.example.project.data.Order
import org.example.project.data.User
import org.example.project.data.CartItem
import org.example.project.data.Product
import org.example.project.data.PaymentMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Utility class for diagnosing PDF generation issues
 */
object PDFDiagnostics {
    
    /**
     * Creates a test order for PDF generation testing
     */
    fun createTestOrder(): Order {
        val testProduct = Product(
            id = "test-product-1",
            name = "Test Gold Ring",
            description = "A test gold ring for PDF generation",
            materialType = "Gold",
            weight = "5.0g",
            price = 25000.0
        )
        
        val testCartItem = CartItem(
            productId = testProduct.id,
            product = testProduct,
            quantity = 1,
            selectedWeight = 5.0
        )
        
        return Order(
            orderId = "TEST-ORDER-${System.currentTimeMillis()}",
            customerId = "test-customer", // Reference to users collection
            totalProductValue = 25000.0,
            discountAmount = 0.0,
            gstAmount = 4500.0,
            totalAmount = 29500.0,
            items = listOf(OrderItem(
                productId = testCartItem.productId,
                barcodeId = testCartItem.selectedBarcodeIds.firstOrNull() ?: "TEST-BARCODE",
                quantity = testCartItem.quantity
            )),
            createdAt = System.currentTimeMillis(),
            transactionDate = "19 October 2025 at 10:30:14 UTC+5:30",
            // status removed from Order model
            isGstIncluded = true
        )
    }
    
    /**
     * Creates a test customer for PDF generation testing
     */
    fun createTestCustomer(): User {
        return User(
            id = "test-customer",
            name = "Test Customer",
            email = "test@example.com",
            phone = "+91 9876543210",
            address = "123 Test Street, Test City, TC 12345"
        )
    }
    
    /**
     * Tests PDF generation using PdfGeneratorService (the production PDF generator)
     */
    fun testAllPDFGenerationMethods(): Boolean {
        println("🧪 ========== PDF GENERATION TESTING ==========")
        
        val testOrder = createTestOrder()
        val testCustomer = createTestCustomer()
        
        // Create test output directory
        val userHome = System.getProperty("user.home")
        val testDir = File(userHome, "PDFTest")
        if (!testDir.exists()) {
            testDir.mkdirs()
        }
        
        var allTestsPassed = true
        
        // Test: PdfGeneratorService (production PDF generator)
        println("🧪 Testing PdfGeneratorService (production PDF generator)...")
        try {
            val pdfPath = File(testDir, "test_invoice_pdfgenerator.pdf")
            val pdfGenerator = PdfGeneratorService()
            
            // Run in blocking context since this is a diagnostic function
            val result = runBlocking(Dispatchers.IO) {
                pdfGenerator.generateInvoicePDF(
                    order = testOrder,
                    customer = testCustomer,
                    outputFile = pdfPath
                )
            }
            
            result.fold(
                onSuccess = { file ->
                    if (file.exists() && file.length() > 0) {
                        println("✅ PdfGeneratorService test passed")
                        println("   Generated PDF: ${file.absolutePath} (${file.length()} bytes)")
            } else {
                        println("❌ PdfGeneratorService test failed: PDF file is empty or doesn't exist")
                allTestsPassed = false
            }
                },
                onFailure = { error ->
                    println("❌ PdfGeneratorService test failed: ${error.message}")
                    error.printStackTrace()
            allTestsPassed = false
        }
            )
        } catch (e: Exception) {
            println("❌ PdfGeneratorService test exception: ${e.message}")
            e.printStackTrace()
            allTestsPassed = false
        }
        
        println("🧪 ========== PDF TESTING COMPLETED ==========")
        println("📊 Overall result: ${if (allTestsPassed) "ALL TESTS PASSED" else "SOME TESTS FAILED"}")
        
        return allTestsPassed
    }
    
    /**
     * Checks system dependencies for PDF generation
     */
    fun checkSystemDependencies(): Boolean {
        println("🔍 ========== DEPENDENCY CHECK ==========")
        
        var allDependenciesOk = true
        
        // Check OpenHTMLToPDF (primary PDF engine)
        try {
            Class.forName("com.openhtmltopdf.pdfboxout.PdfRendererBuilder")
            println("✅ OpenHTMLToPDF available")
        } catch (e: ClassNotFoundException) {
            println("❌ OpenHTMLToPDF not available")
            allDependenciesOk = false
        }
        
        // Check Flying Saucer (fallback PDF engine)
        try {
            Class.forName("org.xhtmlrenderer.pdf.ITextRenderer")
            println("✅ Flying Saucer available (fallback)")
        } catch (e: ClassNotFoundException) {
            println("⚠️ Flying Saucer not available (fallback engine)")
            // Not critical since OpenHTMLToPDF is primary
        }
        
        // Check PDFBox (used by OpenHTMLToPDF)
        try {
            Class.forName("org.apache.pdfbox.pdmodel.PDDocument")
            println("✅ PDFBox available")
        } catch (e: ClassNotFoundException) {
            println("⚠️ PDFBox not available (may be required by OpenHTMLToPDF)")
        }
        
        println("🔍 ========== DEPENDENCY CHECK COMPLETED ==========")
        println("📊 Dependencies status: ${if (allDependenciesOk) "OK" else "ISSUES FOUND"}")
        
        return allDependenciesOk
    }
    
    /**
     * Tests file system permissions
     */
    fun testFileSystemPermissions(): Boolean {
        println("📁 ========== FILE SYSTEM PERMISSION TEST ==========")
        
        var permissionsOk = true
        
        try {
            val userHome = System.getProperty("user.home")
            val homeDir = File(userHome)
            
            if (homeDir.canWrite()) {
                println("✅ User home directory writable: $userHome")
            } else {
                println("❌ User home directory not writable: $userHome")
                permissionsOk = false
            }
            
            // Test creating a test directory
            val testDir = File(homeDir, "PDFTest_${System.currentTimeMillis()}")
            if (testDir.mkdirs()) {
                println("✅ Can create directories in user home")
                
                // Test creating a test file
                val testFile = File(testDir, "test.txt")
                if (testFile.createNewFile()) {
                    println("✅ Can create files in user home")
                    testFile.delete()
                } else {
                    println("❌ Cannot create files in user home")
                    permissionsOk = false
                }
                
                testDir.delete()
            } else {
                println("❌ Cannot create directories in user home")
                permissionsOk = false
            }
            
        } catch (e: Exception) {
            println("❌ File system permission test failed: ${e.message}")
            permissionsOk = false
        }
        
        println("📁 ========== FILE SYSTEM PERMISSION TEST COMPLETED ==========")
        println("📊 Permissions status: ${if (permissionsOk) "OK" else "ISSUES FOUND"}")
        
        return permissionsOk
    }
    
    /**
     * Runs comprehensive diagnostics
     */
    fun runComprehensiveDiagnostics(): Boolean {
        println("🔬 ========== COMPREHENSIVE PDF DIAGNOSTICS ==========")
        
        val dependencyCheck = checkSystemDependencies()
        val permissionCheck = testFileSystemPermissions()
        val pdfTest = testAllPDFGenerationMethods()
        
        val overallResult = dependencyCheck && permissionCheck && pdfTest
        
        println("🔬 ========== DIAGNOSTICS SUMMARY ==========")
        println("📊 Dependencies: ${if (dependencyCheck) "✅ OK" else "❌ ISSUES"}")
        println("📊 Permissions: ${if (permissionCheck) "✅ OK" else "❌ ISSUES"}")
        println("📊 PDF Generation: ${if (pdfTest) "✅ OK" else "❌ ISSUES"}")
        println("📊 Overall: ${if (overallResult) "✅ ALL SYSTEMS OK" else "❌ ISSUES FOUND"}")
        
        return overallResult
    }
}
