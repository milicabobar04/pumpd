package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
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
import org.etf.pumpd.service.ObavjestenjeService;
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

/** Test označavanja obavještenja kao pročitanih, sa pravilima iz SecurityConfig (bez baze). */
@WebMvcTest(ObavjestenjeController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class ObavjestenjeControllerTest {

    private static final String RUTA = "/api/notifications";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObavjestenjeService obavjestenjeService;
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
        mockMvc.perform(patch(RUTA + "/read-all")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(RUTA + "/7/read")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void svakaUlogaOznacavaSvaSvojaObavjestenja(Uloga uloga) throws Exception {
        mockMvc.perform(patch(RUTA + "/read-all").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Obavjestenja su oznacena kao procitana."));

        verify(obavjestenjeService).oznaciSveKaoProcitana(idNaloga(uloga));
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void svakaUlogaOznacavaJednoSvojeObavjestenje(Uloga uloga) throws Exception {
        mockMvc.perform(patch(RUTA + "/7/read").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Obavjestenje je oznaceno kao procitano."));

        verify(obavjestenjeService).oznaciKaoProcitano(7, idNaloga(uloga));
    }

    @Test
    void tudjeIliNepostojeceObavjestenjeVraca404() throws Exception {
        doThrow(new ResursNijePronadjenException("Obavjestenje ne postoji."))
                .when(obavjestenjeService).oznaciKaoProcitano(anyInt(), anyInt());

        mockMvc.perform(patch(RUTA + "/99/read").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Obavjestenje ne postoji."));
    }

    @Test
    void idKojiNijeBrojVraca400() throws Exception {
        mockMvc.perform(patch(RUTA + "/abc/read").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isBadRequest());
        verify(obavjestenjeService, never()).oznaciKaoProcitano(anyInt(), anyInt());
    }
}
