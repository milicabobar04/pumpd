package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
import static org.etf.pumpd.service.TestPomocni.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.dto.MojaObjavaDto;
import org.etf.pumpd.dto.NovaObjavaOdgovor;
import org.etf.pumpd.dto.NovaObjavaZahtjev;
import org.etf.pumpd.dto.ObjavaDto;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Objava;
import org.etf.pumpd.entity.SacuvanaObjava;
import org.etf.pumpd.entity.SacuvanaObjavaId;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.enums.StatusPracenja;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ObjavaRepository;
import org.etf.pumpd.repository.SacuvanaObjavaRepository;
import org.etf.pumpd.repository.TrenerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;

class ObjaveServiceTest {

    private static final int TRENER_ID = 3;
    private static final String SLIKA = "/images/7f3c9a2e-41b8-4d2a-9c11-5e8a0b6d2f44.jpg";

    private ObjavaRepository objavaRepository;
    private SacuvanaObjavaRepository sacuvanaObjavaRepository;
    private TrenerRepository trenerRepository;
    private ClanRepository clanRepository;
    private SlikeService slikeService;
    private ObjaveService servis;
    private Trener trener;

    @BeforeEach
    void postavi() {
        objavaRepository = mock(ObjavaRepository.class);
        sacuvanaObjavaRepository = mock(SacuvanaObjavaRepository.class);
        trenerRepository = mock(TrenerRepository.class);
        clanRepository = mock(ClanRepository.class);
        slikeService = mock(SlikeService.class);
        servis = new ObjaveService(objavaRepository, sacuvanaObjavaRepository, trenerRepository, clanRepository,
                slikeService, SAT);

        trener = trener(TRENER_ID);
        when(trenerRepository.pronadjiAktivnog(TRENER_ID)).thenReturn(Optional.of(trener));
        when(objavaRepository.save(any(Objava.class))).thenAnswer(poziv -> {
            Objava o = poziv.getArgument(0);
            o.setId(31);
            return o;
        });
    }

    private static Trener trener(int id) {
        Nalog nalog = new Nalog("Marko", "Petrović", "trener" + id + "@pumpd.local", "hash");
        nalog.setId(id);
        return new Trener(nalog, null, null, null);
    }

    private Objava sacuvana() {
        ArgumentCaptor<Objava> hvatac = ArgumentCaptor.forClass(Objava.class);
        verify(objavaRepository).save(hvatac.capture());
        return hvatac.getValue();
    }

    // ------------------------------------------------------------ 8.6 dodavanje

