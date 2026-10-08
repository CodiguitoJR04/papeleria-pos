package org.papeleria_pos.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.papeleria_pos.dao.*;
import org.papeleria_pos.dao.Interface.IMovimientoDAO;
import org.papeleria_pos.dao.Interface.IVentaDAO;
import org.papeleria_pos.dto.*;
import org.papeleria_pos.models.Venta;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static java.util.Collections.addAll;

public class ReportesController {
    private final IVentaDAO     dao           = new VentaDAOImpl();
    private final IMovimientoDAO movimientoDAO = new MovimientoDAOImpl();
    FacturaDAOImpl facturaDAO =  new FacturaDAOImpl();
    TurnoDAOImpl turnoDAO = new TurnoDAOImpl();

    /* ============ Header ============ */
    @FXML private Label lblRango, lblFiltro, lblResumen;

    /* ============ Filtros ============ */
    @FXML private ComboBox<String> cbPeriodo, cbCajero, cbMetodo;

    /* ============ KPIs ============ */
    @FXML private Label kpiVentas, kpiTicket, kpiArticulos, kpiMargen;
    @FXML private Label lblDeltaVentas, lblDeltaTicket, lblDeltaArticulos, lblDeltaMargen;

    /* ============ Gráfica + top ============ */
    @FXML private BarChart<String, Number> chartVentas;
    @FXML private VBox topProductos;

    /* ============ Tabs ============ */
    @FXML private Button tabVentas, tabMovimientos, tabFacturas, tabTurnos;

    /* ============ Tabla ============ */
    @SuppressWarnings("rawtypes")
    @FXML private TableView tabla;



    private static final NumberFormat MXN =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"));
    private static final DateTimeFormatter FECHA_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String tabActivo = "ventas";

