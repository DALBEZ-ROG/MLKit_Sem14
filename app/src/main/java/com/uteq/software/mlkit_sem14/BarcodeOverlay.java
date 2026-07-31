package com.uteq.software.mlkit_sem14;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.barcode.common.Barcode;

import java.util.ArrayList;
import java.util.List;

/**
 * Capa transparente sobre el PreviewView que marca los codigos detectados.
 *
 * Las cajas que devuelve ML Kit estan en coordenadas de la imagen de analisis
 * (ya enderezada segun la rotacion), que no coincide ni en tamano ni en relacion
 * de aspecto con la vista. Como el PreviewView usa FILL_CENTER, aqui se replica
 * ese mismo encuadre: se escala por el lado que llena la vista y se centra, de
 * modo que el recuadro caiga justo sobre el codigo que ve el usuario.
 */
public class BarcodeOverlay extends View {

    private final List<Rect> cajas = new ArrayList<>();
    private final List<String> etiquetas = new ArrayList<>();

    private final Paint paintCaja = new Paint();
    private final Paint paintTexto = new Paint();
    private final Paint paintFondo = new Paint();

    /** Dimensiones de la imagen analizada, ya enderezada. */
    private int anchoFuente;
    private int altoFuente;

    public BarcodeOverlay(Context context) {
        this(context, null);
    }

    public BarcodeOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        float densidad = getResources().getDisplayMetrics().density;

        paintCaja.setColor(Color.GREEN);
        paintCaja.setStyle(Paint.Style.STROKE);
        paintCaja.setStrokeWidth(3 * densidad);
        paintCaja.setAntiAlias(true);

        paintTexto.setColor(Color.WHITE);
        paintTexto.setStyle(Paint.Style.FILL);
        paintTexto.setFakeBoldText(true);
        paintTexto.setAntiAlias(true);
        paintTexto.setTextSize(14 * densidad);

        paintFondo.setColor(Color.argb(180, 0, 0, 0));
        paintFondo.setStyle(Paint.Style.FILL);
    }

    /**
     * Actualiza lo que se dibuja. {@code anchoFuente} y {@code altoFuente} son las
     * medidas de la imagen enderezada con la que se detectaron esos codigos.
     */
    public void actualizar(List<Barcode> codigos, int anchoFuente, int altoFuente) {
        this.anchoFuente = anchoFuente;
        this.altoFuente = altoFuente;

        cajas.clear();
        etiquetas.clear();
        for (Barcode codigo : codigos) {
            Rect caja = codigo.getBoundingBox();
            if (caja == null) continue;
            cajas.add(caja);
            etiquetas.add(Codigos.nombreFormato(codigo.getFormat()));
        }
        postInvalidate();
    }

    public void limpiar() {
        cajas.clear();
        etiquetas.clear();
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cajas.isEmpty() || anchoFuente == 0 || altoFuente == 0) return;

        // Mismo encuadre que FILL_CENTER: llenar la vista y recortar lo que sobra.
        float escala = Math.max(
                (float) getWidth() / anchoFuente,
                (float) getHeight() / altoFuente);
        float desplazamientoX = (getWidth() - anchoFuente * escala) / 2f;
        float desplazamientoY = (getHeight() - altoFuente * escala) / 2f;

        for (int i = 0; i < cajas.size(); i++) {
            Rect caja = cajas.get(i);

            float izquierda = caja.left * escala + desplazamientoX;
            float arriba = caja.top * escala + desplazamientoY;
            float derecha = caja.right * escala + desplazamientoX;
            float abajo = caja.bottom * escala + desplazamientoY;

            canvas.drawRect(izquierda, arriba, derecha, abajo, paintCaja);

            String etiqueta = etiquetas.get(i);
            float anchoTexto = paintTexto.measureText(etiqueta);
            float altoTexto = paintTexto.getTextSize();
            float margen = altoTexto * 0.3f;

            // Si no hay sitio arriba, la etiqueta se coloca bajo el recuadro.
            float baseTexto = arriba - margen;
            if (baseTexto - altoTexto < 0) baseTexto = abajo + altoTexto + margen;

            canvas.drawRect(izquierda, baseTexto - altoTexto - margen,
                    izquierda + anchoTexto + margen * 2, baseTexto + margen, paintFondo);
            canvas.drawText(etiqueta, izquierda + margen, baseTexto, paintTexto);
        }
    }
}