    @Test
    void dodavanjeSaSlikom() {
        when(slikeService.postoji(SLIKA)).thenReturn(true);

        NovaObjavaOdgovor odgovor = servis.dodajObjavu(TRENER_ID, new NovaObjavaZahtjev("  Novi trening!  ", SLIKA));

        assertEquals(new NovaObjavaOdgovor(31, "Objava je dodata."), odgovor);
        Objava o = sacuvana();
        assertSame(trener, o.getTrener());
        assertEquals("Novi trening!", o.getSadrzaj());
        assertEquals(SLIKA, o.getSlikaPath());
        // TestPomocni.SAT = 29.10.2026. 10:15:00 UTC
        assertEquals(LocalDateTime.of(2026, 10, 29, 10, 15, 0), o.getVrijeme());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void bezSlike(String slika) {
        servis.dodajObjavu(TRENER_ID, new NovaObjavaZahtjev("Tekst", slika));

        assertNull(sacuvana().getSlikaPath());
        verify(slikeService, never()).postoji(anyString());
    }

    @Test
    void vrijemeJeZaokruzenoNaSekunde() {
        Clock satSaDjelicima = Clock.fixed(Instant.parse("2026-11-22T10:30:15.987654321Z"), ZoneOffset.UTC);
        servis = new ObjaveService(objavaRepository, sacuvanaObjavaRepository, trenerRepository, clanRepository,
                slikeService, satSaDjelicima);

        servis.dodajObjavu(TRENER_ID, new NovaObjavaZahtjev("Tekst", null));

        assertEquals(LocalDateTime.of(2026, 11, 22, 10, 30, 15), sacuvana().getVrijeme());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://nekisajt.com/slika.jpg", SLIKA})
    void slikaKojaNijeNasaIliNePostojiDaje400(String slika) {
        when(slikeService.postoji(anyString())).thenReturn(false);

        assertStatus(400, () -> servis.dodajObjavu(TRENER_ID, new NovaObjavaZahtjev("Tekst", slika)));
        verify(objavaRepository, never()).save(any());
    }

    // ------------------------------------------------------------ 8.5 sopstvene objave

    @Test
    void mojeObjaveUOblikuIzUgovora() {
        Objava o = new Objava(trener, "Tekst", SLIKA, LocalDateTime.of(2026, 11, 22, 10, 30, 15));
        o.setId(31);
        when(objavaRepository.findByTrenerIdOrderByVrijemeDescIdDesc(TRENER_ID)).thenReturn(List.of(o));

        assertEquals(List.of(new MojaObjavaDto(31, "Tekst", SLIKA, LocalDateTime.of(2026, 11, 22, 10, 30, 15))),
                servis.mojeObjave(TRENER_ID));
    }

    @Test
    void bezObjavaPraznaLista() {
        when(objavaRepository.findByTrenerIdOrderByVrijemeDescIdDesc(TRENER_ID)).thenReturn(List.of());

        assertTrue(servis.mojeObjave(TRENER_ID).isEmpty());
    }

    // ------------------------------------------------------------ 8.7 brisanje

    private Objava postojecaObjava(Trener autor, String slika) {
        Objava o = new Objava(autor, "Tekst", slika, LocalDateTime.of(2026, 11, 22, 10, 30, 15));
        o.setId(31);
        when(objavaRepository.findById(31)).thenReturn(Optional.of(o));
        return o;
    }

    @Test
    void brisanjeBriseSacuvaneRedovePaObjavuPaSliku() {
        Objava o = postojecaObjava(trener, SLIKA);

        assertEquals("Objava je obrisana.", servis.obrisiObjavu(31, TRENER_ID).message());

        InOrder redoslijed = inOrder(sacuvanaObjavaRepository, objavaRepository, slikeService);
        redoslijed.verify(sacuvanaObjavaRepository).obrisiZaObjavu(31);
        redoslijed.verify(objavaRepository).delete(o);
        redoslijed.verify(slikeService).obrisiPoslijeCommita(SLIKA);
    }

    @Test
    void nepostojecaObjavaDaje404() {
        when(objavaRepository.findById(99)).thenReturn(Optional.empty());

        ResursNijePronadjenException e =
                assertThrows(ResursNijePronadjenException.class, () -> servis.obrisiObjavu(99, TRENER_ID));
        assertEquals("Objava ne postoji.", e.getMessage());
    }

    @Test
    void tudjaObjavaDaje403INistaSeNeBrise() {
        postojecaObjava(trener(4), SLIKA);

        var e = assertStatus(403, () -> servis.obrisiObjavu(31, TRENER_ID));

        assertEquals("Objava pripada drugom treneru.", e.getReason());
        verify(sacuvanaObjavaRepository, never()).obrisiZaObjavu(anyInt());
        verify(objavaRepository, never()).delete(any());
        verify(slikeService, never()).obrisiPoslijeCommita(any());
    }

    // ------------------------------------------------------------ 8.1 feed

    private static final int CLAN_ID = 6;

    private static Objava objava(int id, Trener autor, String sadrzaj, LocalDateTime vrijeme) {
        Objava o = new Objava(autor, sadrzaj, null, vrijeme);
        o.setId(id);
        return o;
    }

    @Test
    void feedSaImenomTreneraIOznakomSacuvanih() {
        Trener ivana = trener(4);
        ivana.getNalog().setIme("Ivana");
        ivana.getNalog().setPrezime("Jovanović");
        Objava novija = objava(33, ivana, "Joga u subotu!", LocalDateTime.of(2026, 11, 22, 10, 30, 0));
        Objava starija = objava(31, trener, "Novi trening!", LocalDateTime.of(2026, 11, 22, 9, 15, 0));
        when(objavaRepository.feed(CLAN_ID, StatusPracenja.ACCEPTED)).thenReturn(List.of(novija, starija));
        when(sacuvanaObjavaRepository.sacuvaneMedju(CLAN_ID, List.of(33, 31))).thenReturn(List.of(33));

        List<ObjavaDto> feed = servis.feed(CLAN_ID);

        assertEquals(List.of(
                new ObjavaDto(33, 4, "Ivana Jovanović", "Joga u subotu!", null,
                        LocalDateTime.of(2026, 11, 22, 10, 30, 0), true),
                new ObjavaDto(31, TRENER_ID, "Marko Petrović", "Novi trening!", null,
                        LocalDateTime.of(2026, 11, 22, 9, 15, 0), false)), feed);
    }

    @Test
    void sacuvaneSeCitajuJednimPozivomZaCijeliFeed() {
        when(objavaRepository.feed(CLAN_ID, StatusPracenja.ACCEPTED)).thenReturn(List.of(
                objava(3, trener, "a", LocalDateTime.of(2026, 11, 22, 3, 0)),
                objava(2, trener, "b", LocalDateTime.of(2026, 11, 22, 2, 0)),
                objava(1, trener, "c", LocalDateTime.of(2026, 11, 22, 1, 0))));
        when(sacuvanaObjavaRepository.sacuvaneMedju(anyInt(), anyCollection())).thenReturn(List.of());

        servis.feed(CLAN_ID);

        verify(sacuvanaObjavaRepository, times(1)).sacuvaneMedju(CLAN_ID, List.of(3, 2, 1));
    }

    @Test
    void prazanFeedBezUpitaZaSacuvane() {
        when(objavaRepository.feed(CLAN_ID, StatusPracenja.ACCEPTED)).thenReturn(List.of());

        assertTrue(servis.feed(CLAN_ID).isEmpty());
        verify(sacuvanaObjavaRepository, never()).sacuvaneMedju(anyInt(), anyCollection());
    }

    // ------------------------------------------------------------ 8.2 čuvanje

    private Clan clan() {
        Nalog nalog = new Nalog("Amar", "Hodžić", "clan1@pumpd.local", "hash");
        nalog.setId(CLAN_ID);
        Clan clan = new Clan(nalog, null, null, null, null);
        when(clanRepository.pronadjiAktivnog(CLAN_ID)).thenReturn(Optional.of(clan));
        return clan;
    }

    @Test
    void cuvanjeUpisujeRedSaDanasnjimDatumom() {
        Objava o = postojecaObjava(trener, null);
        Clan clan = clan();
        when(sacuvanaObjavaRepository.existsById(new SacuvanaObjavaId(CLAN_ID, 31))).thenReturn(false);

        assertEquals("Objava je sacuvana.", servis.sacuvaj(31, CLAN_ID).message());

        ArgumentCaptor<SacuvanaObjava> hvatac = ArgumentCaptor.forClass(SacuvanaObjava.class);
        verify(sacuvanaObjavaRepository).saveAndFlush(hvatac.capture());
        assertSame(clan, hvatac.getValue().getClan());
        assertSame(o, hvatac.getValue().getObjava());
        assertEquals(LocalDate.of(2026, 10, 29), hvatac.getValue().getDatumCuvanja());
        assertTrue(hvatac.getValue().isNew(), "novi red ide kao INSERT");
    }

    @Test
    void vecSacuvanaDaje409BezUpisa() {
        postojecaObjava(trener, null);
        clan();
        when(sacuvanaObjavaRepository.existsById(new SacuvanaObjavaId(CLAN_ID, 31))).thenReturn(true);

        KonfliktException e = assertThrows(KonfliktException.class, () -> servis.sacuvaj(31, CLAN_ID));

        assertEquals("Objava je vec sacuvana.", e.getMessage());
        verify(sacuvanaObjavaRepository, never()).saveAndFlush(any());
    }

    @Test
    void istovremeniDvostrukiKlikDaje409() {
        postojecaObjava(trener, null);
        clan();
        when(sacuvanaObjavaRepository.existsById(any())).thenReturn(false);
        when(sacuvanaObjavaRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'PRIMARY'"));

        assertThrows(KonfliktException.class, () -> servis.sacuvaj(31, CLAN_ID));
    }

    @Test
    void nepostojecaObjavaDaje404PriCuvanju() {
        when(objavaRepository.findById(99)).thenReturn(Optional.empty());

        ResursNijePronadjenException e =
                assertThrows(ResursNijePronadjenException.class, () -> servis.sacuvaj(99, CLAN_ID));
        assertEquals("Objava ne postoji.", e.getMessage());
    }

    @Test
    void objavaObrisanogTreneraDaje404PriCuvanju() {
        postojecaObjava(trener, null);
        trener.getNalog().deaktiviraj();

        assertThrows(ResursNijePronadjenException.class, () -> servis.sacuvaj(31, CLAN_ID));
        verify(sacuvanaObjavaRepository, never()).saveAndFlush(any());
    }

    // ------------------------------------------------------------ 8.3 sačuvane objave

    @Test
    void sacuvaneSuUOblikuFeedaSaIsSavedTrue() {
        Objava o = objava(31, trener, "Novi trening!", LocalDateTime.of(2026, 11, 22, 9, 15, 0));
        Nalog nalog = new Nalog("Amar", "Hodžić", "clan1@pumpd.local", "hash");
        nalog.setId(CLAN_ID);
        when(sacuvanaObjavaRepository.sacuvaneClana(CLAN_ID)).thenReturn(List.of(
                new SacuvanaObjava(new Clan(nalog, null, null, null, null), o, LocalDate.of(2026, 11, 22))));

        assertEquals(List.of(new ObjavaDto(31, TRENER_ID, "Marko Petrović", "Novi trening!", null,
                LocalDateTime.of(2026, 11, 22, 9, 15, 0), true)), servis.sacuvane(CLAN_ID));
    }

    @Test
    void bezSacuvanihPraznaLista() {
        when(sacuvanaObjavaRepository.sacuvaneClana(CLAN_ID)).thenReturn(List.of());

        assertTrue(servis.sacuvane(CLAN_ID).isEmpty());
    }

    // ------------------------------------------------------------ 8.4 uklanjanje iz sačuvanih

    @Test
    void uklanjanjeBriseRed() {
        Objava o = objava(31, trener, "x", LocalDateTime.of(2026, 11, 22, 9, 15, 0));
        Nalog nalog = new Nalog("Amar", "Hodžić", "clan1@pumpd.local", "hash");
        nalog.setId(CLAN_ID);
        SacuvanaObjava sacuvana = new SacuvanaObjava(new Clan(nalog, null, null, null, null), o,
                LocalDate.of(2026, 11, 22));
        when(sacuvanaObjavaRepository.findById(new SacuvanaObjavaId(CLAN_ID, 31))).thenReturn(Optional.of(sacuvana));

        assertEquals("Objava je uklonjena iz sacuvanih.", servis.ukloniIzSacuvanih(31, CLAN_ID).message());
        verify(sacuvanaObjavaRepository).delete(sacuvana);
    }

    @Test
    void uklanjanjeObjaveKojaNijeSacuvanaDaje404() {
        when(sacuvanaObjavaRepository.findById(any())).thenReturn(Optional.empty());

        ResursNijePronadjenException e =
                assertThrows(ResursNijePronadjenException.class, () -> servis.ukloniIzSacuvanih(31, CLAN_ID));
        assertEquals("Objava nije u sacuvanim.", e.getMessage());
        verify(sacuvanaObjavaRepository, never()).delete(any());
    }
}
