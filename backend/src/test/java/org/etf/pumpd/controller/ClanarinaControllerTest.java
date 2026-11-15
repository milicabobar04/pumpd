package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.ClanarinaClanaDto;
import org.etf.pumpd.dto.ClanarinaDto;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.ProduzenjeClanarineOdgovor;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.ClanarinaService;
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

/**
 * Rute članarine (član: /api/membership, radnik: /api/worker/members/{id}/membership)
 * zajedno sa pravilima pristupa iz SecurityConfig. Servis je mockovan.
 */
@WebMvcTest({ClanarinaController.class, ClanarinaRadnikController.class})
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class ClanarinaControllerTest {

    private static final String MOJA = "/api/membership/my-status";
    private static final String RADNIK = "/api/worker/members/5/membership";
    private static final LocalDate DATUM = LocalDate.of(2026, 10, 29);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClanarinaService clanarinaService;
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
        when(clanarinaService.mojaClanarina(anyInt()))
                .thenReturn(new ClanarinaClanaDto(DATUM, DATUM.plusMonths(1), 40.0, true, "data:image/png;base64,AA"));
        when(clanarinaService.clanarinaClana(anyInt()))
                .thenReturn(new ClanarinaDto(DATUM, DATUM.plusMonths(1), 40.0, true));
        when(clanarinaService.postaviClanarinu(anyInt(), any())).thenReturn(new PorukaDto("Clanarina je sacuvana."));
        when(clanarinaService.produziClanarinu(anyInt(), any()))
                .thenReturn(new ProduzenjeClanarineOdgovor(DATUM.plusMonths(2), "Clanarina je produzena."));
    }

    private static String bearer(Uloga uloga) {
        return "Bearer token-" + uloga.name();
    }

    private static String izmjena(String paymentDate, String validUntil, String price) {
        return """
                {"paymentDate":%s,"validUntil":%s,"price":%s}
                """.formatted(paymentDate, validUntil, price);
    }

    // ------------------------------------------------------------ član: moja članarina

    @Test
    void mojaClanarinaBezTokenaVraca401() throws Exception {
        mockMvc.perform(get(MOJA)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"WORKER", "TRAINER", "ADMIN"})
    void mojaClanarinaSamoZaClana(Uloga uloga) throws Exception {
        mockMvc.perform(get(MOJA).header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
    }

    @Test
    void mojaClanarinaVracaPodatkeZaPrijavljenogClana() throws Exception {
        mockMvc.perform(get(MOJA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentDate").value("2026-10-29"))
                .andExpect(jsonPath("$.validUntil").value("2026-11-29"))
                .andExpect(jsonPath("$.price").value(40.0))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.qrCodeUrl").value("data:image/png;base64,AA"));

        verify(clanarinaService).mojaClanarina(Uloga.MEMBER.ordinal() + 1);
    }

    @Test
    void mojaClanarinaBezClanarineVraca404() throws Exception {
        when(clanarinaService.mojaClanarina(anyInt())).thenThrow(new ResursNijePronadjenException("Nemate clanarinu."));

        mockMvc.perform(get(MOJA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ------------------------------------------------------------ radnik: pristup

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER"})
    void radnickeRuteZabranjeneClanuITreneru(Uloga uloga) throws Exception {
        mockMvc.perform(get(RADNIK).header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(RADNIK + "/extend").header(HttpHeaders.AUTHORIZATION, bearer(uloga))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"numMonths\":1}"))
                .andExpect(status().isForbidden());
        verify(clanarinaService, never()).produziClanarinu(anyInt(), any());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"WORKER", "ADMIN"})
    void radnikIAdministratorVideClanarinu(Uloga uloga) throws Exception {
        mockMvc.perform(get(RADNIK).header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validUntil").value("2026-11-29"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.qrCodeUrl").doesNotExist());

        verify(clanarinaService).clanarinaClana(5);
    }

    // ------------------------------------------------------------ radnik: postavljanje

    @Test
    void postavljanjeIspravnogZahtjeva() throws Exception {
        mockMvc.perform(put(RADNIK).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.WORKER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(izmjena("\"2026-10-29\"", "\"2026-11-29\"", "40")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Clanarina je sacuvana."));

        verify(clanarinaService).postaviClanarinu(eq(5), any());
    }

    @Test
    void postavljanjeBezPoljaVraca400() throws Exception {
        mockMvc.perform(put(RADNIK).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.WORKER))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(clanarinaService, never()).postaviClanarinu(anyInt(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "100001"})
    void postavljanjeSaNeispravnomCijenomVraca400(String cijena) throws Exception {
        mockMvc.perform(put(RADNIK).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.WORKER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(izmjena("\"2026-10-29\"", "\"2026-11-29\"", cijena)))
                .andExpect(status().isBadRequest());

        verify(clanarinaService, never()).postaviClanarinu(anyInt(), any());
    }

    @Test
    void postavljanjeSaNeispravnimDatumomVraca400() throws Exception {
        mockMvc.perform(put(RADNIK).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.WORKER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(izmjena("\"nije-datum\"", "\"2026-11-29\"", "40")))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ radnik: produženje

    @Test
    void produzenjeVracaNoviDatum() throws Exception {
        mockMvc.perform(post(RADNIK + "/extend").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.WORKER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"numMonths\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newValidUntil").value("2026-12-29"))
                .andExpect(jsonPath("$.message").value("Clanarina je produzena."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"numMonths\":0}", "{\"numMonths\":25}", "{}", "{\"numMonths\":\"x\"}"})
    void produzenjeSaNeispravnimBrojemMjeseciVraca400(String tijelo) throws Exception {
        mockMvc.perform(post(RADNIK + "/extend").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.WORKER))
                        .contentType(MediaType.APPLICATION_JSON).content(tijelo))
                .andExpect(status().isBadRequest());

        verify(clanarinaService, never()).produziClanarinu(anyInt(), any());
    }

    @Test
    void produzenjeNepostojecegClanaVraca404() throws Exception {
        when(clanarinaService.produziClanarinu(anyInt(), any()))
                .thenThrow(new ResursNijePronadjenException("Clan ne postoji."));

        mockMvc.perform(post(RADNIK + "/extend").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.WORKER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"numMonths\":1}"))
                .andExpect(status().isNotFound());
    }
}
