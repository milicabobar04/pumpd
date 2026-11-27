package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.ZahtjevZaGrupniTreningDto;
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
import org.etf.pumpd.service.ZahtjeviZaGrupeService;
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

/** Test ruta 10.12 i 10.13 zajedno sa pravilima iz SecurityConfig (bez baze). */
@WebMvcTest(ZahtjeviZaGrupeController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class ZahtjeviZaGrupeControllerTest {

    private static final String RUTA = "/api/trainer/group-requests";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZahtjeviZaGrupeService zahtjeviService;
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
        when(zahtjeviService.zahtjevi(anyInt(), any())).thenReturn(List.of());
        when(zahtjeviService.obradi(anyInt(), anyInt(), any())).thenReturn(new PorukaDto("Zahtjev je prihvacen."));
    }

    private static int idNaloga(Uloga uloga) {
        return uloga.ordinal() + 1;
    }

    private ResultActions obradi(Uloga uloga, String id, String tijelo) throws Exception {
        return mockMvc.perform(patch(RUTA + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name())
                .contentType(MediaType.APPLICATION_JSON).content(tijelo));
    }

    @Test
    void bezTokenaVraca401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch(RUTA + "/5").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "WORKER", "ADMIN"})
    void samoTrener(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name()))
                .andExpect(status().isForbidden());
        obradi(uloga, "5", "{\"status\":\"ACCEPTED\"}").andExpect(status().isForbidden());
        verify(zahtjeviService, never()).obradi(anyInt(), anyInt(), any());
    }

    @Test
    void pregledZahtjeva() throws Exception {
        when(zahtjeviService.zahtjevi(idNaloga(Uloga.TRAINER), null)).thenReturn(List.of(new ZahtjevZaGrupniTreningDto(
                900, 40, "Mila Milic", 12, "Joga", LocalDateTime.of(2026, 12, 1, 18, 0), 3, 10, LocalDate.of(2026, 11, 25))));

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer token-TRAINER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(900))
                .andExpect(jsonPath("$[0].memberId").value(40))
                .andExpect(jsonPath("$[0].memberName").value("Mila Milic"))
                .andExpect(jsonPath("$[0].sessionId").value(12))
                .andExpect(jsonPath("$[0].sessionName").value("Joga"))
                .andExpect(jsonPath("$[0].startTime").value("2026-12-01T18:00:00"))
                .andExpect(jsonPath("$[0].enrolledCount").value(3))
                .andExpect(jsonPath("$[0].maxMembers").value(10))
                .andExpect(jsonPath("$[0].requestDate").value("2026-11-25"));
        verify(zahtjeviService).zahtjevi(idNaloga(Uloga.TRAINER), null);
    }

    @Test
    void filterPoTerminu() throws Exception {
        mockMvc.perform(get(RUTA + "?sessionId=12").header(HttpHeaders.AUTHORIZATION, "Bearer token-TRAINER"))
                .andExpect(status().isOk());
        verify(zahtjeviService).zahtjevi(idNaloga(Uloga.TRAINER), 12);
        mockMvc.perform(get(RUTA + "?sessionId=abc").header(HttpHeaders.AUTHORIZATION, "Bearer token-TRAINER"))
                .andExpect(status().isBadRequest());
        verify(zahtjeviService, times(1)).zahtjevi(anyInt(), any());   // neispravan sessionId ne stiže do servisa
    }

    @ParameterizedTest
    @EnumSource(OdlukaOZahtjevu.class)
    void obradaProsljedjujeOdluku(OdlukaOZahtjevu odluka) throws Exception {
        obradi(Uloga.TRAINER, "900", "{\"status\":\"" + odluka + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Zahtjev je prihvacen."));
        verify(zahtjeviService).obradi(idNaloga(Uloga.TRAINER), 900, odluka);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":\"PENDING\"}", "{\"status\":\"abc\"}",
            "{\"status\":\"accepted\"}", "", "nije json"})
    void neispravnoTijeloVraca400(String tijelo) throws Exception {
        obradi(Uloga.TRAINER, "900", tijelo).andExpect(status().isBadRequest());
        verify(zahtjeviService, never()).obradi(anyInt(), anyInt(), any());
    }

    @Test
    void neispravanIdVraca400() throws Exception {
        obradi(Uloga.TRAINER, "abc", "{\"status\":\"ACCEPTED\"}").andExpect(status().isBadRequest());
    }

    @Test
    void greskeServisa() throws Exception {
        when(zahtjeviService.obradi(idNaloga(Uloga.TRAINER), 1, OdlukaOZahtjevu.ACCEPTED))
                .thenThrow(new ResursNijePronadjenException("Zahtjev ne postoji."));
        when(zahtjeviService.obradi(idNaloga(Uloga.TRAINER), 2, OdlukaOZahtjevu.ACCEPTED))
                .thenThrow(new KonfliktException("Grupa je popunjena."));

        obradi(Uloga.TRAINER, "1", "{\"status\":\"ACCEPTED\"}").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Zahtjev ne postoji."));
        obradi(Uloga.TRAINER, "2", "{\"status\":\"ACCEPTED\"}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Grupa je popunjena."));
    }
}
