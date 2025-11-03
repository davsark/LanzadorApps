package es.davidmarquez.lanzadorapps

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.Icon
import javax.swing.filechooser.FileSystemView

object IconUtils {
    //Crear cache para no tener que recargar el mismo icono

    private val iconCache = mutableMapOf<String, ImageBitmap>()

    suspend fun getIconForFile(file : File): ImageBitmap? = withContext(Dispatchers.IO) {
        //1. comprobar si ya esta el icono en cache
        iconCache[file.absolutePath]?.let { return@withContext it }

        try {
            //2. Usar FileSystemView de Swing para pedirle el icono al SO
            val swingIcon: Icon = FileSystemView.getFileSystemView().getSystemIcon(file)

            //3. Crear una imagen en blanco en memoria con (BufferedImage)
            val bufferedImage = BufferedImage(
                swingIcon.iconWidth,
                swingIcon.iconHeight,
                BufferedImage.TYPE_INT_ARGB
            )
            //4. Pintar el icono de java Swing en la imagen en blanco
            val g = bufferedImage.createGraphics()
            swingIcon.paintIcon(null, g, 0, 0)
            g.dispose()

            //5. Convertir la imagen Java (BufferedImage)
            // a una de compose (ImageBitmap)
            val imageBitmap = bufferedImage.toComposeImageBitmap()

            //6. Guardar cache y devolver
            iconCache[file.absolutePath] = imageBitmap
            return@withContext imageBitmap
        } catch (e: Exception) {
            //Si algo falla no devolvemos el icono
            e.printStackTrace()
            return@withContext null
        }
    }
}