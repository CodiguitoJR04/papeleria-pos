module org.papeleria_pos {
    requires java.sql;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.controls;
    requires org.jetbrains.annotations;
    requires jbcrypt;
    requires java.desktop;
    //requires com.mysql.cj; // Habilitado para comunicación con base de datos MySQL

    // Apertura de paquetes a JavaFX para lectura de controladores, FXML y estilos
    opens org.papeleria_pos to javafx.fxml, javafx.graphics;
    opens org.papeleria_pos.controller to javafx.fxml;
    opens org.papeleria_pos.config to javafx.fxml;
    opens org.papeleria_pos.models to javafx.fxml, javafx.base;
    opens org.papeleria_pos.dto to javafx.fxml, javafx.base;

    exports org.papeleria_pos;
}