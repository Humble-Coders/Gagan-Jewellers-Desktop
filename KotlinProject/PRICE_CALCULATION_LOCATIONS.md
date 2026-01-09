# Product Price Calculation Locations

This document maps all locations where product prices are calculated across different screens in the application.

## Calculation Formula

The standard price calculation follows this pattern:
```
Total Price = Base Amount + Making Charges + Stone Amount + VA Charges
```

Where:
- **Base Amount** = Net Weight × Material Rate × Quantity
- **Making Charges** = Net Weight × Making Rate × Quantity
- **Stone Amount** = Stone Rate × Stone Quantity × CW Weight (if has stones)
- **VA Charges** = Value Addition charges (fixed amount)

---

## 1. Dashboard Screen (`DashboardScreen.kt`)

### Location: `GroupedProductRow` composable (lines ~683-696)
**Purpose**: Display dynamic price for products in inventory table

```kotlin
val dynamicPrice = remember(...) {
    val netWeight = (product.totalWeight - product.lessWeight).coerceAtLeast(0.0)
    val materialRate = getMaterialRateForProduct(product, metalRatesList)
    val baseAmount = netWeight * materialRate
    val makingCharges = netWeight * product.defaultMakingRate
    val stoneAmount = if (product.hasStones && product.cwWeight > 0 && product.stoneRate > 0) {
        product.stoneRate * product.stoneQuantity * product.cwWeight
    } else 0.0
    baseAmount + makingCharges + stoneAmount + product.vaCharges
}
```

**Notes**:
- Uses `getMaterialRateForProduct()` helper function (line 1563)
- Prefers collection rate from `MetalRateViewModel`
- Falls back to `MetalRatesManager` if collection rate not available
- Shows per-item price (quantity = 1)

---

## 2. Product Detail Screen (`ProductDetailScreen.kt`)

### Location: `calculateProductTotalCost()` function (lines 499-524)
**Purpose**: Calculate total cost for a product including quantity

```kotlin
private fun calculateProductTotalCost(product: Product): Double {
    val netWeight = (product.totalWeight - product.lessWeight).coerceAtLeast(0.0)
    val materialRate = getMaterialRateForProduct(product)
    val baseAmount = netWeight * materialRate * product.quantity
    val makingCharges = netWeight * product.defaultMakingRate * product.quantity
    val stoneAmount = if (product.hasStones) {
        if (product.cwWeight > 0 && product.stoneRate > 0) {
            product.cwWeight * product.stoneRate * product.quantity
        } else 0.0
    } else 0.0
    return baseAmount + makingCharges + stoneAmount + product.vaCharges
}
```

**Helper Function**: `getMaterialRateForProduct()` (line 530)
- Uses `MetalRateViewModel.calculateRateForMaterial()`
- Falls back to `MetalRatesManager` rates

---

## 3. Cart Building Step / Shop Inventory (`CartBuildingStep.kt`)

### Location 1: `ShopInventoryProductCard` composable (lines ~599-623)
**Purpose**: Display price for products in cart building screen

```kotlin
val displayPrice = remember(cartItems, product, cartItem, metalRates) {
    if (cartItem != null) {
        // Use cart item values if available, otherwise product defaults
        val metalRate = // ... (collection rate or fallback)
        val netWeight = grossWeight - lessWeight
        val baseAmount = netWeight * metalRate * cartItem.quantity
        val makingCharges = netWeight * makingPerGram * cartItem.quantity
        val stoneAmount = stoneRate * stoneQuantity * cwWeight
        baseAmount + makingCharges + stoneAmount + vaCharges
    } else {
        // Not in cart: use dynamic calculation with current metal rates
        // ... similar calculation
    }
}
```

### Location 2: `calculateProductTotalCost()` function (lines 1622-1656)
**Purpose**: Calculate total cost for shop inventory display

```kotlin
private fun calculateProductTotalCost(product: Product, metalRates: List<MetalRate>): Double {
    val netWeight = (product.totalWeight - product.lessWeight).coerceAtLeast(0.0)
    val qty = 1  // Always show per-item price
    val materialRate = getMaterialRateForProduct(product, metalRates)
    val baseAmount = netWeight * materialRate * qty
    val makingCharges = netWeight * product.defaultMakingRate * qty
    val stoneAmount = if (product.hasStones) {
        product.stoneRate * (product.stoneQuantity.takeIf { it > 0 } ?: 1.0) * product.cwWeight
    } else 0.0
    return baseAmount + makingCharges + stoneAmount + product.vaCharges
}
```

