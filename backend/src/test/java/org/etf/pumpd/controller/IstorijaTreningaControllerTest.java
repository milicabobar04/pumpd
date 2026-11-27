package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.TreningIzIstorijeDto;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.enums.VrstaTreninga;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.IstorijaTreningaService;
import org.etf.pumpd.service.KorisnikService;
import org.etf.pumpd.service.NevazeciTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Test rute 2.4 zajedno sa pravilima iz SecurityConfig (bez baze). */
@WebMvcTest(IstorijaTreningaController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class IstorijaTreningaControllerTest {

    private static final String RUTA = "/api/users/training-history";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IstorijaTreningaService istorijaService;
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
        when(istorijaService.istorija(anyInt())).thenReturn(List.of());
    }

    @Test
    void clanDobijaIstoriju() throws Exception {
        int idClana = Uloga.MEMBER.ordinal() + 1;
        when(istorijaService.istorija(idClana)).thenReturn(List.of(
                new TreningIzIstorijeDto(7, "Joga", LocalDate.of(2026, 11, 24), VrstaTreninga.GROUP),
                new TreningIzIstorijeDto(50, "Samostalni trening", LocalDate.of(2026, 11, 22), VrstaTreninga.SOLO)));

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer token-MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].trainingId").value(7))
                .andExpect(jsonPath("$[0].name").value("Joga"))
                .andExpect(jsonPath("$[0].date").value("2026-11-24"))
                .andExpect(jsonPath("$[0].type").value("GROUP"))
                .andExpect(jsonPath("$[1].type").value("SOLO"));
        verify(istorijaService).istorija(idClana);
    }

    @Test
    void bezTokenaVraca401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"TRAINER", "WORKER", "ADMIN"})
    void samoClan(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name()))
                .andExpect(status().isForbidden());
        verify(istorijaService, never()).istorija(anyInt());
    }

    @Test
    void ostaleMetodeNisuDozvoljene() throws Exception {
        mockMvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer token-MEMBER"))
                .andExpect(status().isMethodNotAllowed());
    }
}
