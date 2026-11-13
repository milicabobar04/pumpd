package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.PromjenaLozinkeZahtjev;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.KorisnikService;
import org.etf.pumpd.service.LozinkaService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

/** Test rute PUT /api/users/my-password zajedno sa pravilima iz SecurityConfig (bez baze). */
@WebMvcTest(LozinkaController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class LozinkaControllerTest {

    private static final String RUTA = "/api/users/my-password";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LozinkaService lozinkaService;
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
        when(lozinkaService.promijeniLozinku(anyInt(), any())).thenReturn(new PorukaDto("Lozinka je promijenjena."));
    }

    private static int idNaloga(Uloga uloga) {
        return uloga.ordinal() + 1;
    }

    private ResultActions promijeni(String tijelo, Uloga uloga) throws Exception {
        var zahtjev = put(RUTA).contentType(MediaType.APPLICATION_JSON).content(tijelo);
        if (uloga != null) {
            zahtjev.header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name());
        }
        return mockMvc.perform(zahtjev);
    }

    @Test
    void bezTokenaVraca401() throws Exception {
        promijeni("{\"oldPassword\":\"Privremena7k\",\"newPassword\":\"mojaNovaLozinka\"}", null)
                .andExpect(status().isUnauthorized());
        verify(lozinkaService, never()).promijeniLozinku(anyInt(), any());
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void svakaUlogaMijenjaSvojuLozinku(Uloga uloga) throws Exception {
        promijeni("{\"oldPassword\":\"Privremena7k\",\"newPassword\":\"mojaNovaLozinka\"}", uloga)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lozinka je promijenjena."));

        verify(lozinkaService).promijeniLozinku(idNaloga(uloga),
                new PromjenaLozinkeZahtjev("Privremena7k", "mojaNovaLozinka"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"newPassword\":\"mojaNovaLozinka\"}",
            "{\"oldPassword\":\"\",\"newPassword\":\"mojaNovaLozinka\"}",
            "{\"oldPassword\":\"Privremena7k\"}",
            "{\"oldPassword\":\"Privremena7k\",\"newPassword\":\"kratka\"}",
            "", "{ovo nije json"})
    void neispravnoTijeloVraca400(String tijelo) throws Exception {
        promijeni(tijelo, Uloga.MEMBER).andExpect(status().isBadRequest());
        verify(lozinkaService, never()).promijeniLozinku(anyInt(), any());
    }

    @Test
    void predugaNovaLozinkaVraca400SaPorukom() throws Exception {
        promijeni("{\"oldPassword\":\"Privremena7k\",\"newPassword\":\"" + "a".repeat(73) + "\"}", Uloga.MEMBER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("newPassword: Nova lozinka mora imati izmedju 8 i 72 znaka."));
    }

    @Test
    void pogresnaStaraLozinkaVraca400ANe401() throws Exception {
        when(lozinkaService.promijeniLozinku(anyInt(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stara lozinka nije ispravna."));

        promijeni("{\"oldPassword\":\"pogresna\",\"newPassword\":\"mojaNovaLozinka\"}", Uloga.MEMBER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Stara lozinka nije ispravna."));
    }
}
