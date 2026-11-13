package org.etf.pumpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.etf.pumpd.dto.DodavanjeTreneraOdgovor;
import org.etf.pumpd.dto.DodavanjeTreneraZahtjev;
import org.etf.pumpd.dto.TrenerDetaljiDto;
import org.etf.pumpd.dto.TrenerListaDto;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.enums.StatusPracenja;
import org.etf.pumpd.repository.NalogRepository;
import org.etf.pumpd.repository.PracenjeRepository;
import org.etf.pumpd.repository.RecenzijaRepository;
import org.etf.pumpd.repository.TrenerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AdminTreneriServiceTest {

    private TrenerRepository trenerRepository;
    private RecenzijaRepository recenzijaRepository;
    private PracenjeRepository pracenjeRepository;
    private NalogRepository nalogRepository;
    private AdminTreneriService servis;

    @BeforeEach
    void postavi() {
        trenerRepository = mock(TrenerRepository.class);
        recenzijaRepository = mock(RecenzijaRepository.class);
        pracenjeRepository = mock(PracenjeRepository.class);
        nalogRepository = mock(NalogRepository.class);
        GeneratorLozinke generatorLozinke = mock(GeneratorLozinke.class);
        when(generatorLozinke.generisi()).thenReturn("Privremena7k");
        // pravi NoviNalogService nad mock repozitorijumom, da test pokriva cijeli tok dodavanja trenera
        servis = new AdminTreneriService(trenerRepository, recenzijaRepository, pracenjeRepository,
                new NoviNalogService(nalogRepository, new BCryptPasswordEncoder(4), generatorLozinke));
    }

    private static Trener trener(int id, String ime, String prezime, String opis) {
        Nalog nalog = new Nalog(ime, prezime, ime.toLowerCase() + "@pumpd.local", "hash");
        nalog.setId(id);
        return new Trener(nalog, null, null, opis);
    }

    private void postojiTrener(Trener trener, Double prosjek, long pratilaca) {
        int id = trener.getNalog().getId();
        when(trenerRepository.pronadjiAktivnog(id)).thenReturn(Optional.of(trener));
        when(recenzijaRepository.prosjecnaOcjena(id)).thenReturn(prosjek);
        // broje se samo prihvaćena praćenja
        when(pracenjeRepository.brojPratilaca(id, StatusPracenja.ACCEPTED)).thenReturn(pratilaca);
    }

    // ------------------------------------------------------------ 12.1 lista trenera

    @Test
    void listaVracaPodatkeIzNalogaRedomIzRepozitorijuma() {
        when(trenerRepository.sviSaNalogom()).thenReturn(List.of(
                trener(4, "Ivana", "Jovanović", null), trener(3, "Marko", "Petrović", null)));

        List<TrenerListaDto> lista = servis.listaTrenera();

        assertEquals(List.of(
                new TrenerListaDto(4, "Ivana", "Jovanović", "ivana@pumpd.local"),
                new TrenerListaDto(3, "Marko", "Petrović", "marko@pumpd.local")), lista);
    }

    @Test
    void bezTreneraVracaPraznuListu() {
        when(trenerRepository.sviSaNalogom()).thenReturn(List.of());

        assertTrue(servis.listaTrenera().isEmpty());
    }

    // ------------------------------------------------------------ 12.2 detalji trenera

    @Test
    void detaljiSaBiografijomOcjenomIPratiocima() {
        postojiTrener(trener(3, "Marko", "Petrović", "Kondicioni trener."), 4.5, 12);

        TrenerDetaljiDto detalji = servis.detaljiTrenera(3);

        assertEquals(new TrenerDetaljiDto(3, "Marko", "Petrović", "marko@pumpd.local",
                "Kondicioni trener.", 4.5, 12), detalji);
    }

    @Test
    void bezRecenzijaOcjenaJeNull() {
        postojiTrener(trener(3, "Marko", "Petrović", null), null, 0);

        TrenerDetaljiDto detalji = servis.detaljiTrenera(3);

        assertNull(detalji.averageGrade());
        assertNull(detalji.biography());
        assertEquals(0, detalji.followerCount());
    }

    @Test
    void prosjekSeZaokruzujeNaJednuDecimalu() {
        postojiTrener(trener(3, "Marko", "Petrović", null), 4.3333, 0);

        assertEquals(4.3, servis.detaljiTrenera(3).averageGrade());
    }

    @Test
    void nepostojeciTrenerBaca404() {
        when(trenerRepository.pronadjiAktivnog(99)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.detaljiTrenera(99));
    }

    // ------------------------------------------------------------ zaokruživanje ocjene

    @ParameterizedTest
    @CsvSource({"4.3333, 4.3", "4.25, 4.3", "4.24, 4.2", "5.0, 5.0", "1.0, 1.0", "3.96, 4.0"})
    void zaokruzivanjeOcjene(double prosjek, double ocekivano) {
        assertEquals(ocekivano, AdminTreneriService.zaokruziOcjenu(prosjek));
    }

    @Test
    void zaokruzivanjeNullOstajeNull() {
        assertNull(AdminTreneriService.zaokruziOcjenu(null));
    }

    // ------------------------------------------------------------ 12.3 dodavanje trenera

    private static DodavanjeTreneraZahtjev zahtjev(String biografija) {
        return new DodavanjeTreneraZahtjev("Ana", "Ilić", "ana@mail.com", biografija);
    }

    /** Baza dodijeli id 11 pri upisu naloga. */
    private void bazaDodjeljujeId() {
        when(nalogRepository.existsByEmail(anyString())).thenReturn(false);
        when(nalogRepository.saveAndFlush(any(Nalog.class))).thenAnswer(poziv -> {
            Nalog n = poziv.getArgument(0);
            n.setId(11);
            return n;
        });
    }

    private Trener sacuvaniTrener() {
        ArgumentCaptor<Trener> trener = ArgumentCaptor.forClass(Trener.class);
        verify(trenerRepository).save(trener.capture());
        return trener.getValue();
    }

    @Test
    void dodavanjeVracaIdNovogTrenera() {
        bazaDodjeljujeId();

        DodavanjeTreneraOdgovor odgovor = servis.dodajTrenera(zahtjev("Pilates instruktor."));

        assertEquals(new DodavanjeTreneraOdgovor(11, "Privremena7k", "Trener je uspjesno dodan."), odgovor);
    }

    @Test
    void trenerSeVezujeZaNalogSaBiografijom() {
        bazaDodjeljujeId();

        servis.dodajTrenera(zahtjev("  Pilates instruktor.  "));

        Trener trener = sacuvaniTrener();
        assertEquals(11, trener.getNalog().getId());
        assertEquals("Ana", trener.getNalog().getIme());
        assertEquals("Pilates instruktor.", trener.getOpis());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void praznaBiografijaSeCuvaKaoNull(String biografija) {
        bazaDodjeljujeId();

        servis.dodajTrenera(zahtjev(biografija));

        assertNull(sacuvaniTrener().getOpis());
    }

    @Test
    void zauzetEmailBaca409ITrenerSeNeUpisuje() {
        when(nalogRepository.existsByEmail("ana@mail.com")).thenReturn(true);

        assertThrows(KonfliktException.class, () -> servis.dodajTrenera(zahtjev(null)));
        verify(trenerRepository, never()).save(any());
    }

    // ------------------------------------------------------------ 12.4 uklanjanje trenera

    @Test
    void uklanjanjeDeaktiviraNalogIOslobadjaEmail() {
        Trener marko = trener(3, "Marko", "Petrović", "Kondicioni trener.");
        when(trenerRepository.pronadjiAktivnog(3)).thenReturn(Optional.of(marko));

        assertEquals("Trener je uspjesno uklonjen.", servis.ukloniTrenera(3).message());

        assertFalse(marko.getNalog().isAktivan());
        assertEquals("obrisan-3@pumpd.invalid", marko.getNalog().getEmail());
        assertEquals("Kondicioni trener.", marko.getOpis(), "podaci trenera ostaju zbog istorije");
    }

    @Test
    void uklanjanjeNepostojecegIliVecObrisanogTreneraBaca404() {
        when(trenerRepository.pronadjiAktivnog(99)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.ukloniTrenera(99));
    }
}
