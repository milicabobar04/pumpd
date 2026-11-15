package org.etf.pumpd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

/**
 * Integracioni test dolazaka i zauzetosti nad pravom MySQL bazom u Dockeru.
 * Svaki test pravi svog člana (radnik ga dodaje i postavlja mu članarinu), a zauzetost
 * se provjerava kao razlika prije i poslije, jer drugi testovi dijele istu bazu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class DolazakIntegracioniTest {

    private static final String SEED_LOZINKA = "pumpd123";
    private static final AtomicInteger BROJAC = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private String radnik;
    private String clan1;

    @BeforeEach
    void prijava() throws Exception {
        radnik = prijava("radnik@pumpd.local", SEED_LOZINKA);
        clan1 = prijava("clan1@pumpd.local", SEED_LOZINKA);
    }

    private record Clan(int id, String qrKod) {
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

    /** Novi član; ako je istek zadat, dobija i članarinu do tog datuma. */
    private Clan noviClan(LocalDate istekClanarine) throws Exception {
        String email = "dolazak." + System.nanoTime() + "." + BROJAC.incrementAndGet() + "@mail.com";
        String odgovor = zahtjev(post("/api/worker/members"), radnik, """
                {"name":"Dolazak","surname":"Test","email":"%s"}
                """.formatted(email)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(odgovor, "$.memberId");

        if (istekClanarine != null) {
            zahtjev(put("/api/worker/members/" + id + "/membership"), radnik, """
                    {"paymentDate":"%s","validUntil":"%s","price":40}
                    """.formatted(istekClanarine.minusMonths(1), istekClanarine))
                    .andExpect(status().isOk());
        }
        String qrKod = jdbc.queryForObject("SELECT qrKod FROM clan WHERE nalog_idnalog = ?", String.class, id);
        return new Clan(id, qrKod);
    }

    private ResultActions ulazak(String qrKod) throws Exception {
        return zahtjev(post("/api/worker/attendance/entry"), radnik, "{\"qrCode\":\"%s\"}".formatted(qrKod));
    }

    private ResultActions izlazak(String qrKod) throws Exception {
        return zahtjev(post("/api/worker/attendance/exit"), radnik, "{\"qrCode\":\"%s\"}".formatted(qrKod));
    }

    private int prisutni() throws Exception {
        String odgovor = zahtjev(get("/api/gym/occupancy"), clan1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(80))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.currentMembers");
    }

    private int brojDolazaka(int idClana) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dolazak WHERE clan_nalog_idnalog = ?", Integer.class, idClana);
    }

    @Test
    void ulazakIIzlazakMijenjajuZauzetost() throws Exception {
        Clan clan = noviClan(LocalDate.now().plusMonths(1));
        int prije = prisutni();

        ulazak(clan.qrKod())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(clan.id()))
                .andExpect(jsonPath("$.memberName").value("Dolazak Test"))
                .andExpect(jsonPath("$.isAllowed").value(true));
        assertEquals(prije + 1, prisutni());

        ulazak(clan.qrKod()).andExpect(status().isConflict());              // već je unutra
        assertEquals(1, brojDolazaka(clan.id()));

        izlazak(clan.qrKod())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitTime").isString());
        assertEquals(prije, prisutni());

        izlazak(clan.qrKod()).andExpect(status().isConflict());             // izlazak bez ulaska
    }

    @Test
    void clanarinaKojaIsticeDanasDozvoljavaUlazak() throws Exception {
        Clan clan = noviClan(LocalDate.now());

        ulazak(clan.qrKod()).andExpect(jsonPath("$.isAllowed").value(true));
        izlazak(clan.qrKod()).andExpect(status().isOk());
    }

    @Test
    void isteklaClanarinaNeDozvoljavaUlazakINistaSeNeUpisuje() throws Exception {
        Clan clan = noviClan(LocalDate.now().minusDays(1));

        ulazak(clan.qrKod())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAllowed").value(false));
        assertEquals(0, brojDolazaka(clan.id()));
    }

    @Test
    void clanBezClanarineNijeDozvoljen() throws Exception {
        Clan clan = noviClan(null);

        ulazak(clan.qrKod()).andExpect(jsonPath("$.isAllowed").value(false));
        assertEquals(0, brojDolazaka(clan.id()));
    }

    @Test
    void nepoznatIliObrisanClanDaje404() throws Exception {
        ulazak("ne-postoji-" + System.nanoTime()).andExpect(status().isNotFound());

        Clan clan = noviClan(LocalDate.now().plusMonths(1));
        zahtjev(delete("/api/worker/members/" + clan.id()), radnik, null).andExpect(status().isOk());
        ulazak(clan.qrKod()).andExpect(status().isNotFound());
    }

    @Test
    void boravakIzJucerNeBlokiraUlazakINeBrojiSe() throws Exception {
        Clan clan = noviClan(LocalDate.now().plusMonths(1));
        jdbc.update("INSERT INTO dolazak (clan_nalog_idnalog, vrijemeUlaska) VALUES (?, NOW() - INTERVAL 1 DAY)",
                clan.id());
        int prije = prisutni();

        izlazak(clan.qrKod()).andExpect(status().isConflict());              // jučerašnji se ne zatvara
        ulazak(clan.qrKod()).andExpect(jsonPath("$.isAllowed").value(true));
        assertEquals(prije + 1, prisutni());
        izlazak(clan.qrKod()).andExpect(status().isOk());
    }

    @Test
    void evidencijuRadiSamoRadnik() throws Exception {
        zahtjev(post("/api/worker/attendance/entry"), clan1, "{\"qrCode\":\"SEED-QR-CLAN1\"}")
                .andExpect(status().isForbidden());
    }
}
