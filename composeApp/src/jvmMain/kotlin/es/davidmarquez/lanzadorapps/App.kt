package es.davidmarquez.lanzadorapps


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Window
import java.io.File
import java.io.FilenameFilter
import java.io.IOException
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width

// Define los tipos de filtro que permitimos
enum class TipoFiltro {
    TODAS,
    SISTEMA,
    USUARIO
}

// Define los tipos de ordenación
enum class TipoOrden {
    ALFABETICO_ASC,
    ALFABETICO_DESC
    // Podríamos añadir TAMAÑO_ASC/DESC si modificáramos el Scanner
}
// La data class (sin 'icono')
data class Juego(
    val nombre: String,
    val ruta: String,
    val isSystemApp: Boolean = false
)

@Composable
fun App(window: Window) { // Recibe la ventana
    MaterialTheme (
        colorScheme = darkColorScheme() // 1. Tema oscuro
    ){
        val juegosState = remember { mutableStateOf<List<Juego>>(emptyList()) }
        val estaEscaneando = remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val searchTextState = remember { mutableStateOf("") }
        val filtroState = remember { mutableStateOf(TipoFiltro.TODAS) }
        val ordenState = remember { mutableStateOf(TipoOrden.ALFABETICO_ASC) }

        val listaFiltrada = remember(
            juegosState.value,    // Si la lista base cambia
            searchTextState.value, // Si el texto cambia
            filtroState.value,     // Si el filtro cambia
            ordenState.value       // Si el orden cambia
        ) {

            // 1. Empezamos con la lista completa
            var lista = juegosState.value

            // 2. Aplicamos el FILTRO (TODAS, SISTEMA, USUARIO)
            lista = when (filtroState.value) {
                TipoFiltro.TODAS -> lista
                TipoFiltro.SISTEMA -> lista.filter { it.isSystemApp }
                TipoFiltro.USUARIO -> lista.filter { !it.isSystemApp }
            }

            // 3. Aplicamos la BÚSQUEDA (por texto)
            if (searchTextState.value.isNotBlank()) {
                lista = lista.filter { juego ->
                    juego.nombre.contains(searchTextState.value, ignoreCase = true)
                }
            }

            // 4. Aplicamos el ORDEN
            lista = when (ordenState.value) {
                TipoOrden.ALFABETICO_ASC -> lista.sortedBy { it.nombre }
                TipoOrden.ALFABETICO_DESC -> lista.sortedByDescending { it.nombre }
            }

            // 5. Devolvemos la lista final
            lista
        }

        // --- ¡ESTE ES EL ORDEN CORRECTO DEL LAYOUT! ---
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                // Leemos el valor directamente de nuestro objeto DetectorSO
                text = "Sistema Operativo detectado: ${DetectorSO.osName}",
                style = MaterialTheme.typography.labelSmall, // Un estilo discreto
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            // --- 1. PRIMERO: El Row de botones ---
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
                ) { Text("Escanear Sistema") }

                // Botón de Añadir Manualmente
                Button(
                    enabled = !estaEscaneando.value,
                    onClick = {
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
                                isSystemApp = false
                            )
                            juegosState.value = juegosState.value + nuevoJuego
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Añadir Manualmente") }
            } // --- Fin del Row de botones ---

            // --- 2. SEGUNDO: La barra de búsqueda ---
            OutlinedTextField(
                value = searchTextState.value,
                onValueChange = { searchTextState.value = it },
                label = { Text("Buscar por nombre...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                singleLine = true
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- Menú Desplegable 1: FILTRAR ---
                var expandedFiltro by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandedFiltro = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Filtrar: ${filtroState.value.name}")
                    }

                    DropdownMenu(
                        expanded = expandedFiltro,
                        onDismissRequest = { expandedFiltro = false }
                    ) {
                        DropdownMenuItem(
                            text = { // <-- Asegúrate de que tu versión de M3 usa 'text ='
                                Text(
                                    "Todas las Apps",
                                    color = if (filtroState.value == TipoFiltro.TODAS)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                filtroState.value = TipoFiltro.TODAS
                                expandedFiltro = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Apps de Sistema",
                                    color = if (filtroState.value == TipoFiltro.SISTEMA)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                filtroState.value = TipoFiltro.SISTEMA
                                expandedFiltro = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Apps de Usuario",
                                    color = if (filtroState.value == TipoFiltro.USUARIO)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                filtroState.value = TipoFiltro.USUARIO
                                expandedFiltro = false
                            }
                        )
                    }
                }

                // --- Menú Desplegable 2: ORDENAR ---
                var expandedOrden by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandedOrden = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(when (ordenState.value) {
                            TipoOrden.ALFABETICO_ASC -> "Orden: A-Z"
                            TipoOrden.ALFABETICO_DESC -> "Orden: Z-A"
                        })
                    }

                    DropdownMenu(
                        expanded = expandedOrden,
                        onDismissRequest = { expandedOrden = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Nombre (A-Z)",
                                    color = if (ordenState.value == TipoOrden.ALFABETICO_ASC)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                ordenState.value = TipoOrden.ALFABETICO_ASC
                                expandedOrden = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Nombre (Z-A)",
                                    color = if (ordenState.value == TipoOrden.ALFABETICO_DESC)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                ordenState.value = TipoOrden.ALFABETICO_DESC
                                expandedOrden = false
                            }
                        )
                    }
                }
            }
            // --- 3. TERCERO: El indicador de carga o contador ---
            if (estaEscaneando.value) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            } else {
                Text(
                    // ¡Contador corregido para usar la lista filtrada!
                    text = "Aplicaciones encontradas: ${listaFiltrada.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // --- 4. CUARTO: La lista ---
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(listaFiltrada) { juego ->
                    FilaDeJuego(juego)
                }
            }
        }
    }
}

// --- FilaDeJuego (con Card) ---
@Composable
fun FilaDeJuego(juego: Juego) {

    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, juego.ruta) {
        value = IconUtils.getIconForFile(File(juego.ruta))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap!!,
                        contentDescription = "Icono de ${juego.nombre}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos
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

            // Botones
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    try { ProcessBuilder(juego.ruta).start() }
                    catch (e: IOException) { e.printStackTrace() }
                }) { Text("Lanzar") }

                Button(onClick = {
                    try {
                        val file = File(juego.ruta)
                        if (file.parentFile == null) return@Button
                        val directory = file.parentFile
                        if (directory != null && directory.exists()) {
                            Desktop.getDesktop().open(directory)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }) { Text("Explorar ruta") }
            }
        }
    }
}