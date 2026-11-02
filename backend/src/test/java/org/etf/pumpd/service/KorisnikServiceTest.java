package org.etf.pumpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.repository.NalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KorisnikServiceTest {

    private NalogRepository nalogRepository;
    private KorisnikService servis;

    @BeforeEach
    void postavi() {
        nalogRepository = mock(NalogRepository.class);
        servis = new KorisnikService(nalogRepository);
    }

    @Test
    void ulogaIzBazePretvaraSeUEnum() {
        when(nalogRepository.pronadjiUlogu(1)).thenReturn(Optional.of("ADMIN"));
        when(nalogRepository.pronadjiUlogu(2)).thenReturn(Optional.of("WORKER"));
        when(nalogRepository.pronadjiUlogu(3)).thenReturn(Optional.of("TRAINER"));
        when(nalogRepository.pronadjiUlogu(4)).thenReturn(Optional.of("MEMBER"));

        assertEquals(Optional.of(Uloga.ADMIN), servis.odrediUlogu(1));
        assertEquals(Optional.of(Uloga.WORKER), servis.odrediUlogu(2));
        assertEquals(Optional.of(Uloga.TRAINER), servis.odrediUlogu(3));
        assertEquals(Optional.of(Uloga.MEMBER), servis.odrediUlogu(4));
    }

    @Test
    void nalogBezUlogeVracaPrazno() {
        when(nalogRepository.pronadjiUlogu(9)).thenReturn(Optional.empty());

        assertTrue(servis.odrediUlogu(9).isEmpty());
    }

    @Test
    void pronadjiPoEmailuVracaNalogIzRepozitorija() {
        Nalog nalog = new Nalog("Amar", "Hodžić", "clan1@pumpd.local", "hash");
        when(nalogRepository.findByEmail("clan1@pumpd.local")).thenReturn(Optional.of(nalog));

        assertSame(nalog, servis.pronadjiPoEmailu("clan1@pumpd.local").orElseThrow());
    }

    @Test
    void emailPostojiPitaRepozitorij() {
        when(nalogRepository.existsByEmail("a@b.c")).thenReturn(true);

        assertTrue(servis.emailPostoji("a@b.c"));
    }
}
