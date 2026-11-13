package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
import static org.etf.pumpd.service.TestPomocni.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.dto.ClanDetaljiDto;
import org.etf.pumpd.dto.ClanListaDto;
import org.etf.pumpd.dto.DodavanjeClanaOdgovor;
import org.etf.pumpd.dto.DodavanjeClanaZahtjev;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Clanarina;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ClanarinaRepository;
import org.etf.pumpd.repository.NalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClanoviServiceTest {

    /** "Danas" prema fiksiranom satu iz TestPomocni. */
    private static final LocalDate DANAS = LocalDate.of(2026, 10, 29);

    private ClanRepository clanRepository;
    private ClanarinaRepository clanarinaRepository;
    private NalogRepository nalogRepository;
    private PasswordEncoder passwordEncoder;
    private ClanoviService servis;

    @BeforeEach
    void postavi() {
        clanRepository = mock(ClanRepository.class);
        clanarinaRepository = mock(ClanarinaRepository.class);
        nalogRepository = mock(NalogRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(4); // mala cijena da test bude brz
        GeneratorLozinke generatorLozinke = mock(GeneratorLozinke.class);
        when(generatorLozinke.generisi()).thenReturn("Privremena7k");
        // pravi NoviNalogService nad mock repozitorijumom, da test pokriva cijeli tok dodavanja člana
        servis = new ClanoviService(clanRepository, clanarinaRepository,
                new NoviNalogService(nalogRepository, passwordEncoder, generatorLozinke), SAT);
    }

    private static Clan clan(int id, String ime, String prezime) {
        Nalog nalog = new Nalog(ime, prezime, ime.toLowerCase() + "@pumpd.local", "hash");
        nalog.setId(id);
        return new Clan(nalog, null, null, null, null);
    }

    private static Clanarina clanarinaDo(int idClana, LocalDate datumIsteka) {
        return new Clanarina(idClana, 40.0, datumIsteka.minusDays(30), datumIsteka);
    }

    // ------------------------------------------------------------ 11.1 lista članova

    @Test
    void vracaPodatkeClanaIzNaloga() {
        when(clanRepository.pretrazi(null)).thenReturn(List.of(clan(6, "Amar", "Hodžić")));
        when(clanarinaRepository.findAllById(anyIterable())).thenReturn(List.of());

        List<ClanListaDto> lista = servis.listaClanova(null);

        assertEquals(1, lista.size());
        ClanListaDto stavka = lista.get(0);
        assertEquals(6, stavka.memberId());
        assertEquals("Amar", stavka.name());
        assertEquals("Hodžić", stavka.surname());
        assertEquals("amar@pumpd.local", stavka.email());
    }

    @Test
    void statusClanarineSeRacunaPremaDanasnjemDatumu() {
        when(clanRepository.pretrazi(null)).thenReturn(List.of(
                clan(6, "Aktivna", "A"),
                clan(7, "IsticeDanas", "B"),
                clan(8, "Istekla", "C"),
                clan(9, "BezClanarine", "D")));
        when(clanarinaRepository.findAllById(anyIterable())).thenReturn(List.of(
                clanarinaDo(6, DANAS.plusDays(20)),
                clanarinaDo(7, DANAS),
                clanarinaDo(8, DANAS.minusDays(1))));

        List<ClanListaDto> lista = servis.listaClanova(null);

        assertTrue(lista.get(0).membershipActive(), "članarina koja još traje je aktivna");
        assertTrue(lista.get(1).membershipActive(), "članarina koja ističe danas je još aktivna");
        assertFalse(lista.get(2).membershipActive(), "istekla članarina nije aktivna");
        assertFalse(lista.get(3).membershipActive(), "član bez članarine nema aktivnu članarinu");
    }

    @Test
    void zadrzavaRedoslijedIzRepozitorijuma() {
        when(clanRepository.pretrazi(null)).thenReturn(List.of(
                clan(8, "Lejla", "Begović"), clan(6, "Amar", "Hodžić")));
        when(clanarinaRepository.findAllById(anyIterable())).thenReturn(List.of());

        List<ClanListaDto> lista = servis.listaClanova(null);

        assertEquals(List.of(8, 6), lista.stream().map(ClanListaDto::memberId).toList());
    }

    @Test
    void bezClanovaVracaPraznuListuINeCitaClanarine() {
        when(clanRepository.pretrazi(null)).thenReturn(List.of());

        List<ClanListaDto> lista = servis.listaClanova(null);

        assertTrue(lista.isEmpty());
        verify(clanarinaRepository, never()).findAllById(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void praznaPretragaZnaciBezFiltera(String search) {
        when(clanRepository.pretrazi(isNull())).thenReturn(List.of());

        servis.listaClanova(search);

        verify(clanRepository).pretrazi(null);
    }

    @Test
    void pretragaSeSaljeBezRazmakaSaStrana() {
        when(clanRepository.pretrazi("Amar")).thenReturn(List.of());

        servis.listaClanova("  Amar ");

        verify(clanRepository).pretrazi("Amar");
    }

    // ------------------------------------------------------------ 11.2 detalji člana

    private void postojiClan(Clan clan) {
        when(clanRepository.pronadjiAktivnog(clan.getNalog().getId())).thenReturn(Optional.of(clan));
    }

    @Test
    void detaljiVracajuSvePodatkeClana() {
        Clan amar = clan(6, "Amar", "Hodžić");
        amar.setDatumRodjenja(LocalDate.of(1998, 4, 12));
        amar.setTjelesnaTezina(80.0);
        amar.setVisina(180.0);
        postojiClan(amar);
        when(clanarinaRepository.findById(6)).thenReturn(Optional.empty());

        ClanDetaljiDto detalji = servis.detaljiClana(6);

        assertEquals(6, detalji.memberId());
        assertEquals("Amar", detalji.name());
        assertEquals("Hodžić", detalji.surname());
        assertEquals("amar@pumpd.local", detalji.email());
        assertEquals(1998, detalji.birthYear());
        assertEquals(80.0, detalji.weight());
        assertEquals(180.0, detalji.height());
    }

    @Test
    void detaljiBezMjeraImajuNullPolja() {
        postojiClan(clan(6, "Amar", "Hodžić"));
        when(clanarinaRepository.findById(6)).thenReturn(Optional.empty());

        ClanDetaljiDto detalji = servis.detaljiClana(6);

        assertNull(detalji.birthYear());
        assertNull(detalji.weight());
        assertNull(detalji.height());
    }

    @Test
    void detaljiSaAktivnomClanarinom() {
        postojiClan(clan(6, "Amar", "Hodžić"));
        when(clanarinaRepository.findById(6)).thenReturn(Optional.of(clanarinaDo(6, DANAS.plusDays(20))));

        ClanDetaljiDto detalji = servis.detaljiClana(6);

        assertTrue(detalji.membershipActive());
        assertEquals(DANAS.plusDays(20), detalji.membershipValidUntil());
    }

    @Test
    void detaljiSaIsteklomClanarinomPrikazujuDatumIsteka() {
        postojiClan(clan(6, "Amar", "Hodžić"));
        when(clanarinaRepository.findById(6)).thenReturn(Optional.of(clanarinaDo(6, DANAS.minusDays(15))));

        ClanDetaljiDto detalji = servis.detaljiClana(6);

        assertFalse(detalji.membershipActive());
        assertEquals(DANAS.minusDays(15), detalji.membershipValidUntil());
    }

    @Test
    void detaljiBezClanarine() {
        postojiClan(clan(6, "Amar", "Hodžić"));
        when(clanarinaRepository.findById(6)).thenReturn(Optional.empty());

        ClanDetaljiDto detalji = servis.detaljiClana(6);

        assertFalse(detalji.membershipActive());
        assertNull(detalji.membershipValidUntil());
    }

    @Test
    void nepostojeciClanBaca404() {
        when(clanRepository.pronadjiAktivnog(99)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.detaljiClana(99));
    }

    // ------------------------------------------------------------ 11.3 dodavanje člana

    private static DodavanjeClanaZahtjev zahtjev(Integer godina) {
        return new DodavanjeClanaZahtjev(" Ana ", " Ilić ", " ana@mail.com ", godina, 60.5, 168.0);
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

    private Nalog sacuvaniNalog() {
        ArgumentCaptor<Nalog> nalog = ArgumentCaptor.forClass(Nalog.class);
        verify(nalogRepository).saveAndFlush(nalog.capture());
        return nalog.getValue();
    }

    private Clan sacuvaniClan() {
        ArgumentCaptor<Clan> clan = ArgumentCaptor.forClass(Clan.class);
        verify(clanRepository).save(clan.capture());
        return clan.getValue();
    }

    @Test
    void dodavanjeVracaIdNovogClana() {
        bazaDodjeljujeId();

        DodavanjeClanaOdgovor odgovor = servis.dodajClana(zahtjev(2000));

        assertEquals(11, odgovor.memberId());
        assertEquals("Clan je uspjesno dodan.", odgovor.message());
        assertEquals("Privremena7k", odgovor.temporaryPassword());
    }

    @Test
    void clanSeVezujeZaNalogSaMjeramaIDatumomRodjenja() {
        bazaDodjeljujeId();

        servis.dodajClana(zahtjev(2000));

        Clan clan = sacuvaniClan();
        assertEquals(11, clan.getNalog().getId());
        assertEquals(LocalDate.of(2000, 1, 1), clan.getDatumRodjenja());
        assertEquals(60.5, clan.getTjelesnaTezina());
        assertEquals(168.0, clan.getVisina());
    }

    @Test
    void bezGodineRodjenjaDatumOstajeNull() {
        bazaDodjeljujeId();

        servis.dodajClana(zahtjev(null));

        assertNull(sacuvaniClan().getDatumRodjenja());
    }

    @Test
    void zauzetEmailBaca409IClanSeNeUpisuje() {
        when(nalogRepository.existsByEmail("ana@mail.com")).thenReturn(true);

        KonfliktException izuzetak = assertThrows(KonfliktException.class, () -> servis.dodajClana(zahtjev(2000)));

        assertEquals("Email vec postoji.", izuzetak.getMessage());
        verify(nalogRepository, never()).saveAndFlush(any());
        verify(clanRepository, never()).save(any());
    }

    @Test
    void godinaRodjenjaUBuducnostiBaca400() {
        assertStatus(400, () -> servis.dodajClana(zahtjev(2027)));
        verify(nalogRepository, never()).saveAndFlush(any());
    }

    @Test
    void tekucaGodinaRodjenjaJeDozvoljena() {
        bazaDodjeljujeId();

        servis.dodajClana(zahtjev(2026));

        assertEquals(LocalDate.of(2026, 1, 1), sacuvaniClan().getDatumRodjenja());
    }

    // ------------------------------------------------------------ 11.4 uklanjanje člana

    @Test
    void uklanjanjeDeaktiviraNalogIOslobadjaEmail() {
        Clan amar = clan(6, "Amar", "Hodžić");
        postojiClan(amar);

        assertEquals("Clan je uspjesno uklonjen.", servis.ukloniClana(6).message());

        assertFalse(amar.getNalog().isAktivan());
        assertEquals("obrisan-6@pumpd.invalid", amar.getNalog().getEmail());
        assertEquals("Amar", amar.getNalog().getIme(), "ime ostaje zbog istorije");
    }

    @Test
    void uklanjanjeNepostojecegIliVecObrisanogClanaBaca404() {
        when(clanRepository.pronadjiAktivnog(99)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.ukloniClana(99));
    }
}
