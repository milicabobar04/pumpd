package org.etf.pumpd;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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
 * Integracioni test označavanja obavještenja kao pročitanih nad pravom MySQL bazom u Dockeru.
 * Obavještenja nastaju stvarnim radnjama (zahtjev za praćenje, 7.3), a svaki test pravi svoje naloge.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class ObavjestenjaIntegracioniTest {

    private static final AtomicInteger BROJAC = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private String admin;
    private String radnik;

    @BeforeEach
    void prijaviSeedKorisnike() throws Exception {
        admin = prijava("admin@pumpd.local", "pumpd123");
        radnik = prijava("radnik@pumpd.local", "pumpd123");
    }

    private record Korisnik(int id, String token) {
    }

    private String prijava(String email, String lozinka) throws Exception {
        String odgovor = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, lozinka)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
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

    private Korisnik noviNalog(String token, String ruta, String poljeId, String ime) throws Exception {
        String email = ime + "." + System.nanoTime() + "." + BROJAC.incrementAndGet() + "@mail.com";
        String odgovor = zahtjev(post(ruta), token,
                "{\"name\":\"%s\",\"surname\":\"Test\",\"email\":\"%s\"}".formatted(ime, email))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(odgovor, "$." + poljeId);
        return new Korisnik(id, prijava(email, JsonPath.read(odgovor, "$.temporaryPassword")));
    }

    /** Trener sa dva obavještenja: dva člana su mu poslala zahtjev za praćenje. */
    private Korisnik trenerSaDvaObavjestenja() throws Exception {
        Korisnik trener = noviNalog(admin, "/api/admin/trainers", "trainerId", "Trener");
        for (String ime : List.of("Prvi", "Drugi")) {
            Korisnik clan = noviNalog(radnik, "/api/worker/members", "memberId", ime);
            zahtjev(post("/api/trainers/" + trener.id() + "/follow"), clan.token(), null).andExpect(status().isCreated());
        }
        return trener;
    }

    private List<Integer> idObavjestenja(Korisnik korisnik) throws Exception {
        String odgovor = zahtjev(get("/api/notifications"), korisnik.token(), null)
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$[*].notificationId");
    }

    private boolean procitanoUBazi(int idObavjestenja) {
        return jdbc.queryForObject("SELECT procitano FROM obavjestenje WHERE idobavjestenje = ?",
                Boolean.class, idObavjestenja);
    }

    @Test
    void jednoObavjestenjeSeOznacavaKaoProcitano() throws Exception {
        Korisnik trener = trenerSaDvaObavjestenja();
        List<Integer> id = idObavjestenja(trener);
        assertEquals(2, id.size());
        zahtjev(get("/api/notifications"), trener.token(), null)
                .andExpect(jsonPath("$[*].isSeen", everyItem(is(false))));

        zahtjev(patch("/api/notifications/" + id.get(0) + "/read"), trener.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Obavjestenje je oznaceno kao procitano."));

        zahtjev(get("/api/notifications"), trener.token(), null)
                .andExpect(jsonPath("$[0].isSeen").value(true))
                .andExpect(jsonPath("$[1].isSeen").value(false));
        assertEquals(true, procitanoUBazi(id.get(0)));
        assertEquals(false, procitanoUBazi(id.get(1)));

        // ponovno označavanje nije greška
        zahtjev(patch("/api/notifications/" + id.get(0) + "/read"), trener.token(), null).andExpect(status().isOk());
    }

    @Test
    void svaObavjestenjaSeOznacavajuKaoProcitana() throws Exception {
        Korisnik trener = trenerSaDvaObavjestenja();

        zahtjev(patch("/api/notifications/read-all"), trener.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Obavjestenja su oznacena kao procitana."));

        zahtjev(get("/api/notifications"), trener.token(), null)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].isSeen", everyItem(is(true))));
        // bez nepročitanih i dalje 200
        zahtjev(patch("/api/notifications/read-all"), trener.token(), null).andExpect(status().isOk());
    }

    @Test
    void tudjeObavjestenjeSeNeMozeOznacitiINeMijenjaSe() throws Exception {
        Korisnik trener = trenerSaDvaObavjestenja();
        int tudje = idObavjestenja(trener).get(0);
        Korisnik drugi = noviNalog(radnik, "/api/worker/members", "memberId", "Drugi");

        zahtjev(patch("/api/notifications/" + tudje + "/read"), drugi.token(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Obavjestenje ne postoji."));
        zahtjev(patch("/api/notifications/read-all"), drugi.token(), null).andExpect(status().isOk());

        assertEquals(false, procitanoUBazi(tudje), "tuđe obavještenje je ostalo nepročitano");
        zahtjev(patch("/api/notifications/999999/read"), drugi.token(), null).andExpect(status().isNotFound());
    }

    @Test
    void radiZaSveUlogeITraziToken() throws Exception {
        for (String token : List.of(admin, radnik, prijava("trener1@pumpd.local", "pumpd123"),
                prijava("clan1@pumpd.local", "pumpd123"))) {
            zahtjev(patch("/api/notifications/read-all"), token, null).andExpect(status().isOk());
        }
        // seed obavještenja clan1 su sada pročitana
        zahtjev(get("/api/notifications"), prijava("clan1@pumpd.local", "pumpd123"), null)
                .andExpect(jsonPath("$[*].isSeen", hasItem(true)))
                .andExpect(jsonPath("$[*].isSeen", everyItem(is(true))));
        mockMvc.perform(patch("/api/notifications/read-all")).andExpect(status().isUnauthorized());
    }
}
