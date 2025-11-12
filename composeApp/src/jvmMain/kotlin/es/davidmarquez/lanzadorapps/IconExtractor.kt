package es.davidmarquez.lanzadorapps

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView
import java.awt.Graphics2D
import java.awt.RenderingHints

/**
 * Extrae iconos de alta calidad de ejecutables de Windows (.exe)
 * y de aplicaciones de Linux.
 * 
 * Adaptado del código de tu compañero para funcionar con tu estructura.
 */
object IconExtractor {

    private val USER_HOME = System.getProperty("user.home")

    // Rutas estándar de iconos en Linux
    private val LINUX_ICON_PATHS = listOf(
        "/usr/share/icons",
        "/usr/share/pixmaps",
        "$USER_HOME/.local/share/icons",
        "$USER_HOME/.icons"
    )

    // Tamaños de iconos comunes en Linux (de mayor a menor)
    private val ICON_SIZES = listOf(256, 128, 96, 64, 48, 32)

    // ==================== WINDOWS ====================

    /**
     * Extrae el icono de mayor calidad posible de un ejecutable de Windows.
     *
     * @param exePath Ruta completa al archivo .exe
     * @param size Tamaño deseado del icono (por defecto 64)
     * @return BufferedImage con el icono de alta calidad, o null si falla
     */
    fun extractIcon(exePath: String, size: Int = 64): BufferedImage? {
        return try {
            val file = File(exePath)
            if (!file.exists() || !file.name.endsWith(".exe", ignoreCase = true)) {
                return null
            }

            // Estrategia 1: Intentar extraer icono de alta resolución
            val highResIcon = extractHighResolutionIcon(file)

            val iconImage = if (highResIcon != null && highResIcon.width >= 48) {
                highResIcon
            } else {
                // Estrategia 2: Usar FileSystemView como fallback
                val systemIcon = FileSystemView.getFileSystemView().getSystemIcon(file)
                when (systemIcon) {
                    is ImageIcon -> systemIcon.image
                    else -> iconToImage(systemIcon)
                }
            }

            // Escalar con máxima calidad al tamaño deseado
            scaleImageWithQuality(iconImage, size, size)

        } catch (e: Exception) {
            println("❌ Error extrayendo icono de $exePath: ${e.message}")
            null
        }
    }

