package org.papeleria_pos.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.papeleria_pos.dao.Interface.IUsuarioDAO;
import org.papeleria_pos.dao.UsuarioDAOImpl;
import org.papeleria_pos.dto.UsuarioResumen;

public class UsuariosController {
    @FXML
    private TableView<UsuarioResumen> tabla;
    @FXML private TableColumn<UsuarioResumen, String> colNombre, colRol, colUltimoAcceso, colEstado;
    @FXML private TableColumn<UsuarioResumen, Integer> colVentasHoy;
    @FXML private TableColumn<UsuarioResumen, Void>    colAcciones;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cbRol, cbEstado;
    @FXML private Label lblSubtitulo;

    @FXML private StackPane modalUsuario;
    @FXML private Label modalTitulo;
    @FXML private TextField usrNombre, usrUsername;
    @FXML private PasswordField usrPassword;
    @FXML private ComboBox<String> usrRol, usrEstado;

    private final IUsuarioDAO dao = new UsuarioDAOImpl();
    private final ObservableList<UsuarioResumen> datos = FXCollections.observableArrayList();
    private Integer editandoId = null;

    @FXML
    public void initialize() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colUltimoAcceso.setCellValueFactory(new PropertyValueFactory<>("ultimoAccesoTexto"));
        colVentasHoy.setCellValueFactory(new PropertyValueFactory<>("ventasHoy"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // Rol con badge
        colRol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add(s.equals("Administrador") ? "info" : "neutral");
                setGraphic(badge); setText(null);
            }
        });

        // Estado con badge
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add(s.equals("Activo") ? "ok" : "out");
                setGraphic(badge); setText(null);
            }
        });

        // Nombre con avatar + username
        colNombre.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }

                UsuarioResumen u = getTableView().getItems().get(getIndex());

                Label avatar = new Label(u.getIniciales());
                avatar.getStyleClass().add("usuario-avatar");
                if ("Administrador".equals(u.getRol())) avatar.getStyleClass().add("admin");

                Label nombre = new Label(u.getNombre());
                nombre.getStyleClass().add("usuario-nombre");
                Label user = new Label("@" + u.getUsername());
                user.getStyleClass().add("usuario-username");

                javafx.scene.layout.VBox info = new javafx.scene.layout.VBox(2, nombre, user);
                HBox box = new HBox(10, avatar, info);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(box); setText(null);
            }
        });

        // Acciones
        colAcciones.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }

                UsuarioResumen u = getTableView().getItems().get(getIndex());

                Button editar = new Button("✎");
                editar.getStyleClass().add("action-btn");
                editar.setOnAction(e -> abrirEditar(u));

                Button borrar = new Button("🗑");
                borrar.getStyleClass().add("action-btn");
                borrar.getStyleClass().add("danger");
                borrar.setOnAction(e -> eliminar(u));

                HBox box = new HBox(4, editar, borrar);
                box.getStyleClass().add("row-actions");
                setGraphic(box);
            }
        });

        cbRol.getItems().addAll("Administrador", "Cajero");
        cbEstado.getItems().addAll("Activos", "Inactivos");
        usrRol.getItems().addAll("Cajero", "Administrador");
        usrEstado.getItems().addAll("Activo", "Inactivo");

        txtBuscar.textProperty().addListener((o, a, b) -> refrescar());
        cbRol.valueProperty().addListener((o, a, b) -> refrescar());
        cbEstado.valueProperty().addListener((o, a, b) -> refrescar());

        tabla.setItems(datos);
        refrescar();
    }

    private void refrescar() {
        String filtro = txtBuscar.getText();
        String rol = cbRol.getValue();
        String estadoTxt = cbEstado.getValue();
        Boolean activo = null;
        if ("Activos".equals(estadoTxt)) activo = true;
        if ("Inactivos".equals(estadoTxt)) activo = false;

        datos.setAll(dao.buscar(filtro, rol, activo));

        long activos = datos.stream().filter(UsuarioResumen::isActivo).count();
        lblSubtitulo.setText(datos.size() + " usuarios · " + activos + " activos");
    }

    /* ============================================================
       Modal
       ============================================================ */
    @FXML private void abrirNuevo() {
        editandoId = null;
        modalTitulo.setText("Nuevo usuario");
        usrNombre.clear(); usrUsername.clear(); usrPassword.clear();
        usrRol.setValue("Cajero");
        usrEstado.setValue("Activo");
        modalUsuario.setVisible(true);
        modalUsuario.setManaged(true);
        usrNombre.requestFocus();
    }

    private void abrirEditar(UsuarioResumen u) {
        editandoId = u.getId();
        modalTitulo.setText("Editar: " + u.getNombre());
        usrNombre.setText(u.getNombre());
        usrUsername.setText(u.getUsername());
        usrPassword.setText(""); // vacío por seguridad
        usrRol.setValue(u.getRol());
        usrEstado.setValue(u.isActivo() ? "Activo" : "Inactivo");
        modalUsuario.setVisible(true);
        modalUsuario.setManaged(true);
    }

    @FXML private void cerrarModal() {
        modalUsuario.setVisible(false);
        modalUsuario.setManaged(false);
    }

    @FXML private void guardar() {
        String nombre = usrNombre.getText().trim();
        String username = usrUsername.getText().trim();
        String pass = usrPassword.getText();
        String rol = usrRol.getValue();
        boolean activo = "Activo".equals(usrEstado.getValue());

        if (nombre.isEmpty() || username.isEmpty()) {
            alerta("Nombre y usuario son obligatorios");
            return;
        }
        if (editandoId == null && (pass == null || pass.isEmpty())) {
            alerta("La contraseña es obligatoria para nuevos usuarios");
            return;
        }

        if (editandoId == null) {
            dao.insertar(nombre, username, pass, rol, activo);
        } else {
            // Si no escribieron nueva contraseña, conservar la anterior
            String passFinal = (pass == null || pass.isEmpty()) ? obtenerPassword(editandoId) : pass;
            dao.actualizar(editandoId, nombre, username, passFinal, rol, activo);
        }
        cerrarModal();
        refrescar();
    }

    /** Obtiene el password actual desde la BD (para no sobreescribirlo). */
    private String obtenerPassword(int id) {
        try (var ps = org.papeleria_pos.config.DatabaseConnection.get()
                .prepareStatement("SELECT password FROM usuario WHERE id_user=?")) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "";
            }
        } catch (Exception e) { return ""; }
    }

    private void eliminar(UsuarioResumen u) {
        if ("admin".equals(u.getUsername())) {
            alerta("No puedes eliminar el usuario administrador principal.");
            return;
        }
        if (!confirmar("¿Eliminar al usuario \"" + u.getNombre() + "\"?")) return;
        dao.eliminar(u.getId());
        refrescar();
    }

    private void alerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private boolean confirmar(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }
}
