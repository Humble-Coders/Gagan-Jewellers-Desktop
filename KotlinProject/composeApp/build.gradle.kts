import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            //implementation("com.alialbaali.kamel:kamel-image:0.7.3")
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation("com.google.firebase:firebase-admin:9.2.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
            implementation("com.google.cloud:google-cloud-storage:2.29.1")
            //implementation("io.coil-kt:coil:3.0.0-alpha06")
            implementation("org.apache.pdfbox:pdfbox:3.0.1")
            implementation("org.xhtmlrenderer:flying-saucer-pdf:9.1.22")
            implementation("org.xhtmlrenderer:flying-saucer-core:9.1.22")
            implementation("com.itextpdf:itext7-core:7.2.5")
            implementation("org.slf4j:slf4j-simple:1.7.36")


            // extended icons
            implementation(compose.materialIconsExtended)

        }
    }
}


compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.example.project"
            packageVersion = "1.0.0"
        }
    }
}