    /* ============================================================
       Init
       ============================================================ */
    @FXML
    public void initialize() {
        // 🔑 Elimina la columna fantasma del final
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ---- Filtro período ----
        cbPeriodo.getItems().addAll("Hoy", "Últimos 7 días", "Últimos 30 días", "Mes actual");
        cbPeriodo.setValue("Últimos 7 días");

        // ---- Filtro cajero: cargar usernames reales desde la BD ----
        cbCajero.getItems().add("Todos");
        new Thread(() -> {
            try {
                List<UsuarioResumen> usuarios = new UsuarioDAOImpl().listarTodos();
                List<String> usernames = new ArrayList<>();
                for (UsuarioResumen u : usuarios) {
                    if (u.isActivo()) usernames.add(u.getUsername());
                }
                Collections.sort(usernames);
                Platform.runLater(() -> {
                    cbCajero.getItems().addAll(usernames);
                    cbCajero.setValue("Todos");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // ---- Filtro método ----
        cbMetodo.getItems().addAll("Todos", "EFECTIVO", "TARJETA");
        cbMetodo.setValue("Todos");

        // ---- Listeners ----
        cbPeriodo.valueProperty().addListener((o, a, b) -> cargarTodo());
        cbCajero.valueProperty().addListener((o, a, b) -> cargarTodo());
        cbMetodo.valueProperty().addListener((o, a, b) -> cargarTodo());

        // ---- Carga inicial ----
        cargarTodo();
    }

    /* ============================================================
       Carga general (al cambiar filtros)
       ============================================================ */
    private void cargarTodo() {
        LocalDate[] rango = rangoActual();
        LocalDate desde = rango[0];
        LocalDate hasta = rango[1];

        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("es-MX"));
        lblRango.setText(desde.format(f) + " – " + hasta.format(f) + " · " + cbPeriodo.getValue());
        lblFiltro.setText(cbPeriodo.getValue());

        List<Venta> ventasBase = dao.obtenerPorRango(desde, hasta);
        List<Venta> ventas = aplicarFiltros(ventasBase);

        cargarKPIs(desde, hasta, ventas);
        cargarGrafica(desde, hasta, ventas);
        cargarTop(desde, hasta, ventas);
        cargarTabla();   // 🔑 sin parámetros
    }

    /* ============================================================
       Filtros en memoria
       ============================================================ */
    private List<Venta> aplicarFiltros(List<Venta> base) {
        String cajero = cbCajero != null ? cbCajero.getValue() : null;
        String metodo = cbMetodo != null ? cbMetodo.getValue() : null;

        return base.stream()
                .filter(v -> cajero == null || "Todos".equals(cajero)
                        || cajero.equals(v.getUsuario()))
                .filter(v -> metodo == null || "Todos".equals(metodo)
                        || metodo.equals(v.getMetodoPago()))
                .toList();
    }

    private LocalDate[] rangoActual() {
        LocalDate hoy = LocalDate.now();
        String p = cbPeriodo.getValue();
        if ("Hoy".equals(p))              return new LocalDate[] { hoy, hoy };
        if ("Últimos 7 días".equals(p))   return new LocalDate[] { hoy.minusDays(6), hoy };
        if ("Últimos 30 días".equals(p))  return new LocalDate[] { hoy.minusDays(29), hoy };
        if ("Mes actual".equals(p))       return new LocalDate[] { hoy.withDayOfMonth(1), hoy };
        return new LocalDate[] { hoy.minusDays(6), hoy };
    }

    /* ============================================================
       KPIs
       ============================================================ */
    private void cargarKPIs(LocalDate desde, LocalDate hasta, List<Venta> ventas) {
        double total = ventas.stream()
                .filter(v -> "COMPLETADA".equals(v.getEstado()))
                .mapToDouble(Venta::getTotal).sum();

        long tickets = ventas.stream()
                .filter(v -> "COMPLETADA".equals(v.getEstado()))
                .count();

        int articulos = (int) (tickets * 2.5);
        double margen = total * 0.4;
        double ticket = tickets > 0 ? total / tickets : 0;

        kpiVentas.setText(MXN.format(total));
        kpiTicket.setText(MXN.format(ticket));
        kpiArticulos.setText(String.valueOf(articulos));
        kpiMargen.setText(MXN.format(margen));

        lblDeltaVentas.setText("—");
        lblDeltaTicket.setText("—");
        lblDeltaArticulos.setText("—");
        lblDeltaMargen.setText("—");
    }

    /* ============================================================
       Gráfica
       ============================================================ */
    private void cargarGrafica(LocalDate desde, LocalDate hasta, List<Venta> ventas) {
        var mapa = new java.util.TreeMap<LocalDate, Double>();
        LocalDate cur = desde;
        while (!cur.isAfter(hasta)) {
            mapa.put(cur, 0.0);
            cur = cur.plusDays(1);
        }

        for (Venta v : ventas) {
            if (!"COMPLETADA".equals(v.getEstado())) continue;
            if (v.getFecha() == null) continue;
            LocalDate d = v.getFecha().toLocalDate();
            mapa.merge(d, v.getTotal(), Double::sum);
        }

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("es-MX"));
        mapa.forEach((fecha, total) -> {
            String label = fecha.format(f);
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
            serie.getData().add(new XYChart.Data<>(label, total));
        });
        chartVentas.getData().setAll(serie);
    }

    /* ============================================================
       Top productos
       ============================================================ */
    private void cargarTop(LocalDate desde, LocalDate hasta, List<Venta> ventas) {
        List<TopProducto> top = dao.obtenerTopProductos(desde, hasta, 5);
        topProductos.getChildren().clear();

        if (top.isEmpty()) {
            Label vacio = new Label("Sin ventas en el período.");
            vacio.getStyleClass().add("content-subtitle");
            topProductos.getChildren().add(vacio);
            return;
        }

        int max = top.stream().mapToInt(TopProducto::getUnidadesVendidas).max().orElse(1);
        for (int i = 0; i < top.size(); i++) {
            TopProducto t = top.get(i);

            HBox row = new HBox();
            row.getStyleClass().add("top-item-row");
            Label rank = new Label(String.valueOf(i + 1));
            rank.getStyleClass().add("top-rank");
            Label nombre = new Label(t.getNombre());
            nombre.getStyleClass().add("top-name");
            HBox.setHgrow(nombre, Priority.ALWAYS);
            Label valor = new Label(t.getUnidadesVendidas() + " uds");
            valor.getStyleClass().add("top-value");
            row.getChildren().addAll(rank, nombre, valor);

            Pane bar = new Pane();
            bar.getStyleClass().add("top-bar");
            bar.setMaxWidth(Double.MAX_VALUE);
            Pane fill = new Pane();
            fill.getStyleClass().add("top-bar-fill");
            final int maxFinal = max;
            fill.prefWidthProperty().bind(bar.widthProperty()
                    .multiply((double) t.getUnidadesVendidas() / maxFinal));
            bar.getChildren().add(fill);

            VBox item = new VBox(row, bar);
            item.getStyleClass().add("top-item");
            topProductos.getChildren().add(item);
        }
    }

    /* ============================================================
       Tabla — SOLO recarga la tabla según tab activo
       ============================================================ */
    private void cargarTabla() {
        LocalDate[] rango = rangoActual();
        List<Venta> ventasBase = dao.obtenerPorRango(rango[0], rango[1]);
        List<Venta> ventas = aplicarFiltros(ventasBase);

        switch (tabActivo) {
            case "ventas":      tablaVentas(ventas);                    break;
            case "movimientos": tablaMovimientos(rango[0], rango[1]);   break;
            case "facturas":    tablaFacturas(rango[0], rango[1]);      break;
            case "turnos":      tablaTurnos(rango[0], rango[1]);        break;
        }
    }

    /* ============================================================
   Tabla de facturas
   ============================================================ */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tablaFacturas(LocalDate desde, LocalDate hasta) {
        tabla.getColumns().clear();

        /* Serie-Folio */
        TableColumn<FacturaResumen, String> cFolio = new TableColumn<>("Folio");
        cFolio.setCellValueFactory(c -> {
            FacturaResumen f = c.getValue();
            return new SimpleStringProperty(f.getSerie() + "-" + f.getFolio());
        });
        cFolio.setPrefWidth(100);

        /* Fecha */
        TableColumn<FacturaResumen, String> cFecha = new TableColumn<>("Fecha");
        cFecha.setCellValueFactory(c -> {
            var fecha = c.getValue().getFecha();
            return new SimpleStringProperty(fecha != null
                    ? fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "");
        });
        cFecha.setPrefWidth(120);

        /* RFC */
        TableColumn<FacturaResumen, String> cRfc = new TableColumn<>("RFC");
        cRfc.setCellValueFactory(c ->
                    new SimpleStringProperty(c.getValue().getRfc()));
        cRfc.setPrefWidth(140);

        /* Razón social */
        TableColumn<FacturaResumen, String> cRazon = new TableColumn<>("Razón social");
        cRazon.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRazonSocial()));
        cRazon.setPrefWidth(260);

        /* Total */
        TableColumn<FacturaResumen, Double> cTotal = new TableColumn<>("Total");
        cTotal.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getTotal()));
        cTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : MXN.format(v));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        cTotal.setPrefWidth(120);

        /* Estado */
        TableColumn<FacturaResumen, String> cEstado = new TableColumn<>("Estado");
        cEstado.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEstado()));
        cEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add(
                        "GENERADA".equals(s)  ? "ok"  :
                                "CANCELADA".equals(s) ? "out" : "neutral");
                setGraphic(badge);
                setText(null);
            }
        });
        cEstado.setPrefWidth(120);

        tabla.getColumns().addAll(cFolio, cFecha, cRfc, cRazon, cTotal, cEstado);

        /* Cargar datos */
        List<FacturaResumen> facturas = facturaDAO.listarPorRango(desde, hasta);
        tabla.setPlaceholder(new Label("Sin facturas en el período."));
        tabla.setItems(FXCollections.observableArrayList((List) facturas));

        /* Resumen */
        double totalFacturado = facturas.stream()
                .filter(f -> "GENERADA".equals(f.getEstado()))
                .mapToDouble(FacturaResumen::getTotal).sum();
        lblResumen.setText(
                facturas.size() + " facturas · Total facturado: "
                        + MXN.format(totalFacturado));
    }

    /* ============================================================
   Tabla de turnos
   ============================================================ */
    /* ============================================================
   Tabla de turnos
   ============================================================ */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tablaTurnos(LocalDate desde, LocalDate hasta) {
        tabla.getColumns().clear();

        /* Turno # */
        TableColumn<TurnoResumen, String> cTurno = new TableColumn<>("Turno");
        cTurno.setCellValueFactory(c ->
                new SimpleStringProperty("#" + c.getValue().getId()));
        cTurno.setPrefWidth(90);

        /* Cajero */
        TableColumn<TurnoResumen, String> cCajero = new TableColumn<>("Cajero");
        cCajero.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCajero()));
        cCajero.setPrefWidth(180);

        /* Caja */
        TableColumn<TurnoResumen, String> cCaja = new TableColumn<>("Caja");
        cCaja.setCellValueFactory(c ->
                new SimpleStringProperty("Caja " + c.getValue().getCaja()));
        cCaja.setPrefWidth(80);

        /* Apertura */
        TableColumn<TurnoResumen, String> cApertura = new TableColumn<>("Apertura");
        cApertura.setCellValueFactory(c -> {
            var f = c.getValue().getApertura();
            return new SimpleStringProperty(f != null ? f.format(FECHA_FMT) : "");
        });
        cApertura.setPrefWidth(140);

        /* Cierre */
        TableColumn<TurnoResumen, String> cCierre = new TableColumn<>("Cierre");
        cCierre.setCellValueFactory(c -> {
            var f = c.getValue().getCierre();
            return new SimpleStringProperty(f != null ? f.format(FECHA_FMT) : "—");
        });
        cCierre.setPrefWidth(140);

        /* # Ventas */
        TableColumn<TurnoResumen, Integer> cNumVentas = new TableColumn<>("Ventas");
        cNumVentas.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getNumVentas()));
        cNumVentas.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.toString());
                setAlignment(Pos.CENTER);
            }
        });
        cNumVentas.setPrefWidth(80);

        /* Total ventas */
        TableColumn<TurnoResumen, Double> cTotal = new TableColumn<>("Total");
        cTotal.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getTotalVentas()));
        cTotal.setCellFactory(c -> celdaMoneda());
        cTotal.setPrefWidth(120);

        /* Diferencia (ya viene calculada del DTO) */
        TableColumn<TurnoResumen, Double> cDif = new TableColumn<>("Diferencia");
        cDif.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getDiferencia()));
        cDif.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); setText(null); return; }

                Label lbl = new Label(MXN.format(v));
                if (Math.abs(v) < 0.01)
                    lbl.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: 600;");   // Cuadra
                else if (v > 0)
                    lbl.setStyle("-fx-text-fill: #E53935; -fx-font-weight: 600;");   // Faltante
                else
                    lbl.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: 600;");   // Sobrante
                setGraphic(lbl);
                setText(null);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        cDif.setPrefWidth(120);

        /* Estado */
        TableColumn<TurnoResumen, String> cEstado = new TableColumn<>("Estado");
        cEstado.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEstado()));
        cEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add(
                        "ABIERTO".equals(s) ? "info" : "neutral");
                setGraphic(badge);
                setText(null);
            }
        });
        cEstado.setPrefWidth    (110);

        tabla.getColumns().addAll(cTurno, cCajero, cCaja, cApertura, cCierre,
                cNumVentas, cTotal, cDif, cEstado);

        /* Cargar datos */
        List<TurnoResumen> turnos = turnoDAO.listarPorRango(desde, hasta);
        tabla.setPlaceholder(new Label("Sin turnos en el período."));
        tabla.setItems(FXCollections.observableArrayList((List) turnos));

        /* Resumen */
        long abiertos = turnos.stream().filter(t -> "ABIERTO".equals(t.getEstado())).count();
        double totalVendido = turnos.stream().mapToDouble(TurnoResumen::getTotalVentas).sum();
        lblResumen.setText(
                turnos.size() + " turnos · " + abiertos + " abiertos · Total: "
                        + MXN.format(totalVendido));
    }

    /* ============================================================
   Tabla de movimientos
   ============================================================ */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tablaMovimientos(LocalDate desde, LocalDate hasta) {
        tabla.getColumns().clear();

        /* Columna FECHA */
        TableColumn<MovimientoResumen, String> cFecha = new TableColumn<>("Fecha");
        cFecha.setCellValueFactory(c -> {
            var f = c.getValue().getFecha();
            return new SimpleStringProperty(f != null ? f.format(FECHA_FMT) : "");
        });
        cFecha.setPrefWidth(140);

        /* Columna PRODUCTO */
        TableColumn<MovimientoResumen, String> cProducto = new TableColumn<>("Producto");
        cProducto.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getProducto()));
        cProducto.setPrefWidth(240);

        /* Columna TIPO (badge con color) */
        TableColumn<MovimientoResumen, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTipo()));
        cTipo.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String tipo, boolean empty) {
                super.updateItem(tipo, empty);
                if (empty || tipo == null) { setGraphic(null); setText(null); return; }

                String etiqueta;
                String clase;
                switch (tipo) {
                    case "SALIDA_VENTA"      -> { etiqueta = "Venta";   clase = "info"; }
                    case "ENTRADA_COMPRA"    -> { etiqueta = "Entrada"; clase = "ok"; }
                    case "MERMA"             -> { etiqueta = "Merma";   clase = "out"; }
                    case "AJUSTE_INVENTARIO" -> { etiqueta = "Ajuste";  clase = "low"; }
                    default                  -> { etiqueta = tipo;      clase = "neutral"; }
                }

                Label badge = new Label(etiqueta);
                badge.getStyleClass().addAll("badge", clase);
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER_LEFT);
            }
        });
        cTipo.setPrefWidth(110);

        /* Columna CANTIDAD (con signo y color) */
        TableColumn<MovimientoResumen, Integer> cCantidad = new TableColumn<>("Cantidad");
        cCantidad.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getCantidad()));
        cCantidad.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer cant, boolean empty) {
                super.updateItem(cant, empty);
                if (empty || cant == null) { setGraphic(null); setText(null); return; }

                MovimientoResumen m = getTableView().getItems().get(getIndex());
                boolean salida = m.esSalida();
                String texto = (salida ? "-" : "+") + cant;

                Label lbl = new Label(texto);
                lbl.setStyle(salida
                        ? "-fx-text-fill: #E53935; -fx-font-weight: 600;"
                        : "-fx-text-fill: #2E7D32; -fx-font-weight: 600;");

                setGraphic(lbl);
                setText(null);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        cCantidad.setPrefWidth(90);

        /* Columna MOTIVO */
        TableColumn<MovimientoResumen, String> cMotivo = new TableColumn<>("Motivo");
        cMotivo.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getMotivo()));
        cMotivo.setPrefWidth(280);

        /* Columna USUARIO */
        TableColumn<MovimientoResumen, String> cUsuario = new TableColumn<>("Usuario");
        cUsuario.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsuario()));
        cUsuario.setPrefWidth(140);

        /* Columna REFERENCIA */
        TableColumn<MovimientoResumen, String> cReferencia = new TableColumn<>("Referencia");
        cReferencia.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getReferencia()));
        cReferencia.setPrefWidth(120);

        tabla.getColumns().addAll(cFecha, cProducto, cTipo, cCantidad,
                cMotivo, cUsuario, cReferencia);

        /* Cargar datos */
        List<MovimientoResumen> movimientos = movimientoDAO.listarPorRango(desde, hasta);
        tabla.setPlaceholder(new Label("Sin movimientos en el período."));
        tabla.setItems(FXCollections.observableArrayList((List) movimientos));

        /* Resumen al pie */
        long entradas  = movimientos.stream().filter(m -> !m.esSalida()).count();
        long salidas   = movimientos.stream().filter(MovimientoResumen::esSalida).count();
        lblResumen.setText(
                movimientos.size() + " movimientos · "
                        + entradas + " entradas · "
                        + salidas + " salidas");
    }

    /* ============================================================
       Tabla de ventas

       ============================================================ */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tablaVentas(List<Venta> ventas) {
        tabla.getColumns().clear();

        TableColumn<Venta, String> cFolio = new TableColumn<>("Folio");
        cFolio.setCellValueFactory(c ->
                new SimpleStringProperty("#" + c.getValue().getFolio()));

        TableColumn<Venta, String> cCajero = new TableColumn<>("Cajero");
        cCajero.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsuario()));

        TableColumn<Venta, String> cFecha = new TableColumn<>("Fecha");
        cFecha.setCellValueFactory(c -> {
            var fecha = c.getValue().getFecha();
            return new SimpleStringProperty(fecha != null ? fecha.format(FECHA_FMT) : "");
        });

        TableColumn<Venta, String> cMetodo = new TableColumn<>("Método");
        cMetodo.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getMetodoPago()));

        TableColumn<Venta, Double> cTotal = new TableColumn<>("Total");
        cTotal.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getTotal()));
        cTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : MXN.format(v));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        TableColumn<Venta, String> cEstado = new TableColumn<>("Estado");
        cEstado.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEstado()));
        cEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add(
                        s.equals("COMPLETADA") ? "ok" : "out");
                setGraphic(badge);
                setText(null);
            }
        });

        tabla.getColumns().addAll(cFolio, cCajero, cFecha, cMetodo, cTotal, cEstado);
        tabla.setPlaceholder(new Label("Sin ventas en el período."));
        tabla.setItems(FXCollections.observableArrayList(ventas));

        double totalPeriodo = ventas.stream()
                .filter(v -> "COMPLETADA".equals(v.getEstado()))
                .mapToDouble(Venta::getTotal).sum();
        lblResumen.setText(ventas.size() + " registros · Total: " + MXN.format(totalPeriodo));
    }

    /* ============================================================
       Tabla vacía (para tabs no implementados)
       ============================================================ */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tablaVacia(String mensaje) {
        // Limpiamos las columnas dinámicas, pero mostramos placeholder
        tabla.getColumns().clear();
        tabla.setItems(FXCollections.observableArrayList());

        Label vacio = new Label(mensaje);
        vacio.getStyleClass().add("content-subtitle");
        tabla.setPlaceholder(vacio);

        lblResumen.setText(mensaje);
    }

    /* ============================================================
       Tabs — SOLO recargan la tabla
       ============================================================ */
    @FXML private void verTabVentas() {
        tabActivo = "ventas";
        marcarTab(tabVentas, tabMovimientos, tabFacturas, tabTurnos);
        cargarTabla();
    }

    @FXML private void verTabMovimientos() {
        tabActivo = "movimientos";
        marcarTab(tabMovimientos, tabVentas, tabFacturas, tabTurnos);
        cargarTabla();
    }

    @FXML private void verTabFacturas() {
        tabActivo = "facturas";
        marcarTab(tabFacturas, tabVentas, tabMovimientos, tabTurnos);
        cargarTabla();
    }

    @FXML private void verTabTurnos() {
        tabActivo = "turnos";
        marcarTab(tabTurnos, tabVentas, tabMovimientos, tabFacturas);
        cargarTabla();
    }

    private void marcarTab(Button activo, Button... otros) {
        activo.getStyleClass().remove("active");
        if (!activo.getStyleClass().contains("active"))
            activo.getStyleClass().add("active");
        for (Button b : otros) b.getStyleClass().remove("active");
    }
    private <T> TableCell<T, Double> celdaMoneda() {
        return new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : MXN.format(v));
                setAlignment(Pos.CENTER_RIGHT);
            }
        };
    }

    /* ============================================================
       Exportar
       ============================================================ */
    @FXML private void exportarCSV() { System.out.println("→ CSV"); }
    @FXML private void exportarPDF() { System.out.println("→ PDF"); }
    @FXML private void imprimir()    { System.out.println("→ imprimir"); }
}