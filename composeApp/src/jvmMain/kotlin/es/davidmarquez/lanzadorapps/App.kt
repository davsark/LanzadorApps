package es.davidmarquez.lanzadorapps

// --- IMPORTACIONES PARA COROUTINES ---
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// -------------------------------------

import java.awt.Desktop
import java.io.File
import java.io.IOException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator // ¡Nuevo! Para mostrar que carga
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// --- Modelo de Datos ---
data class Juego(
    val nombre: String,
    val ruta: String,
    val icono: String
)

@Composable
fun App() {
    MaterialTheme {
        val juegosState = remember { mutableStateOf<List<Juego>>(emptyList()) }
        val estaEscaneando = remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize()) {

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
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text("Escanear Sistema")
            }

            // --- ¡BLOQUE MODIFICADO! ---
            if (estaEscaneando.value) {
                // Si está escaneando, mostramos una barra de carga
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            } else {
                // ¡NUEVO CONTADOR!
                // Cuando no está escaneando, mostramos el total.
                Text(
                    text = "Aplicaciones encontradas: ${juegosState.value.size}",
                    style = MaterialTheme.typography.bodyMedium, // Un estilo de texto
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            // --- FIN DEL BLOQUE MODIFICADO ---


            LazyColumn(modifier = Modifier.weight(1f)) {
                items(juegosState.value) { juego ->
                    FilaDeJuego(juego)
                }
            }
        }
    }
}

// La FilaDeJuego no cambia, la dejamos como estaba
@Composable
fun FilaDeJuego(juego: Juego) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                try {
                    ProcessBuilder(juego.ruta).start()
                    println("Lanzando: ${juego.nombre}")
                } catch (e: IOException) {
                    println("Error al lanzar ${juego.nombre}: ${e.message}")
                    e.printStackTrace()
                }
            }) { Text("Lanzar") }

            Button(onClick = {
                try {
                    val file = File(juego.ruta)
                    if (file.parentFile == null) {
                        println("No se puede explorar la ruta (es un comando del sistema)")
                        return@Button
                    }
                    val directory = file.parentFile
                    if (directory != null && directory.exists()) {
                        Desktop.getDesktop().open(directory)
                        println("Explorando: ${directory.absolutePath}")
                    } else {
                        println("Error: El directorio no existe o la ruta es inválida.")
                    }
                } catch (e: Exception) {
                    println("Error al explorar la ruta: ${e.message}")
                    e.printStackTrace()
                }
            }) { Text("Explorar ruta") }
        }
    }
}