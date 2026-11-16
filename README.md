# M3U Player para Fire TV

App Android nativa (Kotlin + Jetpack Compose para TV + Media3/ExoPlayer) que lee listas M3U/M3U8
y reproduce los canales con el control remoto de Fire TV.

## Funciones

- Carga de listas M3U por URL (se guarda y se recarga al abrir la app).
- Filas por categoría (`group-title`) con logo y nombre del canal.
- "Continuar viendo" (último canal) y "Favoritos" (mantén pulsado OK sobre un canal).
- Búsqueda por nombre o categoría.
- Reproductor a pantalla completa: Arriba/Abajo cambia de canal, OK muestra la información,
  Play/Pausa pausa, Atrás vuelve a la lista.
- Reintento automático como HLS si la URL no termina en `.m3u8`.
- Tema oscuro, tipografía grande, foco visible y animación en cada tarjeta.

## Cómo abrirlo y compilarlo

1. Instala **Android Studio** (versión Ladybug 2024.2 o superior).
2. `File > Open` y elige la carpeta `M3UPlayer`.
3. Espera a que termine el *Gradle Sync* (descarga Gradle 8.9 y las dependencias; necesita internet).
   El proyecto ya incluye el Gradle Wrapper (`gradlew`, `gradlew.bat` y `gradle/wrapper/gradle-wrapper.jar`),
   así que también puedes compilar desde la terminal con `./gradlew assembleDebug` (en Windows: `gradlew.bat assembleDebug`).
4. `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
   El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Instalarlo en el Fire TV

1. En el Fire TV: `Ajustes > Mi Fire TV > Opciones para desarrolladores`
   y activa **Depuración ADB** y **Apps de orígenes desconocidos**.
   (Si no ves las opciones: `Acerca de` y pulsa 7 veces sobre el nombre del dispositivo.)
2. Anota la IP del Fire TV (`Ajustes > Mi Fire TV > Acerca de > Red`).
3. En tu computadora:

```
adb connect IP_DEL_FIRE_TV:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

También puedes subir el APK a un sitio propio y descargarlo con la app **Downloader**.
La app aparece en *Tus apps y canales* con el nombre "M3U Player".

## Estructura

```
app/src/main/java/com/example/m3uplayer/
  MainActivity.kt
  data/   Channel, M3uParser, ChannelRepository, Prefs (DataStore)
  ui/     App, HomeScreen, PlayerScreen, Dialogs, Components, Theme, MainViewModel
```

## Notas

- Escribir una URL larga con el mando es lento. Usa la app Fire TV del móvil como teclado,
  o un acortador de enlaces.
- `usesCleartextTraffic` está activado porque muchas listas usan `http://`.
- El banner (`res/drawable/banner.xml`) y el icono son provisionales. Para publicar en la
  Amazon Appstore sustitúyelos por imágenes PNG propias.
- La app no incluye ninguna lista de canales: el usuario añade la suya. Usa solo listas
  con contenido que tengas derecho a ver.
