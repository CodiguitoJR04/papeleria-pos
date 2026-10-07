package org.papeleria_pos.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.papeleria_pos.dao.Interface.IProductoDAO;
import org.papeleria_pos.dao.Interface.ITurnoDAO;
import org.papeleria_pos.dao.Interface.IVentaDAO;
import org.papeleria_pos.dao.ProductoDAOImpl;
import org.papeleria_pos.dao.TurnoDAOImpl;
import org.papeleria_pos.dao.VentaDAOImpl;
import org.papeleria_pos.dto.TopProducto;
import org.papeleria_pos.models.Producto;
import org.papeleria_pos.models.Venta;

import javafx.scene.control.Label;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class DashboardController {
    /* ============================================================
       DAOs (inyectados, con fallback a las implementaciones JDBC)
       ============================================================ */
    private IVentaDAO ventaDAO    = new VentaDAOImpl();
    private IProductoDAO productoDAO = new ProductoDAOImpl();
    private ITurnoDAO turnoDAO    = new TurnoDAOImpl();

    public void setVentaDAO(IVentaDAO ventaDAO)          { this.ventaDAO = ventaDAO; }
    public void setProductoDAO(IProductoDAO productoDAO) { this.productoDAO = productoDAO; }
    public void setTurnoDAO(ITurnoDAO turnoDAO)          { this.turnoDAO = turnoDAO; }

    /* ============================================================
       Referencias al FXML
       ============================================================ */
    @FXML
    private Label lblFecha;
    @FXML private Label  kpiVentas, kpiTicket, kpiStockBajo, kpiTurnos;
    @FXML private Label  lblDeltaVentas, lblDeltaTicket;
    @FXML private BarChart<String, Number> chartVentas;
    @FXML private VBox topProductos, ultimasVentas, alertasStock;

    private static final NumberFormat MXN =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"));

    /* ============================================================
       Init
       ============================================================ */
    @FXML
    public void initialize() {
        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern(
                "d 'de' MMMM, yyyy", Locale.forLanguageTag("es-MX"));
        lblFecha.setText("Resumen operativo · " + LocalDate.now().format(fechaFmt));

        mostrarCargando();

        cargarResumen();
        cargarGrafica();
        cargarTopProductos();
        cargarUltimasVentas();
        cargarAlertasStock();
    }

    /* ============================================================
       KPIs — usando VentaDAO y TurnoDAO
       ============================================================ */
    private void cargarResumen() {
        CompletableFuture.supplyAsync(() -> {
            LocalDate hoy   = LocalDate.now();
            LocalDate ayer  = hoy.minusDays(1);

            // ⚠️ AJUSTA los nombres de los métodos según tu VentaDAO
            List<Venta> ventasHoy   = ventaDAO.obtenerPorFecha(hoy);
            List<Venta> ventasAyer  = ventaDAO.obtenerPorFecha(ayer);

            double totalHoy  = sumar(ventasHoy);
            double totalAyer = sumar(ventasAyer);

            int ticketsHoy  = ventasHoy.size();
            int ticketsAyer = ventasAyer.size();

            double ticketHoy  = ticketsHoy  > 0 ? totalHoy  / ticketsHoy  : 0;
            double ticketAyer = ticketsAyer > 0 ? totalAyer / ticketsAyer : 0;

            int stockBajo    = productoDAO.contarStockBajo();
            int turnosAbiert = turnoDAO.contarAbiertos();

            return new Object[] { totalHoy, ticketHoy, stockBajo, turnosAbiert,
                    totalAyer, ticketAyer };
        }).thenAccept(r -> Platform.runLater(() -> {
            double totalHoy    = (double) r[0];
            double ticketHoy   = (double) r[1];
            int    stockBajo   = (int)    r[2];
            int    turnosAbiert= (int)    r[3];
            double totalAyer   = (double) r[4];
            double ticketAyer  = (double) r[5];

            kpiVentas.setText(MXN.format(totalHoy));
            kpiTicket.setText(MXN.format(ticketHoy));
            kpiStockBajo.setText(String.valueOf(stockBajo));
            kpiTurnos.setText(String.valueOf(turnosAbiert));

            actualizarDelta(lblDeltaVentas, calcularDelta(totalHoy, totalAyer));
            actualizarDelta(lblDeltaTicket, calcularDelta(ticketHoy, ticketAyer));
        })).exceptionally(this::manejarError);
    }

    private double sumar(List<Venta> ventas) {
        return ventas.stream()
                .filter(v -> "COMPLETADA".equals(v.getEstado()))
                .mapToDouble(Venta::getTotal)
                .sum();
    }

    private double calcularDelta(double actual, double anterior) {
        if (anterior == 0) return actual > 0 ? 100 : 0;
        return ((actual - anterior) / anterior) * 100.0;
    }

    private void actualizarDelta(Label label, double pct) {
        if (label == null) return;
        String flecha = pct >= 0 ? "▲" : "▼";
        label.setText(String.format("%s %.1f%% vs. ayer", flecha, Math.abs(pct)));
        label.getStyleClass().removeAll("kpi-delta-up", "kpi-delta-down");
        label.getStyleClass().add(pct >= 0 ? "kpi-delta-up" : "kpi-delta-down");
    }

    /* ============================================================
       Gráfica — VentaDAO.obtenerPorRango
       ============================================================ */
    private void cargarGrafica() {
        CompletableFuture.supplyAsync(() -> {
                    LocalDate hasta = LocalDate.now();
                    LocalDate desde = hasta.minusDays(6);
                    return ventaDAO.obtenerPorRango(desde, hasta);
                }).thenAccept(ventas -> Platform.runLater(() -> pintarGrafica(ventas)))
                .exceptionally(this::manejarError);
    }

    private void pintarGrafica(List<Venta> ventas) {
        // Agrupa por fecha
        var totalesPorDia = new java.util.TreeMap<LocalDate, Double>();
        LocalDate hoy   = LocalDate.now();
        LocalDate desde = hoy.minusDays(6);

        // Inicializa los 7 días en 0
        for (int i = 0; i < 7; i++) totalesPorDia.put(desde.plusDays(i), 0.0);

        for (Venta v : ventas) {
            if (!"COMPLETADA".equals(v.getEstado())) continue;
            LocalDate d = v.getFecha().toLocalDate();
            totalesPorDia.merge(d, v.getTotal(), Double::sum);
        }

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        DateTimeFormatter etiqueta = DateTimeFormatter.ofPattern(
                "EEE", Locale.forLanguageTag("es-MX"));

        totalesPorDia.forEach((fecha, total) -> {
            String label = capitalizar(fecha.format(etiqueta));
            serie.getData().add(new XYChart.Data<>(label, total));
        });

        chartVentas.getData().setAll(serie);
    }

    private String capitalizar(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /* ============================================================
       Top productos — VentaDAO.obtenerTopProductos
       ============================================================ */
    private void cargarTopProductos() {
        CompletableFuture.supplyAsync(() -> {
                    LocalDate hasta = LocalDate.now();
                    LocalDate desde = hasta.minusDays(6);
                    return ventaDAO.obtenerTopProductos(desde, hasta, 5);
                }).thenAccept(top -> Platform.runLater(() -> pintarTop(top)))
                .exceptionally(this::manejarError);
    }

    /**
     * Se espera que obtenerTopProductos devuelva algo como
     * List<Object[]> con [idProducto, nombre, unidades, importe]
     * o un modelo específico. Ajusta según tu DAO.
     */
    private void pintarTop(List<TopProducto> top) {
        topProductos.getChildren().clear();
        if (top == null || top.isEmpty()) {
            topProductos.getChildren().add(vacio("Sin ventas en los últimos 7 días."));
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
            fill.prefWidthProperty().bind(
                    bar.widthProperty().multiply((double) t.getUnidadesVendidas() / maxFinal));
            bar.getChildren().add(fill);

            VBox item = new VBox(row, bar);
            item.getStyleClass().add("top-item");
            topProductos.getChildren().add(item);
        }
    }

    /* ============================================================
       Últimas ventas — VentaDAO.obtenerUltimas
       ============================================================ */
    private void cargarUltimasVentas() {
        CompletableFuture.supplyAsync(() -> ventaDAO.obtenerUltimas(5))
                .thenAccept(v -> Platform.runLater(() -> pintarUltimas(v)))
                .exceptionally(this::manejarError);
    }

    private void pintarUltimas(List<Venta> ventas) {
        ultimasVentas.getChildren().clear();
        if (ventas.isEmpty()) {
            ultimasVentas.getChildren().add(vacio("Sin ventas registradas."));
            return;
        }
        DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (Venta v : ventas) {
            HBox row = new HBox();
            row.getStyleClass().add("simple-list-item");

            Label izq = new Label("#" + v.getFolio() + " · " + v.getUsuario());
            izq.getStyleClass().add("item-name");
            HBox.setHgrow(izq, Priority.ALWAYS);

            Label meta = new Label(v.getFecha().format(horaFmt));
            meta.getStyleClass().add("item-meta");

            Label val = new Label(MXN.format(v.getTotal()));
            val.getStyleClass().add("item-value");

            row.getChildren().addAll(izq, meta, val);
            ultimasVentas.getChildren().add(row);
        }
    }

    /* ============================================================
       Alertas de stock — ProductoDAO
       ============================================================ */
    private void cargarAlertasStock() {
        CompletableFuture.supplyAsync(() -> productoDAO.obtenerStockBajo(5))
                .thenAccept(p -> Platform.runLater(() -> pintarAlertas(p)))
                .exceptionally(this::manejarError);
    }

    private void pintarAlertas(List<Producto> productos) {
        alertasStock.getChildren().clear();
        if (productos.isEmpty()) {
            alertasStock.getChildren().add(vacio("Todo el stock está en orden."));
            return;
        }
        for (Producto p : productos) {
            HBox row = new HBox();
            row.getStyleClass().add("simple-list-item");

            Label nombre = new Label(p.getNombre());
            nombre.getStyleClass().add("item-name");
            HBox.setHgrow(nombre, Priority.ALWAYS);

            String nivel = p.getStockActual() == 0 ? "out"
                    : p.getStockActual() <= p.getStockMinimo() ? "low" : "ok";

            Label badge = new Label(p.getStockActual() + " uds");
            badge.getStyleClass().addAll("badge", nivel);

            row.getChildren().addAll(nombre, badge);
            alertasStock.getChildren().add(row);
        }
    }

    /* ============================================================
       Helpers
       ============================================================ */
    private Label vacio(String msg) {
        Label l = new Label(msg);
        l.getStyleClass().add("content-subtitle");
        l.setStyle("-fx-padding: 20 0 20 0;");
        return l;
    }

    private void mostrarCargando() {
        kpiVentas.setText("…");
        kpiTicket.setText("…");
        kpiStockBajo.setText("…");
        kpiTurnos.setText("…");
    }

    private Void manejarError(Throwable t) {
        t.printStackTrace();
        Platform.runLater(() -> {
            kpiVentas.setText("error");
            kpiTicket.setText("error");
            kpiStockBajo.setText("—");
            kpiTurnos.setText("—");
        });
        return null;
    }

    /* ============================================================
       Navegación
       ============================================================ */
    @FXML private void irAReportes()   { System.out.println("→ reportes");   }
    @FXML private void irAInventario() { System.out.println("→ inventario"); }
}