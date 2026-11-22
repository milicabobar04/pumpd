package org.etf.pumpd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Objava;
import org.etf.pumpd.entity.SacuvanaObjava;
import org.etf.pumpd.entity.SacuvanaObjavaId;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ObjavaRepository;
import org.etf.pumpd.repository.SacuvanaObjavaRepository;
import org.etf.pumpd.repository.TrenerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

/**
 * Provjera mapiranja entiteta Objava i SacuvanaObjava nad pravom MySQL bazom.
 * ddl-auto=none znači da Hibernate ne provjerava kolone pri pokretanju, pa se to radi ovdje.
 * Svaki test je u transakciji koja se na kraju poništava, pa ne mijenja zajedničku bazu.
 */
@SpringBootTest
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class ObjaveEntitetiIntegracioniTest {

    private static final LocalDateTime VRIJEME = LocalDateTime.of(2026, 11, 22, 10, 30, 0);

    @Autowired
    private ObjavaRepository objavaRepository;
    @Autowired
    private SacuvanaObjavaRepository sacuvanaObjavaRepository;
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

    private Trener trener1() {
        return trenerRepository.findById(idNaloga("trener1@pumpd.local")).orElseThrow();
    }

    private Clan clan1() {
        return clanRepository.findById(idNaloga("clan1@pumpd.local")).orElseThrow();
    }

    /** Upiše objavu, isprazni keš sesije i vrati id, da se sljedeće čitanje radi iz baze. */
    private int upisiObjavu(String sadrzaj, String slika) {
        Objava objava = objavaRepository.saveAndFlush(new Objava(trener1(), sadrzaj, slika, VRIJEME));
        entityManager.clear();
        return objava.getId();
    }

    @Test
    void objavaSeUpisujeICitaIzBaze() {
        int id = upisiObjavu("Novi trening nogu!", "/api/images/abc.jpg");

        Objava procitana = objavaRepository.findById(id).orElseThrow();

        assertEquals("Novi trening nogu!", procitana.getSadrzaj());
        assertEquals("/api/images/abc.jpg", procitana.getSlikaPath());
        assertEquals(VRIJEME, procitana.getVrijeme());
        assertEquals(idNaloga("trener1@pumpd.local"), procitana.getTrener().getNalog().getId());
    }

    @Test
    void objavaBezSlike() {
        int id = upisiObjavu("Bez slike.", null);

        assertNull(objavaRepository.findById(id).orElseThrow().getSlikaPath());
    }

    @Test
    void koloneObjaveUBazi() {
        int id = upisiObjavu("Provjera kolona.", "/api/images/x.png");

        Map<String, Object> red = jdbc.queryForMap(
                "SELECT sadrzaj, vrijeme, slikaPath, trener_nalog_idnalog FROM objava WHERE idobjava = ?", id);

        assertEquals("Provjera kolona.", red.get("sadrzaj"));
        assertEquals("/api/images/x.png", red.get("slikaPath"));
        assertEquals(idNaloga("trener1@pumpd.local"), red.get("trener_nalog_idnalog"));
        assertNotNull(red.get("vrijeme"));
    }

    @Test
    void sacuvanaObjavaSaSlozenimKljucem() {
        int idObjave = upisiObjavu("Za čuvanje.", null);
        Objava objava = objavaRepository.findById(idObjave).orElseThrow();

        sacuvanaObjavaRepository.saveAndFlush(new SacuvanaObjava(clan1(), objava, LocalDate.of(2026, 11, 22)));
        entityManager.clear();

        SacuvanaObjavaId kljuc = new SacuvanaObjavaId(idNaloga("clan1@pumpd.local"), idObjave);
        SacuvanaObjava procitana = sacuvanaObjavaRepository.findById(kljuc).orElseThrow();
        assertEquals(idObjave, procitana.getObjava().getId());
        assertEquals(idNaloga("clan1@pumpd.local"), procitana.getClan().getNalog().getId());
        assertEquals(LocalDate.of(2026, 11, 22), procitana.getDatumCuvanja());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM sacuvana_objava WHERE clan_nalog_idnalog = ? AND objava_idobjava = ?",
                Integer.class, kljuc.getClanId(), kljuc.getObjavaId()));
    }

    @Test
    void bazaOdbijaIstuObjavuSacuvanuDvaput() {
        int idObjave = upisiObjavu("Dvostruki klik.", null);
        int idClana = idNaloga("clan1@pumpd.local");
        String upis = "INSERT INTO sacuvana_objava (clan_nalog_idnalog, objava_idobjava, datumCuvanja) VALUES (?, ?, CURDATE())";
        jdbc.update(upis, idClana, idObjave);

        assertThrows(DuplicateKeyException.class, () -> jdbc.update(upis, idClana, idObjave));
    }

    @Test
    void ponovniSaveIstogKljucaBacaGresku() {
        // 8.2: SacuvanaObjava je Persistable, pa novi objekat ide kao INSERT (a ne merge, tiha izmjena).
        // Drugi upis istog para zato odbija primarni ključ u bazi, a servis tu grešku pretvara u 409.
        int idObjave = upisiObjavu("Dvaput.", null);
        Objava objava = objavaRepository.findById(idObjave).orElseThrow();
        sacuvanaObjavaRepository.saveAndFlush(new SacuvanaObjava(clan1(), objava, LocalDate.of(2026, 11, 20)));
        entityManager.clear();

        assertThrows(DataIntegrityViolationException.class, () -> sacuvanaObjavaRepository.saveAndFlush(
                new SacuvanaObjava(clan1(), objavaRepository.findById(idObjave).orElseThrow(),
                        LocalDate.of(2026, 11, 22))));
    }

    @Test
    void ucitanaSacuvanaObjavaNijeNova() {
        int idObjave = upisiObjavu("Nije nova.", null);
        SacuvanaObjava nova = new SacuvanaObjava(clan1(), objavaRepository.findById(idObjave).orElseThrow(),
                LocalDate.of(2026, 11, 22));
        assertEquals(true, nova.isNew());

        sacuvanaObjavaRepository.saveAndFlush(nova);
        assertEquals(false, nova.isNew(), "nakon upisa");
        entityManager.clear();

        SacuvanaObjavaId kljuc = new SacuvanaObjavaId(idNaloga("clan1@pumpd.local"), idObjave);
        assertEquals(false, sacuvanaObjavaRepository.findById(kljuc).orElseThrow().isNew(), "nakon učitavanja");
    }
}
