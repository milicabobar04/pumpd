package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.dto.IzlazakOdgovor;
import org.etf.pumpd.dto.UlazakOdgovor;
import org.etf.pumpd.dto.ZauzetostDto;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Clanarina;
import org.etf.pumpd.entity.Dolazak;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Teretana;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ClanarinaRepository;
import org.etf.pumpd.repository.DolazakRepository;
import org.etf.pumpd.repository.TeretanaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DolazakServiceTest {

    /** "Sada" prema fiksiranom satu iz TestPomocni: 29.10.2026. u 10:15 UTC. */
    private static final LocalDate DANAS = LocalDate.of(2026, 10, 29);
    private static final LocalDateTime SADA = LocalDateTime.of(2026, 10, 29, 10, 15, 0);
    private static final LocalDateTime POCETAK_DANA = DANAS.atStartOfDay();
    private static final int ID = 6;
    private static final String QR = "qr-ana";

    private ClanRepository clanRepository;
    private ClanarinaRepository clanarinaRepository;
    private DolazakRepository dolazakRepository;
    private TeretanaRepository teretanaRepository;
    private DolazakService servis;
    private Clan clan;

    @BeforeEach
    void postavi() {
        clanRepository = mock(ClanRepository.class);
        clanarinaRepository = mock(ClanarinaRepository.class);
        dolazakRepository = mock(DolazakRepository.class);
        teretanaRepository = mock(TeretanaRepository.class);
        servis = new DolazakService(clanRepository, clanarinaRepository, dolazakRepository, teretanaRepository, SAT);

        Nalog nalog = new Nalog("Ana", "Babic", "ana@pumpd.local", "hash");
        nalog.setId(ID);
        clan = new Clan(nalog, null, null, null, null);
        clan.setQrKod(QR);
        when(clanRepository.pronadjiAktivnogPoQrKodu(QR)).thenReturn(Optional.of(clan));
        when(dolazakRepository.otvoreniOd(ID, POCETAK_DANA)).thenReturn(List.of());
    }

    private void clanarinaDo(LocalDate istek) {
        when(clanarinaRepository.findById(ID))
                .thenReturn(Optional.of(new Clanarina(ID, 40.0, istek.minusMonths(1), istek)));
    }

    private void uTeretaniOd(LocalDateTime ulazak) {
        when(dolazakRepository.otvoreniOd(ID, POCETAK_DANA)).thenReturn(List.of(new Dolazak(clan, ulazak)));
    }

    // ------------------------------------------------------------ 11.8 ulazak

    @Test
    void ulazakSaAktivnomClanarinomSeEvidentira() {
        clanarinaDo(DANAS.plusDays(10));

        UlazakOdgovor odgovor = servis.ulazak(QR);

        assertTrue(odgovor.isAllowed());
        assertEquals(ID, odgovor.memberId());
        assertEquals("Ana Babic", odgovor.memberName());
        ArgumentCaptor<Dolazak> upisan = ArgumentCaptor.forClass(Dolazak.class);
        verify(dolazakRepository).save(upisan.capture());
        assertEquals(SADA, upisan.getValue().getVrijemeUlaska());
        assertNull(upisan.getValue().getVrijemeIzlaska());
    }

    @Test
    void clanarinaKojaIsticeDanasJosDozvoljavaUlazak() {
        clanarinaDo(DANAS);
        assertTrue(servis.ulazak(QR).isAllowed());
    }

    @Test
    void isteklaClanarinaNeDozvoljavaUlazakINistaSeNeUpisuje() {
        clanarinaDo(DANAS.minusDays(1));

        UlazakOdgovor odgovor = servis.ulazak(QR);

        assertFalse(odgovor.isAllowed());
        assertEquals(ID, odgovor.memberId());
        verify(dolazakRepository, never()).save(any());
    }

    @Test
    void clanBezClanarineNijeDozvoljen() {
        when(clanarinaRepository.findById(ID)).thenReturn(Optional.empty());

        assertFalse(servis.ulazak(QR).isAllowed());
        verify(dolazakRepository, never()).save(any());
    }

    @Test
    void clanKojiJeVecUTeretaniDaje409() {
        clanarinaDo(DANAS.plusDays(10));
        uTeretaniOd(SADA.minusHours(1));

        assertThrows(KonfliktException.class, () -> servis.ulazak(QR));
        verify(dolazakRepository, never()).save(any());
    }

    @Test
    void nepoznatQrKodDaje404() {
        when(clanRepository.pronadjiAktivnogPoQrKodu(anyString())).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.ulazak("nepoznat"));
        assertThrows(ResursNijePronadjenException.class, () -> servis.izlazak("nepoznat"));
    }

    @Test
    void qrKodSeCitaBezRazmakaNaKrajevima() {
        clanarinaDo(DANAS.plusDays(10));
        assertTrue(servis.ulazak("  " + QR + " ").isAllowed());
    }

    // ------------------------------------------------------------ 11.9 izlazak

    @Test
    void izlazakZatvaraDanasnjiBoravak() {
        Dolazak boravak = new Dolazak(clan, SADA.minusHours(2));
        when(dolazakRepository.otvoreniOd(ID, POCETAK_DANA)).thenReturn(List.of(boravak));

        IzlazakOdgovor odgovor = servis.izlazak(QR);

        assertEquals(SADA, odgovor.exitTime());
        assertEquals(SADA, boravak.getVrijemeIzlaska());
        assertEquals("Ana Babic", odgovor.memberName());
    }

    @Test
    void izlazakBezUlaskaDaje409() {
        assertThrows(KonfliktException.class, () -> servis.izlazak(QR));
    }

    // ------------------------------------------------------------ 5.3 zauzetost

    @Test
    void zauzetostBrojiOtvoreneBoravkeOdPocetkaDanaIVracaKapacitet() {
        when(dolazakRepository.brojPrisutnihOd(POCETAK_DANA)).thenReturn(7L);
        Teretana teretana = new Teretana();
        teretana.setKapacitet(80);
        when(teretanaRepository.findById(Teretana.ID)).thenReturn(Optional.of(teretana));

        ZauzetostDto zauzetost = servis.zauzetost();

        assertEquals(7, zauzetost.currentMembers());
        assertEquals(80, zauzetost.capacity());
    }

    @Test
    void bezInformacijaOTeretaniKapacitetJeNull() {
        when(teretanaRepository.findById(Teretana.ID)).thenReturn(Optional.empty());

        ZauzetostDto zauzetost = servis.zauzetost();

        assertEquals(0, zauzetost.currentMembers());
        assertNull(zauzetost.capacity());
    }
}
