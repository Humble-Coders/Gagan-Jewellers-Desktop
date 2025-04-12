package org.example.project.utils

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * Storage Service that uses the correct Firebase Storage bucket format.
 */
class StorageService(private val storage: Storage, val bucketName: String) {

    /**
     * Uploads a file to Firebase Storage.
     */
    suspend fun uploadFile(
        filePath: Path,
        directoryPath: String = "products",
        progressFlow: MutableStateFlow<Float>? = null
    ): String? = withContext(Dispatchers.IO) {
        println("Starting upload process for file: $filePath")
        println("Target directory: $directoryPath")
        println("Target bucket: $bucketName")

        try {
            val file = filePath.toFile()
            if (!file.exists()) {
                println("File does not exist: $filePath")
                return@withContext null
            }

            // Update progress to 10%
            progressFlow?.emit(0.1f)

            // Generate a unique storage path
            val uniqueId = UUID.randomUUID().toString()
            val fileName = filePath.fileName.toString()
            val extension = fileName.substringAfterLast('.', "")
            val storageFilePath = "$directoryPath/$uniqueId.$extension"

            println("Generated storage path: $storageFilePath")

            // Read the file bytes
            val fileBytes = Files.readAllBytes(filePath)
            println("Read ${fileBytes.size} bytes from file")

            // Update progress to 30%
            progressFlow?.emit(0.3f)

            // Create blob info
            val blobId = BlobId.of(bucketName, storageFilePath)
            val blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(getContentType(extension))
                .build()

            println("Created blob info with content type: ${getContentType(extension)}")

            // Update progress to 50%
            progressFlow?.emit(0.5f)

            // Upload the file
            try {
                println("Starting upload to Firebase Storage...")
                val blob = storage.create(blobInfo, fileBytes)
                println("Upload complete. Storage blob size: ${blob?.size ?: "unknown"} bytes")

                // Update progress to 100%
                progressFlow?.emit(1.0f)

                // Construct and return the HTTPS URL instead of gs:// URL
                val httpsUrl = getHttpsUrl(bucketName, storageFilePath)
                println("Upload successful. URL: $httpsUrl")
                return@withContext httpsUrl
            } catch (e: Exception) {
                println("Error during upload: ${e.javaClass.name}: ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
        } catch (e: Exception) {
            println("Exception in uploadFile: ${e.javaClass.name}: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Deletes a file from Firebase Storage.
     */
    suspend fun deleteFile(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if it's a gs:// or https:// URL and extract the path
            val path = when {
                url.startsWith("gs://") -> {
                    val gsPrefix = "gs://$bucketName/"
                    if (!url.startsWith(gsPrefix)) {
                        println("URL not from this bucket: $url")
                        return@withContext false
                    }
                    url.substring(gsPrefix.length)
                }
                url.startsWith("https://") -> {
                    val httpsPrefix = "https://firebasestorage.googleapis.com/v0/b/$bucketName/o/"
                    if (!url.startsWith(httpsPrefix)) {
                        println("URL not from this bucket: $url")
                        return@withContext false
                    }

                    // Extract the path and decode it from the URL format
                    url.substring(httpsPrefix.length)
                        .substringBefore("?")
                        .replace("%2F", "/")
                }
                else -> {
                    println("Invalid URL format: $url")
                    return@withContext false
                }
            }

            println("Deleting file at path: $path")

            val blobId = BlobId.of(bucketName, path)
            return@withContext storage.delete(blobId)
        } catch (e: Exception) {
            println("Error deleting file: ${e.message}")
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Gets the appropriate content type based on file extension.
     */
    private fun getContentType(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            else -> "application/octet-stream"
        }
    }

    /**
     * Converts a storage path to an HTTPS URL for Firebase Storage.
     */
    private fun getHttpsUrl(bucketName: String, path: String): String {
        // Firebase Storage HTTPS URL format
        // https://firebasestorage.googleapis.com/v0/b/BUCKET_NAME/o/PATH?alt=media

        // Encode the path for URL (replace / with %2F)
        val encodedPath = path.replace("/", "%2F")

        return "https://firebasestorage.googleapis.com/v0/b/$bucketName/o/$encodedPath?alt=media"
    }
}