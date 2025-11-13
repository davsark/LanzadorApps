# 🚀 Lanzador de Aplicaciones

## 🔹 Autor:
- David Márquez López

## 🧩 Clonar, Compilar y Ejecutar el Proyecto

### 🔹 Requisitos Previos
- **JDK 17 o superior**
- **Gradle Wrapper** (ya incluido en el repositorio)
- **Git** instalado en el sistema
---

## 🔹 Clonar el Repositorio
        ```
        git clone [https://github.com/davsark/LanzadorApps.git](https://github.com/davsark/LanzadorApps.git)
        cd LanzadorDeApps
        ```

## 🔹 Descargar el instalador
1. Acceder a: https://github.com/davsark/LanzadorApps/releases/

2. 🐧 En Linux:
  - Seleccionar el instalador .deb
3. 🪟 En Windows:
  - Seleccionar el instalador .msi
4. Seguir los pasos de instalación de cada equipo

## 🔹 Ejecutar la Aplicación en Modo Desarrollo

1. 🪟 En Windows:
    ```
    .\gradlew.bat :composeApp:run
    ```

2. 🐧 En Linux:
    ```
    ./gradlew :composeApp:run
    ```

3. Esto iniciará la aplicación directamente desde el entorno de desarrollo, utilizando Compose for Desktop.

---

## 🔹 Crear el Instalador / Distribución

1. Para generar el instalador nativo del sistema operativo actual:

  - 🪟 En Windows:
      ```
      .\gradlew.bat packageDistributionForCurrentOS
      ```
  - 🐧 En Linux:
      ```
      ./gradlew packageDistributionForCurrentOS
      ```

2. El instalador resultante se genera automáticamente en la carpeta:
    ```
    build/compose/binaries/main/
    ```

3. Dentro encontrarás el archivo `.msi` (Windows) o `.deb` (Linux) según la plataforma.

---

## 🔹 Publicar una Nueva Release en GitHub

1. Asegúrate de tener todos los cambios confirmados:
    ```
    git add .
    git commit -m "Versión 1.2.0 - mejoras de rendimiento y UI"
    ```

2. Crea una etiqueta (tag) para la nueva versión:
    ```
    git tag -a v1.2.0 -m "Lanzador de Apps v1.2.0"
    ```

3. Sube los cambios y la etiqueta al repositorio remoto:
    ```
    git push origin main
    git push origin v1.2.0
    ```

4. 📦 Crear la release en GitHub:
  - Ve a la pestaña “Releases” del repositorio.
  - Pulsa “Draft a new release”.
  - Selecciona la etiqueta creada (`v1.2.0`), añade notas de cambios y adjunta los instaladores generados.
  - Publica la release.

---
# 🧾 Descripción del Proyecto

**Lanzador de Aplicaciones** es una aplicación de escritorio desarrollada en **Kotlin** con **Compose for Desktop**.  
Su objetivo es ofrecer al usuario una interfaz centralizada para **escanear, visualizar y ejecutar** las aplicaciones y juegos instalados en su sistema operativo (**Windows o Linux**).  
El sistema detecta automáticamente el software instalado, permitiendo filtrarlo, ordenarlo y lanzarlo con un solo clic.

---

## 🎯 1. Objetivos Específicos

- Detectar automáticamente aplicaciones y juegos instalados (diferenciando Windows/Linux).
- Ejecutar aplicaciones mediante `java.lang.ProcessBuilder`.
- Mostrar nombre, icono y ruta completa de cada aplicación.
- Permitir añadir manualmente apps no detectadas (usando `java.awt.FileDialog`).
- Gestionar errores de forma robusta con bloques `try-catch`.
- Ofrecer búsqueda, filtrado (**Todas, Sistema, Usuario**) y ordenación (**A-Z, Z-A**) desde la interfaz.

---

## 🧰 2. Tecnologías Utilizadas

| Tecnología | Descripción |
|-------------|--------------|
| **Kotlin** | Lenguaje principal con null-safety y compatibilidad con Java. |
| **Compose for Desktop** | Framework declarativo para la UI moderna y reactiva. |
| **Kotlin Coroutines** | Manejo de tareas asíncronas sin bloquear la interfaz. |
| **java.lang.ProcessBuilder** | Lanzamiento de procesos externos (.exe o binarios). |
| **java.io.File** | Escaneo de directorios del sistema. |
| **AWT/Swing** | Integración de componentes nativos (FileDialog, ImageIcon, FileSystemView). |

---

## 🏗️ 3. Arquitectura del Sistema

El proyecto sigue el principio de **separación de responsabilidades**:

| Componente | Descripción |
|-------------|-------------|
| `main.kt` | Punto de entrada, lanza la ventana principal. |
| `App.kt` | Núcleo de la UI: define interfaz, estado y eventos. |
| `DetectorSO.kt` | Detecta el sistema operativo actual. |
| `Scanner.kt` | Escanea aplicaciones instaladas según la plataforma. |
| `IconExtractor.kt` / `IconUtils.kt` | Obtención y conversión de iconos nativos. |
| `Greeting.kt`, `Platform.kt` | Archivos auxiliares no críticos. |

---

## ⚙️ 4. Funcionalidades Principales

- **Escaneo del sistema:** Busca automáticamente ejecutables (`.exe` o `.desktop`).
- **Listado de aplicaciones:** Muestra icono, nombre, ruta y tipo (Sistema/Usuario).
- **Búsqueda y filtros:** Permiten buscar, filtrar y ordenar en tiempo real.
- **Lanzamiento directo:** Ejecuta la aplicación seleccionada.
- **Añadir app manualmente:** Permite al usuario incorporar apps no detectadas.
- **Abrir ubicación:** Abre la carpeta del ejecutable en el explorador del sistema.

---

## 🧪 5. Pruebas y Resultados

Todas las pruebas funcionales y de interfaz se realizaron en **Windows 10**, **Windows 11** y **Ubuntu 22.04**,  
con resultados exitosos en todos los casos.

| ID | Prueba | Resultado |
|----|---------|-----------|
| P-01 – P-12 | Detección, escaneo, UI, iconos, filtros y ejecución | ✅ Superadas |

---

## 🧭 6. Conclusiones

El proyecto logró entregar un **lanzador multiplataforma completo**, con interfaz moderna y excelente rendimiento.  
**Kotlin** y **Compose for Desktop** demostraron ser tecnologías idóneas para el desarrollo de aplicaciones gráficas de escritorio.

---

## 📦 7. Repositorio

**Código fuente:**  
👉 [https://github.com/davsark/LanzadorApps](https://github.com/davsark/LanzadorApps)

---

## 🤖 8. Créditos y Uso de Inteligencia Artificial

Durante el desarrollo se utilizaron herramientas de asistencia de código e IA:

- **GitHub Copilot**
- **Claude (Anthropic)**
- **Gemini (Google)**

Empleadas para generación de código, refactorización, depuración y redacción técnica del documento.

---

© **2025** – Proyecto académico desarrollado con **Kotlin + Compose for Desktop**
