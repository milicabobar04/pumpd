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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.dto.KreiranjeTerminaOdgovor;
import org.etf.pumpd.dto.TerminRasporedDto;
import org.etf.pumpd.dto.TerminZahtjev;
import org.etf.pumpd.entity.Grupa;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Termin;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.enums.StatusPrijave;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanNaTerminuRepository;
import org.etf.pumpd.repository.ClanUGrupiRepository;
import org.etf.pumpd.repository.GrupaRepository;
import org.etf.pumpd.repository.TerminRepository;
import org.etf.pumpd.repository.TrenerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TerminTreneraServiceTest {

    /** "Sada" prema fiksiranom satu iz TestPomocni: 29.10.2026. u 10:15. */
    private static final LocalDateTime SADA = LocalDateTime.of(2026, 10, 29, 10, 15);
    private static final LocalDateTime SUTRA_18H = LocalDateTime.of(2026, 10, 30, 18, 0);
    private static final int TRENER = 3;
    private static final int DRUGI_TRENER = 4;

    private TerminRepository terminRepository;
    private GrupaRepository grupaRepository;
    private ClanUGrupiRepository clanUGrupiRepository;
    private ClanNaTerminuRepository prijavaRepository;
    private TrenerRepository trenerRepository;
    private ObavjestenjeService obavjestenjeService;
    private TerminTreneraService servis;
    private Trener trener;

    @BeforeEach
    void postavi() throws Exception {
        terminRepository = mock(TerminRepository.class);
        grupaRepository = mock(GrupaRepository.class);
        clanUGrupiRepository = mock(ClanUGrupiRepository.class);
        prijavaRepository = mock(ClanNaTerminuRepository.class);
        trenerRepository = mock(TrenerRepository.class);
        obavjestenjeService = mock(ObavjestenjeService.class);
        servis = new TerminTreneraService(terminRepository, grupaRepository, clanUGrupiRepository,
                prijavaRepository, trenerRepository, obavjestenjeService, SAT);

        trener = trener(TRENER);
        when(trenerRepository.getReferenceById(TRENER)).thenReturn(trener);
        when(grupaRepository.save(any(Grupa.class))).thenAnswer(g -> postaviId(g.getArgument(0), 50));
        when(terminRepository.save(any(Termin.class))).thenAnswer(t -> postaviId(t.getArgument(0), 70));
        when(terminRepository.terminiTreneraIzmedju(anyInt(), any(), any())).thenReturn(List.of());
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

    /** Postojeći termin trenera sa grupom (id termina 70, grupe 50). */
    private Termin termin(Trener vlasnik, LocalDateTime vrijeme, int trajanje, int kapacitet) throws Exception {
        Grupa grupa = postaviId(new Grupa("Joga", kapacitet, vlasnik), 50);
        Termin termin = postaviId(new Termin(vrijeme, trajanje, vlasnik, grupa), 70);
        when(terminRepository.saGrupom(70)).thenReturn(Optional.of(termin));
        when(grupaRepository.zakljucaj(50)).thenReturn(Optional.of(grupa));
        return termin;
    }

    private static TerminZahtjev zahtjev(LocalDateTime pocetak, int trajanje, int kapacitet) {
        return new TerminZahtjev("Joga", pocetak, trajanje, kapacitet);
    }

    // ------------------------------------------------------------ 10.5 kreiranje

    @Test
    void kreiraTerminIGrupu() {
        KreiranjeTerminaOdgovor odgovor = servis.kreiraj(TRENER, new TerminZahtjev(" Joga ", SUTRA_18H, 60, 12));

        assertEquals(70, odgovor.sessionId());
        assertEquals(50, odgovor.groupId());
        ArgumentCaptor<Grupa> grupa = ArgumentCaptor.forClass(Grupa.class);
        verify(grupaRepository).save(grupa.capture());
        assertEquals("Joga", grupa.getValue().getNaziv());
        assertEquals(12, grupa.getValue().getMaxClanova());
        ArgumentCaptor<Termin> termin = ArgumentCaptor.forClass(Termin.class);
        verify(terminRepository).save(termin.capture());
        assertEquals(SUTRA_18H, termin.getValue().getVrijeme());
        assertEquals(60, termin.getValue().getTrajanjeMinuta());
    }

    @Test
    void terminUProslostiIliSadaDaje400() {
        assertStatus(400, () -> servis.kreiraj(TRENER, zahtjev(SADA.minusMinutes(1), 60, 10)));
        assertStatus(400, () -> servis.kreiraj(TRENER, zahtjev(SADA, 60, 10)));
        verify(terminRepository, never()).save(any());
    }

    @Test
    void preklapanjeSaDrugimTerminomDaje409() throws Exception {
        Termin postojeci = postaviId(new Termin(SUTRA_18H, 60, trener,
                postaviId(new Grupa("Snaga", 10, trener), 51)), 71);
        when(terminRepository.terminiTreneraIzmedju(eq(TRENER), any(), any())).thenReturn(List.of(postojeci));

        assertThrows(KonfliktException.class,
                () -> servis.kreiraj(TRENER, zahtjev(SUTRA_18H.plusMinutes(30), 60, 10)));
        verify(grupaRepository, never()).save(any());
    }

    @Test
    void terminKojiPocinjeKadPrethodniZavrsiJeDozvoljen() throws Exception {
        Termin postojeci = postaviId(new Termin(SUTRA_18H, 60, trener,
                postaviId(new Grupa("Snaga", 10, trener), 51)), 71);
        when(terminRepository.terminiTreneraIzmedju(eq(TRENER), any(), any())).thenReturn(List.of(postojeci));

        assertEquals(70, servis.kreiraj(TRENER, zahtjev(SUTRA_18H.plusMinutes(60), 60, 10)).sessionId());
    }

    // ------------------------------------------------------------ 10.4 raspored

    @Test
    void rasporedBezDatumaJeZaDanasIBrojiClanove() throws Exception {
        Termin t = termin(trener, LocalDateTime.of(2026, 10, 29, 18, 0), 75, 12);
        LocalDate danas = LocalDate.of(2026, 10, 29);
        when(terminRepository.terminiTreneraIzmedju(TRENER, danas.atStartOfDay(), danas.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(t));
        when(clanUGrupiRepository.brojClanova(50)).thenReturn(4L);

        List<TerminRasporedDto> raspored = servis.raspored(TRENER, null);

        assertEquals(1, raspored.size());
        assertEquals(70, raspored.get(0).sessionId());
        assertEquals("Joga", raspored.get(0).name());
        assertEquals(4, raspored.get(0).enrolledCount());
        assertEquals(12, raspored.get(0).maxMembers());
        assertEquals(75, raspored.get(0).durationMinutes());
    }

    // ------------------------------------------------------------ 10.6 izmjena

    @Test
    void nepostojeciTerminDaje404ITudjiDaje403() throws Exception {
        when(terminRepository.saGrupom(99)).thenReturn(Optional.empty());
        assertThrows(ResursNijePronadjenException.class, () -> servis.izmijeni(TRENER, 99, zahtjev(SUTRA_18H, 60, 10)));
        assertThrows(ResursNijePronadjenException.class, () -> servis.obrisi(TRENER, 99));

        termin(trener(DRUGI_TRENER), SUTRA_18H, 60, 10);
        assertStatus(403, () -> servis.izmijeni(TRENER, 70, zahtjev(SUTRA_18H, 60, 10)));
        assertStatus(403, () -> servis.obrisi(TRENER, 70));
    }

    @Test
    void terminKojiJePoceoSeNeMozeMijenjati() throws Exception {
        termin(trener, SADA.minusMinutes(10), 60, 10);
        assertStatus(400, () -> servis.izmijeni(TRENER, 70, zahtjev(SUTRA_18H, 60, 10)));
    }

    @Test
    void kapacitetManjiOdBrojaClanovaDaje409() throws Exception {
        Termin t = termin(trener, SUTRA_18H, 60, 10);
        when(clanUGrupiRepository.brojClanova(50)).thenReturn(3L);

        assertThrows(KonfliktException.class, () -> servis.izmijeni(TRENER, 70, zahtjev(SUTRA_18H, 60, 2)));
        assertEquals(10, t.getGrupa().getMaxClanova());

        servis.izmijeni(TRENER, 70, zahtjev(SUTRA_18H, 60, 3));       // tačno koliko ima članova
        assertEquals(3, t.getGrupa().getMaxClanova());
    }

    @Test
    void izmjenaSeNePreklapaSaSamomSobom() throws Exception {
        Termin t = termin(trener, SUTRA_18H, 60, 10);
        when(terminRepository.terminiTreneraIzmedju(eq(TRENER), any(), any())).thenReturn(List.of(t));

        servis.izmijeni(TRENER, 70, zahtjev(SUTRA_18H.plusMinutes(30), 60, 10));

        assertEquals(SUTRA_18H.plusMinutes(30), t.getVrijeme());
    }

    @Test
    void pomjerenTerminObavjestavaClanoveGrupe() throws Exception {
        termin(trener, SUTRA_18H, 60, 10);
        when(clanUGrupiRepository.idClanova(50)).thenReturn(List.of(6, 7));

        servis.izmijeni(TRENER, 70, zahtjev(SUTRA_18H.plusHours(1), 60, 10));

        verify(obavjestenjeService).posalji(eq(6), eq(TRENER), contains("pomjeren"));
        verify(obavjestenjeService).posalji(eq(7), eq(TRENER), contains("pomjeren"));
    }

    @Test
    void promjenaSamoNazivaIKapacitetaNeSaljeObavjestenje() throws Exception {
        Termin t = termin(trener, SUTRA_18H, 60, 10);

        servis.izmijeni(TRENER, 70, new TerminZahtjev("Joga napredna", SUTRA_18H, 60, 15));

        assertEquals("Joga napredna", t.getGrupa().getNaziv());
        verify(obavjestenjeService, never()).posalji(anyInt(), anyInt(), anyString());
    }

    // ------------------------------------------------------------ 10.7 brisanje

    @Test
    void brisanjeObavjestavaClanoveIOneKojiCekajuPaBriseSve() throws Exception {
        Termin t = termin(trener, SUTRA_18H, 60, 10);
        when(clanUGrupiRepository.idClanova(50)).thenReturn(List.of(6, 7));
        when(prijavaRepository.idClanova(70, EnumSet.of(StatusPrijave.NA_CEKANJU))).thenReturn(List.of(8, 7));

        servis.obrisi(TRENER, 70);

        for (int idClana : List.of(6, 7, 8)) {
            verify(obavjestenjeService).posalji(eq(idClana), eq(TRENER), contains("otkazan"));
        }
        verify(prijavaRepository).obrisiSveZaTermin(70);
        verify(clanUGrupiRepository).obrisiSveIzGrupe(50);
        verify(terminRepository).delete(t);
        verify(grupaRepository).delete(t.getGrupa());
    }

    @Test
    void terminKojiJePoceoSeNeBrise() throws Exception {
        termin(trener, SADA.minusDays(7), 90, 15);

        assertThrows(KonfliktException.class, () -> servis.obrisi(TRENER, 70));
        verify(terminRepository, never()).delete(any());
        verify(obavjestenjeService, never()).posalji(anyInt(), anyInt(), anyString());
    }
}
