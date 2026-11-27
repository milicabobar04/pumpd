package org.etf.pumpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.dto.ZahtjevZaGrupniTreningDto;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.ClanNaTerminu;
import org.etf.pumpd.entity.ClanNaTerminuId;
import org.etf.pumpd.entity.Grupa;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Termin;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.enums.OdlukaOZahtjevu;
import org.etf.pumpd.enums.StatusPrijave;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanNaTerminuRepository;
import org.etf.pumpd.repository.ClanUGrupiRepository;
import org.etf.pumpd.repository.GrupaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataIntegrityViolationException;

/** Jedinični test 10.12 i 10.13 (ZahtjeviZaGrupeService) sa lažnim repozitorijumima. */
class ZahtjeviZaGrupeServiceTest {

    private static final ZoneId ZONA = ZoneId.of("Europe/Sarajevo");
    private static final LocalDateTime SADA = LocalDateTime.of(2026, 11, 27, 10, 0);
    private static final int ID_TRENERA = 5;
    private static final int ID_CLANA = 40;
    private static final int ID_ZAHTJEVA = 900;
    private static final int ID_GRUPE = 11;

    private ClanNaTerminuRepository prijavaRepository;
    private ClanUGrupiRepository clanUGrupiRepository;
    private GrupaRepository grupaRepository;
    private ObavjestenjeService obavjestenjeService;
    private ZahtjeviZaGrupeService servis;

    @BeforeEach
    void pripremi() {
        prijavaRepository = mock(ClanNaTerminuRepository.class);
        clanUGrupiRepository = mock(ClanUGrupiRepository.class);
        grupaRepository = mock(GrupaRepository.class);
        obavjestenjeService = mock(ObavjestenjeService.class);
        servis = new ZahtjeviZaGrupeService(prijavaRepository, clanUGrupiRepository, grupaRepository,
                obavjestenjeService, Clock.fixed(SADA.atZone(ZONA).toInstant(), ZONA));
    }

    private static Grupa grupa(int id, int kapacitet) {
        Grupa grupa = mock(Grupa.class);
        when(grupa.getId()).thenReturn(id);
        when(grupa.getNaziv()).thenReturn("Joga");
        when(grupa.getMaxClanova()).thenReturn(kapacitet);
        return grupa;
    }

    /** Zahtjev člana ID_CLANA za termin trenera ID_TRENERA (grupa ID_GRUPE) koji počinje u datom trenutku. */
    private static ClanNaTerminu zahtjev(int idZahtjeva, int idTermina, Grupa grupa, LocalDateTime pocetak) {
        Trener trener = mock(Trener.class);
        when(trener.getId()).thenReturn(ID_TRENERA);
        Termin termin = mock(Termin.class);
        when(termin.getId()).thenReturn(idTermina);
        when(termin.getTrener()).thenReturn(trener);
        when(termin.getGrupa()).thenReturn(grupa);
        when(termin.getVrijeme()).thenReturn(pocetak);
        when(termin.jePoceo(any())).thenAnswer(poziv -> !pocetak.isAfter(poziv.getArgument(0)));
        Nalog nalog = mock(Nalog.class);
        when(nalog.getIme()).thenReturn("Mila");
        when(nalog.getPrezime()).thenReturn("Milic");
        when(nalog.isAktivan()).thenReturn(true);
        Clan clan = mock(Clan.class);
        when(clan.getNalog()).thenReturn(nalog);
        ClanNaTerminu zahtjev = mock(ClanNaTerminu.class);
        when(zahtjev.getId()).thenReturn(new ClanNaTerminuId(idTermina, ID_CLANA));
        when(zahtjev.getIdZahtjeva()).thenReturn(idZahtjeva);
        when(zahtjev.getTermin()).thenReturn(termin);
        when(zahtjev.getClan()).thenReturn(clan);
        when(zahtjev.getDatumZahtjeva()).thenReturn(LocalDate.of(2026, 11, 25));
        return zahtjev;
    }

