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
                println("⚠️ Sistema operativo no reconocido")
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
        return juegosEncontrados.sortedBy { it.nombre }
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
        println("🐧 Escaneando en Linux...")
        val appsEncontradas = mutableListOf<Juego>()

        // Directorios donde buscar archivos .desktop
        val directoriosApplications = listOf(
            "/usr/share/applications",           // Aplicaciones del sistema
            "/usr/local/share/applications",     // Aplicaciones instaladas localmente
            "${System.getProperty("user.home")}/.local/share/applications" // Apps del usuario
        )

        // Buscar en cada directorio
        for (dirPath in directoriosApplications) {
            val dir = File(dirPath)

            if (!dir.exists() || !dir.isDirectory) {
                println("⚠️ Directorio no encontrado: $dirPath")
                continue
            }

            // Obtener todos los archivos .desktop
            val desktopFiles = dir.listFiles { file ->
                file.isFile && file.name.endsWith(".desktop")
            } ?: emptyArray()

            println("📂 En $dirPath: ${desktopFiles.size} archivos .desktop")

            for (desktopFile in desktopFiles) {
                try {
                    val appInfo = parsearDesktopFile(desktopFile)

                    if (appInfo != null) {
                        // Verificar que no exista ya en la lista (evitar duplicados)
                        val yaExiste = appsEncontradas.any {
                            it.nombre == appInfo.nombre || it.ruta == appInfo.ruta
                        }
                        if (!yaExiste) {
                            appsEncontradas.add(appInfo)
                            println("✅ App añadida: ${appInfo.nombre} -> ${appInfo.ruta}")
                        }
                    }
                } catch (e: Exception) {
                    println("⚠️ Error leyendo ${desktopFile.name}: ${e.message}")
                }
            }
        }

        // Añadir apps básicas si no se encontraron
        if (appsEncontradas.isEmpty()) {
            println("⚠️ No se encontraron apps. Añadiendo apps básicas...")
            appsEncontradas.addAll(obtenerAppsBasicasLinux())
        }

        println("✅ Escaneo de Linux finalizado. Encontradas: ${appsEncontradas.size} aplicaciones.")
        return appsEncontradas.sortedBy { it.nombre }
    }

    /**
     * Función mejorada para parsear archivos .desktop de Linux
     * Maneja correctamente:
     * - Diferentes formatos de Exec
     * - Argumentos especiales (%U, %F, etc.)
     * - Validación de ejecutables
     * - Iconos
     */
    private fun parsearDesktopFile(file: File): Juego? {
        var nombre: String? = null
        var exec: String? = null
        var icon: String? = null
        var noDisplay = false
        var hidden = false
        var terminal = false

        try {
            // Leer el archivo línea por línea
            file.readLines().forEach { linea ->
                val lineaTrim = linea.trim()

                when {
                    // Ignorar si está marcada como oculta o no display
                    lineaTrim.equals("NoDisplay=true", ignoreCase = true) -> {
                        noDisplay = true
                    }
                    lineaTrim.equals("Hidden=true", ignoreCase = true) -> {
                        hidden = true
                    }
                    lineaTrim.equals("Terminal=true", ignoreCase = true) -> {
                        terminal = true
                    }
                    // Buscar el nombre (sin locale)
                    lineaTrim.startsWith("Name=") && !lineaTrim.contains("[") -> {
                        nombre = lineaTrim.substringAfter("Name=").trim()
                    }
                    // Buscar Exec
                    lineaTrim.startsWith("Exec=") -> {
                        exec = lineaTrim.substringAfter("Exec=").trim()
                    }
                    // Buscar Icon
                    lineaTrim.startsWith("Icon=") -> {
                        icon = lineaTrim.substringAfter("Icon=").trim()
                    }
                }
            }

            // No mostrar apps ocultas
            if (noDisplay || hidden) {
                return null
            }

            // Validar que tenemos nombre y exec
            if (nombre.isNullOrBlank() || exec.isNullOrBlank()) {
                return null
            }

            // Limpiar el comando Exec
            val execLimpio = limpiarComandoExec(exec!!)

            // Validar que el ejecutable existe o está en PATH
            if (!validarEjecutable(execLimpio)) {
                println("⚠️ Ejecutable no encontrado: $execLimpio (de ${file.name})")
                return null
            }

            return Juego(
                nombre = nombre!!,
                ruta = execLimpio,
                isSystemApp = true,
                iconPath = icon  // Guardamos la ruta del icono si la tienes en tu data class
            )
        } catch (e: Exception) {
            println("❌ Error al parsear ${file.name}: ${e.message}")
            return null
        }
    }

    /**
     * Limpia el comando Exec eliminando argumentos especiales de .desktop
     * Ejemplos:
     * - "firefox %u" -> "firefox"
     * - "/usr/bin/gnome-terminal" -> "/usr/bin/gnome-terminal"
     * - "env VARIABLE=value app" -> "app"
     */
    private fun limpiarComandoExec(exec: String): String {
        // Eliminar variables de entorno al inicio (env VAR=value)
        var comando = exec
        if (comando.startsWith("env ")) {
            // Buscar la primera palabra que no sea env ni una asignación
            comando = comando.split(" ").firstOrNull {
                !it.startsWith("env") && !it.contains("=") && it.isNotBlank()
            } ?: comando
        }

        // Dividir por espacios y tomar la primera parte (el ejecutable)
        val partes = comando.split(" ")
        var ejecutable = partes[0].trim()

        // Eliminar comillas si las tiene
        ejecutable = ejecutable.replace("\"", "").replace("'", "")

        // Si tiene argumentos especiales como %U, %F, %u, %f, etc., los ignoramos
        // Estos son placeholders de archivos que el sistema pasa

        return ejecutable
    }

    /**
     * Valida si un ejecutable existe en el sistema
     * Busca en:
     * 1. Ruta absoluta
     * 2. PATH del sistema
     */
    private fun validarEjecutable(ejecutable: String): Boolean {
        // Si es una ruta absoluta, verificar que existe
        val archivoAbsoluto = File(ejecutable)
        if (archivoAbsoluto.isAbsolute) {
            return archivoAbsoluto.exists() && archivoAbsoluto.canExecute()
        }

        // Si es un nombre de comando, buscar en PATH
        val pathEnv = System.getenv("PATH") ?: return false
        val paths = pathEnv.split(":")

        for (path in paths) {
            val archivo = File(path, ejecutable)
            if (archivo.exists() && archivo.canExecute()) {
                return true
            }
        }

        return false
    }

    /**
     * Lista de aplicaciones básicas de Linux que se añaden si no se encuentra nada
     */
    private fun obtenerAppsBasicasLinux(): List<Juego> {
        val appsBasicas = listOf(
            Triple("Terminal", "gnome-terminal", "utilities-terminal"),
            Triple("Archivos", "nautilus", "system-file-manager"),
            Triple("Firefox", "firefox", "firefox"),
            Triple("Navegador Web", "google-chrome", "google-chrome"),
            Triple("Editor de Texto", "gedit", "text-editor"),
            Triple("Editor de Texto", "gnome-text-editor", "text-editor"),
            Triple("Calculadora", "gnome-calculator", "accessories-calculator"),
            Triple("Monitor del Sistema", "gnome-system-monitor", "utilities-system-monitor"),
            Triple("Configuración", "gnome-control-center", "preferences-system")
        )

        val appsEncontradas = mutableListOf<Juego>()

        for ((nombre, comando, icon) in appsBasicas) {
            if (validarEjecutable(comando)) {
                // Evitar duplicados por nombre
                if (appsEncontradas.none { it.nombre == nombre }) {
                    appsEncontradas.add(
                        Juego(
                            nombre = nombre,
                            ruta = comando,
                            isSystemApp = true,
                            iconPath = icon
                        )
                    )
                    println("✅ App básica añadida: $nombre -> $comando")
                }
            }
        }

        return appsEncontradas
    }
}