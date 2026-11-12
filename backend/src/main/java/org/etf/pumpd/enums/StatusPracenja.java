package org.etf.pumpd.enums;

/**
 * Stanje reda u tabeli pracenje (vidi V2 migraciju).
 * PENDING = zahtjev čeka trenera, ACCEPTED = član prati trenera, REJECTED = trener je odbio zahtjev.
 * Ovo nije followStatus iz 7.2 (NONE/PENDING/ACCEPTED), koji je oblik odgovora za aplikaciju.
 */
public enum StatusPracenja {
    PENDING("na_cekanju"),
    ACCEPTED("aktivan"),
    REJECTED("neaktivan");

    private final String dbVrijednost;

    StatusPracenja(String dbVrijednost) {
        this.dbVrijednost = dbVrijednost;
    }

    public String getDbVrijednost() {
        return dbVrijednost;
    }

    public static StatusPracenja izBaze(String dbVrijednost) {
        for (StatusPracenja status : values()) {
            if (status.dbVrijednost.equals(dbVrijednost)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Nepoznat status pracenja u bazi: " + dbVrijednost);
    }
}
