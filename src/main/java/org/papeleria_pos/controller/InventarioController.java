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
import javafx.scene.layout.VBox;

import org.papeleria_pos.dao.CategoriaDAOImpl;
import org.papeleria_pos.dao.Interface.IProductoDAO;
import org.papeleria_pos.dao.Interface.IProvedorDAO;
import org.papeleria_pos.dao.ProductoDAOImpl;
import org.papeleria_pos.dao.ProveedorDAOImpl;
import org.papeleria_pos.models.Producto;
import org.papeleria_pos.models.Proveedor;
import org.papeleria_pos.hardware.LectorCodigoBarras;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InventarioController {

    /* ============================================================
       § 1. COMPONENTES FXML — Tabla principal
       ============================================================ */
    @FXML private TableView<Producto> tabla;
    @FXML private TableColumn<Producto, String>  colNombre, colSku, colCategoria;
    @FXML private TableColumn<Producto, Double>  colPrecioCompra, colPrecioVenta, colMargen;
    @FXML private TableColumn<Producto, Integer> colStock;
    @FXML private TableColumn<Producto, Void>    colAcciones;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cbCategoria, cbProveedor;
    @FXML private Button chipStock;
    @FXML private Label lblSubtitulo;
    Proveedor proveedor =  new Proveedor();

    /* ============================================================
       § 2. COMPONENTES FXML — Modal producto
       ============================================================ */
    @FXML private StackPane modalProducto;
    @FXML private Label     modalTitulo;
    @FXML private Button    tabUnidad, tabLote;
    @FXML private VBox      formUnidad, formLote;
    @FXML private TextField pSku, pNombre, pPrecioCompra, pPrecioVenta;
    @FXML private ComboBox<String> pCategoria, pProveedor;
    @FXML private TextField uStock, uStockMin, lCantidad, lPorLote, lCostoLote, lStockMin;
    @FXML private Label     lblMargen, calcStock, calcCosto, calcTotal;

    /* ============================================================
       § 3. COMPONENTES FXML — Modal movimiento
       ============================================================ */
    @FXML private StackPane modalMovimiento;
    @FXML private ComboBox<String> mvProducto;
    @FXML private TextField mvCantidad;
    @FXML private TextArea  mvMotivo;

    /* ============================================================
       § 4. DAOs y estado
       ============================================================ */
    private final IProductoDAO  dao          = new ProductoDAOImpl();
    private final IProvedorDAO  proveedorDAO = new ProveedorDAOImpl();

    private final ObservableList<Producto> datos = FXCollections.observableArrayList();

    private boolean filtroStockBajo = false;   // chip "Stock bajo"
    private Integer editandoId = null;         // null = nuevo, valor = editar

    private static final NumberFormat MXN =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"));

    /* ============================================================
       § 5. INITIALIZE
       ============================================================ */
    @FXML
    public void initialize() {
        // 5.1 — Tabla principal
        configurarTabla();

        // 5.2 — Filtros reactivos
        configurarFiltros();

        // 5.3 — Listeners de cálculos del modal
        configurarListenersModal();

        // 5.4 — SKU / escáner
        configurarEscaner();

        // 5.5 — Carga inicial (categorías + proveedores + productos)
        cargarCategorias();
        cargarProveedores();
        refrescar();

        tabla.setItems(datos);
    }

    /* ============================================================
       § 6. CONFIGURACIÓN DE LA TABLA
       ============================================================ */
    private void configurarTabla() {
        // 6.1 — Reparto proporcional de columnas
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // 6.2 — Producto (nombre + SKU debajo)
        colNombre.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNombre()));
        colNombre.setCellFactory(c -> celdaNombreConSku());

        // 6.3 — SKU
        colSku.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getSku()));

        // 6.4 — Categoría
        colCategoria.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNombreCategoria()));

        // 6.5 — Precio compra
        colPrecioCompra.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getPrecioCompra()));
        colPrecioCompra.setCellFactory(c -> celdaMoneda());

        // 6.6 — Precio venta
        colPrecioVenta.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getPrecioVenta()));
        colPrecioVenta.setCellFactory(c -> celdaMoneda());

        // 6.7 — Margen
        colMargen.setCellValueFactory(c -> {
            Producto p = c.getValue();
            return new SimpleObjectProperty<>(p.getPrecioVenta() - p.getPrecioCompra());
        });
        colMargen.setCellFactory(c -> celdaMargen());

        // 6.8 — Stock (badge)
        colStock.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getStockActual()));
        colStock.setCellFactory(c -> celdaStock());

        // 6.9 — Acciones
        colAcciones.setCellFactory(c -> celdaAcciones());
    }

    /* ============================================================
       § 7. FILTROS REACTIVOS
       ============================================================ */
    private void configurarFiltros() {
        if (txtBuscar   != null) txtBuscar.textProperty()  .addListener((o, a, b) -> refrescar());
        if (cbCategoria != null) cbCategoria.valueProperty().addListener((o, a, b) -> refrescar());
        if (cbProveedor != null) cbProveedor.valueProperty().addListener((o, a, b) -> refrescar());
    }

    /* ============================================================
       § 8. LISTENERS DEL MODAL PRODUCTO
       ============================================================ */
    private void configurarListenersModal() {
        if (lCantidad    != null) lCantidad.textProperty()   .addListener((o, a, b) -> recalcularLote());
        if (lPorLote     != null) lPorLote.textProperty()    .addListener((o, a, b) -> recalcularLote());
        if (lCostoLote   != null) lCostoLote.textProperty()  .addListener((o, a, b) -> recalcularLote());
        if (pPrecioCompra != null) pPrecioCompra.textProperty().addListener((o, a, b) -> calcularMargen());
        if (pPrecioVenta  != null) pPrecioVenta.textProperty() .addListener((o, a, b) -> calcularMargen());
    }

    /* ============================================================
       § 9. SKU / ESCÁNER
       ============================================================ */
    private void configurarEscaner() {
        // 9.1 — Escáner en el SKU del modal
        if (pSku != null) {
            LectorCodigoBarras.attach(pSku, codigo -> {
                pSku.setText(codigo);
                System.out.println("[SCAN] SKU registrado: " + codigo);
                autocompletarDesdeSku(codigo);
            });
        }

        // 9.2 — Escáner sobre el buscador superior
        if (txtBuscar != null) {
            LectorCodigoBarras.attach(txtBuscar, codigo -> {
                txtBuscar.setText(codigo);
                System.out.println("[SCAN] Buscando: " + codigo);
                refrescar();
            });
        }
    }

    /** Botón 🔫 del modal */
    @FXML
    private void activarEscaner() {
        if (pSku != null) {
            LectorCodigoBarras.enfocar(pSku);
            System.out.println("🔫 Escáner activado. Escanea un código...");
        }
    }

    /** Botón 🔫 de la barra de filtros (buscador superior) */
    @FXML
    private void activarEscanerBusqueda() {
        if (txtBuscar != null) {
            LectorCodigoBarras.enfocar(txtBuscar);
            System.out.println("🔫 Escáner activado. Escanea un código...");
        }
    }

    /** Cuando escanean un SKU existente, precarga sus datos en el modal */
    private void autocompletarDesdeSku(String sku) {
        new Thread(() -> {
            try {
                List<Producto> encontrados = dao.buscar(sku, null, null);
                Platform.runLater(() -> {
                    if (!encontrados.isEmpty()) {
                        Producto p = encontrados.get(0);
                        if (pNombre != null)        pNombre.setText(p.getNombre());
                        if (pPrecioCompra != null)  pPrecioCompra.setText(String.valueOf(p.getPrecioCompra()));
                        if (pPrecioVenta != null)   pPrecioVenta.setText(String.valueOf(p.getPrecioVenta()));
                        if (pCategoria != null && p.getNombreCategoria() != null) {
                            pCategoria.setValue(p.getNombreCategoria());
                        }
                        System.out.println("[SCAN] Producto encontrado: " + p.getNombre());
                    } else {
                        System.out.println("[SCAN] SKU nuevo (no existe en BD)");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /* ============================================================
       § 10. CARGA DE CATEGORÍAS  (filtro + modal)
       ============================================================ */
    private void cargarCategorias() {
        new Thread(() -> {
            try {
                var categorias = new CategoriaDAOImpl().listarTodas();
                List<String> nombresCat = new ArrayList<>();
                nombresCat.add("Todas");
                for (var c : categorias) {
                    if (c.isActiva()) nombresCat.add(c.getNombre());
                }

                Platform.runLater(() -> {
                    // 10.1 — Combo del filtro
                    if (cbCategoria != null) {
                        cbCategoria.getItems().setAll(nombresCat);
                        cbCategoria.setValue("Todas");
                    }

                    // 10.2 — Combo del modal (sin "Todas")
                    if (pCategoria != null) {
                        List<String> solo = new ArrayList<>(nombresCat);
                        solo.remove("Todas");
                        pCategoria.getItems().setAll(solo);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        System.err.println("✖ Error cargando categorías: " + e.getMessage()));
            }
        }).start();
    }

    /** Resuelve id_categoria por nombre (usado en refrescar y guardar). */
    private Integer obtenerIdCategoriaPorNombre(String nombre) {
        try (var ps = org.papeleria_pos.config.DatabaseConnection.get()
                .prepareStatement("SELECT id_categoria FROM categorias WHERE nombre = ?")) {
            ps.setString(1, nombre);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /* ============================================================
       § 11. CARGA DE PROVEEDORES  (filtro + modal)
       ============================================================ */
    private void cargarProveedores() {
        new Thread(() -> {
            try {
                List<Proveedor> proveedores = proveedorDAO.listarTodos();
                List<String> nombresProv = new ArrayList<>();
                nombresProv.add("Todos");
                for (Proveedor p : proveedores) {
                    nombresProv.add(p.getNombre());
                }

                Platform.runLater(() -> {
                    // 11.1 — Combo del filtro
                    if (cbProveedor != null) {
                        cbProveedor.getItems().setAll(nombresProv);
                        cbProveedor.setValue("Todos");
                    }

                    // 11.2 — Combo del modal (sin "Todos")
                    if (pProveedor != null) {
                        List<String> solo = new ArrayList<>(nombresProv);
                        solo.remove("Todos");
                        pProveedor.getItems().setAll(solo);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        System.err.println("✖ Error cargando proveedores: " + e.getMessage()));
            }
        }).start();
    }

    /* ============================================================
       § 12. REFRESCAR LA TABLA (con filtros combinados)
       ============================================================ */
    private void refrescar() {
        new Thread(() -> {
            try {
                String filtro = txtBuscar != null ? txtBuscar.getText() : null;

                // 12.1 — Resolver categoría seleccionada
                Integer idCategoria = null;
                if (cbCategoria != null) {
                    String cat = cbCategoria.getValue();
                    if (cat != null && !"Todas".equals(cat)) {
                        idCategoria = obtenerIdCategoriaPorNombre(cat);
                    }
                }

                // 12.2 — Resolver proveedor seleccionado
                Integer idProveedor = null;
                if (cbProveedor != null) {
                    String prov = cbProveedor.getValue();
                    if (prov != null && !"Todos".equals(prov)) {
                        Proveedor pr = proveedorDAO.buscarPorNombre(prov);
                        if (pr != null) idProveedor = pr.getIdProveedor();
                    }
                }

                // 12.3 — UNA SOLA consulta según filtros
                List<Producto> lista;
                if (idProveedor != null) {
                    lista = dao.buscarPorProveedor(idProveedor);
                } else {
                    lista = dao.buscar(filtro, idCategoria, filtroStockBajo);
                }

                int stockBajo = dao.contarStockBajo();

                // 12.4 — Pintar en UI
                final List<Producto> listaFinal = lista;
                final int sb = stockBajo;
                Platform.runLater(() -> {
                    datos.setAll(listaFinal);
                    if (lblSubtitulo != null) {
                        lblSubtitulo.setText(datos.size() + " productos · "
                                + sb + " con stock bajo");
                        lblSubtitulo.setStyle("");
                    }
                    System.out.println("✔ Carga exitosa: " + listaFinal.size() + " productos.");
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    if (lblSubtitulo != null) {
                        lblSubtitulo.setText("✖ Error al cargar: " + ex.getMessage());
                        lblSubtitulo.setStyle("-fx-text-fill: #E53935;");
                    }
                    System.err.println("✖ Error en refrescar: " + ex.getMessage());
                });
            }
        }).start();
    }

    /* ============================================================
       § 13. CHIP "STOCK BAJO"
       ============================================================ */
    @FXML
    private void toggleStockBajo() {
        filtroStockBajo = !filtroStockBajo;
        if (filtroStockBajo) {
            if (!chipStock.getStyleClass().contains("active"))
                chipStock.getStyleClass().add("active");
        } else {
            chipStock.getStyleClass().remove("active");
        }
        refrescar();
    }

    /* ============================================================
       § 14. MODAL PRODUCTO — Abrir NUEVO / EDITAR
       ============================================================ */
    @FXML
    private void abrirNuevo() {
        editandoId = null;
        if (modalTitulo != null) modalTitulo.setText("Nuevo producto");

        // 14.1 — Limpiar campos
        if (pSku != null)          pSku.clear();
        if (pNombre != null)       pNombre.clear();
        if (pPrecioCompra != null) pPrecioCompra.clear();
        if (pPrecioVenta != null)  pPrecioVenta.clear();
        if (uStock != null)        uStock.clear();
        if (uStockMin != null)     uStockMin.setText("5");
        if (lCantidad != null)     lCantidad.setText("1");
        if (lPorLote != null)      lPorLote.clear();
        if (lCostoLote != null)    lCostoLote.clear();
        if (lStockMin != null)     lStockMin.setText("5");
        if (lblMargen != null)     lblMargen.setText("—");
        if (calcStock != null)     calcStock.setText("0 uds");
        if (calcCosto != null)     calcCosto.setText("—");
        if (calcTotal != null)     calcTotal.setText("—");
        if (pCategoria != null)    pCategoria.setValue(null);
        if (pProveedor != null)    pProveedor.setValue(null);

        modoUnidad();

        modalProducto.setVisible(true);
        modalProducto.setManaged(true);
        LectorCodigoBarras.enfocar(pSku);
    }

    private void abrirEditar(Producto p) {
        editandoId = p.getIdProducto();
        if (modalTitulo != null) modalTitulo.setText("Editar: " + p.getNombre());

        if (pSku != null)          pSku.setText(p.getSku());
        if (pNombre != null)       pNombre.setText(p.getNombre());
        if (pPrecioCompra != null) pPrecioCompra.setText(String.valueOf(p.getPrecioCompra()));
        if (pPrecioVenta != null)  pPrecioVenta.setText(String.valueOf(p.getPrecioVenta()));
        if (uStock != null)        uStock.setText(String.valueOf(p.getStockActual()));
        if (uStockMin != null)     uStockMin.setText(String.valueOf(p.getStockMinimo()));
        if (lStockMin != null)     lStockMin.setText(String.valueOf(p.getStockMinimo()));

        if (pCategoria != null && p.getNombreCategoria() != null) {
            pCategoria.setValue(p.getNombreCategoria());
        }
        if (pProveedor != null && proveedor.getNombre() != null) {
            pProveedor.setValue(proveedor.getNombre());
        }

        modoUnidad();
        calcularMargen();

        modalProducto.setVisible(true);
        modalProducto.setManaged(true);
    }

    @FXML
    private void cerrarModalProducto() {
        modalProducto.setVisible(false);
        modalProducto.setManaged(false);
    }

    /* ============================================================
       § 15. TABS UNIDAD / LOTE
       ============================================================ */
    @FXML
    private void modoUnidad() {
        formUnidad.setVisible(true);  formUnidad.setManaged(true);
        formLote.setVisible(false);   formLote.setManaged(false);
        if (!tabUnidad.getStyleClass().contains("active")) tabUnidad.getStyleClass().add("active");
        tabLote.getStyleClass().remove("active");
    }

    @FXML
    private void modoLote() {
        formUnidad.setVisible(false); formUnidad.setManaged(false);
        formLote.setVisible(true);    formLote.setManaged(true);
        if (!tabLote.getStyleClass().contains("active")) tabLote.getStyleClass().add("active");
        tabUnidad.getStyleClass().remove("active");
        recalcularLote();
    }

    /* ============================================================
       § 16. GUARDAR PRODUCTO (INSERT / UPDATE)
       ============================================================ */
    @FXML
    private void guardarProducto() {
        if (pSku == null || pNombre == null) { cerrarModalProducto(); return; }

        // 16.1 — Validar texto
        String sku    = pSku.getText()    != null ? pSku.getText().trim()    : "";
        String nombre = pNombre.getText() != null ? pNombre.getText().trim() : "";
        if (sku.isEmpty() || nombre.isEmpty()) {
            alerta("SKU y Nombre son obligatorios.");
            return;
        }

        // 16.2 — Precios
        double precioCompra = parseDouble(pPrecioCompra.getText());
        double precioVenta  = parseDouble(pPrecioVenta.getText());
        if (precioVenta <= 0) {
            alerta("El precio de venta debe ser mayor a 0.");
            return;
        }

        // 16.3 — Stock / costo (según pestaña activa)
        int stock, stockMin;
        if (formLote != null && formLote.isVisible()) {
            int cantLote = parseInt(lCantidad.getText());
            int porLote  = parseInt(lPorLote.getText());
            double costoLote = parseDouble(lCostoLote.getText());

            stock    = cantLote * porLote;
            stockMin = parseInt(lStockMin.getText());
            if (stock <= 0) {
                alerta("El lote debe generar al menos 1 unidad.");
                return;
            }
            if (costoLote > 0) precioCompra = costoLote / stock;
        } else {
            stock    = parseInt(uStock.getText());
            stockMin = parseInt(uStockMin.getText());
        }

        // 16.4 — Resolver categoría (con fallback)
        Integer idCategoria = null;
        if (pCategoria != null && pCategoria.getValue() != null) {
            idCategoria = obtenerIdCategoriaPorNombre(pCategoria.getValue());
        }
        if (idCategoria == null) {
            idCategoria = obtenerIdCategoriaPorNombre("Sin categoría");
        }
        if (idCategoria == null) {
            alerta("No existe categoría por defecto. Créala primero.");
            return;
        }

        // 16.5 — Resolver proveedor (opcional)
        Integer idProveedor = null;
        if (pProveedor != null && pProveedor.getValue() != null
                && !pProveedor.getValue().isBlank()) {
            Proveedor pr = proveedorDAO.buscarPorNombre(pProveedor.getValue());
            if (pr != null) idProveedor = pr.getIdProveedor();
            else {
                alerta("Proveedor no encontrado: " + pProveedor.getValue());
                return;
            }
        }

        // 16.6 — Armar Producto
        Producto prod = new Producto();
        prod.setIdProducto(editandoId == null ? 0 : editandoId);
        prod.setSku(sku);
        prod.setNombre(nombre);
        prod.setIdCategoria(idCategoria);
        prod.setIdProveedor(idProveedor == null ? 0 : idProveedor);
        prod.setPrecioCompra(precioCompra);
        prod.setPrecioVenta(precioVenta);
        prod.setStockActual(stock);
        prod.setStockMinimo(stockMin);

        final boolean esNuevo = (editandoId == null);

        // 16.7 — Guardar en hilo aparte
        new Thread(() -> {
            try {
                if (esNuevo) {
                    int id = dao.insertar(prod);
                    Platform.runLater(() -> {
                        if (id > 0) {
                            exito("Producto creado correctamente (ID " + id + ").");
                            cerrarModalProducto();
                            refrescar();
                        } else {
                            alerta("No se pudo crear el producto. Verifica los datos.");
                        }
                    });
                } else {
                    boolean ok = dao.actualizar(prod);
                    Platform.runLater(() -> {
                        if (ok) {
                            exito("Producto actualizado correctamente.");
                            cerrarModalProducto();
                            refrescar();
                        } else {
                            alerta("No se pudo actualizar. ¿El producto sigue existiendo?");
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

    /* ============================================================
       § 17. ELIMINAR PRODUCTO
       ============================================================ */
    private void confirmarEliminar(Producto p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el producto \"" + p.getNombre() + "\"?",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        if (!a.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) return;

        new Thread(() -> {
            try {
                boolean ok = dao.eliminar(p.getIdProducto());
                Platform.runLater(() -> {
                    if (ok) {
                        exito("Producto eliminado correctamente.");
                        refrescar();
                    } else {
                        alerta("No se pudo eliminar (ID " + p.getIdProducto() + ").");
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        alerta("Error al eliminar:\n\n" + ex.getMessage()));
            }
        }).start();
    }

    /* ============================================================
       § 18. MODAL MOVIMIENTO
       ============================================================ */
    @FXML
    private void abrirMovimiento() {
        if (mvProducto != null) {
            mvProducto.getItems().setAll(
                    datos.stream().map(Producto::getNombre).toList());
            mvProducto.setValue(null);
        }
        if (mvCantidad != null) mvCantidad.clear();
        if (mvMotivo != null)   mvMotivo.clear();

        modalMovimiento.setVisible(true);
        modalMovimiento.setManaged(true);
    }

    @FXML
    private void cerrarModalMovimiento() {
        modalMovimiento.setVisible(false);
        modalMovimiento.setManaged(false);
    }

    @FXML
    private void guardarMovimiento() {
        System.out.println("Movimiento: " + mvProducto.getValue()
                + " | Cant: " + mvCantidad.getText()
                + " | Motivo: " + mvMotivo.getText());
        // TODO: insertar en movimientos_inventario
        cerrarModalMovimiento();
        refrescar();
    }

    /* ============================================================
       § 19. CELDAS PERSONALIZADAS
       ============================================================ */
    private TableCell<Producto, String> celdaNombreConSku() {
        return new TableCell<>() {
            @Override protected void updateItem(String nombre, boolean empty) {
                super.updateItem(nombre, empty);
                if (empty || nombre == null) { setGraphic(null); setText(null); return; }

                Producto p = getTableView().getItems().get(getIndex());

                Label lblNombre = new Label(p.getNombre());
                lblNombre.getStyleClass().add("cell-name");

                Label lblSku = new Label(p.getSku());
                lblSku.getStyleClass().add("cell-sku");

                VBox box = new VBox(2, lblNombre, lblSku);
                setGraphic(box);
                setText(null);
            }
        };
    }

    private TableCell<Producto, Double> celdaMoneda() {
        return new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : MXN.format(v));
                setAlignment(Pos.CENTER_RIGHT);
            }
        };
    }

    private TableCell<Producto, Double> celdaMargen() {
        return new TableCell<>() {
            @Override protected void updateItem(Double margen, boolean empty) {
                super.updateItem(margen, empty);
                if (empty || margen == null) { setGraphic(null); setText(null); return; }

                Producto p = getTableView().getItems().get(getIndex());
                double pct = p.getPrecioCompra() > 0
                        ? (margen / p.getPrecioCompra()) * 100 : 0;

                Label lbl = new Label(String.format("%s (%.0f%%)", MXN.format(margen), pct));
                lbl.getStyleClass().add("cell-margen");
                if (margen < 0) lbl.getStyleClass().add("cell-margen-negative");

                setGraphic(lbl);
                setText(null);
                setAlignment(Pos.CENTER_RIGHT);
            }
        };
    }

    private TableCell<Producto, Integer> celdaStock() {
        return new TableCell<>() {
            @Override protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) { setGraphic(null); setText(null); return; }

                Producto p = getTableView().getItems().get(getIndex());
                int minimo = p.getStockMinimo();

                String nivel;
                if (stock == 0)                    nivel = "out";
                else if (stock <= minimo)          nivel = "low";
                else if (stock <= minimo * 2)      nivel = "medio";
                else                               nivel = "ok";

                Label badge = new Label(String.valueOf(stock));
                badge.getStyleClass().addAll("stock-badge", nivel);

                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private TableCell<Producto, Void> celdaAcciones() {
        return new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }

                Producto p = getTableView().getItems().get(getIndex());

                Button editar = new Button("✎");
                editar.getStyleClass().add("action-btn");
                editar.setOnAction(e -> abrirEditar(p));

                Button borrar = new Button("🗑");
                borrar.getStyleClass().addAll("action-btn", "danger");
                borrar.setOnAction(e -> confirmarEliminar(p));

                HBox box = new HBox(4, editar, borrar);
                box.setAlignment(Pos.CENTER_RIGHT);
                setGraphic(box);
            }
        };
    }

    /* ============================================================
       § 20. CÁLCULOS DEL MODAL
       ============================================================ */
    private void recalcularLote() {
        if (lCantidad == null) return;
        int cant = parseInt(lCantidad.getText());
        int por  = parseInt(lPorLote.getText());
        double costo = parseDouble(lCostoLote.getText());
        int total = cant * por;

        if (calcStock != null) calcStock.setText(total + " uds");

        if (calcCosto != null && calcTotal != null) {
            if (costo > 0 && total > 0) {
                calcCosto.setText(MXN.format(costo / total));
                calcTotal.setText(MXN.format(costo));
            } else {
                calcCosto.setText("—");
                calcTotal.setText(costo > 0 ? MXN.format(costo) : "—");
            }
        }
    }

    private void calcularMargen() {
        if (lblMargen == null || pPrecioCompra == null || pPrecioVenta == null) return;
        double c = parseDouble(pPrecioCompra.getText());
        double v = parseDouble(pPrecioVenta.getText());
        if (c == 0 && v == 0) { lblMargen.setText("—"); return; }
        double m = v - c;
        lblMargen.setText(MXN.format(m) + String.format(" (%.1f%%)", c > 0 ? (m / c) * 100 : 0));
        lblMargen.getStyleClass().remove("negative");
        if (m < 0) lblMargen.getStyleClass().add("negative");
    }

    /* ============================================================
       § 21. PARSEO
       ============================================================ */
    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
    }

    /* ============================================================
       § 22. FEEDBACK VISUAL (alerts)
       ============================================================ */
    private void alerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Aviso");
        aplicarEstilos(a);
        a.showAndWait();
    }

    private void exito(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Listo");
        aplicarEstilos(a);
        a.showAndWait();
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Error");
        aplicarEstilos(a);
        a.showAndWait();
    }

    private void aplicarEstilos(Alert a) {
        if (tabla == null || tabla.getScene() == null) return;
        a.getDialogPane().getStylesheets().addAll(tabla.getScene().getStylesheets());
        a.getDialogPane().getStyleClass().add("root");
    }
}