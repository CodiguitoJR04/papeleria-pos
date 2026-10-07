package org.papeleria_pos.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import org.papeleria_pos.App;
import org.papeleria_pos.dao.Interface.IUsuarioDAO;
import org.papeleria_pos.dao.UsuarioDAOImpl;
import org.papeleria_pos.dto.UsuarioResumen;
import org.papeleria_pos.session.Sesion;

import java.util.List;

public class LoginViewController {

    /* ============ FXML ============ */
    @FXML private TextField     txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private TextField     txtPasswordVisible;
    @FXML private Button        btnOjo;
    @FXML private Button        btnIngresar;
    @FXML private Label         lblError;
    @FXML private VBox          card;

    /* ============ Estado ============ */
    private boolean passwordVisible = false;
    private final IUsuarioDAO usuarioDAO = new UsuarioDAOImpl();

    /* ============================================================
       Init
       ============================================================ */
    @FXML
    public void initialize() {
        // Sincronizar contenido entre PasswordField y TextField visible
        txtPassword.textProperty().addListener((o, a, b) -> {
            if (!passwordVisible) txtPasswordVisible.setText(b);
        });
        txtPasswordVisible.textProperty().addListener((o, a, b) -> {
            if (passwordVisible) txtPassword.setText(b);
        });

        // Ocultar error al escribir
        txtUsuario.textProperty().addListener((o, a, b) -> ocultarError());
        txtPassword.textProperty().addListener((o, a, b) -> ocultarError());

        // Enter en usuario → salta a contraseña
        txtUsuario.setOnAction(e -> txtPassword.requestFocus());

        // Enter en contraseña → login directo
        txtPassword.setOnAction(e -> ingresar());
        txtPasswordVisible.setOnAction(e -> ingresar());

        // Foco inicial
        Platform.runLater(txtUsuario::requestFocus);
    }

    /* ============================================================
       Mostrar / ocultar contraseña
       ============================================================ */
    @FXML
    private void togglePassword() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            txtPasswordVisible.setText(txtPassword.getText());
            txtPassword.setVisible(false);       txtPassword.setManaged(false);
            txtPasswordVisible.setVisible(true); txtPasswordVisible.setManaged(true);
            txtPasswordVisible.requestFocus();
            txtPasswordVisible.positionCaret(txtPasswordVisible.getText().length());
            btnOjo.setText("🚫");
        } else {
            txtPassword.setText(txtPasswordVisible.getText());
            txtPasswordVisible.setVisible(false); txtPasswordVisible.setManaged(false);
            txtPassword.setVisible(true);         txtPassword.setManaged(true);
            txtPassword.requestFocus();
            txtPassword.positionCaret(txtPassword.getText().length());
            btnOjo.setText("👁");
        }
    }

    /* ============================================================
       Ingresar
       ============================================================ */
    @FXML
    private void ingresar() {
        String usuario  = txtUsuario.getText() == null ? "" : txtUsuario.getText().trim();
        String password = passwordVisible
                ? txtPasswordVisible.getText()
                : txtPassword.getText();

        // ---- Validación básica ----
        if (usuario.isEmpty()) {
            mostrarError("Ingresa tu usuario");
            txtUsuario.requestFocus();
            return;
        }
        if (password == null || password.isEmpty()) {
            mostrarError("Ingresa tu contraseña");
            (passwordVisible ? txtPasswordVisible : txtPassword).requestFocus();
            return;
        }

        // ---- Bloquear botón mientras se verifica ----
        btnIngresar.setDisable(true);
        btnIngresar.setText("Verificando…");
        ocultarError();

        // ---- Autenticación en hilo aparte (no congela la UI) ----
        new Thread(() -> {
            try {
                String rolBD = usuarioDAO.autenticar(usuario, password);

                if (rolBD == null) {
                    Platform.runLater(() -> {
                        btnIngresar.setDisable(false);
                        btnIngresar.setText("Ingresar");
                        mostrarError("Usuario o contraseña incorrectos");
                        txtPassword.clear();
                        txtPasswordVisible.clear();
                        (passwordVisible ? txtPasswordVisible : txtPassword).requestFocus();
                    });
                    return;
                }

                // ---- Normalizar el rol (BD: ADMINISTRADOR/CAJERO → UI: Administrador/Cajero) ----
                String rol;
                if ("ADMINISTRADOR".equalsIgnoreCase(rolBD)) {
                    rol = "Administrador";
                } else if ("CAJERO".equalsIgnoreCase(rolBD)) {
                    rol = "Cajero";
                } else {
                    rol = rolBD;   // fallback
                }

                // ---- Obtener id y nombre del usuario ----
                int idUsuario = 0;
                String nombre = usuario;
                List<UsuarioResumen> todos = usuarioDAO.listarTodos();
                for (UsuarioResumen u : todos) {
                    if (usuario.equals(u.getUsername())) {
                        idUsuario = u.getId();
                        nombre    = u.getNombre();
                        break;
                    }
                }

                final int    idFinal     = idUsuario;
                final String nombreFinal = nombre;
                final String rolFinal    = rol;

                // ---- Guardar sesión y redirigir por rol ----
                Platform.runLater(() -> {
                    Sesion.get().iniciar(idFinal, nombreFinal, usuario, rolFinal);

                    if ("Cajero".equals(rolFinal)) {
                        App.getInstance().mostrarPOS();
                    } else {
                        App.getInstance().mostrarMain(rolFinal, nombreFinal);
                    }

                    // Limpiar campos (por si vuelven al login)
                    txtUsuario.clear();
                    txtPassword.clear();
                    txtPasswordVisible.clear();
                    btnIngresar.setDisable(false);
                    btnIngresar.setText("Ingresar");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    btnIngresar.setDisable(false);
                    btnIngresar.setText("Ingresar");
                    mostrarError("Error al conectar con la base de datos");
                });
            }
        }).start();
    }

    /* ============================================================
       Error helpers
       ============================================================ */
    private void mostrarError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);

        // Blindeado: si el card no está inyectado, no animamos
        if (card == null) return;

        javafx.animation.TranslateTransition tt =
                new javafx.animation.TranslateTransition(
                        javafx.util.Duration.millis(60), card);
        tt.setFromX(-6); tt.setToX(6);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> card.setTranslateX(0));
        tt.play();
    }

    private void ocultarError() {
        if (lblError.isVisible()) {
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }
}