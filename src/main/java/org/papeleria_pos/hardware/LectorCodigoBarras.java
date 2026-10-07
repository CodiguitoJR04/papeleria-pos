package org.papeleria_pos.hardware;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.function.Consumer;

public class LectorCodigoBarras {
    /** Tiempo máximo (ms) entre teclas para considerarlo un escaneo. */
    private static final long UMBRAL_MS = 50;

    /** Longitud mínima para considerarlo código de barras. */
    private static final int LONGITUD_MINIMA = 4;

    private LectorCodigoBarras() {}

    /**
     * Vincula un TextField al lector. Cuando la pistola "escribe"
     * (rápido + Enter), se llama al callback con el código limpio.
     */
    public static void attach(TextField field, Consumer<String> onScan) {

        final StringBuilder buffer = new StringBuilder();
        final long[] ultimaTecla = {0};

        field.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            long ahora = System.currentTimeMillis();
            long delta = ahora - ultimaTecla[0];
            ultimaTecla[0] = ahora;

            char c = e.getCharacter().charAt(0);

            // Si pasa mucho tiempo, es tecleo humano → resetear
            if (delta > UMBRAL_MS && buffer.length() > 0) {
                buffer.setLength(0);
            }

            // Solo aceptamos dígitos, letras y guiones
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                buffer.append(c);
            }
        });

        field.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String codigo = buffer.toString().trim();
                buffer.setLength(0);

                if (codigo.length() >= LONGITUD_MINIMA) {
                    e.consume();
                    Platform.runLater(() -> onScan.accept(codigo));
                }
            }
        });
    }

    /**
     * Fuerza el foco y selecciona el contenido del campo.
     * Útil para el botón de la pistola.
     */
    public static void enfocar(TextField field) {
        Platform.runLater(() -> {
            field.requestFocus();
            field.selectAll();
        });
    }
}
