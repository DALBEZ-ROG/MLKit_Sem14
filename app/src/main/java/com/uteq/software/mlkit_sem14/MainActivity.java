package com.uteq.software.mlkit_sem14;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity
        implements OnSuccessListener<Text>, OnFailureListener {

    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;
    public static int REQUEST_SCANNER = 333;

    /**
     * Placa ecuatoriana: 3 letras + 3 o 4 digitos (ABC-123 antiguas, ABC-1234 actuales).
     * La primera letra es el codigo de la provincia; en Ecuador hay 24 provincias y
     * las letras D y F no se usan como codigo provincial.
     */
    private static final String PROVINCIAS = "ABCEGHIJKLMNOPQRSTUVWXYZ";
    private static final Pattern PLACA_EC =
            Pattern.compile("[" + PROVINCIAS + "][A-Z]{2}[0-9]{3,4}");

    private Bitmap mSelectedImage;
    private ImageView mImageView;
    private TextView txtResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtResults = findViewById(R.id.txtresults);
        txtResults.setMovementMethod(new ScrollingMovementMethod());
        mImageView = findViewById(R.id.image_view);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
    }

    public void abrirGaleria(View view) {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }

    public void abrirCamara(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    /** Abre el escaneo en vivo con CameraX (ScannerActivity). */
    public void EnVivofx(View view) {
        startActivityForResult(new Intent(this, ScannerActivity.class), REQUEST_SCANNER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && null != data) {
            if (requestCode == REQUEST_SCANNER) {
                String codigo = data.getStringExtra(ScannerActivity.EXTRA_CODIGO);
                txtResults.setText(codigo != null ? codigo : "No se recibio ningun codigo");
                txtResults.scrollTo(0, 0);
                return;
            }

            try {
                if (requestCode == REQUEST_CAMERA)
                    mSelectedImage = (Bitmap) data.getExtras().get("data");
                else
                    mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());

                mImageView.setImageBitmap(mSelectedImage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void OCRfx(View v) {
        if (mSelectedImage == null) {
            txtResults.setText("Selecciona primero una imagen");
            return;
        }

        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    public void onSuccess(Text text) {
        List<Text.TextBlock> blocks = text.getTextBlocks();
        String resultados = "";
        if (blocks.size() == 0) {
            resultados = "No hay Texto";
        } else {
            // Se respeta la estructura de la imagen: un salto de linea por cada
            // linea detectada, y una linea en blanco entre bloques (parrafos).
            for (int i = 0; i < blocks.size(); i++) {
                List<Text.Line> lines = blocks.get(i).getLines();
                for (int j = 0; j < lines.size(); j++) {
                    resultados = resultados + lines.get(j).getText() + "\n";
                }
                if (i < blocks.size() - 1) {
                    resultados = resultados + "\n";
                }
            }
        }
        txtResults.setText(resultados);
        txtResults.scrollTo(0, 0);
    }

    public void Rostrosfx(View v) {
        if (mSelectedImage == null) {
            txtResults.setText("Selecciona primero una imagen");
            return;
        }

        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);
        detector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (faces.size() == 0) {
                            txtResults.setText("No Hay rostros");
                        } else {
                            txtResults.setText("Hay " + faces.size() + " rostro(s)");
                        }

                        dibujarRostros(faces);
                    }
                })
                .addOnFailureListener(this);
    }

    /**
     * Dibuja el recuadro de cada rostro y la malla de contornos de TODOS ellos.
     *
     * ML Kit suele devolver los contornos solo del rostro mas prominente de la foto,
     * asi que para las caras que llegan sin contornos se lanza una segunda deteccion
     * sobre el recorte de esa cara (ampliado, porque el modelo de contornos necesita
     * un rostro de buen tamano) y los puntos obtenidos se trasladan a la imagen
     * completa. La imagen final se muestra una sola vez, cuando todas las detecciones
     * pendientes han terminado.
     */
    private void dibujarRostros(List<Face> faces) {
        Bitmap bitmap = mSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);

        Paint paintPuntos = new Paint();
        paintPuntos.setColor(Color.GREEN);
        paintPuntos.setStyle(Paint.Style.FILL);

        float radio = 3 * getResources().getDisplayMetrics().density;

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();
        FaceDetector detector = FaceDetection.getClient(options);

        List<Task<List<Face>>> pendientes = new ArrayList<>();

        for (Face rostro : faces) {
            canvas.drawRect(rostro.getBoundingBox(), paint);

            List<FaceContour> contornos = rostro.getAllContours();
            if (!contornos.isEmpty()) {
                dibujarContornos(canvas, contornos, 0, 0, 1f, radio, paintPuntos);
                continue;
            }

            final Rect recorte = recorteDeRostro(rostro.getBoundingBox());
            if (recorte == null) continue;

            // El modelo de contornos rinde mal con caras pequenas: se amplia el recorte.
            final float escala = Math.min(4f, Math.max(1f, 240f / Math.min(recorte.width(), recorte.height())));

            Bitmap cara = Bitmap.createBitmap(mSelectedImage,
                    recorte.left, recorte.top, recorte.width(), recorte.height());
            if (escala > 1f) {
                cara = Bitmap.createScaledBitmap(cara,
                        Math.round(recorte.width() * escala),
                        Math.round(recorte.height() * escala), true);
            }

            pendientes.add(detector.process(InputImage.fromBitmap(cara, 0))
                    .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                        @Override
                        public void onSuccess(List<Face> subRostros) {
                            for (Face sub : subRostros) {
                                dibujarContornos(canvas, sub.getAllContours(),
                                        recorte.left, recorte.top, escala, radio, paintPuntos);
                            }
                        }
                    }));
        }

        if (pendientes.isEmpty()) {
            mImageView.setImageBitmap(bitmap);
        } else {
            Tasks.whenAllComplete(pendientes)
                    .addOnCompleteListener(tarea -> mImageView.setImageBitmap(bitmap));
        }
    }

    /**
     * Pinta los puntos de los contornos. Los puntos vienen en coordenadas del recorte
     * ampliado, por lo que se dividen por la escala y se desplazan al origen del recorte.
     */
    private void dibujarContornos(Canvas canvas, List<FaceContour> contornos,
                                  int offsetX, int offsetY, float escala,
                                  float radio, Paint paintPuntos) {
        for (FaceContour contorno : contornos) {
            List<PointF> puntos = contorno.getPoints();
            for (PointF punto : puntos) {
                canvas.drawCircle(offsetX + punto.x / escala,
                        offsetY + punto.y / escala, radio, paintPuntos);
            }
        }
    }

    /** Caja del rostro con un 15% de margen, recortada a los limites de la imagen. */
    private Rect recorteDeRostro(Rect caja) {
        if (caja == null) return null;

        int margenX = Math.round(caja.width() * 0.15f);
        int margenY = Math.round(caja.height() * 0.15f);

        Rect r = new Rect(
                Math.max(0, caja.left - margenX),
                Math.max(0, caja.top - margenY),
                Math.min(mSelectedImage.getWidth(), caja.right + margenX),
                Math.min(mSelectedImage.getHeight(), caja.bottom + margenY));

        if (r.width() < 32 || r.height() < 32) return null;
        return r;
    }

    public void Placafx(View v) {
        if (mSelectedImage == null) {
            txtResults.setText("Selecciona primero una imagen");
            return;
        }

        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        List<Placa> placas = buscarPlacas(text);

                        if (placas.isEmpty()) {
                            txtResults.setText("No se detecto ninguna placa");
                            mImageView.setImageBitmap(mSelectedImage);
                            return;
                        }

                        String resultados = "";
                        for (Placa placa : placas) {
                            resultados = resultados + "Placa: " + formatearPlaca(placa.texto)
                                    + "   Precision: " + placa.precision + "%\n";
                        }
                        txtResults.setText(resultados);
                        txtResults.scrollTo(0, 0);

                        Bitmap bitmap = mSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(bitmap);

                        Paint paintPlaca = new Paint();
                        paintPlaca.setColor(Color.RED);
                        paintPlaca.setStrokeWidth(2);
                        paintPlaca.setStyle(Paint.Style.STROKE);

                        Paint paintEtiqueta = new Paint();
                        paintEtiqueta.setColor(Color.RED);
                        paintEtiqueta.setStyle(Paint.Style.FILL);
                        paintEtiqueta.setFakeBoldText(true);
                        paintEtiqueta.setAntiAlias(true);
                        paintEtiqueta.setTextSize(Math.max(16f, bitmap.getWidth() / 28f));

                        for (Placa placa : placas) {
                            canvas.drawRect(placa.caja, paintPlaca);

                            String etiqueta = placa.precision + "%";
                            float y = placa.caja.top - paintEtiqueta.getTextSize() / 4f;
                            if (y < paintEtiqueta.getTextSize()) {
                                y = placa.caja.bottom + paintEtiqueta.getTextSize();
                            }
                            canvas.drawText(etiqueta, placa.caja.left, y, paintEtiqueta);
                        }

                        mImageView.setImageBitmap(bitmap);
                    }
                })
                .addOnFailureListener(this);
    }

    /** Placa detectada: texto normalizado, su caja en la imagen y la precision estimada. */
    private static class Placa {
        final String texto;
        final Rect caja;
        final int precision;

        Placa(String texto, Rect caja, int precision) {
            this.texto = texto;
            this.caja = caja;
            this.precision = precision;
        }
    }

    /**
     * Recorre el texto reconocido buscando placas ecuatorianas. La placa puede venir
     * como una linea completa ("PBA-1234"), como un solo elemento, o partida en dos
     * elementos ("PBA" + "1234"); en ese ultimo caso se unen las dos cajas.
     */
    private List<Placa> buscarPlacas(Text text) {
        List<Placa> placas = new ArrayList<>();

        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String placaLinea = reconocerPlaca(line.getText());
                if (placaLinea != null && line.getBoundingBox() != null) {
                    placas.add(new Placa(placaLinea, line.getBoundingBox(),
                            calcularPrecision(line.getText(), placaLinea, line.getConfidence())));
                    continue;
                }

                List<Text.Element> elements = line.getElements();
                for (int k = 0; k < elements.size(); k++) {
                    Text.Element actual = elements.get(k);
                    Rect caja = actual.getBoundingBox();
                    if (caja == null) continue;

                    String placa = reconocerPlaca(actual.getText());
                    if (placa != null) {
                        placas.add(new Placa(placa, caja,
                                calcularPrecision(actual.getText(), placa, actual.getConfidence())));
                        continue;
                    }

                    if (k + 1 < elements.size() && elements.get(k + 1).getBoundingBox() != null) {
                        Text.Element siguiente = elements.get(k + 1);
                        String textoUnido = actual.getText() + siguiente.getText();
                        String unida = reconocerPlaca(textoUnido);
                        if (unida != null) {
                            Rect union = new Rect(caja);
                            union.union(siguiente.getBoundingBox());
                            float confianza =
                                    (actual.getConfidence() + siguiente.getConfidence()) / 2f;
                            placas.add(new Placa(unida, union,
                                    calcularPrecision(textoUnido, unida, confianza)));
                            k++;
                        }
                    }
                }
            }
        }
        return placas;
    }

    /**
     * Precision estimada de la lectura, en porcentaje. Parte de la confianza que reporta
     * ML Kit para ese texto y descuenta 5 puntos por cada caracter que hubo que corregir
     * para que encajara en el formato de placa (mientras mas se corrigio, menos seguro es
     * que lo leido coincida con la placa real del vehiculo).
     */
    private int calcularPrecision(String original, String placa, float confianzaOcr) {
        float base = confianzaOcr > 0f ? confianzaOcr : 0.85f;

        int correcciones = 0;
        String limpio = normalizar(original);
        if (limpio.length() == placa.length()) {
            for (int i = 0; i < placa.length(); i++) {
                if (limpio.charAt(i) != placa.charAt(i)) correcciones++;
            }
        }

        float valor = base * 100f - correcciones * 5f;
        if (valor < 0f) valor = 0f;
        if (valor > 100f) valor = 100f;
        return Math.round(valor);
    }

    private String normalizar(String texto) {
        return texto.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Devuelve la placa normalizada si el texto corresponde al formato ecuatoriano,
     * o null si no coincide. Intenta primero el texto tal cual y, si falla, corrige
     * las confusiones tipicas del OCR (O/0, I/1, S/5, B/8, Z/2).
     */
    private String reconocerPlaca(String texto) {
        String limpio = normalizar(texto);
        if (PLACA_EC.matcher(limpio).matches()) return limpio;

        String corregido = corregirOCR(limpio);
        if (PLACA_EC.matcher(corregido).matches()) return corregido;

        return null;
    }

    private String corregirOCR(String texto) {
        if (texto.length() < 6 || texto.length() > 7) return texto;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (i < 3) {
                // Las 3 primeras posiciones son letras
                if (c == '0') c = 'O';
                else if (c == '1') c = 'I';
                else if (c == '5') c = 'S';
                else if (c == '8') c = 'B';
                else if (c == '2') c = 'Z';
            } else {
                // El resto son digitos
                if (c == 'O' || c == 'Q' || c == 'D') c = '0';
                else if (c == 'I' || c == 'L') c = '1';
                else if (c == 'S') c = '5';
                else if (c == 'B') c = '8';
                else if (c == 'Z') c = '2';
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private String formatearPlaca(String placa) {
        return placa.substring(0, 3) + "-" + placa.substring(3);
    }

    /**
     * Decodifica los codigos de la imagen seleccionada. Un mismo escaner de ML Kit
     * cubre codigos de barras 1D (EAN, UPC, Code 39/93/128, ITF, Codabar) y 2D
     * (QR, Aztec, Data Matrix, PDF417), asi que se piden todos los formatos.
     */
    public void Codigosfx(View v) {
        if (mSelectedImage == null) {
            txtResults.setText("Selecciona primero una imagen");
            return;
        }

        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        scanner.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> codigos) {
                        if (codigos.isEmpty()) {
                            txtResults.setText("No se detecto ningun codigo");
                            mImageView.setImageBitmap(mSelectedImage);
                            return;
                        }

                        String resultados = "";
                        for (int i = 0; i < codigos.size(); i++) {
                            Barcode codigo = codigos.get(i);
                            resultados = resultados
                                    + "Codigo " + (i + 1) + " [" + Codigos.nombreFormato(codigo.getFormat()) + "]\n"
                                    + "Tipo: " + Codigos.nombreTipo(codigo.getValueType()) + "\n"
                                    + "Valor: " + Codigos.texto(codigo) + "\n";
                            if (i < codigos.size() - 1) resultados = resultados + "\n";
                        }
                        txtResults.setText(resultados);
                        txtResults.scrollTo(0, 0);

                        dibujarCodigos(codigos);
                    }
                })
                .addOnFailureListener(this);
    }

    /** Marca cada codigo: su caja en rojo y el contorno real (4 esquinas) en verde. */
    private void dibujarCodigos(List<Barcode> codigos) {
        Bitmap bitmap = mSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);

        Paint paintCaja = new Paint();
        paintCaja.setColor(Color.RED);
        paintCaja.setStrokeWidth(4);
        paintCaja.setStyle(Paint.Style.STROKE);

        Paint paintContorno = new Paint();
        paintContorno.setColor(Color.GREEN);
        paintContorno.setStrokeWidth(4);
        paintContorno.setStyle(Paint.Style.STROKE);
        paintContorno.setAntiAlias(true);

        Paint paintEtiqueta = new Paint();
        paintEtiqueta.setColor(Color.RED);
        paintEtiqueta.setStyle(Paint.Style.FILL);
        paintEtiqueta.setFakeBoldText(true);
        paintEtiqueta.setAntiAlias(true);
        paintEtiqueta.setTextSize(Math.max(16f, bitmap.getWidth() / 28f));

        for (int i = 0; i < codigos.size(); i++) {
            Barcode codigo = codigos.get(i);
            Rect caja = codigo.getBoundingBox();
            if (caja == null) continue;

            canvas.drawRect(caja, paintCaja);

            Point[] esquinas = codigo.getCornerPoints();
            if (esquinas != null && esquinas.length == 4) {
                for (int j = 0; j < 4; j++) {
                    Point a = esquinas[j];
                    Point b = esquinas[(j + 1) % 4];
                    canvas.drawLine(a.x, a.y, b.x, b.y, paintContorno);
                }
            }

            String etiqueta = (i + 1) + ". " + Codigos.nombreFormato(codigo.getFormat());
            float y = caja.top - paintEtiqueta.getTextSize() / 4f;
            if (y < paintEtiqueta.getTextSize()) {
                y = caja.bottom + paintEtiqueta.getTextSize();
            }
            canvas.drawText(etiqueta, caja.left, y, paintEtiqueta);
        }

        mImageView.setImageBitmap(bitmap);
    }

    public void onFailure(@NonNull Exception e) {
        txtResults.setText("Error al procesar imagen");
    }
}
