package org.etf.pumpd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.enums.OdlukaOZahtjevu;
import org.etf.pumpd.service.GrupniTreninziService;
import org.etf.pumpd.service.ZahtjeviZaGrupeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

/**
 * Integracioni test članove strane grupnih treninga (9.x) nad pravom MySQL bazom u Dockeru.
 * Svaki test pravi svog trenera i svoje članove, a pregled filtrira po tom treneru (?trainerId),
 * pa termini drugih testova i seed termini ne utiču na rezultat.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class GrupniTreninziIntegracioniTest {

    private static final String SEED_LOZINKA = "pumpd123";
    private static final AtomicInteger BROJAC = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private GrupniTreninziService grupniTreninziService;
    @Autowired
    private ZahtjeviZaGrupeService zahtjeviService;

    private String admin;
    private String radnik;
    private Korisnik trener;

    private record Korisnik(int id, String token) {
    }

    private record NoviTermin(int sessionId, int groupId) {
    }

    @BeforeEach
    void pripremi() throws Exception {
        admin = prijava("admin@pumpd.local", SEED_LOZINKA);
        radnik = prijava("radnik@pumpd.local", SEED_LOZINKA);
        trener = noviTrener("Grupni");
    }

    // ------------------------------------------------------------ pomoćne metode

    private String prijava(String email, String lozinka) throws Exception {
        String odgovor = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, lozinka)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.token");
    }

    private ResultActions zahtjev(MockHttpServletRequestBuilder builder, String token, String tijelo) throws Exception {
        builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (tijelo != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(tijelo);
        }
        return mockMvc.perform(builder);
    }

    private static String email(String prefiks) {
        return prefiks + "." + System.nanoTime() + "." + BROJAC.incrementAndGet() + "@mail.com";
    }

    private Korisnik noviTrener(String ime) throws Exception {
        String email = email("trener");
        String odgovor = zahtjev(post("/api/admin/trainers"), admin,
                "{\"name\":\"%s\",\"surname\":\"Trener\",\"email\":\"%s\"}".formatted(ime, email))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Korisnik(JsonPath.read(odgovor, "$.trainerId"), prijava(email, JsonPath.read(odgovor, "$.temporaryPassword")));
    }

    private Korisnik noviClan(String ime) throws Exception {
        String email = email("clan");
        String odgovor = zahtjev(post("/api/worker/members"), radnik,
                "{\"name\":\"%s\",\"surname\":\"Test\",\"email\":\"%s\"}".formatted(ime, email))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Korisnik(JsonPath.read(odgovor, "$.memberId"), prijava(email, JsonPath.read(odgovor, "$.temporaryPassword")));
    }

    /** Termin preko trenerove rute 10.5 (samo budući termini). */
    private NoviTermin noviTermin(Korisnik zaTrenera, String naziv, LocalDateTime pocetak, int kapacitet) throws Exception {
        String odgovor = zahtjev(post("/api/trainer/group-trainings"), zaTrenera.token(), """
                {"name":"%s","startTime":"%s","durationMinutes":45,"maxMembers":%d}
                """.formatted(naziv, pocetak, kapacitet))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new NoviTermin(JsonPath.read(odgovor, "$.sessionId"), JsonPath.read(odgovor, "$.groupId"));
    }

    /** Termin upisan direktno u bazu, za vrijeme koje 10.5 ne prima (prošlost). */
    private NoviTermin terminUBazi(String naziv, LocalDateTime pocetak) {
        int idGrupe = umetni("INSERT INTO grupa (naziv, maxClanova, trener_nalog_idnalog) VALUES (?, 10, ?)",
                naziv, trener.id());
        int idTermina = umetni("INSERT INTO termin (Vrijeme, Trajanje, trener_nalog_idnalog, grupa_idgrupa) VALUES (?, 60, ?, ?)",
                Timestamp.valueOf(pocetak), trener.id(), idGrupe);
        return new NoviTermin(idTermina, idGrupe);
    }

    private int umetni(String sql, Object... parametri) {
        GeneratedKeyHolder kljuc = new GeneratedKeyHolder();
        jdbc.update(veza -> {
            PreparedStatement upit = veza.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < parametri.length; i++) {
                upit.setObject(i + 1, parametri[i]);
            }
            return upit;
        }, kljuc);
        return kljuc.getKey().intValue();
    }

    /** Prijava člana na termin sa datim statusom (9.2 još ne postoji). */
    private void prijavaUBazi(NoviTermin termin, Korisnik clan, String status) {
        jdbc.update("INSERT INTO clan_na_terminu (termin_idtermin, clan_nalog_idnalog, status, datumZahtjeva) "
                + "VALUES (?, ?, ?, CURDATE())", termin.sessionId(), clan.id(), status);
        if (status.equals("prijavljen")) {
            jdbc.update("INSERT INTO clan_u_grupi (clan_nalog_idnalog, grupa_idgrupa, datumPristupanja) "
                    + "VALUES (?, ?, CURDATE())", clan.id(), termin.groupId());
        }
    }

    private ResultActions posalji(NoviTermin termin, Korisnik clan) throws Exception {
        return zahtjev(post("/api/group-trainings/" + termin.sessionId() + "/enrollment"), clan.token(), null);
    }

    private ResultActions odjavi(NoviTermin termin, Korisnik clan) throws Exception {
        return zahtjev(delete("/api/group-trainings/" + termin.sessionId() + "/enrollment"), clan.token(), null);
    }

    private List<Map<String, Object>> redovi(NoviTermin termin, Korisnik clan) {
        return jdbc.queryForList("SELECT idzahtjev, status, datumZahtjeva FROM clan_na_terminu "
                + "WHERE termin_idtermin = ? AND clan_nalog_idnalog = ?", termin.sessionId(), clan.id());
    }

    private int uGrupi(NoviTermin termin, Korisnik clan) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM clan_u_grupi WHERE grupa_idgrupa = ? AND clan_nalog_idnalog = ?",
                Integer.class, termin.groupId(), clan.id());
    }

    private List<String> obavjestenja(int idPrimaoca) {
        return jdbc.queryForList("SELECT sadrzaj FROM obavjestenje WHERE primalac_nalog_idnalog = ? ORDER BY idobavjestenje",
                String.class, idPrimaoca);
    }

    /** 9.2 preko rute; vraća requestId. */
    private int zahtjevZa(NoviTermin termin, Korisnik clan) throws Exception {
        return JsonPath.read(posalji(termin, clan).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.requestId");
    }

    private ResultActions obradi(Korisnik zaTrenera, int idZahtjeva, String odluka) throws Exception {
        return zahtjev(patch("/api/trainer/group-requests/" + idZahtjeva), zaTrenera.token(), "{\"status\":\"" + odluka + "\"}");
    }

    private ResultActions zahtjeviTrenera(Korisnik zaTrenera, String upit) throws Exception {
        return zahtjev(get("/api/trainer/group-requests" + upit), zaTrenera.token(), null).andExpect(status().isOk());
    }

    private ResultActions pregled(String token, String upit) throws Exception {
        return zahtjev(get("/api/group-trainings" + upit), token, null).andExpect(status().isOk());
    }

    private List<Integer> idTermina(String token, String upit) throws Exception {
        return JsonPath.read(pregled(token, upit).andReturn().getResponse().getContentAsString(), "$[*].sessionId");
    }

    // ------------------------------------------------------------ testovi

    @Test
    void clanVidiPodatkeTerminaIBrojPrijavljenih() throws Exception {
        LocalDateTime pocetak = LocalDate.now().plusDays(2).atTime(18, 30);
        NoviTermin termin = noviTermin(trener, "Pilates", pocetak, 3);
        prijavaUBazi(termin, noviClan("Prvi"), "prijavljen");
        prijavaUBazi(termin, noviClan("Drugi"), "prijavljen");
        prijavaUBazi(termin, noviClan("Treci"), "na_cekanju");   // zahtjev na čekanju ne zauzima mjesto

        pregled(noviClan("Gost").token(), "?trainerId=" + trener.id())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sessionId").value(termin.sessionId()))
                .andExpect(jsonPath("$[0].groupId").value(termin.groupId()))
                .andExpect(jsonPath("$[0].name").value("Pilates"))
                .andExpect(jsonPath("$[0].trainerId").value(trener.id()))
                .andExpect(jsonPath("$[0].trainerName").value("Grupni Trener"))
                .andExpect(jsonPath("$[0].startTime").value(pocetak + ":00"))
                .andExpect(jsonPath("$[0].durationMinutes").value(45))
                .andExpect(jsonPath("$[0].enrolledCount").value(2))
                .andExpect(jsonPath("$[0].maxMembers").value(3))
                .andExpect(jsonPath("$[0].enrollmentStatus").value("NONE"));
    }

    @Test
    void statusPrijaveJeZaSvakogClanaSvoj() throws Exception {
        LocalDate dan = LocalDate.now().plusDays(3);
        NoviTermin prvi = noviTermin(trener, "Prvi", dan.atTime(8, 0), 10);
        NoviTermin drugi = noviTermin(trener, "Drugi", dan.atTime(10, 0), 10);
        NoviTermin treci = noviTermin(trener, "Treci", dan.atTime(12, 0), 10);
        NoviTermin cetvrti = noviTermin(trener, "Cetvrti", dan.atTime(14, 0), 10);
        NoviTermin peti = noviTermin(trener, "Peti", dan.atTime(16, 0), 10);
        Korisnik clan = noviClan("Status");
        Korisnik drugiClan = noviClan("Drugi");
        prijavaUBazi(prvi, clan, "na_cekanju");
        prijavaUBazi(drugi, clan, "prijavljen");
        prijavaUBazi(treci, clan, "odbijen");
        prijavaUBazi(cetvrti, clan, "otkazao");
        jdbc.update("UPDATE clan_na_terminu SET status = 'prisutan' WHERE termin_idtermin = ? AND clan_nalog_idnalog = ?",
                drugi.sessionId(), clan.id());
        prijavaUBazi(peti, drugiClan, "prijavljen");

        pregled(clan.token(), "?trainerId=" + trener.id())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].enrollmentStatus").value("PENDING"))
                .andExpect(jsonPath("$[1].enrollmentStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$[2].enrollmentStatus").value("REJECTED"))
                .andExpect(jsonPath("$[3].enrollmentStatus").value("NONE"))   // otkazao = može ponovo
                .andExpect(jsonPath("$[4].enrollmentStatus").value("NONE"))   // prijava drugog člana
                .andExpect(jsonPath("$[4].enrolledCount").value(1));
        pregled(drugiClan.token(), "?trainerId=" + trener.id())
                .andExpect(jsonPath("$[0].enrollmentStatus").value("NONE"))
                .andExpect(jsonPath("$[4].enrollmentStatus").value("ACCEPTED"));
    }

    @Test
    void radnikVidiTermineBezStatusaPrijave() throws Exception {
        NoviTermin termin = noviTermin(trener, "Za radnika", LocalDate.now().plusDays(1).atTime(9, 0), 5);
        prijavaUBazi(termin, noviClan("Prijavljen"), "prijavljen");

        pregled(radnik, "?trainerId=" + trener.id())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sessionId").value(termin.sessionId()))
                .andExpect(jsonPath("$[0].enrolledCount").value(1))
                .andExpect(jsonPath("$[0].enrollmentStatus").doesNotExist());
    }

    @Test
    void bezDatumaOdDanasNadaljeSortiranoPoVremenu() throws Exception {
        NoviTermin jucer = terminUBazi("Jucer", LocalDate.now().minusDays(1).atTime(18, 0));
        NoviTermin danasRano = terminUBazi("Danas rano", LocalDate.now().atStartOfDay());
        NoviTermin zaSedamDana = noviTermin(trener, "Za sedam dana", LocalDate.now().plusDays(7).atTime(9, 0), 5);
        NoviTermin sutra = noviTermin(trener, "Sutra", LocalDate.now().plusDays(1).atTime(9, 0), 5);

        List<Integer> termini = idTermina(radnik, "?trainerId=" + trener.id());

        assertEquals(
                List.of(danasRano.sessionId(), sutra.sessionId(), zaSedamDana.sessionId()), termini);
        assertFalse(termini.contains(jucer.sessionId()));
    }

    @Test
    void filterPoDatumuVracaSamoTajDanUkljucujuciProsli() throws Exception {
        LocalDate dan = LocalDate.now().plusDays(4);
        NoviTermin pocetakDana = terminUBazi("Ponoc", dan.atStartOfDay());
        NoviTermin uvece = noviTermin(trener, "Uvece", dan.atTime(23, 0), 5);
        noviTermin(trener, "Sljedeci dan", dan.plusDays(1).atStartOfDay(), 5);
        noviTermin(trener, "Prethodni dan", dan.minusDays(1).atTime(23, 0), 5);
        NoviTermin jucer = terminUBazi("Jucer", LocalDate.now().minusDays(1).atTime(7, 0));

        assertEquals(List.of(pocetakDana.sessionId(), uvece.sessionId()),
                idTermina(radnik, "?trainerId=" + trener.id() + "&date=" + dan));
        // sa datumom se vide i prošli termini tog dana
        assertEquals(List.of(jucer.sessionId()),
                idTermina(radnik, "?trainerId=" + trener.id() + "&date=" + LocalDate.now().minusDays(1)));
    }

    @Test
    void filterPoTreneruIBezFilteraVidiSveTrenere() throws Exception {
        Korisnik drugiTrener = noviTrener("Drugi");
        LocalDate dan = LocalDate.now().plusDays(5);
        NoviTermin moj = noviTermin(trener, "Moj", dan.atTime(10, 0), 5);
        NoviTermin tudji = noviTermin(drugiTrener, "Tudji", dan.atTime(10, 0), 5);

        assertEquals(List.of(moj.sessionId()), idTermina(radnik, "?trainerId=" + trener.id()));
        assertEquals(List.of(tudji.sessionId()), idTermina(radnik, "?trainerId=" + drugiTrener.id()));
        List<Integer> svi = idTermina(radnik, "?date=" + dan);
        assertTrue(svi.containsAll(List.of(moj.sessionId(), tudji.sessionId())), svi.toString());
        assertEquals(List.of(), idTermina(radnik, "?trainerId=999999"));
    }

    @Test
    void terminiObrisanogTreneraSeNePrikazuju() throws Exception {
        Korisnik obrisani = noviTrener("Obrisani");
        LocalDate dan = LocalDate.now().plusDays(6);
        noviTermin(obrisani, "Obrisanog", dan.atTime(11, 0), 5);
        zahtjev(delete("/api/admin/trainers/" + obrisani.id()), admin, null).andExpect(status().isOk());

        assertEquals(List.of(), idTermina(radnik, "?trainerId=" + obrisani.id()));
        assertFalse(idTermina(radnik, "?date=" + dan).stream()
                .anyMatch(id -> jdbc.queryForObject("SELECT trener_nalog_idnalog FROM termin WHERE idtermin = ?",
                        Integer.class, id) == obrisani.id()));
    }

    @Test
    void pristupPoUlogama() throws Exception {
        zahtjev(get("/api/group-trainings"), trener.token(), null).andExpect(status().isForbidden());
        zahtjev(get("/api/group-trainings"), admin, null).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/group-trainings")).andExpect(status().isUnauthorized());
        zahtjev(get("/api/group-trainings"), prijava("clan1@pumpd.local", SEED_LOZINKA), null).andExpect(status().isOk());
        zahtjev(get("/api/group-trainings?date=sutra"), radnik, null).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 9.2 i 9.3

    @Test
    void zahtjevSeUpisujeSaIdZahtjevaIzBazeIObavjestavaTrenera() throws Exception {
        NoviTermin termin = noviTermin(trener, "Kros", LocalDate.now().plusDays(2).atTime(17, 0), 5);
        Korisnik clan = noviClan("Petra");

        String odgovor = posalji(termin, clan)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Zahtjev za prijavu je poslat."))
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> red = redovi(termin, clan);
        assertEquals(1, red.size());
        // requestId dodjeljuje baza (AUTO_INCREMENT van primarnog ključa) i mora stići u odgovor
        assertEquals(((Number) red.get(0).get("idzahtjev")).intValue(), (int) JsonPath.read(odgovor, "$.requestId"));
        assertEquals("na_cekanju", red.get(0).get("status"));
        assertEquals(Date.valueOf(LocalDate.now()), red.get(0).get("datumZahtjeva"));
        assertEquals(0, uGrupi(termin, clan), "zahtjev na čekanju ne upisuje člana u grupu");
        assertEquals(List.of("Petra Test je poslao zahtjev za prijavu na termin \"Kros\" ("
                + DateTimeFormatter.ofPattern("dd.MM.yyyy.").format(LocalDate.now().plusDays(2))
                + " u 17:00)."), obavjestenja(trener.id()));

        pregled(clan.token(), "?trainerId=" + trener.id()).andExpect(jsonPath("$[0].enrollmentStatus").value("PENDING"));
        posalji(termin, clan).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Zahtjev je vec poslat."));
        assertEquals(1, redovi(termin, clan).size());
    }

    @Test
    void odbijenZahtjevSeMozePonovitiSaIstimIdZahtjeva() throws Exception {
        NoviTermin termin = noviTermin(trener, "Ponovo", LocalDate.now().plusDays(2).atTime(19, 0), 5);
        Korisnik clan = noviClan("Odbijeni");
        int prvi = JsonPath.read(posalji(termin, clan).andReturn().getResponse().getContentAsString(), "$.requestId");
        jdbc.update("UPDATE clan_na_terminu SET status = 'odbijen', datumZahtjeva = CURDATE() - INTERVAL 3 DAY "
                + "WHERE idzahtjev = ?", prvi);
        pregled(clan.token(), "?trainerId=" + trener.id()).andExpect(jsonPath("$[0].enrollmentStatus").value("REJECTED"));

        posalji(termin, clan).andExpect(status().isCreated()).andExpect(jsonPath("$.requestId").value(prvi));

        assertEquals("na_cekanju", redovi(termin, clan).get(0).get("status"));
        assertEquals(Date.valueOf(LocalDate.now()), redovi(termin, clan).get(0).get("datumZahtjeva"));
        assertEquals(2, obavjestenja(trener.id()).size());
    }

    @Test
    void prijavljenClanIPunTerminSu409() throws Exception {
        NoviTermin termin = noviTermin(trener, "Malo mjesta", LocalDate.now().plusDays(3).atTime(9, 0), 1);
        Korisnik prijavljen = noviClan("Prijavljen");
        prijavaUBazi(termin, prijavljen, "prijavljen");

        posalji(termin, prijavljen).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Vec ste prijavljeni na termin."));
        posalji(termin, noviClan("Kasni")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Termin je popunjen."));
        assertEquals(List.of(), obavjestenja(trener.id()));
    }

    @Test
    void zahtjeviNaCekanjuNeZauzimajuMjesto() throws Exception {
        NoviTermin termin = noviTermin(trener, "Jedno mjesto", LocalDate.now().plusDays(3).atTime(11, 0), 1);

        posalji(termin, noviClan("Prvi")).andExpect(status().isCreated());
        posalji(termin, noviClan("Drugi")).andExpect(status().isCreated());   // trener bira kome daje mjesto
    }

    @Test
    void prosliINepostojeciTerminINeaktivanTrener() throws Exception {
        Korisnik clan = noviClan("Kasno");
        posalji(terminUBazi("Jucer", LocalDate.now().minusDays(1).atTime(10, 0)), clan)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Termin je vec poceo."));
        posalji(new NoviTermin(999999, 0), clan)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Termin ne postoji."));

        Korisnik obrisani = noviTrener("Obrisani");
        NoviTermin terminObrisanog = noviTermin(obrisani, "Obrisanog", LocalDate.now().plusDays(4).atTime(8, 0), 5);
        zahtjev(delete("/api/admin/trainers/" + obrisani.id()), admin, null).andExpect(status().isOk());
        posalji(terminObrisanog, clan).andExpect(status().isNotFound());
    }

    @Test
    void istovremeniZahtjeviIstogClanaDajuJedanRed() throws Exception {
        NoviTermin termin = noviTermin(trener, "Dvoklik", LocalDate.now().plusDays(5).atTime(10, 0), 5);
        Korisnik clan = noviClan("Brzi");
        int pokusaja = 4;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService niti = Executors.newFixedThreadPool(pokusaja);
        try {
            List<Future<Boolean>> rezultati = new ArrayList<>();
            for (int i = 0; i < pokusaja; i++) {
                rezultati.add(niti.submit(() -> {
                    start.await();
                    try {
                        grupniTreninziService.posaljiZahtjev(termin.sessionId(), clan.id());
                        return true;
                    } catch (KonfliktException e) {
                        return false;
                    }
                }));
            }
            start.countDown();
            int uspjesnih = 0;
            for (Future<Boolean> rezultat : rezultati) {
                uspjesnih += rezultat.get(30, TimeUnit.SECONDS) ? 1 : 0;
            }
            assertEquals(1, uspjesnih);
        } finally {
            niti.shutdownNow();
        }
        assertEquals(1, redovi(termin, clan).size());
        assertEquals(1, obavjestenja(trener.id()).size());
    }

    @Test
    void povlacenjeZahtjevaNaCekanju() throws Exception {
        NoviTermin termin = noviTermin(trener, "Povlacenje", LocalDate.now().plusDays(2).atTime(8, 0), 5);
        Korisnik clan = noviClan("Predomislio");
        posalji(termin, clan).andExpect(status().isCreated());

        odjavi(termin, clan).andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Zahtjev za prijavu je povucen."));

        assertEquals(List.of(), redovi(termin, clan));
        pregled(clan.token(), "?trainerId=" + trener.id()).andExpect(jsonPath("$[0].enrollmentStatus").value("NONE"));
        assertEquals(1, obavjestenja(trener.id()).size(), "povlačenje ne šalje novo obavještenje");
        odjavi(termin, clan).andExpect(status().isNotFound());
    }

    @Test
    void odjavaPrihvacenogClanaOslobadjaMjesto() throws Exception {
        NoviTermin termin = noviTermin(trener, "Odjava", LocalDate.now().plusDays(2).atTime(20, 0), 1);
        Korisnik clan = noviClan("Odlazi");
        prijavaUBazi(termin, clan, "prijavljen");
        Korisnik drugi = noviClan("Ceka");
        posalji(termin, drugi).andExpect(status().isConflict());   // pun termin

        odjavi(termin, clan).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Odjavljeni ste sa termina."));

        assertEquals(List.of(), redovi(termin, clan));
        assertEquals(0, uGrupi(termin, clan));
        assertEquals(1, obavjestenja(trener.id()).size());
        assertTrue(obavjestenja(trener.id()).get(0).startsWith("Odlazi Test se odjavio sa termina \"Odjava\""));
        pregled(radnik, "?trainerId=" + trener.id()).andExpect(jsonPath("$[0].enrolledCount").value(0));
        posalji(termin, drugi).andExpect(status().isCreated());   // mjesto je slobodno
    }

    @Test
    void odjavaNakonPocetkaINepostojecaPrijava() throws Exception {
        NoviTermin prosli = terminUBazi("Prosli", LocalDate.now().minusDays(2).atTime(18, 0));
        Korisnik clan = noviClan("Istorija");
        prijavaUBazi(prosli, clan, "prijavljen");

        odjavi(prosli, clan).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Termin je vec poceo, odjava nije moguca."));
        assertEquals(1, redovi(prosli, clan).size());
        assertEquals(1, uGrupi(prosli, clan));

        NoviTermin buduci = noviTermin(trener, "Bez prijave", LocalDate.now().plusDays(2).atTime(6, 0), 5);
        odjavi(buduci, clan).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Niste prijavljeni na termin niti imate zahtjev na cekanju."));
        odjavi(new NoviTermin(999999, 0), clan).andExpect(status().isNotFound());
        prijavaUBazi(buduci, clan, "odbijen");
        odjavi(buduci, clan).andExpect(status().isNotFound());
    }

    @Test
    void prijavaIOdjavaSamoZaClana() throws Exception {
        NoviTermin termin = noviTermin(trener, "Uloge", LocalDate.now().plusDays(2).atTime(7, 0), 5);
        for (String token : List.of(trener.token(), radnik, admin)) {
            zahtjev(post("/api/group-trainings/" + termin.sessionId() + "/enrollment"), token, null)
                    .andExpect(status().isForbidden());
            zahtjev(delete("/api/group-trainings/" + termin.sessionId() + "/enrollment"), token, null)
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/group-trainings/" + termin.sessionId() + "/enrollment")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------ 10.12 i 10.13

    @Test
    void trenerVidiZahtjeveNaCekanjuZaSvojeBuduceTermine() throws Exception {
        LocalDateTime sutra = LocalDate.now().plusDays(1).atTime(18, 0);
        NoviTermin kasniji = noviTermin(trener, "Kasniji", sutra.plusDays(1), 4);
        NoviTermin raniji = noviTermin(trener, "Raniji", sutra, 6);
        Korisnik ana = noviClan("Ana");
        Korisnik bora = noviClan("Bora");
        int zaKasniji = zahtjevZa(kasniji, ana);
        int boraRaniji = zahtjevZa(raniji, bora);
        int anaRaniji = zahtjevZa(raniji, ana);
        jdbc.update("UPDATE clan_na_terminu SET datumZahtjeva = CURDATE() - INTERVAL 2 DAY WHERE idzahtjev = ?", anaRaniji);
        prijavaUBazi(raniji, noviClan("Vec"), "prijavljen");
        prijavaUBazi(raniji, noviClan("Odbijen"), "odbijen");                       // nije na čekanju
        NoviTermin prosli = terminUBazi("Prosli", LocalDate.now().minusDays(1).atTime(9, 0));
        prijavaUBazi(prosli, noviClan("Zakasnio"), "na_cekanju");                 // termin je prošao
        NoviTermin tudji = noviTermin(noviTrener("Drugi"), "Tudji", sutra, 5);
        zahtjevZa(tudji, ana);                                                     // zahtjev drugom treneru

        zahtjeviTrenera(trener, "")
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].requestId").value(anaRaniji))           // isti termin: stariji zahtjev prvi
                .andExpect(jsonPath("$[0].memberId").value(ana.id()))
                .andExpect(jsonPath("$[0].memberName").value("Ana Test"))
                .andExpect(jsonPath("$[0].sessionId").value(raniji.sessionId()))
                .andExpect(jsonPath("$[0].sessionName").value("Raniji"))
                .andExpect(jsonPath("$[0].startTime").value(sutra + ":00"))
                .andExpect(jsonPath("$[0].enrolledCount").value(1))
                .andExpect(jsonPath("$[0].maxMembers").value(6))
                .andExpect(jsonPath("$[0].requestDate").value(LocalDate.now().minusDays(2).toString()))
                .andExpect(jsonPath("$[1].requestId").value(boraRaniji))
                .andExpect(jsonPath("$[2].requestId").value(zaKasniji))
                .andExpect(jsonPath("$[2].enrolledCount").value(0));
        zahtjeviTrenera(trener, "?sessionId=" + kasniji.sessionId())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].requestId").value(zaKasniji));
        zahtjeviTrenera(trener, "?sessionId=" + tudji.sessionId()).andExpect(jsonPath("$.length()").value(0));

        zahtjev(delete("/api/worker/members/" + bora.id()), radnik, null).andExpect(status().isOk());
        zahtjeviTrenera(trener, "?sessionId=" + raniji.sessionId()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void prihvatanjeUpisujeClanaUGrupuIObavjestavaGa() throws Exception {
        NoviTermin termin = noviTermin(trener, "Prihvatanje", LocalDate.now().plusDays(2).atTime(16, 0), 3);
        Korisnik clan = noviClan("Prihvacen");
        int idZahtjeva = zahtjevZa(termin, clan);

        obradi(trener, idZahtjeva, "ACCEPTED").andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Zahtjev je prihvacen."));

        assertEquals("prijavljen", redovi(termin, clan).get(0).get("status"));
        assertEquals(1, uGrupi(termin, clan));
        assertTrue(obavjestenja(clan.id()).get(0).startsWith("Trener je prihvatio vas zahtjev za prijavu na termin \"Prihvatanje\""));
        pregled(clan.token(), "?trainerId=" + trener.id())
                .andExpect(jsonPath("$[0].enrollmentStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$[0].enrolledCount").value(1));
        // član je vidljiv i u B1 pregledu grupe (10.9)
        zahtjev(get("/api/trainer/groups/" + termin.groupId() + "/members"), trener.token(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].memberId").value(clan.id()));
        zahtjeviTrenera(trener, "").andExpect(jsonPath("$.length()").value(0));

        obradi(trener, idZahtjeva, "REJECTED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Zahtjev je vec obradjen."));
        // nakon prihvatanja član se može odjaviti (9.3)
        odjavi(termin, clan).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Odjavljeni ste sa termina."));
    }

    @Test
    void odbijanjePostavljaRejectedIClanMozePonovo() throws Exception {
        NoviTermin termin = noviTermin(trener, "Odbijanje", LocalDate.now().plusDays(2).atTime(14, 0), 3);
        Korisnik clan = noviClan("Odbijena");
        int idZahtjeva = zahtjevZa(termin, clan);

        obradi(trener, idZahtjeva, "REJECTED").andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Zahtjev je odbijen."));

        assertEquals("odbijen", redovi(termin, clan).get(0).get("status"));
        assertEquals(0, uGrupi(termin, clan));
        assertTrue(obavjestenja(clan.id()).get(0).startsWith("Trener je odbio vas zahtjev"));
        pregled(clan.token(), "?trainerId=" + trener.id()).andExpect(jsonPath("$[0].enrollmentStatus").value("REJECTED"));
        obradi(trener, idZahtjeva, "ACCEPTED").andExpect(status().isConflict());

        assertEquals(idZahtjeva, zahtjevZa(termin, clan));   // novi zahtjev, isti requestId
        obradi(trener, idZahtjeva, "ACCEPTED").andExpect(status().isOk());
    }

    @Test
    void punaGrupaTudjiINepostojeciZahtjev() throws Exception {
        NoviTermin termin = noviTermin(trener, "Jedno mjesto", LocalDate.now().plusDays(3).atTime(12, 0), 1);
        int prvi = zahtjevZa(termin, noviClan("Prvi"));
        int drugi = zahtjevZa(termin, noviClan("Drugi"));
        obradi(trener, prvi, "ACCEPTED").andExpect(status().isOk());

        obradi(trener, drugi, "ACCEPTED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Grupa je popunjena."));
        obradi(trener, drugi, "REJECTED").andExpect(status().isOk());   // odbiti se može i kad je puna

        int treci = zahtjevZa(noviTermin(trener, "Drugi termin", LocalDate.now().plusDays(3).atTime(15, 0), 5), noviClan("Treci"));
        obradi(noviTrener("Tudji"), treci, "ACCEPTED").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Zahtjev ne postoji."));
        obradi(trener, 999999, "ACCEPTED").andExpect(status().isNotFound());
        obradi(trener, treci, "PENDING").andExpect(status().isBadRequest());
    }

    @Test
    void zahtjevZaTerminKojiJePoceoIPovucenZahtjev() throws Exception {
        NoviTermin prosli = terminUBazi("Prosli", LocalDate.now().minusDays(1).atTime(9, 0));
        Korisnik clan = noviClan("Kasno");
        prijavaUBazi(prosli, clan, "na_cekanju");
        int idZahtjeva = ((Number) redovi(prosli, clan).get(0).get("idzahtjev")).intValue();
        obradi(trener, idZahtjeva, "ACCEPTED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Termin je vec poceo."));

        NoviTermin buduci = noviTermin(trener, "Povucen", LocalDate.now().plusDays(2).atTime(11, 0), 5);
        int povucen = zahtjevZa(buduci, clan);
        odjavi(buduci, clan).andExpect(status().isOk());
        obradi(trener, povucen, "ACCEPTED").andExpect(status().isNotFound());
    }

    @Test
    void istovremenaPrihvatanjaZaPosljednjeMjestoPrimajuJednog() throws Exception {
        NoviTermin termin = noviTermin(trener, "Posljednje mjesto", LocalDate.now().plusDays(4).atTime(10, 0), 1);
        int pokusaja = 4;
        List<Integer> zahtjevi = new ArrayList<>();
        for (int i = 0; i < pokusaja; i++) {
            zahtjevi.add(zahtjevZa(termin, noviClan("Kandidat" + i)));
        }
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService niti = Executors.newFixedThreadPool(pokusaja);
        try {
            List<Future<Boolean>> rezultati = new ArrayList<>();
            for (int idZahtjeva : zahtjevi) {
                rezultati.add(niti.submit(() -> {
                    start.await();
                    try {
                        zahtjeviService.obradi(trener.id(), idZahtjeva, OdlukaOZahtjevu.ACCEPTED);
                        return true;
                    } catch (KonfliktException e) {
                        return false;
                    }
                }));
            }
            start.countDown();
            int uspjesnih = 0;
            for (Future<Boolean> rezultat : rezultati) {
                uspjesnih += rezultat.get(30, TimeUnit.SECONDS) ? 1 : 0;
            }
            assertEquals(1, uspjesnih);
        } finally {
            niti.shutdownNow();
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM clan_u_grupi WHERE grupa_idgrupa = ?",
                Integer.class, termin.groupId()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM clan_na_terminu WHERE termin_idtermin = ? "
                + "AND status = 'prijavljen'", Integer.class, termin.sessionId()));
    }

    @Test
    void istovremenoDvaPutaIstiZahtjevUpisujeJednom() throws Exception {
        NoviTermin termin = noviTermin(trener, "Dvoklik trenera", LocalDate.now().plusDays(4).atTime(13, 0), 5);
        Korisnik clan = noviClan("Jedan");
        int idZahtjeva = zahtjevZa(termin, clan);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService niti = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> rezultati = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                rezultati.add(niti.submit(() -> {
                    start.await();
                    try {
                        zahtjeviService.obradi(trener.id(), idZahtjeva, OdlukaOZahtjevu.ACCEPTED);
                        return true;
                    } catch (KonfliktException e) {
                        return false;
                    }
                }));
            }
            start.countDown();
            assertEquals(1, (rezultati.get(0).get(30, TimeUnit.SECONDS) ? 1 : 0) + (rezultati.get(1).get(30, TimeUnit.SECONDS) ? 1 : 0));
        } finally {
            niti.shutdownNow();
        }
        assertEquals(1, uGrupi(termin, clan));
        assertEquals(1, obavjestenja(clan.id()).size());
    }

    @Test
    void obradaZahtjevaSamoZaTrenera() throws Exception {
        NoviTermin termin = noviTermin(trener, "Uloge obrade", LocalDate.now().plusDays(2).atTime(21, 0), 5);
        Korisnik clan = noviClan("Uloga");
        int idZahtjeva = zahtjevZa(termin, clan);
        for (String token : List.of(clan.token(), radnik, admin)) {
            zahtjev(get("/api/trainer/group-requests"), token, null).andExpect(status().isForbidden());
            zahtjev(patch("/api/trainer/group-requests/" + idZahtjeva), token, "{\"status\":\"ACCEPTED\"}")
                    .andExpect(status().isForbidden());
        }
        assertEquals("na_cekanju", redovi(termin, clan).get(0).get("status"));
    }

    // ------------------------------------------------------------ 2.4

    private void dolazak(Korisnik clan, LocalDateTime ulazak, LocalDateTime izlazak) {
        jdbc.update("INSERT INTO dolazak (clan_nalog_idnalog, vrijemeUlaska, vrijemeIzlaska) VALUES (?, ?, ?)",
                clan.id(), Timestamp.valueOf(ulazak), izlazak == null ? null : Timestamp.valueOf(izlazak));
    }

    private ResultActions istorija(Korisnik clan) throws Exception {
        return zahtjev(get("/api/users/training-history"), clan.token(), null).andExpect(status().isOk());
    }

    @Test
    void istorijaSpajaGrupneTermineIDolaske() throws Exception {
        Korisnik clan = noviClan("Istorija");
        LocalDate danas = LocalDate.now();
        NoviTermin prijeTriDana = terminUBazi("Joga", danas.minusDays(3).atTime(18, 0));
        prijavaUBazi(prijeTriDana, clan, "prijavljen");
        NoviTermin prisutan = terminUBazi("Kros", danas.minusDays(6).atTime(8, 0));
        prijavaUBazi(prisutan, clan, "prijavljen");
        jdbc.update("UPDATE clan_na_terminu SET status = 'prisutan' WHERE termin_idtermin = ?", prisutan.sessionId());
        prijavaUBazi(terminUBazi("Odbijen", danas.minusDays(4).atTime(9, 0)), clan, "odbijen");
        prijavaUBazi(terminUBazi("Na cekanju", danas.minusDays(4).atTime(12, 0)), clan, "na_cekanju");
        prijavaUBazi(noviTermin(trener, "Buduci", danas.plusDays(1).atTime(9, 0), 5), clan, "prijavljen");

        dolazak(clan, danas.minusDays(3).atTime(17, 45), danas.minusDays(3).atTime(19, 15));   // došla na Jogu
        dolazak(clan, danas.minusDays(3).atTime(7, 0), danas.minusDays(3).atTime(8, 0));       // isti dan ujutru, sama
        dolazak(clan, danas.minusDays(2).atTime(7, 0), danas.minusDays(2).atTime(8, 30));
        dolazak(clan, danas.minusDays(10).atTime(20, 0), null);                                   // zaboravljen izlazak
        dolazak(noviClan("Drugi"), danas.minusDays(1).atTime(7, 0), danas.minusDays(1).atTime(8, 0));
        int solo = jdbc.queryForObject("SELECT iddolazak FROM dolazak WHERE clan_nalog_idnalog = ? AND vrijemeUlaska = ?",
                Integer.class, clan.id(), Timestamp.valueOf(danas.minusDays(2).atTime(7, 0)));
        int jutarnji = jdbc.queryForObject("SELECT iddolazak FROM dolazak WHERE clan_nalog_idnalog = ? AND vrijemeUlaska = ?",
                Integer.class, clan.id(), Timestamp.valueOf(danas.minusDays(3).atTime(7, 0)));

        istorija(clan)
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].trainingId").value(solo))
                .andExpect(jsonPath("$[0].name").value("Samostalni trening"))
                .andExpect(jsonPath("$[0].date").value(danas.minusDays(2).toString()))
                .andExpect(jsonPath("$[0].type").value("SOLO"))
                .andExpect(jsonPath("$[1].trainingId").value(prijeTriDana.sessionId()))
                .andExpect(jsonPath("$[1].name").value("Joga"))
                .andExpect(jsonPath("$[1].date").value(danas.minusDays(3).toString()))
                .andExpect(jsonPath("$[1].type").value("GROUP"))
                .andExpect(jsonPath("$[2].trainingId").value(jutarnji))               // isti dan kao Joga, ali ranije
                .andExpect(jsonPath("$[2].type").value("SOLO"))
                .andExpect(jsonPath("$[2].date").value(danas.minusDays(3).toString()))
                .andExpect(jsonPath("$[3].trainingId").value(prisutan.sessionId()))
                .andExpect(jsonPath("$[4].type").value("SOLO"))
                .andExpect(jsonPath("$[4].date").value(danas.minusDays(10).toString()));
    }

    @Test
    void danasnjiBoravakIOtvoreniTerminNisuUIstoriji() throws Exception {
        // test pomjera vremena za do 65 minuta unazad; oko ponoći bi to prešlo u jučerašnji dan
        LocalDateTime sada = LocalDateTime.now();
        assumeTrue(sada.toLocalTime().isAfter(LocalTime.of(2, 0)), "test se preskače između ponoći i 02:00");
        Korisnik clan = noviClan("Danas");
        dolazak(clan, sada.minusMinutes(20), null);                       // još je u teretani
        NoviTermin traje = terminUBazi("Traje", sada.minusMinutes(10));    // traje 60 minuta
        prijavaUBazi(traje, clan, "prijavljen");

        istorija(clan).andExpect(jsonPath("$.length()").value(0));

        jdbc.update("UPDATE dolazak SET vrijemeIzlaska = ? WHERE clan_nalog_idnalog = ?", Timestamp.valueOf(sada), clan.id());
        // termin od prije 65 do prije 5 minuta: završen, a preklapa se sa boravkom od prije 20 minuta do sada
        jdbc.update("UPDATE termin SET Vrijeme = ? WHERE idtermin = ?", Timestamp.valueOf(sada.minusMinutes(65)), traje.sessionId());
        // trening je završio, pa je u istoriji, a dolazak za vrijeme treninga se ne broji dvaput
        istorija(clan).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].type").value("GROUP"));
    }

    @Test
    void istorijaOstajeIKadJeTrenerObrisan() throws Exception {
        Korisnik obrisani = noviTrener("Bivsi");
        int idGrupe = umetni("INSERT INTO grupa (naziv, maxClanova, trener_nalog_idnalog) VALUES ('Stari trening', 5, ?)", obrisani.id());
        int idTermina = umetni("INSERT INTO termin (Vrijeme, Trajanje, trener_nalog_idnalog, grupa_idgrupa) VALUES (?, 60, ?, ?)",
                Timestamp.valueOf(LocalDate.now().minusDays(8).atTime(10, 0)), obrisani.id(), idGrupe);
        Korisnik clan = noviClan("Vjerni");
        prijavaUBazi(new NoviTermin(idTermina, idGrupe), clan, "prijavljen");
        zahtjev(delete("/api/admin/trainers/" + obrisani.id()), admin, null).andExpect(status().isOk());

        istorija(clan).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trainingId").value(idTermina))
                .andExpect(jsonPath("$[0].name").value("Stari trening"));
    }

    private int idDolaska(Korisnik clan, LocalDateTime ulazak) {
        return jdbc.queryForObject("SELECT iddolazak FROM dolazak WHERE clan_nalog_idnalog = ? AND vrijemeUlaska = ?",
                Integer.class, clan.id(), Timestamp.valueOf(ulazak));
    }

    @Test
    void dvaGrupnaISamostalniIstogDanaSuTriTreninga() throws Exception {
        Korisnik clan = noviClan("Vrijedna");
        LocalDate dan = LocalDate.now().minusDays(2);
        NoviTermin pilates = terminUBazi("Pilates", dan.atTime(8, 0));   // 08:00–09:00
        NoviTermin joga = terminUBazi("Joga", dan.atTime(19, 0));        // 19:00–20:00
        prijavaUBazi(pilates, clan, "prijavljen");
        prijavaUBazi(joga, clan, "prijavljen");
        dolazak(clan, dan.atTime(7, 50), dan.atTime(9, 10));     // došla na Pilates
        dolazak(clan, dan.atTime(12, 55), dan.atTime(14, 10));   // sama, između
        dolazak(clan, dan.atTime(18, 50), dan.atTime(20, 15));   // došla na Jogu

        istorija(clan)
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].trainingId").value(joga.sessionId()))
                .andExpect(jsonPath("$[0].type").value("GROUP"))
                .andExpect(jsonPath("$[1].trainingId").value(idDolaska(clan, dan.atTime(12, 55))))
                .andExpect(jsonPath("$[1].type").value("SOLO"))
                .andExpect(jsonPath("$[2].trainingId").value(pilates.sessionId()))
                .andExpect(jsonPath("$[2].type").value("GROUP"))
                .andExpect(jsonPath("$[*].date", everyItem(is(dan.toString()))));
    }

    @Test
    void jedanDugiDolazakPrekoDvaTerminaNijeSamostalni() throws Exception {
        // ušla ujutru, izašla uveče: iz jednog ulaska i izlaska ne zna se da je trenirala i sama između
        Korisnik clan = noviClan("Cijeli dan");
        LocalDate dan = LocalDate.now().minusDays(2);
        NoviTermin pilates = terminUBazi("Pilates", dan.atTime(8, 0));
        NoviTermin joga = terminUBazi("Joga", dan.atTime(19, 0));
        prijavaUBazi(pilates, clan, "prijavljen");
        prijavaUBazi(joga, clan, "prijavljen");
        dolazak(clan, dan.atTime(7, 50), dan.atTime(20, 15));

        istorija(clan)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].trainingId").value(joga.sessionId()))
                .andExpect(jsonPath("$[1].trainingId").value(pilates.sessionId()))
                .andExpect(jsonPath("$[*].type", everyItem(is("GROUP"))));
    }

    @Test
    void dolazakZaVrijemeTerminaNaKojiNijePrijavljenaJeSamostalni() throws Exception {
        LocalDate dan = LocalDate.now().minusDays(3);
        NoviTermin pilates = terminUBazi("Pilates", dan.atTime(18, 0));   // 18:00–19:00
        Map<String, String> statusi = new LinkedHashMap<>();
        statusi.put("Odbijena", "odbijen");
        statusi.put("Ceka", "na_cekanju");
        statusi.put("Otkazala", "otkazao");
        statusi.put("Bez prijave", null);
        for (Map.Entry<String, String> slucaj : statusi.entrySet()) {
            Korisnik clan = noviClan(slucaj.getKey());
            if (slucaj.getValue() != null) {
                prijavaUBazi(pilates, clan, slucaj.getValue());
            }
            dolazak(clan, dan.atTime(17, 45), dan.atTime(19, 15));

            istorija(clan)
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].trainingId").value(idDolaska(clan, dan.atTime(17, 45))))
                    .andExpect(jsonPath("$[0].name").value("Samostalni trening"))
                    .andExpect(jsonPath("$[0].type").value("SOLO"));
        }
        // za poređenje: prijavljena članica u isto vrijeme ima samo Pilates
        Korisnik prijavljena = noviClan("Prijavljena");
        prijavaUBazi(pilates, prijavljena, "prijavljen");
        dolazak(prijavljena, dan.atTime(17, 45), dan.atTime(19, 15));
        istorija(prijavljena)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trainingId").value(pilates.sessionId()))
                .andExpect(jsonPath("$[0].type").value("GROUP"));
    }

    @Test
    void istorijaSamoZaClana() throws Exception {
        istorija(noviClan("Nov")).andExpect(jsonPath("$.length()").value(0));
        for (String token : List.of(trener.token(), radnik, admin)) {
            zahtjev(get("/api/users/training-history"), token, null).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/users/training-history")).andExpect(status().isUnauthorized());
    }
}
