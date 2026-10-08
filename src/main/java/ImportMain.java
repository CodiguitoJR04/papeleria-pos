

import org.papeleria_pos.Services.ImportadorProductos;

public class ImportMain {
    public static void main(String[] args) throws Exception {
        // 🔑 RUTA ABSOLUTA — funciona siempre
        String ruta = "C:\\Users\\reyes\\Desktop\\POS_PAPELERIA\\inventario_productos.csv";

        System.out.println("Importando desde: " + ruta);
        new ImportadorProductos().importarDesdeCSV(ruta);
        System.out.println("✅ Importación terminada.");
    }
}