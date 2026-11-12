package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
import static org.etf.pumpd.service.TestPomocni.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.etf.pumpd.dto.InfoTeretanaDto;
import org.etf.pumpd.dto.IzmjenaTeretaneZahtjev;

import org.etf.pumpd.entity.Teretana;
import org.etf.pumpd.repository.TeretanaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class TeretanaServiceTest {

    private TeretanaRepository teretanaRepository;
    private TeretanaService servis;

    @BeforeEach
    void postavi() {
        teretanaRepository = mock(TeretanaRepository.class);
        servis = new TeretanaService(teretanaRepository, SAT);
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

    // ------------------------------------------------------------ 5.2 uređivanje

    /** "Danas" prema fiksiranom satu iz TestPomocni je 29.10.2026. */
    private static IzmjenaTeretaneZahtjev izmjena(LocalDate osnovana) {
        return new IzmjenaTeretaneZahtjev(" Pumpd Gym Plus ", " Novi opis. ", 45.5, osnovana, " Pon-Ned 06-23 ");
    }

    private Teretana sacuvana() {
        ArgumentCaptor<Teretana> hvatac = ArgumentCaptor.forClass(Teretana.class);
        verify(teretanaRepository).save(hvatac.capture());
        return hvatac.getValue();
    }

    @Test
    void izmjenaMijenjaPetPoljaIOstavljaOstale() {
        Teretana postojeca = teretana(new BigDecimal("40.00"));
        postojeca.setAdresa("Ulica Zdravlja 12");
        postojeca.setKapacitet(80);
        when(teretanaRepository.findById(Teretana.ID)).thenReturn(Optional.of(postojeca));

        assertEquals("Informacije o teretani su azurirane.",
                servis.izmijeni(izmjena(LocalDate.of(2020, 1, 1))).message());

        Teretana t = sacuvana();
        assertEquals("Pumpd Gym Plus", t.getNaziv());
        assertEquals("Novi opis.", t.getOpis());
        assertEquals(0, new BigDecimal("45.5").compareTo(t.getCijenaClanarine()));
        assertEquals(LocalDate.of(2020, 1, 1), t.getDatumOsnivanja());
        assertEquals("Pon-Ned 06-23", t.getRadnoVrijeme());
        assertEquals("Ulica Zdravlja 12", t.getAdresa(), "kolone van ugovora se ne mijenjaju");
        assertEquals(80, t.getKapacitet());
    }

    @Test
    void izmjenaPraviRedAkoNePostoji() {
        when(teretanaRepository.findById(Teretana.ID)).thenReturn(Optional.empty());

        servis.izmijeni(izmjena(LocalDate.of(2020, 1, 1)));

        Teretana t = sacuvana();
        assertEquals(Teretana.ID, t.getId());
        assertEquals("Pumpd Gym Plus", t.getNaziv());
    }

    @Test
    void danasnjiDatumOsnivanjaJeDozvoljen() {
        when(teretanaRepository.findById(Teretana.ID)).thenReturn(Optional.empty());

        servis.izmijeni(izmjena(LocalDate.of(2026, 10, 29)));

        assertEquals(LocalDate.of(2026, 10, 29), sacuvana().getDatumOsnivanja());
    }

    @Test
    void datumOsnivanjaUBuducnostiBaca400() {
        assertStatus(400, () -> servis.izmijeni(izmjena(LocalDate.of(2026, 10, 30))));
        verify(teretanaRepository, never()).save(any());
    }
}
