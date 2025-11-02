package es.davidmarquez.lanzadorapps

import java.io.File // ¡Importante para manejar archivos!

object Scanner {

    /**
     * Esta es la función principal que llamará nuestra UI.
     * Decide qué lógica de escaneo usar.
     */
    fun escanearJuegos(): List<Juego> {
        return when (DetectorSO.actual) {
            DetectorSO.SistemaOperativo.WINDOWS -> escanearWindows()
            DetectorSO.SistemaOperativo.LINUX -> escanearLinux()
            DetectorSO.SistemaOperativo.OTRO -> emptyList() // No soportamos otros SO
        }
    }

    // --- LÓGICA DE WINDOWS ---
    private fun escanearWindows(): List<Juego> {
        println("Escaneando en Windows...")
        val juegosEncontrados = mutableListOf<Juego>()

        // Lista de directorios raíz donde suelen estar los juegos/apps
        val directoriosBusqueda = listOf(
            File("C:\\Program Files"),
            File("C:\\Program Files (x86)")
            // Podrías añadir más, como el Escritorio o el Menú Inicio
        )

        for (directorio in directoriosBusqueda) {
            // Empezamos la búsqueda recursiva
            if (directorio.exists() && directorio.isDirectory) {
                buscarRecursivamente(directorio, juegosEncontrados)
            }
        }

        println("Escaneo de Windows finalizado. Encontrados: ${juegosEncontrados.size} juegos.")
        return juegosEncontrados
    }

    /**
     * Esta función se llama a sí misma para buscar dentro de subcarpetas.
     */
    private fun buscarRecursivamente(directorio: File, lista: MutableList<Juego>) {
        // .listFiles() puede devolver null si no tenemos permisos
        val archivos = directorio.listFiles() ?: return

        for (archivo in archivos) {
            try {
                if (archivo.isDirectory) {
                    // Si es un directorio, seguimos buscando dentro (recursividad)
                    buscarRecursivamente(archivo, lista)
                } else if (archivo.isFile && archivo.name.endsWith(".exe")) {
                    // ¡Encontrado! Lo añadimos a la lista
                    val nuevoJuego = Juego(
                        nombre = archivo.nameWithoutExtension, // "juego.exe" -> "juego"
                        ruta = archivo.absolutePath,
                        icono = "" // TODO: Extraer el icono (es un paso avanzado)
                    )
                    lista.add(nuevoJuego)
                }
            } catch (e: Exception) {
                // Si falla (ej. carpeta sin permisos), solo lo imprimimos y seguimos
                println("Error al acceder a ${archivo.absolutePath}: ${e.message}")
            }
        }
    }

    // --- LÓGICA DE LINUX (Pendiente) ---
    private fun escanearLinux(): List<Juego> {
        println("Escaneo de Linux... (no implementado)")
        // TODO: Implementar la lectura de archivos .desktop
        // en /usr/share/applications y ~/.local/share/applications
        return emptyList()
    }
}