package org.papeleria_pos.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import org.papeleria_pos.dao.CategoriaDAOImpl;
import org.papeleria_pos.dao.Interface.ICategoriaDAO;
import org.papeleria_pos.dto.CategoriaResumen;

import java.util.List;

public class CategoriaController {

    /* ============ Tabla ============ */
    @FXML private TableView<CategoriaResumen> tabla;
    @FXML private TableColumn<CategoriaResumen, String>  colNombre, colDescripcion, colEstado;
    @FXML private TableColumn<CategoriaResumen, Integer> colProductos;
    @FXML private TableColumn<CategoriaResumen, Void>    colAcciones;
    @FXML private TextField txtBuscar;
    @FXML private Label lblSubtitulo;

    /* ============ Modal ============ */
    @FXML private StackPane modalCategoria;
    @FXML private Label modalTitulo;
    @FXML private TextField catNombre;
    @FXML private TextArea catDescripcion;
    @FXML private ComboBox<String> catEstado;

    private final ICategoriaDAO dao = new CategoriaDAOImpl();
    private final ObservableList<CategoriaResumen> datos = FXCollections.observableArrayList();
    private Integer editandoId = null;

    /* ============================================================
       Init
       ============================================================ */
    @FXML
    public void initialize() {
        // ✅ ESTA ES LA LÍNEA QUE ELIMINA LA COLUMNA FANTASMA
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        // Si usas JavaFX < 20, usa en su lugar:
        // tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ---- Columnas con lambdas (no reflexión) ----
        colNombre.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNombre()));

        colDescripcion.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDescripcion()));

        colProductos.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getNumProductos()));

        colEstado.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEstado()));

        // ---- Columna Estado con badge ----
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add(s.equals("Activa") ? "ok" : "neutral");
                setGraphic(badge);
                setText(null);
            }
        });

        // ---- Columna Acciones ----
        colAcciones.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }

                CategoriaResumen cat = getTableView().getItems().get(getIndex());

                Button editar = new Button("✎");
                editar.getStyleClass().add("action-btn");
                editar.setOnAction(e -> abrirEditar(cat));

                Button borrar = new Button("🗑");
                borrar.getStyleClass().addAll("action-btn", "danger");
                borrar.setOnAction(e -> eliminar(cat));

                HBox box = new HBox(4, editar, borrar);
                box.setAlignment(Pos.CENTER_RIGHT);
                box.getStyleClass().add("row-actions");
                setGraphic(box);
                setText(null);
            }
        });

        catEstado.getItems().addAll("Activa", "Inactiva");
        catEstado.setValue("Activa");

        if (txtBuscar != null)
            txtBuscar.textProperty().addListener((o, a, b) -> refrescar());

        tabla.setItems(datos);
        refrescar();
    }

    /* ============================================================
       Refrescar la tabla
       ============================================================ */
    private void refrescar() {
        new Thread(() -> {
            try {
                String filtro = txtBuscar != null ? txtBuscar.getText() : null;
                List<CategoriaResumen> lista = (filtro == null || filtro.isEmpty())
                        ? dao.listarTodas()
                        : dao.buscar(filtro);

                Platform.runLater(() -> {
                    datos.setAll(lista);
                    int total = datos.size();
                    long activas = datos.stream().filter(CategoriaResumen::isActiva).count();
                    lblSubtitulo.setText(total + " categorías · " + activas + " activas");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /* ============================================================
       Modal
       ============================================================ */
    @FXML private void abrirNuevo() {
        editandoId = null;
        modalTitulo.setText("Nueva categoría");
        catNombre.clear();
        catDescripcion.clear();
        catEstado.setValue("Activa");
        modalCategoria.setVisible(true);
        modalCategoria.setManaged(true);
        catNombre.requestFocus();
    }

    private void abrirEditar(CategoriaResumen c) {
        editandoId = c.getId();
        modalTitulo.setText("Editar: " + c.getNombre());
        catNombre.setText(c.getNombre());
        catDescripcion.setText(c.getDescripcion());
        catEstado.setValue(c.isActiva() ? "Activa" : "Inactiva");
        modalCategoria.setVisible(true);
        modalCategoria.setManaged(true);
    }

    @FXML
    private void cerrarModal() {
        modalCategoria.setVisible(false);
        modalCategoria.setManaged(false);
    }

    @FXML private void guardar() {
        String nombre = catNombre.getText().trim();
        String desc = catDescripcion.getText() == null ? "" : catDescripcion.getText().trim();
        boolean activa = "Activa".equals(catEstado.getValue());

        if (nombre.isEmpty()) {
            alerta("El nombre es obligatorio");
            return;
        }

        if (editandoId == null) {
            dao.insertar(nombre, desc, activa);
        } else {
            dao.actualizar(editandoId, nombre, desc, activa);
        }
        cerrarModal();
        refrescar();
    }

    private void eliminar(CategoriaResumen c) {
        if (c.getNumProductos() > 0) {
            boolean ok = confirmar("La categoría \"" + c.getNombre() + "\" tiene " +
                    c.getNumProductos() + " productos asociados.\n¿Eliminar de todas formas?");
            if (!ok) return;
        } else {
            if (!confirmar("¿Eliminar la categoría \"" + c.getNombre() + "\"?")) return;
        }
        dao.eliminar(c.getId());
        refrescar();
    }

    /* ============================================================
       Helpers
       ============================================================ */
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