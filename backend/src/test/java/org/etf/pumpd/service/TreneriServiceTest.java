package org.etf.pumpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.dto.RecenzijaDto;
import org.etf.pumpd.dto.TrenerZaClanaDetaljiDto;
import org.etf.pumpd.dto.TrenerZaClanaDto;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Pracenje;
import org.etf.pumpd.entity.Recenzija;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.enums.StatusPracenja;
import org.etf.pumpd.enums.StatusPracenjaClana;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.PracenjeRepository;
import org.etf.pumpd.repository.ProsjekOcjene;
import org.etf.pumpd.repository.RecenzijaRepository;
import org.etf.pumpd.repository.TrenerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TreneriServiceTest {

    private static final int CLAN_ID = 6;

    private TrenerRepository trenerRepository;
    private RecenzijaRepository recenzijaRepository;
    private PracenjeRepository pracenjeRepository;
    private TreneriService servis;

    @BeforeEach
    void postavi() {
        trenerRepository = mock(TrenerRepository.class);
        recenzijaRepository = mock(RecenzijaRepository.class);
        pracenjeRepository = mock(PracenjeRepository.class);
        servis = new TreneriService(trenerRepository, recenzijaRepository, pracenjeRepository);
    }

    private static Trener trener(int id, String ime, String prezime, String opis) {
        Nalog nalog = new Nalog(ime, prezime, ime.toLowerCase() + "@pumpd.local", "hash");
        nalog.setId(id);
        return new Trener(nalog, null, null, opis);
    }

    private static Clan clan(int id, String ime, String prezime) {
        Nalog nalog = new Nalog(ime, prezime, ime.toLowerCase() + "@pumpd.local", "hash");
        nalog.setId(id);
        return new Clan(nalog, null, null, null, null);
    }

    // ------------------------------------------------------------ 7.1 lista trenera

    @Test
    void listaSaProsjekomIBezRecenzija() {
        when(trenerRepository.sviSaNalogom()).thenReturn(List.of(
                trener(4, "Ivana", "Jovanović", null), trener(3, "Marko", "Petrović", null)));
        when(recenzijaRepository.prosjeciOcjena(List.of(4, 3)))
                .thenReturn(List.of(new ProsjekOcjene(3, 4.3333)));

        List<TrenerZaClanaDto> lista = servis.listaTrenera();

        assertEquals(List.of(
                new TrenerZaClanaDto(4, "Ivana", "Jovanović", null),
                new TrenerZaClanaDto(3, "Marko", "Petrović", 4.3)), lista);
    }

    @Test
    void prosjeciSeCitajuJednimPozivom() {
        when(trenerRepository.sviSaNalogom()).thenReturn(List.of(
                trener(3, "A", "A", null), trener(4, "B", "B", null), trener(5, "C", "C", null)));
        when(recenzijaRepository.prosjeciOcjena(anyCollection())).thenReturn(List.of());

        servis.listaTrenera();

        verify(recenzijaRepository, times(1)).prosjeciOcjena(anyCollection());
        verify(recenzijaRepository, never()).prosjecnaOcjena(any());
    }

    @Test
    void bezTreneraPraznaListaBezUpitaZaProsjeke() {
        when(trenerRepository.sviSaNalogom()).thenReturn(List.of());

        assertTrue(servis.listaTrenera().isEmpty());
        verify(recenzijaRepository, never()).prosjeciOcjena(anyCollection());
    }

    // ------------------------------------------------------------ 7.2 detalji trenera

    private void postojiTrener(Trener t, Double prosjek, List<Recenzija> recenzije) {
        int id = t.getNalog().getId();
        when(trenerRepository.pronadjiAktivnog(id)).thenReturn(Optional.of(t));
        when(recenzijaRepository.prosjecnaOcjena(id)).thenReturn(prosjek);
        when(recenzijaRepository.recenzijeTrenera(id)).thenReturn(recenzije);
    }

    @Test
    void detaljiSaRecenzijamaIImenomClana() {
        Trener marko = trener(3, "Marko", "Petrović", "Kondicioni trener.");
        Recenzija r = new Recenzija(marko, clan(CLAN_ID, "Amar", "Hodžić"), 5, "Odličan.", LocalDate.of(2026, 11, 10));
        r.setId(9);
        postojiTrener(marko, 4.6666, List.of(r));
        when(pracenjeRepository.findByTrenerIdAndClanId(3, CLAN_ID)).thenReturn(Optional.empty());

        TrenerZaClanaDetaljiDto d = servis.detaljiTrenera(3, CLAN_ID);

        assertEquals(new TrenerZaClanaDetaljiDto(3, "Marko", "Petrović", "Kondicioni trener.", 4.7,
                StatusPracenjaClana.NONE,
                List.of(new RecenzijaDto(9, "Amar Hodžić", 5, "Odličan.", LocalDate.of(2026, 11, 10)))), d);
    }

    @Test
    void detaljiBezRecenzija() {
        postojiTrener(trener(3, "Marko", "Petrović", null), null, List.of());
        when(pracenjeRepository.findByTrenerIdAndClanId(3, CLAN_ID)).thenReturn(Optional.empty());

        TrenerZaClanaDetaljiDto d = servis.detaljiTrenera(3, CLAN_ID);

        assertNull(d.averageGrade());
        assertNull(d.biography());
        assertTrue(d.reviews().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"PENDING, PENDING", "ACCEPTED, ACCEPTED", "REJECTED, NONE"})
    void followStatusPremaRedUBazi(StatusPracenja uBazi, StatusPracenjaClana ocekivano) {
        Trener marko = trener(3, "Marko", "Petrović", null);
        postojiTrener(marko, null, List.of());
        when(pracenjeRepository.findByTrenerIdAndClanId(3, CLAN_ID)).thenReturn(Optional.of(
                new Pracenje(marko, clan(CLAN_ID, "Amar", "Hodžić"), LocalDate.of(2026, 11, 1), uBazi)));

        assertEquals(ocekivano, servis.detaljiTrenera(3, CLAN_ID).followStatus());
    }

    @Test
    void bezRedaUBaziFollowStatusJeNone() {
        postojiTrener(trener(3, "Marko", "Petrović", null), null, List.of());
        when(pracenjeRepository.findByTrenerIdAndClanId(3, CLAN_ID)).thenReturn(Optional.empty());

        assertEquals(StatusPracenjaClana.NONE, servis.detaljiTrenera(3, CLAN_ID).followStatus());
    }

    @Test
    void nepostojeciIliObrisanTrenerBaca404() {
        when(trenerRepository.pronadjiAktivnog(99)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.detaljiTrenera(99, CLAN_ID));
    }
}
