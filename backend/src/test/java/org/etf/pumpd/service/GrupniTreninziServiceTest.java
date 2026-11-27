package org.etf.pumpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import org.etf.pumpd.dto.TerminGrupnogTreningaDto;
import org.etf.pumpd.dto.ZahtjevZaPrijavuOdgovor;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.ClanNaTerminu;
import org.etf.pumpd.entity.ClanNaTerminuId;
import org.etf.pumpd.entity.ClanUGrupiId;
import org.etf.pumpd.entity.Grupa;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Termin;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.enums.StatusPrijave;
import org.etf.pumpd.enums.StatusPrijaveClana;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanNaTerminuRepository;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ClanUGrupiRepository;
import org.etf.pumpd.repository.TerminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Jedinični test 9.1–9.3 (GrupniTreninziService) sa lažnim repozitorijumima. */
class GrupniTreninziServiceTest {

    private static final ZoneId ZONA = ZoneId.of("Europe/Sarajevo");
    private static final LocalDate DANAS = LocalDate.of(2026, 11, 26);
    private static final LocalDateTime SADA = DANAS.atTime(15, 20);
    private static final int ID_CLANA = 40;
    private static final int ID_TERMINA = 1;
    private static final int ID_GRUPE = 11;
    private static final int ID_TRENERA = 5;

    private TerminRepository terminRepository;
    private ClanUGrupiRepository clanUGrupiRepository;
    private ClanNaTerminuRepository prijavaRepository;
    private ClanRepository clanRepository;
    private ObavjestenjeService obavjestenjeService;
    private GrupniTreninziService servis;

    @BeforeEach
    void pripremi() {
        terminRepository = mock(TerminRepository.class);
        clanUGrupiRepository = mock(ClanUGrupiRepository.class);
        prijavaRepository = mock(ClanNaTerminuRepository.class);
        clanRepository = mock(ClanRepository.class);
        obavjestenjeService = mock(ObavjestenjeService.class);
        Clock sat = Clock.fixed(SADA.atZone(ZONA).toInstant(), ZONA);
        servis = new GrupniTreninziService(terminRepository, clanUGrupiRepository, prijavaRepository,
                clanRepository, obavjestenjeService, sat);
    }

    private static Termin termin(int id, int idGrupe, String naziv, LocalDateTime vrijeme) {
        Nalog nalog = mock(Nalog.class);
        when(nalog.getIme()).thenReturn("Ana");
        when(nalog.getPrezime()).thenReturn("Anic");
        when(nalog.isAktivan()).thenReturn(true);
        Trener trener = mock(Trener.class);
        when(trener.getId()).thenReturn(5);
        when(trener.getNalog()).thenReturn(nalog);
        Grupa grupa = mock(Grupa.class);
        when(grupa.getId()).thenReturn(idGrupe);
        when(grupa.getNaziv()).thenReturn(naziv);
        when(grupa.getMaxClanova()).thenReturn(12);
        Termin termin = mock(Termin.class);
        when(termin.getId()).thenReturn(id);
        when(termin.getGrupa()).thenReturn(grupa);
        when(termin.getTrener()).thenReturn(trener);
        when(termin.getVrijeme()).thenReturn(vrijeme);
        when(termin.getTrajanjeMinuta()).thenReturn(60);
        return termin;
    }

    private static ClanNaTerminu prijava(int idTermina, StatusPrijave status) {
        ClanNaTerminu prijava = mock(ClanNaTerminu.class);
        when(prijava.getId()).thenReturn(new ClanNaTerminuId(idTermina, ID_CLANA));
        when(prijava.getStatus()).thenReturn(status);
        when(prijava.getIdZahtjeva()).thenReturn(900 + idTermina);
        Clan clan = clan();
        when(prijava.getClan()).thenReturn(clan);
        return prijava;
    }

    private static Clan clan() {
        Nalog nalog = mock(Nalog.class);
        when(nalog.getIme()).thenReturn("Mila");
        when(nalog.getPrezime()).thenReturn("Milic");
        Clan clan = mock(Clan.class);
        when(clan.getNalog()).thenReturn(nalog);
        return clan;
    }

    /** Termin ID_TERMINA (grupa ID_GRUPE "Joga", kapacitet 12, trener ID_TRENERA) koji počinje u datom trenutku. */
    private Termin terminUBazi(LocalDateTime pocetak) {
        Termin termin = termin(ID_TERMINA, ID_GRUPE, "Joga", pocetak);
        when(terminRepository.saGrupomITrenerom(ID_TERMINA)).thenReturn(Optional.of(termin));
        return termin;
    }

