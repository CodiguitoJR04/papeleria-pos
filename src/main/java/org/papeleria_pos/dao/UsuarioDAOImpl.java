package org.papeleria_pos.dao;

import org.papeleria_pos.config.DatabaseConnection;
import org.papeleria_pos.dao.Interface.IUsuarioDAO;
import org.papeleria_pos.dto.UsuarioResumen;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAOImpl implements IUsuarioDAO {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<UsuarioResumen> listarTodos() {
        return buscar(null, null, null);
    }

    @Override
    public List<UsuarioResumen> buscar(String filtro, String rol, Boolean activo) {
        StringBuilder sql = new StringBuilder(
                "SELECT u.id_user, u.nombre, u.username, u.rol, u.activo, u.ultimo_acceso, " +
                        "       (SELECT COUNT(*) FROM venta v " +
                        "         WHERE v.usuario = u.username " +
                        "           AND DATE(v.fecha) = CURDATE()) AS ventas_hoy " +   // 🔑
                        "  FROM usuario u WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (filtro != null && !filtro.isEmpty()) {
            sql.append(" AND (LOWER(u.nombre) LIKE ? OR LOWER(u.username) LIKE ?) ");
            String f = "%" + filtro.toLowerCase() + "%";
            params.add(f);
            params.add(f);
        }
        if (rol != null && !rol.isEmpty()) {
            sql.append(" AND u.rol = ? ");
            params.add(rol);
        }
        if (activo != null) {
            sql.append(" AND u.activo = ? ");
            params.add(activo ? 1 : 0);
        }
        sql.append(" ORDER BY u.nombre");

        List<UsuarioResumen> lista = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fechaStr = rs.getString("ultimo_acceso");
                    LocalDateTime ultimo = fechaStr != null
                            ? LocalDateTime.parse(fechaStr, FMT) : null;

                    lista.add(new UsuarioResumen(
                            rs.getInt("id_user"),
                            rs.getString("nombre"),
                            rs.getString("username"),
                            rs.getString("rol"),
                            rs.getInt("activo") == 1,
                            ultimo,
                            rs.getInt("ventas_hoy")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando usuarios: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public boolean insertar(String nombre, String username, String password,
                            String rol, boolean activo) {
        String sql = "INSERT INTO usuario (nombre, username, password, rol, activo) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, rol);
            ps.setInt(5, activo ? 1 : 0);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(int id, String nombre, String username, String password,
                              String rol, boolean activo) {
        String sql = "UPDATE usuario SET nombre=?, username=?, password=?, rol=?, activo=? " +
                "WHERE id_user=?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, rol);
            ps.setInt(5, activo ? 1 : 0);
            ps.setInt(6, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(int id) {
        String sql = "DELETE FROM usuario WHERE id_user=?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public int contarActivos() {
        String sql = "SELECT COUNT(*) FROM usuario WHERE activo = 1";
        try (Statement st = DatabaseConnection.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String autenticar(String username, String password) {
        String sql = "SELECT rol, password_hash FROM usuarios " +
                " WHERE username = ? AND activo = 1";

        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String rol  = rs.getString("rol");
                String hash = rs.getString("password_hash");

                // 🔑 Verificar según el formato del hash
                boolean ok;
                if (hash != null && hash.startsWith("$2a$")) {
                    // Es un hash BCrypt → verificar con BCrypt
                    ok = org.mindrot.jbcrypt.BCrypt.checkpw(password, hash);
                } else {
                    // Es texto plano (modo dev) → comparar directo
                    ok = password.equals(hash);
                }

                if (!ok) return null;

                // Actualizar último acceso
                String upd = "UPDATE usuarios SET ultimo_acceso = NOW() WHERE username = ?";
                try (PreparedStatement ps2 = DatabaseConnection.get().prepareStatement(upd)) {
                    ps2.setString(1, username);
                    ps2.executeUpdate();
                }

                return rol;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error autenticando: " + e.getMessage(), e);
        }
    }
}