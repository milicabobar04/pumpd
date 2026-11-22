package org.etf.pumpd;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

/**
 * Integracioni test objava (8.1–8.7) i slika nad pravom MySQL bazom u Dockeru.
 * Pokreće se i pravi web server (nasumičan port) jer samo on primjenjuje ograničenje od 5 MB.
 * Slike idu u privremeni folder koji se na kraju briše. Testovi ne poništavaju transakcije,
 * a svaki pravi svoje trenere i članove, pa ne zavise jedan od drugog.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(MySqlTestKonfiguracija.class)
@Testcontainers(disabledWithoutDocker = true)
class ObjaveIntegracioniTest {

    private static final Path FOLDER_SLIKA;
    static {
        try {
            FOLDER_SLIKA = Files.createTempDirectory("pumpd-slike-it");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void folderZaSlike(DynamicPropertyRegistry registry) {
        registry.add("pumpd.slike.folder", FOLDER_SLIKA::toString);
    }

    @AfterAll
    static void obrisiFolder() throws IOException {
        try (Stream<Path> fajlovi = Files.walk(FOLDER_SLIKA)) {
            fajlovi.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
        }
    }

    private static final AtomicInteger BROJAC = new AtomicInteger();
    /** Početak pravog JPEG fajla; ostatak bajtova je "sadržaj". */
    private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 1, 2, 3 };

    @LocalServerPort
    private int port;
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

    // ------------------------------------------------------------ pomoćne metode

    private record Korisnik(int id, String token) {
    }

    private static String jedinstvenEmail(String prefiks) {
        return prefiks + "." + System.nanoTime() + "." + BROJAC.incrementAndGet() + "@mail.com";
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

    private static <T> T procitaj(ResultActions odgovor, String putanja) throws Exception {
        return JsonPath.read(odgovor.andReturn().getResponse().getContentAsString(), putanja);
    }

    private Korisnik noviNalog(String token, String ruta, String poljeId, String ime) throws Exception {
        String email = jedinstvenEmail(ime);
        ResultActions odgovor = zahtjev(post(ruta), token,
                "{\"name\":\"%s\",\"surname\":\"Test\",\"email\":\"%s\"}".formatted(ime, email))
                .andExpect(status().isCreated());
        int id = procitaj(odgovor, "$." + poljeId);
        return new Korisnik(id, prijava(email, procitaj(odgovor, "$.temporaryPassword")));
    }

    private Korisnik noviClan(String ime) throws Exception {
        return noviNalog(radnik, "/api/worker/members", "memberId", ime);
    }

    private Korisnik noviTrener(String ime) throws Exception {
        return noviNalog(admin, "/api/admin/trainers", "trainerId", ime);
    }

    /** Član šalje zahtjev (7.3), trener ga prihvata (10.2). */
    private void prati(Korisnik clan, Korisnik trener) throws Exception {
        int zahtjev = procitaj(zahtjev(post("/api/trainers/" + trener.id() + "/follow"), clan.token(), null)
                .andExpect(status().isCreated()), "$.followRequestId");
        zahtjev(patch("/api/trainer/follow-requests/" + zahtjev), trener.token(), "{\"status\":\"ACCEPTED\"}")
                .andExpect(status().isOk());
    }

    private String posaljiSliku(Korisnik trener) throws Exception {
        return procitaj(mockMvc.perform(multipart("/api/posts/images")
                        .file(new MockMultipartFile("image", "trening.jpg", "image/jpeg", JPEG))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + trener.token()))
                .andExpect(status().isCreated()), "$.imageUrl");
    }

    private int objavi(Korisnik trener, String sadrzaj, String slika) throws Exception {
        String tijelo = slika == null ? "{\"content\":\"%s\"}".formatted(sadrzaj)
                : "{\"content\":\"%s\",\"imageUrl\":\"%s\"}".formatted(sadrzaj, slika);
        return procitaj(zahtjev(post("/api/posts"), trener.token(), tijelo).andExpect(status().isCreated()), "$.postId");
    }

    private ResultActions feed(Korisnik clan) throws Exception {
        return zahtjev(get("/api/posts/feed"), clan.token(), null).andExpect(status().isOk());
    }

    private ResultActions sacuvane(Korisnik clan) throws Exception {
        return zahtjev(get("/api/posts/saved"), clan.token(), null).andExpect(status().isOk());
    }

    private Path fajlSlike(String imageUrl) {
        return FOLDER_SLIKA.resolve(imageUrl.substring("/images/".length()));
    }

    private int brojSacuvanih(int idObjave) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sacuvana_objava WHERE objava_idobjava = ?", Integer.class, idObjave);
    }

    // ------------------------------------------------------------ cijeli tok

    @Test
    void cijeliTokOdSlikeDoBrisanjaObjave() throws Exception {
        Korisnik trener = noviTrener("Marko");
        Korisnik clan = noviClan("Amar");
        prati(clan, trener);

        // slika: upis na disk i javno čitanje
        String slika = posaljiSliku(trener);
        assertTrue(slika.matches("/images/[0-9a-f-]{36}\\.jpg"), slika);
        assertArrayEquals(JPEG, Files.readAllBytes(fajlSlike(slika)));
        // aplikacija spaja imageUrl sa adresom API-ja (EXPO_PUBLIC_API_URL = http://IP:8080/api)
        mockMvc.perform(get("/api" + slika))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(JPEG));

        // objava i trenerov pregled
        int id = objavi(trener, "Novi trening nogu!", slika);
        zahtjev(get("/api/posts/my-posts"), trener.token(), null)
                .andExpect(jsonPath("$[0].postId").value(id))
                .andExpect(jsonPath("$[0].imageUrl").value(slika))
                .andExpect(jsonPath("$[0].postTimestamp").value(matchesPattern(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")));

        // feed i čuvanje
        feed(clan)
                .andExpect(jsonPath("$[0].postId").value(id))
                .andExpect(jsonPath("$[0].trainerId").value(trener.id()))
                .andExpect(jsonPath("$[0].trainerName").value("Marko Test"))
                .andExpect(jsonPath("$[0].isSaved").value(false));
        zahtjev(post("/api/posts/" + id + "/save"), clan.token(), null).andExpect(status().isOk());
        feed(clan).andExpect(jsonPath("$[0].isSaved").value(true));
        sacuvane(clan)
                .andExpect(jsonPath("$[0].postId").value(id))
                .andExpect(jsonPath("$[0].isSaved").value(true));
        zahtjev(post("/api/posts/" + id + "/save"), clan.token(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Objava je vec sacuvana."));
        assertEquals(1, brojSacuvanih(id));

        // uklanjanje iz sačuvanih
        zahtjev(delete("/api/posts/" + id + "/save"), clan.token(), null).andExpect(status().isOk());
        feed(clan).andExpect(jsonPath("$[0].isSaved").value(false));
        sacuvane(clan).andExpect(jsonPath("$").isEmpty());
        zahtjev(delete("/api/posts/" + id + "/save"), clan.token(), null).andExpect(status().isNotFound());

        // trener briše objavu koju je član ponovo sačuvao: nestaje svuda, a slika sa diska
        zahtjev(post("/api/posts/" + id + "/save"), clan.token(), null).andExpect(status().isOk());
        zahtjev(delete("/api/posts/" + id), trener.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Objava je obrisana."));

        assertEquals(0, brojSacuvanih(id));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM objava WHERE idobjava = ?", Integer.class, id));
        sacuvane(clan).andExpect(jsonPath("$").isEmpty());
        feed(clan).andExpect(jsonPath("$[*].postId", not(hasItem(id))));
        assertFalse(Files.exists(fajlSlike(slika)), "fajl slike je obrisan");
        mockMvc.perform(get("/api" + slika)).andExpect(status().isNotFound());
        zahtjev(post("/api/posts/" + id + "/save"), clan.token(), null).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ feed (8.1)

    @Test
    void feedImaSamoObjavePrihvacenihPracenjaNajnovijePrve() throws Exception {
        Korisnik clan = noviClan("Feed");
        Korisnik pracen = noviTrener("Pracen");
        Korisnik ceka = noviTrener("Ceka");
        Korisnik odbio = noviTrener("Odbio");
        Korisnik nepracen = noviTrener("Nepracen");
        prati(clan, pracen);
        zahtjev(post("/api/trainers/" + ceka.id() + "/follow"), clan.token(), null).andExpect(status().isCreated());
        int odbijen = procitaj(zahtjev(post("/api/trainers/" + odbio.id() + "/follow"), clan.token(), null),
                "$.followRequestId");
        zahtjev(patch("/api/trainer/follow-requests/" + odbijen), odbio.token(), "{\"status\":\"REJECTED\"}")
                .andExpect(status().isOk());

        int starija = objavi(pracen, "Starija", null);
        int novija = objavi(pracen, "Novija", null);
        objavi(ceka, "Od trenera koji ceka", null);
        objavi(odbio, "Od trenera koji je odbio", null);
        objavi(nepracen, "Od nepracenog", null);

        feed(clan)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].postId").value(novija))
                .andExpect(jsonPath("$[1].postId").value(starija))
                .andExpect(jsonPath("$[1].imageUrl").value(nullValue()));
    }

    @Test
    void obrisanTrenerNestajeIzFeedaISacuvanihAliSeMozeUkloniti() throws Exception {
        Korisnik clan = noviClan("Clan");
        Korisnik trener = noviTrener("Brisani");
        prati(clan, trener);
        int id = objavi(trener, "Objava obrisanog", null);
        zahtjev(post("/api/posts/" + id + "/save"), clan.token(), null).andExpect(status().isOk());

        zahtjev(delete("/api/admin/trainers/" + trener.id()), admin, null).andExpect(status().isOk());

        feed(clan).andExpect(jsonPath("$[*].postId", not(hasItem(id))));
        sacuvane(clan).andExpect(jsonPath("$[*].postId", not(hasItem(id))));
        Korisnik drugi = noviClan("Drugi");
        zahtjev(post("/api/posts/" + id + "/save"), drugi.token(), null).andExpect(status().isNotFound());
        // red je u bazi (meko brisanje), a član ga može ukloniti sa stare liste
        assertEquals(1, brojSacuvanih(id));
        zahtjev(delete("/api/posts/" + id + "/save"), clan.token(), null).andExpect(status().isOk());
        assertEquals(0, brojSacuvanih(id));
    }

    // ------------------------------------------------------------ sačuvane (8.2–8.4)

    @Test
    void sacuvanaObjavaOstajeNakonPrekidaPracenja() throws Exception {
        Korisnik clan = noviClan("Prekida");
        Korisnik trener = noviTrener("Prekinuti");
        prati(clan, trener);
        int id = objavi(trener, "Ostaje u sacuvanim", null);
        zahtjev(post("/api/posts/" + id + "/save"), clan.token(), null).andExpect(status().isOk());

        zahtjev(delete("/api/trainers/" + trener.id() + "/follow"), clan.token(), null).andExpect(status().isOk());

        feed(clan).andExpect(jsonPath("$[*].postId", not(hasItem(id))));
        sacuvane(clan).andExpect(jsonPath("$[0].postId").value(id));
    }

    @Test
    void cuvanjeNeTraziPracenje() throws Exception {
        Korisnik clan = noviClan("Bez");
        int id = objavi(noviTrener("Nepracen"), "Sacuvaj me", null);

        zahtjev(post("/api/posts/" + id + "/save"), clan.token(), null).andExpect(status().isOk());
        sacuvane(clan).andExpect(jsonPath("$[0].postId").value(id));
    }

    // ------------------------------------------------------------ trenerova strana (8.5–8.7)

    @Test
    void trenerNeMozeObrisatiTudjuObjavuAClanNijednu() throws Exception {
        Korisnik autor = noviTrener("Autor");
        Korisnik drugi = noviTrener("Drugi");
        int id = objavi(autor, "Moja objava", null);

        zahtjev(delete("/api/posts/" + id), drugi.token(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Objava pripada drugom treneru."));
        zahtjev(delete("/api/posts/" + id), noviClan("Clan").token(), null).andExpect(status().isForbidden());
        zahtjev(get("/api/posts/my-posts"), autor.token(), null).andExpect(jsonPath("$[0].postId").value(id));
        zahtjev(get("/api/posts/my-posts"), drugi.token(), null).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void objavaPrimaSamoNaseSlikeKojePostoje() throws Exception {
        Korisnik trener = noviTrener("Slike");

        zahtjev(post("/api/posts"), trener.token(), "{\"content\":\"x\",\"imageUrl\":\"https://nekisajt.com/a.jpg\"}")
                .andExpect(status().isBadRequest());
        zahtjev(post("/api/posts"), trener.token(),
                "{\"content\":\"x\",\"imageUrl\":\"/images/7f3c9a2e-41b8-4d2a-9c11-5e8a0b6d2f44.jpg\"}")
                .andExpect(status().isBadRequest());
        zahtjev(post("/api/posts"), trener.token(), "{\"content\":\"   \"}").andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM objava WHERE trener_nalog_idnalog = ?",
                Integer.class, trener.id()));
    }

    // ------------------------------------------------------------ slanje slike

    @Test
    void tekstPreimenovanUJpgSeOdbija() throws Exception {
        Korisnik trener = noviTrener("Lazna");
        long fajlovaPrije = brojFajlova();

        mockMvc.perform(multipart("/api/posts/images")
                        .file(new MockMultipartFile("image", "slika.jpg", "image/jpeg", "nije slika".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + trener.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dozvoljene su samo JPEG, PNG i WebP slike."));
        assertEquals(fajlovaPrije, brojFajlova());
    }

    private long brojFajlova() throws IOException {
        try (Stream<Path> fajlovi = Files.list(FOLDER_SLIKA)) {
            return fajlovi.count();
        }
    }

    /** Šalje sliku pravom serveru (port) i vraća "status|tijelo". */
    private String posaljiPravomServeru(String token, int velicina) {
        byte[] bajtovi = Arrays.copyOf(JPEG, velicina);
        LinkedMultiValueMap<String, Object> forma = new LinkedMultiValueMap<>();
        forma.add("image", new ByteArrayResource(bajtovi) {
            @Override
            public String getFilename() {
                return "velika.jpg";
            }
        });
        return RestClient.create("http://localhost:" + port).post().uri("/api/posts/images")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(forma)
                .exchange((req, res) -> res.getStatusCode().value() + "|"
                        + new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void pravaGranicaOdPetMegabajta() throws Exception {
        String token = noviTrener("Velika").token();

        String ispod = posaljiPravomServeru(token, 4 * 1024 * 1024);
        assertTrue(ispod.startsWith("201|"), ispod);

        String iznad = posaljiPravomServeru(token, 5 * 1024 * 1024 + 512 * 1024);
        assertTrue(iznad.startsWith("400|"), iznad);
        assertTrue(iznad.contains("Slika moze imati najvise 5 MB."), iznad);
    }
}
