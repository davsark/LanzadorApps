package es.davidmarquez.lanzadorapps

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.filechooser.FileSystemView

object IconUtils {
    // Crear cache para no tener que recargar el mismo icono
    private val iconCache = mutableMapOf<String, ImageBitmap>()

    /**
     * Obtiene el icono para un archivo/aplicación
     * Maneja tanto Windows (FileSystemView) como Linux (búsqueda de iconos)
     */
    suspend fun getIconForFile(file: File): ImageBitmap? = withContext(Dispatchers.IO) {
        // 1. Comprobar si ya está el icono en cache
        val cacheKey = file.absolutePath
        iconCache[cacheKey]?.let { return@withContext it }

        try {
            val imageBitmap = when (DetectorSO.actual) {
                DetectorSO.SistemaOperativo.WINDOWS -> obtenerIconoWindows(file)
                DetectorSO.SistemaOperativo.LINUX -> obtenerIconoLinux(file)
                else -> null
            }

            // Guardar en cache si se obtuvo
            if (imageBitmap != null) {
                iconCache[cacheKey] = imageBitmap
            }

            return@withContext imageBitmap
        } catch (e: Exception) {
            println("❌ Error obteniendo icono para ${file.name}: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Obtiene el icono para Windows usando FileSystemView
     */
    private fun obtenerIconoWindows(file: File): ImageBitmap? {
        try {
            // Usar FileSystemView de Swing para pedirle el icono al SO
            val swingIcon: Icon = FileSystemView.getFileSystemView().getSystemIcon(file)

            // Crear una imagen en blanco en memoria con BufferedImage
            val bufferedImage = BufferedImage(
                swingIcon.iconWidth,
                swingIcon.iconHeight,
                BufferedImage.TYPE_INT_ARGB
            )

            // Pintar el icono de Java Swing en la imagen en blanco
            val g = bufferedImage.createGraphics()
            swingIcon.paintIcon(null, g, 0, 0)
            g.dispose()

            // Convertir la imagen Java (BufferedImage) a una de Compose (ImageBitmap)
            return bufferedImage.toComposeImageBitmap()
        } catch (e: Exception) {
            println("⚠️ Error obteniendo icono Windows: ${e.message}")
            return null
        }
    }

    /**
     * Obtiene el icono para Linux buscando en las ubicaciones estándar
     */
    private fun obtenerIconoLinux(file: File): ImageBitmap? {
        // Si file.absolutePath es en realidad un nombre de comando (como "firefox"),
        // intentamos buscar el icono correspondiente
        val nombreIcono = if (file.isAbsolute && file.exists()) {
            file.nameWithoutExtension
        } else {
            // Es un comando, usar el nombre directamente
            file.name
        }

        // Intentar encontrar el archivo de icono
        val archivoIcono = buscarIconoEnSistema(nombreIcono)

        if (archivoIcono != null && archivoIcono.exists()) {
            try {
                val bufferedImage = ImageIO.read(archivoIcono)
                if (bufferedImage != null) {
                    return bufferedImage.toComposeImageBitmap()
                }
            } catch (e: Exception) {
                println("⚠️ Error leyendo archivo de icono ${archivoIcono.path}: ${e.message}")
            }
        }

        // Si no encontramos icono específico, intentar con un icono genérico
        return obtenerIconoGenerico()
    }

    /**
     * Busca un icono en las ubicaciones estándar de Linux
     * Sigue el estándar freedesktop.org Icon Theme Specification
     */
    private fun buscarIconoEnSistema(nombreIcono: String): File? {
        // Posibles ubicaciones de temas de iconos
        val directoriosIconos = listOf(
            "/usr/share/icons/hicolor",
            "/usr/share/icons/gnome",
            "/usr/share/icons/Adwaita",
            "/usr/share/pixmaps",
            "${System.getProperty("user.home")}/.local/share/icons",
            "${System.getProperty("user.home")}/.icons"
        )

        // Tamaños comunes de iconos (de mayor a menor)
        val tamaños = listOf("256x256", "128x128", "96x96", "64x64", "48x48", "32x32", "scalable")

        // Categorías comunes
        val categorias = listOf("apps", "applications", "mimetypes", "places", "")

        // Extensiones de archivo de imagen
        val extensiones = listOf(".png", ".svg", ".xpm", ".jpg")

        // Buscar en cada directorio
        for (dirBase in directoriosIconos) {
            val dirIconos = File(dirBase)
            if (!dirIconos.exists() || !dirIconos.isDirectory) continue

            // Buscar en diferentes tamaños
            for (tamaño in tamaños) {
                for (categoria in categorias) {
                    val rutaBusqueda = if (categoria.isEmpty()) {
                        File(dirIconos, tamaño)
                    } else {
                        File(dirIconos, "$tamaño/$categoria")
                    }

                    if (!rutaBusqueda.exists()) continue

                    // Probar diferentes extensiones
                    for (extension in extensiones) {
                        val archivoIcono = File(rutaBusqueda, "$nombreIcono$extension")
                        if (archivoIcono.exists() && archivoIcono.isFile) {
                            println("✅ Icono encontrado: ${archivoIcono.path}")
                            return archivoIcono
                        }
                    }
                }
            }
        }

        // Buscar directamente en /usr/share/pixmaps (muchos iconos están ahí)
        val pixmapsDir = File("/usr/share/pixmaps")
        if (pixmapsDir.exists()) {
            for (extension in extensiones) {
                val archivoIcono = File(pixmapsDir, "$nombreIcono$extension")
                if (archivoIcono.exists() && archivoIcono.isFile) {
                    println("✅ Icono encontrado en pixmaps: ${archivoIcono.path}")
                    return archivoIcono
                }
            }
        }

        println("⚠️ No se encontró icono para: $nombreIcono")
        return null
    }

    /**
     * Devuelve un icono genérico para aplicaciones sin icono específico
     */
    private fun obtenerIconoGenerico(): ImageBitmap? {
        val iconosGenericos = listOf(
            "/usr/share/icons/hicolor/48x48/apps/application-x-executable.png",
            "/usr/share/icons/Adwaita/48x48/mimetypes/application-x-executable.png",
            "/usr/share/pixmaps/application-x-executable.png"
        )

        for (ruta in iconosGenericos) {
            val archivo = File(ruta)
            if (archivo.exists()) {
                try {
                    val bufferedImage = ImageIO.read(archivo)
                    if (bufferedImage != null) {
                        return bufferedImage.toComposeImageBitmap()
                    }
                } catch (e: Exception) {
                    // Continuar con el siguiente
                }
            }
        }

        return null
    }

    /**
     * Función auxiliar para obtener icono por nombre de icono (no archivo)
     * Útil cuando tienes el nombre del icono desde un .desktop file
     */
    suspend fun getIconByName(iconName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        // Comprobar cache
        iconCache[iconName]?.let { return@withContext it }

        val archivoIcono = buscarIconoEnSistema(iconName)
        if (archivoIcono != null && archivoIcono.exists()) {
            try {
                val bufferedImage = ImageIO.read(archivoIcono)
                if (bufferedImage != null) {
                    val imageBitmap = bufferedImage.toComposeImageBitmap()
                    iconCache[iconName] = imageBitmap
                    return@withContext imageBitmap
                }
            } catch (e: Exception) {
                println("⚠️ Error cargando icono $iconName: ${e.message}")
            }
        }

        return@withContext obtenerIconoGenerico()
    }
}