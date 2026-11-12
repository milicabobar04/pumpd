package org.etf.pumpd;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

/**
 * Integracioni test toka B (Faza 2) nad pravom MySQL bazom u Dockeru: praćenje (7.3, 7.4, 10.1–10.3),
 * treneri za člana (7.1, 7.2), recenzije (7.5, 7.6), administracija (4.2, 4.3, 5.2) i obavještenja (6.1).
 * Isti princip kao TokAIntegracioniTest: prava prijava, bez poništavanja transakcija, a svaki test
 * pravi svoje trenere i članove (jedinstveni emailovi), pa ne zavisi od drugih testova.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class TokBIntegracioniTest {

    private static final String SEED_LOZINKA = "pumpd123";
    private static final String LOZINKA = "lozinka123";
    private static final AtomicInteger BROJAC = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private String admin;
    private String radnik;

    @BeforeEach
    void prijaviSeedKorisnike() throws Exception {
        admin = prijava("admin@pumpd.local", SEED_LOZINKA);
        radnik = prijava("radnik@pumpd.local", SEED_LOZINKA);
    }

    // ------------------------------------------------------------ pomoćne metode

    /** Nalog napravljen u testu: id i token. */
    private record Korisnik(int id, String token) {
    }

    private static String jedinstvenEmail(String prefiks) {
        return prefiks + "." + System.nanoTime() + "." + BROJAC.incrementAndGet() + "@mail.com";
    }

    private String prijava(String email, String lozinka) throws Exception {
        String odgovor = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, lozinka)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.token");
    }

    private ResultActions zahtjev(MockHttpServletRequestBuilder builder, String token, String tijelo) throws Exception {
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (tijelo != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(tijelo);
        }
        return mockMvc.perform(builder);
    }

    private ResultActions get_(String token, String ruta) throws Exception {
        return zahtjev(get(ruta), token, null);
    }

    private ResultActions post_(String token, String ruta, String tijelo) throws Exception {
        return zahtjev(post(ruta), token, tijelo);
    }

    private ResultActions patch_(String token, String ruta, String tijelo) throws Exception {
        return zahtjev(patch(ruta), token, tijelo);
    }

    private ResultActions delete_(String token, String ruta) throws Exception {
        return zahtjev(delete(ruta), token, null);
    }

    private static int broj(ResultActions odgovor, String putanja) throws Exception {
        return JsonPath.read(odgovor.andReturn().getResponse().getContentAsString(), putanja);
    }

    private Korisnik noviClan(String ime, String prezime) throws Exception {
        String email = jedinstvenEmail("clan");
        int id = broj(post_(radnik, "/api/worker/members", """
                {"name":"%s","surname":"%s","email":"%s","password":"%s"}
                """.formatted(ime, prezime, email, LOZINKA)).andExpect(status().isCreated()), "$.memberId");
        return new Korisnik(id, prijava(email, LOZINKA));
    }

    private Korisnik noviTrener(String ime, String prezime) throws Exception {
        String email = jedinstvenEmail("trener");
        int id = broj(post_(admin, "/api/admin/trainers", """
                {"name":"%s","surname":"%s","email":"%s","password":"%s"}
                """.formatted(ime, prezime, email, LOZINKA)).andExpect(status().isCreated()), "$.trainerId");
        return new Korisnik(id, prijava(email, LOZINKA));
    }

    private int posaljiZahtjev(Korisnik clan, Korisnik trener) throws Exception {
        return broj(post_(clan.token(), "/api/trainers/" + trener.id() + "/follow", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING")), "$.followRequestId");
    }

    private ResultActions odluka(Korisnik trener, int requestId, String status) throws Exception {
        return patch_(trener.token(), "/api/trainer/follow-requests/" + requestId, "{\"status\":\"" + status + "\"}");
    }

    private ResultActions recenzija(Korisnik clan, Korisnik trener, String tijelo) throws Exception {
        return post_(clan.token(), "/api/trainers/" + trener.id() + "/reviews", tijelo);
    }

    private ResultActions obavjestenja(Korisnik korisnik) throws Exception {
        return get_(korisnik.token(), "/api/notifications").andExpect(status().isOk());
    }

    // ------------------------------------------------------------ pristup po ulogama

    @Test
    void pristupPoUlogama() throws Exception {
        Korisnik clan = noviClan("Pristup", "Clan");
        Korisnik trener = noviTrener("Pristup", "Trener");

        get_(clan.token(), "/api/trainers").andExpect(status().isOk());
        get_(trener.token(), "/api/trainers").andExpect(status().isForbidden());
        get_(radnik, "/api/trainers").andExpect(status().isForbidden());

        get_(trener.token(), "/api/trainer/follow-requests").andExpect(status().isOk());
        get_(trener.token(), "/api/trainer/reviews").andExpect(status().isOk());
        get_(clan.token(), "/api/trainer/followers").andExpect(status().isForbidden());
        get_(admin, "/api/trainer/reviews").andExpect(status().isForbidden());

        get_(null, "/api/gym/info").andExpect(status().isOk());
        zahtjev(put("/api/gym/info"), radnik, "{}").andExpect(status().isForbidden());
        get_(radnik, "/api/admin/issues").andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ praćenje (7.3, 10.1–10.3, 7.2, 12.2, 6.1)

    @Test
    void cijeliTokPracenjaSaObavjestenjima() throws Exception {
        Korisnik trener = noviTrener("Marko", "Pratic");
        Korisnik clan = noviClan("Ana", "Pratilac");

        int requestId = posaljiZahtjev(clan, trener);

        obavjestenja(trener).andExpect(jsonPath("$[*].content",
                hasItem("Ana Pratilac vam je poslao zahtjev za praćenje.")));
        get_(clan.token(), "/api/trainers/" + trener.id()).andExpect(jsonPath("$.followStatus").value("PENDING"));
        get_(trener.token(), "/api/trainer/follow-requests")
                .andExpect(jsonPath("$[0].requestId").value(requestId))
                .andExpect(jsonPath("$[0].memberId").value(clan.id()))
                .andExpect(jsonPath("$[0].memberName").value("Ana Pratilac"))
                .andExpect(jsonPath("$[0].requestDate").isString());
        post_(clan.token(), "/api/trainers/" + trener.id() + "/follow", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Zahtjev za pracenje je vec poslat."));

        odluka(trener, requestId, "ACCEPTED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Zahtjev je prihvacen."));

        obavjestenja(clan).andExpect(jsonPath("$[*].content",
                hasItem("Marko Pratic je prihvatio vaš zahtjev za praćenje.")));
        get_(clan.token(), "/api/trainers/" + trener.id()).andExpect(jsonPath("$.followStatus").value("ACCEPTED"));
        get_(trener.token(), "/api/trainer/follow-requests").andExpect(jsonPath("$").isEmpty());
        get_(trener.token(), "/api/trainer/followers")
                .andExpect(jsonPath("$[0].memberId").value(clan.id()))
                .andExpect(jsonPath("$[0].name").value("Ana"))
                .andExpect(jsonPath("$[0].surname").value("Pratilac"));
        get_(admin, "/api/admin/trainers/" + trener.id()).andExpect(jsonPath("$.followerCount").value(1));

        odluka(trener, requestId, "REJECTED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Zahtjev je vec obradjen."));
        post_(clan.token(), "/api/trainers/" + trener.id() + "/follow", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Vec pratite ovog trenera."));
    }

    @Test
    void odbijanjeIPonovniZahtjevNadIstimRedom() throws Exception {
        Korisnik trener = noviTrener("Odbija", "Trener");
        Korisnik clan = noviClan("Uporna", "Clanica");

        int prviId = posaljiZahtjev(clan, trener);
        odluka(trener, prviId, "REJECTED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Zahtjev je odbijen."));

        obavjestenja(clan).andExpect(jsonPath("$[*].content",
                hasItem("Odbija Trener je odbio vaš zahtjev za praćenje.")));
        get_(clan.token(), "/api/trainers/" + trener.id()).andExpect(jsonPath("$.followStatus").value("NONE"));
        delete_(clan.token(), "/api/trainers/" + trener.id() + "/follow").andExpect(status().isNotFound());

        int drugiId = posaljiZahtjev(clan, trener);

        assertEquals(prviId, drugiId, "ponovni zahtjev koristi isti red");
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM pracenje WHERE trener_nalog_idnalog = ? AND clan_nalog_idnalog = ?",
                Integer.class, trener.id(), clan.id()));
        assertEquals("na_cekanju", jdbc.queryForObject(
                "SELECT status FROM pracenje WHERE idpracenje = ?", String.class, prviId));
        get_(trener.token(), "/api/trainer/follow-requests").andExpect(jsonPath("$[0].requestId").value(prviId));
    }

    @Test
    void povlacenjeZahtjevaIPrekidPracenja() throws Exception {
        Korisnik trener = noviTrener("Prekid", "Trener");
        Korisnik clan = noviClan("Prekid", "Clan");
        String ruta = "/api/trainers/" + trener.id() + "/follow";

        posaljiZahtjev(clan, trener);
        delete_(clan.token(), ruta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Zahtjev za pracenje je povucen."));
        get_(trener.token(), "/api/trainer/follow-requests").andExpect(jsonPath("$").isEmpty());
        delete_(clan.token(), ruta).andExpect(status().isNotFound());

        odluka(trener, posaljiZahtjev(clan, trener), "ACCEPTED").andExpect(status().isOk());
        delete_(clan.token(), ruta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pracenje je prekinuto."));

        get_(trener.token(), "/api/trainer/followers").andExpect(jsonPath("$").isEmpty());
        get_(clan.token(), "/api/trainers/" + trener.id()).andExpect(jsonPath("$.followStatus").value("NONE"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM pracenje WHERE trener_nalog_idnalog = ?", Integer.class, trener.id()));
    }

    @Test
    void trenerNeMozeObraditiTudjiZahtjev() throws Exception {
        Korisnik pravi = noviTrener("Pravi", "Trener");
        Korisnik drugi = noviTrener("Drugi", "Trener");
        Korisnik clan = noviClan("Neki", "Clan");
        int requestId = posaljiZahtjev(clan, pravi);

        odluka(drugi, requestId, "ACCEPTED")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Zahtjev ne postoji."));

        get_(drugi.token(), "/api/trainer/follow-requests").andExpect(jsonPath("$").isEmpty());
        get_(pravi.token(), "/api/trainer/follow-requests").andExpect(jsonPath("$[0].requestId").value(requestId));
        get_(clan.token(), "/api/trainers/" + pravi.id()).andExpect(jsonPath("$.followStatus").value("PENDING"));
    }

    @Test
    void neispravnaOdlukaVraca400() throws Exception {
        Korisnik trener = noviTrener("Odluka", "Trener");
        int requestId = posaljiZahtjev(noviClan("Odluka", "Clan"), trener);

        odluka(trener, requestId, "PENDING").andExpect(status().isBadRequest());
        get_(trener.token(), "/api/trainer/follow-requests").andExpect(jsonPath("$[0].requestId").value(requestId));
    }

    // ------------------------------------------------------------ treneri za člana (7.1, 7.2)

    @Test
    void listaIDetaljiTreneraZaClana() throws Exception {
        Korisnik trener = noviTrener("Lista", "Trener");
        Korisnik clan = noviClan("Lista", "Clan");

        get_(clan.token(), "/api/trainers")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].trainerId", hasItem(trener.id())))
                .andExpect(jsonPath("$[?(@.trainerId==" + trener.id() + ")].averageGrade").value(hasItem(nullValue())));

        get_(clan.token(), "/api/trainers/" + trener.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lista"))
                .andExpect(jsonPath("$.averageGrade").value(nullValue()))
                .andExpect(jsonPath("$.followStatus").value("NONE"))
                .andExpect(jsonPath("$.reviews").isEmpty());

        get_(clan.token(), "/api/trainers/" + clan.id()).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ recenzije (7.5, 7.6, V7)

    @Test
    void recenzijeProsjekIstiNaSvimRutamaIZamjenaStare() throws Exception {
        Korisnik trener = noviTrener("Ocjena", "Trener");
        Korisnik prvi = noviClan("Prvi", "Ocjenjivac");
        Korisnik drugi = noviClan("Drugi", "Ocjenjivac");

        int reviewId = broj(recenzija(prvi, trener, "{\"grade\":5,\"comment\":\"Odličan.\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.averageGrade").value(5.0))
                .andExpect(jsonPath("$.message").value("Recenzija je sacuvana.")), "$.reviewId");
        recenzija(drugi, trener, "{\"grade\":4}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.averageGrade").value(4.5));

        // isti član ponovo: zamjenjuje svoju recenziju (odluka A), prosjek (3+4)/2 = 3.5
        recenzija(prvi, trener, "{\"grade\":3,\"comment\":\"Ipak prosječno.\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").value(reviewId))
                .andExpect(jsonPath("$.averageGrade").value(3.5))
                .andExpect(jsonPath("$.message").value("Recenzija je azurirana."));
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM recenzija WHERE trener_nalog_idnalog = ?", Integer.class, trener.id()));

        get_(prvi.token(), "/api/trainers")
                .andExpect(jsonPath("$[?(@.trainerId==" + trener.id() + ")].averageGrade").value(hasItem(3.5)));
        get_(prvi.token(), "/api/trainers/" + trener.id())
                .andExpect(jsonPath("$.averageGrade").value(3.5))
                .andExpect(jsonPath("$.reviews.length()").value(2))
                .andExpect(jsonPath("$.reviews[*].memberName", hasItem("Prvi Ocjenjivac")))
                .andExpect(jsonPath("$.reviews[*].comment", hasItem("Ipak prosječno.")));
        get_(trener.token(), "/api/trainer/reviews")
                .andExpect(jsonPath("$.averageGrade").value(3.5))
                .andExpect(jsonPath("$.reviews.length()").value(2));
        get_(admin, "/api/admin/trainers/" + trener.id()).andExpect(jsonPath("$.averageGrade").value(3.5));

        obavjestenja(trener)
                .andExpect(jsonPath("$[*].content", hasItem("Prvi Ocjenjivac vam je ostavio recenziju (ocjena: 5).")))
                .andExpect(jsonPath("$[*].content", hasItem("Drugi Ocjenjivac vam je ostavio recenziju (ocjena: 4).")))
                .andExpect(jsonPath("$[*].content", hasItem("Prvi Ocjenjivac je izmijenio recenziju (ocjena: 3).")));
    }

    @Test
    void neispravnaOcjenaVraca400INistaSeNeUpisuje() throws Exception {
        Korisnik trener = noviTrener("Validacija", "Trener");
        Korisnik clan = noviClan("Validacija", "Clan");

        recenzija(clan, trener, "{\"grade\":4.5}").andExpect(status().isBadRequest());
        recenzija(clan, trener, "{\"grade\":6}").andExpect(status().isBadRequest());
        recenzija(clan, trener, "{\"grade\":0}").andExpect(status().isBadRequest());

        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM recenzija WHERE trener_nalog_idnalog = ?", Integer.class, trener.id()));
    }

    @Test
    void v7DozvoljavaSamoJednuRecenzijuPoParu() {
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'recenzija'
                  AND index_name = 'recenzija_trener_clan_UNIQUE' AND non_unique = 0 AND seq_in_index = 1
                """, Integer.class));
    }

    // ------------------------------------------------------------ meko brisanje u toku B

    @Test
    void obrisanTrenerNePostojiZaClana() throws Exception {
        Korisnik trener = noviTrener("Obrisani", "Trener");
        Korisnik clan = noviClan("Gleda", "Clan");
        delete_(admin, "/api/admin/trainers/" + trener.id()).andExpect(status().isOk());

        get_(clan.token(), "/api/trainers").andExpect(jsonPath("$[*].trainerId", not(hasItem(trener.id()))));
        get_(clan.token(), "/api/trainers/" + trener.id()).andExpect(status().isNotFound());
        post_(clan.token(), "/api/trainers/" + trener.id() + "/follow", null).andExpect(status().isNotFound());
        recenzija(clan, trener, "{\"grade\":5}").andExpect(status().isNotFound());
    }

    @Test
    void zahtjevIPracenjeObrisanogClanaNestajuKodTrenera() throws Exception {
        Korisnik trener = noviTrener("Ostavljeni", "Trener");
        Korisnik ceka = noviClan("Ceka", "Clan");
        Korisnik prati = noviClan("Prati", "Clan");
        int zahtjevKojiCeka = posaljiZahtjev(ceka, trener);
        odluka(trener, posaljiZahtjev(prati, trener), "ACCEPTED").andExpect(status().isOk());

        delete_(radnik, "/api/worker/members/" + ceka.id()).andExpect(status().isOk());
        delete_(radnik, "/api/worker/members/" + prati.id()).andExpect(status().isOk());

        get_(trener.token(), "/api/trainer/follow-requests").andExpect(jsonPath("$").isEmpty());
        odluka(trener, zahtjevKojiCeka, "ACCEPTED").andExpect(status().isNotFound());
        get_(trener.token(), "/api/trainer/followers").andExpect(jsonPath("$").isEmpty());
        get_(admin, "/api/admin/trainers/" + trener.id()).andExpect(jsonPath("$.followerCount").value(0));
    }

    // ------------------------------------------------------------ administracija (5.2, 4.2, 4.3)

    @Test
    void izmjenaTeretaneSeVidiUJavnojRuti() throws Exception {
        zahtjev(put("/api/gym/info"), admin, """
                {"name":"Pumpd Gym IT","description":"Opis iz testa.","membershipPrice":45.5,
                 "dateOfEstablishment":"2019-03-15","workingHours":"Pon-Ned 06-23"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Informacije o teretani su azurirane."));

        get_(null, "/api/gym/info")
                .andExpect(jsonPath("$.name").value("Pumpd Gym IT"))
                .andExpect(jsonPath("$.description").value("Opis iz testa."))
                .andExpect(jsonPath("$.membershipPrice").value(45.5))
                .andExpect(jsonPath("$.dateOfEstablishment").value("2019-03-15"))
                .andExpect(jsonPath("$.workingHours").value("Pon-Ned 06-23"));
        assertEquals("Ulica Zdravlja 12, Banja Luka", jdbc.queryForObject(
                "SELECT adresa FROM teretana WHERE idteretana = 1", String.class), "kolone van ugovora ostaju");

        zahtjev(put("/api/gym/info"), admin, """
                {"name":"N","description":"D","membershipPrice":45.5,"dateOfEstablishment":"2999-01-01","workingHours":"W"}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void listaProblemaUkljucujeNeobradjenBezAdministratora() throws Exception {
        get_(admin, "/api/admin/issues")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Neispravna traka za trčanje")))
                .andExpect(jsonPath("$[*].title", hasItem("Nema tople vode")))
                .andExpect(jsonPath("$[0].reportDate").isString());
        get_(admin, "/api/admin/issues?status=xyz").andExpect(status().isBadRequest());
    }

    @Test
    void obradaProblemaMijenjaStatusIPostavljaAdministratora() throws Exception {
        String naslov = "IT problem " + System.nanoTime();
        int issueId = broj(post_(null, "/api/issues", """
                {"title":"%s","description":"Opis."}
                """.formatted(naslov)).andExpect(status().isCreated()), "$.issueId");

        mockMvc.perform(get("/api/admin/issues").param("status", "UNPROCESSED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(jsonPath("$[*].issueId", hasItem(issueId)));

        patch_(admin, "/api/admin/issues/" + issueId, "{\"status\":\"IN_PROGRESS\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueId").value(issueId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/admin/issues").param("status", "IN_PROGRESS")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(jsonPath("$[*].issueId", hasItem(issueId)));
        mockMvc.perform(get("/api/admin/issues").param("status", "UNPROCESSED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(jsonPath("$[*].issueId", not(hasItem(issueId))));

        Integer idAdmina = jdbc.queryForObject("SELECT idnalog FROM nalog WHERE email = 'admin@pumpd.local'", Integer.class);
        assertEquals(idAdmina, jdbc.queryForObject(
                "SELECT administrator_zaposleni_nalog_idnalog FROM problem WHERE idproblem = ?", Integer.class, issueId));
        assertEquals("u_obradi", jdbc.queryForObject(
                "SELECT status FROM problem WHERE idproblem = ?", String.class, issueId));

        patch_(admin, "/api/admin/issues/999999", "{\"status\":\"PROCESSED\"}").andExpect(status().isNotFound());
    }
}
