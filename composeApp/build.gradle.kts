import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        // Esta es tu clase principal, está correcta
        mainClass = "es.davidmarquez.lanzadorapps.MainKt"

        nativeDistributions {
            // Los formatos que quieres generar
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            // ID interno de la app (el tuyo estaba bien)
            packageName = "es.davidmarquez.lanzadorapps"

            // Versión (es mejor usar 'version' que 'packageVersion')
            version = "1.0.0"

            // --- Información adicional para el instalador ---
            description = "Un lanzador de apps simple"
            vendor = "David Marquez"

            // --- Configuración Específica de Windows ---
            windows {
                menuGroup = "Lanzador de Apps"
                // Apunta a tu icono .ico
                iconFile.set(project.file("src/jvmMain/composeResources/drawable/lanzador_icono.ico"))
            }

            // --- Configuración Específica de Linux ---
            linux {
                // Nombre del paquete en Linux (distinto al 'packageName' general)
                packageName = "lanzador-de-apps"
                // Apunta a tu icono .png
                iconFile.set(project.file("src/jvmMain/composeResources/drawable/lanzador_icono.png"))
            }
        }
    }
}