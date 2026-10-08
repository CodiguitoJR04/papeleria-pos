package org.papeleria_pos.Services;

import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.dao.ProductoDAOImpl;
import org.papeleria_pos.dao.ProveedorDAOImpl;
import org.papeleria_pos.hardware.BarcodeGenerator;
import org.papeleria_pos.models.Producto;
import org.papeleria_pos.models.Proveedor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class ImportadorProductos {

    private final ProveedorDAOImpl provDAO = new ProveedorDAOImpl();
    private final ProductoDAOImpl  prodDAO = new ProductoDAOImpl();

    public int importarDesdeCSV(String ruta) throws Exception {
        int importados = 0, omitidos = 0, errores = 0;

        try (BufferedReader br = new BufferedReader(
                new FileReader(ruta, StandardCharsets.UTF_8))) {

            String linea = br.readLine();   // saltar encabezado
            while ((linea = br.readLine()) != null) {
                if (linea.isBlank()) continue;

                try {
                    String[] c = linea.split(",", -1);
                    if (c.length < 5) { errores++; continue; }

                    String proveedor = c[0].trim();
                    String nombre    = c[1].trim();
                    int    stock     = Integer.parseInt(c[2].trim());
                    double costo     = Double.parseDouble(c[3].trim());
                    double venta     = Double.parseDouble(c[4].trim());

                    // 1) Resolver/crear proveedor
                    int idProveedor = resolverProveedor(proveedor);

                    // 2) ¿Ya existe el producto?
                    if (existeProducto(nombre)) {
                        System.out.println("⏭ Ya existe: " + nombre);
                        omitidos++;
                        continue;
                    }

                    // 3) Generar código de barras único
                    String codigo = BarcodeGenerator.generar(siguienteSecuencial());

                    // 4) Crear el producto
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
                        importados++;
                        System.out.printf("✔ [%s] %s → %s%n", proveedor, nombre, codigo);
                    } else {
                        errores++;
                    }

                } catch (Exception e) {
                    System.err.println("⚠ Error en línea: " + linea + " → " + e.getMessage());
                    errores++;
                }
            }
        }

        System.out.printf("Importados: %d · Omitidos: %d · Errores: %d%n",
                importados, omitidos, errores);
        return importados;
    }

    /* ============================================================
       Helpers
       ============================================================ */
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
                "INSERT INTO categorias (nombre, descripcion, activa) " +
                        "VALUES ('Sin categoría', 'Importación masiva', 1)",
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