package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
import static org.etf.pumpd.service.TestPomocni.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.etf.pumpd.dto.ProfilDto;
import org.etf.pumpd.dto.ProfilIzmjenaZahtjev;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.NalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfilServiceTest {

    private NalogRepository nalogRepository;
    private ClanRepository clanRepository;
    private KorisnikService korisnikService;
    private ProfilService servis;

    private Nalog nalog;
    private Clan clan;

    @BeforeEach
    void postavi() {
        nalogRepository = mock(NalogRepository.class);
        clanRepository = mock(ClanRepository.class);
        korisnikService = mock(KorisnikService.class);
        servis = new ProfilService(nalogRepository, clanRepository, korisnikService, SAT);

        nalog = new Nalog("Amar", "Hodžić", "clan1@pumpd.local", "hash");
        nalog.setId(5);
        clan = new Clan(nalog, 80.0, 180.0, "musko", LocalDate.of(1998, 4, 12));

        when(nalogRepository.findById(5)).thenReturn(Optional.of(nalog));
        when(clanRepository.findById(5)).thenReturn(Optional.of(clan));
    }

    private void prijavljenKao(Uloga uloga) {
        when(korisnikService.odrediUlogu(5)).thenReturn(Optional.of(uloga));
    }

    private ProfilIzmjenaZahtjev zahtjev(String ime, String prezime, Integer godina, Double tezina, Double visina) {
        return new ProfilIzmjenaZahtjev(ime, prezime, godina, tezina, visina);
    }

    // ------------------------------------------------------------ 2.1 pregled

    @Test
    void clanVidiSvojePodatkeSaGodinomRodjenja() {
        prijavljenKao(Uloga.MEMBER);

        ProfilDto profil = servis.mojProfil(5);

        assertEquals(5, profil.id());
        assertEquals("Amar", profil.name());
        assertEquals("Hodžić", profil.surname());
        assertEquals("clan1@pumpd.local", profil.email());
        assertEquals(1998, profil.birthYear());
        assertEquals(80.0, profil.weight());
        assertEquals(180.0, profil.height());
        assertEquals(Uloga.MEMBER, profil.role());
    }

    @Test
    void clanBezDatumaRodjenjaDobijaNullGodinu() {
        prijavljenKao(Uloga.MEMBER);
        clan.setDatumRodjenja(null);

        assertNull(servis.mojProfil(5).birthYear());
    }

    @Test
    void trenerDobijaNullZaGodinuTezinuIVisinu() {
        prijavljenKao(Uloga.TRAINER);

        ProfilDto profil = servis.mojProfil(5);

        assertNull(profil.birthYear());
        assertNull(profil.weight());
        assertNull(profil.height());
        assertEquals(Uloga.TRAINER, profil.role());
        verify(clanRepository, never()).findById(any());
    }

    @Test
    void nepostojeciNalogJeNeovlascen() {
        when(nalogRepository.findById(99)).thenReturn(Optional.empty());

        assertStatus(401, () -> servis.mojProfil(99));
    }

    @Test
    void nalogBezUlogeJeZabranjen() {
        when(korisnikService.odrediUlogu(5)).thenReturn(Optional.empty());

        assertStatus(403, () -> servis.mojProfil(5));
    }

    // ------------------------------------------------------------ 2.2 izmjena

    @Test
    void izmjenaImenaCuvaPunDatumRodjenjaKadaSeGodinaNijePromijenila() {
        prijavljenKao(Uloga.MEMBER);

        servis.izmijeniProfil(5, zahtjev("  Amar  ", "Hodžić-Novi", 1998, 82.5, 181.0));

        assertEquals("Amar", nalog.getIme());
        assertEquals("Hodžić-Novi", nalog.getPrezime());
        assertEquals(LocalDate.of(1998, 4, 12), clan.getDatumRodjenja());
        assertEquals(82.5, clan.getTjelesnaTezina());
        assertEquals(181.0, clan.getVisina());
        verify(nalogRepository).save(nalog);
        verify(clanRepository).save(clan);
    }

    @Test
    void promjenaGodineRodjenjaPostavljaPrviJanuar() {
        prijavljenKao(Uloga.MEMBER);

        servis.izmijeniProfil(5, zahtjev("Amar", "Hodžić", 2000, 80.0, 180.0));

        assertEquals(LocalDate.of(2000, 1, 1), clan.getDatumRodjenja());
    }

    @Test
    void nullVrijednostiBrisuGodinuTezinuIVisinu() {
        prijavljenKao(Uloga.MEMBER);

        servis.izmijeniProfil(5, zahtjev("Amar", "Hodžić", null, null, null));

        assertNull(clan.getDatumRodjenja());
        assertNull(clan.getTjelesnaTezina());
        assertNull(clan.getVisina());
    }

    @Test
    void trenerMijenjaSamoImeIPrezimeADrugaPoljaSeIgnorisu() {
        prijavljenKao(Uloga.TRAINER);

        // godina je neispravna, ali se za trenera ne provjerava jer se ne koristi
        servis.izmijeniProfil(5, zahtjev("Marko", "Petrović", 1500, -3.0, 9999.0));

        assertEquals("Marko", nalog.getIme());
        assertEquals("Petrović", nalog.getPrezime());
        verify(nalogRepository).save(nalog);
        verify(clanRepository, never()).save(any());
        verify(clanRepository, never()).findById(any());
    }

    @Test
    void praznoIliPreDugoImeJeNeispravno() {
        prijavljenKao(Uloga.MEMBER);
        String predugo = "a".repeat(46);

        assertStatus(400, () -> servis.izmijeniProfil(5, zahtjev(null, "Hodžić", 1998, 80.0, 180.0)));
        assertStatus(400, () -> servis.izmijeniProfil(5, zahtjev("   ", "Hodžić", 1998, 80.0, 180.0)));
        assertStatus(400, () -> servis.izmijeniProfil(5, zahtjev("Amar", "", 1998, 80.0, 180.0)));
        assertStatus(400, () -> servis.izmijeniProfil(5, zahtjev(predugo, "Hodžić", 1998, 80.0, 180.0)));
        assertEquals("Amar", nalog.getIme());
        verify(nalogRepository, never()).save(any());
    }

    @Test
    void imeOd45ZnakovaJeDozvoljeno() {
        prijavljenKao(Uloga.MEMBER);

        servis.izmijeniProfil(5, zahtjev("a".repeat(45), "Hodžić", 1998, 80.0, 180.0));

        assertEquals(45, nalog.getIme().length());
    }

    @Test
    void neispravneMjereClanaSuNeispravne() {
        prijavljenKao(Uloga.MEMBER);

        assertStatus(400, () -> servis.izmijeniProfil(5, zahtjev("A", "B", 1899, 80.0, 180.0)));
        // SAT je u 2026. godini, pa je 2027. u budućnosti
        assertStatus(400, () -> servis.izmijeniProfil(5, zahtjev("A", "B", 2027, 80.0, 180.0)));
        assertStatus(400, () -> servis.izmijeniProfil(5, zahtjev("A", "B", 1998, 0.0, 180.0)));
        assertStatus(400, () -> servis.izmijeniProfil(5, zahtjev("A", "B", 1998, -5.0, 180.0)));
        assertStatus(400, () -> servis.izmijeniProfil(5, zahtjev("A", "B", 1998, 501.0, 180.0)));
        assertStatus(400, () -> servis.izmijeniProfil(5, zahtjev("A", "B", 1998, 80.0, 0.0)));
        assertStatus(400, () -> servis.izmijeniProfil(5, zahtjev("A", "B", 1998, 80.0, 301.0)));
        verify(clanRepository, never()).save(any());
        verify(nalogRepository, never()).save(any());
    }

    @Test
    void granicneVrijednostiMjeraSuDozvoljene() {
        prijavljenKao(Uloga.MEMBER);

        servis.izmijeniProfil(5, zahtjev("A", "B", 1900, 500.0, 300.0));
        servis.izmijeniProfil(5, zahtjev("A", "B", 2026, 0.1, 0.1));

        assertEquals(LocalDate.of(2026, 1, 1), clan.getDatumRodjenja());
    }

    @Test
    void izmjenaNepostojecegNalogaJeNeovlascena() {
        when(nalogRepository.findById(99)).thenReturn(Optional.empty());

        assertStatus(401, () -> servis.izmijeniProfil(99, zahtjev("A", "B", 1998, 80.0, 180.0)));
    }
}
