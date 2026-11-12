package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.dto.RecenzijaDto;
import org.etf.pumpd.dto.RecenzijaOdgovor;
import org.etf.pumpd.dto.RecenzijaZahtjev;
import org.etf.pumpd.dto.RecenzijeTreneraDto;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Recenzija;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.RecenzijaRepository;
import org.etf.pumpd.repository.TrenerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class RecenzijeServiceTest {

    /** "Danas" prema fiksiranom satu iz TestPomocni. */
    private static final LocalDate DANAS = LocalDate.of(2026, 10, 29);
    private static final int TRENER_ID = 3;
    private static final int CLAN_ID = 6;

    private RecenzijaRepository recenzijaRepository;
    private TrenerRepository trenerRepository;
    private ClanRepository clanRepository;
    private ObavjestenjeService obavjestenjeService;
    private RecenzijeService servis;

    private Trener trener;
    private Clan clan;

    @BeforeEach
    void postavi() {
        recenzijaRepository = mock(RecenzijaRepository.class);
        trenerRepository = mock(TrenerRepository.class);
        clanRepository = mock(ClanRepository.class);
        obavjestenjeService = mock(ObavjestenjeService.class);
        servis = new RecenzijeService(recenzijaRepository, trenerRepository, clanRepository, obavjestenjeService, SAT);

        Nalog nalogTrenera = new Nalog("Marko", "Petrović", "trener1@pumpd.local", "hash");
        nalogTrenera.setId(TRENER_ID);
        trener = new Trener(nalogTrenera, null, null, null);
        Nalog nalogClana = new Nalog("Amar", "Hodžić", "clan1@pumpd.local", "hash");
        nalogClana.setId(CLAN_ID);
        clan = new Clan(nalogClana, null, null, null, null);

        when(trenerRepository.pronadjiAktivnog(TRENER_ID)).thenReturn(Optional.of(trener));
        when(clanRepository.pronadjiAktivnog(CLAN_ID)).thenReturn(Optional.of(clan));
        when(recenzijaRepository.saveAndFlush(any(Recenzija.class))).thenAnswer(poziv -> {
            Recenzija r = poziv.getArgument(0);
            if (r.getId() == null) {
                r.setId(21);
            }
            return r;
        });
    }

    private void nemaRecenzije() {
        when(recenzijaRepository.findByTrenerIdAndClanId(TRENER_ID, CLAN_ID)).thenReturn(Optional.empty());
    }

    private Recenzija sacuvana() {
        ArgumentCaptor<Recenzija> hvatac = ArgumentCaptor.forClass(Recenzija.class);
        verify(recenzijaRepository).saveAndFlush(hvatac.capture());
        return hvatac.getValue();
    }

    // ------------------------------------------------------------ 7.5 nova recenzija

    @Test
    void novaRecenzijaSeUpisujeIVracaNoviProsjek() {
        nemaRecenzije();
        when(recenzijaRepository.prosjecnaOcjena(TRENER_ID)).thenReturn(4.3333);

        RecenzijaOdgovor odgovor = servis.ostaviRecenziju(TRENER_ID, CLAN_ID, new RecenzijaZahtjev(5, "  Odličan.  "));

        assertEquals(new RecenzijaOdgovor(21, 4.3, "Recenzija je sacuvana."), odgovor);
        Recenzija r = sacuvana();
        assertEquals(5, r.getOcjena());
        assertEquals("Odličan.", r.getKomentar());
        assertEquals(DANAS, r.getDatum());
        assertSame(trener, r.getTrener());
        assertSame(clan, r.getClan());
        verify(obavjestenjeService).posalji(TRENER_ID, CLAN_ID, "Amar Hodžić vam je ostavio recenziju (ocjena: 5).");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void prazanKomentarSeCuvaKaoNull(String komentar) {
        nemaRecenzije();
        when(recenzijaRepository.prosjecnaOcjena(TRENER_ID)).thenReturn(4.0);

        servis.ostaviRecenziju(TRENER_ID, CLAN_ID, new RecenzijaZahtjev(4, komentar));

        assertNull(sacuvana().getKomentar());
    }

    // ------------------------------------------------------------ 7.5 jedna recenzija po treneru (odluka A)

    @Test
    void ponovnaRecenzijaZamjenjujeStaruIZadrzavaId() {
        Recenzija stara = new Recenzija(trener, clan, 2, "Loše.", LocalDate.of(2026, 9, 1));
        stara.setId(8);
        when(recenzijaRepository.findByTrenerIdAndClanId(TRENER_ID, CLAN_ID)).thenReturn(Optional.of(stara));
        when(recenzijaRepository.prosjecnaOcjena(TRENER_ID)).thenReturn(5.0);

        RecenzijaOdgovor odgovor = servis.ostaviRecenziju(TRENER_ID, CLAN_ID, new RecenzijaZahtjev(5, "Ipak odlično."));

        assertEquals(new RecenzijaOdgovor(8, 5.0, "Recenzija je azurirana."), odgovor);
        assertSame(stara, sacuvana());
        assertEquals(5, stara.getOcjena());
        assertEquals("Ipak odlično.", stara.getKomentar());
        assertEquals(DANAS, stara.getDatum());
        verify(obavjestenjeService).posalji(TRENER_ID, CLAN_ID, "Amar Hodžić je izmijenio recenziju (ocjena: 5).");
    }

    @Test
    void dvostrukiKlikBaca409() {
        nemaRecenzije();
        when(recenzijaRepository.saveAndFlush(any(Recenzija.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'recenzija_trener_clan_UNIQUE'"));

        assertThrows(KonfliktException.class,
                () -> servis.ostaviRecenziju(TRENER_ID, CLAN_ID, new RecenzijaZahtjev(5, null)));
        verifyNoInteractions(obavjestenjeService);
    }

    @Test
    void nepostojeciIliObrisanTrenerBaca404() {
        when(trenerRepository.pronadjiAktivnog(99)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class,
                () -> servis.ostaviRecenziju(99, CLAN_ID, new RecenzijaZahtjev(5, null)));
        verify(recenzijaRepository, never()).saveAndFlush(any());
        verifyNoInteractions(obavjestenjeService);
    }

    // ------------------------------------------------------------ 7.6 trenerov pregled

    @Test
    void recenzijeTreneraSaProsjekom() {
        Recenzija r = new Recenzija(trener, clan, 5, "Odličan.", LocalDate.of(2026, 11, 10));
        r.setId(21);
        when(recenzijaRepository.prosjecnaOcjena(TRENER_ID)).thenReturn(4.6666);
        when(recenzijaRepository.recenzijeTrenera(TRENER_ID)).thenReturn(List.of(r));

        assertEquals(new RecenzijeTreneraDto(4.7,
                List.of(new RecenzijaDto(21, "Amar Hodžić", 5, "Odličan.", LocalDate.of(2026, 11, 10)))),
                servis.recenzijeTrenera(TRENER_ID));
    }

    @Test
    void bezRecenzijaProsjekNullIPraznaLista() {
        when(recenzijaRepository.prosjecnaOcjena(TRENER_ID)).thenReturn(null);
        when(recenzijaRepository.recenzijeTrenera(TRENER_ID)).thenReturn(List.of());

        RecenzijeTreneraDto dto = servis.recenzijeTrenera(TRENER_ID);

        assertNull(dto.averageGrade());
        assertTrue(dto.reviews().isEmpty());
    }
}
