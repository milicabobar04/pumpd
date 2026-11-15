package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.IzlazakOdgovor;
import org.etf.pumpd.dto.UlazakOdgovor;
import org.etf.pumpd.dto.ZauzetostDto;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.service.DolazakService;
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

/**
 * Rute 11.8, 11.9 i 5.3 zajedno sa pravilima pristupa iz SecurityConfig.
 * Servis je mockovan, token "token-<ULOGA>" predstavlja prijavljenog korisnika te uloge.
 */
@WebMvcTest({DolazakController.class, ZauzetostController.class})
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class})
class DolazakControllerTest {

    private static final String ULAZAK = "/api/worker/attendance/entry";
    private static final String IZLAZAK = "/api/worker/attendance/exit";
    private static final String ZAUZETOST = "/api/gym/occupancy";
    private static final String TIJELO = "{\"qrCode\":\"SEED-QR-CLAN1\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DolazakService dolazakService;
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
        when(dolazakService.ulazak(anyString()))
                .thenReturn(new UlazakOdgovor(6, "Amar Hodzic", true, "Dolazak je evidentiran."));
        when(dolazakService.izlazak(anyString()))
                .thenReturn(new IzlazakOdgovor(6, "Amar Hodzic", LocalDateTime.of(2026, 10, 29, 18, 30, 5),
                        "Izlazak je evidentiran."));
        when(dolazakService.zauzetost()).thenReturn(new ZauzetostDto(12, 80));
    }

    private ResultActions posalji(String ruta, Uloga uloga, String tijelo) throws Exception {
        var zahtjev = post(ruta).contentType(MediaType.APPLICATION_JSON).content(tijelo);
        if (uloga != null) {
            zahtjev.header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name());
        }
        return mockMvc.perform(zahtjev);
    }

    // ------------------------------------------------------------ pristup

    @Test
    void evidencijaBezTokenaVraca401() throws Exception {
        posalji(ULAZAK, null, TIJELO).andExpect(status().isUnauthorized());
        posalji(IZLAZAK, null, TIJELO).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER", "ADMIN"})
    void evidencijaSamoZaRadnika(Uloga uloga) throws Exception {
        posalji(ULAZAK, uloga, TIJELO).andExpect(status().isForbidden());
        posalji(IZLAZAK, uloga, TIJELO).andExpect(status().isForbidden());
        verify(dolazakService, never()).ulazak(anyString());
    }

    @Test
    void zauzetostBezTokenaVraca401() throws Exception {
        mockMvc.perform(get(ZAUZETOST)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void zauzetostVidiSvakiPrijavljeniKorisnik(Uloga uloga) throws Exception {
        mockMvc.perform(get(ZAUZETOST).header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMembers").value(12))
                .andExpect(jsonPath("$.capacity").value(80));
    }

    // ------------------------------------------------------------ 11.8 i 11.9 odgovori

    @Test
    void ulazakVracaFormatIzUgovora() throws Exception {
        posalji(ULAZAK, Uloga.WORKER, TIJELO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(6))
                .andExpect(jsonPath("$.memberName").value("Amar Hodzic"))
                .andExpect(jsonPath("$.isAllowed").value(true))
                .andExpect(jsonPath("$.message").value("Dolazak je evidentiran."));
    }

    @Test
    void izlazakVracaVrijemeIzlaska() throws Exception {
        posalji(IZLAZAK, Uloga.WORKER, TIJELO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(6))
                .andExpect(jsonPath("$.exitTime").value("2026-10-29T18:30:05"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"qrCode\":\"\"}", "{\"qrCode\":\"   \"}"})
    void bezQrKodaVraca400(String tijelo) throws Exception {
        posalji(ULAZAK, Uloga.WORKER, tijelo).andExpect(status().isBadRequest());
        posalji(IZLAZAK, Uloga.WORKER, tijelo).andExpect(status().isBadRequest());
        verify(dolazakService, never()).ulazak(anyString());
        verify(dolazakService, never()).izlazak(anyString());
    }

    @Test
    void nepoznatQrKodVraca404() throws Exception {
        when(dolazakService.ulazak(anyString()))
                .thenThrow(new ResursNijePronadjenException("Clan sa ovim QR kodom ne postoji."));

        posalji(ULAZAK, Uloga.WORKER, TIJELO)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void izlazakBezUlaskaVraca409() throws Exception {
        when(dolazakService.izlazak(anyString()))
                .thenThrow(new KonfliktException("Clan nema evidentiran ulazak danas."));

        posalji(IZLAZAK, Uloga.WORKER, TIJELO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Clan nema evidentiran ulazak danas."));
    }
}
