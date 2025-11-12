package es.davidmarquez.lanzadorapps

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

object IconUtils {
    // Cache para evitar recargas
    private val iconCache = mutableMapOf<String, ImageBitmap>()

    /**
     * Obtiene el icono para un archivo/aplicación usando IconExtractor
     */
    suspend fun getIconForFile(file: File): ImageBitmap? = withContext(Dispatchers.IO) {
        val cacheKey = file.absolutePath
        iconCache[cacheKey]?.let { return@withContext it }

        try {
            val imageBitmap = when (DetectorSO.actual) {
                DetectorSO.SistemaOperativo.WINDOWS -> {
                    // Usar IconExtractor para Windows
                    val iconBytes = IconExtractor.extractIconAsBytes(file.absolutePath, size = 64)
                    if (iconBytes != null) {
                        bytesToImageBitmap(iconBytes)
                    } else {
                        null
                    }
                }
                DetectorSO.SistemaOperativo.LINUX -> {
                    // Para Linux, si es un comando simple, no hay archivo real
                    // Intentar obtener icono por el nombre del comando
                    val nombreComando = if (file.isAbsolute && file.exists()) {
                        file.nameWithoutExtension
                    } else {
                        file.name
                    }
                    
                    val iconBytes = IconExtractor.extractLinuxIcon(nombreComando)
                    if (iconBytes != null) {
                        bytesToImageBitmap(iconBytes)
                    } else {
                        null
                    }
                }
                else -> null
            }

            if (imageBitmap != null) {
                iconCache[cacheKey] = imageBitmap
            }

            return@withContext imageBitmap
        } catch (e: Exception) {
            println("❌ Error obteniendo icono para ${file.name}: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Obtiene icono por nombre (útil cuando tienes el nombre del icono desde .desktop)
     */
    suspend fun getIconByName(iconName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        iconCache[iconName]?.let { return@withContext it }

        try {
            val iconBytes = when (DetectorSO.actual) {
                DetectorSO.SistemaOperativo.LINUX -> {
                    IconExtractor.extractLinuxIcon(iconName)
                }
                else -> null
            }

            if (iconBytes != null) {
                val imageBitmap = bytesToImageBitmap(iconBytes)
                if (imageBitmap != null) {
                    iconCache[iconName] = imageBitmap
                }
                return@withContext imageBitmap
            }

            return@withContext null
        } catch (e: Exception) {
            println("⚠️ Error cargando icono $iconName: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Convierte ByteArray a ImageBitmap
     */
    private fun bytesToImageBitmap(bytes: ByteArray): ImageBitmap? {
        return try {
            val inputStream = ByteArrayInputStream(bytes)
            val bufferedImage: BufferedImage = ImageIO.read(inputStream)
            bufferedImage.toComposeImageBitmap()
        } catch (e: Exception) {
            println("⚠️ Error convirtiendo bytes a ImageBitmap: ${e.message}")
            null
        }
    }
}
