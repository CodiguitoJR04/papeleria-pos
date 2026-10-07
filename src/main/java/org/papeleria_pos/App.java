package org.papeleria_pos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.papeleria_pos.controller.MainController;

import java.io.IOException;
import java.util.Objects;

public class App extends Application {

    private static App instancia;
    private Stage stage;

    public static App getInstance() { return instancia; }

    @Override
    public void start(Stage stage) {
        instancia = this;
        this.stage = stage;
        stage.setTitle("Papelería · Punto de Venta");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        mostrarLogin();
        stage.show();
    }

    /* ============================================================
       Pantalla de Login
       ============================================================ */
    public void mostrarLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    App.class.getResource("/org/papeleria_pos/fxml/loginView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 960, 640);
            aplicarEstilos(scene);

            stage.setScene(scene);
            stage.setWidth(960);
            stage.setHeight(640);
            stage.centerOnScreen();
            stage.setTitle("Papelería · Iniciar sesión");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ============================================================
       Pantalla principal (panel admin / POS)
       ============================================================ */
    public void mostrarMain(String rol, String nombre) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    App.class.getResource("/org/papeleria_pos/fxml/main.fxml"));
            Parent root = loader.load();

            // Pasa rol/nombre al MainController
            MainController ctrl = loader.getController();
            ctrl.setSesion(rol, nombre);

            Scene scene = new Scene(root, 1400, 860);
            aplicarEstilos(scene);

            stage.setScene(scene);
            stage.setWidth(1400);
            stage.setHeight(860);
            stage.centerOnScreen();
            stage.setTitle("Papelería · " + rol);
            stage.setMinWidth(1100);
            stage.setMinHeight(700);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String[] STYLESHEETS = {
            "/org/papeleria_pos/styles/base.css",
            "/org/papeleria_pos/styles/layout.css",
            "/org/papeleria_pos/styles/components.css",
            "/org/papeleria_pos/styles/views.css"
    };

    private void aplicarEstilos(Scene scene) {
        for (String css : STYLESHEETS) {
            var url = App.class.getResource(css);
            if (url != null) {
                scene.getStylesheets().add(url.toExternalForm());
            } else {
                System.err.println("⚠️ CSS no encontrado: " + css);
            }
        }
    }
    public void mostrarPOS() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    App.class.getResource("/org/papeleria_pos/fxml/punto_venta.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/org/papeleria_pos/styles/base.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/org/papeleria_pos/styles/layout.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/org/papeleria_pos/styles/components.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/org/papeleria_pos/styles/views.css")).toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Papelería · Punto de Venta");
            stage.centerOnScreen();
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) { launch(args); }
}