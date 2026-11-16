package org.etf.pumpd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.ClanNaTerminu;
import org.etf.pumpd.entity.ClanNaTerminuId;
import org.etf.pumpd.entity.ClanUGrupi;
import org.etf.pumpd.entity.Grupa;
import org.etf.pumpd.entity.Termin;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.enums.StatusPrijave;
import org.etf.pumpd.repository.ClanNaTerminuRepository;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ClanUGrupiRepository;
import org.etf.pumpd.repository.GrupaRepository;
import org.etf.pumpd.repository.TerminRepository;
import org.etf.pumpd.repository.TrenerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

/**
 * Provjera mapiranja entiteta grupnih treninga nad pravom MySQL bazom sa seed podacima.
 * Svaki test se izvršava u transakciji koja se na kraju poništava, pa ne mijenja bazu
 * koju dijele drugi integracioni testovi.
 */
@SpringBootTest
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class GrupniTreninziEntitetiIntegracioniTest {

    @Autowired
    private GrupaRepository grupaRepository;
    @Autowired
    private TerminRepository terminRepository;
    @Autowired
    private ClanUGrupiRepository clanUGrupiRepository;
    @Autowired
    private ClanNaTerminuRepository prijavaRepository;
    @Autowired
    private TrenerRepository trenerRepository;
    @Autowired
    private ClanRepository clanRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    private int idNaloga(String email) {
        return jdbc.queryForObject("SELECT idnalog FROM nalog WHERE email = ?", Integer.class, email);
    }

    private int idSeedGrupe(String naziv) {
        return jdbc.queryForObject("SELECT idgrupa FROM grupa WHERE naziv = ?", Integer.class, naziv);
    }

    @Test
    void citaSeedTerminSaGrupomIClanovima() {
        int idGrupe = idSeedGrupe("Jutarnji trening");

        Termin termin = terminRepository.poGrupi(idGrupe).orElseThrow();

        assertEquals("Jutarnji trening", termin.getGrupa().getNaziv());
        assertEquals(10, termin.getGrupa().getMaxClanova());
        assertEquals(60, termin.getTrajanjeMinuta());
        assertEquals(LocalDate.now().plusDays(1).atTime(7, 0), termin.getVrijeme());
        assertTrue(termin.pripadaTreneru(idNaloga("trener1@pumpd.local")));

        // seed: clan1 i clan4 prijavljeni (i u grupi), clan3 čeka
        assertEquals(2, clanUGrupiRepository.brojClanova(idGrupe));
        List<ClanUGrupi> clanovi = clanUGrupiRepository.clanoviGrupe(idGrupe);
        assertEquals(2, clanovi.size());
        assertTrue(clanUGrupiRepository.idClanova(idGrupe).contains(idNaloga("clan1@pumpd.local")));

        List<ClanNaTerminu> cekaju = prijavaRepository.prijaveNaTermin(termin.getId(), EnumSet.of(StatusPrijave.NA_CEKANJU));
        assertEquals(1, cekaju.size());
        assertEquals(idNaloga("clan3@pumpd.local"), cekaju.get(0).getId().getClanId());
        assertNotNull(cekaju.get(0).getIdZahtjeva());

        List<ClanNaTerminu> prihvaceni = prijavaRepository.prijaveNaTermin(termin.getId(),
                EnumSet.of(StatusPrijave.PRIJAVLJEN, StatusPrijave.PRISUTAN));
        assertEquals(2, prihvaceni.size());
    }

    @Test
    void dnevniRasporedTreneraVracaTermineTogDanaPoVremenu() {
        int trener1 = idNaloga("trener1@pumpd.local");
        LocalDateTime sutra = LocalDate.now().plusDays(1).atStartOfDay();

        List<Termin> termini = terminRepository.terminiTreneraIzmedju(trener1, sutra, sutra.plusDays(1));

        assertTrue(termini.stream().anyMatch(t -> t.getGrupa().getNaziv().equals("Jutarnji trening")));
        assertTrue(termini.stream().allMatch(t -> t.pripadaTreneru(trener1)));
        assertTrue(terminRepository.terminiTrenera(trener1).size() >= 2);
    }

    @Test
    void upisujeNoviTerminGrupuClanaIPrijavu() {
        Trener trener = trenerRepository.findById(idNaloga("trener2@pumpd.local")).orElseThrow();
        Clan clan = clanRepository.findById(idNaloga("clan5@pumpd.local")).orElseThrow();
        LocalDateTime vrijeme = LocalDate.now().plusDays(400).atTime(10, 0);

        Grupa grupa = grupaRepository.save(new Grupa("Test grupa", 5, trener));
        Termin termin = terminRepository.save(new Termin(vrijeme, 45, trener, grupa));
        clanUGrupiRepository.save(new ClanUGrupi(clan, grupa, LocalDate.now()));
        ClanNaTerminu prijava = prijavaRepository.save(
                new ClanNaTerminu(clan, termin, StatusPrijave.NA_CEKANJU, LocalDate.now()));
        entityManager.flush();

        // requestId dodjeljuje baza, a Hibernate ga čita nakon upisa
        assertNotNull(prijava.getIdZahtjeva());
        assertEquals("na_cekanju", jdbc.queryForObject(
                "SELECT status FROM clan_na_terminu WHERE idzahtjev = ?", String.class, prijava.getIdZahtjeva()));

        entityManager.clear();
        ClanNaTerminu ucitana = prijavaRepository.findByIdZahtjeva(prijava.getIdZahtjeva()).orElseThrow();
        assertEquals(new ClanNaTerminuId(termin.getId(), clan.getId()), ucitana.getId());
        assertEquals(StatusPrijave.NA_CEKANJU, ucitana.getStatus());

        ucitana.postaviStatus(StatusPrijave.PRIJAVLJEN);
        entityManager.flush();
        assertEquals("prijavljen", jdbc.queryForObject(
                "SELECT status FROM clan_na_terminu WHERE idzahtjev = ?", String.class, prijava.getIdZahtjeva()));

        assertEquals(1, clanUGrupiRepository.brojClanova(grupa.getId()));
        assertEquals(grupa.getId(), grupaRepository.zakljucaj(grupa.getId()).orElseThrow().getId());
        assertEquals(termin.getId(), terminRepository.saGrupom(termin.getId()).orElseThrow().getId());
    }

    @Test
    void brisanjePrijavaIClanovaGrupe() {
        int idGrupe = idSeedGrupe("Snaga i izdržljivost");
        Termin termin = terminRepository.poGrupi(idGrupe).orElseThrow();

        assertEquals(2, prijavaRepository.obrisiSveZaTermin(termin.getId()));
        assertEquals(2, clanUGrupiRepository.obrisiSveIzGrupe(idGrupe));
        assertEquals(0, clanUGrupiRepository.brojClanova(idGrupe));
    }
}
