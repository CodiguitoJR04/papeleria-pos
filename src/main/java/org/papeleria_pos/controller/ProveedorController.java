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

import org.papeleria_pos.dao.Interface.IProductoDAO;
import org.papeleria_pos.dao.Interface.IProvedorDAO;
import org.papeleria_pos.dao.ProductoDAOImpl;
import org.papeleria_pos.dao.ProveedorDAOImpl;
import org.papeleria_pos.models.Producto;
import org.papeleria_pos.models.Proveedor;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProveedorController {

    /* -------- Tabla proveedores -------- */
    @FXML private TableView<Proveedor> tabla;
    @FXML private TableColumn<Proveedor, String>  colNombre, colContacto, colTelefono, colEmail, colEstado;
    @FXML private TableColumn<Proveedor, Void>    colAcciones;
    @FXML private TextField txtBuscar;
    @FXML private Label lblSubtitulo;

    /* -------- Tabla productos del proveedor -------- */
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, String>  colProdNombre, colProdSku, colProdCategoria;
    @FXML private TableColumn<Producto, Double>  colProdPrecio;
    @FXML private TableColumn<Producto, Integer> colProdStock;
    @FXML private Label lblProductosProveedor;

    /* -------- Modal -------- */
    @FXML private StackPane modalProveedor;
    @FXML private Label     modalTitulo;
    @FXML private TextField provNombre, provContacto, provTelefono, provEmail;
    @FXML private ComboBox<String> provEstado;

    private final IProvedorDAO provDAO = new ProveedorDAOImpl();
    private final IProductoDAO prodDAO = new ProductoDAOImpl();

    private final ObservableList<Proveedor> datos     = FXCollections.observableArrayList();
    private final ObservableList<Producto>  productos = FXCollections.observableArrayList();

    private Integer editandoId = null;

    private static final NumberFormat MXN =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"));

    /* ============================================================ */
    @FXML
    public void initialize() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaProductos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        configurarColumnasProveedores();
        configurarColumnasProductos();

        provEstado.getItems().addAll("Activo", "Inactivo");
        provEstado.setValue("Activo");

        if (txtBuscar != null)
            txtBuscar.textProperty().addListener((o, a, b) -> refrescar());

        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, prov) -> {
            if (prov != null) cargarProductosDeProveedor(prov);
            else {
                productos.clear();
                lblProductosProveedor.setText("PRODUCTOS DEL PROVEEDOR");
            }
        });

        tabla.setItems(datos);
        tablaProductos.setItems(productos);
        refrescar();
    }

    /* ============================================================ */
    private void configurarColumnasProveedores() {
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colContacto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getContacto()));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelefono()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));

        colEstado.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add("Activo".equals(s) ? "ok" : "neutral");
                setGraphic(badge);
                setText(null);
            }
        });

        colAcciones.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                Proveedor p = getTableView().getItems().get(getIndex());

                Button editar = new Button("✎");
                editar.getStyleClass().add("action-btn");
                editar.setOnAction(e -> abrirEditar(p));

                Button borrar = new Button("🗑");
                borrar.getStyleClass().addAll("action-btn", "danger");
                borrar.setOnAction(e -> eliminar(p));

                HBox box = new HBox(4, editar, borrar);
                box.setAlignment(Pos.CENTER_RIGHT);
                setGraphic(box);
            }
        });
    }

    private void configurarColumnasProductos() {
        colProdNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colProdSku.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSku()));
        colProdCategoria.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreCategoria()));

        colProdPrecio.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecioVenta()));
        colProdPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : MXN.format(v));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        colProdStock.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getStockActual()));
        colProdStock.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.toString());
                setAlignment(Pos.CENTER);
            }
        });
    }

    /* ============================================================ */
    private void refrescar() {
        new Thread(() -> {
            try {
                String filtro = txtBuscar != null ? txtBuscar.getText() : null;
                List<Proveedor> lista = (filtro == null || filtro.isBlank())
                        ? provDAO.listarTodos()
                        : provDAO.buscar(filtro);

                Platform.runLater(() -> {
                    Proveedor sel = tabla.getSelectionModel().getSelectedItem();
                    datos.setAll(lista);
                    if (sel != null) {
                        for (Proveedor p : lista) {
                            if (p.getIdProveedor() == sel.getIdProveedor()) {
                                tabla.getSelectionModel().select(p);
                                break;
                            }
                        }
                    }
                    if (lblSubtitulo != null)
                        lblSubtitulo.setText(lista.size() + " proveedores activos");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        alerta("Error al cargar proveedores:\n\n" + ex.getMessage()));
            }
        }).start();
    }

    private void cargarProductosDeProveedor(Proveedor prov) {
        lblProductosProveedor.setText("PRODUCTOS DE " + prov.getNombre().toUpperCase());
        new Thread(() -> {
            try {
                List<Producto> lista = prodDAO.listarPorProveedor(prov.getIdProveedor());
                Platform.runLater(() -> productos.setAll(lista));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> productos.clear());
            }
        }).start();
    }

    /* ============================================================ */
    @FXML
    private void abrirNuevo() {
        editandoId = null;
        modalTitulo.setText("Nuevo proveedor");
        provNombre.clear();
        provContacto.clear();
        provTelefono.clear();
        provEmail.clear();
        provEstado.setValue("Activo");

        modalProveedor.setVisible(true);
        modalProveedor.setManaged(true);
        provNombre.requestFocus();
    }

    private void abrirEditar(Proveedor p) {
        editandoId = p.getIdProveedor();
        modalTitulo.setText("Editar: " + p.getNombre());
        provNombre.setText(p.getNombre());
        provContacto.setText(p.getContacto());
        provTelefono.setText(p.getTelefono());
        provEmail.setText(p.getEmail());
        provEstado.setValue(p.isActivo() ? "Activo" : "Inactivo");

        modalProveedor.setVisible(true);
        modalProveedor.setManaged(true);
    }

    @FXML
    private void cerrarModal() {
        modalProveedor.setVisible(false);
        modalProveedor.setManaged(false);
    }

    @FXML
    private void guardar() {
        String nombre   = provNombre.getText()   == null ? "" : provNombre.getText().trim();
        String contacto = provContacto.getText() == null ? "" : provContacto.getText().trim();
        String telefono = provTelefono.getText() == null ? "" : provTelefono.getText().trim();
        String email    = provEmail.getText()    == null ? "" : provEmail.getText().trim();
        boolean activo  = "Activo".equals(provEstado.getValue());

        if (nombre.isEmpty()) {
            alerta("El nombre es obligatorio.");
            return;
        }

        Proveedor p = new Proveedor();
        p.setIdProveedor(editandoId == null ? 0 : editandoId);
        p.setNombre(nombre);
        p.setContacto(contacto);
        p.setTelefono(telefono);
        p.setEmail(email);
        p.setActivo(activo);

        final boolean esNuevo = (editandoId == null);

        new Thread(() -> {
            try {
                if (esNuevo) {
                    int id = provDAO.insertar(p);
                    Platform.runLater(() -> {
                        if (id > 0) {
                            exito("Proveedor creado (ID " + id + ").");
                            cerrarModal();
                            refrescar();
                        } else {
                            alerta("No se pudo crear el proveedor.");
                        }
                    });
                } else {
                    boolean ok = provDAO.actualizar(p);
                    Platform.runLater(() -> {
                        if (ok) {
                            exito("Proveedor actualizado.");
                            cerrarModal();
                            refrescar();
                        } else {
                            alerta("No se pudo actualizar.");
                        }
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        alerta("Error al guardar:\n\n" + ex.getMessage()));
            }
        }).start();
    }

    private void eliminar(Proveedor p) {
        new Thread(() -> {
            try {
                List<Producto> prods = prodDAO.listarPorProveedor(p.getIdProveedor());
                Platform.runLater(() -> {
                    String msg = prods.isEmpty()
                            ? "¿Desactivar el proveedor \"" + p.getNombre() + "\"?"
                            : "El proveedor \"" + p.getNombre() + "\" tiene "
                            + prods.size() + " productos.\n¿Desactivar de todas formas?";

                    Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg,
                            ButtonType.OK, ButtonType.CANCEL);
                    a.setHeaderText(null);
                    if (!a.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) return;

                    new Thread(() -> {
                        try {
                            boolean ok = provDAO.eliminar(p.getIdProveedor());
                            Platform.runLater(() -> {
                                if (ok) {
                                    exito("Proveedor desactivado.");
                                    refrescar();
                                } else {
                                    alerta("No se pudo desactivar.");
                                }
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> alerta("Error:\n\n" + ex.getMessage()));
                        }
                    }).start();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    /* ============================================================ */
    private void alerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Aviso");
        a.showAndWait();
    }

    private void exito(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Listo");
        a.showAndWait();
    }
}