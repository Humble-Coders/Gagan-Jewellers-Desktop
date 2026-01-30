package org.example.project.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import org.example.project.JewelryAppInitializer
import org.example.project.data.StoreInfoRepository
import org.example.project.data.StoreInfo
import org.example.project.data.MainStore
import org.example.project.data.BankInfo
import org.example.project.data.StoreHours
import org.example.project.data.DayHours
import org.example.project.data.Category
import org.example.project.data.ThemedCollection
import org.example.project.data.FirestoreThemedCollectionRepository
import org.example.project.utils.ImageLoader
import org.example.project.viewModels.ProductsViewModel
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URL
import java.nio.file.Paths

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    productsViewModel: ProductsViewModel,
    onStoreInfoSaved: () -> Unit = {}
) {
    val repository = StoreInfoRepository
    val imageLoader = JewelryAppInitializer.getImageLoader()
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var storeInfo by remember { mutableStateOf<StoreInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            storeInfo = repository.getStoreInfo()
        } catch (e: Exception) {
            errorMessage = "Error loading store info: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        storeInfo?.let { info ->
            SettingsScreenContent(
                initialStoreInfo = info,
                imageLoader = imageLoader,
                productsViewModel = productsViewModel,
                onSave = { updatedInfo ->
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        successMessage = null
                        try {
                            val success = repository.saveStoreInfo(updatedInfo)
                            if (success) {
                                successMessage = "Settings saved successfully!"
                                storeInfo = updatedInfo
                                onStoreInfoSaved() // Notify that store info was saved
                            } else {
                                errorMessage = "Failed to save settings"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Error saving settings: ${e.message}"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                onCancel = onNavigateBack,
                isSaving = isSaving,
                errorMessage = errorMessage,
                successMessage = successMessage
            )
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Failed to load store info")
            }
        }
    }
}

@Composable
private fun SettingsScreenContent(
    initialStoreInfo: StoreInfo,
    imageLoader: ImageLoader,
    productsViewModel: ProductsViewModel,
    onSave: (StoreInfo) -> Unit,
    onCancel: () -> Unit,
    isSaving: Boolean,
    errorMessage: String?,
    successMessage: String?
) {
    val categories = productsViewModel.categories.value
    val products = productsViewModel.products.value
    val scope = rememberCoroutineScope()
    
    // Collections state
    var collections by remember { mutableStateOf<List<ThemedCollection>>(emptyList()) }
    val collectionRepository = remember { FirestoreThemedCollectionRepository(JewelryAppInitializer.getFirestore()) }
    
    // Load collections
    LaunchedEffect(Unit) {
        collections = collectionRepository.getAllCollections()
        println("🔍 Loaded ${collections.size} collections from Firestore:")
        collections.forEach { collection ->
            println("   📦 Collection: ${collection.name}")
            println("      - ID: ${collection.id}")
            println("      - imageUrl: '${collection.imageUrl}'")
            println("      - images list: ${collection.images.size} items")
            collection.images.forEachIndexed { i, img ->
                println("         [$i] url='${img.url}', isActive=${img.isActive}, order=${img.order}")
            }
            println("      - isActive: ${collection.isActive}")
            println("      - productIds: ${collection.productIds.size} products")
        }
    }
    var companyName by remember { mutableStateOf(initialStoreInfo.mainStore.name) }
    var establishedYear by remember { mutableStateOf(initialStoreInfo.mainStore.establishedYear) }
    var gstin by remember { mutableStateOf(initialStoreInfo.mainStore.gstIn) }
    var email by remember { mutableStateOf(initialStoreInfo.mainStore.email) }
    var phonePrimary by remember { mutableStateOf(initialStoreInfo.mainStore.phone_primary) }
    var phoneSecondary by remember { mutableStateOf(initialStoreInfo.mainStore.phone_secondary) }
    var whatsappNumber by remember { mutableStateOf(initialStoreInfo.mainStore.whatsappNumber) }
    var whatsappMessage by remember { mutableStateOf(initialStoreInfo.mainStore.whatsappMessage) }

    var fullAddress by remember { mutableStateOf(initialStoreInfo.mainStore.address) }
    var latitude by remember { mutableStateOf(initialStoreInfo.mainStore.latitude) }
    var longitude by remember { mutableStateOf(initialStoreInfo.mainStore.longitude) }

    var storeImages by remember { mutableStateOf(initialStoreInfo.storeImages) }
    var logoImages by remember { mutableStateOf(initialStoreInfo.mainStore.logo_images) }

    var accountHolder by remember { mutableStateOf(initialStoreInfo.bankInfo.account_holder) }
    var accountNumber by remember { mutableStateOf(initialStoreInfo.bankInfo.AccountNumber) }
    var accountType by remember { mutableStateOf(initialStoreInfo.bankInfo.Acc_type) }
    var ifscCode by remember { mutableStateOf(initialStoreInfo.bankInfo.IFSC_Code) }
    var branch by remember { mutableStateOf(initialStoreInfo.bankInfo.Branch) }
    var pan by remember { mutableStateOf(initialStoreInfo.bankInfo.pan_no) }

    var mondayClosed by remember { mutableStateOf(initialStoreInfo.storeHours.monday.isClosed) }
    var mondayOpen by remember { mutableStateOf(initialStoreInfo.storeHours.monday.openTime) }
    var mondayClose by remember { mutableStateOf(initialStoreInfo.storeHours.monday.closeTime) }

    var tuesdayClosed by remember { mutableStateOf(initialStoreInfo.storeHours.tuesday.isClosed) }
    var tuesdayOpen by remember { mutableStateOf(initialStoreInfo.storeHours.tuesday.openTime) }
    var tuesdayClose by remember { mutableStateOf(initialStoreInfo.storeHours.tuesday.closeTime) }

    var wednesdayClosed by remember { mutableStateOf(initialStoreInfo.storeHours.wednesday.isClosed) }
    var wednesdayOpen by remember { mutableStateOf(initialStoreInfo.storeHours.wednesday.openTime) }
    var wednesdayClose by remember { mutableStateOf(initialStoreInfo.storeHours.wednesday.closeTime) }

    var thursdayClosed by remember { mutableStateOf(initialStoreInfo.storeHours.thursday.isClosed) }
    var thursdayOpen by remember { mutableStateOf(initialStoreInfo.storeHours.thursday.openTime) }
    var thursdayClose by remember { mutableStateOf(initialStoreInfo.storeHours.thursday.closeTime) }

    var fridayClosed by remember { mutableStateOf(initialStoreInfo.storeHours.friday.isClosed) }
    var fridayOpen by remember { mutableStateOf(initialStoreInfo.storeHours.friday.openTime) }
    var fridayClose by remember { mutableStateOf(initialStoreInfo.storeHours.friday.closeTime) }

    var saturdayClosed by remember { mutableStateOf(initialStoreInfo.storeHours.saturday.isClosed) }
    var saturdayOpen by remember { mutableStateOf(initialStoreInfo.storeHours.saturday.openTime) }
    var saturdayClose by remember { mutableStateOf(initialStoreInfo.storeHours.saturday.closeTime) }

    var sundayClosed by remember { mutableStateOf(initialStoreInfo.storeHours.sunday.isClosed) }
    var sundayOpen by remember { mutableStateOf(initialStoreInfo.storeHours.sunday.openTime) }
    var sundayClose by remember { mutableStateOf(initialStoreInfo.storeHours.sunday.closeTime) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage store details, categories, and collections",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        errorMessage?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFFFEBEE),
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = it,
                    color = Color(0xFFC62828),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        successMessage?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFE8F5E9),
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = it,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        ExpandableSection(
            title = "Store Information",
            subtitle = "Business details and contact information",
            icon = Icons.Filled.Home,
            iconColor = Color(0xFFD4AF37)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = "Store Name"
                    )
                    CompactTextField(
                        value = establishedYear,
                        onValueChange = { establishedYear = it },
                        label = "Established Year",
                        keyboardType = KeyboardType.Number
                    )
                    CompactTextField(
                        value = gstin,
                        onValueChange = { gstin = it },
                        label = "GSTIN"
                    )
                    CompactTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        keyboardType = KeyboardType.Email
                    )
                    CompactTextField(
                        value = phonePrimary,
                        onValueChange = { phonePrimary = it },
                        label = "Primary Phone Number",
                        keyboardType = KeyboardType.Phone
                    )
                    CompactTextField(
                        value = phoneSecondary,
                        onValueChange = { phoneSecondary = it },
                        label = "Secondary Phone Number",
                        keyboardType = KeyboardType.Phone
                    )
                    CompactTextField(
                        value = whatsappNumber,
                        onValueChange = { whatsappNumber = it },
                        label = "WhatsApp Number",
                        keyboardType = KeyboardType.Phone
                    )
                    CompactTextField(
                        value = whatsappMessage,
                        onValueChange = { whatsappMessage = it },
                        label = "Default WhatsApp Message",
                        singleLine = false,
                        minLines = 2
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
//                    Text(
//                        text = "Address & Location",
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color(0xFF1A1A1A),
//                        modifier = Modifier.padding(bottom = 4.dp)
//                    )
                    CompactTextField(
                        value = fullAddress,
                        onValueChange = { fullAddress = it },
                        label = "Full Address",
                        singleLine = false,
                        minLines = 2
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactTextField(
                            value = latitude,
                            onValueChange = { latitude = it },
                            label = "Latitude",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        CompactTextField(
                            value = longitude,
                            onValueChange = { longitude = it },
                            label = "Longitude",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Store Images",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    ProductImageManager(
                        existingImages = storeImages,
                        onImagesChanged = { updatedImages: List<String> -> storeImages = updatedImages },
                        imageLoader = imageLoader,
                        productId = "store"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Logo Images",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    ProductImageManager(
                        existingImages = logoImages,
                        onImagesChanged = { updatedImages: List<String> -> logoImages = updatedImages },
                        imageLoader = imageLoader,
                        productId = "store_logos"
                    )
                }
            }
        }

        ExpandableSection(
            title = "Store Hours",
            subtitle = "Opening and closing times",
            icon = Icons.Filled.AccessTime,
            iconColor = Color(0xFFD4AF37)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Apply to All button - shows when any timing is entered
                val hasAnyTiming = listOf(
                    mondayOpen, mondayClose,
                    tuesdayOpen, tuesdayClose,
                    wednesdayOpen, wednesdayClose,
                    thursdayOpen, thursdayClose,
                    fridayOpen, fridayClose,
                    saturdayOpen, saturdayClose,
                    sundayOpen, sundayClose
                ).any { it.isNotEmpty() }

                if (hasAnyTiming) {
                    var selectedDay by remember { mutableStateOf("") }

                    // Determine which day has timing to use as source
                    val sourceTiming = when {
                        mondayOpen.isNotEmpty() || mondayClose.isNotEmpty() -> {
                            selectedDay = "Monday"
                            Triple(mondayOpen, mondayClose, mondayClosed)
                        }
                        tuesdayOpen.isNotEmpty() || tuesdayClose.isNotEmpty() -> {
                            selectedDay = "Tuesday"
                            Triple(tuesdayOpen, tuesdayClose, tuesdayClosed)
                        }
                        wednesdayOpen.isNotEmpty() || wednesdayClose.isNotEmpty() -> {
                            selectedDay = "Wednesday"
                            Triple(wednesdayOpen, wednesdayClose, wednesdayClosed)
                        }
                        thursdayOpen.isNotEmpty() || thursdayClose.isNotEmpty() -> {
                            selectedDay = "Thursday"
                            Triple(thursdayOpen, thursdayClose, thursdayClosed)
                        }
                        fridayOpen.isNotEmpty() || fridayClose.isNotEmpty() -> {
                            selectedDay = "Friday"
                            Triple(fridayOpen, fridayClose, fridayClosed)
                        }
                        saturdayOpen.isNotEmpty() || saturdayClose.isNotEmpty() -> {
                            selectedDay = "Saturday"
                            Triple(saturdayOpen, saturdayClose, saturdayClosed)
                        }
                        sundayOpen.isNotEmpty() || sundayClose.isNotEmpty() -> {
                            selectedDay = "Sunday"
                            Triple(sundayOpen, sundayClose, sundayClosed)
                        }
                        else -> Triple("", "", false)
                    }

                    OutlinedButton(
                        onClick = {
                            val (open, close, closed) = sourceTiming
                            mondayOpen = open
                            mondayClose = close
                            mondayClosed = closed
                            tuesdayOpen = open
                            tuesdayClose = close
                            tuesdayClosed = closed
                            wednesdayOpen = open
                            wednesdayClose = close
                            wednesdayClosed = closed
                            thursdayOpen = open
                            thursdayClose = close
                            thursdayClosed = closed
                            fridayOpen = open
                            fridayClose = close
                            fridayClosed = closed
                            saturdayOpen = open
                            saturdayClose = close
                            saturdayClosed = closed
                            sundayOpen = open
                            sundayClose = close
                            sundayClosed = closed
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD4AF37)
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply these hours to all days", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                DayHoursRow("Monday", mondayClosed, mondayOpen, mondayClose,
                    onClosedChange = { mondayClosed = it },
                    onOpenChange = { mondayOpen = it },
                    onCloseChange = { mondayClose = it }
                )

                DayHoursRow("Tuesday", tuesdayClosed, tuesdayOpen, tuesdayClose,
                    onClosedChange = { tuesdayClosed = it },
                    onOpenChange = { tuesdayOpen = it },
                    onCloseChange = { tuesdayClose = it }
                )

                DayHoursRow("Wednesday", wednesdayClosed, wednesdayOpen, wednesdayClose,
                    onClosedChange = { wednesdayClosed = it },
                    onOpenChange = { wednesdayOpen = it },
                    onCloseChange = { wednesdayClose = it }
                )

                DayHoursRow("Thursday", thursdayClosed, thursdayOpen, thursdayClose,
                    onClosedChange = { thursdayClosed = it },
                    onOpenChange = { thursdayOpen = it },
                    onCloseChange = { thursdayClose = it }
                )

                DayHoursRow("Friday", fridayClosed, fridayOpen, fridayClose,
                    onClosedChange = { fridayClosed = it },
                    onOpenChange = { fridayOpen = it },
                    onCloseChange = { fridayClose = it }
                )

                DayHoursRow("Saturday", saturdayClosed, saturdayOpen, saturdayClose,
                    onClosedChange = { saturdayClosed = it },
                    onOpenChange = { saturdayOpen = it },
                    onCloseChange = { saturdayClose = it }
                )

                DayHoursRow("Sunday", sundayClosed, sundayOpen, sundayClose,
                    onClosedChange = { sundayClosed = it },
                    onOpenChange = { sundayOpen = it },
                    onCloseChange = { sundayClose = it }
                )
            }
        }

        ExpandableSection(
            title = "Bank Details",
            subtitle = "Account and payment information",
            icon = Icons.Filled.AccountBalance,
            iconColor = Color(0xFFD4AF37)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactTextField(
                    value = accountHolder,
                    onValueChange = { accountHolder = it },
                    label = "Account Holder Name",
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = "Account Number",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    CompactTextField(
                        value = accountType,
                        onValueChange = { accountType = it },
                        label = "Account Type",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactTextField(
                        value = ifscCode,
                        onValueChange = { ifscCode = it },
                        label = "IFSC Code",
                        modifier = Modifier.weight(1f)
                    )
                    CompactTextField(
                        value = pan,
                        onValueChange = { pan = it },
                        label = "PAN Number",
                        modifier = Modifier.weight(1f)
                    )
                }
                CompactTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = "Branch Name",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        ExpandableSection(
            title = "Categories",
            subtitle = "Manage product categories",
            icon = Icons.Filled.Category,
            iconColor = Color(0xFFD4AF37)
        ) {
            CategoriesSection(
                categories = categories,
                products = products,
                productsViewModel = productsViewModel,
                scope = scope
            )
        }

        ExpandableSection(
            title = "Collections",
            subtitle = "Manage special collections",
            icon = Icons.Filled.Collections,
            iconColor = Color(0xFFD4AF37)
        ) {
            CollectionsSection(
                collections = collections,
                products = products,
                collectionRepository = collectionRepository,
                scope = scope,
                onCollectionsChanged = { newCollections ->
                    collections = newCollections
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.padding(end = 12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            Button(
                onClick = {
                    val updatedInfo = StoreInfo(
                        mainStore = MainStore(
                            name = companyName,
                            address = fullAddress,
                            phone_primary = phonePrimary,
                            phone_secondary = phoneSecondary,
                            email = email,
                            gstIn = gstin,
                            certification = initialStoreInfo.mainStore.certification,
                            stateCode = initialStoreInfo.mainStore.stateCode,
                            stateName = initialStoreInfo.mainStore.stateName,
                            whatsappNumber = whatsappNumber,
                            whatsappMessage = whatsappMessage,
                            latitude = latitude,
                            longitude = longitude,
                            establishedYear = establishedYear,
                            logo_images = logoImages
                        ),
                        bankInfo = BankInfo(
                            account_holder = accountHolder,
                            AccountNumber = accountNumber,
                            Acc_type = accountType,
                            IFSC_Code = ifscCode,
                            Branch = branch,
                            pan_no = pan
                        ),
                        storeHours = StoreHours(
                            monday = DayHours(mondayOpen, mondayClose, mondayClosed),
                            tuesday = DayHours(tuesdayOpen, tuesdayClose, tuesdayClosed),
                            wednesday = DayHours(wednesdayOpen, wednesdayClose, wednesdayClosed),
                            thursday = DayHours(thursdayOpen, thursdayClose, thursdayClosed),
                            friday = DayHours(fridayOpen, fridayClose, fridayClosed),
                            saturday = DayHours(saturdayOpen, saturdayClose, saturdayClosed),
                            sunday = DayHours(sundayOpen, sundayClose, sundayClosed)
                        ),
                        storeImages = storeImages
                    )
                    onSave(updatedInfo)
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFD4AF37),
                    contentColor = Color.White
                ),
                enabled = !isSaving,
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.elevation(4.dp)
            ) {
                Text(
                    if (isSaving) "Saving..." else "Save Changes",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = Color(0xFFD4AF37),
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            subtitle,
                            fontSize = 13.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                Icon(
                    if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                if (placeholder.isNotEmpty()) {
                    Text(placeholder, color = Color(0xFF999999), fontSize = 12.sp)
                }
            },
//            leadingIcon = {
//                Spacer(modifier = Modifier.width(4.dp))
//            },
            modifier = if (singleLine) {
                Modifier.fillMaxWidth().heightIn(min = 42.dp)
            } else {
                Modifier.fillMaxWidth()
            },
            singleLine = singleLine,
            minLines = if (!singleLine) minLines else 1,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = Color(0xFFF5F5F5),
                textColor = Color(0xFF333333),
                focusedBorderColor = Color(0xFFD4AF37),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = Color(0xFFD4AF37)
            ),
            shape = RoundedCornerShape(6.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp
            )
        )
    }
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                if (placeholder.isNotEmpty()) {
                    Text(placeholder, color = Color(0xFF999999))
                }
            },
            leadingIcon = {
                Spacer(modifier = Modifier.width(4.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = Color.White,
                textColor = Color(0xFF1A1A1A),
                focusedBorderColor = Color(0xFFD4AF37),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = Color(0xFFD4AF37)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun DayHoursRow(
    dayName: String,
    isClosed: Boolean,
    openTime: String,
    closeTime: String,
    onClosedChange: (Boolean) -> Unit,
    onOpenChange: (String) -> Unit,
    onCloseChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = dayName,
            modifier = Modifier.width(80.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        TimePickerField(
            value = openTime,
            onValueChange = onOpenChange,
            placeholder = "10:00",
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "-",
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        TimePickerField(
            value = closeTime,
            onValueChange = onCloseChange,
            placeholder = "20:00",
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Close",
                fontSize = 12.sp,
                color = if (isClosed) Color(0xFF1A1A1A) else Color(0xFF999999),
                fontWeight = if (isClosed) FontWeight.SemiBold else FontWeight.Normal
            )
            Switch(
                checked = isClosed,
                onCheckedChange = onClosedChange,
                modifier = Modifier.height(24.dp)
            )
            Text(
                text = "Open",
                fontSize = 12.sp,
                color = if (!isClosed) Color(0xFF1A1A1A) else Color(0xFF999999),
                fontWeight = if (!isClosed) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var timeValue by remember { mutableStateOf(value) }
    var amPm by remember {
        mutableStateOf(
            if (value.contains("AM", ignoreCase = true)) "AM"
            else if (value.contains("PM", ignoreCase = true)) "PM"
            else ""
        )
    }

    LaunchedEffect(value) {
        timeValue = value.replace(Regex("\\s*(AM|PM|am|pm)\\s*"), "").trim()
        amPm = when {
            value.contains("AM", ignoreCase = true) -> "AM"
            value.contains("PM", ignoreCase = true) -> "PM"
            else -> ""
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = timeValue,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() || it == ':' }
                if (filtered.length <= 5) {
                    timeValue = filtered
                    onValueChange(if (amPm.isNotEmpty()) "$filtered $amPm" else filtered)
                }
            },
            placeholder = {
                Text(placeholder, color = Color(0xFF999999), fontSize = 12.sp)
            },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 38.dp)
                .clickable { showDialog = true },
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = Color(0xFFF5F5F5),
                textColor = Color(0xFF333333),
                focusedBorderColor = Color(0xFFD4AF37),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                cursorColor = Color(0xFFD4AF37)
            ),
            shape = RoundedCornerShape(6.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
            trailingIcon = {
                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Filled.AccessTime,
                        contentDescription = "Pick time",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )

        Box(
            modifier = Modifier
                .width(60.dp)
                .heightIn(min = 38.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                .clickable {
                    val newAmPm = if (amPm == "AM") "PM" else "AM"
                    amPm = newAmPm
                    onValueChange(if (timeValue.isNotEmpty()) "$timeValue $newAmPm" else "")
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (amPm.isEmpty()) "--" else amPm,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (amPm.isEmpty()) Color(0xFF999999) else Color(0xFF333333)
            )
        }
    }

    if (showDialog) {
        TimePickerDialog(
            initialTime = timeValue,
            initialAmPm = amPm.ifEmpty { "AM" },
            onDismiss = { showDialog = false },
            onConfirm = { time, selectedAmPm ->
                timeValue = time
                amPm = selectedAmPm
                onValueChange("$time $selectedAmPm")
                showDialog = false
            }
        )
    }
}

@Composable
private fun TimePickerDialog(
    initialTime: String,
    initialAmPm: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var hour by remember {
        mutableStateOf(
            initialTime.split(":").getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 10
        )
    }
    var minute by remember {
        mutableStateOf(
            initialTime.split(":").getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        )
    }
    var amPm by remember { mutableStateOf(initialAmPm) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Time", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
        },

        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour Picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { hour = if (hour == 12) 1 else hour + 1 }) {
                            Icon(Icons.Filled.KeyboardArrowUp, "Increase hour")
                        }
                        Text(
                            text = hour.toString().padStart(2, '0'),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { hour = if (hour == 1) 12 else hour - 1 }) {
                            Icon(Icons.Filled.KeyboardArrowDown, "Decrease hour")
                        }
                    }

                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minute Picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { minute = (minute + 15) % 60 }) {
                            Icon(Icons.Filled.KeyboardArrowUp, "Increase minute")
                        }
                        Text(
                            text = minute.toString().padStart(2, '0'),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { minute = if (minute == 0) 45 else minute - 15 }) {
                            Icon(Icons.Filled.KeyboardArrowDown, "Decrease minute")
                        }
                    }
                }

                // AM/PM Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { amPm = "AM" },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (amPm == "AM") Color(0xFFD4AF37) else Color(0xFFF5F5F5),
                            contentColor = if (amPm == "AM") Color.White else Color(0xFF666666)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("AM", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { amPm = "PM" },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (amPm == "PM") Color(0xFFD4AF37) else Color(0xFFF5F5F5),
                            contentColor = if (amPm == "PM") Color.White else Color(0xFF666666)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("PM", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val timeStr = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                    onConfirm(timeStr, amPm)
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFD4AF37)
                )
            ) {
                Text("OK", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF666666))
            }
        }
    )
}

