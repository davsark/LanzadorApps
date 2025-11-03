package es.davidmarquez.lanzadorapps

import java.io.File

object Scanner {

    // (Filtro 1: Carpetas principales a ignorar - Sin cambios)
    private val directoriosIgnorados = listOf(
        "Common Files", "Intel", "NVIDIA", "NVIDIA Corporation", "drivers",
        "Microsoft.NET", "Microsoft SDKs", "Windows Defender", "Temp",
        "Redist", "vcredist", "DirectX"
    )

    // --- FILTRO 2 (MEJORADO) ---
    // Palabras clave en el *nombre del .exe* a ignorar
    private val keywordsIgnoradas = listOf(
        "unins", "setup", "update", "crash", "dbg", "report", "support",
        "install", "service", "agent", "helper", "plugin", "eula", "readme",
        // --- AÑADIDOS GRACIAS A TUS IMÁGENES ---
        "daemon", "symbolizer", "clangd", "redist", "perf", "vcredist"
    )

    // --- ¡NUEVO FILTRO 3! ---
    // Palabras clave en la *ruta completa* a ignorar.
    // Usamos \\ para "escapar" la barra \ en el texto.
    // ¡Este filtro es el más potente!
    private val pathKeywordsIgnorados = listOf(
        "\\plugins\\",   // e.g., Android Studio\plugins\
        "\\resources\\", // e.g., ...\plugins\android\resources\
        "\\lldb\\",      // e.g., ...\ndk\resources\lldb\
        "\\VC\\"         // e.g., ...\EA Desktop\VC\ (para los vc_redist)
    )

    // (Filtro 4: Tamaño mínimo - Sin cambios)
    private const val TAMANO_MINIMO_MB = 5


    fun escanearJuegos(): List<Juego> {
        return when (DetectorSO.actual) {
            DetectorSO.SistemaOperativo.WINDOWS -> escanearWindows()
            DetectorSO.SistemaOperativo.LINUX -> escanearLinux()
            DetectorSO.SistemaOperativo.OTRO -> emptyList()
        }
    }

    private fun escanearWindows(): List<Juego> {
        println("Escaneando en Windows (modo super-agresivo)...")
        val juegosEncontrados = mutableListOf<Juego>()

        // (Paso 1: Búsqueda profunda - Sin cambios)
        val directoriosBusqueda = listOf(
            File("C:\\Program Files"),
            File("C:\\Program Files (x86)")
        )
        for (directorio in directoriosBusqueda) {
            if (directorio.exists() && directorio.isDirectory) {
                buscarRecursivamente(directorio, juegosEncontrados)
            }
        }

        // (Paso 2: Apps clásicas - Sin cambios)
        println("Añadiendo aplicaciones clásicas de Windows...")
        val system32Dir = File("C:\\Windows\\System32")
        val appsClasicas = listOf(
            "calc.exe", "notepad.exe", "mspaint.exe", "cmd.exe",
            "explorer.exe", "charmap.exe", "msinfo32.exe"
        )
        if (system32Dir.exists() && system32Dir.isDirectory) {
            for (appName in appsClasicas) {
                val appFile = File(system32Dir, appName)
                if (appFile.exists() && appFile.isFile) {
                    val yaExiste = juegosEncontrados.any { it.ruta == appFile.absolutePath }
                    if (!yaExiste) {
                        juegosEncontrados.add(Juego(
                            nombre = appFile.nameWithoutExtension,
                            ruta = appFile.absolutePath,
                            icono = ""
                        ))
                    }
                }
            }
        }

        println("Escaneo de Windows finalizado. Encontrados: ${juegosEncontrados.size} juegos.")
        return juegosEncontrados.sortedBy { it.nombre } // ¡Extra! Los ordenamos alfabéticamente
    }

    /**
     * Función recursiva actualizada con el NUEVO FILTRO 3
     */
    private fun buscarRecursivamente(directorio: File, lista: MutableList<Juego>) {

        // Filtro 1: Ignorar carpetas principales (e.g., NVIDIA)
        if (directoriosIgnorados.any { dirIgnorado -> directorio.name.contains(dirIgnorado) }) {
            return
        }

        val archivos = directorio.listFiles() ?: return

        for (archivo in archivos) {
            try {
                if (archivo.isDirectory) {
                    buscarRecursivamente(archivo, lista)
                } else if (archivo.isFile && archivo.name.endsWith(".exe")) {

                    // Filtro 4: Tamaño (5MB)
                    val tamanoEnMB = archivo.length() / (1024 * 1024)
                    if (tamanoEnMB < TAMANO_MINIMO_MB) {
                        continue
                    }

                    // Filtro 2: Nombre del .exe (daemon, redist...)
                    val nombreEnMinusculas = archivo.name.lowercase()
                    if (keywordsIgnoradas.any { nombreEnMinusculas.contains(it) }) {
                        continue
                    }

                    // Filtro por ruta completa (plugins, resources, VC...)
                    val rutaEnMinusculas = archivo.absolutePath.lowercase()
                    if (pathKeywordsIgnorados.any { rutaEnMinusculas.contains(it) }) {
                        continue
                    }

                    // Si pasa TODOS los filtros, lo añadimos
                    val nuevoJuego = Juego(
                        nombre = archivo.nameWithoutExtension,
                        ruta = archivo.absolutePath,
                        icono = ""
                    )
                    lista.add(nuevoJuego)
                }
            } catch (e: Exception) {
                // Ignorar errores de acceso
            }
        }
    }

    private fun escanearLinux(): List<Juego> {
        println("Escaneo de Linux... (no implementado)")
        return emptyList()
    }
}