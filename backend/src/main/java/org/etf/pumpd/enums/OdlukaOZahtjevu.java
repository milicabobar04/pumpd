package org.etf.pumpd.enums;

/**
 * 10.2: trenerova odluka o zahtjevu za praćenje. Samo dvije vrijednosti, pa "PENDING"
 * ili nepoznata vrijednost u zahtjevu automatski daju 400.
 */
public enum OdlukaOZahtjevu {
    ACCEPTED,
    REJECTED
}
