package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.PratilacDto;
import org.etf.pumpd.dto.ZahtjevZaPracenjeDto;
import org.etf.pumpd.enums.OdlukaOZahtjevu;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.KorisnikService;
import org.etf.pumpd.service.NevazeciTokenService;
import org.etf.pumpd.service.PracenjeService;
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

/** Test ruta 10.1–10.3 zajedno sa pravilima pristupa iz SecurityConfig (bez baze). */
@WebMvcTest(TrenerPracenjeController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class TrenerPracenjeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PracenjeService pracenjeService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private KorisnikService korisnikService;
    @MockitoBean
    private NevazeciTokenService nevazeciTokenService;

    @BeforeEach
    void postaviTokene() {
        for (Uloga uloga : Uloga.values()) {
            int idNalog = idNaloga(uloga);
            when(jwtService.procitaj("token-" + uloga.name()))
                    .thenReturn(Optional.of(new JwtPodaci(idNalog, "jti-" + uloga.name(), Instant.MAX)));
            when(korisnikService.odrediUlogu(idNalog)).thenReturn(Optional.of(uloga));
        }
        when(pracenjeService.zahtjeviZaTrenera(anyInt())).thenReturn(List.of());
        when(pracenjeService.pratioci(anyInt())).thenReturn(List.of());
    }

    private static int idNaloga(Uloga uloga) {
        return uloga.ordinal() + 1;
    }

    private static String bearer(Uloga uloga) {
        return "Bearer token-" + uloga.name();
    }

    private ResultActions obradi(String id, String tijelo, Uloga uloga) throws Exception {
        return mockMvc.perform(patch("/api/trainer/follow-requests/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer(uloga))
                .contentType(MediaType.APPLICATION_JSON)
                .content(tijelo));
    }

    // ------------------------------------------------------------ pristup

    @Test
    void bezTokenaVraca401() throws Exception {
        mockMvc.perform(get("/api/trainer/follow-requests")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/trainer/followers")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "WORKER", "ADMIN"})
    void samoTrenerImaPristup(Uloga uloga) throws Exception {
        mockMvc.perform(get("/api/trainer/follow-requests").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/trainer/followers").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        obradi("15", "{\"status\":\"ACCEPTED\"}", uloga).andExpect(status().isForbidden());

        verify(pracenjeService, never()).obradiZahtjev(anyInt(), any(), anyInt());
    }

    // ------------------------------------------------------------ 10.1

    @Test
    void zahtjeviUFormatuIzUgovoraZaTreneraIzTokena() throws Exception {
        when(pracenjeService.zahtjeviZaTrenera(idNaloga(Uloga.TRAINER))).thenReturn(List.of(
                new ZahtjevZaPracenjeDto(15, 6, "Amar Hodžić", LocalDate.of(2026, 11, 12))));

        mockMvc.perform(get("/api/trainer/follow-requests").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(15))
                .andExpect(jsonPath("$[0].memberId").value(6))
                .andExpect(jsonPath("$[0].memberName").value("Amar Hodžić"))
                .andExpect(jsonPath("$[0].requestDate").value("2026-11-12"));
    }

    @Test
    void bezZahtjevaPrazanNiz() throws Exception {
        mockMvc.perform(get("/api/trainer/follow-requests").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------ 10.2

    @ParameterizedTest
    @EnumSource(OdlukaOZahtjevu.class)
    void obradaPrenosiOdlukuIIdTreneraIzTokena(OdlukaOZahtjevu odluka) throws Exception {
        when(pracenjeService.obradiZahtjev(15, odluka, idNaloga(Uloga.TRAINER))).thenReturn(new PorukaDto("ok"));

        obradi("15", "{\"status\":\"" + odluka.name() + "\"}", Uloga.TRAINER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("ok"));
    }

    @Test
    void nepostojeciZahtjevVraca404() throws Exception {
        when(pracenjeService.obradiZahtjev(anyInt(), any(), anyInt()))
                .thenThrow(new ResursNijePronadjenException("Zahtjev ne postoji."));

        obradi("99", "{\"status\":\"ACCEPTED\"}", Uloga.TRAINER)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Zahtjev ne postoji."));
    }

    @Test
    void vecObradjenZahtjevVraca409() throws Exception {
        when(pracenjeService.obradiZahtjev(anyInt(), any(), anyInt()))
                .thenThrow(new KonfliktException("Zahtjev je vec obradjen."));

        obradi("15", "{\"status\":\"REJECTED\"}", Uloga.TRAINER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Zahtjev je vec obradjen."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"status\":\"PENDING\"}", "{\"status\":\"xyz\"}", "{}", "{\"status\":null}", "",
            "{ovo nije json"})
    void neispravnoTijeloVraca400(String tijelo) throws Exception {
        obradi("15", tijelo, Uloga.TRAINER).andExpect(status().isBadRequest());
        verify(pracenjeService, never()).obradiZahtjev(anyInt(), any(), anyInt());
    }

    @Test
    void idKojiNijeBrojVraca400() throws Exception {
        obradi("abc", "{\"status\":\"ACCEPTED\"}", Uloga.TRAINER).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 10.3

    @Test
    void pratiociUFormatuIzUgovora() throws Exception {
        when(pracenjeService.pratioci(idNaloga(Uloga.TRAINER))).thenReturn(List.of(
                new PratilacDto(6, "Amar", "Hodžić")));

        mockMvc.perform(get("/api/trainer/followers").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberId").value(6))
                .andExpect(jsonPath("$[0].name").value("Amar"))
                .andExpect(jsonPath("$[0].surname").value("Hodžić"));
    }

    @Test
    void bezPratilacaPrazanNiz() throws Exception {
        mockMvc.perform(get("/api/trainer/followers").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
