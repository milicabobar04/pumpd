package org.etf.pumpd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.etf.pumpd.enums.Uloga;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

/**
 * Sigurnost (2.4.3): zaštita po ulogama za SVAKU rutu iz openapi.yaml, nad pravim serverom i bazom.
 * Tabela PRISTUP je prepisana iz ugovora (polje "Pristup"); test provjerava i da tabela pokriva
 * tačno sve rute iz openapi.yaml, pa nova ruta bez upisanog pristupa obara test.
 *
 * Za svaku rutu i svaku ulogu: zabranjena uloga dobija 403, zahtjev bez tokena 401 (osim javnih
 * ruta), a dozvoljena uloga prolazi zaštitu (bilo koji odgovor osim 401 i 403). Id-jevi u putanji
 * ne postoje (999999), a tijelo je namjerno neispravan JSON: zaštita se provjerava prije čitanja
 * tijela, pa dozvoljena uloga dobija 400 ili 404 i seed podaci se ne mijenjaju.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PristupPoUlogamaIntegracioniTest {

    private static final String NEISPRAVNO_TIJELO = "{nije json";
    private static final Set<Uloga> SVI = EnumSet.allOf(Uloga.class);
    private static final Set<Uloga> JAVNO = Set.of();   // oznaka: ruta je javna (i bez tokena)

    private record Ruta(HttpMethod metoda, String putanja, Set<Uloga> uloge) {
        boolean javna() {
            return uloge == JAVNO;
        }

        String kljuc() {
            return metoda + " " + putanja.replaceAll("\\d{6}", "{}");
        }
    }

    private static Ruta r(String metoda, String putanja, Set<Uloga> uloge) {
        return new Ruta(HttpMethod.valueOf(metoda), putanja, uloge);
    }

    private static final Uloga M = Uloga.MEMBER, T = Uloga.TRAINER, W = Uloga.WORKER, A = Uloga.ADMIN;

    /** Iz ugovora (Pristup) + rute dodate kasnije (lozinka, slike objava, obavještenja). */
    private static final List<Ruta> PRISTUP = List.of(
            // 1. prijava
            r("POST", "/api/auth/login", JAVNO),
            r("POST", "/api/auth/logout", SVI),
            // 2. nalog i profil
            r("GET", "/api/users/my-profile", SVI),
            r("PUT", "/api/users/my-profile", SVI),
            r("PUT", "/api/users/my-password", SVI),
            r("GET", "/api/membership/my-status", Set.of(M)),
            r("GET", "/api/users/training-history", Set.of(M)),
            // 3. planovi treninga
            r("GET", "/api/training-plans", Set.of(M, T)),
            r("GET", "/api/training-plans/999999", Set.of(M, T)),
            r("POST", "/api/training-plans", Set.of(M, T)),
            r("PUT", "/api/training-plans/999999", Set.of(M, T)),
            r("DELETE", "/api/training-plans/999999", Set.of(M, T)),
            // 4. problemi
            r("POST", "/api/issues", JAVNO),
            r("GET", "/api/admin/issues", Set.of(A)),
            r("PATCH", "/api/admin/issues/999999", Set.of(A)),
            // 5. teretana
            r("GET", "/api/gym/info", JAVNO),
            r("PUT", "/api/gym/info", Set.of(A)),
            r("GET", "/api/gym/occupancy", SVI),
            // 6. obavještenja
            r("GET", "/api/notifications", SVI),
            r("PATCH", "/api/notifications/read-all", SVI),
            r("PATCH", "/api/notifications/999999/read", SVI),
            // 7. treneri (član)
            r("GET", "/api/trainers", Set.of(M)),
            r("GET", "/api/trainers/999999", Set.of(M)),
            r("POST", "/api/trainers/999999/follow", Set.of(M)),
            r("DELETE", "/api/trainers/999999/follow", Set.of(M)),
            r("POST", "/api/trainers/999999/reviews", Set.of(M)),
            // 8. objave
            r("GET", "/api/posts/feed", Set.of(M)),
            r("POST", "/api/posts/999999/save", Set.of(M)),
            r("GET", "/api/posts/saved", Set.of(M)),
            r("DELETE", "/api/posts/999999/save", Set.of(M)),
            r("GET", "/api/posts/my-posts", Set.of(T)),
            r("POST", "/api/posts", Set.of(T)),
            r("DELETE", "/api/posts/999999", Set.of(T)),
            r("POST", "/api/posts/images", Set.of(T)),
            r("GET", "/api/images/999999.jpg", JAVNO),
            // 9. grupni treninzi (član)
            r("GET", "/api/group-trainings", Set.of(M, W)),
            r("POST", "/api/group-trainings/999999/enrollment", Set.of(M)),
            r("DELETE", "/api/group-trainings/999999/enrollment", Set.of(M)),
            // 10. trener
            r("GET", "/api/trainer/reviews", Set.of(T)),
            r("GET", "/api/trainer/follow-requests", Set.of(T)),
            r("PATCH", "/api/trainer/follow-requests/999999", Set.of(T)),
            r("GET", "/api/trainer/followers", Set.of(T)),
            r("GET", "/api/trainer/schedule", Set.of(T)),
            r("POST", "/api/trainer/group-trainings", Set.of(T)),
            r("PUT", "/api/trainer/group-trainings/999999", Set.of(T)),
            r("DELETE", "/api/trainer/group-trainings/999999", Set.of(T)),
            r("GET", "/api/trainer/groups", Set.of(T)),
            r("GET", "/api/trainer/groups/999999/members", Set.of(T)),
            r("POST", "/api/trainer/groups/999999/members", Set.of(T)),
            r("DELETE", "/api/trainer/groups/999999/members/999998", Set.of(T)),
            r("GET", "/api/trainer/group-requests", Set.of(T)),
            r("PATCH", "/api/trainer/group-requests/999999", Set.of(T)),
            // 11. zaposleni
            r("GET", "/api/worker/members", Set.of(W, A)),
            r("GET", "/api/worker/members/999999", Set.of(W, A)),
            r("POST", "/api/worker/members", Set.of(W, A)),
            r("DELETE", "/api/worker/members/999999", Set.of(W, A)),
            r("GET", "/api/worker/members/999999/membership", Set.of(W, A)),
            r("PUT", "/api/worker/members/999999/membership", Set.of(W, A)),
            r("POST", "/api/worker/members/999999/membership/extend", Set.of(W, A)),
            r("POST", "/api/worker/attendance/entry", Set.of(W)),
            r("POST", "/api/worker/attendance/exit", Set.of(W)),
            // 12. administrator
            r("GET", "/api/admin/trainers", Set.of(A)),
            r("GET", "/api/admin/trainers/999999", Set.of(A)),
            r("POST", "/api/admin/trainers", Set.of(A)),
            r("DELETE", "/api/admin/trainers/999999", Set.of(A)),
            r("GET", "/api/admin/workers", Set.of(A)),
            r("GET", "/api/admin/workers/999999", Set.of(A)),
            r("POST", "/api/admin/workers", Set.of(A)),
            r("DELETE", "/api/admin/workers/999999", Set.of(A)));

    @Autowired
    private MockMvc mockMvc;

    private final Map<Uloga, String> tokeni = new EnumMap<>(Uloga.class);

    @BeforeAll
    void prijaviSeedKorisnike() throws Exception {
        tokeni.put(A, prijava("admin@pumpd.local"));
        tokeni.put(W, prijava("radnik@pumpd.local"));
        tokeni.put(T, prijava("trener1@pumpd.local"));
        tokeni.put(M, prijava("clan1@pumpd.local"));
    }

    private String prijava(String email) throws Exception {
        String odgovor = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"pumpd123\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.token");
    }

    private MockHttpServletResponse posalji(Ruta ruta, String token) throws Exception {
        MockHttpServletRequestBuilder zahtjev = request(ruta.metoda(), ruta.putanja());
        if (ruta.metoda() != HttpMethod.GET) {
            zahtjev.contentType(MediaType.APPLICATION_JSON).content(NEISPRAVNO_TIJELO);
        }
        if (token != null) {
            zahtjev.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(zahtjev).andReturn().getResponse();
    }

    /** Odjava poništava token, pa za nju svaka uloga dobija novi. */
    private String tokenZa(Ruta ruta, Uloga uloga) throws Exception {
        if (!ruta.putanja().equals("/api/auth/logout")) {
            return tokeni.get(uloga);
        }
        return prijava(switch (uloga) {
            case ADMIN -> "admin@pumpd.local";
            case WORKER -> "radnik@pumpd.local";
            case TRAINER -> "trener1@pumpd.local";
            case MEMBER -> "clan1@pumpd.local";
        });
    }

    @Test
    void tabelaPokrivaTacnoSveRuteIzOpenApi() throws Exception {
        String yaml = Files.readString(Path.of("docs/openapi.yaml"));
        Set<String> uOpenApi = new TreeSet<>();
        String putanja = null;
        Matcher red = Pattern.compile("(?m)^  (/\\S*):\\s*$|^    (get|post|put|patch|delete):\\s*$").matcher(yaml);
        while (red.find()) {
            if (red.group(1) != null) {
                putanja = "/api" + red.group(1).replaceAll("\\{[^}]+}", "{}");
            } else {
                uOpenApi.add(red.group(2).toUpperCase() + " " + putanja);
            }
        }
        Set<String> uTabeli = PRISTUP.stream()
                .map(r -> r.kljuc().replace("/api/images/{}.jpg", "/api/images/{}"))
                .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(69, uOpenApi.size(), "broj ruta u openapi.yaml");
        assertEquals(uOpenApi, uTabeli);
        assertEquals(PRISTUP.size(), uTabeli.size(), "ruta je u tabeli dvaput");
    }

    @Test
    void svakaRutaPropustaSamoSvojeUloge() throws Exception {
        List<String> greske = new ArrayList<>();
        for (Ruta ruta : PRISTUP) {
            MockHttpServletResponse bezTokena = posalji(ruta, null);
            if (ruta.javna()) {
                if (bezTokena.getStatus() == 401 || bezTokena.getStatus() == 403) {
                    greske.add(ruta.kljuc() + " je javna, a bez tokena daje " + bezTokena.getStatus());
                }
            } else if (bezTokena.getStatus() != 401) {
                greske.add(ruta.kljuc() + " bez tokena daje " + bezTokena.getStatus() + " umjesto 401");
            }

            for (Uloga uloga : Uloga.values()) {
                MockHttpServletResponse odgovor = posalji(ruta, tokenZa(ruta, uloga));
                boolean dozvoljena = ruta.javna() || ruta.uloge().contains(uloga);
                if (dozvoljena && (odgovor.getStatus() == 401 || odgovor.getStatus() == 403)) {
                    greske.add(ruta.kljuc() + " odbija dozvoljenu ulogu " + uloga + " (" + odgovor.getStatus() + ")");
                }
                if (!dozvoljena && (odgovor.getStatus() != 403
                        || !odgovor.getContentAsString().contains("Nemate dozvolu za pristup resursu."))) {
                    greske.add(ruta.kljuc() + " propušta zabranjenu ulogu " + uloga + " (" + odgovor.getStatus() + ")");
                }
            }
        }
        assertEquals(List.of(), greske, String.join("\n", greske));
    }

    @Test
    void nepostojecaRutaTraziPrijavu() throws Exception {
        // anyRequest().authenticated(): ruta koja ne postoji ne otkriva ništa bez tokena
        assertEquals(401, posalji(r("GET", "/api/nepostojeca", SVI), null).getStatus());
        assertNotEquals(401, posalji(r("GET", "/api/nepostojeca", SVI), tokeni.get(M)).getStatus());
    }

    @Test
    void odjavljenTokenSeViseNeMozeKoristiti() throws Exception {
        String token = prijava("clan2@pumpd.local");
        Ruta profil = r("GET", "/api/users/my-profile", SVI);
        assertEquals(200, posalji(profil, token).getStatus());

        assertEquals(200, posalji(r("POST", "/api/auth/logout", SVI), token).getStatus());

        assertEquals(401, posalji(profil, token).getStatus());
        assertEquals(200, posalji(profil, prijava("clan2@pumpd.local")).getStatus());   // nova prijava radi
    }

    @Test
    void lazniIIstekliTokenSuOdbijeni() throws Exception {
        Ruta profil = r("GET", "/api/users/my-profile", SVI);
        assertEquals(401, posalji(profil, "nije.pravi.token").getStatus());
        // token potpisan drugim ključem (npr. napravljen van servera)
        String tudjiKljuc = io.jsonwebtoken.Jwts.builder().subject("1")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(new byte[32])).compact();
        assertEquals(401, posalji(profil, tudjiKljuc).getStatus());
    }
}
