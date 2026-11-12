package org.etf.pumpd;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

/**
 * Integracioni test toka A (Faza 2) nad pravom MySQL bazom u Dockeru.
 * Svaki zahtjev ide kroz cijeli put: prijava (1.1) → token → JwtAuthFilter → pronadjiUlogu
 * → SecurityConfig → kontroler → servis → baza. Seed nalozi imaju lozinku pumpd123.
 * Testovi ne poništavaju transakcije (bez @Transactional), da se provjeri i stvarni upis;
 * svaki test pravi svoje naloge sa jedinstvenim emailom, pa ne zavise jedan od drugog.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class TokAIntegracioniTest {

    private static final String SEED_LOZINKA = "pumpd123";
    private static final String LOZINKA = "lozinka123";
    private static final AtomicInteger BROJAC = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private String admin;
    private String radnik;
    private String clan;
    private String trener;

    @BeforeEach
    void prijaviSeedKorisnike() throws Exception {
        admin = prijava("admin@pumpd.local", SEED_LOZINKA);
        radnik = prijava("radnik@pumpd.local", SEED_LOZINKA);
        clan = prijava("clan1@pumpd.local", SEED_LOZINKA);
        trener = prijava("trener1@pumpd.local", SEED_LOZINKA);
    }

    // ------------------------------------------------------------ pomoćne metode

    private static String jedinstvenEmail(String prefiks) {
        return prefiks + "." + System.nanoTime() + "." + BROJAC.incrementAndGet() + "@mail.com";
    }

    private ResultActions prijavaZahtjev(String email, String lozinka) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, lozinka)));
    }

    private String prijava(String email, String lozinka) throws Exception {
        String odgovor = prijavaZahtjev(email, lozinka)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.token");
    }

    private String uloga(String email, String lozinka) throws Exception {
        String odgovor = prijavaZahtjev(email, lozinka).andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.role");
    }

    private ResultActions get_(String token, String ruta) throws Exception {
        var zahtjev = get(ruta);
        if (token != null) {
            zahtjev.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(zahtjev);
    }

    /** GET sa parametrom search; .param() kodira razmake, za razliku od teksta u URL šablonu. */
    private ResultActions pretraga(String token, String search) throws Exception {
        return mockMvc.perform(get("/api/worker/members").param("search", search)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions post_(String token, String ruta, String tijelo) throws Exception {
        return mockMvc.perform(post(ruta)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tijelo));
    }

    private ResultActions delete_(String token, String ruta) throws Exception {
        return mockMvc.perform(delete(ruta).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private int idIzOdgovora(ResultActions odgovor, String polje) throws Exception {
        return JsonPath.read(odgovor.andReturn().getResponse().getContentAsString(), "$." + polje);
    }

    private int dodajClana(String email) throws Exception {
        return idIzOdgovora(post_(radnik, "/api/worker/members", """
                {"name":"Test","surname":"Clan","email":"%s","password":"%s","birthYear":2000}
                """.formatted(email, LOZINKA)).andExpect(status().isCreated()), "memberId");
    }

    private int dodajTrenera(String email) throws Exception {
        return idIzOdgovora(post_(admin, "/api/admin/trainers", """
                {"name":"Test","surname":"Trener","email":"%s","password":"%s","biography":"  Test bio  "}
                """.formatted(email, LOZINKA)).andExpect(status().isCreated()), "trainerId");
    }

    private int dodajRadnika(String email) throws Exception {
        return idIzOdgovora(post_(admin, "/api/admin/workers", """
                {"name":"Test","surname":"Radnik","email":"%s","password":"%s"}
                """.formatted(email, LOZINKA)).andExpect(status().isCreated()), "workerId");
    }

    private int idSeedNaloga(String email) {
        return jdbc.queryForObject("SELECT idnalog FROM nalog WHERE email = ?", Integer.class, email);
    }

    private Map<String, Object> nalogUBazi(int id) {
        return jdbc.queryForMap("SELECT email, aktivan FROM nalog WHERE idnalog = ?", id);
    }

    private int brojRedova(String tabela, String kolona, int id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela + " WHERE " + kolona + " = ?", Integer.class, id);
    }

    // ------------------------------------------------------------ V6 i sigurnost

    @Test
    void seedNaloziSuAktivni() {
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM nalog WHERE email LIKE '%@pumpd.local' AND aktivan = 0",
                Integer.class));
    }

    @Test
    void pristupPoUlogama() throws Exception {
        get_(null, "/api/worker/members").andExpect(status().isUnauthorized());
        get_(clan, "/api/worker/members").andExpect(status().isForbidden());
        get_(trener, "/api/worker/members").andExpect(status().isForbidden());
        get_(radnik, "/api/worker/members").andExpect(status().isOk());
        get_(admin, "/api/worker/members").andExpect(status().isOk());

        get_(radnik, "/api/admin/trainers").andExpect(status().isForbidden());
        get_(radnik, "/api/admin/workers").andExpect(status().isForbidden());
        get_(clan, "/api/admin/trainers").andExpect(status().isForbidden());
        get_(admin, "/api/admin/trainers").andExpect(status().isOk());
        get_(admin, "/api/admin/workers").andExpect(status().isOk());
    }

    // ------------------------------------------------------------ 11.1, 11.2

    @Test
    void listaClanovaSaStatusomClanarineIzSeedPodataka() throws Exception {
        pretraga(radnik, "pumpd.local")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[?(@.email=='clan1@pumpd.local')].membershipActive").value(true))
                .andExpect(jsonPath("$[?(@.email=='clan2@pumpd.local')].membershipActive").value(true))   // ističe danas
                .andExpect(jsonPath("$[?(@.email=='clan3@pumpd.local')].membershipActive").value(false))  // istekla
                .andExpect(jsonPath("$[?(@.email=='clan4@pumpd.local')].membershipActive").value(true))
                .andExpect(jsonPath("$[?(@.email=='clan5@pumpd.local')].membershipActive").value(false)); // nema je
    }

    @Test
    void pretragaPoImenuIPunomImenuBezObziraNaVelikaSlova() throws Exception {
        pretraga(radnik, "amar")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("clan1@pumpd.local"));
        pretraga(radnik, "Amar Hod")
                .andExpect(jsonPath("$.length()").value(1));
        pretraga(radnik, "PUMPD.LOCAL")
                .andExpect(jsonPath("$.length()").value(5));
        pretraga(radnik, "nema-takvog")
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void detaljiSeedClanaINepostojeciClan() throws Exception {
        int clan1 = idSeedNaloga("clan1@pumpd.local");

        get_(radnik, "/api/worker/members/" + clan1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(clan1))
                .andExpect(jsonPath("$.name").value("Amar"))
                .andExpect(jsonPath("$.birthYear").value(1998))
                .andExpect(jsonPath("$.membershipActive").value(true))
                .andExpect(jsonPath("$.membershipValidUntil").isString());

        get_(radnik, "/api/worker/members/" + idSeedNaloga("trener1@pumpd.local")).andExpect(status().isNotFound());
        get_(radnik, "/api/worker/members/abc").andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 11.3

    @Test
    void noviClanSeUpisujeIMozeSePrijaviti() throws Exception {
        String email = jedinstvenEmail("clan");
        int id = dodajClana(email);

        get_(radnik, "/api/worker/members/" + id)
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.birthYear").value(2000))
                .andExpect(jsonPath("$.membershipActive").value(false))
                .andExpect(jsonPath("$.membershipValidUntil").value(nullValue()));
        assertNotNull(jdbc.queryForObject("SELECT qrKod FROM clan WHERE nalog_idnalog = ?", String.class, id));
        assertEquals("MEMBER", uloga(email, LOZINKA));

        post_(radnik, "/api/worker/members", """
                {"name":"X","surname":"Y","email":"%s","password":"%s"}
                """.formatted(email.toUpperCase(), LOZINKA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email vec postoji."));
    }

    // ------------------------------------------------------------ 12.1, 12.2, 12.3

    @Test
    void listaIDetaljiSeedTrenera() throws Exception {
        get_(admin, "/api/admin/trainers")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("trener1@pumpd.local")))
                .andExpect(jsonPath("$[*].email", hasItem("trener2@pumpd.local")))
                .andExpect(jsonPath("$[*].email", hasItem("trener3@pumpd.local")));

        get_(admin, "/api/admin/trainers/" + idSeedNaloga("trener1@pumpd.local"))
                .andExpect(jsonPath("$.name").value("Marko"))
                .andExpect(jsonPath("$.biography").isString());
        get_(admin, "/api/admin/trainers/" + idSeedNaloga("clan1@pumpd.local")).andExpect(status().isNotFound());
    }

    @Test
    void noviTrenerSeUpisujeIMozeSePrijaviti() throws Exception {
        String email = jedinstvenEmail("trener");
        int id = dodajTrenera(email);

        get_(admin, "/api/admin/trainers/" + id)
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.biography").value("Test bio"))
                .andExpect(jsonPath("$.averageGrade").value(nullValue()))
                .andExpect(jsonPath("$.followerCount").value(0));
        assertEquals("TRAINER", uloga(email, LOZINKA));

        post_(admin, "/api/admin/trainers", """
                {"name":"X","surname":"Y","email":"%s","password":"%s"}
                """.formatted(email, LOZINKA)).andExpect(status().isConflict());
    }

    @Test
    void prosjecnaOcjenaIBrojPratilacaIzBaze() throws Exception {
        int trenerId = dodajTrenera(jedinstvenEmail("trener"));
        int c1 = idSeedNaloga("clan1@pumpd.local");
        int c2 = idSeedNaloga("clan2@pumpd.local");
        int c3 = idSeedNaloga("clan3@pumpd.local");
        int c4 = idSeedNaloga("clan4@pumpd.local");

        String recenzija = "INSERT INTO recenzija (ocjena, komentar, trener_nalog_idnalog, datum, clan_nalog_idnalog) "
                + "VALUES (?, 'test', ?, CURDATE(), ?)";
        jdbc.update(recenzija, 5, trenerId, c1);
        jdbc.update(recenzija, 4, trenerId, c2);
        jdbc.update(recenzija, 4, trenerId, c3);

        String pracenje = "INSERT INTO pracenje (trener_nalog_idnalog, clan_nalog_idnalog, DatumPracenja, status) "
                + "VALUES (?, ?, CURDATE(), ?)";
        jdbc.update(pracenje, trenerId, c1, "aktivan");
        jdbc.update(pracenje, trenerId, c2, "aktivan");
        jdbc.update(pracenje, trenerId, c3, "na_cekanju");
        jdbc.update(pracenje, trenerId, c4, "neaktivan");

        get_(admin, "/api/admin/trainers/" + trenerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageGrade").value(4.3))   // (5+4+4)/3 = 4.333… → 4.3
                .andExpect(jsonPath("$.followerCount").value(2));    // samo 'aktivan'
    }

    @Test
    void obrisanClanSeNeBrojiKaoPratilacAliMuOcjenaOstaje() throws Exception {
        int trenerId = dodajTrenera(jedinstvenEmail("trener"));
        int clanId = dodajClana(jedinstvenEmail("clan"));
        jdbc.update("INSERT INTO pracenje (trener_nalog_idnalog, clan_nalog_idnalog, DatumPracenja, status) "
                + "VALUES (?, ?, CURDATE(), 'aktivan')", trenerId, clanId);
        jdbc.update("INSERT INTO recenzija (ocjena, komentar, trener_nalog_idnalog, datum, clan_nalog_idnalog) "
                + "VALUES (3, 'test', ?, CURDATE(), ?)", trenerId, clanId);

        get_(admin, "/api/admin/trainers/" + trenerId).andExpect(jsonPath("$.followerCount").value(1));

        delete_(radnik, "/api/worker/members/" + clanId).andExpect(status().isOk());

        get_(admin, "/api/admin/trainers/" + trenerId)
                .andExpect(jsonPath("$.followerCount").value(0))
                .andExpect(jsonPath("$.averageGrade").value(3.0));
    }

    // ------------------------------------------------------------ 12.5, 12.6, 12.7

    @Test
    void listaRadnikaNemaAdministratora() throws Exception {
        get_(admin, "/api/admin/workers")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("radnik@pumpd.local")))
                .andExpect(jsonPath("$[*].email", not(hasItem("admin@pumpd.local"))));

        get_(admin, "/api/admin/workers/" + idSeedNaloga("radnik@pumpd.local"))
                .andExpect(jsonPath("$.name").value("Emina"));
        get_(admin, "/api/admin/workers/" + idSeedNaloga("admin@pumpd.local")).andExpect(status().isNotFound());
    }

    @Test
    void noviRadnikImaRedoveUTriTabeleIMozeRaditiKaoRadnik() throws Exception {
        String email = jedinstvenEmail("radnik");
        int id = dodajRadnika(email);

        assertEquals(1, brojRedova("zaposleni", "nalog_idnalog", id));
        assertEquals(1, brojRedova("radnik", "zaposleni_nalog_idnalog", id));
        assertEquals(0, brojRedova("administrator", "zaposleni_nalog_idnalog", id));
        get_(admin, "/api/admin/workers/" + id).andExpect(jsonPath("$.email").value(email));

        assertEquals("WORKER", uloga(email, LOZINKA));
        String token = prijava(email, LOZINKA);
        get_(token, "/api/worker/members").andExpect(status().isOk());
        get_(token, "/api/admin/workers").andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ meko brisanje (11.4, 12.4, 12.8)

    @Test
    void mekoBrisanjeClana() throws Exception {
        String email = jedinstvenEmail("clan");
        int id = dodajClana(email);
        String token = prijava(email, LOZINKA);
        get_(token, "/api/users/my-profile").andExpect(status().isOk());

        delete_(clan, "/api/worker/members/" + id).andExpect(status().isForbidden());
        delete_(radnik, "/api/worker/members/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Clan je uspjesno uklonjen."));

        get_(radnik, "/api/worker/members/" + id).andExpect(status().isNotFound());
        pretraga(radnik, email).andExpect(jsonPath("$").isEmpty());
        delete_(radnik, "/api/worker/members/" + id).andExpect(status().isNotFound());
        get_(token, "/api/users/my-profile").andExpect(status().isUnauthorized());
        prijavaZahtjev(email, LOZINKA).andExpect(status().isUnauthorized());

        Map<String, Object> red = nalogUBazi(id);
        assertEquals("obrisan-" + id + "@pumpd.invalid", red.get("email"));
        assertFalse((Boolean) red.get("aktivan"));
        assertEquals(1, brojRedova("clan", "nalog_idnalog", id), "red člana ostaje zbog istorije");

        int noviId = dodajClana(email);   // email je oslobođen
        assertEquals("MEMBER", uloga(email, LOZINKA));
        pretraga(radnik, email).andExpect(jsonPath("$[0].memberId").value(noviId));
    }

    @Test
    void mekoBrisanjeTrenera() throws Exception {
        String email = jedinstvenEmail("trener");
        int id = dodajTrenera(email);
        String token = prijava(email, LOZINKA);

        delete_(radnik, "/api/admin/trainers/" + id).andExpect(status().isForbidden());
        delete_(admin, "/api/admin/trainers/" + id).andExpect(status().isOk());

        get_(admin, "/api/admin/trainers/" + id).andExpect(status().isNotFound());
        get_(admin, "/api/admin/trainers").andExpect(jsonPath("$[*].trainerId", not(hasItem(id))));
        get_(token, "/api/users/my-profile").andExpect(status().isUnauthorized());
        prijavaZahtjev(email, LOZINKA).andExpect(status().isUnauthorized());
        assertFalse((Boolean) nalogUBazi(id).get("aktivan"));
    }

    @Test
    void mekoBrisanjeRadnika() throws Exception {
        String email = jedinstvenEmail("radnik");
        int id = dodajRadnika(email);
        String token = prijava(email, LOZINKA);

        delete_(admin, "/api/admin/workers/" + id).andExpect(status().isOk());

        get_(admin, "/api/admin/workers/" + id).andExpect(status().isNotFound());
        get_(admin, "/api/admin/workers").andExpect(jsonPath("$[*].workerId", not(hasItem(id))));
        delete_(admin, "/api/admin/workers/" + id).andExpect(status().isNotFound());
        get_(token, "/api/worker/members").andExpect(status().isUnauthorized());
        assertEquals("obrisan-" + id + "@pumpd.invalid", nalogUBazi(id).get("email"));
    }

    @Test
    void obrisaniNaloziSeNePojavljujuUListama() throws Exception {
        int clanId = dodajClana(jedinstvenEmail("clan"));
        int trenerId = dodajTrenera(jedinstvenEmail("trener"));
        int radnikId = dodajRadnika(jedinstvenEmail("radnik"));
        delete_(radnik, "/api/worker/members/" + clanId).andExpect(status().isOk());
        delete_(admin, "/api/admin/trainers/" + trenerId).andExpect(status().isOk());
        delete_(admin, "/api/admin/workers/" + radnikId).andExpect(status().isOk());

        pretraga(radnik, "obrisan-").andExpect(jsonPath("$").isEmpty());
        get_(radnik, "/api/worker/members").andExpect(jsonPath("$[*].memberId", not(hasItem(clanId))));
        get_(admin, "/api/admin/trainers").andExpect(jsonPath("$[*].trainerId", not(hasItem(trenerId))));
        get_(admin, "/api/admin/workers").andExpect(jsonPath("$[*].workerId", not(hasItem(radnikId))));
    }

    @Test
    void pretragaIgnoriseRazmakePraznaPretragaVracaSve() throws Exception {
        String svi = get_(radnik, "/api/worker/members").andReturn().getResponse().getContentAsString();
        String prazna = pretraga(radnik, "  ").andReturn().getResponse().getContentAsString();
        assertEquals(svi, prazna);
        pretraga(radnik, " amar ")
                .andExpect(jsonPath("$[*].email", containsInAnyOrder("clan1@pumpd.local")));
    }
}
