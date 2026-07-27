# MLKit_Sem14 — Google ML Kit (Rostros + Malla Facial + OCR)

App Android en Java que usa **Google ML Kit** para reconocer texto (OCR) y detectar rostros con recuadro y malla facial (contornos), a partir de una imagen tomada con la cámara o elegida de la galería.

## Características

- Seleccionar imagen desde la **galería** o capturarla con la **cámara**.
- **OCR**: reconocimiento de texto latino sobre la imagen, respetando la estructura de líneas y párrafos del original (si la imagen tiene 3 líneas, el resultado muestra 3 líneas).
- **Placas ecuatorianas**: detecta la matrícula de un vehículo, la encierra en un recuadro rojo y muestra un porcentaje de precisión estimada de la lectura.
- **Detección de rostros**: conteo de rostros ("Hay X rostro(s)") y recuadro rojo sobre cada uno.
- **Malla facial**: puntos verdes sobre los contornos de **todos** los rostros de la foto (óvalo de la cara, cejas, ojos, labios, puente y base de la nariz).
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
```

> `face-detection` se actualizó de `16.1.5` a `16.1.7` porque en la versión anterior la librería nativa `libface_detector_v2_jni.so` (arm64-v8a) no estaba alineada a 16 KB y Android mostraba el aviso de compatibilidad.

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
│   └── MainActivity.java        // selección de imagen, OCR y detección de rostros
├── res/layout/
│   └── activity_main.xml        // logo, botones Galería/Cámara, imagen, resultados, OCR/Rostros
├── res/drawable/
│   └── ic_mlkit.png             // logo mostrado en la parte superior
└── AndroidManifest.xml
```

### Métodos principales de `MainActivity`

| Método | Descripción |
|---|---|
| `abrirGaleria(View)` | Lanza un `ACTION_PICK` sobre `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`. |
| `abrirCamara(View)` | Lanza `MediaStore.ACTION_IMAGE_CAPTURE`. |
| `onActivityResult(...)` | Recibe el `Bitmap` y lo muestra en el `ImageView`. |
| `OCRfx(View)` | Procesa la imagen con `TextRecognition` y vuelca el texto en `txtresults`. |
| `Rostrosfx(View)` | Procesa la imagen con `FaceDetection` (`PERFORMANCE_MODE_ACCURATE` + `CONTOUR_MODE_ALL`), cuenta rostros y dibuja recuadro + puntos de contorno sobre el `Canvas`. |
| `Placafx(View)` | Reutiliza el OCR, filtra las cadenas con formato de placa ecuatoriana, dibuja un recuadro rojo alrededor de cada una y estima su precisión. |

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

## Cómo ejecutar

1. Clonar el repositorio y abrirlo en Android Studio.
2. Sincronizar Gradle (descarga las dependencias de ML Kit).
3. Ejecutar en un dispositivo o emulador con API 24 o superior.
4. Pulsar **Galería** o **Cámara**, y luego **OCR** o **Rostros**.

O desde la terminal:

```bash
./gradlew assembleDebug
```

## Pruebas manuales sugeridas

- **Un solo rostro**: retrato frontal bien iluminado → debe indicar "Hay 1 rostro(s)" con recuadro y malla.
- **Imagen grupal**: foto con varias personas → el conteo debe coincidir y cada rostro tener su recuadro y sus puntos.
- **Sin imagen**: pulsar OCR o Rostros al abrir la app → debe mostrar "Selecciona primero una imagen".
- **OCR sin texto**: foto de un objeto sin letras → debe mostrar "No hay Texto".

## Notas

- El proyecto usa `startActivityForResult`/`onActivityResult`, marcados como deprecados en AndroidX, para mantener el código idéntico al del material de clase.
- La captura por cámara devuelve la miniatura del extra `"data"`, por lo que la resolución de la imagen tomada con cámara es menor que la de una imagen de galería.

---

Proyecto académico — UTEQ, Software.
