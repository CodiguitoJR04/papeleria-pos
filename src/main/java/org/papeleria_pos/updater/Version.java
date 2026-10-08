package org.papeleria_pos.updater;

/** Representa una versión semántica: 1.0.2 */
public class Version implements Comparable<Version> {

    private final int[] partes;

    public Version(String s) {
        String limpio = s.trim().replaceAll("^v", "");   // quita "v" inicial
        String[] trozos = limpio.split("\\.");
        this.partes = new int[trozos.length];
        for (int i = 0; i < trozos.length; i++) {
            try { partes[i] = Integer.parseInt(trozos[i].replaceAll("[^0-9]", "")); }
            catch (Exception e) { partes[i] = 0; }
        }
    }

    @Override
    public int compareTo(Version otra) {
        int len = Math.max(partes.length, otra.partes.length);
        for (int i = 0; i < len; i++) {
            int a = i < partes.length ? partes[i] : 0;
            int b = i < otra.partes.length ? otra.partes[i] : 0;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    public boolean esMayorQue(Version otra) { return compareTo(otra) > 0; }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            if (i > 0) sb.append(".");
            sb.append(partes[i]);
        }
        return sb.toString();
    }
}