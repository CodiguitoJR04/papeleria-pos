package org.papeleria_pos.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
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

    private DatabaseConnection() {}

    /**
     * Devuelve una NUEVA conexión a la base de datos.
     * Es responsabilidad del DAO cerrarla (conn.close()).
     */
    public static Connection get() throws SQLException {
        try {
            // Carga del driver (solo se ejecuta una vez en memoria, no afecta el rendimiento)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL no encontrado.", e);
        }

        // 🔑 MAGIA: Retornamos una conexión NUEVA e independiente cada vez
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /*
     * Ya no necesitas el método cerrar() aquí porque cada DAO
     * cerrará su propia conexión.
     */
}