@Composable
private fun CompactTimeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = Color(0xFF999999), fontSize = 12.sp)
        },
        leadingIcon = {
            Spacer(modifier = Modifier.width(4.dp))
        },
        modifier = modifier.heightIn(min = 38.dp),
        singleLine = true,
        enabled = enabled,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = Color(0xFFF5F5F5),
            textColor = Color(0xFF333333),
            focusedBorderColor = Color(0xFFD4AF37),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            cursorColor = Color(0xFFD4AF37),
            disabledTextColor = Color(0xFF999999),
            disabledBorderColor = Color(0xFFE0E0E0)
        ),
        shape = RoundedCornerShape(6.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
    )
}

@Composable
private fun CategoriesSection(
    categories: List<Category>,
    products: List<org.example.project.data.Product>,
    productsViewModel: ProductsViewModel,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }
    val productCountByCategory = remember(products) {
        products.groupingBy { it.categoryId }.eachCount()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add Category Button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFD4AF37),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Category")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Category", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = Color(0xFFBBBBBB),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "No categories yet",
                        color = Color(0xFF999999),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Categories Grid - 2 per row
            val activeCategories = categories.filter { it.isActive }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeCategories.chunked(2).forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCategories.forEach { category ->
                            CategoryItem(
                                category = category,
                                productCount = productCountByCategory[category.id] ?: 0,
                                onEdit = { editingCategory = it },
                                onDelete = { deletingCategory = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Add spacer if odd number of categories in last row
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onSave = { name, description, imageUrl ->
                val newCategory = Category(
                    name = name,
                    description = description,
                    imageUrl = imageUrl,
                    categoryType = "JEWELRY",
                    hasGenderVariants = false,
                    order = categories.size,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                productsViewModel.addCompleteCategory(newCategory) { categoryId ->
                    println("✅ Category added: $categoryId")
                }
                showAddDialog = false
            },
            onCancel = { showAddDialog = false }
        )
    }

    // Edit Category Dialog
    editingCategory?.let { category ->
        AddCategoryDialog(
            initialCategory = category,
            onSave = { name, description, imageUrl ->
                val updatedCategory = category.copy(
                    name = name,
                    description = description,
                    imageUrl = imageUrl
                )
                productsViewModel.updateCategory(updatedCategory) { success ->
                    if (success) {
                        println("✅ Category updated: $name")
                    } else {
                        println("❌ Failed to update category")
                    }
                }
                editingCategory = null
            },
            onCancel = { editingCategory = null }
        )
    }

    // Delete Confirmation Dialog
    deletingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Delete Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete \"${category.name}\"?")
                    val productCount = productCountByCategory[category.id] ?: 0
                    if (productCount > 0) {
                        Text(
                            "⚠️ This category has $productCount product(s). They will not be deleted but will need to be recategorized.",
                            color = Color(0xFFFF6B00),
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        productsViewModel.deleteCategory(category.id) { success ->
                            if (success) {
                                println("✅ Category deleted: ${category.name}")
                            } else {
                                println("❌ Failed to delete category")
                            }
                        }
                        deletingCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Red,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    productCount: Int,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(category.imageUrl) {
        if (category.imageUrl.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                try {
                    val imageBytes = URL(category.imageUrl).openStream().readBytes()
                    imageBitmap = decodeAndResizeCategoryImage(imageBytes, maxSize = 100)
                } catch (e: Exception) {
                    println("Error loading category image: ${e.message}")
                }
            }
        }
    }

    Card(
        modifier = modifier
            .height(80.dp),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color(0xFFFAFAFA),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Image
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = category.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = Color(0xFFBBBBBB),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Category Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = category.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (category.description.isNotEmpty()) {
                    Text(
                        text = category.description,
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Product Count Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFD4AF37),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = productCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Edit Button
            IconButton(
                onClick = { onEdit(category) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Category",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete Button
            IconButton(
                onClick = { onDelete(category) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Category",
                    tint = Color(0xFFCC0000),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