    /** Zahtjev ID_ZAHTJEVA na čekanju za termin sutra, u grupi sa kapacitetom i trenutnim brojem članova. */
    private ClanNaTerminu zahtjevNaCekanju(int kapacitet, long clanova) {
        Grupa grupa = grupa(ID_GRUPE, kapacitet);
        ClanNaTerminu zahtjev = zahtjev(ID_ZAHTJEVA, 1, grupa, SADA.plusDays(1));
        when(prijavaRepository.findByIdZahtjeva(ID_ZAHTJEVA)).thenReturn(Optional.of(zahtjev));
        when(grupaRepository.zakljucaj(ID_GRUPE)).thenReturn(Optional.of(grupa));
        when(prijavaRepository.statusZahtjeva(ID_ZAHTJEVA)).thenReturn(Optional.of(StatusPrijave.NA_CEKANJU));
        when(clanUGrupiRepository.brojClanova(ID_GRUPE)).thenReturn(clanova);
        return zahtjev;
    }

    // ------------------------------------------------------------ 10.12

    @Test
    void zahtjeviSaBrojemClanovaPoGrupi() {
        List<ClanNaTerminu> nadjeni = List.of(zahtjev(901, 1, grupa(11, 10), SADA.plusDays(1)),
                zahtjev(902, 2, grupa(12, 8), SADA.plusDays(2)));
        when(prijavaRepository.zahtjeviZaTrenera(ID_TRENERA, StatusPrijave.NA_CEKANJU, SADA, null)).thenReturn(nadjeni);
        when(clanUGrupiRepository.brojClanovaPoGrupama(List.of(11, 12))).thenReturn(List.<Object[]>of(new Object[] {11, 3L}));

        List<ZahtjevZaGrupniTreningDto> zahtjevi = servis.zahtjevi(ID_TRENERA, null);

        assertEquals(new ZahtjevZaGrupniTreningDto(901, ID_CLANA, "Mila Milic", 1, "Joga", SADA.plusDays(1), 3, 10,
                LocalDate.of(2026, 11, 25)), zahtjevi.get(0));
        assertEquals(0, zahtjevi.get(1).enrolledCount());
        assertEquals(8, zahtjevi.get(1).maxMembers());
    }

    @Test
    void filterPoTerminuIPraznaLista() {
        when(prijavaRepository.zahtjeviZaTrenera(ID_TRENERA, StatusPrijave.NA_CEKANJU, SADA, 7)).thenReturn(List.of());

        assertEquals(List.of(), servis.zahtjevi(ID_TRENERA, 7));
        verifyNoInteractions(clanUGrupiRepository);
    }

    // ------------------------------------------------------------ 10.13

    @Test
    void prihvatanjeUpisujeUGrupuIObavjestavaClana() {
        ClanNaTerminu zahtjev = zahtjevNaCekanju(10, 9);   // posljednje mjesto

        assertEquals("Zahtjev je prihvacen.", servis.obradi(ID_TRENERA, ID_ZAHTJEVA, OdlukaOZahtjevu.ACCEPTED).message());

        verify(clanUGrupiRepository).saveAndFlush(any());
        verify(zahtjev).postaviStatus(StatusPrijave.PRIJAVLJEN);
        verify(obavjestenjeService).posalji(ID_CLANA, ID_TRENERA,
                "Trener je prihvatio vas zahtjev za prijavu na termin \"Joga\" (28.11.2026. u 10:00).");
    }

    @Test
    void odbijanjeMijenjaStatusBezUpisaUGrupu() {
        ClanNaTerminu zahtjev = zahtjevNaCekanju(10, 10);   // ni pun termin ne smeta odbijanju

        assertEquals("Zahtjev je odbijen.", servis.obradi(ID_TRENERA, ID_ZAHTJEVA, OdlukaOZahtjevu.REJECTED).message());

        verify(zahtjev).postaviStatus(StatusPrijave.ODBIJEN);
        verify(clanUGrupiRepository, never()).saveAndFlush(any());
        verify(obavjestenjeService).posalji(ID_CLANA, ID_TRENERA,
                "Trener je odbio vas zahtjev za prijavu na termin \"Joga\" (28.11.2026. u 10:00).");
    }

