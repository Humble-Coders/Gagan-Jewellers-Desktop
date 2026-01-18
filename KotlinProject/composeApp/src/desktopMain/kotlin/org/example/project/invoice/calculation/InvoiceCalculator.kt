package org.example.project.invoice.calculation

import org.example.project.data.Order
import org.example.project.data.User
import org.example.project.data.StoreInfo
import org.example.project.data.Product
import org.example.project.data.CartItem
import org.example.project.data.extractKaratFromMaterialType
import org.example.project.invoice.model.*
import org.example.project.JewelryAppInitializer
import java.time.LocalDate
import java.time.ZoneId
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import java.security.MessageDigest

/**
 * Calculation layer - freezes all numbers before rendering
 * This ensures legal reproducibility
 */
class InvoiceCalculator {
    
    /**
     * Calculate invoice from InvoiceDraft (editable source)
     * This is the primary method for inline editing
     */
    fun calculate(draft: InvoiceDraft, storeInfo: StoreInfo): Invoice {
        // Calculate subtotal from items
        val subtotal = draft.items.sumOf { it.grossProductPrice }
        val exchangeGoldValue = draft.exchangeGold?.finalGoldExchangePrice ?: 0.0
        
        // Calculate taxable amount
        val amountAfterExchangeGold = (subtotal - exchangeGoldValue).coerceAtLeast(0.0)
        val taxableAmount = amountAfterExchangeGold - draft.discountAmount
        
        // Calculate GST
        val gstAmount = (taxableAmount * draft.gstPercentage / 100.0)
        
        // Calculate gross total
        val grossTotal = taxableAmount + gstAmount
        
        // Round off
        val roundedTotal = grossTotal.roundToInt().toDouble()
        val roundOff = roundedTotal - grossTotal
        
        // Convert draft items to invoice items
        // IMPORTANT: Preserve all item data exactly as-is when editing invoice
        // Only party section (buyer/seller details) should change, items remain unchanged
        val invoiceItems = draft.items.map { itemDraft ->
            // Preserve all item data from draft - do not recalculate anything
            // This ensures items table remains unchanged when editing invoice header fields
            // Use preserved net weights from draft, or calculate if not available (shouldn't happen after first calculation)
            val netStoneWeight = if (itemDraft.netStoneWeight > 0.0) {
                itemDraft.netStoneWeight // Use preserved value
            } else {
                itemDraft.stoneAmount / 100.0 // Fallback estimate (shouldn't be needed)
            }
            val netMetalWeight = if (itemDraft.netMetalWeight > 0.0) {
                itemDraft.netMetalWeight // Use preserved value
            } else {
                (itemDraft.grossWeight - netStoneWeight).coerceAtLeast(0.0) // Fallback estimate
            }
            
            InvoiceItem(
                variantNo = itemDraft.variantNo,
                productName = itemDraft.productName,
                materialWithPurity = itemDraft.materialWithPurity,
                quantity = itemDraft.quantity,
                grossWeight = itemDraft.grossWeight,
                netStoneWeight = netStoneWeight, // Preserved from original invoice
                netMetalWeight = netMetalWeight, // Preserved from original invoice
                makingPercent = itemDraft.makingPercent,
                grossProductPrice = itemDraft.grossProductPrice, // Preserved - no recalculation
                labourCharges = itemDraft.labourCharges, // Preserved - no recalculation
                stoneAmount = itemDraft.stoneAmount, // Preserved - no recalculation
                costValue = itemDraft.grossProductPrice + itemDraft.labourCharges + itemDraft.stoneAmount, // Preserved calculation
                barcodeId = itemDraft.barcodeId, // Preserved
                materialRatePerGram = itemDraft.materialRatePerGram, // Preserved
                diamondWeightInCarats = itemDraft.diamondWeightInCarats, // Preserved from original
                solitaireWeightInCarats = itemDraft.solitaireWeightInCarats // Preserved from original
            )
        }
        
        // Build bank info
        val bankInfo = BankInfo(
            accountHolder = storeInfo.bankInfo.accountHolder,
            accountNumber = storeInfo.bankInfo.accountNumber,
            ifscCode = storeInfo.bankInfo.ifscCode,
            branch = storeInfo.bankInfo.branch,
            accountType = storeInfo.bankInfo.accountType,
            pan = storeInfo.bankInfo.pan
        )
        
        // Format dates for IRN/ACK
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timestamp = draft.issueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val formattedAckDate = dateFormat.format(Date(timestamp))
        
        // Generate IRN and ACK
        val irn = generateIRN(draft.invoiceNo, timestamp)
        val ackNo = draft.invoiceNo.take(15)
        
        // Calculate memo number (use editable value or default)
        val memoNo = draft.memoNo ?: "GST/${draft.invoiceNo.takeLast(8)}"
        
        // Extract city from address (use editable value or extract from address)
        val city = draft.city ?: extractCityFromAddress(draft.buyer.address)
        
        // Calculate place of delivery (use editable value or default to buyer state)
        val placeOfDelivery = draft.placeOfDelivery ?: (draft.buyer.stateName.ifEmpty { draft.seller.stateName })
        
        return Invoice(
            invoiceNo = draft.invoiceNo,
            issueDate = draft.issueDate,
            seller = draft.seller,
            buyer = draft.buyer,
            items = invoiceItems,
            exchangeGold = draft.exchangeGold,
            subtotal = subtotal,
            exchangeGoldValue = exchangeGoldValue,
            discountAmount = draft.discountAmount,
            taxableAmount = taxableAmount,
            gstPercentage = draft.gstPercentage,
            gstAmount = gstAmount,
            grossTotal = grossTotal,
            roundOff = roundOff,
            netAmount = roundedTotal,
            paymentSplit = draft.paymentSplit,
            notes = draft.notes ?: "",
            irn = irn,
            ackNo = ackNo,
            ackDate = formattedAckDate,
            bankInfo = bankInfo,
            memoNo = memoNo,
            city = city,
            placeOfDelivery = placeOfDelivery
        )
    }
    
