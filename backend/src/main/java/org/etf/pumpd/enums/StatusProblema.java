package org.etf.pumpd.enums;

public enum StatusProblema {
    UNPROCESSED("neobradjen"),
    IN_PROGRESS("u_obradi"),
    PROCESSED("obradjen");

    private final String dbVrijednost;

    StatusProblema(String dbVrijednost) {
        this.dbVrijednost = dbVrijednost;
    }

    public String getDbVrijednost() {
        return dbVrijednost;
    }

    public static StatusProblema izBaze(String dbVrijednost) {
        for (StatusProblema status : values()) {
            if (status.dbVrijednost.equals(dbVrijednost)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Nepoznat status problema u bazi: " + dbVrijednost);
    }
}