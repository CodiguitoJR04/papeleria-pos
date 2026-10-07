package org.papeleria_pos.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.util.Duration;

import org.papeleria_pos.dao.FacturaDAOImpl;
import org.papeleria_pos.dao.Interface.IFacturaDAO;
import org.papeleria_pos.dao.Interface.IProductoDAO;
import org.papeleria_pos.dao.Interface.ITurnoDAO;
import org.papeleria_pos.dao.Interface.IVentaDAO;
import org.papeleria_pos.dao.ProductoDAOImpl;
import org.papeleria_pos.dao.TurnoDAOImpl;
import org.papeleria_pos.dao.VentaDAOImpl;
import org.papeleria_pos.dto.FacturaRequest;
import org.papeleria_pos.dto.ItemCarrito;
import org.papeleria_pos.dto.TurnoAbierto;
import org.papeleria_pos.dto.VentaRequest;
import org.papeleria_pos.hardware.ImpresoraTermica;
import org.papeleria_pos.hardware.LectorCodigoBarras;
import org.papeleria_pos.models.Factura;
import org.papeleria_pos.models.Producto;
import org.papeleria_pos.session.Sesion;

import java.text.NumberFormat;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

public class PuntoDeVentaController {

    /* ============================================================
       Header
       ============================================================ */
    @FXML private Label lblTurno, lblCajero, lblCaja, lblReloj;

    /* ============================================================
       Buscador
       ============================================================ */
    @FXML private TextField txtEscaner;

    /* ============================================================
       Carrito
       ============================================================ */
    @FXML private TableView<ItemCarrito> tablaCarrito;
    @FXML private TableColumn<ItemCarrito, String>  colItemNombre;
    @FXML private TableColumn<ItemCarrito, Integer> colItemCantidad;
    @FXML private TableColumn<ItemCarrito, Double>  colItemPrecio;
    @FXML private TableColumn<ItemCarrito, Double>  colItemImporte;
    @FXML private TableColumn<ItemCarrito, Void>    colItemAcciones;

    /* ============================================================
       Panel de totales
       ============================================================ */
    @FXML private Label lblTotal, lblArticulos, lblSubtotal, lblIva;
    @FXML private Button btnCobrar, btnDescuento, btnCancelar;

    /* ============================================================
       Modal de cobro
       ============================================================ */
    @FXML private StackPane modalCobro;
    @FXML private Label     lblModalTotal, lblCambio;
    @FXML private TextField txtRecibido;
    @FXML private ToggleButton tbEfectivo, tbTarjeta, tbTransferencia;
    @FXML private CheckBox  chkFacturar;

    /* ============================================================
       Modal de factura
       ============================================================ */
    @FXML private StackPane modalFactura;
    @FXML private Label     lblFacturaTotal;
    @FXML private TextField facRfc, facRazon;
    @FXML private ComboBox<String> facRegimen, facUso, facMetodo;

    /* ============================================================
       DAOs y estado
       ============================================================ */
    private final IProductoDAO productoDAO = new ProductoDAOImpl();
    private final IVentaDAO    ventaDAO    = new VentaDAOImpl();
    private final ITurnoDAO    turnoDAO    = new TurnoDAOImpl();
    private final IFacturaDAO  facturaDAO  = new FacturaDAOImpl();

    private final ObservableList<ItemCarrito> carrito = FXCollections.observableArrayList();

    private TurnoAbierto turnoActual;
    private double descuento = 0;

    /** Guarda el id de la venta recién creada para poder facturarla. */
    private int idVentaPendienteFactura = -1;
    /** Total de la última venta, para mostrarlo en el modal de factura. */
    private double totalUltimaVenta = 0;

    /** Último folio, para el botón "Reimprimir" (F11). */
    private String ultimoFolio = null;

    private static final NumberFormat MXN =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"));
    private static final double IVA_RATE = 0.16;

