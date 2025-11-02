package es.davidmarquez.lanzadorapps

/**
 * Un 'object' es un singleton. Solo habrá una instancia de DetectorSO
 * en toda la aplicación.
 */
object DetectorSO {

    /**
     * Un enum para tener tipos seguros de SO.
     */
    enum class SistemaOperativo {
        WINDOWS,
        LINUX,
        OTRO
    }

    /**
     * 'lazy' significa que este código solo se ejecutará
     * la primera vez que alguien pregunte por 'DetectorSO.actual'.
     * Las siguientes veces, devolverá el valor ya calculado.
     */
    val actual: SistemaOperativo by lazy {
        // Obtenemos el nombre del SO de las propiedades del sistema
        val osName = System.getProperty("os.name").lowercase()

        // 'when' es el 'switch' mejorado de Kotlin
        when {
            osName.contains("win") -> SistemaOperativo.WINDOWS
            osName.contains("nix") || osName.contains("nux") -> SistemaOperativo.LINUX
            else -> SistemaOperativo.OTRO
        }
    }
}
