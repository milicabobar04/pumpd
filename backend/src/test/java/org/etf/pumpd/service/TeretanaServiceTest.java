package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.etf.pumpd.dto.InfoTeretanaDto;

import org.etf.pumpd.entity.Teretana;
import org.etf.pumpd.repository.TeretanaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeretanaServiceTest {

    private TeretanaRepository teretanaRepository;
    private TeretanaService servis;

    @BeforeEach
    void postavi() {
        teretanaRepository = mock(TeretanaRepository.class);
        servis = new TeretanaService(teretanaRepository);
    }

    private Teretana teretana(BigDecimal cijena) {
        Teretana t = new Teretana();
        t.setNaziv("Pumpd Gym");
        t.setOpis("Moderna teretana.");
        t.setRadnoVrijeme("Pon-Pet 06:00-22:00");
        t.setCijenaClanarine(cijena);
        t.setDatumOsnivanja(LocalDate.of(2019, 3, 15));
        return t;
    }

    @Test
    void vracaPodatkeUOblikuIzUgovora() {
        when(teretanaRepository.findById(Teretana.ID)).thenReturn(Optional.of(teretana(new BigDecimal("40.00"))));

        InfoTeretanaDto info = servis.informacije();

        assertEquals(new InfoTeretanaDto("Pumpd Gym", "Moderna teretana.", 40.0,
                LocalDate.of(2019, 3, 15), "Pon-Pet 06:00-22:00"), info);
    }

    @Test
    void nepoznataCijenaJeNull() {
        when(teretanaRepository.findById(Teretana.ID)).thenReturn(Optional.of(teretana(null)));

        assertNull(servis.informacije().membershipPrice());
    }

    @Test
    void bezRedaUBaziVraca404() {
        when(teretanaRepository.findById(Teretana.ID)).thenReturn(Optional.empty());

        assertStatus(404, () -> servis.informacije());
    }
}
