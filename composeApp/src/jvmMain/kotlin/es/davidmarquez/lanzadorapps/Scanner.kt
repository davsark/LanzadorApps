package es.davidmarquez.lanzadorapps

import java.io.File

object Scanner {

    // --- NUEVO FILTRO ---
    // Carpetas que ignoraremos por completo. Si el escáner
    // ve una carpeta con uno de estos nombres, no entrará.
    private val directoriosIgnorados = listOf(
        "Common Files",     // Archivos comunes de Microsoft
        "Intel",            // Drivers
        "NVIDIA",           // Drivers
        "NVIDIA Corporation",
        "drivers",          // Drivers
        "Microsoft.NET",    // Frameworks
        "Microsoft SDKs",
        "Windows Defender", // Antivirus
        "Temp",
        "Redist",           // Redistribuibles
        "vcredist",
        "DirectX"
    )

    // --- FILTRO MEJORADO ---
    // Palabras clave en nombres de .exe a ignorar
    private val keywordsIgnoradas = listOf(
        "unins", "setup", "update", "crash", "dbg", "report", "support",
        "install", "service", "agent", "helper", "plugin", "eula", "readme" // Añadidos
    )

    // --- FILTRO MEJORADO ---
    // Aumentamos el tamaño mínimo a 5 MB
    private const val TAMANO_MINIMO_MB = 5

    fun escanearJuegos(): List<Juego> {
        return when (DetectorSO.actual) {
            DetectorSO.SistemaOperativo.WINDOWS -> escanearWindows()
            DetectorSO.SistemaOperativo.LINUX -> escanearLinux()
            DetectorSO.SistemaOperativo.OTRO -> emptyList()
        }
    }

    private fun escanearWindows(): List<Juego> {
        println("Escaneando en Windows (modo agresivo)...")
        val juegosEncontrados = mutableListOf<Juego>()

        val directoriosBusqueda = listOf(
            File("C:\\Program Files"),
            File("C:\\Program Files (x86)")
        )

        for (directorio in directoriosBusqueda) {
            if (directorio.exists() && directorio.isDirectory) {
                buscarRecursivamente(directorio, juegosEncontrados)
            }
        }

        println("Escaneo de Windows finalizado. Encontrados: ${juegosEncontrados.size} juegos.")
        return juegosEncontrados
    }

    /**
     * Función recursiva con los nuevos filtros
     */
    private fun buscarRecursivamente(directorio: File, lista: MutableList<Juego>) {

        // --- NUEVO FILTRO POR DIRECTORIO ---
        // Si el nombre de la carpeta está en la lista de ignorados,
        // no seguimos buscando dentro. 'return' para la ejecución aquí.
        if (directoriosIgnorados.any { dirIgnorado -> directorio.name.contains(dirIgnorado) }) {
            return
        }

        val archivos = directorio.listFiles() ?: return

        for (archivo in archivos) {
            try {
                if (archivo.isDirectory) {
                    buscarRecursivamente(archivo, lista)
                } else if (archivo.isFile && archivo.name.endsWith(".exe")) {

                    // 1. Filtro por tamaño (ahora 5MB)
                    val tamanoEnMB = archivo.length() / (1024 * 1024)
                    if (tamanoEnMB < TAMANO_MINIMO_MB) {
                        continue
                    }

                    // 2. Filtro por nombre (lista más larga)
                    val nombreEnMinusculas = archivo.name.lowercase()
                    val contieneKeywordIgnorada = keywordsIgnoradas.any { keyword ->
                        nombreEnMinusculas.contains(keyword)
                    }

                    if (contieneKeywordIgnorada) {
                        continue
                    }

                    // Si pasa todos los filtros, lo añadimos
                    val nuevoJuego = Juego(
                        nombre = archivo.nameWithoutExtension,
                        ruta = archivo.absolutePath,
                        icono = ""
                    )
                    lista.add(nuevoJuego)
                }
            } catch (e: Exception) {
                println("Error al acceder a ${archivo.absolutePath}: ${e.message}")
            }
        }
    }

    private fun escanearLinux(): List<Juego> {
        println("Escaneo de Linux... (no implementado)")
        return emptyList()
    }
}