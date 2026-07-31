# MLKit_Sem14 — Google ML Kit (Rostros + Malla Facial + OCR + Códigos QR/Barras)

App Android en Java que usa **Google ML Kit** para reconocer texto (OCR), detectar rostros con recuadro y malla facial (contornos) y decodificar códigos de barras y QR, a partir de una imagen tomada con la cámara o elegida de la galería. Incluye además un modo de **escaneo de códigos en vivo** con CameraX.

## Características

- Seleccionar imagen desde la **galería** o capturarla con la **cámara**.
- **OCR**: reconocimiento de texto latino sobre la imagen, respetando la estructura de líneas y párrafos del original (si la imagen tiene 3 líneas, el resultado muestra 3 líneas).
- **Placas ecuatorianas**: detecta la matrícula de un vehículo, la encierra en un recuadro rojo y muestra un porcentaje de precisión estimada de la lectura.
- **Detección de rostros**: conteo de rostros ("Hay X rostro(s)") y recuadro rojo sobre cada uno.
- **Malla facial**: puntos verdes sobre los contornos de **todos** los rostros de la foto (óvalo de la cara, cejas, ojos, labios, puente y base de la nariz).
- **Códigos de barras y QR sobre foto**: decodifica los códigos presentes en la imagen cargada, los marca sobre ella y muestra formato, tipo de contenido y valor.
- **Escaneo en vivo**: pantalla aparte con CameraX que lee códigos de forma continua desde el preview de la cámara, sin sacar ninguna foto, y devuelve la lectura a la pantalla principal.
- Validaciones básicas: solicitud del permiso de cámara en tiempo de ejecución y aviso "Selecciona primero una imagen" si aún no hay imagen cargada.

## Requisitos

| | |
|---|---|
| Lenguaje | Java |
| minSdk | 24 (Android 7.0 Nougat) |
| targetSdk / compileSdk | 36 |
| Build | Gradle con Kotlin DSL (`build.gradle.kts`) |
| Java | 11 |
| IDE | Android Studio |

## Dependencias ML Kit

```kotlin
// Face features
implementation("com.google.mlkit:face-detection:16.1.7")
// Text features
implementation("com.google.android.gms:play-services-mlkit-text-recognition:18.0.2")
// Barcode & QR features
implementation("com.google.mlkit:barcode-scanning:17.3.0")
// CameraX (escaneo en vivo)
implementation("androidx.camera:camera-camera2:1.4.2")
implementation("androidx.camera:camera-lifecycle:1.4.2")
implementation("androidx.camera:camera-view:1.4.2")
```

> `face-detection` se actualizó de `16.1.5` a `16.1.7` porque en la versión anterior la librería nativa `libface_detector_v2_jni.so` (arm64-v8a) no estaba alineada a 16 KB y Android mostraba el aviso de compatibilidad.

> Para códigos se usa `barcode-scanning` (modelo **empaquetado** dentro del APK) en lugar de la variante `play-services-mlkit-barcode-scanning`: así el escáner funciona desde el primer uso, sin descarga previa del modelo ni conexión a internet.

## Permisos

Declarados en `AndroidManifest.xml`:

- `INTERNET`
- `WRITE_EXTERNAL_STORAGE`
- `READ_EXTERNAL_STORAGE`
- `CAMERA`

El permiso de `CAMERA` además se solicita en tiempo de ejecución desde `MainActivity.onCreate()`.

## Estructura

```
app/src/main/
├── java/com/uteq/software/mlkit_sem14/
│   ├── MainActivity.java        // selección de imagen, OCR, rostros, placas y códigos sobre foto
│   ├── ScannerActivity.java     // escaneo de códigos en vivo con CameraX
│   ├── BarcodeOverlay.java      // capa que dibuja los recuadros sobre el preview de la cámara
│   └── Codigos.java             // nombres de formato/tipo y lectura legible de un Barcode
├── res/layout/
│   ├── activity_main.xml        // logo, Galería/Cámara, imagen, resultados y botonera
│   └── activity_scanner.xml     // PreviewView + overlay + panel de resultado en vivo
├── res/drawable/
│   └── ic_mlkit.png             // logo mostrado en la parte superior
└── AndroidManifest.xml
```

La botonera inferior de la pantalla principal está en dos filas:
`OCR · Placas · Rostros` arriba y `Códigos · En vivo` abajo.

### Métodos principales de `MainActivity`

| Método | Descripción |
|---|---|
| `abrirGaleria(View)` | Lanza un `ACTION_PICK` sobre `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`. |
| `abrirCamara(View)` | Lanza `MediaStore.ACTION_IMAGE_CAPTURE`. |
| `onActivityResult(...)` | Recibe el `Bitmap` y lo muestra en el `ImageView`, o el código devuelto por `ScannerActivity`. |
| `OCRfx(View)` | Procesa la imagen con `TextRecognition` y vuelca el texto en `txtresults`. |
| `Rostrosfx(View)` | Procesa la imagen con `FaceDetection` (`PERFORMANCE_MODE_ACCURATE` + `CONTOUR_MODE_ALL`), cuenta rostros y dibuja recuadro + puntos de contorno sobre el `Canvas`. |
| `Placafx(View)` | Reutiliza el OCR, filtra las cadenas con formato de placa ecuatoriana, dibuja un recuadro rojo alrededor de cada una y estima su precisión. |
| `Codigosfx(View)` | Procesa la imagen con `BarcodeScanning`, lista formato/tipo/valor de cada código y los marca sobre la imagen. |
| `EnVivofx(View)` | Abre `ScannerActivity` para escanear con la cámara en tiempo real. |

