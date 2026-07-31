package com.uteq.software.mlkit_sem14;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Escaneo de codigos en vivo con CameraX + ML Kit.
 *
 * CameraX entrega cada fotograma al caso de uso ImageAnalysis y ese fotograma se pasa
 * directamente al escaner, sin sacar ninguna foto. Se usa STRATEGY_KEEP_ONLY_LATEST:
 * mientras ML Kit analiza un fotograma, los que llegan se descartan y solo se conserva
 * el mas reciente, de modo que el preview nunca se atasca aunque el analisis vaya
 * mas lento que la camara.
 */
public class ScannerActivity extends AppCompatActivity {

    /** Clave con la que se devuelve el codigo elegido a MainActivity. */
    public static final String EXTRA_CODIGO = "codigo";

    private static final int REQUEST_CAMERA_PERMISSION = 300;

    private PreviewView preview;
    private BarcodeOverlay overlay;
    private TextView txtLive;
    private Button btUsar;

    private BarcodeScanner scanner;
    private ExecutorService ejecutorAnalisis;

    /** Ultimo codigo leido; es lo que se devuelve al pulsar "Usar codigo". */
    private String ultimoCodigo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scanner);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scanner_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        preview = findViewById(R.id.preview);
        overlay = findViewById(R.id.overlay);
        txtLive = findViewById(R.id.txtLive);
        txtLive.setMovementMethod(new ScrollingMovementMethod());
        btUsar = findViewById(R.id.btUsar);

        btUsar.setOnClickListener(v -> {
            Intent resultado = new Intent();
            resultado.putExtra(EXTRA_CODIGO, ultimoCodigo);
            setResult(RESULT_OK, resultado);
            finish();
        });
        findViewById(R.id.btCerrar).setOnClickListener(v -> finish());

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build();
        scanner = BarcodeScanning.getClient(options);
        ejecutorAnalisis = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CAMERA_PERMISSION) return;

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            Toast.makeText(this, "Se necesita permiso de camara para escanear",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Enlaza el preview y el analisis al ciclo de vida de esta pantalla: CameraX abre
     * y cierra la camara solo, sin que haya que liberarla a mano.
     */
    private void iniciarCamara() {
        final ListenableFuture<ProcessCameraProvider> futuro =
                ProcessCameraProvider.getInstance(this);

        futuro.addListener(() -> {
            try {
                ProcessCameraProvider proveedor = futuro.get();

                Preview vistaPrevia = new Preview.Builder().build();
                vistaPrevia.setSurfaceProvider(preview.getSurfaceProvider());

                ImageAnalysis analisis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();
                analisis.setAnalyzer(ejecutorAnalisis, this::analizar);

                proveedor.unbindAll();
                proveedor.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                        vistaPrevia, analisis);
            } catch (Exception e) {
                Toast.makeText(this, "No se pudo iniciar la camara", Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Analiza un fotograma. Es obligatorio cerrar el ImageProxy siempre —tambien si
     * falla el analisis—, porque hasta que no se cierra CameraX no entrega el siguiente.
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analizar(@NonNull ImageProxy imageProxy) {
        Image fotograma = imageProxy.getImage();
        if (fotograma == null) {
            imageProxy.close();
            return;
        }

        int rotacion = imageProxy.getImageInfo().getRotationDegrees();
        InputImage image = InputImage.fromMediaImage(fotograma, rotacion);

        // Con 90 o 270 grados la imagen se endereza girando: ancho y alto se intercambian.
        boolean girada = rotacion == 90 || rotacion == 270;
        final int anchoFuente = girada ? imageProxy.getHeight() : imageProxy.getWidth();
        final int altoFuente = girada ? imageProxy.getWidth() : imageProxy.getHeight();

        scanner.process(image)
                .addOnSuccessListener(codigos -> mostrar(codigos, anchoFuente, altoFuente))
                .addOnCompleteListener(tarea -> imageProxy.close());
    }

    private void mostrar(List<Barcode> codigos, int anchoFuente, int altoFuente) {
        if (codigos.isEmpty()) {
            overlay.limpiar();
            return;
        }

        overlay.actualizar(codigos, anchoFuente, altoFuente);

        String resultados = "";
        for (int i = 0; i < codigos.size(); i++) {
            Barcode codigo = codigos.get(i);
            resultados = resultados
                    + "[" + Codigos.nombreFormato(codigo.getFormat()) + "] "
                    + Codigos.nombreTipo(codigo.getValueType()) + "\n"
                    + Codigos.texto(codigo) + "\n";
            if (i < codigos.size() - 1) resultados = resultados + "\n";
        }

        ultimoCodigo = resultados;
        txtLive.setText(resultados);
        txtLive.scrollTo(0, 0);
        btUsar.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ejecutorAnalisis != null) ejecutorAnalisis.shutdown();
        if (scanner != null) scanner.close();
    }
}
