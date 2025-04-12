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

                // Construct and return the gs:// URL
                val gsUrl = "gs://$bucketName/$storageFilePath"
                println("Upload successful. URL: $gsUrl")
                return@withContext gsUrl
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
    suspend fun deleteFile(gsUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!gsUrl.startsWith("gs://")) {
                return@withContext false
            }

            val gsPrefix = "gs://$bucketName/"
            if (!gsUrl.startsWith(gsPrefix)) {
                println("URL not from this bucket: $gsUrl")
                return@withContext false
            }

            val path = gsUrl.substring(gsPrefix.length)
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
}