## Malla facial en fotos con varias caras

ML Kit detecta y encuadra todos los rostros, pero al pedir contornos normalmente solo los
devuelve para el rostro más prominente de la imagen. Para que la malla salga en **todas** las
caras, `dibujarRostros()` hace lo siguiente:

1. Dibuja el recuadro rojo de cada rostro de la primera detección.
2. Si un rostro ya trae contornos, los pinta directamente.
3. Si llega sin contornos, recorta esa cara (con un 15 % de margen), la amplía si es pequeña
   (el modelo de contornos rinde mal con caras chicas) y lanza una segunda detección sobre ese
   recorte. Los puntos obtenidos se dividen por la escala y se desplazan al origen del recorte
   para caer en su sitio dentro de la imagen completa.
4. Espera a que terminen todas las detecciones pendientes con `Tasks.whenAllComplete()` y
   entonces muestra el bitmap una sola vez.

## Formato de placas ecuatorianas

La detección se apoya en el OCR y filtra los resultados con este patrón:

```
[A-Z]{3} - [0-9]{3,4}        ej. PBA-1234 (actuales) · PBA-123 (antiguas)
```

- **Primera letra**: código de la provincia. Ecuador tiene 24 provincias y sus códigos son
  `A` Azuay · `B` Bolívar · `U` Cañar · `C` Carchi · `X` Cotopaxi · `H` Chimborazo ·
  `O` El Oro · `E` Esmeraldas · `W` Galápagos · `G` Guayas · `I` Imbabura · `L` Loja ·
  `R` Los Ríos · `M` Manabí · `V` Morona Santiago · `N` Napo · `Q` Orellana · `S` Pastaza ·
  `P` Pichincha · `K` Sucumbíos · `T` Tungurahua · `Z` Zamora Chinchipe · `Y` Santa Elena ·
  `J` Santo Domingo de los Tsáchilas.
  Las letras **D** y **F** no se usan como código provincial.
- **Segunda y tercera letra**: serie, cualquier letra.
- **Dígitos**: 3 en las placas antiguas, 4 en las actuales.

Como el OCR confunde caracteres parecidos, si la lectura directa no coincide se reintenta
corrigiendo por posición: `0→O`, `1→I`, `5→S`, `8→B`, `2→Z` en las letras y
`O/Q/D→0`, `I/L→1`, `S→5`, `B→8`, `Z→2` en los dígitos.

La placa puede venir en una sola línea (`PBA-1234`), en un solo elemento, o partida en dos
elementos (`PBA` + `1234`); en ese último caso se unen las dos cajas con `Rect.union()` para
que el recuadro cubra la placa completa.

### Porcentaje de precisión

Junto a cada placa se muestra un porcentaje, calculado como:

```
precision = confianza_MLKit * 100 - (caracteres_corregidos * 5)
```

- **`confianza_MLKit`**: valor que devuelve `Text.Line.getConfidence()` / `Text.Element.getConfidence()`
  para ese texto (si el modelo no la reporta se asume 0.85). Cuando la placa viene partida en dos
  elementos se promedia la confianza de ambos.
- **`caracteres_corregidos`**: cuántos caracteres hubo que cambiar con la corrección O/0, I/1, etc.
  para que la lectura encajara en el formato. Mientras más se corrigió, menos seguro es que lo leído
  coincida con la placa real.

El resultado se acota a 0–100 % y se dibuja también sobre la imagen, encima del recuadro.

> Es una estimación de la confianza del reconocimiento, no una comparación contra la placa real:
> la app no tiene forma de conocer la matrícula verdadera del vehículo de la foto.

## Códigos de barras y QR

Un mismo escáner de ML Kit cubre los códigos 1D y 2D, así que se configura con
`FORMAT_ALL_FORMATS` y con eso quedan cubiertos ambos casos pedidos:

| | Formatos |
|---|---|
| **Barras (1D)** | EAN-13, EAN-8, UPC-A, UPC-E, Code 39, Code 93, Code 128, Codabar, ITF |
| **2D** | **QR**, Aztec, Data Matrix, PDF417 |

Por cada código detectado se muestra:

- **Formato**: QR, EAN-13, Code 128, etc.
- **Tipo de contenido**: URL, WiFi, Texto, Teléfono, Correo, Contacto, Ubicación…
- **Valor decodificado**.

ML Kit no solo devuelve la cadena cruda: cuando el contenido sigue un formato conocido lo
entrega ya separado en campos. `Codigos.texto()` aprovecha eso, de modo que un QR de red WiFi
muestra SSID y clave por separado, uno de contacto muestra nombre y organización, y uno de URL
muestra título y enlace. Para lo demás cae al `getRawValue()`.

