package org.etf.pumpd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

/**
 * Integracioni test članarine nad pravom MySQL bazom u Dockeru: radnik postavlja i produžava
 * članarinu, a član vidi svoju članarinu sa QR kodom. Svaki test pravi svog člana.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class ClanarinaIntegracioniTest {

    private static final String SEED_LOZINKA = "pumpd123";
    private static final AtomicInteger BROJAC = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    private String radnik;

    @BeforeEach
    void prijaviRadnika() throws Exception {
        radnik = prijava("radnik@pumpd.local", SEED_LOZINKA);
    }

    private record Korisnik(int id, String token) {
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

    private Korisnik noviClan() throws Exception {
        String email = "clanarina." + System.nanoTime() + "." + BROJAC.incrementAndGet() + "@mail.com";
        String odgovor = zahtjev(post("/api/worker/members"), radnik, """
                {"name":"Clanarina","surname":"Test","email":"%s"}
                """.formatted(email)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(odgovor, "$.memberId");
        return new Korisnik(id, prijava(email, JsonPath.read(odgovor, "$.temporaryPassword")));
    }

    private static String ruta(Korisnik clan) {
        return "/api/worker/members/" + clan.id() + "/membership";
    }

    private ResultActions postavi(Korisnik clan, LocalDate uplata, LocalDate istek, double cijena) throws Exception {
        return zahtjev(put(ruta(clan)), radnik, """
                {"paymentDate":"%s","validUntil":"%s","price":%s}
                """.formatted(uplata, istek, cijena));
    }

    @Test
    void noviClanNemaClanarinu() throws Exception {
        Korisnik clan = noviClan();

        zahtjev(get("/api/membership/my-status"), clan.token(), null).andExpect(status().isNotFound());
        zahtjev(get(ruta(clan)), radnik, null).andExpect(status().isNotFound());
        zahtjev(post(ruta(clan) + "/extend"), radnik, "{\"numMonths\":1}").andExpect(status().isNotFound());
    }

    @Test
    void cijeliTokPostavljanjaIProduzenja() throws Exception {
        Korisnik clan = noviClan();
        LocalDate danas = LocalDate.now();

        postavi(clan, danas, danas.plusDays(10), 40.0).andExpect(status().isOk());

        zahtjev(get(ruta(clan)), radnik, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validUntil").value(danas.plusDays(10).toString()))
                .andExpect(jsonPath("$.price").value(40.0))
                .andExpect(jsonPath("$.isActive").value(true));

        // aktivna: produžava se od datuma isteka
        zahtjev(post(ruta(clan) + "/extend"), radnik, "{\"numMonths\":2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newValidUntil").value(danas.plusDays(10).plusMonths(2).toString()));

        // član vidi promjenu i dobija QR kod
        zahtjev(get("/api/membership/my-status"), clan.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentDate").value(danas.toString()))
                .andExpect(jsonPath("$.validUntil").value(danas.plusDays(10).plusMonths(2).toString()))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.qrCodeUrl").value(org.hamcrest.Matchers.startsWith("data:image/png;base64,")));
    }

    @Test
    void isteklaClanarinaSeProduzavaOdDanas() throws Exception {
        Korisnik clan = noviClan();
        LocalDate danas = LocalDate.now();

        postavi(clan, danas.minusMonths(2), danas.minusMonths(1), 40.0).andExpect(status().isOk());
        zahtjev(get("/api/membership/my-status"), clan.token(), null)
                .andExpect(jsonPath("$.isActive").value(false));

        String odgovor = zahtjev(post(ruta(clan) + "/extend"), radnik, "{\"numMonths\":1}")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(danas.plusMonths(1).toString(), JsonPath.read(odgovor, "$.newValidUntil"));
    }

    @Test
    void ponovnoPostavljanjePrepisujeClanarinu() throws Exception {
        Korisnik clan = noviClan();
        LocalDate danas = LocalDate.now();

        postavi(clan, danas, danas.plusMonths(1), 40.0).andExpect(status().isOk());
        postavi(clan, danas.minusDays(1), danas.plusMonths(3), 99.5).andExpect(status().isOk());

        zahtjev(get(ruta(clan)), radnik, null)
                .andExpect(jsonPath("$.paymentDate").value(danas.minusDays(1).toString()))
                .andExpect(jsonPath("$.validUntil").value(danas.plusMonths(3).toString()))
                .andExpect(jsonPath("$.price").value(99.5));
    }

    @Test
    void neispravniZahtjeviVracaju400INistaNeUpisuju() throws Exception {
        Korisnik clan = noviClan();
        LocalDate danas = LocalDate.now();

        postavi(clan, danas, danas.minusDays(1), 40.0).andExpect(status().isBadRequest());
        postavi(clan, danas, danas.plusDays(1), -5).andExpect(status().isBadRequest());
        zahtjev(get(ruta(clan)), radnik, null).andExpect(status().isNotFound());

        postavi(clan, danas, danas.plusDays(1), 40.0).andExpect(status().isOk());
        zahtjev(post(ruta(clan) + "/extend"), radnik, "{\"numMonths\":25}").andExpect(status().isBadRequest());
        zahtjev(get(ruta(clan)), radnik, null)
                .andExpect(jsonPath("$.validUntil").value(danas.plusDays(1).toString()));
    }

    @Test
    void pristupPoUlogama() throws Exception {
        Korisnik clan = noviClan();

        zahtjev(get("/api/membership/my-status"), radnik, null).andExpect(status().isForbidden());
        zahtjev(get(ruta(clan)), clan.token(), null).andExpect(status().isForbidden());
        zahtjev(get("/api/membership/my-status"), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void nepostojeciClanVraca404() throws Exception {
        zahtjev(get("/api/worker/members/999999/membership"), radnik, null).andExpect(status().isNotFound());
    }
}
