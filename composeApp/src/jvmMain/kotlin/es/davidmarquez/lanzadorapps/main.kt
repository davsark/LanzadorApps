package es.davidmarquez.lanzadorapps

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Launcher de Apps", //Este es el título que se muestra en la ventana
    ) {
        App(window) //le pasamos esta propiedad a la funcion App()
    }
}