**Helper Function**: `getMaterialRateForProduct()` (line 1662)
- Similar to DashboardScreen implementation

---

## 4. Cart Table / Cart Detail Panel (`CartTable.kt`)

### Location: Cart item detail calculation (lines ~700-751)
**Purpose**: Calculate detailed breakdown for cart items including GST

```kotlin
val baseAmount = netWeight * metalRate * quantity
val makingCharges = netWeight * makingChargesPerGram * quantity
val stoneAmount = stoneRate * stoneQuantity * cwWeight * quantity
val totalCharges = baseAmount + makingCharges + stoneAmount + (vaCharges * quantity)
val discountAmount = totalCharges * (discountPercent / 100)
val taxableAmount = totalCharges - discountAmount

// Split GST: 3% on base, 5% on making (after discount)
val discountFactor = if (totalCharges > 0) (taxableAmount / totalCharges) else 1.0
val discountedBase = baseAmount * discountFactor
val discountedMaking = makingCharges * discountFactor
val gstOnBase = discountedBase * 0.03
val gstOnMaking = discountedMaking * 0.05
val totalGst = gstOnBase + gstOnMaking
val finalAmount = taxableAmount + totalGst
```

**Notes**:
- Includes discount calculation
- Uses split GST model (3% on base, 5% on making)
- Applies discount proportionally to base and making charges

---

## 5. Cart ViewModel (`CartViewModel.kt`)

### Location 1: `calculateItemSubtotal()` function (lines 537-571)
**Purpose**: Calculate subtotal for individual cart items

```kotlin
private fun calculateItemSubtotal(cartItem: CartItem, metalRates: MetalRates): Double {
    val actualNetWeight = if (netWeight > 0) netWeight else (grossWeight - lessWeight)
    val goldRate = metalRates.getGoldRateForKarat(extractKaratFromMaterialType(...))
    val silverRate = metalRates.getSilverRateForPurity(999)
    val baseAmount = when {
        cartItem.product.materialType.contains("gold", ignoreCase = true) -> actualNetWeight * goldRate
        cartItem.product.materialType.contains("silver", ignoreCase = true) -> actualNetWeight * silverRate
        else -> actualNetWeight * goldRate
    }
    val makingCharges = makingChargesPerGram * actualNetWeight
    val totalCharges = baseAmount + makingCharges + valueAddition
    val discountAmount = totalCharges * (cartItem.discountPercent / 100)
    val amountAfterDiscount = totalCharges - discountAmount
    val gst = amountAfterDiscount * 0.03  // 3% GST
    return amountAfterDiscount + gst
}
```

### Location 2: `getGST()` function (lines ~500-534)
**Purpose**: Calculate GST for cart items with split model

```kotlin
val baseAmount = // ... calculation
val makingCharges = netWeight * makingChargesPerGram * quantity
val stoneAmount = stoneRate * stoneQuantity * cwWeight
val totalCharges = baseAmount + makingCharges + stoneAmount + vaCharges
val discountAmount = totalCharges * (discountPercent / 100.0)
val taxableAmount = totalCharges - discountAmount

val discountFactor = if (totalCharges > 0) (taxableAmount / totalCharges) else 1.0
val discountedBase = baseAmount * discountFactor
val discountedMaking = makingCharges * discountFactor

val gstOnBase = discountedBase * 0.03
val gstOnMaking = discountedMaking * 0.05
return gstOnBase + gstOnMaking
```

**Notes**:
- Uses split GST model (3% on base, 5% on making)
- Applies discount proportionally before GST calculation

---

## 6. Add/Edit Product Screen (`AddEditProductScreen.kt`)

### Location: `totalProductCost` remember block (lines 259-284)
**Purpose**: Real-time price calculation while editing product

```kotlin
val totalProductCost = remember(totalWeight, lessWeight, defaultMakingRate, vaCharges, cwWeight, materialRate, stoneRate, stoneQuantity, hasStones) {
    val netWeight = (totalWeightValue - lessWeightValue).coerceAtLeast(0.0)
    val materialCost = netWeight * (materialRate.toDoubleOrNull() ?: 0.0)
    val makingCharges = netWeight * makingRate
    val stoneAmount = if (hasStones) {
        val stoneRateValue = stoneRate.toDoubleOrNull() ?: 0.0
        val stoneQuantityValue = stoneQuantity.toDoubleOrNull() ?: 0.0
        if (cw > 0 && stoneRateValue > 0 && stoneQuantityValue > 0) {
            cw * stoneRateValue * stoneQuantityValue
        } else 0.0
    } else 0.0
    materialCost + makingCharges + stoneAmount + va
}
```

