package org.papeleria_pos.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    /* ============================================================
       Configuración MySQL (local)
       ============================================================ */
    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";              // ⚠️ verifica tu puerto
    private static final String DATABASE = "papeleria_pos";
    private static final String USER     = "root";
    private static final String PASSWORD = "1234";

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE +
                    "?useSSL=false" +
                    "&allowPublicKeyRetrieval=true" +
                    "&serverTimezone=America/Mexico_City" +
                    "&useUnicode=true" +
                    "&characterEncoding=UTF-8" +
                    "&zeroDateTimeBehavior=convertToNull";

    private static Connection conexion;

    /* Constructor privado — clase utilitaria, no se instancia */
    private DatabaseConnection() {}

    /**
     * Devuelve la conexión activa. La crea si no existe o si se cerró.
     * Thread-safe gracias a {@code synchronized}.
     */
    public static synchronized Connection get() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            try {
                // Carga explícita del driver (evita "No suitable driver" en algunos IDEs)
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException(
                        "Driver MySQL no encontrado. Verifica la dependencia mysql-connector-java en pom.xml", e);
            }
            conexion = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        System.out.println("Estado de la conexion"+conexion);
        return conexion;
    }

    /** Cierra la conexión si está abierta. */
    public static synchronized void cerrar() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
            }
        } catch (SQLException ignored) {
            // silencioso a propósito
        }
        conexion = null;
    }
}