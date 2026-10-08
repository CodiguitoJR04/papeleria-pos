package org.papeleria_pos.updater;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consulta el version.json del repo de GitHub.
 * Si hay una versión nueva, pregunta al usuario y la descarga/instala.
 */
public class Actualizador {

    /** URL del version.json en el release más reciente. */
    private static final String URL_VERSION =
            "https://github.com/CodiguitoJR04/papeleria-pos/releases/latest/download/version.json";

    /** Tiempo máximo de espera por petición (ms). */
    private static final int TIMEOUT_MS = 5000;

    /** Versión actual (leída de app.properties). */
    private static String versionActual = "0.0.0";

    /** Evita que se ejecute dos veces. */
    private static boolean yaCorrio = false;

    /* ============================================================
       Punto de entrada público
       ============================================================ */
    public static void verificarEnBackground() {
        if (yaCorrio) return;
        yaCorrio = true;

        versionActual = leerVersionLocal();

        new Thread(() -> {
            try {
                InfoVersion remota = consultarVersionRemota();
                if (remota == null) {
                    System.out.println("[UPDATE] No se pudo consultar la versión remota.");
                    return;
                }

                Version local  = new Version(versionActual);
                Version ultima = new Version(remota.version);

                if (!ultima.esMayorQue(local)) {
                    System.out.println("[UPDATE] Ya tienes la última versión (" + versionActual + ").");
                    return;
                }

                System.out.println("[UPDATE] Nueva versión disponible: " + remota.version);
                Platform.runLater(() -> mostrarDialogo(remota));

            } catch (Exception e) {
                System.err.println("[UPDATE] Error consultando actualización: " + e.getMessage());
            }
        }, "actualizador").start();
    }

    /* ============================================================
       Leer versión local desde app.properties
       ============================================================ */
    private static String leerVersionLocal() {
        try (InputStream in = Actualizador.class.getResourceAsStream("/app.properties")) {
            if (in == null) return "0.0.0";
            Properties p = new Properties();
            p.load(in);
            return p.getProperty("app.version", "0.0.0").trim();
        } catch (Exception e) {
            return "0.0.0";
        }
    }

    /* ============================================================
       Consultar version.json remoto
       ============================================================ */
    private static InfoVersion consultarVersionRemota() throws IOException {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URI(URL_VERSION).toURL().openConnection();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        con.setRequestMethod("GET");
        con.setConnectTimeout(TIMEOUT_MS);
        con.setReadTimeout(TIMEOUT_MS);
        con.setRequestProperty("User-Agent", "PapeleriaPOS-Updater");
        con.setInstanceFollowRedirects(true);

        if (con.getResponseCode() != 200) {
            System.err.println("[UPDATE] HTTP " + con.getResponseCode());
            return null;
        }

        String json;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) sb.append(linea).append('\n');
            json = sb.toString();
        }

        InfoVersion info = new InfoVersion();
        info.version = extraer(json, "version");
        info.url     = extraer(json, "url");
        info.notes   = extraer(json, "notes");

        if (info.version == null || info.url == null) return null;
        return info;
    }

    /** Extrae el valor de un campo del JSON. */
    private static String extraer(String json, String clave) {
        Pattern p = Pattern.compile("\"" + clave + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1).replace("\\n", "\n").replace("\\\"", "\"") : null;
    }

    /* ============================================================
       Diálogo de actualización
       ============================================================ */
    private static void mostrarDialogo(InfoVersion info) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Actualización disponible");
        alert.setHeaderText("Nueva versión " + info.version + " disponible");

        String contenido = "Tienes la versión " + versionActual + ".\n"
                + "¿Quieres actualizar a la " + info.version + "?";
        if (info.notes != null && !info.notes.isBlank()) {
            contenido += "\n\nNovedades:\n" + info.notes;
        }

        alert.setContentText(contenido);

        ButtonType btnActualizar = new ButtonType("Actualizar ahora", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnDespues    = new ButtonType("Más tarde",       ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnActualizar, btnDespues);

        alert.showAndWait().ifPresent(resp -> {
            if (resp == btnActualizar) {
                descargarEInstalar(info);
            }
        });
    }

    /* ============================================================
       Descarga + ejecución del instalador
       ============================================================ */
    private static void descargarEInstalar(InfoVersion info) {
        // Diálogo con barra de progreso
        ProgressBar barra = new ProgressBar(0);
        barra.setPrefWidth(360);

        Alert progreso = new Alert(Alert.AlertType.INFORMATION);
        progreso.setTitle("Descargando actualización");
        progreso.setHeaderText("Descargando " + info.version + "...");
        progreso.getDialogPane().setContent(new VBox(10, new Label("Por favor espera…"), barra));
        progreso.getDialogPane().setPadding(new Insets(10));
        progreso.getButtonTypes().setAll(new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));

        // Diálogo NO bloqueante — la descarga corre en hilo aparte
        new Thread(() -> {
            Path archivoDescargado = null;
            try {
                archivoDescargado = descargar(info.url, bytes -> {
                    double pct = bytes > 0
                            ? 1.0 * descargado / bytes
                            : -1;   // indeterminado
                    Platform.runLater(() -> barra.setProgress(pct));
                });

                Path finalArchivo = archivoDescargado;
                Platform.runLater(() -> {
                    progreso.close();
                    ejecutarInstalador(finalArchivo);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    progreso.close();
                    Alert err = new Alert(Alert.AlertType.ERROR,
                            "No se pudo descargar la actualización:\n" + e.getMessage());
                    err.setHeaderText(null);
                    err.showAndWait();
                });
            }
        }).start();

        progreso.show();
    }

    private static volatile long descargado = 0;

    private static Path descargar(String url, java.util.function.LongConsumer onProgress) throws IOException {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URI(url).toURL().openConnection();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        con.setConnectTimeout(TIMEOUT_MS);
        con.setReadTimeout(60000);
        con.setRequestProperty("User-Agent", "PapeleriaPOS-Updater");
        con.setInstanceFollowRedirects(true);

        long total = con.getContentLengthLong();
        descargado = 0;

        Path temp = Files.createTempFile("PapeleriaPOS-update-", ".exe");
        try (InputStream in = con.getInputStream();
             OutputStream out = Files.newOutputStream(temp)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                descargado += n;
                if (onProgress != null) onProgress.accept(total);
            }
        }
        return temp;
    }

    private static void ejecutarInstalador(Path exe) {
        try {
            // Ejecuta el instalador y cierra la app.
            // El instalador (WiX) sobreescribe la versión anterior por el mismo winUpgradeUuid.
            new ProcessBuilder(exe.toAbsolutePath().toString())
                    .start();
            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            Alert err = new Alert(Alert.AlertType.ERROR,
                    "No se pudo ejecutar el instalador:\n" + e.getMessage());
            err.setHeaderText(null);
            err.showAndWait();
        }
    }

    /* ============================================================
       DTO interno
       ============================================================ */
    private static class InfoVersion {
        String version;
        String url;
        String notes;
    }
}