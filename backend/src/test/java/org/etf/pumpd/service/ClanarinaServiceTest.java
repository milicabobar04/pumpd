package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
import static org.etf.pumpd.service.TestPomocni.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.etf.pumpd.dto.ClanarinaClanaDto;
import org.etf.pumpd.dto.ClanarinaDto;
import org.etf.pumpd.dto.IzmjenaClanarineZahtjev;
import org.etf.pumpd.dto.ProduzenjeClanarineOdgovor;
import org.etf.pumpd.dto.ProduzenjeClanarineZahtjev;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Clanarina;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ClanarinaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ClanarinaServiceTest {

    /** "Danas" prema fiksiranom satu iz TestPomocni. */
    private static final LocalDate DANAS = LocalDate.of(2026, 10, 29);
    private static final int ID = 7;

    private ClanRepository clanRepository;
    private ClanarinaRepository clanarinaRepository;
    private ClanarinaService servis;

    @BeforeEach
    void postavi() {
        clanRepository = mock(ClanRepository.class);
        clanarinaRepository = mock(ClanarinaRepository.class);
        servis = new ClanarinaService(clanarinaRepository, clanRepository, new QRKodGenerator(), SAT);
    }

    private void postojiClan() {
        Nalog nalog = new Nalog("Ana", "Anic", "ana@pumpd.local", "hash");
        nalog.setId(ID);
        Clan clan = new Clan(nalog, null, null, null, null);
        clan.setQrKod("qr-ana");
        when(clanRepository.pronadjiAktivnog(ID)).thenReturn(Optional.of(clan));
    }

    private Clanarina postojiClanarinaDo(LocalDate datumIsteka) {
        Clanarina c = new Clanarina(ID, 40.0, datumIsteka.minusMonths(1), datumIsteka);
        when(clanarinaRepository.findById(ID)).thenReturn(Optional.of(c));
        return c;
    }

    // ------------------------------------------------------------ moja članarina (član)

    @Test
    void mojaClanarinaVracaPodatkeIQrKod() {
        postojiClan();
        postojiClanarinaDo(DANAS.plusDays(10));

        ClanarinaClanaDto dto = servis.mojaClanarina(ID);

        assertEquals(DANAS.plusDays(10), dto.validUntil());
        assertEquals(40.0, dto.price());
        assertTrue(dto.isActive());
        assertTrue(dto.qrCodeUrl().startsWith("data:image/png;base64,"));
    }

    @Test
    void mojaClanarinaKojaIsticeDanasJeAktivna() {
        postojiClan();
        postojiClanarinaDo(DANAS);

        assertTrue(servis.mojaClanarina(ID).isActive());
    }

    @Test
    void mojaClanarinaKojaJeIsteklaNijeAktivna() {
        postojiClan();
        postojiClanarinaDo(DANAS.minusDays(1));

        assertFalse(servis.mojaClanarina(ID).isActive());
    }

    @Test
    void mojaClanarinaBezClanarineVraca404() {
        postojiClan();
        when(clanarinaRepository.findById(ID)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.mojaClanarina(ID));
    }

    // ------------------------------------------------------------ pregled (radnik)

    @Test
    void clanarinaClanaZaRadnika() {
        postojiClan();
        postojiClanarinaDo(DANAS.minusDays(3));

        ClanarinaDto dto = servis.clanarinaClana(ID);

        assertEquals(DANAS.minusDays(3), dto.validUntil());
        assertFalse(dto.isActive());
    }

    @Test
    void nepostojeciClanVraca404NaSvimRutama() {
        when(clanRepository.pronadjiAktivnog(ID)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.mojaClanarina(ID));
        assertThrows(ResursNijePronadjenException.class, () -> servis.clanarinaClana(ID));
        assertThrows(ResursNijePronadjenException.class,
                () -> servis.postaviClanarinu(ID, new IzmjenaClanarineZahtjev(DANAS, DANAS, 40.0)));
        assertThrows(ResursNijePronadjenException.class,
                () -> servis.produziClanarinu(ID, new ProduzenjeClanarineZahtjev(1)));
    }

    // ------------------------------------------------------------ postavljanje (radnik)

    @Test
    void postavljanjeKreiraClanarinuAkoNePostoji() {
        postojiClan();
        when(clanarinaRepository.findById(ID)).thenReturn(Optional.empty());

        servis.postaviClanarinu(ID, new IzmjenaClanarineZahtjev(DANAS, DANAS.plusMonths(1), 35.0));

        ArgumentCaptor<Clanarina> sacuvana = ArgumentCaptor.forClass(Clanarina.class);
        verify(clanarinaRepository).save(sacuvana.capture());
        assertEquals(ID, sacuvana.getValue().getId());
        assertEquals(35.0, sacuvana.getValue().getCijena());
        assertEquals(DANAS, sacuvana.getValue().getDatumPocetka());
        assertEquals(DANAS.plusMonths(1), sacuvana.getValue().getDatumIsteka());
    }

    @Test
    void postavljanjeMijenjaPostojecuClanarinu() {
        postojiClan();
        Clanarina postojeca = postojiClanarinaDo(DANAS.minusDays(5));

        servis.postaviClanarinu(ID, new IzmjenaClanarineZahtjev(DANAS, DANAS.plusMonths(2), 50.0));

        verify(clanarinaRepository, never()).save(any());
        assertEquals(50.0, postojeca.getCijena());
        assertEquals(DANAS, postojeca.getDatumPocetka());
        assertEquals(DANAS.plusMonths(2), postojeca.getDatumIsteka());
    }

    @Test
    void postavljanjeSaIstimDatumimaJeDozvoljeno() {
        postojiClan();
        when(clanarinaRepository.findById(ID)).thenReturn(Optional.empty());

        servis.postaviClanarinu(ID, new IzmjenaClanarineZahtjev(DANAS, DANAS, 0.0));

        verify(clanarinaRepository).save(any());
    }

    @Test
    void datumIstekaPrijeUplateVraca400INistaNeMijenja() {
        postojiClan();
        Clanarina postojeca = postojiClanarinaDo(DANAS.plusDays(5));

        assertStatus(400, () -> servis.postaviClanarinu(ID,
                new IzmjenaClanarineZahtjev(DANAS, DANAS.minusDays(1), 40.0)));

        verify(clanarinaRepository, never()).save(any());
        assertEquals(DANAS.plusDays(5), postojeca.getDatumIsteka());
    }

    // ------------------------------------------------------------ produženje (radnik)

    @Test
    void aktivnaClanarinaSeProduzavaOdDatumaIsteka() {
        postojiClan();
        Clanarina c = postojiClanarinaDo(DANAS.plusDays(10));

        ProduzenjeClanarineOdgovor odgovor = servis.produziClanarinu(ID, new ProduzenjeClanarineZahtjev(2));

        assertEquals(DANAS.plusDays(10).plusMonths(2), odgovor.newValidUntil());
        assertEquals(odgovor.newValidUntil(), c.getDatumIsteka());
        assertEquals(DANAS, c.getDatumPocetka());
    }

    @Test
    void clanarinaKojaIsticeDanasSeProduzavaOdDanas() {
        postojiClan();
        postojiClanarinaDo(DANAS);

        assertEquals(DANAS.plusMonths(1),
                servis.produziClanarinu(ID, new ProduzenjeClanarineZahtjev(1)).newValidUntil());
    }

    @Test
    void isteklaClanarinaSeProduzavaOdDanasnjegDana() {
        postojiClan();
        Clanarina c = postojiClanarinaDo(DANAS.minusMonths(3));

        ProduzenjeClanarineOdgovor odgovor = servis.produziClanarinu(ID, new ProduzenjeClanarineZahtjev(1));

        assertEquals(DANAS.plusMonths(1), odgovor.newValidUntil());
        assertEquals(DANAS, c.getDatumPocetka());
    }

    @Test
    void produzenjeBezClanarineVraca404() {
        postojiClan();
        when(clanarinaRepository.findById(ID)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class,
                () -> servis.produziClanarinu(ID, new ProduzenjeClanarineZahtjev(1)));
    }
}
