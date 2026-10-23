package org.papeleria_pos.Services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.dao.ProductoDAOImpl;
import org.papeleria_pos.dao.ProveedorDAOImpl;
import org.papeleria_pos.hardware.BarcodeGenerator;
import org.papeleria_pos.models.Producto;
import org.papeleria_pos.models.Proveedor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class ImportadorProductos {

    private final ProveedorDAOImpl provDAO = new ProveedorDAOImpl();
    private final ProductoDAOImpl  prodDAO = new ProductoDAOImpl();

    public int importarDesdeArchivo(String ruta) throws Exception {
        if (ruta.toLowerCase().endsWith(".csv")) {
            return importarCSV(ruta);
        } else if (ruta.toLowerCase().endsWith(".xlsx") || ruta.toLowerCase().endsWith(".xls")) {
            return importarExcel(ruta);
        } else {
            throw new Exception("Formato no soportado. Usa .csv o .xlsx");
        }
    }

    /* ============================================================
       IMPORTAR DESDE CSV (Ahora espera 6 columnas)
       ============================================================ */
    private int importarCSV(String ruta) throws Exception {
        int importados = 0;
        try (BufferedReader br = new BufferedReader(
                new FileReader(ruta, StandardCharsets.UTF_8))) {

            String linea = br.readLine();   // saltar encabezado
            while ((linea = br.readLine()) != null) {
                if (linea.isBlank()) continue;
                String[] c = linea.split(",", -1);

                // Esperamos: proveedor, sku, producto, stock, costo, venta
                if (c.length >= 6) {
                    if (procesarFila(c[0].trim(), c[1].trim(), c[2].trim(), c[3].trim(), c[4].trim(), c[5].trim())) {
                        importados++;
                    }
                }
            }
        }
        return importados;
    }

    /* ============================================================
       IMPORTAR DESDE EXCEL (Ahora lee 6 columnas)
       ============================================================ */
    private int importarExcel(String ruta) throws Exception {
        int importados = 0;
        try (FileInputStream fis = new FileInputStream(ruta);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // 0: Proveedor | 1: SKU | 2: Producto | 3: Stock | 4: Costo | 5: Venta
                String prov   = formatter.formatCellValue(row.getCell(0)).trim();
                String sku    = formatter.formatCellValue(row.getCell(1)).trim();
                String nombre = formatter.formatCellValue(row.getCell(2)).trim();
                String stock  = formatter.formatCellValue(row.getCell(3)).trim();
                String costo  = formatter.formatCellValue(row.getCell(4)).trim();
                String venta  = formatter.formatCellValue(row.getCell(5)).trim();

                if (!prov.isEmpty() && !nombre.isEmpty()) {
                    if (procesarFila(prov, sku, nombre, stock, costo, venta)) {
                        importados++;
                    }
                }
            }
        }
        return importados;
    }

    /* ============================================================
       PROCESAR FILA (Lógica compartida)
       ============================================================ */
    private boolean procesarFila(String proveedor, String skuStr, String nombre, String stockStr, String costoStr, String ventaStr) {
        try {
            int    stock = (int) Double.parseDouble(stockStr);
            double costo = Double.parseDouble(costoStr);
            double venta = Double.parseDouble(ventaStr);

            int idProveedor = resolverProveedor(proveedor);

            if (existeProducto(nombre)) {
                System.out.println("⏭ Ya existe: " + nombre);
                return false;
            }

            // Generación automática si el SKU viene vacío
            String codigo = skuStr;
            if (codigo == null || codigo.trim().isEmpty()) {
                codigo = BarcodeGenerator.generar(siguienteSecuencial());
            }

            Producto p = new Producto();
            p.setIdProducto(0);
            p.setSku(codigo);
            p.setNombre(nombre);
            p.setIdCategoria(idCategoriaPorDefecto());
            p.setIdProveedor(idProveedor);
            p.setPrecioCompra(costo);
            p.setPrecioVenta(venta);
            p.setStockActual(stock);
            p.setStockMinimo(5);

            int id = prodDAO.insertar(p);
            if (id > 0) {
                System.out.printf("✔ [%s] %s → %s%n", proveedor, nombre, codigo);
                return true;
            }
        } catch (Exception e) {
            System.err.println("⚠ Error procesando: " + nombre + " -> " + e.getMessage());
        }
        return false;
    }

    private int resolverProveedor(String nombre) {
        Proveedor p = provDAO.buscarPorNombre(nombre);
        if (p != null) return p.getIdProveedor();

        Proveedor nuevo = new Proveedor();
        nuevo.setNombre(nombre);
        nuevo.setActivo(true);
        int id = provDAO.insertar(nuevo);
        System.out.println("➕ Nuevo proveedor: " + nombre + " (ID " + id + ")");
        return id;
    }

    private boolean existeProducto(String nombre) {
        String sql = "SELECT 1 FROM productos WHERE LOWER(nombre) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private long siguienteSecuencial() {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo_barras, 4, 9) AS UNSIGNED)), 0) " +
                "  FROM productos WHERE codigo_barras LIKE '200%'";
        try (Statement st = DatabaseConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) + 1 : 1;
        } catch (Exception e) {
            return System.currentTimeMillis() % 1_000_000_000L;
        }
    }

    private int idCategoriaPorDefecto() {
        String sql = "SELECT id_categoria FROM categorias WHERE nombre = 'Sin categoría' LIMIT 1";
        try (Statement st = DatabaseConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}

        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(
                "INSERT INTO categorias (nombre, descripcion, activo) VALUES ('Sin categoría', 'Importación masiva', 1)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        } catch (Exception e) {
            return 1;
        }
    }
}