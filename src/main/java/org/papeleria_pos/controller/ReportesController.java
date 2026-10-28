package org.papeleria_pos.controller;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
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
import javafx.stage.FileChooser;

import org.papeleria_pos.dao.*;
import org.papeleria_pos.dao.Interface.IMovimientoDAO;
import org.papeleria_pos.dao.Interface.IVentaDAO;
import org.papeleria_pos.dto.*;
import org.papeleria_pos.models.Venta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ReportesController {
    private final IVentaDAO     dao           = new VentaDAOImpl();
    private final IMovimientoDAO movimientoDAO = new MovimientoDAOImpl();
    FacturaDAOImpl facturaDAO =  new FacturaDAOImpl();
    TurnoDAOImpl turnoDAO = new TurnoDAOImpl();

    @FXML private Label lblRango, lblFiltro, lblResumen;
    @FXML private DatePicker dateDesde, dateHasta;

    // 🔑 COMBO DE NEGOCIO
    @FXML private ComboBox<String> cbTipoNegocio, cbCajero, cbMetodo;

    @FXML private Label kpiVentas, kpiTicket, kpiArticulos, kpiMargen;
    @FXML private Label lblDeltaVentas, lblDeltaTicket, lblDeltaArticulos, lblDeltaMargen;

    @FXML private BarChart<String, Number> chartVentas;
    @FXML private VBox topProductos;

    @FXML private Button tabVentas, tabMovimientos, tabFacturas, tabTurnos;
    @SuppressWarnings("rawtypes")
    @FXML private TableView tabla;

    private static final NumberFormat MXN = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"));
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String tabActivo = "ventas";

    @FXML
    public void initialize() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        dateDesde.setValue(LocalDate.now().minusDays(6));
        dateHasta.setValue(LocalDate.now());

        // 🔑 Llenar combo de tipo negocio
        cbTipoNegocio.getItems().addAll("Todos", "PAPELERIA", "SKINCARE");
        cbTipoNegocio.setValue("Todos");

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
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        cbMetodo.getItems().addAll("Todos", "EFECTIVO", "TARJETA", "TRANSFERENCIA");
        cbMetodo.setValue("Todos");

        dateDesde.valueProperty().addListener((o, a, b) -> cargarTodo());
        dateHasta.valueProperty().addListener((o, a, b) -> cargarTodo());
        cbTipoNegocio.valueProperty().addListener((o, a, b) -> cargarTodo());
        cbCajero.valueProperty().addListener((o, a, b) -> cargarTodo());
        cbMetodo.valueProperty().addListener((o, a, b) -> cargarTodo());

        cargarTodo();
    }

    private void cargarTodo() {
        LocalDate[] rango = rangoActual();
        LocalDate desde = rango[0];
        LocalDate hasta = rango[1];

        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("es-MX"));
        String textoRango = desde.format(f) + " – " + hasta.format(f);

        lblRango.setText(textoRango);
        lblFiltro.setText(textoRango);

        String tipoNegocio = cbTipoNegocio.getValue();
        List<Venta> ventasBase = dao.obtenerPorRango(desde, hasta, tipoNegocio);
        List<Venta> ventas = aplicarFiltros(ventasBase);

        cargarKPIs(desde, hasta, ventas, tipoNegocio);
        cargarGrafica(desde, hasta, ventas);
        cargarTop(desde, hasta, ventas, tipoNegocio);
        cargarTabla();
    }

    private List<Venta> aplicarFiltros(List<Venta> base) {
        String cajero = cbCajero != null ? cbCajero.getValue() : null;
        String metodo = cbMetodo != null ? cbMetodo.getValue() : null;

        return base.stream()
                .filter(v -> cajero == null || "Todos".equals(cajero) || cajero.equals(v.getUsuario()))
                .filter(v -> metodo == null || "Todos".equals(metodo) || metodo.equals(v.getMetodoPago()))
                .toList();
    }

    private LocalDate[] rangoActual() {
        LocalDate desde = dateDesde.getValue();
        LocalDate hasta = dateHasta.getValue();
        if (desde == null) desde = LocalDate.now().minusDays(6);
        if (hasta == null) hasta = LocalDate.now();
        return new LocalDate[] { desde, hasta };
    }

    private void cargarKPIs(LocalDate desde, LocalDate hasta, List<Venta> ventas, String tipoNegocio) {
        double total = ventas.stream().filter(v -> "COMPLETADA".equals(v.getEstado())).mapToDouble(Venta::getTotal).sum();
        long tickets = ventas.stream().filter(v -> "COMPLETADA".equals(v.getEstado())).count();

        // 🔑 Artículos reales por tipo de negocio
        int articulos = dao.contarArticulosVendidos(desde, hasta, tipoNegocio);

        double margen = total * 0.4;
        double ticket = tickets > 0 ? total / tickets : 0;

        kpiVentas.setText(MXN.format(total));
        kpiTicket.setText(MXN.format(ticket));
        kpiArticulos.setText(String.valueOf(articulos));
        kpiMargen.setText(MXN.format(margen));

        lblDeltaVentas.setText("—"); lblDeltaTicket.setText("—");
        lblDeltaArticulos.setText("—"); lblDeltaMargen.setText("—");
    }

    private void cargarGrafica(LocalDate desde, LocalDate hasta, List<Venta> ventas) {
        var mapa = new java.util.TreeMap<LocalDate, Double>();
        LocalDate cur = desde;
        while (!cur.isAfter(hasta)) { mapa.put(cur, 0.0); cur = cur.plusDays(1); }

        for (Venta v : ventas) {
            if (!"COMPLETADA".equals(v.getEstado()) || v.getFecha() == null) continue;
            LocalDate d = v.getFecha().toLocalDate();
            mapa.merge(d, v.getTotal(), Double::sum);
        }

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE dd", Locale.forLanguageTag("es-MX"));
        mapa.forEach((fecha, total) -> {
            String label = fecha.format(f);
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
            serie.getData().add(new XYChart.Data<>(label, total));
        });
        chartVentas.getData().setAll(serie);
    }

    private void cargarTop(LocalDate desde, LocalDate hasta, List<Venta> ventas, String tipoNegocio) {
        List<TopProducto> top = dao.obtenerTopProductos(desde, hasta, 5, tipoNegocio);
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
            HBox row = new HBox(); row.getStyleClass().add("top-item-row");
            Label rank = new Label(String.valueOf(i + 1)); rank.getStyleClass().add("top-rank");
            Label nombre = new Label(t.getNombre()); nombre.getStyleClass().add("top-name");
            HBox.setHgrow(nombre, Priority.ALWAYS);
            Label valor = new Label(t.getUnidadesVendidas() + " uds"); valor.getStyleClass().add("top-value");
            row.getChildren().addAll(rank, nombre, valor);

            Pane bar = new Pane(); bar.getStyleClass().add("top-bar"); bar.setMaxWidth(Double.MAX_VALUE);
            Pane fill = new Pane(); fill.getStyleClass().add("top-bar-fill");
            final int maxFinal = max;
            fill.prefWidthProperty().bind(bar.widthProperty().multiply((double) t.getUnidadesVendidas() / maxFinal));
            bar.getChildren().add(fill);

            VBox item = new VBox(row, bar); item.getStyleClass().add("top-item");
            topProductos.getChildren().add(item);
        }
    }

    private void cargarTabla() {
        LocalDate[] rango = rangoActual();
        List<Venta> ventasBase = dao.obtenerPorRango(rango[0], rango[1], cbTipoNegocio.getValue());
        List<Venta> ventas = aplicarFiltros(ventasBase);

        switch (tabActivo) {
            case "ventas":      tablaVentas(ventas);                    break;
            case "movimientos": tablaMovimientos(rango[0], rango[1]);   break;
            case "facturas":    tablaFacturas(rango[0], rango[1]);      break;
            case "turnos":      tablaTurnos(rango[0], rango[1]);        break;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tablaFacturas(LocalDate desde, LocalDate hasta) {
        tabla.getItems().clear(); tabla.getColumns().clear();
        TableColumn<FacturaResumen, String> cFolio = new TableColumn<>("Folio"); cFolio.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSerieFolio()));
        TableColumn<FacturaResumen, String> cFecha = new TableColumn<>("Fecha"); cFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFecha() != null ? c.getValue().getFecha().toString() : ""));
        TableColumn<FacturaResumen, String> cRfc = new TableColumn<>("RFC"); cRfc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRfc()));
        TableColumn<FacturaResumen, String> cRazon = new TableColumn<>("Razón social"); cRazon.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRazonSocial()));
        TableColumn<FacturaResumen, Double> cTotal = new TableColumn<>("Total"); cTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotal())); cTotal.setCellFactory(c -> celdaMoneda());
        TableColumn<FacturaResumen, String> cEstado = new TableColumn<>("Estado"); cEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado()));
        tabla.getColumns().addAll(cFolio, cFecha, cRfc, cRazon, cTotal, cEstado);
        List<FacturaResumen> facturas = facturaDAO.listarPorRango(desde, hasta);
        tabla.setPlaceholder(new Label("Sin facturas en el período."));
        tabla.setItems(FXCollections.observableArrayList((List) facturas));
        double totalFacturado = facturas.stream().filter(f -> "GENERADA".equals(f.getEstado())).mapToDouble(FacturaResumen::getTotal).sum();
        lblResumen.setText(facturas.size() + " facturas · Total facturado: " + MXN.format(totalFacturado));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tablaTurnos(LocalDate desde, LocalDate hasta) {
        tabla.getItems().clear(); tabla.getColumns().clear();
        TableColumn<TurnoResumen, String> cTurno = new TableColumn<>("Turno"); cTurno.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        TableColumn<TurnoResumen, String> cCajero = new TableColumn<>("Cajero"); cCajero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCajero()));
        TableColumn<TurnoResumen, String> cFecha = new TableColumn<>("Apertura"); cFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getApertura() != null ? c.getValue().getApertura().format(FECHA_FMT) : ""));
        TableColumn<TurnoResumen, Double> cInicial = new TableColumn<>("Inicial"); cInicial.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getMontoInicial())); cInicial.setCellFactory(c -> celdaMoneda());
        TableColumn<TurnoResumen, Double> cEsperado = new TableColumn<>("Esperado"); cEsperado.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getMontoEsperado())); cEsperado.setCellFactory(c -> celdaMoneda());
        TableColumn<TurnoResumen, Double> cContado = new TableColumn<>("Contado"); cContado.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getMontoFinal())); cContado.setCellFactory(c -> celdaMoneda());
        TableColumn<TurnoResumen, Double> cDif = new TableColumn<>("Diferencia"); cDif.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDiferencia()));
        cDif.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); setText(null); return; }
                Label lbl = new Label(MXN.format(v));
                if (Math.abs(v) < 0.01) lbl.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: 600;");
                else if (v > 0) lbl.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: 600;");
                else lbl.setStyle("-fx-text-fill: #E53935; -fx-font-weight: 600;");
                setGraphic(lbl); setText(null); setAlignment(Pos.CENTER_RIGHT);
            }
        });
        TableColumn<TurnoResumen, String> cEstado = new TableColumn<>("Estado"); cEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado()));
        tabla.getColumns().addAll(cTurno, cCajero, cFecha, cInicial, cEsperado, cContado, cDif, cEstado);
        List<TurnoResumen> turnos = turnoDAO.listarPorRango(desde, hasta);
        tabla.setPlaceholder(new Label("Sin turnos en el período."));
        tabla.setItems(FXCollections.observableArrayList((List) turnos));
        lblResumen.setText(turnos.size() + " turnos en el período.");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tablaMovimientos(LocalDate desde, LocalDate hasta) {
        tabla.getItems().clear(); tabla.getColumns().clear();
        TableColumn<MovimientoResumen, String> cFecha = new TableColumn<>("Fecha"); cFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFecha() != null ? c.getValue().getFecha().format(FECHA_FMT) : ""));
        TableColumn<MovimientoResumen, String> cProducto = new TableColumn<>("Producto"); cProducto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProducto()));
        TableColumn<MovimientoResumen, String> cTipo = new TableColumn<>("Tipo"); cTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo()));
        TableColumn<MovimientoResumen, Integer> cCantidad = new TableColumn<>("Cantidad"); cCantidad.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCantidad()));
        TableColumn<MovimientoResumen, String> cMotivo = new TableColumn<>("Motivo"); cMotivo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMotivo()));
        tabla.getColumns().addAll(cFecha, cProducto, cTipo, cCantidad, cMotivo);
        List<MovimientoResumen> movimientos = movimientoDAO.listarPorRango(desde, hasta);
        tabla.setPlaceholder(new Label("Sin movimientos en el período."));
        tabla.setItems(FXCollections.observableArrayList((List) movimientos));
        lblResumen.setText(movimientos.size() + " movimientos registrados.");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tablaVentas(List<Venta> ventas) {
        tabla.getItems().clear(); tabla.getColumns().clear();
        TableColumn<Venta, String> cFolio = new TableColumn<>("Folio"); cFolio.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getFolio()));
        TableColumn<Venta, String> cCajero = new TableColumn<>("Cajero"); cCajero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsuario()));
        TableColumn<Venta, String> cFecha = new TableColumn<>("Fecha"); cFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFecha() != null ? c.getValue().getFecha().format(FECHA_FMT) : ""));
        TableColumn<Venta, Double> cTotal = new TableColumn<>("Total"); cTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotal())); cTotal.setCellFactory(c -> celdaMoneda());
        tabla.getColumns().addAll(cFolio, cCajero, cFecha, cTotal);
        tabla.setPlaceholder(new Label("Sin ventas en el período."));
        tabla.setItems(FXCollections.observableArrayList(ventas));
        double totalPeriodo = ventas.stream().filter(v -> "COMPLETADA".equals(v.getEstado())).mapToDouble(Venta::getTotal).sum();
        lblResumen.setText(ventas.size() + " registros · Total: " + MXN.format(totalPeriodo));
    }

    @FXML private void verTabVentas()      { tabActivo = "ventas";      marcarTab(tabVentas, tabMovimientos, tabFacturas, tabTurnos); cargarTabla(); }
    @FXML private void verTabMovimientos() { tabActivo = "movimientos"; marcarTab(tabMovimientos, tabVentas, tabFacturas, tabTurnos); cargarTabla(); }
    @FXML private void verTabFacturas()    { tabActivo = "facturas";    marcarTab(tabFacturas, tabVentas, tabMovimientos, tabTurnos); cargarTabla(); }
    @FXML private void verTabTurnos()      { tabActivo = "turnos";      marcarTab(tabTurnos, tabVentas, tabMovimientos, tabFacturas); cargarTabla(); }

    private void marcarTab(Button activo, Button... otros) {
        activo.getStyleClass().remove("active");
        if (!activo.getStyleClass().contains("active")) activo.getStyleClass().add("active");
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

    @FXML
    private void exportarCSV() {
        if (tabla.getItems().isEmpty()) { alerta("No hay datos para exportar."); return; }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte CSV");
        fileChooser.setInitialFileName("reporte_" + tabActivo + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = fileChooser.showSaveDialog(tabla.getScene().getWindow());
        if (file == null) return;
        new Thread(() -> {
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                StringBuilder header = new StringBuilder();
                for (Object col : tabla.getColumns()) { TableColumn column = (TableColumn) col; header.append(column.getText()).append(","); }
                writer.println(header.toString());
                for (Object item : tabla.getItems()) {
                    StringBuilder row = new StringBuilder();
                    for (Object col : tabla.getColumns()) { TableColumn column = (TableColumn) col; Object val = column.getCellObservableValue(item).getValue(); row.append(val != null ? val.toString() : "").append(","); }
                    writer.println(row.toString());
                }
                Platform.runLater(() -> exito("Reporte CSV exportado correctamente."));
            } catch (Exception e) { e.printStackTrace(); Platform.runLater(() -> alerta("Error al exportar CSV:\n" + e.getMessage())); }
        }).start();
    }

    @FXML
    private void exportarPDF() {
        if (tabla.getItems().isEmpty()) { alerta("No hay datos para exportar."); return; }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte PDF");
        fileChooser.setInitialFileName("reporte_" + tabActivo + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(tabla.getScene().getWindow());
        if (file == null) return;
        new Thread(() -> {
            try {
                Document document = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();
                com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.BOLD, 16);
                document.add(new Paragraph("Reporte de " + tabActivo.toUpperCase() + " - " + LocalDate.now(), titleFont));
                document.add(new Paragraph(" "));
                PdfPTable pdfTable = new PdfPTable(tabla.getColumns().size());
                pdfTable.setWidthPercentage(100);
                com.lowagie.text.Font headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.BOLD, 10);
                for (Object col : tabla.getColumns()) { TableColumn column = (TableColumn) col; PdfPCell cell = new PdfPCell(new Paragraph(column.getText(), headerFont)); cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY); pdfTable.addCell(cell); }
                com.lowagie.text.Font rowFont = new com.lowagie.text.Font(com.lowagie.text.Font.NORMAL, 9);
                for (Object item : tabla.getItems()) {
                    for (Object col : tabla.getColumns()) { TableColumn column = (TableColumn) col; Object val = column.getCellObservableValue(item).getValue(); pdfTable.addCell(new PdfPCell(new Paragraph(val != null ? val.toString() : "", rowFont))); }
                }
                document.add(pdfTable); document.close();
                Platform.runLater(() -> exito("Reporte PDF exportado correctamente."));
            } catch (Exception e) { e.printStackTrace(); Platform.runLater(() -> alerta("Error al exportar PDF:\n" + e.getMessage())); }
        }).start();
    }

    @FXML private void imprimir() { alerta("La función de imprimir se puede agregar después."); }
    private void alerta(String msg) { Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }
    private void exito(String msg) { Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }
}