### Marcado sobre la imagen

`dibujarCodigos()` dibuja dos cosas por código: el `boundingBox` en rojo y el contorno real de
cuatro esquinas (`getCornerPoints()`) en verde. El contorno importa porque un código fotografiado
en ángulo queda como un cuadrilátero, no como un rectángulo, y la caja por sí sola no lo refleja.

## Escaneo en vivo con CameraX

`ScannerActivity` enlaza dos casos de uso de CameraX al ciclo de vida de la pantalla con
`bindToLifecycle()`: `Preview` (lo que se ve) e `ImageAnalysis` (lo que se analiza). Cada
fotograma se pasa directamente al escáner con `InputImage.fromMediaImage(...)`, sin sacar
ninguna foto intermedia.

Detalles que importan para que no se congele el preview:

- **`STRATEGY_KEEP_ONLY_LATEST`**: mientras ML Kit analiza un fotograma, los que van llegando se
  descartan y solo se conserva el más reciente. Sin esto el preview se atasca en cuanto el
  análisis va más lento que la cámara.
- **Cerrar siempre el `ImageProxy`**: se cierra en `addOnCompleteListener`, no en el de éxito.
  Si el análisis falla y el proxy no se cierra, CameraX deja de entregar fotogramas.
- **Análisis fuera del hilo principal**: el analizador corre en un `ExecutorService` de un solo
  hilo, que se apaga en `onDestroy()` junto con el escáner.

### Recuadros sobre el preview

`BarcodeOverlay` es una vista transparente encima del `PreviewView` que dibuja el recuadro y el
formato de cada código en tiempo real.

Las cajas de ML Kit vienen en coordenadas de la **imagen de análisis** (ya enderezada según la
rotación), que no coincide ni en tamaño ni en relación de aspecto con la vista. Por eso el overlay
replica el mismo encuadre que usa `PreviewView` con `FILL_CENTER`: escala por el lado que llena la
vista y centra el resto. Sin esa conversión los recuadros salen desplazados respecto al código que
ve el usuario.

Además, cuando la rotación es de 90° o 270° la imagen se endereza girando, así que ancho y alto se
intercambian antes de pasarlos al overlay.

### Flujo

1. **En vivo** abre `ScannerActivity`, que pide el permiso de cámara si aún no lo tiene y sale
   con un aviso si se deniega.
2. La lectura se va mostrando en el panel inferior conforme se detecta.
3. **Usar código** cierra la pantalla y devuelve la última lectura a `MainActivity`, que la
   muestra en su panel de resultados.
4. **Cerrar** sale sin devolver nada.

## Cómo ejecutar

1. Clonar el repositorio y abrirlo en Android Studio.
2. Sincronizar Gradle (descarga las dependencias de ML Kit).
3. Ejecutar en un dispositivo o emulador con API 24 o superior.
4. Pulsar **Galería** o **Cámara**, y luego **OCR**, **Placas**, **Rostros** o **Códigos**.
   Para el escaneo en tiempo real, pulsar **En vivo** (no necesita imagen cargada).

O desde la terminal:

```bash
./gradlew assembleDebug
```

## Pruebas manuales sugeridas

- **Un solo rostro**: retrato frontal bien iluminado → debe indicar "Hay 1 rostro(s)" con recuadro y malla.
- **Imagen grupal**: foto con varias personas → el conteo debe coincidir y cada rostro tener su recuadro y sus puntos.
- **Sin imagen**: pulsar OCR o Rostros al abrir la app → debe mostrar "Selecciona primero una imagen".
- **OCR sin texto**: foto de un objeto sin letras → debe mostrar "No hay Texto".
- **QR sobre foto**: captura de pantalla de un QR abierta desde Galería → **Códigos** debe mostrar `[QR]` con su contenido.
- **Código de barras sobre foto**: foto del código de un producto → debe mostrar `[EAN-13]` (o el formato que corresponda) y los dígitos.
- **Varios códigos en una imagen**: deben listarse todos, numerados, cada uno con su recuadro.
- **Sin códigos**: foto cualquiera → debe mostrar "No se detecto ningun codigo".
- **En vivo**: apuntar a un QR y a un código de barras → el recuadro verde debe caer justo sobre el código y el panel actualizarse; **Usar código** debe volver a la pantalla principal con esa lectura.

## Notas

- El proyecto usa `startActivityForResult`/`onActivityResult`, marcados como deprecados en AndroidX, para mantener el código idéntico al del material de clase.
- La captura por cámara devuelve la miniatura del extra `"data"`, por lo que la resolución de la imagen tomada con cámara es menor que la de una imagen de galería. Para códigos de barras finos conviene usar **Galería** (resolución completa) o el modo **En vivo**.
- `ScannerActivity` está fijada a orientación vertical en el manifiesto. Si se permitiera rotar, el `rotationDegrees` que se pasa a `InputImage` seguiría siendo correcto porque viene del `ImageInfo` de cada fotograma, pero además convendría actualizar el `targetRotation` del `ImageAnalysis` en los cambios de orientación.

---

Proyecto académico — UTEQ, Software.
