# Models and Screens Mapping Documentation

This document provides a comprehensive mapping of all data models used in the Gagan Jewellers Desktop application and the screens where they are utilized.

---

## Table of Contents
1. [Product Management Models](#product-management-models)
2. [User & Customer Models](#user--customer-models)
3. [Cart & Order Models](#cart--order-models)
4. [Payment Models](#payment-models)
5. [Metal Rate Models](#metal-rate-models)
6. [Inventory Models](#inventory-models)
7. [Appointment & Booking Models](#appointment--booking-models)
8. [Customization Models](#customization-models)
9. [Transaction Models](#transaction-models)
10. [Other Models](#other-models)

---

## Product Management Models

### 1. **Product**
**Location**: `data/models.kt`

**Description**: Core product model containing all product information including weights, materials, stones, pricing, and visibility configurations.

**Fields**:
- Basic Info: `id`, `name`, `description`, `price`, `categoryId`, `materialId`, `materialType`
- Weight Fields: `totalWeight`, `lessWeight`, `netWeight`, `cwWeight`
- Pricing: `defaultMakingRate`, `vaCharges`, `totalProductCost`, `customPrice`
- Stone Info: `hasStones`, `stoneName`, `stoneQuantity`, `stoneRate`, `stoneAmount`
- Materials: `materials` (List<ProductMaterial>)
- Visibility: `show` (ProductShowConfig)
- Collection: `isCollectionProduct`, `collectionId`
- Status: `available`, `featured`, `quantity`, `commonId`

**Used in Screens**:
- ✅ **DashboardScreen** - Display products in inventory table
- ✅ **AddEditProductScreen** - Create/edit product details
- ✅ **ProductDetailScreen** - Show detailed product information
- ✅ **CartBuildingStep** - Add products to cart
- ✅ **CartTable** - Display cart items
- ✅ **ReceiptScreen** - Generate receipts with product details
- ✅ **BarcodeEditScreen** - Edit product via barcode
- ✅ **PaymentScreen** - Process payment for products
- ✅ **BillingScreen** - Generate bills
- ✅ **PreviewHomeScreen** - Preview products on homepage

---

### 2. **ProductShowConfig**
**Location**: `data/models.kt`

**Description**: Configuration model for field-level visibility control in product display.

**Fields**: Boolean flags for each product field (`name`, `description`, `category`, `material`, `price`, `weight`, etc.)

**Used in Screens**:
- ✅ **AddEditProductScreen** - Configure field visibility
- ✅ **ProductDetailScreen** - Respect visibility settings
- ✅ **DashboardScreen** - Display only visible fields

---

### 3. **GroupedProduct**
**Location**: `data/models.kt`

**Description**: Groups products by `commonId` for dashboard display with combined quantities.

**Fields**: `baseProduct`, `quantity`, `individualProducts`, `barcodeIds`, `commonId`

**Used in Screens**:
- ✅ **DashboardScreen** - Display grouped products in inventory table
- ✅ **ProductDetailScreen** - Show grouped product information

---

### 4. **ProductMaterial**
**Location**: `data/models.kt`

**Description**: Represents a material specification within a product (metal or stone).

**Fields**: `id`, `materialId`, `materialName`, `materialType`, `weight`, `rate`, `isMetal`

**Used in Screens**:
- ✅ **AddEditProductScreen** - Add/edit material specifications
- ✅ **AddMaterialDialog** - Add materials to products
- ✅ **ProductDetailScreen** - Display material breakdown
- ✅ **CartBuildingStep** - Show material details in cart

---

### 5. **Category**
**Location**: `data/models.kt`

**Description**: Product category classification.

**Fields**: `id`, `name`, `description`, `imageUrl`, `hasGenderVariants`, `categoryType`, `isActive`, `order`

**Used in Screens**:
- ✅ **AddEditProductScreen** - Select product category
- ✅ **CategoryManagementScreen** - Manage categories
- ✅ **DashboardScreen** - Filter products by category
- ✅ **ProductDetailScreen** - Display category information

---

### 6. **CategoryType** (Enum)
**Location**: `data/models.kt`

**Description**: Enumeration of category types (RAW_GOLD, FINE_GOLD, CRYSTALS, SILVER, DIAMONDS, etc.)

**Used in Screens**:
- ✅ **CategoryManagementScreen** - Categorize products
- ✅ **AddEditProductScreen** - Category selection

---

### 7. **Material**
**Location**: `data/models.kt`

**Description**: Material master data (Gold, Silver, etc.)

**Fields**: `id`, `name`, `imageUrl`, `types`

**Used in Screens**:
- ✅ **AddEditProductScreen** - Material selection
- ✅ **AddMaterialDialog** - Material autocomplete
- ✅ **GoldRateScreen** - Material rate management
- ✅ **ProductDetailScreen** - Display material info

---

### 8. **Stone**
**Location**: `data/StonesRepository.kt`

**Description**: Stone master data (Diamond, Kundan, Jarkan, etc.)

**Fields**: `id`, `name`, `createdAt`

**Used in Screens**:
- ✅ **AddEditProductScreen** - Stone selection
- ✅ **AddMaterialDialog** - Stone selection for materials
- ✅ **GoldRateScreen** - Stone rate management

---

## User & Customer Models

### 9. **User**
**Location**: `data/models.kt`

**Description**: Customer/user information model.

**Fields**: `id`, `email`, `name`, `phone`, `address`, `notes`, `balance`, `customerId`, `profilePictureUrl`, `googleId`, `fcmToken`

**Used in Screens**:
- ✅ **ProfileScreen** - Display/edit user profile
- ✅ **CustomerSelectionStep** - Select customer for cart
- ✅ **CustomerTransactionsScreen** - Show customer transaction history
- ✅ **PaymentScreen** - Customer information in payment
- ✅ **AppointmentScreen** - Customer booking information
- ✅ **BillingScreen** - Customer details on bills
- ✅ **ReceiptScreen** - Customer information on receipts

---

### 10. **RecentlyViewedItem**
**Location**: `data/models.kt`

**Description**: Tracks products recently viewed by users.

**Fields**: `id`, `productId`, `viewedAt`

**Used in Screens**:
- ✅ **ProfileScreen** - Show recently viewed products
- ✅ **ProductDetailScreen** - Track product views

---

### 11. **WishlistItem**
**Location**: `data/models.kt`

**Description**: User wishlist items.

**Fields**: `id`, `productId`, `addedAt`

**Used in Screens**:
- ✅ **ProfileScreen** - Display wishlist
- ✅ **ProductDetailScreen** - Add/remove from wishlist
- ✅ **AppointmentScreen** - Wishlist products in bookings

---

## Cart & Order Models

### 12. **CartItem**
**Location**: `data/models.kt`

**Description**: Individual item in shopping cart with detailed pricing breakdown.

**Fields**: 
- Product Info: `productId`, `product`, `quantity`, `selectedBarcodeIds`
- Weight: `selectedWeight`, `grossWeight`, `totalWeight`, `lessWeight`, `netWeight`, `cwWeight`
- Pricing: `metal`, `customGoldRate`, `makingCharges`, `totalMakingCharges`, `stoneRate`, `stoneQuantity`, `stoneAmount`, `va`
- Calculations: `amount`, `discountPercent`, `discountAmount`, `totalCharges`, `taxableAmount`, `cgst`, `sgst`, `igst`, `totalGst`, `finalAmount`

**Used in Screens**:
- ✅ **CartBuildingStep** - Build cart with products
- ✅ **CartTable** - Display cart items with calculations
- ✅ **PaymentScreen** - Process cart items for payment
- ✅ **ReceiptScreen** - Generate receipts from cart items
- ✅ **BillingScreen** - Bill generation from cart

---

### 13. **Cart**
**Location**: `data/models.kt`

**Description**: Shopping cart container with items and customer reference.

**Fields**: `items`, `customerId`, `createdAt`, `updatedAt`

**Used in Screens**:
- ✅ **CartBuildingStep** - Manage cart
- ✅ **CartTable** - Display cart summary
- ✅ **PaymentScreen** - Process cart payment

---

### 14. **Order**
**Location**: `data/models.kt`

**Description**: Order model representing a completed transaction.

**Fields**:
- Order Info: `orderId`, `customerId`, `status`, `transactionDate`
- Payment: `paymentSplit`, `paymentStatus`
- Financial: `subtotal`, `discountAmount`, `discountPercent`, `taxableAmount`, `gstAmount`, `totalAmount`, `finalAmount`, `isGstIncluded`
- Items: `items` (List<OrderItem>)
- References: `metalRatesReference`
- Timestamps: `createdAt`, `updatedAt`, `completedAt`
- Notes: `notes`

**Used in Screens**:
- ✅ **ReceiptScreen** - Generate receipts from orders
- ✅ **BillingScreen** - Generate bills from orders
- ✅ **CustomerTransactionsScreen** - Display order history
- ✅ **PaymentScreen** - Create orders after payment

---

### 15. **OrderItem**
**Location**: `data/models.kt`

**Description**: Individual item within an order.

**Fields**: `productId`, `barcodeId`, `quantity`, `defaultMakingRate`, `vaCharges`, `materialType`

**Used in Screens**:
- ✅ **ReceiptScreen** - Display order items
- ✅ **BillingScreen** - Show items on bills
- ✅ **CustomerTransactionsScreen** - Order item details

---

### 16. **OrderStatus** (Enum)
**Location**: `data/models.kt`

**Description**: Order status enumeration (CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED)

**Used in Screens**:
- ✅ **CustomerTransactionsScreen** - Filter orders by status
- ✅ **ReceiptScreen** - Display order status
- ✅ **BillingScreen** - Order status on bills

---

## Payment Models

### 17. **PaymentMethod** (Enum)
**Location**: `data/models.kt`

**Description**: Payment method types (CASH, CARD, UPI, NET_BANKING, BANK_TRANSFER, CASH_ON_DELIVERY, DUE)

**Used in Screens**:
- ✅ **PaymentScreen** - Select payment method
- ✅ **PaymentSplitScreen** - Split payment across methods
- ✅ **ReceiptScreen** - Display payment method
- ✅ **BillingScreen** - Payment method on bills
- ✅ **CustomerTransactionsScreen** - Payment method filter

---

### 18. **PaymentSplit**
**Location**: `data/models.kt`

**Description**: Payment split across multiple payment methods.

**Fields**: `cashAmount`, `cardAmount`, `bankAmount`, `onlineAmount`, `dueAmount`, `totalAmount`

**Used in Screens**:
- ✅ **PaymentScreen** - Split payment
- ✅ **PaymentSplitScreen** - Detailed payment splitting
- ✅ **ReceiptScreen** - Payment breakdown
- ✅ **BillingScreen** - Payment split on bills
- ✅ **CustomerTransactionsScreen** - Payment split details

---

### 19. **PaymentStatus** (Enum)
**Location**: `data/models.kt`

**Description**: Payment status (PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED)

**Used in Screens**:
- ✅ **PaymentScreen** - Payment status tracking
- ✅ **CustomerTransactionsScreen** - Filter by payment status
- ✅ **ReceiptScreen** - Payment status display

---

### 20. **PaymentTransaction**
**Location**: `data/models.kt`

**Description**: Payment transaction record.

**Fields**: `id`, `cartId`, `paymentMethod`, `paymentSplit`, `subtotal`, `makingCharges`, `discountAmount`, `gstAmount`, `totalAmount`, `isGstIncluded`, `status`, `timestamp`, `items`

**Used in Screens**:
- ✅ **CustomerTransactionsScreen** - Display payment transactions
- ✅ **ReceiptScreen** - Transaction details

---

### 21. **DiscountType** (Enum)
**Location**: `data/models.kt`

**Description**: Discount calculation type (AMOUNT, PERCENTAGE, TOTAL_PAYABLE)

**Used in Screens**:
- ✅ **PaymentScreen** - Apply discounts
- ✅ **CartTable** - Discount calculation
- ✅ **ReceiptScreen** - Discount display

---

## Metal Rate Models

### 22. **MetalRate**
**Location**: `data/models.kt`

**Description**: Metal rate per gram for specific material and karat/purity.

**Fields**: `id`, `materialId`, `materialName`, `materialType`, `karat`, `pricePerGram`, `isActive`, `createdAt`, `updatedAt`, `previousRate`

**Used in Screens**:
- ✅ **GoldRateScreen** - Manage metal rates
- ✅ **AddEditProductScreen** - Auto-calculate product prices
- ✅ **AddMaterialDialog** - Material rate selection
- ✅ **DashboardScreen** - Dynamic price calculation
- ✅ **ProductDetailScreen** - Price calculation
- ✅ **CartBuildingStep** - Cart price calculation
- ✅ **CartTable** - Cart item pricing
- ✅ **PaymentScreen** - Final price calculation
- ✅ **ReceiptScreen** - Rate display on receipts
- ✅ **BillingScreen** - Rate on bills

---

### 23. **RateHistory**
**Location**: `data/models.kt`

**Description**: Historical rate information for tracking rate changes.

**Fields**: `pricePerGram`, `updatedAt`, `updatedBy`

**Used in Screens**:
- ✅ **GoldRateScreen** - Rate history display

---

### 24. **MetalPrices**
**Location**: `data/models.kt`

**Description**: Legacy metal prices model (deprecated in favor of MetalRate).

**Fields**: `goldPricePerGram`, `silverPricePerGram`, `lastUpdated`

**Used in Screens**:
- ⚠️ **Legacy screens** (being phased out)

---

### 25. **GoldRates**
**Location**: `data/models.kt`

**Description**: Gold rates for different karats (24K, 22K, 20K, 18K, 14K, 10K).

**Fields**: `rate24k`, `rate22k`, `rate20k`, `rate18k`, `rate14k`, `rate10k`, `lastUpdated`

**Used in Screens**:
- ⚠️ **Legacy screens** (being phased out in favor of MetalRate)

---

### 26. **SilverRates**
**Location**: `data/models.kt`

**Description**: Silver rates for different purities (999, 925, 900, 800).

**Fields**: `rate999`, `rate925`, `rate900`, `rate800`, `lastUpdated`

**Used in Screens**:
- ⚠️ **Legacy screens** (being phased out in favor of MetalRate)

---

### 27. **MetalRates**
**Location**: `data/models.kt`

**Description**: Combined gold and silver rates container.

**Fields**: `goldRates`, `silverRates`, `lastUpdated`

**Used in Screens**:
- ⚠️ **Legacy screens** (being phased out in favor of MetalRate)

---

## Inventory Models

### 28. **InventoryItem**
**Location**: `data/models.kt`

**Description**: Individual inventory item with barcode tracking.

**Fields**: `id`, `productId`, `barcodeId`, `status`, `location`, `notes`, `createdAt`, `updatedAt`, `soldAt`, `soldTo`

**Used in Screens**:
- ✅ **DashboardScreen** - Display inventory items
- ✅ **AddEditProductScreen** - Create inventory items
- ✅ **BarcodeEditScreen** - Edit inventory via barcode
- ✅ **CartBuildingStep** - Select inventory items for cart
- ✅ **CartTable** - Show selected barcodes
- ✅ **ReceiptScreen** - Mark items as sold

---

### 29. **InventoryStatus** (Enum)
**Location**: `data/models.kt`

**Description**: Inventory item status (AVAILABLE, SOLD, RESERVED, DAMAGED, RETURNED, LOST, REPAIR)

**Used in Screens**:
- ✅ **DashboardScreen** - Filter by inventory status
- ✅ **BarcodeEditScreen** - Update inventory status
- ✅ **CartBuildingStep** - Check availability

---

## Appointment & Booking Models

### 30. **Booking**
**Location**: `data/models.kt`

**Description**: Appointment/booking information.

**Fields**: `id`, `type`, `userId`, `userName`, `startTime`, `endTime`, `status`, `createdAt`, `wishlistProductIds`, `notes`, `serviceType`, `estimatedDuration`

**Used in Screens**:
- ✅ **AppointmentScreen** - Create/manage appointments
- ✅ **AppointmentTimeline** - Display appointment timeline
- ✅ **AddSlotsScreen** - Book available slots
- ✅ **ProfileScreen** - User appointment history

---

### 31. **BookingStatus** (Enum)
**Location**: `data/models.kt`

**Description**: Booking status (PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW)

**Used in Screens**:
- ✅ **AppointmentScreen** - Filter appointments by status
- ✅ **AppointmentTimeline** - Status indicators
- ✅ **AddSlotsScreen** - Slot booking status

---

### 32. **AppointmentWithUser**
**Location**: `data/models.kt`

**Description**: Booking combined with user information.

**Fields**: `booking`, `user`

**Used in Screens**:
- ✅ **AppointmentScreen** - Display appointments with customer info
- ✅ **AppointmentTimeline** - Timeline with user details

---

### 33. **AvailabilitySlot**
**Location**: `data/models.kt`

**Description**: Available time slot for appointments.

**Fields**: `id`, `type`, `startTime`, `endTime`, `slotDuration`, `createdAt`, `isActive`

**Used in Screens**:
- ✅ **AddSlotsScreen** - Create/manage availability slots
- ✅ **AppointmentScreen** - Show available slots
- ✅ **AppointmentTimeline** - Slot visualization

---

### 34. **AvailabilitySlotRequest**
**Location**: `data/models.kt`

**Description**: Request model for creating availability slots.

**Fields**: `startDate`, `endDate`, `startTime`, `endTime`, `slotDuration`, `excludeWeekends`, `excludeDates`

**Used in Screens**:
- ✅ **AddSlotsScreen** - Create slots in bulk

---

## Customization Models

### 35. **ThemedCollection**
**Location**: `data/ThemedCollection.kt`

**Description**: Themed product collection for marketing and display.

**Fields**: `id`, `name`, `description`, `imageUrl`, `isActive`, `createdAt`, `updatedAt`, `productIds`, `createdBy`, `order`, `images`, `metadata`

**Used in Screens**:
- ✅ **CustomizationScreen** - Manage themed collections
- ✅ **AddEditProductScreen** - Assign products to collections
- ✅ **PreviewHomeScreen** - Display collections on homepage
- ✅ **DashboardScreen** - Filter by collection

---

### 36. **CollectionImage**
**Location**: `data/CustomizationModels.kt`

**Description**: Image within a themed collection.

**Fields**: `url`, `isActive`, `order`

**Used in Screens**:
- ✅ **CustomizationScreen** - Collection image management
- ✅ **PreviewHomeScreen** - Display collection images

---

### 37. **CollectionMetadata**
**Location**: `data/CustomizationModels.kt`

**Description**: Metadata for collections.

**Fields**: `createdAt`, `updatedAt`, `createdBy`

**Used in Screens**:
- ✅ **CustomizationScreen** - Collection metadata

---

### 38. **CarouselItem**
**Location**: `data/CustomizationModels.kt`

**Description**: Homepage carousel item.

**Fields**: `id`, `title`, `titleActive`, `subtitle`, `subtitleActive`, `image`, `isActive`, `order`, `metadata`

**Used in Screens**:
- ✅ **CustomizationScreen** - Manage carousel items
- ✅ **PreviewHomeScreen** - Display carousel

---

### 39. **CarouselImage**
**Location**: `data/CustomizationModels.kt`

**Description**: Image for carousel item.

**Fields**: `url`, `isActive`

**Used in Screens**:
- ✅ **CustomizationScreen** - Carousel image management
- ✅ **PreviewHomeScreen** - Carousel display

---

### 40. **CarouselMetadata**
**Location**: `data/CustomizationModels.kt`

**Description**: Metadata for carousel items.

**Fields**: `createdAt`, `updatedAt`, `createdBy`

**Used in Screens**:
- ✅ **CustomizationScreen** - Carousel metadata

---

### 41. **AppConfig**
**Location**: `data/CustomizationModels.kt`

**Description**: Application display configuration.

**Fields**: `id`, `activeCollectionIds`, `activeCarouselIds`, `collectionsEnabled`, `carouselsEnabled`, `lastPublished`

**Used in Screens**:
- ✅ **CustomizationScreen** - Configure app display settings
- ✅ **PreviewHomeScreen** - Apply configuration

---

### 42. **DraftState**
**Location**: `data/CustomizationModels.kt`

**Description**: Draft state for customization changes.

**Fields**: `collections`, `carouselItems`, `appConfig`

**Used in Screens**:
- ✅ **CustomizationScreen** - Manage draft changes

---

## Transaction Models

### 43. **UnifiedTransaction**
**Location**: `data/UnifiedTransaction.kt`

**Description**: Unified model for displaying both orders and cash transactions.

**Fields**: 
- Common: `id`, `customerId`, `transactionType`, `amount`, `finalAmount`, `notes`, `createdAt`, `updatedAt`, `completedAt`, `transactionDate`, `status`, `paymentStatus`
- Order-specific: `orderId`, `items`, `subtotal`, `discountAmount`, `discountPercent`, `taxableAmount`, `gstAmount`, `totalAmount`, `isGstIncluded`, `paymentSplit`, `metalRatesReference`
- Cash-specific: `cashAmountId`, `cashTransactionType`, `createdBy`

**Used in Screens**:
- ✅ **CustomerTransactionsScreen** - Unified transaction display
- ✅ **BillingScreen** - Transaction details
- ✅ **ReceiptScreen** - Transaction receipts

---

### 44. **TransactionType** (Enum)
**Location**: `data/UnifiedTransaction.kt`

**Description**: Transaction type (ORDER, CASH)

**Used in Screens**:
- ✅ **CustomerTransactionsScreen** - Filter transaction types

---

### 45. **CashAmount**
**Location**: `data/CashAmount.kt`

**Description**: Cash transaction model for give/receive operations.

**Fields**: 
- Payment: `paymentSplit`, `paymentStatus`
- Financial: `subtotal`, `discountAmount`, `discountPercent`, `taxableAmount`, `gstAmount`, `totalAmount`, `finalAmount`, `isGstIncluded`
- Cash-specific: `cashAmountId`, `amount`, `transactionType`
- References: `items`, `metalRatesReference`
- Timestamps: `createdAt`, `updatedAt`, `completedAt`, `transactionDate`
- Info: `notes`, `status`, `createdBy`

**Used in Screens**:
- ✅ **CashAmountDialog** - Create cash transactions
- ✅ **CustomerTransactionsScreen** - Display cash transactions
- ✅ **BillingScreen** - Cash transaction bills
- ✅ **ReceiptScreen** - Cash transaction receipts

---

### 46. **CashTransactionType** (Enum)
**Location**: `data/CashAmount.kt`

**Description**: Cash transaction type (GIVE, RECEIVE)

**Used in Screens**:
- ✅ **CashAmountDialog** - Select transaction type
- ✅ **CustomerTransactionsScreen** - Filter cash transactions

---

## Other Models

### 47. **InvoiceConfig**
**Location**: `data/models.kt`

**Description**: Invoice/bill configuration and customization.

**Fields**: `id`, `companyName`, `companyAddress`, `companyPhone`, `companyEmail`, `companyGST`, `showCustomerDetails`, `showGoldRate`, `showMakingCharges`, `showPaymentBreakup`, `showItemDetails`, `showTermsAndConditions`, `termsAndConditions`, `logoUrl`, `footerText`, `isActive`, `createdAt`, `updatedAt`

**Used in Screens**:
- ✅ **SettingsScreen** - Configure invoice settings
- ✅ **ReceiptScreen** - Apply invoice configuration
- ✅ **BillingScreen** - Apply invoice configuration

---

### 48. **InvoiceField**
**Location**: `data/models.kt`

**Description**: Individual invoice field configuration.

**Fields**: `fieldName`, `displayName`, `isEnabled`, `isRequired`, `order`

**Used in Screens**:
- ✅ **SettingsScreen** - Configure invoice fields
- ✅ **ReceiptScreen** - Field visibility
- ✅ **BillingScreen** - Field visibility

---

### 49. **ExchangeGold**
**Location**: `data/models.kt`

**Description**: Gold exchange transaction model.

**Fields**: `id`, `type`, `firm`, `account`, `description`, `name`, `grossWeight`, `lessWeight`, `netWeight`, `tunch`, `fineWeight`, `laborWeight`, `ffnWeight`, `rate`, `value`, `averageRate`, `createdAt`

**Used in Screens**:
- ✅ **ExchangeGoldScreen** - Gold exchange transactions
- ✅ **PaymentScreen** - Exchange gold in payment
- ✅ **ReceiptScreen** - Exchange gold details

---

## Summary by Screen

### **DashboardScreen**
- Product, GroupedProduct, Category, MetalRate, InventoryItem, InventoryStatus, ThemedCollection

### **AddEditProductScreen**
- Product, ProductShowConfig, ProductMaterial, Category, Material, Stone, InventoryItem, InventoryStatus, ThemedCollection, MetalRate

### **ProductDetailScreen**
- Product, GroupedProduct, Category, Material, MetalRate, RecentlyViewedItem, WishlistItem

### **CartBuildingStep**
- Product, CartItem, Cart, InventoryItem, MetalRate, ProductMaterial

### **CartTable**
- CartItem, Cart, Product, MetalRate, PaymentMethod, PaymentSplit, DiscountType

### **PaymentScreen**
- CartItem, PaymentMethod, PaymentSplit, DiscountType, ExchangeGold, MetalRate, Order, OrderItem

### **PaymentSplitScreen**
- PaymentSplit, PaymentMethod, CartItem

### **ReceiptScreen**
- Order, OrderItem, CartItem, PaymentTransaction, PaymentSplit, PaymentMethod, InvoiceConfig, InvoiceField, MetalRate, ExchangeGold, UnifiedTransaction, CashAmount

### **BillingScreen**
- Order, OrderItem, UnifiedTransaction, CashAmount, InvoiceConfig, InvoiceField

### **CustomerTransactionsScreen**
- UnifiedTransaction, Order, CashAmount, PaymentMethod, PaymentSplit, PaymentStatus, OrderStatus, TransactionType, CashTransactionType

### **ProfileScreen**
- User, RecentlyViewedItem, WishlistItem, Booking

### **AppointmentScreen**
- Booking, BookingStatus, AppointmentWithUser, AvailabilitySlot, User

### **AppointmentTimeline**
- Booking, AppointmentWithUser, AvailabilitySlot, User

### **AddSlotsScreen**
- AvailabilitySlot, AvailabilitySlotRequest

### **GoldRateScreen**
- MetalRate, RateHistory, Material, Stone

### **AddMaterialDialog**
- ProductMaterial, Material, Stone, MetalRate

### **BarcodeEditScreen**
- Product, InventoryItem, InventoryStatus

### **CustomizationScreen**
- ThemedCollection, CollectionImage, CollectionMetadata, CarouselItem, CarouselImage, CarouselMetadata, AppConfig, DraftState

### **PreviewHomeScreen**
- Product, ThemedCollection, CarouselItem, AppConfig

### **CategoryManagementScreen**
- Category, CategoryType

### **SettingsScreen**
- InvoiceConfig, InvoiceField

### **ExchangeGoldScreen**
- ExchangeGold

### **CashAmountDialog**
- CashAmount, CashTransactionType, PaymentSplit, PaymentMethod

### **SplashScreen**
- (No data models - loading screen)

### **PreviewComponents**
- (UI components only)

---

## Model Usage Statistics

**Most Used Models**:
1. **Product** - Used in 15+ screens
2. **MetalRate** - Used in 10+ screens
3. **CartItem** - Used in 8+ screens
4. **User** - Used in 7+ screens
5. **Order** - Used in 6+ screens

**Screens with Most Models**:
1. **ReceiptScreen** - 12+ models
2. **BillingScreen** - 8+ models
3. **CustomerTransactionsScreen** - 8+ models
4. **PaymentScreen** - 7+ models
5. **AddEditProductScreen** - 10+ models

---

## Notes

- Some legacy models (MetalPrices, GoldRates, SilverRates, MetalRates) are being phased out in favor of the new MetalRate system
- UnifiedTransaction provides a unified view of both orders and cash transactions
- ProductShowConfig enables field-level visibility control for flexible product display
- ThemedCollection and CarouselItem models support homepage customization
- All models follow Firestore document structure for seamless database integration

---

**Last Updated**: 2024
**Project**: Gagan Jewellers Desktop Application
**Language**: Kotlin (Compose Multiplatform)

