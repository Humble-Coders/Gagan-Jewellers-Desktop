package org.example.project.data

import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Transaction

/**
 * Extension function to safely get DocumentSnapshot from transaction.
 * In Firebase Admin SDK, transaction.get() should return DocumentSnapshot synchronously
 * but the type system shows it as ApiFuture, so we handle it at runtime.
 */
fun Transaction.getDocument(ref: DocumentReference): DocumentSnapshot {
    val result = this.get(ref)
    return when (result) {
        is DocumentSnapshot -> result
        else -> {
            // If it's wrapped in a Future, try to get the result using reflection
            try {
                val getMethod = result.javaClass.getMethod("get")
                @Suppress("UNCHECKED_CAST")
                getMethod.invoke(result) as DocumentSnapshot
            } catch (e: Exception) {
                throw IllegalStateException("Failed to get DocumentSnapshot from transaction.get(). Returned type: ${result::class.java.name}", e)
            }
        }
    }
}

