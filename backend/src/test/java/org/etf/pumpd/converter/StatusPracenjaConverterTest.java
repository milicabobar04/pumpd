package org.etf.pumpd.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.etf.pumpd.enums.StatusPracenja;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Prevod statusa praćenja između enuma i vrijednosti iz baze (V2 migracija). */
class StatusPracenjaConverterTest {

    private final StatusPracenjaConverter konverter = new StatusPracenjaConverter();

    @ParameterizedTest
    @CsvSource({"PENDING, na_cekanju", "ACCEPTED, aktivan", "REJECTED, neaktivan"})
    void prevodiUObaSmjera(StatusPracenja status, String uBazi) {
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
        assertThrows(IllegalArgumentException.class, () -> konverter.convertToEntityAttribute("pratim"));
    }
}
