package es.davidmarquez.lanzadorapps

import java.io.File

object Scanner {

    private val directoriosIgnorados = listOf(
        "Common Files", "Intel", "NVIDIA", "NVIDIA Corporation", "drivers",
        "Microsoft.NET", "Microsoft SDKs", "Windows Defender", "Temp",
        "Redist", "vcredist", "DirectX"
    )

    private val keywordsIgnoradas = listOf(
        "unins", "setup", "update", "crash", "dbg", "report", "support",
        "install", "service", "agent", "helper", "plugin", "eula", "readme",
        "daemon", "symbolizer", "clangd", "redist", "perf", "vcredist"
    )

    private val pathKeywordsIgnorados = listOf(
        "\\plugins\\", "\\resources\\", "\\lldb\\", "\\VC\\"
    )

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

        // PASO 1: Búsqueda profunda en Program Files
        val directoriosBusqueda = listOf(
            File("C:\\Program Files"),
            File("C:\\Program Files (x86)")
        )
        for (directorio in directoriosBusqueda) {
            if (directorio.exists() && directorio.isDirectory) {
                buscarRecursivamente(directorio, juegosEncontrados)
            }
        }

        // PASO 2: Añadir apps clásicas de System32
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
                            ruta = appFile.absolutePath ,
                            isSystemApp = true
                        ))
                    }
                }
            }
        }

        println("Escaneo de Windows finalizado. Encontrados: ${juegosEncontrados.size} juegos.")
        return juegosEncontrados.sortedBy { it.nombre } // Ordenados alfabéticamente
    }

    private fun buscarRecursivamente(directorio: File, lista: MutableList<Juego>) {

        if (directoriosIgnorados.any { dirIgnorado -> directorio.name.contains(dirIgnorado) }) {
            return
        }

        val archivos = directorio.listFiles() ?: return

        for (archivo in archivos) {
            try {
                if (archivo.isDirectory) {
                    buscarRecursivamente(archivo, lista)
                } else if (archivo.isFile && archivo.name.endsWith(".exe")) {

                    val tamanoEnMB = archivo.length() / (1024 * 1024)
                    if (tamanoEnMB < TAMANO_MINIMO_MB) {
                        continue
                    }

                    val nombreEnMinusculas = archivo.name.lowercase()
                    if (keywordsIgnoradas.any { nombreEnMinusculas.contains(it) }) {
                        continue
                    }

                    val rutaEnMinusculas = archivo.absolutePath.lowercase()
                    if (pathKeywordsIgnorados.any { rutaEnMinusculas.contains(it) }) {
                        continue
                    }

                    val nuevoJuego = Juego(
                        nombre = archivo.nameWithoutExtension,
                        ruta = archivo.absolutePath)
                    lista.add(nuevoJuego)
                }
            } catch (e: Exception) {
                // Ignorar errores
            }
        }
    }

    private fun escanearLinux(): List<Juego> {
        println("Escaneo de Linux... (no implementado)")
        return emptyList()
    }
}