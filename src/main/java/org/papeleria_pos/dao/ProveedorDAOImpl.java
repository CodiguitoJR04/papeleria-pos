package org.papeleria_pos.dao;

import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.dao.Interface.IProvedorDAO;
import org.papeleria_pos.models.Proveedor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAOImpl implements IProvedorDAO {

    @Override
    public List<Proveedor> listarTodos() {
        String sql = "SELECT id_proveedor, nombre, contacto, telefono, email, activo " +
                "  FROM proveedores WHERE activo = 1 ORDER BY nombre";
        return ejecutar(sql, null);
    }

    @Override
    public List<Proveedor> buscar(String filtro) {
        String sql = "SELECT id_proveedor, nombre, contacto, telefono, email, activo " +
                "  FROM proveedores WHERE activo = 1 AND LOWER(nombre) LIKE ? ORDER BY nombre";
        return ejecutar(sql, filtro == null ? null : "%" + filtro.toLowerCase() + "%");
    }

    @Override
    public Proveedor buscarPorId(int id) {
        String sql = "SELECT id_proveedor, nombre, contacto, telefono, email, activo " +
                "  FROM proveedores WHERE id_proveedor = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando proveedor: " + e.getMessage(), e);
        }
    }

    @Override
    public int insertar(Proveedor p) {
        String sql = "INSERT INTO proveedores (nombre, contacto, telefono, email) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.get()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getContacto());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getEmail());
            if (ps.executeUpdate() == 0) return -1;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando proveedor: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(Proveedor p) {
        String sql = "UPDATE proveedores SET nombre=?, contacto=?, telefono=?, email=? " +
                "WHERE id_proveedor=?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getContacto());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getEmail());
            ps.setInt(5, p.getIdProveedor());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando proveedor: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(int id) {
        // Soft delete para no romper la FK con productos
        String sql = "UPDATE proveedores SET activo = 0 WHERE id_proveedor = ?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando proveedor: " + e.getMessage(), e);
        }
    }

    @Override
    public Proveedor buscarPorNombre(String nombre) {
        String sql = "SELECT id_proveedor, nombre, contacto, telefono, email, activo " +
                "  FROM proveedores WHERE nombre = ? LIMIT 1";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando proveedor: " + e.getMessage(), e);
        }
    }

    private List<Proveedor> ejecutar(String sql, String param) {
        List<Proveedor> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            if (param != null) ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando proveedores: " + e.getMessage(), e);
        }
        return lista;
    }

    private Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setIdProveedor(rs.getInt("id_proveedor"));
        p.setNombre(rs.getString("nombre"));
        p.setContacto(rs.getString("contacto"));
        p.setTelefono(rs.getString("telefono"));
        p.setEmail(rs.getString("email"));
        p.setActivo(rs.getInt("activo") == 1);
        return p;
    }
}