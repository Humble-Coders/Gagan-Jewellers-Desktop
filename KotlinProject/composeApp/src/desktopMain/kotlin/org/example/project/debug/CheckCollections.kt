package org.example.project.debug

import org.example.project.JewelryAppInitializer

fun main() {
    println("🔍 Checking Collections in Firestore...")
    println("=".repeat(80))
    
    try {
        val firestore = JewelryAppInitializer.getFirestore()
        val snapshot = firestore.collection("themed_collections").get().get()
        
        println("Found ${snapshot.documents.size} collections\n")
        
        snapshot.documents.forEach { doc ->
            val data = doc.data ?: return@forEach
            
            println("📦 Collection: ${data["name"]}")
            println("   ID: ${doc.id}")
            println("   imageUrl: '${data["imageUrl"] ?: "EMPTY"}'")
            println("   images field exists: ${data.containsKey("images")}")
            
            if (data.containsKey("images")) {
                val images = data["images"]
                println("   images type: ${images?.javaClass?.simpleName}")
                println("   images value: $images")
            }
            
            println("   All fields: ${data.keys.joinToString(", ")}")
            println()
        }
        
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}
