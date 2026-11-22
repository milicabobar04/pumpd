package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.service.KorisnikService;
import org.etf.pumpd.service.NevazeciTokenService;
import org.etf.pumpd.service.SlikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/** Test slanja (POST /api/posts/images) i čitanja (GET /api/images/{ime}) slike, sa pravilima iz SecurityConfig. */
@WebMvcTest(SlikeController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class})
class SlikeControllerTest {

    private static final String IME = "7f3c9a2e-41b8-4d2a-9c11-5e8a0b6d2f44.jpg";
    private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3 };

    @TempDir
    Path folder;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SlikeService slikeService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private KorisnikService korisnikService;
    @MockitoBean
    private NevazeciTokenService nevazeciTokenService;

    @BeforeEach
    void postaviTokene() {
        for (Uloga uloga : Uloga.values()) {
            int idNalog = uloga.ordinal() + 1;
            when(jwtService.procitaj("token-" + uloga.name()))
                    .thenReturn(Optional.of(new JwtPodaci(idNalog, "jti-" + uloga.name(), Instant.MAX)));
            when(korisnikService.odrediUlogu(idNalog)).thenReturn(Optional.of(uloga));
        }
        when(slikeService.sacuvaj(any())).thenReturn("/api/images/" + IME);
    }

    private ResultActions posalji(MockMultipartFile fajl, Uloga uloga) throws Exception {
        var zahtjev = multipart("/api/posts/images").file(fajl);
        if (uloga != null) {
            zahtjev.header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name());
        }
        return mockMvc.perform(zahtjev);
    }

    private static MockMultipartFile slika() {
        return new MockMultipartFile("image", "trening.jpg", "image/jpeg", JPEG);
    }

    // ------------------------------------------------------------ slanje: pristup

    @Test
    void slanjeBezTokenaVraca401() throws Exception {
        posalji(slika(), null).andExpect(status().isUnauthorized());
        verify(slikeService, never()).sacuvaj(any());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "WORKER", "ADMIN"})
    void samoTrenerSaljeSliku(Uloga uloga) throws Exception {
        posalji(slika(), uloga).andExpect(status().isForbidden());
        verify(slikeService, never()).sacuvaj(any());
    }

    // ------------------------------------------------------------ slanje: odgovor i greške

    @Test
    void slanjeVraca201SaAdresomSlike() throws Exception {
        posalji(slika(), Uloga.TRAINER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value("/api/images/" + IME));

        verify(slikeService).sacuvaj(any());
    }

    @Test
    void poljeSeMoraZvatiImage() throws Exception {
        posalji(new MockMultipartFile("photo", "trening.jpg", "image/jpeg", JPEG), Uloga.TRAINER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Slika je obavezna (polje image)."));
        verify(slikeService, never()).sacuvaj(any());
    }

    @Test
    void zahtjevKojiNijeMultipartVraca400() throws Exception {
        mockMvc.perform(post("/api/posts/images").header(HttpHeaders.AUTHORIZATION, "Bearer token-TRAINER")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"image\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void pogresanFormatVraca400IzServisa() throws Exception {
        when(slikeService.sacuvaj(any())).thenThrow(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dozvoljene su samo JPEG, PNG i WebP slike."));

        posalji(slika(), Uloga.TRAINER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dozvoljene su samo JPEG, PNG i WebP slike."));
    }

    @Test
    void prevelikaSlikaVraca400ANe413() throws Exception {
        // MockMvc ne primjenjuje ograničenje veličine; ovdje se provjerava obrada greške koju Spring baca
        when(slikeService.sacuvaj(any())).thenThrow(new MaxUploadSizeExceededException(5 * 1024 * 1024));

        posalji(slika(), Uloga.TRAINER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Slika moze imati najvise 5 MB."));
    }

    // ------------------------------------------------------------ čitanje

    private Path napraviSliku() throws IOException {
        Path fajl = Files.write(folder.resolve(IME), JPEG);
        when(slikeService.putanjaPoImenu(IME)).thenReturn(Optional.of(fajl));
        when(slikeService.tip(fajl)).thenReturn(MediaType.IMAGE_JPEG);
        return fajl;
    }

    @Test
    void citanjeJeJavnoIVracaBajtoveSaTipomIKesiranjem() throws Exception {
        napraviSliku();

        mockMvc.perform(get("/api/images/" + IME))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(JPEG))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=31536000, public"));
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void citanjeRadiISaTokenomBiloKojeUloge(Uloga uloga) throws Exception {
        napraviSliku();

        mockMvc.perform(get("/api/images/" + IME).header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name()))
                .andExpect(status().isOk());
    }

    @Test
    void nepostojecaSlikaVraca404() throws Exception {
        when(slikeService.putanjaPoImenu(IME)).thenReturn(Optional.of(folder.resolve(IME)));

        mockMvc.perform(get("/api/images/" + IME))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Slika ne postoji."));
    }

    @Test
    void neispravnoImeVraca404() throws Exception {
        when(slikeService.putanjaPoImenu(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/images/application.properties")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/images/slika.jpg")).andExpect(status().isNotFound());
    }

    @Test
    void slanjeNaAdresuZaCitanjeNijeJavno() throws Exception {
        // permitAll važi samo za GET; POST na /api/images/... traži prijavu
        mockMvc.perform(post("/api/images/" + IME)).andExpect(status().isUnauthorized());
    }
}