    /* ============================================================
       Popup de búsqueda reactiva
       ============================================================ */
    private final Popup popupResultados = new Popup();
    private final ListView<Producto> listaResultados = new ListView<>();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(250));

    /* ============================================================
       Init
       ============================================================ */
    @FXML
    public void initialize() {
        configurarColumnas();
        configurarMetodoPago();
        configurarEscaner();
        configurarModalFactura();
        configurarAtajos();

        if (txtRecibido != null) {
            txtRecibido.textProperty().addListener((obs, viejo, nuevo) -> calcularCambio());
        }
        tablaCarrito.setItems(carrito);
        tablaCarrito.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        actualizarTotales();
        iniciarReloj();
        cargarTurno();

        Platform.runLater(() -> txtEscaner.requestFocus());
    }

    /* ============================================================
       Columnas del carrito
       ============================================================ */
    private void configurarColumnas() {
        colItemNombre.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getProducto().getNombre()));

        colItemCantidad.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getCantidad()));

        colItemPrecio.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getProducto().getPrecioVenta()));
        colItemPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : MXN.format(v));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        colItemImporte.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getImporte()));
        colItemImporte.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : MXN.format(v));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        colItemAcciones.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }

                ItemCarrito item = getTableView().getItems().get(getIndex());

                Button quitar = new Button("✕");
                quitar.getStyleClass().addAll("action-btn", "danger");
                quitar.setOnAction(e -> {
                    carrito.remove(item);
                    actualizarTotales();
                });

                HBox box = new HBox(quitar);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });
    }

    /* ============================================================
       Método de pago (ToggleButtons exclusivos)
       ============================================================ */
    private void configurarMetodoPago() {
        ToggleGroup grupo = new ToggleGroup();
        tbEfectivo.setToggleGroup(grupo);
        tbTarjeta.setToggleGroup(grupo);
        tbTransferencia.setToggleGroup(grupo);
        tbEfectivo.setSelected(true);
    }

    /* ============================================================
       Escáner + búsqueda reactiva
       ============================================================ */
    private void configurarEscaner() {
        // ---- Popup de resultados ----
        listaResultados.setPrefWidth(420);
        listaResultados.setPrefHeight(260);
        listaResultados.setStyle("-fx-font-size: 13px;");

        listaResultados.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Producto p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); return; }
                setText(String.format("%s  ·  %s  ·  $%.2f  ·  stock %d",
                        p.getNombre(), p.getSku(),
                        p.getPrecioVenta(), p.getStockActual()));
            }
        });

        listaResultados.setOnMouseClicked(e -> {
            Producto p = listaResultados.getSelectionModel().getSelectedItem();
            if (p != null) seleccionarDelPopup(p);
        });

        listaResultados.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                Producto p = listaResultados.getSelectionModel().getSelectedItem();
                if (p != null) seleccionarDelPopup(p);
            }
        });

        popupResultados.getContent().add(listaResultados);
        popupResultados.setAutoHide(true);

        // ---- Debounce ----
        debounce.setOnFinished(e -> buscarCoincidencias(txtEscaner.getText()));

        // ---- Escáner (Enter del lector) ----
        LectorCodigoBarras.attach(txtEscaner, codigo -> {
            buscarYAgregarPorSku(codigo);
            txtEscaner.clear();
            txtEscaner.requestFocus();
            popupResultados.hide();
        });

        // ---- Búsqueda reactiva al escribir ----
        txtEscaner.textProperty().addListener((o, a, b) -> {
            String q = b == null ? "" : b.trim();
            if (q.isEmpty()) {
                popupResultados.hide();
                return;
            }
            debounce.playFromStart();
        });

        txtEscaner.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) popupResultados.hide();
        });
    }

    private void buscarCoincidencias(String query) {
        new Thread(() -> {
            try {
                List<Producto> matches = productoDAO.buscarCoincidencias(query, 20);
                Platform.runLater(() -> {
                    if (matches.isEmpty()) {
                        popupResultados.hide();
                        return;
                    }
                    listaResultados.getItems().setAll(matches);
                    listaResultados.getSelectionModel().selectFirst();

                    Bounds b = txtEscaner.localToScreen(txtEscaner.getBoundsInLocal());
                    popupResultados.show(txtEscaner, b.getMinX(), b.getMaxY() + 2);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void seleccionarDelPopup(Producto p) {
        if (p.getStockActual() <= 0) {
            alerta("Sin stock: " + p.getNombre());
            return;
        }
        agregarAlCarrito(p);
        txtEscaner.clear();
        popupResultados.hide();
        txtEscaner.requestFocus();
    }

    private void buscarYAgregarPorSku(String codigo) {
        new Thread(() -> {
            try {
                List<Producto> lista = productoDAO.buscar(codigo, null, null);
                Platform.runLater(() -> {
                    if (lista.isEmpty()) {
                        alerta("Producto no encontrado: " + codigo);
                        return;
                    }
                    Producto p = lista.get(0);
                    if (p.getStockActual() <= 0) {
                        alerta("Sin stock: " + p.getNombre());
                        return;
                    }
                    agregarAlCarrito(p);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void agregarAlCarrito(Producto p) {
        for (ItemCarrito it : carrito) {
            if (it.getProducto().getIdProducto() == p.getIdProducto()) {
                if (it.getCantidad() + 1 > p.getStockActual()) {
                    alerta("Stock insuficiente: " + p.getNombre());
                    return;
                }
                it.setCantidad(it.getCantidad() + 1);
                tablaCarrito.refresh();
                actualizarTotales();
                return;
            }
        }
        carrito.add(new ItemCarrito(p, 1));
        actualizarTotales();
    }

    /* ============================================================
       Totales
       ============================================================ */
    private void actualizarTotales() {
        double subtotal = 0;
        int articulos = 0;

        for (ItemCarrito it : carrito) {
            subtotal += it.getImporte();
            articulos += it.getCantidad();
        }

        subtotal -= descuento;
        if (subtotal < 0) subtotal = 0;

        double iva = subtotal * IVA_RATE;
        double total = subtotal + iva;

        lblArticulos.setText(String.valueOf(articulos));
        lblSubtotal.setText(MXN.format(subtotal));
        lblIva.setText(MXN.format(iva));
        lblTotal.setText(MXN.format(total));

        boolean vacio = carrito.isEmpty();
        btnCobrar.setDisable(vacio);
        btnCancelar.setDisable(vacio);
        btnDescuento.setDisable(vacio);
    }

    private double getTotal() {
        double subtotal = carrito.stream()
                .mapToDouble(ItemCarrito::getImporte).sum() - descuento;
        if (subtotal < 0) subtotal = 0;
        return subtotal * (1 + IVA_RATE);
    }

    /* ============================================================
       Turno
       ============================================================ */
    private void cargarTurno() {
        int idCajero = Sesion.get().getIdUsuario();
        new Thread(() -> {
            try {
                TurnoAbierto t = turnoDAO.obtenerTurnoAbierto(idCajero);
                Platform.runLater(() -> {
                    if (t != null) {
                        turnoActual = t;
                        lblTurno.setText("Turno #" + t.getIdTurno());
                        lblCajero.setText(t.getCajero());
                        lblCaja.setText("Caja " + t.getCaja());
                    } else {
                        mostrarModalAbrirTurno();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void mostrarModalAbrirTurno() {
        TextInputDialog dlg = new TextInputDialog("500");
        dlg.setTitle("Abrir turno");
        dlg.setHeaderText("No tienes un turno abierto");
        dlg.setContentText("Monto inicial en caja:");
        dlg.showAndWait().ifPresent(montoStr -> {
            try {
                double monto = Double.parseDouble(montoStr);
                int idCajero = Sesion.get().getIdUsuario();
                int idTurno = turnoDAO.abrirTurno(idCajero, 1, monto);
                if (idTurno > 0) {
                    lblTurno.setText("Turno #" + idTurno);
                    lblCajero.setText(Sesion.get().getNombre());
                    lblCaja.setText("Caja 1");
                    cargarTurno();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /* ============================================================
       Modal de cobro
       ============================================================ */
    @FXML
    private void abrirCobro() {
        if (carrito.isEmpty()) return;

        double total = getTotal();
        lblModalTotal.setText(MXN.format(total));

        txtRecibido.setText(String.format(Locale.US, "%.2f", total));

        lblCambio.setText(MXN.format(0));
        tbEfectivo.setSelected(true);
        chkFacturar.setSelected(false);

        modalCobro.setVisible(true);
        modalCobro.setManaged(true);
        Platform.runLater(() -> {
            txtRecibido.requestFocus();
            txtRecibido.selectAll();
        });
    }

    @FXML
    private void cerrarCobro() {
        modalCobro.setVisible(false);
        modalCobro.setManaged(false);
        txtEscaner.requestFocus();
    }

    @FXML
    private void calcularCambio() {
        try {
            double total = getTotal();
            double rec   = parseMonto(txtRecibido.getText());
            double diff  = rec - total;
            lblCambio.setText(MXN.format(Math.abs(diff)));
            lblCambio.getStyleClass().remove("falta");
            if (diff < 0) lblCambio.getStyleClass().add("falta");
        } catch (Exception e) {
            lblCambio.setText(MXN.format(0));
        }
    }

    private double parseMonto(String s) {
        if (s == null) return 0;
        String limpio = s.trim().replace("$", "").replace(" ", "");
        if (limpio.isEmpty()) return 0;

        int lastComma = limpio.lastIndexOf(',');
        int lastDot   = limpio.lastIndexOf('.');

        if (lastComma > lastDot) {
            limpio = limpio.replace(".", "").replace(",", ".");
        } else {
            limpio = limpio.replace(",", "");
        }
        try { return Double.parseDouble(limpio); }
        catch (Exception e) { return 0; }
    }

    /* ============================================================
       CONFIRMAR COBRO  ← integración con impresora
       ============================================================ */
    @FXML
    private void confirmarCobro() {
        if (turnoActual == null) {
            alerta("No hay turno abierto");
            return;
        }

        double total = getTotal();
        double recibido = parseMonto(txtRecibido.getText());
        if (recibido <= 0 && !"0".equals(txtRecibido.getText().trim())) {
            alerta("Monto recibido inválido");
            return;
        }

        String metodo;
        if (tbTarjeta.isSelected())            metodo = "TARJETA";
        else if (tbTransferencia.isSelected()) metodo = "TRANSFERENCIA";
        else                                   metodo = "EFECTIVO";

        if ("EFECTIVO".equals(metodo) && recibido < total) {
            alerta("El monto recibido es menor al total");
            return;
        }

        final boolean facturar = chkFacturar.isSelected();
        final String metodoFinal = metodo;
        final double recibidoFinal = recibido;
        final double totalFinal = total;
        final double descuentoFinal = descuento;

        // 🔑 Datos del ticket ANTES de limpiar el carrito
        final List<ItemCarrito> itemsParaTicket = List.copyOf(carrito);
        final String cajeroNombre = Sesion.get().getNombre();
        final String turnoTexto = turnoActual != null
                ? "#" + turnoActual.getIdTurno() : "—";

        VentaRequest req = new VentaRequest(
                Sesion.get().getIdUsuario(),
                turnoActual.getIdTurno(),
                metodoFinal,
                recibidoFinal,
                itemsParaTicket);

        new Thread(() -> {
            try {
                String folio = ventaDAO.registrarVentaCompleta(req);
                Platform.runLater(() -> {
                    if (folio == null) {
                        alerta("No se pudo registrar la venta");
                        return;
                    }

                    // Guardar datos para facturación / reimpresión
                    idVentaPendienteFactura = obtenerIdVentaPorFolio(folio);
                    totalUltimaVenta = totalFinal;
                    ultimoFolio = folio;

                    // 🖨️ Imprimir ticket + abrir cajón (en background)
                    double cambioTicket = Math.max(0, recibidoFinal - totalFinal);
                    imprimirTicketAsync(
                            folio,
                            cajeroNombre,
                            turnoTexto,
                            itemsParaTicket,
                            descuentoFinal,
                            totalFinal,
                            metodoFinal,
                            recibidoFinal,
                            cambioTicket);

                    // Limpiar carrito
                    carrito.clear();
                    descuento = 0;
                    actualizarTotales();

                    cerrarCobro();
                    // ⚠️ Ya NO llamamos abrirCajon() — el ticket abre el cajón

                    if (facturar) {
                        abrirModalFactura();
                    } else {
                        info("Venta registrada: " + folio);
                        txtEscaner.requestFocus();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> alerta("Error: " + e.getMessage()));
            }
        }).start();
    }

    /* ============================================================
       🖨️ IMPRESIÓN DEL TICKET (en hilo aparte)
       ============================================================ */
    /**
     * Imprime el ticket en un hilo separado. Nunca bloquea la UI.
     * Si falla la impresora, la venta ya está en BD — solo se loguea.
     */
    private void imprimirTicketAsync(String folio,
                                     String cajero,
                                     String turno,
                                     List<ItemCarrito> items,
                                     double descuento,
                                     double total,
                                     String metodoPago,
                                     double recibido,
                                     double cambio) {
        new Thread(() -> {
            try {
                // Reconstruir subtotal e IVA (porque 'total' = subtotal + iva)
                double subtotal = items.stream()
                        .mapToDouble(ItemCarrito::getImporte).sum() - descuento;
                if (subtotal < 0) subtotal = 0;
                double iva = subtotal * IVA_RATE;

                ImpresoraTermica.imprimirTicket(
                        folio, cajero, turno, items,
                        subtotal, iva, descuento, total,
                        metodoPago, recibido, cambio);

                System.out.println("✔ Ticket enviado a la impresora: " + folio);

            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("⚠ No se pudo imprimir el ticket " + folio
                        + ": " + ex.getMessage());
                // Opcional: mostrar toast discreto en la UI
                Platform.runLater(() -> {
                    // No bloquees con Alert. Solo log o un toast pequeño.
                    System.err.println("Venta registrada. La impresora no respondió.");
                });
            }
        }).start();
    }

    /* ============================================================
       🖨️ REIMPRIMIR TICKET (F11)
       ============================================================ */
    @FXML
    private void reimprimirTicket() {
        if (ultimoFolio == null || idVentaPendienteFactura <= 0) {
            alerta("No hay ticket reciente para reimprimir.");
            return;
        }
        // Reimprime los mismos datos del último ticket
        // (en una versión más completa se cargaría desde BD por folio)
        alerta("Función de reimpresión: reimprimir folio " + ultimoFolio
                + ".\n(Integrar con carga desde BD si se desea reimpresión histórica.)");
    }

    /* ============================================================
       Modal de factura
       ============================================================ */
    private void configurarModalFactura() {
        if (facRegimen != null) {
            facRegimen.getItems().addAll(
                    "601 - General de Ley Personas Morales",
                    "612 - Personas Físicas con Actividades Empresariales",
                    "616 - Sin obligaciones fiscales");
            facRegimen.setValue("616 - Sin obligaciones fiscales");
        }
        if (facUso != null) {
            facUso.getItems().addAll(
                    "G01 - Adquisición de mercancías",
                    "G03 - Gastos en general",
                    "P01 - Por definir");
            facUso.setValue("G03 - Gastos en general");
        }
        if (facMetodo != null) {
            facMetodo.getItems().addAll(
                    "PUE - Pago en una sola exhibición",
                    "PPD - Pago en parcialidades o diferido");
            facMetodo.setValue("PUE - Pago en una sola exhibición");
        }
    }

    @FXML
    private void abrirModalFactura() {
        if (idVentaPendienteFactura <= 0) {
            alerta("No hay una venta pendiente de facturar");
            return;
        }

        lblFacturaTotal.setText(MXN.format(totalUltimaVenta));

        facRfc.setText("XAXX010101000");
        facRazon.setText("Público en general");
        facRegimen.setValue("616 - Sin obligaciones fiscales");
        facUso.setValue("G03 - Gastos en general");
        facMetodo.setValue("PUE - Pago en una sola exhibición");

        modalFactura.setVisible(true);
        modalFactura.setManaged(true);
        Platform.runLater(() -> facRfc.requestFocus());
    }

    @FXML
    private void cerrarModalFactura() {
        modalFactura.setVisible(false);
        modalFactura.setManaged(false);
        txtEscaner.requestFocus();
    }

    @FXML
    private void confirmarFactura() {
        if (idVentaPendienteFactura <= 0) {
            alerta("Venta pendiente no encontrada");
            return;
        }

        String rfc = facRfc.getText() == null
                ? "" : facRfc.getText().trim().toUpperCase();
        String razon = facRazon.getText() == null
                ? "" : facRazon.getText().trim();

        if (rfc.isEmpty() || razon.isEmpty()) {
            alerta("RFC y Razón social son obligatorios");
            return;
        }

        String regimen = extraerClave(facRegimen.getValue());
        String uso     = extraerClave(facUso.getValue());
        String metodo  = extraerClave(facMetodo.getValue());

        FacturaRequest req = new FacturaRequest(
                idVentaPendienteFactura, rfc, razon, regimen, uso, metodo);

        new Thread(() -> {
            try {
                Factura f = facturaDAO.generar(req);
                Platform.runLater(() -> {
                    if (f != null) {
                        info("Factura generada: " + f.getSerieFolio()
                                + "\nTotal: " + MXN.format(f.getTotal()));
                        idVentaPendienteFactura = -1;
                        cerrarModalFactura();
                        txtEscaner.requestFocus();
                    } else {
                        alerta("No se pudo generar la factura");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> alerta("Error: " + e.getMessage()));
            }
        }).start();
    }

    private String extraerClave(String valorCombo) {
        if (valorCombo == null) return "";
        int idx = valorCombo.indexOf(" - ");
        return idx > 0 ? valorCombo.substring(0, idx).trim() : valorCombo.trim();
    }

    /* ============================================================
       Helpers
       ============================================================ */
    private int obtenerIdVentaPorFolio(String folio) {
        String sql = "SELECT id_venta FROM ventas WHERE folio = ?";
        try (var ps = org.papeleria_pos.config.DatabaseConnection
                .get().prepareStatement(sql)) {
            ps.setString(1, folio);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    /* ============================================================
       Descuento
       ============================================================ */
    @FXML
    private void aplicarDescuento() {
        TextInputDialog dlg = new TextInputDialog(String.format("%.2f", descuento));
        dlg.setTitle("Descuento");
        dlg.setHeaderText("Descuento en pesos");
        dlg.setContentText("Monto:");
        dlg.showAndWait().ifPresent(s -> {
            try {
                descuento = Math.max(0, Double.parseDouble(s));
                actualizarTotales();
            } catch (Exception ignored) {}
        });
    }

    /* ============================================================
       Cancelar venta
       ============================================================ */
    @FXML
    private void cancelarVenta() {
        if (carrito.isEmpty()) return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Cancelar la venta actual?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        if (a.showAndWait().filter(b -> b == ButtonType.YES).isPresent()) {
            carrito.clear();
            descuento = 0;
            actualizarTotales();
            txtEscaner.requestFocus();
        }
    }

    /* ============================================================
       Enfoque del escáner
       ============================================================ */
    @FXML
    private void enfocarEscaner() {
        LectorCodigoBarras.enfocar(txtEscaner);
    }

    /* ============================================================
       Atajos de teclado globales
       ============================================================ */
    private void configurarAtajos() {
        Platform.runLater(() -> {
            if (txtEscaner.getScene() != null) {
                txtEscaner.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    switch (e.getCode()) {
                        case F2 -> { e.consume(); txtEscaner.requestFocus(); txtEscaner.selectAll(); }
                        case F4 -> { e.consume(); aplicarDescuento(); }
                        case F8 -> { e.consume(); cancelarVenta(); }
                        case F11 -> { e.consume(); reimprimirTicket(); }
                        case F12 -> {
                            e.consume();
                            if (modalFactura.isVisible()) {
                                confirmarFactura();
                            } else if (modalCobro.isVisible()) {
                                confirmarCobro();
                            } else {
                                abrirCobro();
                            }
                        }
                        case ESCAPE -> {
                            if (modalFactura.isVisible()) { e.consume(); cerrarModalFactura(); }
                            else if (modalCobro.isVisible()) { e.consume(); cerrarCobro(); }
                        }
                    }
                });
            }
        });
    }

    /* ============================================================
       Cerrar sesión
       ============================================================ */
    @FXML
    private void cerrarSesion() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Cerrar sesión?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        if (a.showAndWait().filter(b -> b == ButtonType.YES).isPresent()) {
            Sesion.get().cerrar();
            org.papeleria_pos.App.getInstance().mostrarLogin();
        }
    }

    /* ============================================================
       Reloj
       ============================================================ */
    private void iniciarReloj() {
        actualizarReloj();
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(30),
                e -> actualizarReloj()));
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
    }

    private void actualizarReloj() {
        LocalTime d = LocalTime.now();
        lblReloj.setText(String.format("%02d:%02d", d.getHour(), d.getMinute()));
    }

    /* ============================================================
       Alerts
       ============================================================ */
    private void alerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}