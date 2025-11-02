package es.davidmarquez.lanzadorapps // Asegúrate de que este paquete coincide con el tuyo

// --- IMPORTACIONES NUEVAS ---
import java.awt.Desktop // Para abrir el explorador de archivos
import java.io.File      // Para manejar la ruta del archivo
import java.io.IOException // Para el manejo de errores
// ------------------------------

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button // Material 3
import androidx.compose.material3.MaterialTheme // Material 3
import androidx.compose.material3.Text // Material 3
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


// --- PASO 1: Definir el "Modelo" de Datos ---
data class Juego(
    val nombre: String,
    val ruta: String,
    val icono: String
)

// --- PASO 2: Crear la Lista "Fake" ---
val listaDeJuegosFalsos = listOf(
    // Ponemos rutas reales de Windows para que la prueba funcione
    Juego("Calculadora", "calc.exe", "icono_default.png"),
    Juego("Bloc de notas", "notepad.exe", "icono_default.png"),
    Juego("Paint", "mspaint.exe", "icono_default.png")
)


// --- PASO 3: Construir la UI ---
@Composable
fun App() {
    MaterialTheme {
        val juegosState = remember { mutableStateOf(listaDeJuegosFalsos) }

        Column(modifier = Modifier.fillMaxSize()) {

            Button(
                onClick = { /* TODO: Aquí irá la lógica de escaneo */ },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Text("Escanear Sistema")
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(juegosState.value) { juego ->
                    FilaDeJuego(juego)
                }
            }
        }
    }
}

// --- PASO 4: Diseñar la Fila (Versión MEJORADA) ---
@Composable
fun FilaDeJuego(juego: Juego) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Columna para el nombre y la ruta
        Column(
            modifier = Modifier.weight(1f) // Ocupa el espacio sobrante
        ) {
            Text(
                text = juego.nombre,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = juego.ruta,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // --- NUEVO BLOQUE DE BOTONES ---
        // Envolvemos los botones en su propio Row
        // para que se agrupen al final.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre botones
        ) {
            // --- Botón de Lanzar ---
            Button(onClick = {
                try {
                    ProcessBuilder(juego.ruta).start()
                    println("Lanzando: ${juego.nombre}")
                } catch (e: IOException) {
                    println("Error al lanzar ${juego.nombre}: ${e.message}")
                    e.printStackTrace()
                }
            }) {
                Text("Lanzar")
            }

            // --- ¡NUEVO BOTÓN! ---
            Button(onClick = {
                try {
                    // Obtenemos el directorio padre del archivo
                    val file = File(juego.ruta)
                    // Si la ruta es solo "calc.exe", no tiene padre, así que no hacemos nada
                    if (file.parentFile == null) {
                        println("No se puede explorar la ruta (es un comando del sistema)")
                        return@Button
                    }
                    val directory = file.parentFile

                    // Usamos Desktop.open() que es la forma
                    // correcta y multiplataforma de abrir un directorio
                    if (directory != null && directory.exists()) {
                        Desktop.getDesktop().open(directory)
                        println("Explorando: ${directory.absolutePath}")
                    } else {
                        println("Error: El directorio no existe o la ruta es inválida.")
                    }

                } catch (e: Exception) {
                    // Capturamos cualquier error (IO, SecurityException, etc.)
                    println("Error al explorar la ruta: ${e.message}")
                    e.printStackTrace()
                }
            }) {
                Text("Explorar ruta")
            }
        }
    }
}