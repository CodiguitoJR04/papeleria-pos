package org.papeleria_pos.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.papeleria_pos.dao.Interface.IVentaDAO;
import org.papeleria_pos.dao.UsuarioDAOImpl;
import org.papeleria_pos.dao.VentaDAOImpl;
import org.papeleria_pos.dto.TopProducto;
import org.papeleria_pos.dto.UsuarioResumen;
import org.papeleria_pos.models.Venta;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ReportesController {

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
    @FXML private TableView<Venta> tabla;

    private final IVentaDAO dao = new VentaDAOImpl();

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
            case "ventas":      tablaVentas(ventas);      break;
            case "movimientos": tablaVacia("Movimientos: pendiente de implementar"); break;
            case "facturas":    tablaVacia("Facturas: pendiente de implementar");    break;
            case "turnos":      tablaVacia("Turnos: pendiente de implementar");      break;
        }
    }

    /* ============================================================
       Tabla de ventas
       ============================================================ */
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

    /* ============================================================
       Exportar
       ============================================================ */
    @FXML private void exportarCSV() { System.out.println("→ CSV"); }
    @FXML private void exportarPDF() { System.out.println("→ PDF"); }
    @FXML private void imprimir()    { System.out.println("→ imprimir"); }
}