package org.papeleria_pos.hardware;

public class BarcodeGenerator {

    private static final String PREFIJO = "200";   // GS1 interno México

    /** Genera un EAN-13 válido a partir de un número secuencial. */
    public static String generar(long secuencial) {
        String base = PREFIJO + String.format("%09d", secuencial);
        return base + calcularDigitoVerificador(base);
    }

    /** Dígito verificador EAN-13. */
    private static int calcularDigitoVerificador(String base12) {
        int suma = 0;
        for (int i = 0; i < 12; i++) {
            int d = Character.getNumericValue(base12.charAt(i));
            suma += (i % 2 == 0) ? d : d * 3;
        }
        int resto = suma % 10;
        return resto == 0 ? 0 : 10 - resto;
    }

    public static boolean esValido(String ean13) {
        if (ean13 == null || !ean13.matches("\\d{13}")) return false;
        String base = ean13.substring(0, 12);
        int esperado = Character.getNumericValue(ean13.charAt(12));
        return calcularDigitoVerificador(base) == esperado;
    }
}