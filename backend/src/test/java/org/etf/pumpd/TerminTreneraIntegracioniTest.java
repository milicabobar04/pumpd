package org.etf.pumpd;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * Integracioni test trenerovih termina (10.4–10.7) nad pravom MySQL bazom u Dockeru.
 * Svaki test koristi svoj dan daleko u budućnosti i jedinstven naziv termina, pa se
 * termini različitih testova ne preklapaju, a obavještenja se mogu tačno prebrojati.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class TerminTreneraIntegracioniTest {

    private static final String SEED_LOZINKA = "pumpd123";
    private static final String TERMINI = "/api/trainer/group-trainings";
    private static final AtomicInteger BROJAC = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private String trener2;
    private String trener1;
    private LocalDate dan;
    private String naziv;

    @BeforeEach
    void pripremi() throws Exception {
        trener1 = prijava("trener1@pumpd.local");
        trener2 = prijava("trener2@pumpd.local");
        int redniBroj = BROJAC.incrementAndGet();
        dan = LocalDate.now().plusDays(500 + redniBroj);
        naziv = "Test termin " + System.nanoTime() % 1_000_000;
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

    private static String tijelo(String naziv, LocalDateTime pocetak, int trajanje, int kapacitet) {
        return """
                {"name":"%s","startTime":"%s","durationMinutes":%d,"maxMembers":%d}
                """.formatted(naziv, pocetak, trajanje, kapacitet);
    }

    private int kreiraj(LocalDateTime pocetak, int trajanje, int kapacitet) throws Exception {
        String odgovor = zahtjev(post(TERMINI), trener2, tijelo(naziv, pocetak, trajanje, kapacitet))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.sessionId");
    }

    private int idNaloga(String email) {
        return jdbc.queryForObject("SELECT idnalog FROM nalog WHERE email = ?", Integer.class, email);
    }

    private int idGrupe(int idTermina) {
        return jdbc.queryForObject("SELECT grupa_idgrupa FROM termin WHERE idtermin = ?", Integer.class, idTermina);
    }

    /** Član u grupi termina kao prihvaćen (oba reda, kao što to rade 10.10 i 10.13). */
    private void dodajClana(int idTermina, String email, String status) {
        int idClana = idNaloga(email);
        if (!status.equals("na_cekanju")) {
            jdbc.update("INSERT INTO clan_u_grupi (clan_nalog_idnalog, grupa_idgrupa, datumPristupanja) VALUES (?, ?, CURDATE())",
                    idClana, idGrupe(idTermina));
        }
        jdbc.update("INSERT INTO clan_na_terminu (termin_idtermin, clan_nalog_idnalog, status, datumZahtjeva) VALUES (?, ?, ?, CURDATE())",
                idTermina, idClana, status);
    }

    private int obavjestenja(String email, String dioTeksta) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM obavjestenje WHERE primalac_nalog_idnalog = ? AND sadrzaj LIKE ?",
                Integer.class, idNaloga(email), "%" + naziv + "%" + dioTeksta + "%");
    }

    private int brojRedova(String tabela, String uslov, Object parametar) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela + " WHERE " + uslov, Integer.class, parametar);
    }

    @Test
    void kreiranjeRasporedIPreklapanje() throws Exception {
        LocalDateTime pocetak = dan.atTime(10, 0);
        int idTermina = kreiraj(pocetak, 60, 12);

        zahtjev(get("/api/trainer/schedule").param("date", dan.toString()), trener2, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sessionId").value(idTermina))
                .andExpect(jsonPath("$[0].name").value(naziv))
                .andExpect(jsonPath("$[0].startTime").value(pocetak + ":00"))
                .andExpect(jsonPath("$[0].enrolledCount").value(0))
                .andExpect(jsonPath("$[0].maxMembers").value(12));

        // preklapanje sa sopstvenim terminom → 409, termin odmah poslije → 201
        zahtjev(post(TERMINI), trener2, tijelo(naziv, pocetak.plusMinutes(30), 60, 5)).andExpect(status().isConflict());
        kreiraj(pocetak.plusMinutes(60), 30, 5);

        // drugi trener smije imati termin u isto vrijeme (sala se ne koristi)
        zahtjev(post(TERMINI), trener1, tijelo(naziv, pocetak, 60, 5)).andExpect(status().isCreated());

        // termin u prošlosti → 400
        zahtjev(post(TERMINI), trener2, tijelo(naziv, LocalDateTime.now().minusHours(1).withNano(0), 60, 5))
                .andExpect(status().isBadRequest());

        // svaki termin ima svoju grupu
        assertEquals(1, brojRedova("grupa", "idgrupa = ?", idGrupe(idTermina)));
    }

    @Test
    void izmjenaKapacitetaIPomjeranjeTermina() throws Exception {
        int idTermina = kreiraj(dan.atTime(10, 0), 60, 10);
        dodajClana(idTermina, "clan1@pumpd.local", "prijavljen");
        dodajClana(idTermina, "clan2@pumpd.local", "prijavljen");

        zahtjev(put(TERMINI + "/" + idTermina), trener2, tijelo(naziv, dan.atTime(10, 0), 60, 1))
                .andExpect(status().isConflict());

        zahtjev(put(TERMINI + "/" + idTermina), trener2, tijelo(naziv, dan.atTime(12, 0), 45, 2))
                .andExpect(status().isOk());

        zahtjev(get("/api/trainer/schedule").param("date", dan.toString()), trener2, null)
                .andExpect(jsonPath("$[0].startTime").value(dan.atTime(12, 0) + ":00"))
                .andExpect(jsonPath("$[0].durationMinutes").value(45))
                .andExpect(jsonPath("$[0].enrolledCount").value(2))
                .andExpect(jsonPath("$[0].maxMembers").value(2));
        assertEquals(1, obavjestenja("clan1@pumpd.local", "pomjeren"));
        assertEquals(1, obavjestenja("clan2@pumpd.local", "pomjeren"));
    }

    @Test
    void brisanjeObavjestavaClanoveIBriseSve() throws Exception {
        int idTermina = kreiraj(dan.atTime(10, 0), 60, 10);
        int idGrupe = idGrupe(idTermina);
        dodajClana(idTermina, "clan3@pumpd.local", "prijavljen");
        dodajClana(idTermina, "clan4@pumpd.local", "na_cekanju");

        zahtjev(delete(TERMINI + "/" + idTermina), trener2, null).andExpect(status().isOk());

        assertEquals(1, obavjestenja("clan3@pumpd.local", "otkazan"));
        assertEquals(1, obavjestenja("clan4@pumpd.local", "otkazan"));
        assertEquals(0, brojRedova("termin", "idtermin = ?", idTermina));
        assertEquals(0, brojRedova("grupa", "idgrupa = ?", idGrupe));
        assertEquals(0, brojRedova("clan_u_grupi", "grupa_idgrupa = ?", idGrupe));
        assertEquals(0, brojRedova("clan_na_terminu", "termin_idtermin = ?", idTermina));
        zahtjev(delete(TERMINI + "/" + idTermina), trener2, null).andExpect(status().isNotFound());
    }

    @Test
    void tudjiTerminDaje403APrasliSeNeMijenjaNiBrise() throws Exception {
        int idTermina = kreiraj(dan.atTime(10, 0), 60, 10);
        zahtjev(put(TERMINI + "/" + idTermina), trener1, tijelo(naziv, dan.atTime(11, 0), 60, 10))
                .andExpect(status().isForbidden());
        zahtjev(delete(TERMINI + "/" + idTermina), trener1, null).andExpect(status().isForbidden());

        // seed: "CrossFit (prošli)" je termin trener3 od prošle sedmice
        String trener3 = prijava("trener3@pumpd.local");
        int prosli = jdbc.queryForObject("SELECT t.idtermin FROM termin t JOIN grupa g ON g.idgrupa = t.grupa_idgrupa "
                + "WHERE g.naziv = 'CrossFit (prošli)'", Integer.class);
        zahtjev(put(TERMINI + "/" + prosli), trener3, tijelo("CrossFit", dan.atTime(20, 0), 90, 15))
                .andExpect(status().isBadRequest());
        zahtjev(delete(TERMINI + "/" + prosli), trener3, null).andExpect(status().isConflict());

        zahtjev(put(TERMINI + "/999999"), trener2, tijelo(naziv, dan.atTime(11, 0), 60, 10))
                .andExpect(status().isNotFound());
    }

    @Test
    void rasporedVidiSamoSvojeTermineIClanNemaPristup() throws Exception {
        int idTermina = kreiraj(dan.atTime(10, 0), 60, 10);

        zahtjev(get("/api/trainer/schedule").param("date", dan.toString()), trener1, null)
                .andExpect(jsonPath("$[*].sessionId").value(org.hamcrest.Matchers.not(hasItem(idTermina))));
        zahtjev(get("/api/trainer/schedule"), prijava("clan1@pumpd.local"), null).andExpect(status().isForbidden());
    }
}
