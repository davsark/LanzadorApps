package es.davidmarquez.lanzadorapps // Asegúrate de que este paquete coincide con el tuyo

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

// --- PASO 1: Definir el "Modelo" de Datos ---
// Esta data class representa la información de cada juego o app
data class Juego(
    val nombre: String,
    val ruta: String,
    val icono: String // Por ahora, podemos ignorar esto o usar una ruta a una imagen de prueba
)

// --- PASO 2: Crear la Lista "Fake" ---
// Una lista de ejemplo para construir la UI sin tener la lógica de escaneo
val listaDeJuegosFalsos = listOf(
    Juego("Calculadora", "C:\\Windows\\System32\\calc.exe", "icono_default.png"),
    Juego("Bloc de notas", "C:\\Windows\\System32\\notepad.exe", "icono_default.png"),
    Juego("Paint", "C:\\Windows\\System32\\mspaint.exe", "icono_default.png")
)


// --- PASO 3: Construir la UI ---
// Este es tu Composable principal
@Composable
@Preview // @Preview te permite ver una vista previa en IntelliJ
fun App() {
    MaterialTheme {
        // 'juegosState' es la variable que "recuerda" la lista de juegos.
        // Usamos mutableStateOf para que Compose sepa que si esta lista cambia,
        // tiene que "redibujar" la pantalla.
        // Inicialmente, le damos la lista falsa.
        val juegosState = remember { mutableStateOf(listaDeJuegosFalsos) }

        Column(modifier = Modifier.fillMaxSize()) {

            // Botón de ejemplo (aún no hace nada)
            Button(onClick = { /* TODO: Aquí irá la lógica de escaneo */ }) {
                Text("Escanear Sistema")
            }

            // 'LazyColumn' es la forma eficiente de mostrar listas en Compose
            // Le damos .weight(1f) para que ocupe todo el espacio sobrante
            LazyColumn(modifier = Modifier.weight(1f)) {

                // Creamos un item por cada 'juego' en nuestro 'juegosState'
                items(juegosState.value) { juego ->
                    // Llamamos a otro Composable para dibujar cada fila
                    FilaDeJuego(juego)
                }
            }
        }
    }
}

// --- PASO 4: Diseñar la Fila (Versión simple) ---
// Este será el Composable que define cómo se ve CADA fila de la lista
@Composable
fun FilaDeJuego(juego: Juego) {
    // Por ahora, solo mostramos el nombre
    // TODO: Hacer esto más bonito con una Row, un Icono y un Botón
    Text(juego.nombre)
}