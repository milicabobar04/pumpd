package org.etf.pumpd.enums;

/**
 * followStatus iz 7.2: odnos prijavljenog člana i trenera, za prikaz dugmeta „Prati“.
 * Odbijen zahtjev (StatusPracenja.REJECTED) se prikazuje kao NONE, jer član može poslati novi.
 */
public enum StatusPracenjaClana {
    NONE,
    PENDING,
    ACCEPTED;

    public static StatusPracenjaClana iz(StatusPracenja statusUBazi) {
        if (statusUBazi == null) {
            return NONE;
        }
        return switch (statusUBazi) {
            case PENDING -> PENDING;
            case ACCEPTED -> ACCEPTED;
            case REJECTED -> NONE;
        };
    }
}
