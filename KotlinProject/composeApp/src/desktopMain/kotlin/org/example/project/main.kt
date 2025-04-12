package org.example.project

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.project.navigation.JewelryApp
import java.io.File
import java.nio.file.Paths

fun main() {
    // Define the path to your Firebase service account key file

        // Define the path to your Firebase service account key file
        val currentDir = System.getProperty("user.dir")

        // The file is directly in the current directory, not in a nested composeApp directory
        val credentialsPath = Paths.get(currentDir, "firebase-credentials.json").toString()

        // Check if the credentials file exists
        if (!File(credentialsPath).exists()) {
            println("Error: Firebase credentials file not found at $credentialsPath")
            println("Please place your firebase-credentials.json file in the project directory.")
            return
        }

        try {
            // Initialize Firebase with the path to credentials
            JewelryAppInitializer.initialize(credentialsPath)

            // Create and show the application window
            application {
                Window(
                    onCloseRequest = ::exitApplication,
                    title = "Jewelry Inventory",
                    state = rememberWindowState(width = 1280.dp, height = 800.dp)
                ) {
                    // Get the ViewModel from the initializer
                    val viewModel = JewelryAppInitializer.getViewModel()

                    // Render the main app UI
                    JewelryApp(viewModel)
                }
            }
        } catch (e: Exception) {
            println("Error starting application: ${e.message}")
            e.printStackTrace()
        }
    }