    @Test
    void punaGrupaJe409() {
        ClanNaTerminu zahtjev = zahtjevNaCekanju(10, 10);

        KonfliktException e = assertThrows(KonfliktException.class,
                () -> servis.obradi(ID_TRENERA, ID_ZAHTJEVA, OdlukaOZahtjevu.ACCEPTED));
        assertEquals("Grupa je popunjena.", e.getMessage());
        verify(zahtjev, never()).postaviStatus(any());
        verify(obavjestenjeService, never()).posalji(anyInt(), anyInt(), anyString());
    }

    @Test
    void triggerUBaziJe409() {
        ClanNaTerminu zahtjev = zahtjevNaCekanju(10, 3);
        when(clanUGrupiRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Grupa je puna"));

        KonfliktException e = assertThrows(KonfliktException.class,
                () -> servis.obradi(ID_TRENERA, ID_ZAHTJEVA, OdlukaOZahtjevu.ACCEPTED));
        assertEquals("Grupa je popunjena.", e.getMessage());
        verify(zahtjev, never()).postaviStatus(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPrijave.class, names = "NA_CEKANJU", mode = EnumSource.Mode.EXCLUDE)
    void obradjenZahtjevJe409(StatusPrijave status) {
        ClanNaTerminu zahtjev = zahtjevNaCekanju(10, 0);
        when(prijavaRepository.statusZahtjeva(ID_ZAHTJEVA)).thenReturn(Optional.of(status));

        for (OdlukaOZahtjevu odluka : OdlukaOZahtjevu.values()) {
            KonfliktException e = assertThrows(KonfliktException.class, () -> servis.obradi(ID_TRENERA, ID_ZAHTJEVA, odluka));
            assertEquals("Zahtjev je vec obradjen.", e.getMessage());
        }
        verify(zahtjev, never()).postaviStatus(any());
    }

    @Test
    void zahtjevPovucenUMedjuvremenuJe404() {
        zahtjevNaCekanju(10, 0);
        when(prijavaRepository.statusZahtjeva(ID_ZAHTJEVA)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class,
                () -> servis.obradi(ID_TRENERA, ID_ZAHTJEVA, OdlukaOZahtjevu.ACCEPTED));
    }

    @Test
    void nepostojeciTudjIliZahtjevObrisanogClanaJe404() {
        when(prijavaRepository.findByIdZahtjeva(ID_ZAHTJEVA)).thenReturn(Optional.empty());
        ResursNijePronadjenException e = assertThrows(ResursNijePronadjenException.class,
                () -> servis.obradi(ID_TRENERA, ID_ZAHTJEVA, OdlukaOZahtjevu.ACCEPTED));
        assertEquals("Zahtjev ne postoji.", e.getMessage());

        ClanNaTerminu zahtjev = zahtjevNaCekanju(10, 0);
        assertThrows(ResursNijePronadjenException.class,
                () -> servis.obradi(ID_TRENERA + 1, ID_ZAHTJEVA, OdlukaOZahtjevu.ACCEPTED));

        when(zahtjev.getClan().getNalog().isAktivan()).thenReturn(false);
        assertThrows(ResursNijePronadjenException.class,
                () -> servis.obradi(ID_TRENERA, ID_ZAHTJEVA, OdlukaOZahtjevu.REJECTED));
        verify(grupaRepository, never()).zakljucaj(anyInt());
    }

    @Test
    void terminKojiJePoceoJe409() {
        Grupa grupa = grupa(ID_GRUPE, 10);
        ClanNaTerminu zahtjev = zahtjev(ID_ZAHTJEVA, 1, grupa, SADA);
        when(prijavaRepository.findByIdZahtjeva(ID_ZAHTJEVA)).thenReturn(Optional.of(zahtjev));
        when(grupaRepository.zakljucaj(ID_GRUPE)).thenReturn(Optional.of(grupa));
        when(prijavaRepository.statusZahtjeva(ID_ZAHTJEVA)).thenReturn(Optional.of(StatusPrijave.NA_CEKANJU));

        KonfliktException e = assertThrows(KonfliktException.class,
                () -> servis.obradi(ID_TRENERA, ID_ZAHTJEVA, OdlukaOZahtjevu.REJECTED));
        assertEquals("Termin je vec poceo.", e.getMessage());
        verify(zahtjev, never()).postaviStatus(any());
    }
}
