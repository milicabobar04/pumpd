package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.dto.PratilacDto;
import org.etf.pumpd.dto.ZahtjevZaPracenjeDto;
import org.etf.pumpd.dto.ZahtjevZaPracenjeOdgovor;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Pracenje;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.enums.OdlukaOZahtjevu;
import org.etf.pumpd.enums.StatusPracenja;
import org.etf.pumpd.enums.StatusPracenjaClana;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.PracenjeRepository;
import org.etf.pumpd.repository.TrenerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class PracenjeServiceTest {

    /** "Danas" prema fiksiranom satu iz TestPomocni. */
    private static final LocalDate DANAS = LocalDate.of(2026, 10, 29);
    private static final int TRENER_ID = 3;
    private static final int CLAN_ID = 6;

    private PracenjeRepository pracenjeRepository;
    private TrenerRepository trenerRepository;
    private ClanRepository clanRepository;
    private ObavjestenjeService obavjestenjeService;
    private PracenjeService servis;

    private Trener trener;
    private Clan clan;

    @BeforeEach
    void postavi() {
        pracenjeRepository = mock(PracenjeRepository.class);
        trenerRepository = mock(TrenerRepository.class);
        clanRepository = mock(ClanRepository.class);
        obavjestenjeService = mock(ObavjestenjeService.class);
        servis = new PracenjeService(pracenjeRepository, trenerRepository, clanRepository, obavjestenjeService, SAT);

        Nalog nalogTrenera = new Nalog("Marko", "Petrović", "trener1@pumpd.local", "hash");
        nalogTrenera.setId(TRENER_ID);
        trener = new Trener(nalogTrenera, null, null, null);
        Nalog nalogClana = new Nalog("Amar", "Hodžić", "clan1@pumpd.local", "hash");
        nalogClana.setId(CLAN_ID);
        clan = new Clan(nalogClana, null, null, null, null);

        when(trenerRepository.pronadjiAktivnog(TRENER_ID)).thenReturn(Optional.of(trener));
        when(clanRepository.pronadjiAktivnog(CLAN_ID)).thenReturn(Optional.of(clan));
    }

    private Pracenje postojece(StatusPracenja status) {
        Pracenje p = new Pracenje(trener, clan, LocalDate.of(2026, 10, 1), status);
        p.setId(15);
        when(pracenjeRepository.findByTrenerIdAndClanId(TRENER_ID, CLAN_ID)).thenReturn(Optional.of(p));
        return p;
    }

    private void nemaReda() {
        when(pracenjeRepository.findByTrenerIdAndClanId(TRENER_ID, CLAN_ID)).thenReturn(Optional.empty());
    }

    // ------------------------------------------------------------ 7.3 zahtjev za praćenje

    @Test
    void noviZahtjevSeUpisujeIObavjestavaTrenera() {
        nemaReda();
        when(pracenjeRepository.saveAndFlush(any(Pracenje.class))).thenAnswer(poziv -> {
            Pracenje p = poziv.getArgument(0);
            p.setId(15);
            return p;
        });

        ZahtjevZaPracenjeOdgovor odgovor = servis.posaljiZahtjev(TRENER_ID, CLAN_ID);

        assertEquals(new ZahtjevZaPracenjeOdgovor(15, StatusPracenjaClana.PENDING, "Zahtjev za pracenje je poslat."),
                odgovor);
        ArgumentCaptor<Pracenje> upisano = ArgumentCaptor.forClass(Pracenje.class);
        verify(pracenjeRepository).saveAndFlush(upisano.capture());
        assertEquals(StatusPracenja.PENDING, upisano.getValue().getStatus());
        assertEquals(DANAS, upisano.getValue().getDatum());
        assertSame(trener, upisano.getValue().getTrener());
        assertSame(clan, upisano.getValue().getClan());
        verify(obavjestenjeService).posalji(TRENER_ID, CLAN_ID, "Amar Hodžić vam je poslao zahtjev za praćenje.");
    }

    @Test
    void ponovniZahtjevNakonOdbijanjaKoristiIstiRed() {
        Pracenje odbijeno = postojece(StatusPracenja.REJECTED);

        ZahtjevZaPracenjeOdgovor odgovor = servis.posaljiZahtjev(TRENER_ID, CLAN_ID);

        assertEquals(15, odgovor.followRequestId());
        assertEquals(StatusPracenja.PENDING, odbijeno.getStatus());
        assertEquals(DANAS, odbijeno.getDatum());
        verify(pracenjeRepository, never()).saveAndFlush(any());
        verify(obavjestenjeService).posalji(TRENER_ID, CLAN_ID, "Amar Hodžić vam je poslao zahtjev za praćenje.");
    }

    @Test
    void zahtjevKojiVecCekaBaca409() {
        postojece(StatusPracenja.PENDING);

        KonfliktException e = assertThrows(KonfliktException.class, () -> servis.posaljiZahtjev(TRENER_ID, CLAN_ID));

        assertEquals("Zahtjev za pracenje je vec poslat.", e.getMessage());
        verify(pracenjeRepository, never()).saveAndFlush(any());
        verifyNoInteractions(obavjestenjeService);
    }

    @Test
    void postojecePracenjeBaca409() {
        postojece(StatusPracenja.ACCEPTED);

        KonfliktException e = assertThrows(KonfliktException.class, () -> servis.posaljiZahtjev(TRENER_ID, CLAN_ID));

        assertEquals("Vec pratite ovog trenera.", e.getMessage());
        verifyNoInteractions(obavjestenjeService);
    }

    @Test
    void dvostrukiKlikBaca409() {
        nemaReda();
        when(pracenjeRepository.saveAndFlush(any(Pracenje.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'PRIMARY'"));

        assertThrows(KonfliktException.class, () -> servis.posaljiZahtjev(TRENER_ID, CLAN_ID));
        verifyNoInteractions(obavjestenjeService);
    }

    @Test
    void nepostojeciIliObrisanTrenerBaca404() {
        when(trenerRepository.pronadjiAktivnog(99)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.posaljiZahtjev(99, CLAN_ID));
        verify(pracenjeRepository, never()).saveAndFlush(any());
        verify(obavjestenjeService, never()).posalji(anyInt(), anyInt(), anyString());
    }

    // ------------------------------------------------------------ 7.4 prekid i povlačenje

    @Test
    void prekidPracenjaBriseRed() {
        Pracenje aktivno = postojece(StatusPracenja.ACCEPTED);

        assertEquals("Pracenje je prekinuto.", servis.prekini(TRENER_ID, CLAN_ID).message());
        verify(pracenjeRepository).delete(aktivno);
    }

    @Test
    void povlacenjeZahtjevaKojiCekaBriseRed() {
        Pracenje naCekanju = postojece(StatusPracenja.PENDING);

        assertEquals("Zahtjev za pracenje je povucen.", servis.prekini(TRENER_ID, CLAN_ID).message());
        verify(pracenjeRepository).delete(naCekanju);
    }

    @ParameterizedTest
    @EnumSource(value = StatusPracenja.class, names = "REJECTED")
    void odbijenZahtjevSeNeBriseI404(StatusPracenja status) {
        postojece(status);

        assertThrows(ResursNijePronadjenException.class, () -> servis.prekini(TRENER_ID, CLAN_ID));
        verify(pracenjeRepository, never()).delete(any());
    }

    @Test
    void bezRedaPrekidBaca404() {
        nemaReda();

        ResursNijePronadjenException e =
                assertThrows(ResursNijePronadjenException.class, () -> servis.prekini(TRENER_ID, CLAN_ID));
        assertEquals("Ne pratite ovog trenera.", e.getMessage());
    }

    @Test
    void prekidNeProvjeravaDaLiJeTrenerAktivan() {
        Pracenje aktivno = postojece(StatusPracenja.ACCEPTED);
        when(trenerRepository.pronadjiAktivnog(TRENER_ID)).thenReturn(Optional.empty());

        servis.prekini(TRENER_ID, CLAN_ID);

        verify(pracenjeRepository).delete(aktivno);
    }

    // ------------------------------------------------------------ 10.1 zahtjevi kod trenera

    @Test
    void zahtjeviZaTreneraSaImenomClanaIDatumom() {
        Pracenje p = new Pracenje(trener, clan, LocalDate.of(2026, 10, 20), StatusPracenja.PENDING);
        p.setId(15);
        when(pracenjeRepository.zahtjeviTrenera(TRENER_ID, StatusPracenja.PENDING)).thenReturn(List.of(p));

        assertEquals(List.of(new ZahtjevZaPracenjeDto(15, CLAN_ID, "Amar Hodžić", LocalDate.of(2026, 10, 20))),
                servis.zahtjeviZaTrenera(TRENER_ID));
    }

    // ------------------------------------------------------------ 10.2 obrada zahtjeva

    private Pracenje zahtjevPoId(StatusPracenja status) {
        Pracenje p = new Pracenje(trener, clan, LocalDate.of(2026, 10, 20), status);
        p.setId(15);
        when(pracenjeRepository.findById(15)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void prihvatanjeMijenjaStatusIObavjestavaClana() {
        Pracenje p = zahtjevPoId(StatusPracenja.PENDING);

        assertEquals("Zahtjev je prihvacen.", servis.obradiZahtjev(15, OdlukaOZahtjevu.ACCEPTED, TRENER_ID).message());

        assertEquals(StatusPracenja.ACCEPTED, p.getStatus());
        verify(obavjestenjeService).posalji(CLAN_ID, TRENER_ID, "Marko Petrović je prihvatio vaš zahtjev za praćenje.");
    }

    @Test
    void odbijanjeMijenjaStatusIObavjestavaClana() {
        Pracenje p = zahtjevPoId(StatusPracenja.PENDING);

        assertEquals("Zahtjev je odbijen.", servis.obradiZahtjev(15, OdlukaOZahtjevu.REJECTED, TRENER_ID).message());

        assertEquals(StatusPracenja.REJECTED, p.getStatus());
        verify(pracenjeRepository, never()).delete(any());
        verify(obavjestenjeService).posalji(CLAN_ID, TRENER_ID, "Marko Petrović je odbio vaš zahtjev za praćenje.");
    }

    @Test
    void nepostojeciZahtjevBaca404() {
        when(pracenjeRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class,
                () -> servis.obradiZahtjev(99, OdlukaOZahtjevu.ACCEPTED, TRENER_ID));
    }

    @Test
    void zahtjevDrugogTreneraBaca404IOstajeNetaknut() {
        Pracenje p = zahtjevPoId(StatusPracenja.PENDING);

        ResursNijePronadjenException e = assertThrows(ResursNijePronadjenException.class,
                () -> servis.obradiZahtjev(15, OdlukaOZahtjevu.ACCEPTED, 4));

        assertEquals("Zahtjev ne postoji.", e.getMessage());
        assertEquals(StatusPracenja.PENDING, p.getStatus());
        verifyNoInteractions(obavjestenjeService);
    }

    @Test
    void zahtjevObrisanogClanaBaca404() {
        zahtjevPoId(StatusPracenja.PENDING);
        clan.getNalog().deaktiviraj();

        assertThrows(ResursNijePronadjenException.class,
                () -> servis.obradiZahtjev(15, OdlukaOZahtjevu.ACCEPTED, TRENER_ID));
        verifyNoInteractions(obavjestenjeService);
    }

    @ParameterizedTest
    @EnumSource(value = StatusPracenja.class, names = {"ACCEPTED", "REJECTED"})
    void vecObradjenZahtjevBaca409(StatusPracenja status) {
        Pracenje p = zahtjevPoId(status);

        KonfliktException e = assertThrows(KonfliktException.class,
                () -> servis.obradiZahtjev(15, OdlukaOZahtjevu.ACCEPTED, TRENER_ID));

        assertEquals("Zahtjev je vec obradjen.", e.getMessage());
        assertEquals(status, p.getStatus());
        verifyNoInteractions(obavjestenjeService);
    }

    // ------------------------------------------------------------ 10.3 pratioci

    @Test
    void pratiociTrenera() {
        when(pracenjeRepository.pratiociTrenera(TRENER_ID, StatusPracenja.ACCEPTED)).thenReturn(List.of(
                new Pracenje(trener, clan, LocalDate.of(2026, 10, 20), StatusPracenja.ACCEPTED)));

        assertEquals(List.of(new PratilacDto(CLAN_ID, "Amar", "Hodžić")), servis.pratioci(TRENER_ID));
    }
}
