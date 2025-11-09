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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke

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
        colorScheme = darkColorScheme(
            // Colores principales
            primary = Color(0xFF6200EE),
            onPrimary = Color.White,  // <-- AÑADIDO: texto blanco sobre botones morados

            secondary = Color(0xFF03DAC6),
            onSecondary = Color.Black,  // <-- AÑADIDO: texto negro sobre cyan

            tertiary = Color(0xFF3700B3),
            onTertiary = Color.White,  // <-- AÑADIDO: texto blanco sobre morado oscuro

            // Fondos
            background = Color(0xFF121212),
            onBackground = Color.White,  // <-- AÑADIDO: texto blanco sobre fondo negro

            surface = Color(0xFF1E1E1E),
            onSurface = Color.White,  // <-- AÑADIDO: texto blanco sobre superficie gris

            // Contenedores (usados por badges, chips, etc.)
            primaryContainer = Color(0xFF3700B3),
            onPrimaryContainer = Color(0xFFE1DDFF),

            secondaryContainer = Color(0xFF005353),
            onSecondaryContainer = Color(0xFFA6EEED),

            tertiaryContainer = Color(0xFF4A148C),
            onTertiaryContainer = Color(0xFFEADDFF),

            // Estados de error
            error = Color(0xFFCF6679),
            onError = Color.Black
        )
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
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // --- ENCABEZADO ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🚀 Lanzador de Apps",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sistema: ${DetectorSO.osName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- 1. NUEVO LAYOUT: Botón Escanear (Principal) ---
            OutlinedButton(
                enabled = !estaEscaneando.value,
                onClick = {
                    scope.launch {
                        estaEscaneando.value = true
                        try {
                            val juegosEncontrados = withContext(Dispatchers.IO) {
                                Scanner.escanearJuegos()
                            }
                            juegosState.value = juegosEncontrados
                            println("✅ Escaneo completado: ${juegosEncontrados.size} apps encontradas")
                        } catch (e: Exception) {
                            println("❌ Error durante el escaneo: ${e.message}")
                            e.printStackTrace()
                        } finally {
                            estaEscaneando.value = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Text(
                    "🔍 Escanear Sistema",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- 2. NUEVO LAYOUT: Fila de Búsqueda y Filtros ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically // <-- Clave para alinear todo
            ) {

                // --- Barra de Búsqueda (con peso) ---
                OutlinedTextField(
                    value = searchTextState.value,
                    onValueChange = { searchTextState.value = it },
                    label = { Text("🔎 Buscar...") }, // <-- Texto acortado
                    modifier = Modifier.weight(1f), // <-- Ocupa el espacio sobrante
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        cursorColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.secondary
                    )
                )

                // --- Menú Desplegable 1: FILTRAR ---
                var expandedFiltro by remember { mutableStateOf(false) }
                Box { // <-- SIN .weight()
                    OutlinedButton(
                        onClick = { expandedFiltro = true },
                        modifier = Modifier.height(56.dp), // <-- Altura 56.dp
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(when (filtroState.value) {
                            TipoFiltro.TODAS -> "📱 Todas"
                            TipoFiltro.SISTEMA -> "⚙️ Sistema"
                            TipoFiltro.USUARIO -> "👤 Usuario"
                        })
                    }

                    DropdownMenu(
                        expanded = expandedFiltro,
                        onDismissRequest = { expandedFiltro = false }
                    ) {
                        // ... (El código de tus DropdownMenuItem va aquí) ...
                        // (Asegúrate de que el código del menú desplegable esté aquí dentro)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Todas las Apps",
                                    color = if (filtroState.value == TipoFiltro.TODAS)
                                        Color.White
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
                                        Color.White
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
                                        Color.White                                    else
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
                Box { // <-- SIN .weight()
                    OutlinedButton(
                        onClick = { expandedOrden = true },
                        modifier = Modifier.height(56.dp), // <-- Altura 56.dp
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(when (ordenState.value) {
                            TipoOrden.ALFABETICO_ASC -> "🔤 A-Z"
                            TipoOrden.ALFABETICO_DESC -> "🔤 Z-A"
                        })
                    }

                    DropdownMenu(
                        expanded = expandedOrden,
                        onDismissRequest = { expandedOrden = false }
                    ) {
                        // ... (El código de tus DropdownMenuItem va aquí) ...
                        // (Asegúrate de que el código del menú desplegable esté aquí dentro)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Nombre (A-Z)",
                                    color = if (ordenState.value == TipoOrden.ALFABETICO_ASC)
                                        Color.White
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
                                        Color.White
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
            } // --- Fin del Row de Búsqueda y Filtros ---

            Spacer(modifier = Modifier.height(12.dp))

            // --- 3. NUEVO LAYOUT: Botón Añadir App ---
            OutlinedButton(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Text("➕ Añadir App",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- 3. TERCERO: El indicador de carga o contador ---
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            ) {
            if (estaEscaneando.value) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Escaneando aplicaciones...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White

                    )
                }
            } else {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 ${listaFiltrada.size} aplicaciones encontradas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            }

            Spacer(modifier = Modifier.height(8.dp))
        // --- 4. CUARTO: La lista ---
            LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            items(listaFiltrada) { juego ->
                FilaDeJuego(juego)
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
      }
    }
}

// --- FilaDeJuego (con Card mejorada) ---
@Composable
fun FilaDeJuego(juego: Juego) {

    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, juego.ruta) {
        value = IconUtils.getIconForFile(File(juego.ruta))
    }

    Card(
        modifier = Modifier.fillMaxWidth(), // <-- Se quita el padding de aquí
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), // <-- Más elevación
        colors = CardDefaults.cardColors( // <-- Color de fondo explícito
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium // <-- Bordes redondeados
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono con mejor diseño
            Surface( // <-- Se cambia Box por Surface
                modifier = Modifier.size(48.dp), // <-- Más grande (era 40dp)
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap!!,
                            contentDescription = "Icono de ${juego.nombre}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // <-- AÑADIDO: Fallback si no hay icono
                        Text(
                            text = "📦",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos mejorados
            Column(modifier = Modifier.weight(1f)) {
                // <-- AÑADIDO: Row para poner la etiqueta "Sistema" al lado
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = juego.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface // <-- Color explícito
                    )
                    // <-- AÑADIDO: Lógica de la etiqueta "Sistema"
                    if (juego.isSystemApp) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Sistema",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp)) // <-- Espacio añadido
                Text(
                    text = juego.ruta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), // <-- Color más suave
                    maxLines = 1, // <-- Para que no ocupe varias líneas
                    overflow = TextOverflow.Ellipsis // <-- Añadido por si la ruta es muy larga
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Botones mejorados
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        try { ProcessBuilder(juego.ruta).start() }
                        catch (e: IOException) { e.printStackTrace() }
                    },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)

                ) {
                    Text("▶️ Lanzar") // <-- Emoji añadido
                }

                OutlinedButton(onClick = { // <-- CAMBIO: Button por OutlinedButton
                    try {
                        val file = File(juego.ruta)
                        if (file.parentFile == null) return@OutlinedButton
                        val directory = file.parentFile
                        if (directory != null && directory.exists()) {
                            Desktop.getDesktop().open(directory)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                    Text("📁 Ubicación") // <-- CAMBIO: Texto por icono
                }
            }
        }
    }
}