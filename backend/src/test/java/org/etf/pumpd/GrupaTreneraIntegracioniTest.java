package org.etf.pumpd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.service.GrupaTreneraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

/**
 * Integracioni test trenerovih grupa (10.8–10.11) nad pravom MySQL bazom u Dockeru.
 * Svaki test pravi svog trenera (12.3) i svoje članove (11.3), a praćenje se upisuje direktno
 * u bazu, pa testovi ne mijenjaju seed podatke na koje se oslanjaju drugi testovi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class GrupaTreneraIntegracioniTest {

    private static final String SEED_LOZINKA = "pumpd123";
    private static final AtomicInteger BROJAC = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private GrupaTreneraService grupaService;

    private String admin;
    private String radnik;
    private Korisnik trener;

    private record Korisnik(int id, String token) {
    }

    @BeforeEach
    void pripremi() throws Exception {
        admin = prijava("admin@pumpd.local", SEED_LOZINKA);
        radnik = prijava("radnik@pumpd.local", SEED_LOZINKA);
        trener = noviTrener();
    }

    // ------------------------------------------------------------ pomoćne metode

    private String prijava(String email, String lozinka) throws Exception {
        String odgovor = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, lozinka)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
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

    private Korisnik noviTrener() throws Exception {
        String email = email("trener");
        String odgovor = zahtjev(post("/api/admin/trainers"), admin,
                "{\"name\":\"Grupni\",\"surname\":\"Trener\",\"email\":\"%s\"}".formatted(email))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(odgovor, "$.trainerId");
        return new Korisnik(id, prijava(email, JsonPath.read(odgovor, "$.temporaryPassword")));
    }

    private int noviClan(String ime) throws Exception {
        String odgovor = zahtjev(post("/api/worker/members"), radnik,
                "{\"name\":\"%s\",\"surname\":\"Test\",\"email\":\"%s\"}".formatted(ime, email("clan")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.memberId");
    }

    /** Član koji prati trenera (aktivno praćenje). */
    private int pratilac(String ime) throws Exception {
        int idClana = noviClan(ime);
        jdbc.update("INSERT INTO pracenje (trener_nalog_idnalog, clan_nalog_idnalog, DatumPracenja, status) "
                + "VALUES (?, ?, CURDATE(), 'aktivan')", trener.id(), idClana);
        return idClana;
    }

    /** Novi termin trenera; vraća id grupe. */
    private int noviTermin(LocalDateTime pocetak, int kapacitet) throws Exception {
        String odgovor = zahtjev(post("/api/trainer/group-trainings"), trener.token(), """
                {"name":"Grupa %d","startTime":"%s","durationMinutes":60,"maxMembers":%d}
                """.formatted(BROJAC.incrementAndGet(), pocetak, kapacitet))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.groupId");
    }

    private int idTermina(int idGrupe) {
        return jdbc.queryForObject("SELECT idtermin FROM termin WHERE grupa_idgrupa = ?", Integer.class, idGrupe);
    }

    private ResultActions dodaj(int idGrupe, int idClana) throws Exception {
        return zahtjev(post("/api/trainer/groups/" + idGrupe + "/members"), trener.token(),
                "{\"memberId\":" + idClana + "}");
    }

    private int obavjestenja(int idClana, String dioTeksta) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM obavjestenje WHERE primalac_nalog_idnalog = ? AND sadrzaj LIKE ?",
                Integer.class, idClana, "%" + dioTeksta + "%");
    }

    private String statusPrijave(int idGrupe, int idClana) {
        List<String> statusi = jdbc.queryForList("SELECT status FROM clan_na_terminu WHERE termin_idtermin = ? "
                + "AND clan_nalog_idnalog = ?", String.class, idTermina(idGrupe), idClana);
        return statusi.isEmpty() ? null : statusi.get(0);
    }

    // ------------------------------------------------------------ testovi

    @Test
    void dodavanjeIUklanjanjeClanova() throws Exception {
        LocalDate sutra = LocalDate.now().plusDays(1);
        int idGrupe = noviTermin(sutra.atTime(10, 0), 2);
        int ana = pratilac("Ana");
        int boris = pratilac("Boris");
        int cedo = pratilac("Cedo");
        int nijePratilac = noviClan("Dino");

        dodaj(idGrupe, ana).andExpect(status().isCreated());
        dodaj(idGrupe, ana).andExpect(status().isConflict());                  // već je u grupi
        dodaj(idGrupe, nijePratilac).andExpect(status().isBadRequest());        // ne prati trenera
        dodaj(idGrupe, 999999).andExpect(status().isNotFound());                // ne postoji
        dodaj(idGrupe, boris).andExpect(status().isCreated());                  // posljednje mjesto
        dodaj(idGrupe, cedo).andExpect(status().isConflict());                  // grupa je puna

        zahtjev(get("/api/trainer/groups/" + idGrupe + "/members"), trener.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Ana"))
                .andExpect(jsonPath("$[1].memberId").value(boris));
        assertEquals("prijavljen", statusPrijave(idGrupe, ana));
        assertEquals(1, obavjestenja(ana, "dodao"));

        // trenerov raspored (10.4) vidi iste članove
        zahtjev(get("/api/trainer/schedule").param("date", sutra.toString()), trener.token(), null)
                .andExpect(jsonPath("$[0].enrolledCount").value(2));

        zahtjev(delete("/api/trainer/groups/" + idGrupe + "/members/" + ana), trener.token(), null)
                .andExpect(status().isOk());
        zahtjev(delete("/api/trainer/groups/" + idGrupe + "/members/" + ana), trener.token(), null)
                .andExpect(status().isNotFound());
        assertEquals(null, statusPrijave(idGrupe, ana));                       // može ponovo poslati zahtjev
        assertEquals(1, obavjestenja(ana, "uklonio"));

        dodaj(idGrupe, cedo).andExpect(status().isCreated());                  // mjesto se oslobodilo
    }

    @Test
    void zahtjevKojiCekaPostajePrihvacen() throws Exception {
        int idGrupe = noviTermin(LocalDate.now().plusDays(1).atTime(10, 0), 5);
        int ana = pratilac("Ana");
        jdbc.update("INSERT INTO clan_na_terminu (termin_idtermin, clan_nalog_idnalog, status, datumZahtjeva) "
                + "VALUES (?, ?, 'na_cekanju', CURDATE())", idTermina(idGrupe), ana);
        int idZahtjeva = jdbc.queryForObject("SELECT idzahtjev FROM clan_na_terminu WHERE termin_idtermin = ? "
                + "AND clan_nalog_idnalog = ?", Integer.class, idTermina(idGrupe), ana);

        dodaj(idGrupe, ana).andExpect(status().isCreated());

        assertEquals("prijavljen", statusPrijave(idGrupe, ana));
        assertEquals(idZahtjeva, jdbc.queryForObject("SELECT idzahtjev FROM clan_na_terminu WHERE termin_idtermin = ? "
                + "AND clan_nalog_idnalog = ?", Integer.class, idTermina(idGrupe), ana));
    }

    @Test
    void listaGrupaPrvoPredstojeci() throws Exception {
        int kasnija = noviTermin(LocalDate.now().plusDays(3).atTime(10, 0), 5);
        int ranija = noviTermin(LocalDate.now().plusDays(1).atTime(10, 0), 8);

        zahtjev(get("/api/trainer/groups"), trener.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].groupId").value(ranija))
                .andExpect(jsonPath("$[0].maxMembers").value(8))
                .andExpect(jsonPath("$[0].memberCount").value(0))
                .andExpect(jsonPath("$[1].groupId").value(kasnija));
    }

    @Test
    void tudjaGrupaProsliTerminINepostojecaGrupa() throws Exception {
        int idGrupe = noviTermin(LocalDate.now().plusDays(1).atTime(10, 0), 5);
        String trener1 = prijava("trener1@pumpd.local", SEED_LOZINKA);
        zahtjev(get("/api/trainer/groups/" + idGrupe + "/members"), trener1, null).andExpect(status().isForbidden());
        zahtjev(post("/api/trainer/groups/" + idGrupe + "/members"), trener1, "{\"memberId\":1}")
                .andExpect(status().isForbidden());
        zahtjev(get("/api/trainer/groups/999999/members"), trener.token(), null).andExpect(status().isNotFound());

        // seed: "CrossFit (prošli)" je termin trener3 od prošle sedmice, clan2 je u grupi
        String trener3 = prijava("trener3@pumpd.local", SEED_LOZINKA);
        int prosla = jdbc.queryForObject("SELECT idgrupa FROM grupa WHERE naziv = 'CrossFit (prošli)'", Integer.class);
        int clan2 = jdbc.queryForObject("SELECT idnalog FROM nalog WHERE email = 'clan2@pumpd.local'", Integer.class);
        zahtjev(delete("/api/trainer/groups/" + prosla + "/members/" + clan2), trener3, null)
                .andExpect(status().isConflict());
        zahtjev(get("/api/trainer/groups/" + prosla + "/members"), trener3, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void dvaIstovremenaDodavanjaNeZauzimajuIstoPosljednjeMjesto() throws Exception {
        int idGrupe = noviTermin(LocalDate.now().plusDays(1).atTime(10, 0), 1);
        int ana = pratilac("Ana");
        int boris = pratilac("Boris");
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService niti = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> prvi = niti.submit(() -> pokusaj(start, idGrupe, ana));
            Future<Boolean> drugi = niti.submit(() -> pokusaj(start, idGrupe, boris));
            start.countDown();                       // obje niti kreću u istom trenutku
            int uspjesnih = (prvi.get(30, TimeUnit.SECONDS) ? 1 : 0) + (drugi.get(30, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, uspjesnih);
        } finally {
            niti.shutdownNow();
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM clan_u_grupi WHERE grupa_idgrupa = ?",
                Integer.class, idGrupe));
    }

    /** Dodavanje direktno preko servisa (svaki poziv je zasebna transakcija); true ako je uspjelo. */
    private boolean pokusaj(CountDownLatch start, int idGrupe, int idClana) throws InterruptedException {
        start.await();
        try {
            grupaService.dodajClana(trener.id(), idGrupe, idClana);
            return true;
        } catch (KonfliktException grupaPuna) {
            return false;
        }
    }
}
