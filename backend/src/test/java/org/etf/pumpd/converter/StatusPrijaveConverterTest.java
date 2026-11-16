package org.etf.pumpd.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.etf.pumpd.enums.StatusPrijave;
import org.etf.pumpd.enums.StatusPrijaveClana;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Prevod statusa prijave na termin između enuma, baze (V2 migracija) i ugovora (9.1). */
class StatusPrijaveConverterTest {

    private final StatusPrijaveConverter konverter = new StatusPrijaveConverter();

    @ParameterizedTest
    @CsvSource({"PRIJAVLJEN, prijavljen", "OTKAZAO, otkazao", "PRISUTAN, prisutan",
            "NA_CEKANJU, na_cekanju", "ODBIJEN, odbijen"})
    void prevodiUObaSmjera(StatusPrijave status, String uBazi) {
        assertEquals(uBazi, konverter.convertToDatabaseColumn(status));
        assertEquals(status, konverter.convertToEntityAttribute(uBazi));
    }

    @Test
    void nullOstajeNull() {
        assertNull(konverter.convertToDatabaseColumn(null));
        assertNull(konverter.convertToEntityAttribute(null));
    }

    @Test
    void nepoznataVrijednostIzBazeJeGreska() {
        assertThrows(IllegalArgumentException.class, () -> konverter.convertToEntityAttribute("upisan"));
    }

    @ParameterizedTest
    @CsvSource({"NA_CEKANJU, PENDING", "PRIJAVLJEN, ACCEPTED", "PRISUTAN, ACCEPTED",
            "ODBIJEN, REJECTED", "OTKAZAO, NONE"})
    void statusZaUgovor(StatusPrijave uBazi, StatusPrijaveClana zaUgovor) {
        assertEquals(zaUgovor, StatusPrijaveClana.iz(uBazi));
    }

    @Test
    void bezPrijaveJeNone() {
        assertEquals(StatusPrijaveClana.NONE, StatusPrijaveClana.iz(null));
    }

    @Test
    void samoPrijavljenIPrisutanZauzimajuMjesto() {
        assertTrue(StatusPrijave.PRIJAVLJEN.jePrihvacen());
        assertTrue(StatusPrijave.PRISUTAN.jePrihvacen());
        assertFalse(StatusPrijave.NA_CEKANJU.jePrihvacen());
        assertFalse(StatusPrijave.ODBIJEN.jePrihvacen());
        assertFalse(StatusPrijave.OTKAZAO.jePrihvacen());
    }
}
