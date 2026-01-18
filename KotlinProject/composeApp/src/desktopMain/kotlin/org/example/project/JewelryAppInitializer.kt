package org.example.project

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import org.example.project.data.CustomerRepository
import org.example.project.data.FirestoreCustomerRepository
import org.example.project.data.FirestorePaymentRepository
import org.example.project.data.FirestoreProductRepository
import org.example.project.data.PaymentRepository
import org.example.project.data.ProductRepository
import org.example.project.data.GoldRateRepository
import org.example.project.data.FirestoreGoldRateRepository
import org.example.project.data.MetalRatesManager
import org.example.project.data.MetalRateRepository
import org.example.project.data.FirestoreMetalRateRepository
import org.example.project.data.StonesRepository
import org.example.project.data.FirestoreStonesRepository
import org.example.project.data.InventoryRepository
import org.example.project.data.FirestoreInventoryRepository
import org.example.project.data.AvailabilityRepository
import org.example.project.data.BookingRepository
import org.example.project.data.FirestoreAvailabilityRepository
import org.example.project.data.FirestoreBookingRepository
import org.example.project.data.OrderRepository
import org.example.project.data.FirestoreOrderRepository
import org.example.project.utils.ImageLoader
import org.example.project.utils.StorageService
import org.example.project.viewModels.CartViewModel
import org.example.project.viewModels.CustomerViewModel
import org.example.project.viewModels.PaymentViewModel
import org.example.project.viewModels.ProductsViewModel
import org.example.project.viewModels.GoldRateViewModel
import org.example.project.viewModels.MetalRateViewModel
import org.example.project.viewModels.AppointmentViewModel
import java.io.FileInputStream
import java.nio.file.Path

/**
 * Initializer for the Jewelry App.
 *
 * This singleton handles Firebase initialization and provides access to
 * repositories, view models, and other app-wide dependencies.
 */
object JewelryAppInitializer {
    private var initialized = false
    private lateinit var firebaseApp: FirebaseApp
    private lateinit var firestore: com.google.cloud.firestore.Firestore
    private lateinit var storage: Storage
    private lateinit var bucketName: String
    private lateinit var storageService: StorageService

    private var repository: ProductRepository = FirestoreProductRepository
    private var viewModel: ProductsViewModel? = null
    private var imageLoader: ImageLoader? = null
    private var customerRepository: CustomerRepository? = null
    private var customerViewModel: CustomerViewModel? = null
    private var cartViewModel: CartViewModel? = null
    private var paymentRepository: PaymentRepository? = null
    private var paymentViewModel: PaymentViewModel? = null
    private var goldRateRepository: GoldRateRepository? = null
    private var goldRateViewModel: GoldRateViewModel? = null
    private var metalRateRepository: MetalRateRepository = FirestoreMetalRateRepository
    private var metalRateViewModel: MetalRateViewModel? = null
    private var bookingRepository: BookingRepository? = null
    private var appointmentViewModel: AppointmentViewModel? = null
    private var availabilityRepository: AvailabilityRepository? = null
    private var inventoryRepository: InventoryRepository? = null
    private var orderRepository: OrderRepository? = null


