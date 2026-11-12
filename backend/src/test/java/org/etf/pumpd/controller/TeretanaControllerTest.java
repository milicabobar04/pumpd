package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.InfoTeretanaDto;
import org.etf.pumpd.dto.IzmjenaTeretaneZahtjev;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.service.KorisnikService;
import org.etf.pumpd.service.NevazeciTokenService;
import org.etf.pumpd.service.TeretanaService;
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

/** Test rute 5.1 (javna) i 5.2 (samo ADMIN) zajedno sa pravilima iz SecurityConfig (bez baze). */
@WebMvcTest(TeretanaController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class})
class TeretanaControllerTest {

    private static final String RUTA = "/api/gym/info";

    private static final String ISPRAVNA_IZMJENA = """
            {"name":"Pumpd Gym","description":"Moderna teretana.","membershipPrice":45.5,
             "dateOfEstablishment":"2019-03-15","workingHours":"Pon-Pet 06-22"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeretanaService teretanaService;
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
        when(teretanaService.izmijeni(any())).thenReturn(new PorukaDto("Informacije o teretani su azurirane."));
    }

    private ResultActions izmijeni(String tijelo, Uloga uloga) throws Exception {
        var zahtjev = put(RUTA).contentType(MediaType.APPLICATION_JSON).content(tijelo);
        if (uloga != null) {
            zahtjev.header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name());
        }
        return mockMvc.perform(zahtjev);
    }

    // ------------------------------------------------------------ 5.1 ostaje javna

    @Test
    void pregledInformacijaRadiBezTokena() throws Exception {
        when(teretanaService.informacije()).thenReturn(new InfoTeretanaDto(
                "Pumpd Gym", "Opis", 40.0, LocalDate.of(2019, 3, 15), "Pon-Pet 06-22"));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pumpd Gym"));
    }

    // ------------------------------------------------------------ 5.2 pristup

    @Test
    void izmjenaBezTokenaVraca401() throws Exception {
        izmijeni(ISPRAVNA_IZMJENA, null).andExpect(status().isUnauthorized());
        verify(teretanaService, never()).izmijeni(any());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER", "WORKER"})
    void izmjenaSamoZaAdministratora(Uloga uloga) throws Exception {
        izmijeni(ISPRAVNA_IZMJENA, uloga)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(teretanaService, never()).izmijeni(any());
    }

    // ------------------------------------------------------------ 5.2 odgovor i validacija

    @Test
    void administratorMijenjaInformacije() throws Exception {
        izmijeni(ISPRAVNA_IZMJENA, Uloga.ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Informacije o teretani su azurirane."));

        verify(teretanaService).izmijeni(new IzmjenaTeretaneZahtjev(
                "Pumpd Gym", "Moderna teretana.", 45.5, LocalDate.of(2019, 3, 15), "Pon-Pet 06-22"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"description\":\"D\",\"membershipPrice\":45.5,\"dateOfEstablishment\":\"2019-03-15\",\"workingHours\":\"W\"}",
            "{\"name\":\" \",\"description\":\"D\",\"membershipPrice\":45.5,\"dateOfEstablishment\":\"2019-03-15\",\"workingHours\":\"W\"}",
            "{\"name\":\"N\",\"membershipPrice\":45.5,\"dateOfEstablishment\":\"2019-03-15\",\"workingHours\":\"W\"}",
            "{\"name\":\"N\",\"description\":\"D\",\"dateOfEstablishment\":\"2019-03-15\",\"workingHours\":\"W\"}",
            "{\"name\":\"N\",\"description\":\"D\",\"membershipPrice\":0,\"dateOfEstablishment\":\"2019-03-15\",\"workingHours\":\"W\"}",
            "{\"name\":\"N\",\"description\":\"D\",\"membershipPrice\":-10,\"dateOfEstablishment\":\"2019-03-15\",\"workingHours\":\"W\"}",
            "{\"name\":\"N\",\"description\":\"D\",\"membershipPrice\":45.5,\"workingHours\":\"W\"}",
            "{\"name\":\"N\",\"description\":\"D\",\"membershipPrice\":45.5,\"dateOfEstablishment\":\"2019-13-45\",\"workingHours\":\"W\"}",
            "{\"name\":\"N\",\"description\":\"D\",\"membershipPrice\":45.5,\"dateOfEstablishment\":\"2019-03-15\"}"
    })
    void nevalidniPodaciVracaju400(String tijelo) throws Exception {
        izmijeni(tijelo, Uloga.ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verify(teretanaService, never()).izmijeni(any());
    }

    @Test
    void predugNazivVraca400SaPorukom() throws Exception {
        izmijeni("""
                {"name":"%s","description":"D","membershipPrice":45.5,"dateOfEstablishment":"2019-03-15","workingHours":"W"}
                """.formatted("a".repeat(46)), Uloga.ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name: Naziv moze imati najvise 45 znakova."));
    }
}
