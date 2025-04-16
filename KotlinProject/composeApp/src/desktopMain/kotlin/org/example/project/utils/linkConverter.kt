package org.example.project.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A component that provides functionality to convert between GS and HTTPS links
 * in the Firestore database for product images.
 * @param firestore The Firestore instance from com.google.cloud.firestore.Firestore
 */
@Composable
fun StorageLinkConverter(firestore: Firestore) {
    val coroutineScope = rememberCoroutineScope()
    var docLimit by remember { mutableStateOf("10") }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var processedCount by remember { mutableStateOf(0) }
    var totalToProcess by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Firebase Storage Link Converter",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Document limit input
        OutlinedTextField(
            value = docLimit,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    docLimit = newValue
                }
            },
            label = { Text("Number of documents to process") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // Process feedback
        if (isProcessing && totalToProcess > 0) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = processedCount.toFloat() / totalToProcess,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Processing $processedCount of $totalToProcess documents...",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Status message
        statusMessage?.let { message ->
            Text(
                text = message,
                color = if (isError) Color.Red else Color.Green,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // GS to HTTPS conversion button
            Button(
                onClick = {
                    coroutineScope.launch {
                        convertLinks(
                            firestore = firestore,
                            toHttps = true,
                            limit = docLimit.toIntOrNull() ?: 10,
                            onProcessingChange = { processing -> isProcessing = processing },
                            onStatusUpdate = { message, error ->
                                statusMessage = message
                                isError = error
                            },
                            onProgressUpdate = { processed, total ->
                                processedCount = processed
                                totalToProcess = total
                            }
                        )
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Convert to HTTPS")
                Spacer(modifier = Modifier.width(8.dp))
                Text("GS → HTTPS")
            }

            // HTTPS to GS conversion button
            Button(
                onClick = {
                    coroutineScope.launch {
                        convertLinks(
                            firestore = firestore,
                            toHttps = false,
                            limit = docLimit.toIntOrNull() ?: 10,
                            onProcessingChange = { processing -> isProcessing = processing },
                            onStatusUpdate = { message, error ->
                                statusMessage = message
                                isError = error
                            },
                            onProgressUpdate = { processed, total ->
                                processedCount = processed
                                totalToProcess = total
                            }
                        )
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2196F3))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Convert to GS")
                Spacer(modifier = Modifier.width(8.dp))
                Text("HTTPS → GS")
            }
        }
    }
}

/**
 * Converts links between GS and HTTPS formats in the Firestore database.
 * @param firestore The Firestore instance from com.google.cloud.firestore.Firestore
 */
/**
 * Converts links between GS and HTTPS formats in the Firestore database.
 * @param firestore The Firestore instance from com.google.cloud.firestore.Firestore
 */
private suspend fun convertLinks(
    firestore: Firestore,
    toHttps: Boolean,
    limit: Int,
    onProcessingChange: (Boolean) -> Unit,
    onStatusUpdate: (String, Boolean) -> Unit,
    onProgressUpdate: (Int, Int) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        onProcessingChange(true)
        onStatusUpdate("Starting conversion process...", false)

        val carouselCollection = firestore.collection("themed_collections")

        // Get the documents with a limit - using get().get() instead of await() for Admin SDK
        val querySnapshot = try {
            carouselCollection.limit(limit.toLong().toInt()).get().get()
        } catch (e: Exception) {
            onStatusUpdate("Error fetching documents: ${e.message}", true)
            onProcessingChange(false)
            return@withContext
        }

        val totalDocs = querySnapshot.documents.size
        onStatusUpdate("Found $totalDocs documents to process", false)
        onProgressUpdate(0, totalDocs)

        var processedCount = 0
        var successCount = 0
        var errorCount = 0

        // Process each document
        for (document in querySnapshot.documents) {
            try {
                val imageUrl = document.getString("imageUrl")
                if (imageUrl != null) {
                    val updatedUrl = convertUrl(imageUrl, toHttps)

                    // Only update if there's a change
                    if (imageUrl != updatedUrl) {
                        // Update the document with the new image URL
                        try {
                            firestore.runTransaction { transaction ->
                                transaction.update(document.reference, "imageUrl", updatedUrl)
                                null // Transaction function must return null or a value
                            }.get()
                            successCount++
                        } catch (e: Exception) {
                            throw Exception("Failed to update document ${document.id}: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error processing document ${document.id}: ${e.message}")
                errorCount++
            } finally {
                processedCount++
                onProgressUpdate(processedCount, totalDocs)
            }
        }

        val resultMessage = if (errorCount > 0) {
            "Completed with issues: $successCount succeeded, $errorCount failed"
        } else {
            "Successfully converted links in $successCount documents"
        }

        onStatusUpdate(resultMessage, errorCount > 0)
        onProcessingChange(false)
    } catch (e: Exception) {
        onStatusUpdate("Error: ${e.message}", true)
        onProcessingChange(false)
    }
}

/**
 * Converts a URL between GS and HTTPS formats
 */
private fun convertUrl(url: String, toHttps: Boolean): String {
    return if (toHttps) {
        // GS to HTTPS conversion
        if (url.startsWith("gs://")) {
            val gsPrefix = "gs://"
            val bucketAndPath = url.substring(gsPrefix.length)
            val bucketName = bucketAndPath.substringBefore("/")
            val path = bucketAndPath.substringAfter("/")

            // Encode the path for URL (replace / with %2F)
            val encodedPath = path.replace("/", "%2F")

            "https://firebasestorage.googleapis.com/v0/b/$bucketName/o/$encodedPath?alt=media"
        } else {
            // Already HTTPS or unknown format
            url
        }
    } else {
        // HTTPS to GS conversion
        if (url.startsWith("https://firebasestorage.googleapis.com/v0/b/")) {
            val pattern = "https://firebasestorage.googleapis.com/v0/b/(.+?)/o/(.+?)\\?alt=media".toRegex()
            val matchResult = pattern.find(url)

            if (matchResult != null && matchResult.groupValues.size >= 3) {
                val bucketName = matchResult.groupValues[1]
                val encodedPath = matchResult.groupValues[2]

                // Decode the path (replace %2F with /)
                val path = encodedPath.replace("%2F", "/")

                "gs://$bucketName/$path"
            } else {
                // Can't parse the URL
                url
            }
        } else {
            // Already GS or unknown format
            url
        }
    }
}