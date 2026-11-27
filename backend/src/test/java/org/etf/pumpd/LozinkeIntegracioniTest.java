package org.etf.pumpd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
 * Sigurnost (2.4.3): lozinke se u bazi čuvaju samo kao BCrypt heš, nad pravom bazom, i nijedan
 * odgovor API-ja ne sadrži heš lozinke.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class LozinkeIntegracioniTest {

    /** BCrypt: $2a$/$2b$/$2y$, cijena (2 cifre), pa 53 znaka soli i heša. */
    private static final Pattern BCRYPT = Pattern.compile("^\\$2[aby]\\$(\\d{2})\\$[./A-Za-z0-9]{53}$");
    private static final Pattern HES_U_TEKSTU = Pattern.compile("\\$2[aby]\\$\\d{2}\\$");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

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

    private String hesIzBaze(String email) {
        return jdbc.queryForObject("SELECT lozinka FROM nalog WHERE email = ?", String.class, email);
    }

    private static void provjeriBcrypt(String hes, String opis) {
        var poklapanje = BCRYPT.matcher(hes == null ? "" : hes);
        assertTrue(poklapanje.matches(), opis + " nije BCrypt heš");
        assertTrue(Integer.parseInt(poklapanje.group(1)) >= 10, opis + ": cijena BCrypt-a manja od 10");
    }

    @Test
    void sviNaloziUBaziImajuBcryptHes() {
        // uključuje seed naloge i sve naloge koje su napravili drugi testovi preko ruta
        List<Map<String, Object>> nalozi = jdbc.queryForList("SELECT email, lozinka FROM nalog");
        assertFalse(nalozi.isEmpty());
        for (Map<String, Object> nalog : nalozi) {
            provjeriBcrypt((String) nalog.get("lozinka"), "lozinka naloga " + nalog.get("email"));
        }
    }

    @Test
    void novaIPromijenjenaLozinkaSeCuvajuKaoHes() throws Exception {
        String radnik = prijava("radnik@pumpd.local", "pumpd123");
        String email = "hes." + System.nanoTime() + "@mail.com";
        String odgovor = zahtjev(post("/api/worker/members"), radnik,
                "{\"name\":\"Hes\",\"surname\":\"Test\",\"email\":\"%s\"}".formatted(email))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String privremena = JsonPath.read(odgovor, "$.temporaryPassword");

        String hes = hesIzBaze(email);
        provjeriBcrypt(hes, "privremena lozinka");
        assertFalse(hes.contains(privremena), "privremena lozinka je u bazi kao čist tekst");

        String token = prijava(email, privremena);
        zahtjev(put("/api/users/my-password"), token,
                "{\"oldPassword\":\"%s\",\"newPassword\":\"NovaLozinka123!\"}".formatted(privremena))
                .andExpect(status().isOk());

        String noviHes = hesIzBaze(email);
        provjeriBcrypt(noviHes, "nova lozinka");
        assertNotEquals(hes, noviHes);
        assertFalse(noviHes.contains("NovaLozinka123!"));
        prijava(email, "NovaLozinka123!");
    }

    @Test
    void istaLozinkaDajeRazliciteHeseve() throws Exception {
        // BCrypt dodaje nasumičnu so: dva naloga kojima aplikacija postavi istu lozinku nemaju isti heš.
        // (Seed nalozi iz V4 dijele jedan unaprijed izračunat heš, jer ga upisuje SQL, a ne aplikacija.)
        String radnik = prijava("radnik@pumpd.local", "pumpd123");
        String[] emailovi = new String[2];
        for (int i = 0; i < 2; i++) {
            emailovi[i] = "so" + i + "." + System.nanoTime() + "@mail.com";
            String odgovor = zahtjev(post("/api/worker/members"), radnik,
                    "{\"name\":\"So\",\"surname\":\"Test\",\"email\":\"%s\"}".formatted(emailovi[i]))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            String privremena = JsonPath.read(odgovor, "$.temporaryPassword");
            zahtjev(put("/api/users/my-password"), prijava(emailovi[i], privremena),
                    "{\"oldPassword\":\"%s\",\"newPassword\":\"IstaLozinka123!\"}".formatted(privremena))
                    .andExpect(status().isOk());
        }
        assertNotEquals(hesIzBaze(emailovi[0]), hesIzBaze(emailovi[1]));
    }

    @Test
    void odgovoriNeSadrzeHesLozinke() throws Exception {
        String admin = prijava("admin@pumpd.local", "pumpd123");
        String radnik = prijava("radnik@pumpd.local", "pumpd123");
        String trener = prijava("trener1@pumpd.local", "pumpd123");
        String clan = prijava("clan1@pumpd.local", "pumpd123");
        int idClana = jdbc.queryForObject("SELECT idnalog FROM nalog WHERE email = 'clan1@pumpd.local'", Integer.class);
        int idTrenera = jdbc.queryForObject("SELECT idnalog FROM nalog WHERE email = 'trener1@pumpd.local'", Integer.class);
        int idRadnika = jdbc.queryForObject("SELECT idnalog FROM nalog WHERE email = 'radnik@pumpd.local'", Integer.class);

        List<String> odgovori = List.of(
                tekst(get("/api/users/my-profile"), admin), tekst(get("/api/users/my-profile"), radnik),
                tekst(get("/api/users/my-profile"), trener), tekst(get("/api/users/my-profile"), clan),
                tekst(get("/api/worker/members"), radnik), tekst(get("/api/worker/members/" + idClana), radnik),
                tekst(get("/api/admin/trainers"), admin), tekst(get("/api/admin/trainers/" + idTrenera), admin),
                tekst(get("/api/admin/workers"), admin), tekst(get("/api/admin/workers/" + idRadnika), admin),
                tekst(get("/api/trainers"), clan), tekst(get("/api/trainers/" + idTrenera), clan),
                tekst(get("/api/trainer/followers"), trener));
        for (String odgovor : odgovori) {
            assertFalse(HES_U_TEKSTU.matcher(odgovor).find(), "odgovor sadrži heš lozinke: " + odgovor);
            assertFalse(odgovor.toLowerCase().contains("\"password\""), "odgovor sadrži polje password: " + odgovor);
            assertFalse(odgovor.toLowerCase().contains("lozinka\""), "odgovor sadrži polje lozinka: " + odgovor);
        }
        assertEquals(13, odgovori.size());
    }

    private String tekst(MockHttpServletRequestBuilder builder, String token) throws Exception {
        return zahtjev(builder, token, null).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
