package org.etf.pumpd.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.RecenzijaDto;
import org.etf.pumpd.dto.RecenzijeTreneraDto;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.KorisnikService;
import org.etf.pumpd.service.NevazeciTokenService;
import org.etf.pumpd.service.RecenzijeService;
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

/** Test rute 7.6 zajedno sa pravilima pristupa iz SecurityConfig (bez baze). */
@WebMvcTest(TrenerRecenzijeController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class TrenerRecenzijeControllerTest {

    private static final String RUTA = "/api/trainer/reviews";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecenzijeService recenzijeService;
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
    }

    private static int idNaloga(Uloga uloga) {
        return uloga.ordinal() + 1;
    }

    private static String bearer(Uloga uloga) {
        return "Bearer token-" + uloga.name();
    }

    @Test
    void bezTokenaVraca401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "WORKER", "ADMIN"})
    void samoTrenerVidiSvojeRecenzije(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        verify(recenzijeService, never()).recenzijeTrenera(anyInt());
    }

    @Test
    void recenzijeUFormatuIzUgovoraZaTreneraIzTokena() throws Exception {
        when(recenzijeService.recenzijeTrenera(idNaloga(Uloga.TRAINER))).thenReturn(new RecenzijeTreneraDto(4.5,
                List.of(new RecenzijaDto(21, "Amar Hodžić", 5, "Odličan.", LocalDate.of(2026, 11, 12)))));

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageGrade").value(4.5))
                .andExpect(jsonPath("$.reviews[0].reviewId").value(21))
                .andExpect(jsonPath("$.reviews[0].memberName").value("Amar Hodžić"))
                .andExpect(jsonPath("$.reviews[0].grade").value(5))
                .andExpect(jsonPath("$.reviews[0].comment").value("Odličan."))
                .andExpect(jsonPath("$.reviews[0].date").value("2026-11-12"));
    }

    @Test
    void bezRecenzijaNullIPrazanNiz() throws Exception {
        when(recenzijeService.recenzijeTrenera(idNaloga(Uloga.TRAINER)))
                .thenReturn(new RecenzijeTreneraDto(null, List.of()));

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageGrade").value(nullValue()))
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews").isEmpty());
    }
}
