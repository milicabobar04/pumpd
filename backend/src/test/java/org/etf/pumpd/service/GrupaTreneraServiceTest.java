package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
import static org.etf.pumpd.service.TestPomocni.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.dto.ClanGrupeDto;
import org.etf.pumpd.dto.GrupaDto;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.ClanNaTerminu;
import org.etf.pumpd.entity.ClanNaTerminuId;
import org.etf.pumpd.entity.ClanUGrupi;
import org.etf.pumpd.entity.ClanUGrupiId;
import org.etf.pumpd.entity.Grupa;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Pracenje;
import org.etf.pumpd.entity.Termin;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.enums.StatusPracenja;
import org.etf.pumpd.enums.StatusPrijave;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanNaTerminuRepository;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ClanUGrupiRepository;
import org.etf.pumpd.repository.GrupaRepository;
import org.etf.pumpd.repository.PracenjeRepository;
import org.etf.pumpd.repository.TerminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GrupaTreneraServiceTest {

    /** "Sada" prema fiksiranom satu iz TestPomocni: 29.10.2026. u 10:15. */
    private static final LocalDateTime SADA = LocalDateTime.of(2026, 10, 29, 10, 15);
    private static final LocalDate DANAS = SADA.toLocalDate();
    private static final LocalDateTime SUTRA_18H = LocalDateTime.of(2026, 10, 30, 18, 0);
    private static final int TRENER = 3;
    private static final int DRUGI_TRENER = 4;
    private static final int GRUPA = 50;
    private static final int TERMIN = 70;
    private static final int CLAN = 6;

    private TerminRepository terminRepository;
    private GrupaRepository grupaRepository;
    private ClanUGrupiRepository clanUGrupiRepository;
    private ClanNaTerminuRepository prijavaRepository;
    private ClanRepository clanRepository;
    private PracenjeRepository pracenjeRepository;
    private ObavjestenjeService obavjestenjeService;
    private GrupaTreneraService servis;
    private Trener trener;
    private Clan clan;

    @BeforeEach
    void postavi() throws Exception {
        terminRepository = mock(TerminRepository.class);
        grupaRepository = mock(GrupaRepository.class);
        clanUGrupiRepository = mock(ClanUGrupiRepository.class);
        prijavaRepository = mock(ClanNaTerminuRepository.class);
        clanRepository = mock(ClanRepository.class);
        pracenjeRepository = mock(PracenjeRepository.class);
        obavjestenjeService = mock(ObavjestenjeService.class);
        servis = new GrupaTreneraService(terminRepository, grupaRepository, clanUGrupiRepository,
                prijavaRepository, clanRepository, pracenjeRepository, obavjestenjeService, SAT);

        trener = trener(TRENER);
        clan = clan(CLAN);
        when(clanRepository.pronadjiAktivnog(CLAN)).thenReturn(Optional.of(clan));
        when(prijavaRepository.findById(any())).thenReturn(Optional.empty());
    }

    private static <T> T postaviId(T entitet, int id) throws Exception {
        Field polje = entitet.getClass().getDeclaredField("id");
        polje.setAccessible(true);
        polje.set(entitet, id);
        return entitet;
    }

    private static Trener trener(int id) throws Exception {
        return postaviId(new Trener(new Nalog("Marko", "Petrovic", "t" + id + "@pumpd.local", "hash"), null, null, null), id);
    }

    private static Clan clan(int id) {
        Nalog nalog = new Nalog("Ana", "Babic", "c" + id + "@pumpd.local", "hash");
        nalog.setId(id);
        return new Clan(nalog, null, null, null, null);
    }

    /** Termin 70 sa grupom 50 (kapacitet zadat) koji pripada datom treneru. */
    private Termin termin(Trener vlasnik, LocalDateTime vrijeme, int kapacitet) throws Exception {
        Grupa grupa = postaviId(new Grupa("Joga", kapacitet, vlasnik), GRUPA);
        Termin termin = postaviId(new Termin(vrijeme, 60, vlasnik, grupa), TERMIN);
        when(terminRepository.poGrupi(GRUPA)).thenReturn(Optional.of(termin));
        when(grupaRepository.zakljucaj(GRUPA)).thenReturn(Optional.of(grupa));
        return termin;
    }

    private void pratilac(StatusPracenja status) {
        when(pracenjeRepository.findByTrenerIdAndClanId(TRENER, CLAN))
                .thenReturn(Optional.of(new Pracenje(trener, clan, DANAS, status)));
    }

    // ------------------------------------------------------------ 10.8 lista grupa

    @Test
    void listaPrvoPredstojeciPaProsli() throws Exception {
        Termin prosli = postaviId(new Termin(SADA.minusDays(7), 60, trener, postaviId(new Grupa("Stari", 10, trener), 1)), 11);
        Termin prekosutra = postaviId(new Termin(SUTRA_18H.plusDays(1), 60, trener, postaviId(new Grupa("Kasniji", 10, trener), 2)), 12);
        Termin sutra = postaviId(new Termin(SUTRA_18H, 60, trener, postaviId(new Grupa("Raniji", 8, trener), 3)), 13);
        when(terminRepository.terminiTrenera(TRENER)).thenReturn(List.of(prekosutra, sutra, prosli));
        when(clanUGrupiRepository.brojClanova(3)).thenReturn(5L);

        List<GrupaDto> grupe = servis.listaGrupa(TRENER);

        assertEquals(List.of("Raniji", "Kasniji", "Stari"), grupe.stream().map(GrupaDto::name).toList());
        assertEquals(3, grupe.get(0).groupId());
        assertEquals(13, grupe.get(0).sessionId());
        assertEquals(5, grupe.get(0).memberCount());
        assertEquals(8, grupe.get(0).maxMembers());
    }

    // ------------------------------------------------------------ 10.9 članovi grupe

    @Test
    void clanoviGrupe() throws Exception {
        Termin t = termin(trener, SUTRA_18H, 10);
        when(clanUGrupiRepository.clanoviGrupe(GRUPA)).thenReturn(List.of(new ClanUGrupi(clan, t.getGrupa(), DANAS)));

        List<ClanGrupeDto> clanovi = servis.clanoviGrupe(TRENER, GRUPA);

        assertEquals(List.of(new ClanGrupeDto(CLAN, "Ana", "Babic")), clanovi);
    }

    @Test
    void nepostojecaGrupaDaje404ITudjaDaje403() throws Exception {
        when(terminRepository.poGrupi(99)).thenReturn(Optional.empty());
        assertThrows(ResursNijePronadjenException.class, () -> servis.clanoviGrupe(TRENER, 99));
        assertThrows(ResursNijePronadjenException.class, () -> servis.dodajClana(TRENER, 99, CLAN));

        termin(trener(DRUGI_TRENER), SUTRA_18H, 10);
        assertStatus(403, () -> servis.clanoviGrupe(TRENER, GRUPA));
        assertStatus(403, () -> servis.dodajClana(TRENER, GRUPA, CLAN));
        assertStatus(403, () -> servis.ukloniClana(TRENER, GRUPA, CLAN));
    }

    // ------------------------------------------------------------ 10.10 dodavanje

    @Test
    void dodajePratiocaUGrupuITerminIObavjestavaGa() throws Exception {
        Termin t = termin(trener, SUTRA_18H, 10);
        pratilac(StatusPracenja.ACCEPTED);
        when(clanUGrupiRepository.brojClanova(GRUPA)).thenReturn(9L);   // posljednje slobodno mjesto

        servis.dodajClana(TRENER, GRUPA, CLAN);

        ArgumentCaptor<ClanUGrupi> uGrupi = ArgumentCaptor.forClass(ClanUGrupi.class);
        verify(clanUGrupiRepository).save(uGrupi.capture());
        assertEquals(new ClanUGrupiId(CLAN, GRUPA), uGrupi.getValue().getId());
        ArgumentCaptor<ClanNaTerminu> prijava = ArgumentCaptor.forClass(ClanNaTerminu.class);
        verify(prijavaRepository).save(prijava.capture());
        assertEquals(new ClanNaTerminuId(TERMIN, CLAN), prijava.getValue().getId());
        assertEquals(StatusPrijave.PRIJAVLJEN, prijava.getValue().getStatus());
        assertEquals(t, prijava.getValue().getTermin());
        verify(obavjestenjeService).posalji(eq(CLAN), eq(TRENER), contains("dodao"));
    }

    @Test
    void zahtjevKojiCekaPostajePrihvacen() throws Exception {
        Termin t = termin(trener, SUTRA_18H, 10);
        pratilac(StatusPracenja.ACCEPTED);
        ClanNaTerminu ceka = new ClanNaTerminu(clan, t, StatusPrijave.NA_CEKANJU, DANAS.minusDays(1));
        when(prijavaRepository.findById(new ClanNaTerminuId(TERMIN, CLAN))).thenReturn(Optional.of(ceka));

        servis.dodajClana(TRENER, GRUPA, CLAN);

        assertEquals(StatusPrijave.PRIJAVLJEN, ceka.getStatus());
        verify(prijavaRepository, never()).save(any());
        verify(clanUGrupiRepository).save(any(ClanUGrupi.class));
    }

    @Test
    void clanKojiNijeAktivanPratilacDaje400() throws Exception {
        termin(trener, SUTRA_18H, 10);
        when(pracenjeRepository.findByTrenerIdAndClanId(TRENER, CLAN)).thenReturn(Optional.empty());
        assertStatus(400, () -> servis.dodajClana(TRENER, GRUPA, CLAN));

        pratilac(StatusPracenja.PENDING);
        assertStatus(400, () -> servis.dodajClana(TRENER, GRUPA, CLAN));
        verify(clanUGrupiRepository, never()).save(any());
    }

    @Test
    void nepostojeciClanDaje404() throws Exception {
        termin(trener, SUTRA_18H, 10);
        when(clanRepository.pronadjiAktivnog(77)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.dodajClana(TRENER, GRUPA, 77));
    }

    @Test
    void clanVecUGrupiDaje409() throws Exception {
        termin(trener, SUTRA_18H, 10);
        pratilac(StatusPracenja.ACCEPTED);
        when(clanUGrupiRepository.existsById(new ClanUGrupiId(CLAN, GRUPA))).thenReturn(true);

        assertThrows(KonfliktException.class, () -> servis.dodajClana(TRENER, GRUPA, CLAN));
        verify(clanUGrupiRepository, never()).save(any());
    }

    @Test
    void punaGrupaDaje409() throws Exception {
        termin(trener, SUTRA_18H, 10);
        pratilac(StatusPracenja.ACCEPTED);
        when(clanUGrupiRepository.brojClanova(GRUPA)).thenReturn(10L);

        assertThrows(KonfliktException.class, () -> servis.dodajClana(TRENER, GRUPA, CLAN));
        verify(clanUGrupiRepository, never()).save(any());
        verify(obavjestenjeService, never()).posalji(anyInt(), anyInt(), anyString());
    }

    @Test
    void terminKojiJePoceoSeNeMijenja() throws Exception {
        termin(trener, SADA.minusMinutes(5), 10);
        pratilac(StatusPracenja.ACCEPTED);

        assertThrows(KonfliktException.class, () -> servis.dodajClana(TRENER, GRUPA, CLAN));
        assertThrows(KonfliktException.class, () -> servis.ukloniClana(TRENER, GRUPA, CLAN));
    }

    // ------------------------------------------------------------ 10.11 uklanjanje

    @Test
    void uklanjaClanaIzGrupeITerminaIObavjestavaGa() throws Exception {
        Termin t = termin(trener, SUTRA_18H, 10);
        ClanUGrupi uGrupi = new ClanUGrupi(clan, t.getGrupa(), DANAS);
        ClanNaTerminu prijava = new ClanNaTerminu(clan, t, StatusPrijave.PRIJAVLJEN, DANAS);
        when(clanUGrupiRepository.findById(new ClanUGrupiId(CLAN, GRUPA))).thenReturn(Optional.of(uGrupi));
        when(prijavaRepository.findById(new ClanNaTerminuId(TERMIN, CLAN))).thenReturn(Optional.of(prijava));

        servis.ukloniClana(TRENER, GRUPA, CLAN);

        verify(clanUGrupiRepository).delete(uGrupi);
        verify(prijavaRepository).delete(prijava);
        verify(obavjestenjeService).posalji(eq(CLAN), eq(TRENER), contains("uklonio"));
    }

    @Test
    void clanKojiNijeUGrupiDaje404() throws Exception {
        termin(trener, SUTRA_18H, 10);
        when(clanUGrupiRepository.findById(new ClanUGrupiId(CLAN, GRUPA))).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.ukloniClana(TRENER, GRUPA, CLAN));
        verify(obavjestenjeService, never()).posalji(anyInt(), anyInt(), anyString());
    }
}
