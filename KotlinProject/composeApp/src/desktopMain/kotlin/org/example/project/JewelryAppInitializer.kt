package org.example.project

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import org.example.project.data.CartRepository
import org.example.project.data.CustomerRepository
import org.example.project.data.FirestoreCartRepository
import org.example.project.data.FirestoreCustomerRepository
import org.example.project.data.FirestorePaymentRepository
import org.example.project.data.FirestoreProductRepository
import org.example.project.data.PaymentRepository
import org.example.project.data.ProductRepository
import org.example.project.utils.ImageLoader
import org.example.project.utils.StorageService
import org.example.project.viewModels.CartViewModel
import org.example.project.viewModels.CustomerViewModel
import org.example.project.viewModels.PaymentViewModel
import org.example.project.viewModels.ProductsViewModel
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

    private var repository: ProductRepository? = null
    private var viewModel: ProductsViewModel? = null
    private var imageLoader: ImageLoader? = null
    private var customerRepository: CustomerRepository? = null
    private var customerViewModel: CustomerViewModel? = null
    private var cartRepository: CartRepository? = null
    private var cartViewModel: CartViewModel? = null
    private var paymentRepository: PaymentRepository? = null
    private var paymentViewModel: PaymentViewModel? = null


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

            // Create dependencies
            repository = FirestoreProductRepository(firestore, storage)
            viewModel = ProductsViewModel(repository!!)
            imageLoader = ImageLoader(repository!!)
            customerRepository = FirestoreCustomerRepository(firestore)
            customerViewModel = CustomerViewModel(customerRepository!!)
            cartRepository = FirestoreCartRepository(firestore)
            cartViewModel = CartViewModel(repository!!, cartRepository!!, imageLoader!!)
            paymentRepository = FirestorePaymentRepository(firestore)
            paymentViewModel = PaymentViewModel(paymentRepository!!)


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