    /**
     * Calculate invoice from Order (legacy method for backward compatibility)
     */
    fun calculate(
        order: Order,
        customer: User,
        storeInfo: StoreInfo,
        products: Map<String, Product>,
        cartItems: List<CartItem>? = null // Optional: use CartItems from receipt screen for accurate item details
    ): Invoice {
        val subtotal = order.totalProductValue
        val exchangeGoldValue = order.exchangeGold?.finalGoldExchangePrice ?: 0.0
        
        // Calculate taxable amount
        val amountAfterExchangeGold = (subtotal - exchangeGoldValue).coerceAtLeast(0.0)
        val taxableAmount = amountAfterExchangeGold - order.discountAmount
        
        // Calculate GST
        val gstPercentage = order.gstPercentage
        val gstAmount = order.gstAmount
        
        // Calculate gross total
        val grossTotal = taxableAmount + gstAmount
        
        // Round off
        val roundedTotal = grossTotal.roundToInt().toDouble()
        val roundOff = roundedTotal - grossTotal
        
        // Build invoice items - using same calculation logic as ReceiptScreen
        // CRITICAL: If cartItems are provided, ALWAYS use them (from receipt screen) - they contain updated product values
        // This ensures items table values match ReceiptScreen values exactly
        // Note: cartItems should always be provided when generating PDF from ReceiptScreen/Transaction
        if (cartItems == null || cartItems.isEmpty()) {
            println("⚠️ WARNING: No CartItems provided - will use OrderItems with database products")
            println("   - This may result in incorrect values if product was updated in CartTable")
            println("   - Order has ${order.items.size} OrderItems")
        }
        
        val invoiceItems = if (cartItems != null && cartItems.isNotEmpty()) {
            println("📊 INVOICE CALCULATOR: Using ${cartItems.size} CartItems from receipt screen")
            println("✅ CRITICAL: Using cartItem.product directly (same as ReceiptScreen) to ensure values match exactly")
            println("⚠️ IMPORTANT: Ignoring products map - using cartItem.product exclusively for accurate values")
            
            // Validate that all cartItems have valid products
            val validCartItems = cartItems.filter { cartItem ->
                val hasProduct = cartItem.product.id.isNotEmpty()
                if (!hasProduct) {
                    println("⚠️ WARNING: CartItem ${cartItem.productId} has empty product - skipping")
                }
                hasProduct
            }
            
            if (validCartItems.isEmpty()) {
                println("❌ ERROR: No valid CartItems found - falling back to OrderItems")
                // Fallback to OrderItems if no valid CartItems
                order.items.mapNotNull { orderItem ->
                    val product = products[orderItem.productId] ?: return@mapNotNull null
                    val quantity = orderItem.quantity
                    val grossWeightPerItem = product.totalWeight
                    val grossWeight = grossWeightPerItem * quantity
                    createInvoiceItemFromOrderItem(orderItem, product, quantity, grossWeightPerItem, grossWeight)
                }
            } else {
                // Use CartItems from receipt screen - matches ReceiptScreen calculation exactly
                // CRITICAL: Use cartItem.product directly - same as ReceiptScreen (line 1055: val currentProduct = cartItem.product)
                validCartItems.mapNotNull { cartItem ->
                    // CRITICAL: Use cartItem.product directly - DO NOT use products map
                    // This ensures we use the updated makingPercent and labourRate from CartTable edits
                    val productToUse = cartItem.product
                val quantity = cartItem.quantity
                
                    // Verify we're using cartItem.product and not database product
                    val dbProduct = products[cartItem.productId]
                    if (dbProduct != null) {
                        val makingPercentDiff = kotlin.math.abs(productToUse.makingPercent - dbProduct.makingPercent)
                        val labourRateDiff = kotlin.math.abs(productToUse.labourRate - dbProduct.labourRate)
                        if (makingPercentDiff > 0.001 || labourRateDiff > 0.001) {
                            println("✅ VERIFIED: Using UPDATED values from cartItem.product (not database)")
                            println("   - Making %: DB=${dbProduct.makingPercent} vs Cart=${productToUse.makingPercent} (diff=$makingPercentDiff)")
                            println("   - Labour Rate: DB=${dbProduct.labourRate} vs Cart=${productToUse.labourRate} (diff=$labourRateDiff)")
                        }
                    }
                
                println("📊 INVOICE ITEM: ${productToUse.name}")
                    println("   - Product ID: ${cartItem.productId}")
                println("   - Quantity: $quantity")
                    println("   - Making %: ${productToUse.makingPercent} (from cartItem.product)")
                    println("   - Labour Rate: ${productToUse.labourRate} (from cartItem.product)")
                println("   - Material Weight: ${productToUse.materialWeight}")
                println("   - Total Weight: ${productToUse.totalWeight}")
                    println("   - Gross Weight: ${if (cartItem.grossWeight > 0) cartItem.grossWeight else productToUse.totalWeight}")
                println("   - Metal: ${cartItem.metal}")
                println("   - Custom Gold Rate: ${cartItem.customGoldRate}")
                println("   - Stones count: ${productToUse.stones.size}")
                
                    // Pass cartItem.product directly (same as ReceiptScreen uses)
                createInvoiceItemFromCartItem(cartItem, productToUse, quantity)
                }
            }
        } else {
            // Fallback to OrderItems if CartItems not available
            order.items.mapNotNull { orderItem ->
            val product = products[orderItem.productId] ?: return@mapNotNull null
            
            val quantity = orderItem.quantity
            
            // Use same logic as ReceiptScreen: use cartItem.grossWeight if available, else product.totalWeight
            // Since OrderItem doesn't have grossWeight, we use product.totalWeight
            val grossWeightPerItem = product.totalWeight
            val grossWeight = grossWeightPerItem * quantity
            
                createInvoiceItemFromOrderItem(orderItem, product, quantity, grossWeightPerItem, grossWeight)
            }
        }
        
        // Build seller (store) party
        val mainStore = storeInfo.mainStore
        val seller = Party(
            name = mainStore.companyName,
            address = mainStore.address,
            phone = mainStore.phone,
            phonePrimary = mainStore.phonePrimary,
            phoneSecondary = mainStore.phoneSecondary,
            email = mainStore.email,
            gstin = mainStore.gstin,
            pan = mainStore.pan.takeIf { it.isNotEmpty() },
            stateCode = mainStore.stateCode,
            stateName = mainStore.stateName,
            certification = mainStore.certification
        )
        
        // Build buyer (customer) party
        val buyer = Party(
            name = customer.name,
            address = customer.address,
            phone = customer.phone,
            email = customer.email,
            gstin = "", // Customer may not have GSTIN
            pan = null,
            stateCode = extractStateCode(customer.address),
            stateName = extractStateName(customer.address),
            certification = ""
        )
        
        // Build exchange gold info
        val exchangeGoldInfo = order.exchangeGold?.let {
            ExchangeGoldInfo(
                productName = it.productName,
                goldWeight = it.goldWeight,
                goldPurity = it.goldPurity,
                goldRate = it.goldRate,
                finalGoldExchangePrice = it.finalGoldExchangePrice,
                totalProductWeight = it.totalProductWeight,
                percentage = it.percentage
            )
        }
        
        // Build payment split
        val paymentSplit = order.paymentSplit?.let {
            PaymentSplit(
                cash = it.cash,
                bank = it.bank,
                dueAmount = it.dueAmount
            )
        }
        
        // Build bank info
        val bankInfo = BankInfo(
            accountHolder = storeInfo.bankInfo.accountHolder,
            accountNumber = storeInfo.bankInfo.accountNumber,
            ifscCode = storeInfo.bankInfo.ifscCode,
            branch = storeInfo.bankInfo.branch,
            accountType = storeInfo.bankInfo.accountType
        )
        
        // Format dates for IRN/ACK
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedAckDate = dateFormat.format(Date(order.createdAt))
        
        // Generate IRN and ACK
        val irn = generateIRN(order.orderId, order.createdAt)
        val ackNo = order.orderId.take(15)
        
        // Calculate memo number (default format)
        val memoNo = "GST/${order.orderId.takeLast(8)}"
        
        // Extract city from address
        val city = extractCityFromAddress(buyer.address)
        
        // Calculate place of delivery (default to buyer state)
        val placeOfDelivery = buyer.stateName.ifEmpty { seller.stateName }
        
        return Invoice(
            invoiceNo = order.orderId,
            issueDate = LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(order.createdAt),
                ZoneId.systemDefault()
            ),
            seller = seller,
            buyer = buyer,
            items = invoiceItems,
            exchangeGold = exchangeGoldInfo,
            subtotal = subtotal,
            exchangeGoldValue = exchangeGoldValue,
            discountAmount = order.discountAmount,
            taxableAmount = taxableAmount,
            gstPercentage = gstPercentage,
            gstAmount = gstAmount,
            grossTotal = grossTotal,
            roundOff = roundOff,
            netAmount = roundedTotal,
            paymentSplit = paymentSplit,
            notes = order.notes,
            irn = irn,
            ackNo = ackNo,
            ackDate = formattedAckDate,
            bankInfo = bankInfo,
            memoNo = memoNo,
            city = city,
            placeOfDelivery = placeOfDelivery
        )
    }
    
    private fun extractStateCode(address: String): String {
        val stateCodePattern = Regex("""\b([0-9]{2})\b""")
        val match = stateCodePattern.find(address)
        return match?.groupValues?.get(1) ?: ""
    }
    
    private fun extractStateName(address: String): String {
        val states = listOf("Punjab", "Haryana", "Delhi", "Uttar Pradesh", "Rajasthan", "Himachal Pradesh", "Bihar", "West Bengal")
        for (state in states) {
            if (address.contains(state, ignoreCase = true)) {
                return state
            }
        }
        return ""
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
    
    /**
     * Generate IRN (Invoice Reference Number) using SHA-256 hash
     */
    private fun generateIRN(orderId: String, timestamp: Long): String {
        val input = "$orderId$timestamp"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Create InvoiceItem from CartItem using ReceiptScreen calculation logic
     * Uses EXACT same calculation as CartTable, PaymentScreen, and ReceiptScreen
     * This ensures invoice table matches receipt screen exactly
     */
    private fun createInvoiceItemFromCartItem(
        cartItem: CartItem,
        currentProduct: Product,
        quantity: Int
    ): InvoiceItem {
        // CRITICAL: Use currentProduct parameter (same product that ReceiptScreen uses)
        // ReceiptScreen uses: val currentProduct = cartItem.product (line 1055)
        // The currentProduct parameter is cartItem.product (passed from caller), matching ReceiptScreen exactly
        // DO NOT use products map - it may have stale values from database
        val productToUse = currentProduct
        
        // Verify we're using the correct product (should match cartItem.product)
        if (productToUse.id != cartItem.product.id) {
            println("⚠️ WARNING: Product ID mismatch! Using productToUse.id=${productToUse.id} but cartItem.product.id=${cartItem.product.id}")
        }
        
        // EXACT same as ReceiptScreen line 1058
        val grossWeight = if (cartItem.grossWeight > 0) cartItem.grossWeight else productToUse.totalWeight
        // EXACT same as ReceiptScreen line 1059 - use makingPercent from cartItem.product (may be updated in CartTable)
        val makingPercentage = productToUse.makingPercent
        // EXACT same as ReceiptScreen line 1060 - use labourRate from cartItem.product (may be updated in CartTable)
        val labourRatePerGram = productToUse.labourRate
        
        println("🔄 RECALCULATING AS RECEIPT SCREEN:")
        println("   - Product ID: ${productToUse.id}")
        println("   - Product Name: ${productToUse.name}")
        println("   - Gross Weight: $grossWeight (cartItem.grossWeight=${cartItem.grossWeight}, product.totalWeight=${productToUse.totalWeight})")
        println("   - Making %: $makingPercentage (from cartItem.product.makingPercent=${cartItem.product.makingPercent})")
        println("   - Labour Rate: $labourRatePerGram (from cartItem.product.labourRate=${cartItem.product.labourRate})")
        println("   - Material Weight: ${productToUse.materialWeight}")
        println("   - Total Weight: ${productToUse.totalWeight}")
        println("   - Quantity: $quantity")
        
        // EXACT same as ReceiptScreen line 1063
        val stoneBreakdown = org.example.project.ui.calculateStonePrices(productToUse.stones)
        val kundanPrice = stoneBreakdown.kundanPrice
        val kundanWeight = stoneBreakdown.kundanWeight
        val jarkanPrice = stoneBreakdown.jarkanPrice
        val jarkanWeight = stoneBreakdown.jarkanWeight
        val diamondPrice = stoneBreakdown.diamondPrice
        val diamondWeight = stoneBreakdown.diamondWeight // in carats (for display)
        val diamondWeightInGrams = stoneBreakdown.diamondWeightInGrams // in grams (for calculation)
        val solitairePrice = stoneBreakdown.solitairePrice
        val solitaireWeight = stoneBreakdown.solitaireWeight // in carats (for display)
        val solitaireWeightInGrams = stoneBreakdown.solitaireWeightInGrams // in grams (for calculation)
        val colorStonesPrice = stoneBreakdown.colorStonesPrice
        val colorStonesWeight = stoneBreakdown.colorStonesWeight // in grams
        
        // EXACT same as ReceiptScreen lines 1078-1104
        val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
        val metalKarat = if (cartItem.metal.isNotEmpty()) {
            cartItem.metal.replace("K", "").toIntOrNull() ?: extractKaratFromMaterialType(productToUse.materialType)
        } else {
            extractKaratFromMaterialType(productToUse.materialType)
        }
        val collectionRate = try {
            ratesVM.calculateRateForMaterial(productToUse.materialId, productToUse.materialType, metalKarat)
        } catch (e: Exception) { 
            0.0 
        }
        // EXACT same as ReceiptScreen - use MetalRatesManager.metalRates.value
        val metalRates = org.example.project.data.MetalRatesManager.metalRates.value
        val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
        val goldRate = if (cartItem.customGoldRate > 0) cartItem.customGoldRate else defaultGoldRate
        
        // EXACT same as ReceiptScreen lines 1089-1099
        val materialTypeLower = productToUse.materialType.lowercase()
        val silverPurity = when {
            materialTypeLower.contains("999") -> 999
            materialTypeLower.contains("925") || materialTypeLower.contains("92.5") -> 925
            materialTypeLower.contains("900") || materialTypeLower.contains("90.0") -> 900
            else -> {
                val threeDigits = Regex("(\\d{3})").find(materialTypeLower)?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (threeDigits != null && threeDigits in listOf(900, 925, 999)) threeDigits else 999
            }
        }
        val silverRate = metalRates.getSilverRateForPurity(silverPurity)
        val goldRatePerGram = if (collectionRate > 0) collectionRate else when {
            productToUse.materialType.contains("gold", ignoreCase = true) -> goldRate
            productToUse.materialType.contains("silver", ignoreCase = true) -> silverRate
            else -> goldRate
        }
        
        println("   - Metal Karat: $metalKarat")
        println("   - Collection Rate: $collectionRate")
        println("   - Default Gold Rate: $defaultGoldRate")
        println("   - Custom Gold Rate: ${cartItem.customGoldRate}")
        println("   - Gold Rate Used: $goldRate")
        println("   - Silver Purity: $silverPurity")
        println("   - Silver Rate: $silverRate")
        println("   - Final Gold Rate Per Gram: $goldRatePerGram")
        
        // EXACT same as ReceiptScreen lines 1107-1126
        val priceInputs = org.example.project.ui.ProductPriceInputs(
            grossWeight = grossWeight, // Use grossWeight (not grossWeightPerItem - this matches ReceiptScreen)
            goldPurity = productToUse.materialType,
            goldWeight = productToUse.materialWeight.takeIf { it > 0 } ?: grossWeight, // Use grossWeight
            makingPercentage = makingPercentage,
            labourRatePerGram = labourRatePerGram,
            kundanPrice = kundanPrice,
            kundanWeight = kundanWeight,
            jarkanPrice = jarkanPrice,
            jarkanWeight = jarkanWeight,
            diamondPrice = diamondPrice,
            diamondWeight = diamondWeight,
            diamondWeightInGrams = diamondWeightInGrams,
            solitairePrice = solitairePrice,
            solitaireWeight = solitaireWeight,
            solitaireWeightInGrams = solitaireWeightInGrams,
            colorStonesPrice = colorStonesPrice,
            colorStonesWeight = colorStonesWeight,
            goldRatePerGram = goldRatePerGram
        )
        
        // Use the same calculation function as ReceiptScreen (line 1129)
        val priceResult = org.example.project.ui.calculateProductPrice(priceInputs)
        
        println("📊 PRICE RESULT:")
        println("   - New Weight (per item): ${priceResult.newWeight}")
        println("   - Gold Price (per item): ${priceResult.goldPrice}")
        println("   - Labour Charges (per item): ${priceResult.labourCharges}")
        println("   - Stone Price (per item): ${priceResult.totalStonesPrice}")
        println("   - Total Price (per item): ${priceResult.totalProductPrice}")
        
        // Calculate weights (EXACT same as ReceiptScreen)
        // Receipt screen shows priceResult.newWeight as the weight per item (line 1180)
        // For invoice table, we show total weights (multiplied by quantity)
        val totalStonesWeightInGrams = (stoneBreakdown.kundanWeight + 
                                        stoneBreakdown.jarkanWeight + 
                                        stoneBreakdown.diamondWeightInGrams + 
                                        stoneBreakdown.solitaireWeightInGrams + 
                                        stoneBreakdown.colorStonesWeight) * quantity
        val netStoneWeight = totalStonesWeightInGrams
        val netMetalWeight = priceResult.newWeight * quantity - netStoneWeight
        
        // Calculate prices (EXACT same as ReceiptScreen lines 1133-1136)
        // Base Amount = priceResult.goldPrice * quantity
        val grossProductPrice = priceResult.goldPrice * quantity
        // Labour Charges = priceResult.labourCharges * quantity
        val labourCharges = priceResult.labourCharges * quantity
        // Stone Amount = priceResult.totalStonesPrice * quantity
        val stoneAmount = priceResult.totalStonesPrice * quantity
        // Item Total = priceResult.totalProductPrice * quantity
        val costValue = priceResult.totalProductPrice * quantity
        
        println("📊 FINAL VALUES:")
        println("   - Gross Weight (total): ${priceResult.newWeight * quantity}")
        println("   - Base Amount: $grossProductPrice")
        println("   - Labour Charges: $labourCharges")
        println("   - Stone Amount: $stoneAmount")
        println("   - Item Total: $costValue")
        
        // Use currentProduct.makingPercent directly (EXACT same as ReceiptScreen line 1188)
        // This will reflect any changes made in cart screen
        val makingPercent = makingPercentage
        
        // Format material display - use "K" instead of "Karat"
        // Use cartItem.product for display values
        val materialDisplay = formatMaterialDisplay(productToUse.materialType)
        
        // Calculate diamond and solitaire weights separately in carats (cent value) - multiply by quantity
        val diamondWeightInCarats = diamondWeight * quantity
        val solitaireWeightInCarats = solitaireWeight * quantity
        
        return InvoiceItem(
            variantNo = productToUse.id.takeLast(12),
            productName = productToUse.name,
            materialWithPurity = materialDisplay,
            quantity = quantity,
            grossWeight = priceResult.newWeight * quantity, // Use newWeight (after making) like ReceiptScreen shows
            netStoneWeight = netStoneWeight,
            netMetalWeight = netMetalWeight,
            makingPercent = makingPercent,
            grossProductPrice = grossProductPrice,
            labourCharges = labourCharges,
            stoneAmount = stoneAmount,
            costValue = costValue,
            barcodeId = cartItem.selectedBarcodeIds.firstOrNull() ?: "", // Get barcode ID from CartItem
            diamondWeightInCarats = diamondWeightInCarats, // Diamond weight in carats
            solitaireWeightInCarats = solitaireWeightInCarats, // Solitaire weight in carats
            materialRatePerGram = goldRatePerGram // Material rate per gram
        )
    }
    
    /**
     * Create InvoiceItem from OrderItem (fallback when CartItems not available)
     */
    private fun createInvoiceItemFromOrderItem(
        orderItem: org.example.project.data.OrderItem,
        product: Product,
        quantity: Int,
        grossWeightPerItem: Double,
        grossWeight: Double
    ): InvoiceItem {
        val makingPercentage = product.makingPercent
        val labourRatePerGram = product.labourRate
        
        // Extract all stone types from stones array using helper function
        val stoneBreakdown = org.example.project.ui.calculateStonePrices(product.stones)
        val kundanPrice = stoneBreakdown.kundanPrice
        val kundanWeight = stoneBreakdown.kundanWeight
        val jarkanPrice = stoneBreakdown.jarkanPrice
        val jarkanWeight = stoneBreakdown.jarkanWeight
        val diamondPrice = stoneBreakdown.diamondPrice
        val diamondWeight = stoneBreakdown.diamondWeight
        val diamondWeightInGrams = stoneBreakdown.diamondWeightInGrams
        val solitairePrice = stoneBreakdown.solitairePrice
        val solitaireWeight = stoneBreakdown.solitaireWeight
        val solitaireWeightInGrams = stoneBreakdown.solitaireWeightInGrams
        val colorStonesPrice = stoneBreakdown.colorStonesPrice
        val colorStonesWeight = stoneBreakdown.colorStonesWeight
        
        // Get material rate
        val ratesVM = JewelryAppInitializer.getMetalRateViewModel()
        val metalKarat = extractKaratFromMaterialType(product.materialType)
        val collectionRate = try {
            ratesVM.calculateRateForMaterial(product.materialId, product.materialType, metalKarat)
        } catch (e: Exception) { 
            0.0 
        }
        val metalRates = org.example.project.data.MetalRatesManager.metalRates.value
        val defaultGoldRate = metalRates.getGoldRateForKarat(metalKarat)
        
        // Extract silver purity from material type
        val materialTypeLower = product.materialType.lowercase()
        val silverPurity = when {
            materialTypeLower.contains("999") -> 999
            materialTypeLower.contains("925") || materialTypeLower.contains("92.5") -> 925
            materialTypeLower.contains("900") || materialTypeLower.contains("90.0") -> 900
            else -> {
                val threeDigits = Regex("(\\d{3})").find(materialTypeLower)?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (threeDigits != null && threeDigits in listOf(900, 925, 999)) threeDigits else 999
            }
        }
        val silverRate = metalRates.getSilverRateForPurity(silverPurity)
        val goldRatePerGram = if (collectionRate > 0) collectionRate else when {
            product.materialType.contains("gold", ignoreCase = true) -> defaultGoldRate
            product.materialType.contains("silver", ignoreCase = true) -> silverRate
            else -> defaultGoldRate
        }
        
        // Build ProductPriceInputs
        val priceInputs = org.example.project.ui.ProductPriceInputs(
            grossWeight = grossWeightPerItem,
            goldPurity = product.materialType,
            goldWeight = product.materialWeight.takeIf { it > 0 } ?: grossWeightPerItem,
            makingPercentage = makingPercentage,
            labourRatePerGram = labourRatePerGram,
            kundanPrice = kundanPrice,
            kundanWeight = kundanWeight,
            jarkanPrice = jarkanPrice,
            jarkanWeight = jarkanWeight,
            diamondPrice = diamondPrice,
            diamondWeight = diamondWeight,
            diamondWeightInGrams = diamondWeightInGrams,
            solitairePrice = solitairePrice,
            solitaireWeight = solitaireWeight,
            solitaireWeightInGrams = solitaireWeightInGrams,
            colorStonesPrice = colorStonesPrice,
            colorStonesWeight = colorStonesWeight,
            goldRatePerGram = goldRatePerGram
        )
        
        val priceResult = org.example.project.ui.calculateProductPrice(priceInputs)
        
        // Calculate weights
        val totalStonesWeightInGrams = (stoneBreakdown.kundanWeight + 
                                        stoneBreakdown.jarkanWeight + 
                                        stoneBreakdown.diamondWeightInGrams + 
                                        stoneBreakdown.solitaireWeightInGrams + 
                                        stoneBreakdown.colorStonesWeight) * quantity
        val netStoneWeight = totalStonesWeightInGrams
        val netMetalWeight = priceResult.newWeight * quantity - netStoneWeight
        
        // Calculate prices
        val grossProductPrice = priceResult.goldPrice * quantity
        val labourCharges = priceResult.labourCharges * quantity
        val stoneAmount = priceResult.totalStonesPrice * quantity
        val costValue = priceResult.totalProductPrice * quantity
        
        // Use product.makingPercent directly (same as ReceiptScreen)
        val makingPercent = makingPercentage
        
        // Format material display
        val materialDisplay = formatMaterialDisplay(product.materialType)
        
        return InvoiceItem(
            variantNo = product.id.takeLast(12),
            productName = product.name,
            materialWithPurity = materialDisplay,
            quantity = quantity,
            grossWeight = priceResult.newWeight * quantity, // Use newWeight (after making) like ReceiptScreen shows
            netStoneWeight = netStoneWeight,
            netMetalWeight = netMetalWeight,
            makingPercent = makingPercent,
            grossProductPrice = grossProductPrice,
            labourCharges = labourCharges,
            stoneAmount = stoneAmount,
            costValue = costValue,
            barcodeId = orderItem.barcodeId
        )
    }
    
    /**
     * Format material display - use "K" instead of "Karat"
     */
    private fun formatMaterialDisplay(materialType: String): String {
        return when {
            materialType.contains("24", ignoreCase = true) -> "G-24K"
            materialType.contains("22", ignoreCase = true) -> "G-22K"
            materialType.contains("18", ignoreCase = true) -> "G-18K"
            materialType.contains("silver", ignoreCase = true) -> {
                val materialTypeLower = materialType.lowercase()
                val silverPurity = when {
                    materialTypeLower.contains("999") -> "999"
                    materialTypeLower.contains("925") || materialTypeLower.contains("92.5") -> "925"
                    materialTypeLower.contains("900") || materialTypeLower.contains("90.0") -> "900"
                    else -> {
                        val threeDigits = Regex("(\\d{3})").find(materialTypeLower)?.groupValues?.getOrNull(1)
                        threeDigits ?: "999"
                    }
                }
                "S-$silverPurity"
            }
            else -> {
                val karatMatch = Regex("""(\d+)K""", RegexOption.IGNORE_CASE).find(materialType)
                if (karatMatch != null) {
                    "G-${karatMatch.groupValues[1]}K"
                } else {
                    materialType.uppercase()
                }
            }
        }
    }
}

