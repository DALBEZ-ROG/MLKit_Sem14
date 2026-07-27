# MLKit_Sem14 — Google ML Kit (Rostros + Malla Facial + OCR)

App Android en Java que usa **Google ML Kit** para reconocer texto (OCR) y detectar rostros con recuadro y malla facial (contornos), a partir de una imagen tomada con la cámara o elegida de la galería.

## Características

- Seleccionar imagen desde la **galería** o capturarla con la **cámara**.
- **OCR**: reconocimiento de texto latino sobre la imagen, mostrando el resultado en pantalla.
- **Detección de rostros**: conteo de rostros ("Hay X rostro(s)") y recuadro rojo sobre cada uno.
- **Malla facial**: puntos verdes sobre los contornos de cada rostro (óvalo de la cara, cejas, ojos, labios, puente y base de la nariz).
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
