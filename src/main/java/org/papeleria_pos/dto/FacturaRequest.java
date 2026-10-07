package org.papeleria_pos.dto;

public class FacturaRequest {
    private final int idVenta;
    private final String rfcReceptor;
    private final String razonSocial;
    private final String regimenFiscal;   // clave SAT: "601", "612", "616"
    private final String usoCfdi;         // clave SAT: "G01", "G03", "P01"
    private final String metodoPago;      // "PUE" | "PPD"

    public FacturaRequest(int idVenta, String rfcReceptor, String razonSocial,
                          String regimenFiscal, String usoCfdi, String metodoPago) {
        this.idVenta = idVenta;
        this.rfcReceptor = rfcReceptor;
        this.razonSocial = razonSocial;
        this.regimenFiscal = regimenFiscal;
        this.usoCfdi = usoCfdi;
        this.metodoPago = metodoPago;
    }

    public int getIdVenta()             { return idVenta; }
    public String getRfcReceptor()      { return rfcReceptor; }
    public String getRazonSocial()      { return razonSocial; }
    public String getRegimenFiscal()    { return regimenFiscal; }
    public String getUsoCfdi()          { return usoCfdi; }
    public String getMetodoPago()       { return metodoPago; }
}