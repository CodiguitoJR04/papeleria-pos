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
        // 🔑 Usamos la tabla real 'usuarios'
        StringBuilder sql = new StringBuilder(
                "SELECT id_usuario AS id_user, nombre, username, rol, activo, ultimo_acceso, " +
                        "       (SELECT COUNT(*) FROM ventas v " +
                        "         WHERE v.id_cajero = u.id_usuario " +
                        "           AND DATE(v.fecha_hora) = CURDATE()) AS ventas_hoy " +
                        "  FROM usuarios u WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (filtro != null && !filtro.isEmpty()) {
            sql.append(" AND (LOWER(nombre) LIKE ? OR LOWER(username) LIKE ?) ");
            String f = "%" + filtro.toLowerCase() + "%";
            params.add(f);
            params.add(f);
        }
        if (rol != null && !rol.isEmpty()) {
            sql.append(" AND rol = ? ");
            params.add(rol.toUpperCase()); // En la BD dice ADMINISTRADOR o CAJERO
        }
        if (activo != null) {
            sql.append(" AND activo = ? ");
            params.add(activo ? 1 : 0);
        }
        sql.append(" ORDER BY nombre");

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
        // 🔑 INSERT en la tabla 'usuarios' con la columna 'password_hash'
        String sql = "INSERT INTO usuarios (nombre, username, password_hash, rol, activo) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, rol.toUpperCase()); // 'ADMINISTRADOR' o 'CAJERO'
            ps.setInt(5, activo ? 1 : 0);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(int id, String nombre, String username, String password,
                              String rol, boolean activo) {
        String sql = "UPDATE usuarios SET nombre=?, username=?, password_hash=?, rol=?, activo=? " +
                "WHERE id_usuario=?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, rol.toUpperCase());
            ps.setInt(5, activo ? 1 : 0);
            ps.setInt(6, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean eliminar(int id) {
        String sql = "DELETE FROM usuarios WHERE id_usuario=?";
        try (PreparedStatement ps = DatabaseConnection.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public int contarActivos() {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE activo = 1";
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

                boolean ok;
                if (hash != null && hash.startsWith("$2a$")) {
                    ok = org.mindrot.jbcrypt.BCrypt.checkpw(password, hash);
                } else {
                    ok = password.equals(hash);
                }

                if (!ok) return null;

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