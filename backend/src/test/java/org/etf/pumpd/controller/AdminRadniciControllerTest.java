package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.DodavanjeRadnikaOdgovor;
import org.etf.pumpd.dto.DodavanjeRadnikaZahtjev;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.RadnikDto;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.service.AdminRadniciService;
import org.etf.pumpd.service.KorisnikService;
import org.etf.pumpd.service.NevazeciTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Test ruta 12.5–12.7 zajedno sa pravilima pristupa iz SecurityConfig (bez baze). */
@WebMvcTest(AdminRadniciController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class})
class AdminRadniciControllerTest {

    private static final String RUTA = "/api/admin/workers";

    private static final String ISPRAVAN_RADNIK = """
            {"name":"Haris","surname":"Zukić","email":"haris@mail.com","password":"lozinka123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminRadniciService adminRadniciService;
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
        when(adminRadniciService.listaRadnika()).thenReturn(List.of());
    }

    private static String bearer(Uloga uloga) {
        return "Bearer token-" + uloga.name();
    }

    private ResultActions posalji(String tijelo, Uloga uloga) throws Exception {
        return mockMvc.perform(post(RUTA)
                .header(HttpHeaders.AUTHORIZATION, bearer(uloga))
                .contentType(MediaType.APPLICATION_JSON)
                .content(tijelo));
    }

    // ------------------------------------------------------------ pristup po ulogama

    @Test
    void bezTokenaVraca401() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER", "WORKER"})
    void samoAdministratorVidiListu(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(adminRadniciService, never()).listaRadnika();
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER", "WORKER"})
    void samoAdministratorVidiDetalje(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA + "/2").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());

        verify(adminRadniciService, never()).detaljiRadnika(anyInt());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER", "WORKER"})
    void samoAdministratorDodajeRadnika(Uloga uloga) throws Exception {
        posalji(ISPRAVAN_RADNIK, uloga)
                .andExpect(status().isForbidden());

        verify(adminRadniciService, never()).dodajRadnika(any());
    }

    // ------------------------------------------------------------ 12.5 lista

    @Test
    void listaUFormatuIzUgovora() throws Exception {
        when(adminRadniciService.listaRadnika()).thenReturn(List.of(
                new RadnikDto(2, "Emina", "Kurtović", "radnik@pumpd.local")));

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workerId").value(2))
                .andExpect(jsonPath("$[0].name").value("Emina"))
                .andExpect(jsonPath("$[0].surname").value("Kurtović"))
                .andExpect(jsonPath("$[0].email").value("radnik@pumpd.local"));
    }

    @Test
    void bezRadnikaVracaPrazanNiz() throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------ 12.6 detalji

    @Test
    void detaljiUFormatuIzUgovora() throws Exception {
        when(adminRadniciService.detaljiRadnika(2)).thenReturn(
                new RadnikDto(2, "Emina", "Kurtović", "radnik@pumpd.local"));

        mockMvc.perform(get(RUTA + "/2").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerId").value(2))
                .andExpect(jsonPath("$.name").value("Emina"))
                .andExpect(jsonPath("$.surname").value("Kurtović"))
                .andExpect(jsonPath("$.email").value("radnik@pumpd.local"));
    }

    @Test
    void nepostojeciRadnikVraca404UFormatuGreske() throws Exception {
        when(adminRadniciService.detaljiRadnika(anyInt()))
                .thenThrow(new ResursNijePronadjenException("Radnik ne postoji."));

        mockMvc.perform(get(RUTA + "/99").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Radnik ne postoji."));
    }

    @Test
    void idKojiNijeBrojVraca400() throws Exception {
        mockMvc.perform(get(RUTA + "/abc").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 12.7 dodavanje

    @Test
    void dodavanjeVraca201SaIdomIPorukom() throws Exception {
        when(adminRadniciService.dodajRadnika(any()))
                .thenReturn(new DodavanjeRadnikaOdgovor(11, "Radnik je uspjesno dodan."));

        posalji(ISPRAVAN_RADNIK, Uloga.ADMIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workerId").value(11))
                .andExpect(jsonPath("$.message").value("Radnik je uspjesno dodan."));

        verify(adminRadniciService).dodajRadnika(
                new DodavanjeRadnikaZahtjev("Haris", "Zukić", "haris@mail.com", "lozinka123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"surname\":\"Zukić\",\"email\":\"haris@mail.com\",\"password\":\"lozinka123\"}",
            "{\"name\":\"Haris\",\"surname\":\" \",\"email\":\"haris@mail.com\",\"password\":\"lozinka123\"}",
            "{\"name\":\"Haris\",\"surname\":\"Zukić\",\"email\":\"nije-email\",\"password\":\"lozinka123\"}",
            "{\"name\":\"Haris\",\"surname\":\"Zukić\",\"email\":\"haris@mail.com\",\"password\":\"kratka\"}",
            "{\"name\":\"Haris\",\"surname\":\"Zukić\",\"email\":\"haris@mail.com\"}"
    })
    void nevalidniPodaciVracaju400(String tijelo) throws Exception {
        posalji(tijelo, Uloga.ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(adminRadniciService, never()).dodajRadnika(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{ovo nije json"})
    void neispravnoTijeloVraca400(String tijelo) throws Exception {
        posalji(tijelo, Uloga.ADMIN)
                .andExpect(status().isBadRequest());
    }

    @Test
    void zauzetEmailVraca409UFormatuGreske() throws Exception {
        when(adminRadniciService.dodajRadnika(any())).thenThrow(new KonfliktException("Email vec postoji."));

        posalji(ISPRAVAN_RADNIK, Uloga.ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email vec postoji."));
    }

    // ------------------------------------------------------------ uklanjanje (meko brisanje)

    @Test
    void uklanjanjeVraca200SaPorukom() throws Exception {
        when(adminRadniciService.ukloniRadnika(2)).thenReturn(new PorukaDto("Radnik je uspjesno uklonjen."));

        mockMvc.perform(delete(RUTA + "/2").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Radnik je uspjesno uklonjen."));
    }

    @Test
    void uklanjanjeNepostojecegVraca404() throws Exception {
        when(adminRadniciService.ukloniRadnika(anyInt())).thenThrow(new ResursNijePronadjenException("Radnik ne postoji."));

        mockMvc.perform(delete(RUTA + "/99").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Radnik ne postoji."));
    }

    @Test
    void uklanjanjeSaIdomKojiNijeBrojVraca400() throws Exception {
        mockMvc.perform(delete(RUTA + "/abc").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER", "WORKER"})
    void uklanjanjeBezPravaVraca403(Uloga uloga) throws Exception {
        mockMvc.perform(delete(RUTA + "/2").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());

        verify(adminRadniciService, never()).ukloniRadnika(anyInt());
    }

    @Test
    void uklanjanjeBezTokenaVraca401() throws Exception {
        mockMvc.perform(delete(RUTA + "/2"))
                .andExpect(status().isUnauthorized());
    }
}