**Notes**:
- Updates in real-time as user edits fields
- Uses string inputs converted to doubles
- No quantity multiplier (shows per-item cost)

---

## 7. Payment Screen (`PaymentScreen.kt`)

### Location: Payment calculation logic
**Purpose**: Calculate final payment amounts including discounts and GST

**Notes**:
- Uses `CartViewModel.calculateItemSubtotal()` for individual items
- Aggregates all cart items for total
- Applies payment-level discounts
- Calculates final GST and total

---

## 8. Receipt/Invoice Generation

### Locations:
- `HtmlCssBillGenerator.kt` (lines ~255-283)
- `PdfGeneratorService.kt` (lines ~241-270)
- `BillPDFGenerator.kt`

**Purpose**: Generate bill/invoice with price breakdown

**Notes**:
- Uses same calculation logic as CartViewModel
- Includes detailed breakdown for printing
- Shows base amount, making charges, stone amount, VA charges
- Displays discount and GST separately
- Shows final total

---

## 9. Preview Home Screen (`PreviewHomeScreen.kt`)

### Location: `calculateProductTotalCost()` function (lines 841-866)
**Purpose**: Calculate product cost for preview/home screen

```kotlin
private fun calculateProductTotalCost(product: Product): Double {
    val netWeight = (product.totalWeight - product.lessWeight).coerceAtLeast(0.0)
    val materialRate = getMaterialRateForProduct(product)
    val baseAmount = netWeight * materialRate * product.quantity
    val makingCharges = netWeight * product.defaultMakingRate * product.quantity
    val stoneAmount = if (product.hasStones) {
        if (product.cwWeight > 0 && product.stoneRate > 0) {
            product.cwWeight * product.stoneRate * product.quantity
        } else 0.0
    } else 0.0
    return baseAmount + makingCharges + stoneAmount + product.vaCharges
}
```

**Helper Function**: `getMaterialRateForProduct()` (line 872)
- Uses `MetalRatesManager` directly (no collection rate lookup)

---

## Material Rate Calculation

### Helper Functions

All screens use variations of `getMaterialRateForProduct()` which:

1. **Extract karat/purity** from `materialType` string (e.g., "18K Gold" → 18)
2. **Try collection rate first**: Use `MetalRateViewModel.calculateRateForMaterial()`
3. **Fallback to MetalRatesManager**: Use `getGoldRateForKarat()` or `getSilverRateForPurity()`

### Rate Sources Priority:
1. **Collection Rate** (from `rates` collection in Firestore) - Preferred
2. **MetalRatesManager** (from `metalRates` collection) - Fallback
3. **Default rates** (22K gold, 999 silver) - Last resort

---

## Key Differences Between Screens

| Screen | Quantity Multiplier | Discount | GST | Stone Calculation |
|--------|-------------------|----------|-----|-------------------|
| Dashboard | No (per-item) | No | No | `stoneRate × stoneQuantity × cwWeight` |
| Product Detail | Yes | No | No | `cwWeight × stoneRate × quantity` |
| Cart Building | Yes (if in cart) | No | No | `stoneRate × stoneQuantity × cwWeight` |
| Cart Table | Yes | Yes | Yes (split) | `stoneRate × stoneQuantity × cwWeight × quantity` |
| CartViewModel | Yes | Yes | Yes (split) | `stoneRate × stoneQuantity × cwWeight` |
| Add/Edit Product | No | No | No | `cw × stoneRate × stoneQuantity` |
| Payment | Yes | Yes | Yes | Via CartViewModel |

---

## Common Issues to Watch For

1. **Stone Amount Formula Inconsistency**: 
   - Some screens: `stoneRate × stoneQuantity × cwWeight`
   - Others: `cwWeight × stoneRate × quantity`
   - Cart: `stoneRate × stoneQuantity × cwWeight × quantity`

2. **Quantity Multiplier**:
   - Dashboard/Inventory: Shows per-item price (qty = 1)
   - Cart/Payment: Includes quantity multiplier

3. **Material Rate Source**:
   - Most screens prefer collection rate from `MetalRateViewModel`
   - Some fallback to `MetalRatesManager` directly

4. **GST Calculation**:
   - Simple screens: No GST
   - Cart/Payment: Split GST (3% base + 5% making)
   - Applied after discount proportionally

---

## Recommendations

1. **Centralize Calculation Logic**: Create a shared utility class for price calculations
2. **Standardize Stone Formula**: Use consistent formula across all screens
3. **Unify Rate Lookup**: Use same helper function everywhere
4. **Document Formula**: Add clear comments explaining calculation steps










