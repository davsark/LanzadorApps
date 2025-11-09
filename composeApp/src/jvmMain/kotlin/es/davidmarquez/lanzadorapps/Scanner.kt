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
            DetectorSO.SistemaOperativo.OTRO -> {
                println("⚠️ Sistema operativo no reconocido") // <-- Esto es lo nuevo
                emptyList()
            }
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
                    // Determinar si es una app de sistema basándose en la ubicación
                    val esAppSistema = rutaEnMinusculas.startsWith("c:\\windows")

                    val nuevoJuego = Juego(
                        nombre = archivo.nameWithoutExtension,
                        ruta = archivo.absolutePath,
                        isSystemApp = esAppSistema
                    )
                    lista.add(nuevoJuego)
                }
            } catch (e: Exception) {
                // Ignorar errores
            }
        }
    }

    private fun escanearLinux(): List<Juego> {
        println("Escaneando en Linux...")
        val appsEncontradas = mutableListOf<Juego>()

        // Directorio principal de aplicaciones del sistema
        val applicationsDir = File("/usr/share/applications")

        if (!applicationsDir.exists() || !applicationsDir.isDirectory) {
            println("⚠️ No se encontró el directorio /usr/share/applications")
            return emptyList()
        }

        // Obtener todos los archivos .desktop
        val desktopFiles = applicationsDir.listFiles { file ->
            file.isFile && file.name.endsWith(".desktop")
        } ?: emptyArray()

        println("📁 Encontrados ${desktopFiles.size} archivos .desktop")

        for (desktopFile in desktopFiles) {
            try {
                val appInfo = parsearDesktopFile(desktopFile)

                if (appInfo != null) {
                    // Verificar que no exista ya en la lista
                    val yaExiste = appsEncontradas.any { it.nombre == appInfo.nombre }
                    if (!yaExiste) {
                        appsEncontradas.add(appInfo)
                    }
                }
            } catch (e: Exception) {
                // Ignorar archivos .desktop mal formados
                println("⚠️ Error leyendo ${desktopFile.name}: ${e.message}")
            }
        }

        println("Escaneo de Linux finalizado. Encontradas: ${appsEncontradas.size} aplicaciones.")
        return appsEncontradas.sortedBy { it.nombre }
    }
    /**
     * Función helper para parsear archivos .desktop de Linux
     * Retorna un objeto Juego si se pudo leer correctamente, o null si falla
     */
    private fun parsearDesktopFile(file: File): Juego? {
        var nombre: String? = null
        var exec: String? = null

        // Leer el archivo línea por línea
        file.readLines().forEach { linea ->
            val lineaTrim = linea.trim()

            when {
                // Buscar la línea Name= (sin locale como [es] o [en])
                lineaTrim.startsWith("Name=") && !lineaTrim.contains("[") -> {
                    nombre = lineaTrim.substringAfter("Name=").trim()
                }
                // Buscar la línea Exec=
                lineaTrim.startsWith("Exec=") -> {
                    val execCompleto = lineaTrim.substringAfter("Exec=").trim()
                    // Tomar solo la primera palabra (antes del espacio o argumentos)
                    exec = execCompleto.split(" ").firstOrNull()?.trim()
                }
            }
        }

        // Solo crear el Juego si tenemos tanto nombre como exec
        return if (nombre != null && exec != null && nombre!!.isNotBlank() && exec!!.isNotBlank()) {
            Juego(
                nombre = nombre!!,
                ruta = exec!!,
                isSystemApp = true
            )
        } else {
            null
        }
    }
}