    /**
     * Initialize the app with Firebase credentials.
     *
     * @param credentialsPath Path to the Firebase service account JSON file
     * @throws IllegalArgumentException If the credentials file doesn't exist
     * @throws IllegalStateException If initialization fails
     */
    fun initialize(credentialsPath: String) {
        if (initialized) return

        try {
            // Validate the credentials file
            val credentialsFile = Path.of(credentialsPath).toFile()
            if (!credentialsFile.exists()) {
                throw IllegalArgumentException("Firebase credentials file not found at: $credentialsPath")
            }

            // Load credentials and initialize Firebase
            val credentials = GoogleCredentials.fromStream(FileInputStream(credentialsFile))

            // Get project ID from the credentials file for storage setup
            val projectId = extractProjectId(credentialsFile.readText())

            // Set the bucket name based on the project ID
            bucketName = "jewellery-app-f6302.firebasestorage.app"


            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build()

            firebaseApp = FirebaseApp.initializeApp(options)
            firestore = FirestoreClient.getFirestore(firebaseApp)

            // Initialize Storage with the same project ID
            storage = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build()
                .service

            // Initialize StorageService
            storageService = StorageService(storage, bucketName)

            // Initialize ProductRepository as object with Firestore listeners
            println("🔧 JEWELRY APP INITIALIZER: Initializing ProductRepository object")
            println("   - This will create the SINGLE Firestore listeners")
            println("   - All ViewModels will share this same repository instance")
            FirestoreProductRepository.initialize(firestore, storage)
            repository = FirestoreProductRepository
            println("✅ JEWELRY APP INITIALIZER: ProductRepository initialized")
            println("   - Repository instance: ${FirestoreProductRepository.hashCode()}")
            println("   - Listeners should now be active")
            
            // Initialize StonesRepository as object with Firestore listener
            FirestoreStonesRepository.initialize(firestore)
            val stonesRepository: StonesRepository = FirestoreStonesRepository
            // Initialize InventoryRepository as object
            FirestoreInventoryRepository.initialize(firestore)
            inventoryRepository = FirestoreInventoryRepository
            viewModel = ProductsViewModel(repository, stonesRepository, inventoryRepository)
            imageLoader = ImageLoader(repository)
            
            // Initialize CustomerRepository as object with Firestore listener
            println("🔧 JEWELRY APP INITIALIZER: Initializing CustomerRepository object")
            println("   - This will create the SINGLE Firestore listener")
            println("   - All ViewModels will share this same repository instance")
            FirestoreCustomerRepository.initialize(firestore)
            customerRepository = FirestoreCustomerRepository
            println("✅ JEWELRY APP INITIALIZER: CustomerRepository initialized")
            println("   - Repository instance: ${FirestoreCustomerRepository.hashCode()}")
            println("   - Listener should now be active")
            
            customerViewModel = CustomerViewModel()
            println("✅ JEWELRY APP INITIALIZER: CustomerViewModel created")
            println("   - ViewModel instance: ${customerViewModel.hashCode()}")
            println("   - Using shared StateFlow from repository")
            cartViewModel = CartViewModel(repository, imageLoader!!)
            paymentRepository = FirestorePaymentRepository(firestore)
            orderRepository = FirestoreOrderRepository(firestore)
            paymentViewModel = PaymentViewModel(paymentRepository!!, orderRepository!!)
            goldRateRepository = FirestoreGoldRateRepository(firestore)
            goldRateViewModel = GoldRateViewModel(goldRateRepository!!)
            // Initialize MetalRateRepository as object with Firestore listeners
            println("🔧 JEWELRY APP INITIALIZER: Initializing MetalRateRepository object")
            println("   - This will create the SINGLE Firestore listeners")
            println("   - All ViewModels will share this same repository instance")
            FirestoreMetalRateRepository.initialize(firestore)
            metalRateRepository = FirestoreMetalRateRepository
            println("✅ JEWELRY APP INITIALIZER: MetalRateRepository initialized")
            println("   - Repository instance: ${FirestoreMetalRateRepository.hashCode()}")
            println("   - Listeners should now be active")
            MetalRatesManager.initialize(metalRateRepository)
            metalRateViewModel = MetalRateViewModel(metalRateRepository)
            bookingRepository = FirestoreBookingRepository(firestore)
            appointmentViewModel = AppointmentViewModel(bookingRepository!!)
            availabilityRepository = FirestoreAvailabilityRepository(firestore)

            initialized = true
            println("Firebase initialized successfully with project ID: $projectId")
            println("Storage bucket: $bucketName")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize Firebase: ${e.message}", e)
        }
    }
    fun getCustomerViewModel(): CustomerViewModel {
        checkInitialized()
        return customerViewModel!!
    }

    fun getCartViewModel(): CartViewModel {
        checkInitialized()
        return cartViewModel!!
    }

    fun getPaymentViewModel(): PaymentViewModel {
        checkInitialized()
        return paymentViewModel!!
    }

    fun getGoldRateViewModel(): GoldRateViewModel {
        checkInitialized()
        return goldRateViewModel!!
    }

    fun getMetalRateViewModel(): MetalRateViewModel {
        checkInitialized()
        return metalRateViewModel!!
    }

    fun getProductRepository(): ProductRepository {
        checkInitialized()
        return repository
    }

    fun getInventoryRepository(): InventoryRepository {
        checkInitialized()
        return inventoryRepository!!
    }

    fun getAppointmentViewModel(): AppointmentViewModel {
        checkInitialized()
        return appointmentViewModel!!
    }

    fun getAvailabilityRepository(): AvailabilityRepository {
        checkInitialized()
        return availabilityRepository!!
    }
    
    fun getCustomerRepository(): CustomerRepository {
        checkInitialized()
        return customerRepository!!
    }

    /**
     * Extract the project ID from the credentials JSON.
     * This is a simple parser and assumes a standard format of the credentials file.
     */
    private fun extractProjectId(jsonContent: String): String {
        val projectIdPattern = "\"project_id\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val matchResult = projectIdPattern.find(jsonContent)
        return matchResult?.groupValues?.get(1)
            ?: throw IllegalArgumentException("Could not extract project_id from credentials file")
    }

    /**
     * Get the products view model instance.
     * @throws IllegalStateException If called before initialization
     */
    fun getViewModel(): ProductsViewModel {
        checkInitialized()
        return viewModel!!
    }

    /**
     * Get the image loader instance.
     * @throws IllegalStateException If called before initialization
     */
    fun getImageLoader(): ImageLoader {
        checkInitialized()
        return imageLoader!!
    }

    /**
     * Get the storage service instance.
     * @throws IllegalStateException If called before initialization
     */
    fun getStorageService(): StorageService {
        checkInitialized()
        return storageService
    }

    fun getFirestore(): Firestore{
        checkInitialized()
        return firestore
    }

    /**
     * Check if the app has been initialized.
     * @throws IllegalStateException If the app hasn't been initialized
     */
    private fun checkInitialized() {
        if (!initialized) {
            throw IllegalStateException("JewelryAppInitializer not initialized. Call initialize() first.")
        }
    }

}