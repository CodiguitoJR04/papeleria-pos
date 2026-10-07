package org.papeleria_pos.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label lblReloj, lblNombre,lblRol;
    @FXML private Button navDashboard, navInventario, navCategorias, navReportes, navUsuarios, navProveedores;

    private List<Button> navItems;

    @FXML
    public void initialize() {
        navItems = List.of(navDashboard, navInventario, navCategorias, navReportes, navUsuarios);

        // Reloj
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(30), e ->
                lblReloj.setText(LocalTime.now().format(fmt))));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
        lblReloj.setText(LocalTime.now().format(fmt));

        irDashboard(); // vista inicial
    }

    @FXML private void irDashboard()  { mostrar("dashboard",  navDashboard); }
    @FXML private void irInventario() { mostrar("inventario", navInventario); }
    @FXML private void irCategorias() { mostrar("categorias", navCategorias); }
    @FXML private void irReportes()   { mostrar("reportes",   navReportes); }
    @FXML private void irUsuarios()   { mostrar("usuarios",   navUsuarios); }
    @FXML private void irProveedores() {mostrar("proveedores", navProveedores);}
    @FXML private void cerrarSesion() { /* TODO */ }

    private void mostrar(String vista, Button botonActivo) {
        String archivo = "/org/papeleria_pos/fxml/" + vista.toLowerCase() + ".fxml";
        var url = MainController.class.getResource(archivo);

        if (url == null) {
            System.err.println("❌ No se encontró: " + archivo);
            System.err.println("   Verifica que exista en src/main/resources" + archivo);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(url);
            Node view = loader.load();
            contentArea.getChildren().setAll(view);

            navItems.forEach(b -> b.getStyleClass().remove("active"));
            if (!botonActivo.getStyleClass().contains("active"))
                botonActivo.getStyleClass().add("active");

        } catch (Exception e) {
            System.err.println("❌ Error cargando " + vista + ":");
            e.printStackTrace();
        }
    }
    public void setSesion(String rol, String nombre) {
        // Actualiza los labels del header con el usuario logueado
        // (En main.fxml, dale fx:id="lblNombre" y fx:id="lblRol" al label correspondiente)
        if (lblNombre != null) lblNombre.setText(nombre);
        if (lblRol    != null) lblRol.setText(rol);

        // Oculta secciones que el cajero no debe ver
        if (!"Administrador".equals(rol)) {
            navUsuarios.setVisible(false);   navUsuarios.setManaged(false);
            navReportes.setVisible(false);   navReportes.setManaged(false);
            navCategorias.setVisible(false); navCategorias.setManaged(false);
        }
    }
    public void navegarA(String seccion) {
        if (seccion == null) return;
        switch (seccion) {
            case "dashboard":  irDashboard();  break;
            case "inventario": irInventario(); break;
            case "categorias": irCategorias(); break;
            case "reportes":   irReportes();   break;
            case "usuarios":   irUsuarios();   break;
        }
    }
}