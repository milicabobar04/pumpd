package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
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
import org.etf.pumpd.dto.ProblemDto;
import org.etf.pumpd.dto.PromjenaStatusaProblemaOdgovor;
import org.etf.pumpd.enums.StatusProblema;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.KorisnikService;
import org.etf.pumpd.service.NevazeciTokenService;
import org.etf.pumpd.service.ProblemService;
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

/** Test ruta 4.2 i 4.3 zajedno sa pravilima pristupa iz SecurityConfig (bez baze). */
@WebMvcTest(AdminProblemiController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class AdminProblemiControllerTest {

    private static final String RUTA = "/api/admin/issues";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProblemService problemService;
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
        when(problemService.listaProblema(any())).thenReturn(List.of());
    }

    private static String bearer(Uloga uloga) {
        return "Bearer token-" + uloga.name();
    }

    /** id naloga za token uloge (vidi postaviTokene). */
    private static int idNaloga(Uloga uloga) {
        return uloga.ordinal() + 1;
    }

    private ResultActions promijeni(String id, String tijelo, Uloga uloga) throws Exception {
        return mockMvc.perform(patch(RUTA + "/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer(uloga))
                .contentType(MediaType.APPLICATION_JSON)
                .content(tijelo));
    }

    // ------------------------------------------------------------ pristup

    @Test
    void bezTokenaVraca401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER", "WORKER"})
    void samoAdministratorVidiProbleme(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        verify(problemService, never()).listaProblema(any());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER", "WORKER"})
    void samoAdministratorMijenjaStatus(Uloga uloga) throws Exception {
        promijeni("1", "{\"status\":\"PROCESSED\"}", uloga).andExpect(status().isForbidden());
        verify(problemService, never()).promijeniStatus(anyInt(), any(), anyInt());
    }

    // ------------------------------------------------------------ 4.2

    @Test
    void listaUFormatuIzUgovora() throws Exception {
        when(problemService.listaProblema(isNull())).thenReturn(List.of(new ProblemDto(
                2, "Nema tople vode", "U svlacionici nema tople vode.", StatusProblema.IN_PROGRESS,
                LocalDate.of(2026, 11, 11))));

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].issueId").value(2))
                .andExpect(jsonPath("$[0].title").value("Nema tople vode"))
                .andExpect(jsonPath("$[0].description").value("U svlacionici nema tople vode."))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].reportDate").value("2026-11-11"));
    }

    @Test
    void bezProblemaVracaPrazanNiz() throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @ParameterizedTest
    @EnumSource(StatusProblema.class)
    void filterPoStatusuStizeDoServisa(StatusProblema status) throws Exception {
        mockMvc.perform(get(RUTA).param("status", status.name())
                        .header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk());

        verify(problemService).listaProblema(status);
    }

    @Test
    void nepoznatStatusUFilteruVraca400() throws Exception {
        mockMvc.perform(get(RUTA).param("status", "xyz").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 4.3

    @Test
    void promjenaStatusaVraca200IPrenosiIdAdministratora() throws Exception {
        when(problemService.promijeniStatus(1, StatusProblema.PROCESSED, idNaloga(Uloga.ADMIN)))
                .thenReturn(new PromjenaStatusaProblemaOdgovor(1, StatusProblema.PROCESSED,
                        "Status problema je promijenjen."));

        promijeni("1", "{\"status\":\"PROCESSED\"}", Uloga.ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueId").value(1))
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.message").value("Status problema je promijenjen."));
    }

    @Test
    void nepostojeciProblemVraca404() throws Exception {
        when(problemService.promijeniStatus(anyInt(), any(), anyInt()))
                .thenThrow(new ResursNijePronadjenException("Problem ne postoji."));

        promijeni("99", "{\"status\":\"PROCESSED\"}", Uloga.ADMIN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Problem ne postoji."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":\"ZAVRSEN\"}", "", "{ovo nije json"})
    void neispravnoTijeloVraca400(String tijelo) throws Exception {
        promijeni("1", tijelo, Uloga.ADMIN).andExpect(status().isBadRequest());
        verify(problemService, never()).promijeniStatus(anyInt(), any(), anyInt());
    }

    @Test
    void idKojiNijeBrojVraca400() throws Exception {
        promijeni("abc", "{\"status\":\"PROCESSED\"}", Uloga.ADMIN).andExpect(status().isBadRequest());
    }
}
