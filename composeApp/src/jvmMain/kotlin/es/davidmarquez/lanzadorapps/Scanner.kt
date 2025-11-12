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
            "/usr/share/applications",
            "/usr/local/share/applications",
            "${System.getProperty("user.home")}/.local/share/applications"
        )

        var totalDesktopFiles = 0

        // Buscar en cada directorio
        for (dirPath in directoriosApplications) {
            val dir = File(dirPath)
            
            if (!dir.exists() || !dir.isDirectory) {
                println("  ⚠️ Directorio no encontrado: $dirPath")
                continue
            }

            val desktopFiles = dir.listFiles { file ->
                file.isFile && file.name.endsWith(".desktop")
            } ?: emptyArray()

            totalDesktopFiles += desktopFiles.size
            println("  📂 En $dirPath: ${desktopFiles.size} archivos .desktop")

            for (desktopFile in desktopFiles) {
                try {
                    val appInfo = parsearDesktopFile(desktopFile)

                    if (appInfo != null) {
                        // Verificar duplicados por nombre o ruta
                        val yaExiste = appsEncontradas.any { 
                            it.nombre == appInfo.nombre || it.ruta == appInfo.ruta 
                        }
                        if (!yaExiste) {
                            appsEncontradas.add(appInfo)
                        }
                    }
                } catch (e: Exception) {
                    // Continuar con el siguiente archivo
                }
            }
        }

        println("  📊 Total archivos .desktop encontrados: $totalDesktopFiles")
        println("  ✅ Apps válidas procesadas: ${appsEncontradas.size}")

        // Si encontramos pocas apps, añadir apps básicas
        if (appsEncontradas.size < 10) {
            println("  ⚠️ Pocas apps encontradas. Añadiendo apps básicas...")
            val appsBasicas = obtenerAppsBasicasLinux()
            for (app in appsBasicas) {
                val yaExiste = appsEncontradas.any { 
                    it.nombre == app.nombre || it.ruta == app.ruta 
                }
                if (!yaExiste) {
                    appsEncontradas.add(app)
                }
            }
        }

        println("✅ Escaneo finalizado. Total: ${appsEncontradas.size} aplicaciones")
        return appsEncontradas.sortedBy { it.nombre }
    }

    private fun parsearDesktopFile(file: File): Juego? {
        var nombre: String? = null
        var exec: String? = null
        var icon: String? = null
        var noDisplay = false
        var hidden = false
        var terminal = false
        var type: String? = null

        try {
            val lines = file.readLines()
            
            for (linea in lines) {
                val lineaTrim = linea.trim()

                // Ignorar líneas vacías y comentarios
                if (lineaTrim.isEmpty() || lineaTrim.startsWith("#")) {
                    continue
                }

                when {
                    lineaTrim.startsWith("Type=") -> {
                        type = lineaTrim.substringAfter("Type=").trim()
                    }
                    lineaTrim.equals("NoDisplay=true", ignoreCase = true) -> {
                        noDisplay = true
                    }
                    lineaTrim.equals("Hidden=true", ignoreCase = true) -> {
                        hidden = true
                    }
                    lineaTrim.equals("Terminal=true", ignoreCase = true) -> {
                        terminal = true
                    }
                    lineaTrim.startsWith("Name=") && !lineaTrim.contains("[") -> {
                        nombre = lineaTrim.substringAfter("Name=").trim()
                    }
                    lineaTrim.startsWith("Exec=") -> {
                        exec = lineaTrim.substringAfter("Exec=").trim()
                    }
                    lineaTrim.startsWith("Icon=") -> {
                        icon = lineaTrim.substringAfter("Icon=").trim()
                    }
                }
            }

            // Validaciones
            if (type != "Application") {
                return null
            }

            if (noDisplay || hidden) {
                return null
            }

            if (nombre.isNullOrBlank() || exec.isNullOrBlank()) {
                return null
            }

            // Limpiar comando Exec
            val execLimpio = limpiarComandoExec(exec!!)
            
            // Validar que el ejecutable existe
            if (!validarEjecutable(execLimpio)) {
                return null
            }

            println("  ✅ ${nombre!!.padEnd(30)} -> $execLimpio" + (if (icon != null) " [icon: $icon]" else ""))

            return Juego(
                nombre = nombre!!,
                ruta = execLimpio,
                isSystemApp = true,
                iconPath = icon
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun limpiarComandoExec(exec: String): String {
        var comando = exec.trim()

        // Eliminar prefijos comunes
        val prefixesToRemove = listOf("env ", "gtk-launch ", "gio launch ")
        for (prefix in prefixesToRemove) {
            if (comando.startsWith(prefix)) {
                comando = comando.substring(prefix.length).trim()
            }
        }

        // Si empieza con variables de entorno (VAR=value), saltarlas
        while (comando.contains("=") && !comando.startsWith("/") && !comando.startsWith("~")) {
            val firstSpace = comando.indexOf(" ")
            if (firstSpace > 0) {
                comando = comando.substring(firstSpace + 1).trim()
            } else {
                break
            }
        }

        // Dividir por espacios y tomar solo el ejecutable
        val partes = comando.split(Regex("\\s+"))
        var ejecutable = partes[0].trim()

        // Eliminar comillas
        ejecutable = ejecutable.replace("\"", "").replace("'", "")

        // Si termina en .desktop, buscar el nombre base
        if (ejecutable.endsWith(".desktop")) {
            ejecutable = ejecutable.substringBeforeLast(".desktop")
        }

        return ejecutable
    }

    private fun validarEjecutable(ejecutable: String): Boolean {
        // Si es ruta absoluta
        val archivoAbsoluto = File(ejecutable)
        if (archivoAbsoluto.isAbsolute) {
            return archivoAbsoluto.exists() && archivoAbsoluto.canExecute()
        }

        // Buscar en PATH
        val pathEnv = System.getenv("PATH") ?: return false
        val paths = pathEnv.split(":")
        
        for (path in paths) {
            val archivo = File(path, ejecutable)
            if (archivo.exists() && archivo.canExecute()) {
                return true
            }
        }

        // Algunos ejecutables comunes que sabemos que existen
        val commonExecutables = listOf(
            "gnome-terminal", "konsole", "xterm", "terminator",
            "nautilus", "dolphin", "thunar", "pcmanfm",
            "firefox", "chromium", "google-chrome", "brave",
            "gedit", "kate", "mousepad", "pluma",
            "gnome-calculator", "kcalc", "galculator",
            "gnome-system-monitor", "ksysguard",
            "gnome-control-center", "systemsettings"
        )

        return commonExecutables.contains(ejecutable)
    }

    private fun obtenerAppsBasicasLinux(): List<Juego> {
        val appsBasicas = listOf(
            // Terminales
            Triple("Terminal GNOME", "gnome-terminal", "utilities-terminal"),
            Triple("Terminal", "konsole", "utilities-terminal"),
            Triple("XTerm", "xterm", "utilities-terminal"),
            Triple("Terminator", "terminator", "terminator"),
            
            // Gestores de archivos
            Triple("Archivos", "nautilus", "system-file-manager"),
            Triple("Dolphin", "dolphin", "system-file-manager"),
            Triple("Thunar", "thunar", "system-file-manager"),
            
            // Navegadores
            Triple("Firefox", "firefox", "firefox"),
            Triple("Chromium", "chromium", "chromium"),
            Triple("Google Chrome", "google-chrome", "google-chrome"),
            Triple("Brave", "brave", "brave"),
            
            // Editores
            Triple("Editor de Texto", "gedit", "text-editor"),
            Triple("Kate", "kate", "kate"),
            Triple("Mousepad", "mousepad", "mousepad"),
            
            // Utilidades
            Triple("Calculadora", "gnome-calculator", "accessories-calculator"),
            Triple("Monitor del Sistema", "gnome-system-monitor", "utilities-system-monitor"),
            Triple("Configuración", "gnome-control-center", "preferences-system")
        )

        val appsEncontradas = mutableListOf<Juego>()

        for ((nombre, comando, icon) in appsBasicas) {
            if (validarEjecutable(comando)) {
                if (appsEncontradas.none { it.nombre == nombre }) {
                    appsEncontradas.add(
                        Juego(
                            nombre = nombre,
                            ruta = comando,
                            isSystemApp = true,
                            iconPath = icon
                        )
                    )
                    println("  ✅ App básica: ${nombre.padEnd(30)} -> $comando")
                }
            }
        }

        return appsEncontradas
    }
}