    private void postojecaPrijava(StatusPrijave status) {
        ClanNaTerminu prijava = prijava(ID_TERMINA, status);
        when(prijavaRepository.findById(new ClanNaTerminuId(ID_TERMINA, ID_CLANA))).thenReturn(Optional.of(prijava));
    }

    private void aktivanClan() {
        Clan clan = clan();
        when(clanRepository.pronadjiAktivnog(ID_CLANA)).thenReturn(Optional.of(clan));
    }

    private static HttpStatus status(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    void bezDatumaTraziOdPocetkaDanasBezKraja() {
        when(terminRepository.pregledTermina(any(), any(), any())).thenReturn(List.of());

        servis.pregled(ID_CLANA, null, null);

        verify(terminRepository).pregledTermina(DANAS.atStartOfDay(), null, null);
    }

    @Test
    void saDatumomTraziCijeliTajDan() {
        when(terminRepository.pregledTermina(any(), any(), any())).thenReturn(List.of());
        LocalDate dan = LocalDate.of(2026, 12, 3);

        servis.pregled(null, dan, 7);

        verify(terminRepository).pregledTermina(dan.atStartOfDay(), dan.plusDays(1).atStartOfDay(), 7);
    }

    @Test
    void praznaListaBezDodatnihUpita() {
        when(terminRepository.pregledTermina(any(), any(), any())).thenReturn(List.of());

        assertEquals(List.of(), servis.pregled(ID_CLANA, null, null));

        verifyNoInteractions(clanUGrupiRepository, prijavaRepository);
    }

    @Test
    void clanDobijaStatusIBrojClanovaPoTerminu() {
        LocalDateTime sutra = DANAS.plusDays(1).atTime(9, 0);
        // lažni objekti se prave prije when(...), jer Mockito ne dozvoljava novi mock usred stubovanja
        List<Termin> nadjeni = List.of(termin(1, 11, "Joga", sutra), termin(2, 12, "Pilates", sutra.plusHours(2)),
                termin(3, 13, "Kros", sutra.plusHours(4)));
        List<ClanNaTerminu> prijave = List.of(prijava(1, StatusPrijave.NA_CEKANJU), prijava(3, StatusPrijave.ODBIJEN));
        when(terminRepository.pregledTermina(any(), any(), any())).thenReturn(nadjeni);
        when(clanUGrupiRepository.brojClanovaPoGrupama(List.of(11, 12, 13)))
                .thenReturn(List.<Object[]>of(new Object[] {11, 4L}, new Object[] {13, 1L}));
        when(prijavaRepository.prijaveClana(ID_CLANA, List.of(1, 2, 3))).thenReturn(prijave);

        List<TerminGrupnogTreningaDto> termini = servis.pregled(ID_CLANA, null, null);

        assertEquals(new TerminGrupnogTreningaDto(1, 11, "Joga", 5, "Ana Anic", sutra, 60, 4, 12,
                StatusPrijaveClana.PENDING), termini.get(0));
        assertEquals(0, termini.get(1).enrolledCount());   // grupa bez članova nema red u upitu
        assertEquals(StatusPrijaveClana.NONE, termini.get(1).enrollmentStatus());
        assertEquals(StatusPrijaveClana.REJECTED, termini.get(2).enrollmentStatus());
        assertEquals(1, termini.get(2).enrolledCount());
    }

    @Test
    void radnikNeCitaPrijaveIBezStatusa() {
        List<Termin> nadjeni = List.of(termin(1, 11, "Joga", DANAS.atTime(18, 0)));
        when(terminRepository.pregledTermina(any(), any(), any())).thenReturn(nadjeni);
        when(clanUGrupiRepository.brojClanovaPoGrupama(any())).thenReturn(List.of());

        List<TerminGrupnogTreningaDto> termini = servis.pregled(null, null, null);

        assertNull(termini.get(0).enrollmentStatus());
        verify(prijavaRepository, never()).prijaveClana(anyInt(), any());
    }

    @Test
    void viseTerminaIsteGrupeBrojiGrupuJednom() {
        LocalDateTime vrijeme = DANAS.plusDays(1).atTime(9, 0);
        List<Termin> nadjeni = List.of(termin(1, 11, "Joga", vrijeme), termin(2, 11, "Joga", vrijeme.plusDays(7)));
        when(terminRepository.pregledTermina(any(), any(), any())).thenReturn(nadjeni);
        when(clanUGrupiRepository.brojClanovaPoGrupama(List.of(11))).thenReturn(List.<Object[]>of(new Object[] {11, 3L}));

        List<TerminGrupnogTreningaDto> termini = servis.pregled(null, null, null);

        assertEquals(3, termini.get(0).enrolledCount());
        assertEquals(3, termini.get(1).enrolledCount());
    }

    // ------------------------------------------------------------ 9.2

    @Test
    void noviZahtjevSeUpisujeNaCekanjuIObavjestavaTrenera() {
        terminUBazi(SADA.plusDays(1));
        aktivanClan();
        when(clanUGrupiRepository.brojClanova(ID_GRUPE)).thenReturn(11L);   // jedno mjesto slobodno
        ClanNaTerminu upisana = prijava(ID_TERMINA, StatusPrijave.NA_CEKANJU);
        when(prijavaRepository.saveAndFlush(any())).thenReturn(upisana);

        ZahtjevZaPrijavuOdgovor odgovor = servis.posaljiZahtjev(ID_TERMINA, ID_CLANA);

        assertEquals(new ZahtjevZaPrijavuOdgovor(901, StatusPrijaveClana.PENDING, "Zahtjev za prijavu je poslat."), odgovor);
        verify(prijavaRepository).saveAndFlush(argThat(p ->
                p.getStatus() == StatusPrijave.NA_CEKANJU && DANAS.equals(p.getDatumZahtjeva())));
        verify(obavjestenjeService).posalji(ID_TRENERA, ID_CLANA,
                "Mila Milic je poslao zahtjev za prijavu na termin \"Joga\" (27.11.2026. u 15:20).");
    }

    @Test
    void nepostojeciTerminJe404() {
        when(terminRepository.saGrupomITrenerom(ID_TERMINA)).thenReturn(Optional.empty());

        ResursNijePronadjenException e = assertThrows(ResursNijePronadjenException.class,
                () -> servis.posaljiZahtjev(ID_TERMINA, ID_CLANA));
        assertEquals("Termin ne postoji.", e.getMessage());
    }

    @Test
    void terminObrisanogTreneraJe404() {
        Termin termin = terminUBazi(SADA.plusDays(1));
        when(termin.getTrener().getNalog().isAktivan()).thenReturn(false);

        assertThrows(ResursNijePronadjenException.class, () -> servis.posaljiZahtjev(ID_TERMINA, ID_CLANA));
        verify(prijavaRepository, never()).saveAndFlush(any());
    }

    @Test
    void terminKojiJePoceoJe400() {
        for (LocalDateTime pocetak : List.of(SADA, SADA.minusMinutes(1), SADA.minusDays(3))) {
            terminUBazi(pocetak);
            ResponseStatusException e = assertThrows(ResponseStatusException.class,
                    () -> servis.posaljiZahtjev(ID_TERMINA, ID_CLANA));
            assertEquals(HttpStatus.BAD_REQUEST, status(e));
            assertEquals("Termin je vec poceo.", e.getReason());
        }
        verify(prijavaRepository, never()).saveAndFlush(any());
    }

    @Test
    void zahtjevNaCekanjuJe409() {
        terminUBazi(SADA.plusDays(1));
        postojecaPrijava(StatusPrijave.NA_CEKANJU);

        KonfliktException e = assertThrows(KonfliktException.class, () -> servis.posaljiZahtjev(ID_TERMINA, ID_CLANA));
        assertEquals("Zahtjev je vec poslat.", e.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPrijave.class, names = {"PRIJAVLJEN", "PRISUTAN"})
    void vecPrijavljenJe409(StatusPrijave status) {
        terminUBazi(SADA.plusDays(1));
        postojecaPrijava(status);

        KonfliktException e = assertThrows(KonfliktException.class, () -> servis.posaljiZahtjev(ID_TERMINA, ID_CLANA));
        assertEquals("Vec ste prijavljeni na termin.", e.getMessage());
        verify(obavjestenjeService, never()).posalji(anyInt(), anyInt(), anyString());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPrijave.class, names = {"ODBIJEN", "OTKAZAO"})
    void odbijenIliOtkazanZahtjevSePonavljaUIstomRedu(StatusPrijave status) {
        terminUBazi(SADA.plusDays(1));
        ClanNaTerminu prijava = prijava(ID_TERMINA, status);
        when(prijavaRepository.findById(new ClanNaTerminuId(ID_TERMINA, ID_CLANA))).thenReturn(Optional.of(prijava));
        aktivanClan();

        ZahtjevZaPrijavuOdgovor odgovor = servis.posaljiZahtjev(ID_TERMINA, ID_CLANA);

        assertEquals(901, odgovor.requestId());   // isti red, isti requestId
        verify(prijava).ponoviZahtjev(DANAS);
        verify(prijavaRepository, never()).saveAndFlush(any());
        verify(obavjestenjeService).posalji(eq(ID_TRENERA), eq(ID_CLANA), anyString());
    }

    @Test
    void punTerminJe409() {
        terminUBazi(SADA.plusDays(1));
        when(clanUGrupiRepository.brojClanova(ID_GRUPE)).thenReturn(12L);

        KonfliktException e = assertThrows(KonfliktException.class, () -> servis.posaljiZahtjev(ID_TERMINA, ID_CLANA));
        assertEquals("Termin je popunjen.", e.getMessage());
        verify(prijavaRepository, never()).saveAndFlush(any());
    }

    @Test
    void istovremeniDrugiZahtjevJe409() {
        terminUBazi(SADA.plusDays(1));
        aktivanClan();
        when(prijavaRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("PRIMARY"));

        KonfliktException e = assertThrows(KonfliktException.class, () -> servis.posaljiZahtjev(ID_TERMINA, ID_CLANA));
        assertEquals("Zahtjev je vec poslat.", e.getMessage());
        verify(obavjestenjeService, never()).posalji(anyInt(), anyInt(), anyString());
    }

    // ------------------------------------------------------------ 9.3

    @Test
    void zahtjevNaCekanjuSePovlaci() {
        postojecaPrijava(StatusPrijave.NA_CEKANJU);

        assertEquals("Zahtjev za prijavu je povucen.", servis.odjava(ID_TERMINA, ID_CLANA).message());

        verify(prijavaRepository).delete(any());
        verify(clanUGrupiRepository, never()).deleteById(any());
        verify(obavjestenjeService, never()).posalji(anyInt(), anyInt(), anyString());
    }

    @Test
    void prihvacenClanSeOdjavljujeIzTerminaIGrupe() {
        terminUBazi(SADA.plusHours(2));
        postojecaPrijava(StatusPrijave.PRIJAVLJEN);

        assertEquals("Odjavljeni ste sa termina.", servis.odjava(ID_TERMINA, ID_CLANA).message());

        verify(prijavaRepository).delete(any());
        verify(clanUGrupiRepository).deleteById(new ClanUGrupiId(ID_CLANA, ID_GRUPE));
        verify(obavjestenjeService).posalji(ID_TRENERA, ID_CLANA,
                "Mila Milic se odjavio sa termina \"Joga\" (26.11.2026. u 17:20).");
    }

    @Test
    void odjavaKodObrisanogTreneraBezObavjestenja() {
        Termin termin = terminUBazi(SADA.plusDays(1));
        when(termin.getTrener().getNalog().isAktivan()).thenReturn(false);
        postojecaPrijava(StatusPrijave.PRIJAVLJEN);

        assertEquals("Odjavljeni ste sa termina.", servis.odjava(ID_TERMINA, ID_CLANA).message());
        verify(obavjestenjeService, never()).posalji(anyInt(), anyInt(), anyString());
    }

    @Test
    void odjavaNakonPocetkaTerminaJe400() {
        terminUBazi(SADA);
        postojecaPrijava(StatusPrijave.PRIJAVLJEN);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> servis.odjava(ID_TERMINA, ID_CLANA));
        assertEquals(HttpStatus.BAD_REQUEST, status(e));
        assertEquals("Termin je vec poceo, odjava nije moguca.", e.getReason());
        verify(prijavaRepository, never()).delete(any());
        verify(clanUGrupiRepository, never()).deleteById(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPrijave.class, names = {"ODBIJEN", "OTKAZAO"})
    void odbijenIliOtkazanNijePrijavaJe404(StatusPrijave status) {
        postojecaPrijava(status);

        ResursNijePronadjenException e = assertThrows(ResursNijePronadjenException.class,
                () -> servis.odjava(ID_TERMINA, ID_CLANA));
        assertEquals("Niste prijavljeni na termin niti imate zahtjev na cekanju.", e.getMessage());
        verify(prijavaRepository, never()).delete(any());
    }

    @Test
    void bezPrijaveJe404() {
        when(prijavaRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.odjava(ID_TERMINA, ID_CLANA));
    }
}
