package org.papeleria_pos.dao;

import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.dao.Interface.ICategoriaDAO;
import org.papeleria_pos.dto.CategoriaResumen;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAOImpl implements ICategoriaDAO {

    @Override
    public List<CategoriaResumen> listarTodas() {
        String sql =
                "SELECT c.id_categoria, c.nombre, c.descripcion, c.activo, " +
                        "       (SELECT COUNT(*) FROM productos p " +
                        "         WHERE p.id_categoria = c.id_categoria) AS num_productos " +
                        "  FROM categorias c ORDER BY c.nombre";
        return ejecutar(sql, null);
    }

    @Override
    public List<CategoriaResumen> buscar(String filtro) {
        String sql =
                "SELECT c.id_categoria, c.nombre, c.descripcion, c.activo, " +
                        "       (SELECT COUNT(*) FROM productos p " +
                        "         WHERE p.id_categoria = c.id_categoria) AS num_productos " +
                        "  FROM categorias c WHERE LOWER(c.nombre) LIKE ? ORDER BY c.nombre";
        return ejecutar(sql, filtro == null ? null : "%" + filtro.toLowerCase() + "%");
    }

    private List<CategoriaResumen> ejecutar(String sql, String filtro) {
        List<CategoriaResumen> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            if (filtro != null) ps.setString(1, filtro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new CategoriaResumen(
                            rs.getInt("id_categoria"),
                            rs.getString("nombre"),
                            rs.getString("descripcion"),
                            rs.getInt("num_productos"),
                            rs.getInt("activo") == 1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando categorías: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public boolean insertar(String nombre, String descripcion, boolean activa) {
        String sql = "INSERT INTO categorias (nombre, descripcion, activo) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, descripcion);
            ps.setInt(3, activa ? 1 : 0);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando categoría: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(int id, String nombre, String descripcion, boolean activa) {
        String sql = "UPDATE categorias SET nombre=?, descripcion=?, activo=? WHERE id_categoria=?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, descripcion);
            ps.setInt(3, activa ? 1 : 0);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando categoría: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(int id) {
        String sql = "DELETE FROM categorias WHERE id_categoria=?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando categoría: " + e.getMessage(), e);
        }
    }

    @Override
    public int contarActivas() {
        String sql = "SELECT COUNT(*) FROM categorias WHERE activo = 1";
        try (Statement st = DatabaseConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}