    private fun extractHighResolutionIcon(file: File): BufferedImage? {
        return try {
            val icon = FileSystemView.getFileSystemView().getSystemIcon(file)
            val image = when (icon) {
                is ImageIcon -> icon.image
                else -> iconToImage(icon)
            }
            imageToBufferedImage(image)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extrae el icono como ByteArray para usar directamente en Compose.
     * @param exePath Ruta al ejecutable
     * @param size Tamaño objetivo (por defecto 64)
     * @return ByteArray con la imagen PNG, o null si falla
     */
    fun extractIconAsBytes(exePath: String, size: Int = 64): ByteArray? {
        val image = extractIcon(exePath, size) ?: return null

        return try {
            val outputStream = java.io.ByteArrayOutputStream()
            ImageIO.write(image, "png", outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            println("❌ Error convirtiendo icono a bytes: ${e.message}")
            null
        }
    }

    // ==================== LINUX ====================

    /**
     * Extrae el icono de una aplicación de Linux.
     * Maneja tanto rutas absolutas como nombres de iconos.
     */
    fun extractLinuxIcon(iconName: String, size: Int = 64): ByteArray? {
        // Si ya es una ruta absoluta, cargarla directamente
        if (iconName.startsWith("/")) {
            return loadIconFromPath(iconName, size)
        }

        // Buscar en las rutas estándar de iconos
        val iconFile = findLinuxIconFile(iconName)
        if (iconFile != null) {
            println("  ✅ Icono encontrado: ${iconFile.absolutePath}")
            return loadIconFromPath(iconFile.absolutePath, size)
        }

        println("  ⚠️ No se encontró icono para: $iconName")
        return null
    }

    /**
     * Busca un archivo de icono en las rutas estándar de Linux.
     * Prioriza el tamaño más grande disponible para mejor calidad.
     */
    private fun findLinuxIconFile(iconName: String): File? {
        val extensions = listOf(".png", ".svg", ".xpm", ".jpg")

        // Primero intentar encontrar el icono en el tamaño más grande
        for (size in ICON_SIZES) {
            for (basePath in LINUX_ICON_PATHS) {
                val baseDir = File(basePath)
                if (!baseDir.exists() || !baseDir.isDirectory) continue

                for (ext in extensions) {
                    // 1. Buscar en hicolor/SIZE/apps/ICON
                    val hicolorPath = File(baseDir, "hicolor/${size}x${size}/apps/$iconName$ext")
                    if (hicolorPath.exists()) return hicolorPath

                    // 2. Buscar en hicolor/SIZE/applications/ICON (variante)
                    val hicolorAppsPath = File(baseDir, "hicolor/${size}x${size}/applications/$iconName$ext")
                    if (hicolorAppsPath.exists()) return hicolorAppsPath

                    // 3. Buscar en otros temas populares
                    val popularThemes = listOf("Adwaita", "gnome", "oxygen", "breeze", "Papirus")
                    for (theme in popularThemes) {
                        val themePath = File(baseDir, "$theme/${size}x${size}/apps/$iconName$ext")
                        if (themePath.exists()) return themePath
                    }

                    // 4. Buscar en subcarpetas de tamaño
                    baseDir.listFiles()?.forEach { themeDir ->
                        if (themeDir.isDirectory) {
                            val themePath = File(themeDir, "${size}x${size}/apps/$iconName$ext")
                            if (themePath.exists()) return themePath
                        }
                    }
                }
            }
        }

        // Si no encontramos en carpetas de tamaño, buscar directamente en pixmaps
        for (basePath in LINUX_ICON_PATHS) {
            val baseDir = File(basePath)
            if (!baseDir.exists()) continue

            for (ext in extensions) {
                val pixmapPath = File(baseDir, "$iconName$ext")
                if (pixmapPath.exists()) return pixmapPath
            }
        }

        // Búsqueda recursiva limitada en pixmaps (algunos iconos están en subdirectorios)
        val pixmapsDir = File("/usr/share/pixmaps")
        if (pixmapsDir.exists()) {
            for (ext in extensions) {
                val result = findIconRecursive(pixmapsDir, iconName + ext, 2)
                if (result != null) return result
            }
        }

        return null
    }

    /**
     * Búsqueda recursiva limitada de un archivo
     */
    private fun findIconRecursive(dir: File, filename: String, maxDepth: Int): File? {
        if (maxDepth < 0 || !dir.isDirectory) return null

        val directFile = File(dir, filename)
        if (directFile.exists()) return directFile

        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                val result = findIconRecursive(child, filename, maxDepth - 1)
                if (result != null) return result
            }
        }

        return null
    }

    /**
     * Carga un icono desde una ruta y lo convierte a ByteArray PNG
     */
    private fun loadIconFromPath(path: String, size: Int = 64): ByteArray? {
        return try {
            val file = File(path)
            if (!file.exists()) {
                println("  ⚠️ Archivo no existe: $path")
                return null
            }

            val image = ImageIO.read(file)
            if (image == null) {
                println("  ⚠️ No se pudo leer la imagen: $path")
                return null
            }

            val scaledImage = scaleImageWithQuality(image, size, size)

            val outputStream = java.io.ByteArrayOutputStream()
            ImageIO.write(scaledImage, "png", outputStream)
            outputStream.toByteArray()

        } catch (e: Exception) {
            println("  ❌ Error cargando icono de $path: ${e.message}")
            null
        }
    }

    // ==================== SHARED UTILITIES ====================

    private fun iconToImage(icon: javax.swing.Icon): Image {
        val bufferedImage = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
        val g = bufferedImage.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        icon.paintIcon(null, g, 0, 0)
        g.dispose()
        return bufferedImage
    }

    private fun imageToBufferedImage(image: Image): BufferedImage {
        if (image is BufferedImage) return image
        val width = image.getWidth(null)
        val height = image.getHeight(null)
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = bufferedImage.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(image, 0, 0, null)
        g.dispose()
        return bufferedImage
    }

    /**
     * Escala una imagen con la máxima calidad posible usando interpolación bicúbica
     * y técnicas de escalado progresivo para imágenes grandes.
     */
    private fun scaleImageWithQuality(source: Image, targetWidth: Int, targetHeight: Int): BufferedImage {
        val sourceWidth = source.getWidth(null)
        val sourceHeight = source.getHeight(null)

        // Si ya tiene el tamaño objetivo, devolver sin cambios
        if (sourceWidth == targetWidth && sourceHeight == targetHeight) {
            return imageToBufferedImage(source)
        }

        val scaledImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d: Graphics2D = scaledImage.createGraphics()

        // Configurar hints de renderizado para máxima calidad
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)

        // Si la imagen es mucho más grande que el objetivo, usar escalado progresivo
        if (sourceWidth > targetWidth * 2 || sourceHeight > targetHeight * 2) {
            var currentImage = imageToBufferedImage(source)
            var currentWidth = sourceWidth
            var currentHeight = sourceHeight

            // Reducir progresivamente a la mitad hasta acercarse al tamaño objetivo
            while (currentWidth / 2 >= targetWidth || currentHeight / 2 >= targetHeight) {
                currentWidth /= 2
                currentHeight /= 2

                val tempImage = BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_ARGB)
                val tempG2d = tempImage.createGraphics()

                tempG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                tempG2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                tempG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                tempG2d.drawImage(currentImage, 0, 0, currentWidth, currentHeight, null)
                tempG2d.dispose()

                currentImage = tempImage
            }

            // Escalado final al tamaño exacto
            g2d.drawImage(currentImage, 0, 0, targetWidth, targetHeight, null)
        } else {
            // Escalado directo si la diferencia no es muy grande
            g2d.drawImage(source, 0, 0, targetWidth, targetHeight, null)
        }

        g2d.dispose()
        return scaledImage
    }
}
