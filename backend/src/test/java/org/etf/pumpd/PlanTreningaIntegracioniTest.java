package org.etf.pumpd;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
 * Integracioni test planova treninga (3.1–3.5) nad pravom MySQL bazom u Dockeru.
 * Svaki test pravi svoje planove, pa ne zavisi od drugih testova koji dijele bazu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class PlanTreningaIntegracioniTest {

    private static final String SEED_LOZINKA = "pumpd123";
    private static final String RUTA = "/api/training-plans";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private String clan1;
    private String clan2;
    private String trener;
    private String radnik;

    @BeforeEach
    void prijava() throws Exception {
        clan1 = prijava("clan1@pumpd.local");
        clan2 = prijava("clan2@pumpd.local");
        trener = prijava("trener1@pumpd.local");
        radnik = prijava("radnik@pumpd.local");
    }

    private String prijava(String email) throws Exception {
        String odgovor = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, SEED_LOZINKA)))
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

    private int kreiraj(String token, String naziv) throws Exception {
        String odgovor = zahtjev(post(RUTA), token, """
                {"name":"%s","description":"Opis","durationMinutes":60,
                 "exercises":[{"name":"Cucanj","numSets":4,"numRepetitions":8},
                              {"name":"Iskorak","numSets":3,"numRepetitions":12}]}
                """.formatted(naziv))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.planId");
    }

    private List<Integer> idVjezbi(String token, int planId) throws Exception {
        String odgovor = zahtjev(get(RUTA + "/" + planId), token, null)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.exercises[*].exerciseId");
    }

    private int brojRedova(String tabela, String uslov, Object... parametri) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela + " WHERE " + uslov, Integer.class, parametri);
    }

    @Test
    void clanPraviMijenjaIBriseSvojPlan() throws Exception {
        int planId = kreiraj(clan1, "Snaga clan1");

        zahtjev(get(RUTA), clan1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].planId", hasItem(planId)));
        zahtjev(get(RUTA + "/" + planId), clan1, null)
                .andExpect(jsonPath("$.name").value("Snaga clan1"))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.exercises.length()").value(2))
                .andExpect(jsonPath("$.exercises[0].name").value("Cucanj"));

        List<Integer> vjezbe = idVjezbi(clan1, planId);
        int cucanj = vjezbe.get(0);
        int iskorak = vjezbe.get(1);

        // čučanj se mijenja, iskorak se briše, zgib se dodaje
        zahtjev(put(RUTA + "/" + planId), clan1, """
                {"name":"Snaga 2","description":null,"durationMinutes":45,
                 "exercises":[{"exerciseId":%d,"name":"Cucanj sa sipkom","numSets":5,"numRepetitions":5},
                              {"name":"Zgib","numSets":3,"numRepetitions":10}]}
                """.formatted(cucanj)).andExpect(status().isOk());

        zahtjev(get(RUTA + "/" + planId), clan1, null)
                .andExpect(jsonPath("$.name").value("Snaga 2"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.durationMinutes").value(45))
                .andExpect(jsonPath("$.exercises.length()").value(2))
                .andExpect(jsonPath("$.exercises[0].exerciseId").value(cucanj))
                .andExpect(jsonPath("$.exercises[0].name").value("Cucanj sa sipkom"))
                .andExpect(jsonPath("$.exercises[0].numSets").value(5))
                .andExpect(jsonPath("$.exercises[1].name").value("Zgib"));
        assertEquals(0, brojRedova("vjezba", "idvjezba = ?", iskorak));

        List<Integer> poslijeIzmjene = idVjezbi(clan1, planId);
        zahtjev(delete(RUTA + "/" + planId), clan1, null).andExpect(status().isOk());

        zahtjev(get(RUTA + "/" + planId), clan1, null).andExpect(status().isNotFound());
        zahtjev(get(RUTA), clan1, null).andExpect(jsonPath("$[*].planId", not(hasItem(planId))));
        assertEquals(0, brojRedova("vjezba_u_treningu", "plan_treninga_idplanTreninga = ?", planId));
        for (int idVjezbe : poslijeIzmjene) {
            assertEquals(0, brojRedova("vjezba", "idvjezba = ?", idVjezbe));
        }
    }

    @Test
    void trenerJeVlasnikSvogPlana() throws Exception {
        int planId = kreiraj(trener, "Plan trenera");

        assertEquals(1, brojRedova("plan_treninga",
                "idplanTreninga = ? AND trener_nalog_idnalog IS NOT NULL AND clan_nalog_idnalog IS NULL", planId));
        zahtjev(get(RUTA), trener, null).andExpect(jsonPath("$[*].planId", hasItem(planId)));
        zahtjev(get(RUTA), clan1, null).andExpect(jsonPath("$[*].planId", not(hasItem(planId))));
    }

    @Test
    void tudjiPlanDaje403() throws Exception {
        int planId = kreiraj(clan1, "Privatni plan");
        String izmjena = """
                {"name":"Tudji","durationMinutes":30,"exercises":[{"name":"A","numSets":1,"numRepetitions":1}]}
                """;

        zahtjev(get(RUTA + "/" + planId), clan2, null).andExpect(status().isForbidden());
        zahtjev(put(RUTA + "/" + planId), clan2, izmjena).andExpect(status().isForbidden());
        zahtjev(delete(RUTA + "/" + planId), trener, null).andExpect(status().isForbidden());

        zahtjev(get(RUTA + "/" + planId), clan1, null).andExpect(jsonPath("$.name").value("Privatni plan"));
    }

    @Test
    void vjezbaIzDrugogPlanaDaje400() throws Exception {
        int prvi = kreiraj(clan1, "Prvi");
        int drugi = kreiraj(clan1, "Drugi");
        int tudjaVjezba = idVjezbi(clan1, drugi).get(0);

        zahtjev(put(RUTA + "/" + prvi), clan1, """
                {"name":"Prvi","durationMinutes":60,
                 "exercises":[{"exerciseId":%d,"name":"X","numSets":1,"numRepetitions":1}]}
                """.formatted(tudjaVjezba)).andExpect(status().isBadRequest());

        assertEquals(2, idVjezbi(clan1, prvi).size());        // ništa se nije promijenilo
    }

    @Test
    void nepostojeciPlanDaje404() throws Exception {
        zahtjev(get(RUTA + "/999999"), clan1, null).andExpect(status().isNotFound());
        zahtjev(delete(RUTA + "/999999"), clan1, null).andExpect(status().isNotFound());
    }

    @Test
    void radnikNemaPristupPlanovima() throws Exception {
        zahtjev(get(RUTA), radnik, null).andExpect(status().isForbidden());
    }
}
