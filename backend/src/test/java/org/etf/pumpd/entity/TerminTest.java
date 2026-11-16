package org.etf.pumpd.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Vremenska pravila termina: kraj, da li je počeo i preklapanje (za 10.5 i 10.6). */
class TerminTest {

    /** Termin od 18:00 do 19:00. */
    private static final LocalDateTime POCETAK = LocalDateTime.of(2026, 11, 20, 18, 0);
    private final Termin termin = new Termin(POCETAK, 60, null, null);

    @Test
    void krajJePocetakPlusTrajanje() {
        assertEquals(POCETAK.plusMinutes(60), termin.getKraj());
    }

    @Test
    void jePoceoOdTrenutkaPocetka() {
        assertFalse(termin.jePoceo(POCETAK.minusMinutes(1)));
        assertTrue(termin.jePoceo(POCETAK));
        assertTrue(termin.jePoceo(POCETAK.plusDays(1)));
    }

    @ParameterizedTest(name = "{0} + {1} min → preklapanje {2}")
    @CsvSource({
            "17:00, 60, false",   // završava tačno kad ovaj počinje
            "19:00, 30, false",   // počinje tačno kad ovaj završava
            "17:30, 60, true",    // ulazi u početak
            "18:30, 60, true",    // počinje u sredini
            "18:15, 15, true",    // cijeli unutar
            "17:00, 180, true",   // obuhvata cijeli termin
            "20:00, 60, false"})  // kasnije
    void preklapanje(String vrijeme, int trajanje, boolean ocekivano) {
        LocalDateTime pocetak = POCETAK.toLocalDate().atTime(java.time.LocalTime.parse(vrijeme));
        assertEquals(ocekivano, termin.preklapaSe(pocetak, trajanje));
    }
}
