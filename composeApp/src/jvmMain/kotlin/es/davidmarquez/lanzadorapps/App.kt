package es.davidmarquez.lanzadorapps

// --- IMPORTACIONES CLAVE ---
import java.awt.FileDialog
import java.awt.Frame // ¡La que faltaba!
import java.awt.Window
import java.io.FilenameFilter
// -----------------------------
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.io.IOException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import es.davidmarquez.lanzadorapps.IconUtils

data class Juego(
    val nombre: String,
    val ruta: String,
)

@Composable
fun App(window: Window) { // Recibe la ventana
    MaterialTheme {
        val juegosState = remember { mutableStateOf<List<Juego>>(emptyList()) }
        val estaEscaneando = remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize()) {

            // --- Fila de Botones Superiores ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón de Escanear
                Button(
                    enabled = !estaEscaneando.value,
                    onClick = {
                        scope.launch {
                            estaEscaneando.value = true
                            val juegosEncontrados = withContext(Dispatchers.IO) {
                                Scanner.escanearJuegos()
                            }
                            juegosState.value = juegosEncontrados
                            estaEscaneando.value = false
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Escanear Sistema")
                }

                // Botón de Añadir Manualmente
                Button(
                    enabled = !estaEscaneando.value,
                    onClick = {
                        // --- LÓGICA DE FILEDIALOG CORREGIDA ---
                        val fileDialog = FileDialog(window as Frame, "Seleccionar aplicación (.exe)", FileDialog.LOAD).apply {
                            filenameFilter = FilenameFilter { _, name -> name.endsWith(".exe") }
                            isMultipleMode = false
                            isVisible = true
                        }

                        val directory = fileDialog.directory
                        val file = fileDialog.file

                        if (directory != null && file != null) {
                            val nuevoJuego = Juego(
                                nombre = file.removeSuffix(".exe"),
                                ruta = File(directory, file).absolutePath,
                            )
                            juegosState.value = juegosState.value + nuevoJuego
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Añadir Manualmente")
                }
            }

            // --- Indicador de Carga o Contador ---
            if (estaEscaneando.value) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            } else {
                Text(
                    text = "Aplicaciones encontradas: ${juegosState.value.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // --- Lista de Juegos ---
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(juegosState.value) { juego ->
                    FilaDeJuego(juego)
                }
            }
        }
    }
}

@Composable
fun FilaDeJuego(juego: Juego) {
    //Logica para cargar icono
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, juego.ruta){
        value = IconUtils.getIconForFile(File(juego.ruta))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Composable del Icono ---
        Box(
            modifier = Modifier
                .size(40.dp) // Tamaño fijo para el icono
                .background(Color.Gray.copy(alpha = 0.3f)) // Fondo gris de placeholder
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                // Si el icono se ha cargado, lo mostramos
                Image(
                    bitmap = iconBitmap!!,
                    contentDescription = "Icono de ${juego.nombre}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit // Ajusta el icono al Box
                )
            }
            // Si iconBitmap es null, solo se ve el Box gris (el placeholder)
        }

        Spacer(modifier = Modifier.width(16.dp)) // Espacio entre icono y texto

        // Columna de Texto
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = juego.nombre,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = juego.ruta,
                style = MaterialTheme.typography.bodySmall
            )
        }
        //Fila de botones
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                try {
                    ProcessBuilder(juego.ruta).start()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }) { Text("Lanzar") }

            Button(onClick = {
                try {
                    val file = File(juego.ruta)
                    if (file.parentFile == null) return@Button
                    val directory = file.parentFile
                    if (directory != null && directory.exists()) {
                        Desktop.getDesktop().open(directory)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }) { Text("Explorar ruta") }
        }
    }
}