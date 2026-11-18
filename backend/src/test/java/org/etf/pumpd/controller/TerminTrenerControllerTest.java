package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.KreiranjeTerminaOdgovor;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.TerminRasporedDto;
import org.etf.pumpd.dto.TerminZahtjev;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.KorisnikService;
import org.etf.pumpd.service.NevazeciTokenService;
import org.etf.pumpd.service.TerminTreneraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Rute 10.4–10.7 zajedno sa pravilima pristupa iz SecurityConfig i validacijom tijela.
 * Servis je mockovan; token "token-<ULOGA>" je korisnik te uloge sa id = ordinal + 1.
 */
@WebMvcTest(TerminTrenerController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class TerminTrenerControllerTest {

    private static final String RASPORED = "/api/trainer/schedule";
    private static final String TERMINI = "/api/trainer/group-trainings";
    private static final int ID_TRENERA = Uloga.TRAINER.ordinal() + 1;
    private static final String ISPRAVAN = """
            {"name":"Joga","startTime":"2026-11-20T18:00:00","durationMinutes":60,"maxMembers":12}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TerminTreneraService terminService;
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
        when(terminService.raspored(anyInt(), any())).thenReturn(List.of(new TerminRasporedDto(
                70, "Joga", LocalDateTime.of(2026, 11, 20, 18, 0), 60, 4, 12)));
        when(terminService.kreiraj(anyInt(), any())).thenReturn(new KreiranjeTerminaOdgovor(70, 50, "Termin je dodat."));
        when(terminService.izmijeni(anyInt(), anyInt(), any())).thenReturn(new PorukaDto("Termin je izmijenjen."));
        when(terminService.obrisi(anyInt(), anyInt())).thenReturn(new PorukaDto("Termin je obrisan."));
    }

    private ResultActions posalji(MockHttpServletRequestBuilder zahtjev, Uloga uloga, String tijelo) throws Exception {
        if (uloga != null) {
            zahtjev.header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name());
        }
        if (tijelo != null) {
            zahtjev.contentType(MediaType.APPLICATION_JSON).content(tijelo);
        }
        return mockMvc.perform(zahtjev);
    }

    // ------------------------------------------------------------ pristup

    @Test
    void bezTokenaVraca401() throws Exception {
        posalji(get(RASPORED), null, null).andExpect(status().isUnauthorized());
        posalji(post(TERMINI), null, ISPRAVAN).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "WORKER", "ADMIN"})
    void samoTrenerImaPristup(Uloga uloga) throws Exception {
        posalji(get(RASPORED), uloga, null).andExpect(status().isForbidden());
        posalji(post(TERMINI), uloga, ISPRAVAN).andExpect(status().isForbidden());
        posalji(put(TERMINI + "/70"), uloga, ISPRAVAN).andExpect(status().isForbidden());
        posalji(delete(TERMINI + "/70"), uloga, null).andExpect(status().isForbidden());
        verify(terminService, never()).kreiraj(anyInt(), any());
    }

    // ------------------------------------------------------------ 10.4

    @Test
    void rasporedUFormatuIzUgovora() throws Exception {
        posalji(get(RASPORED).param("date", "2026-11-20"), Uloga.TRAINER, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(70))
                .andExpect(jsonPath("$[0].name").value("Joga"))
                .andExpect(jsonPath("$[0].startTime").value("2026-11-20T18:00:00"))
                .andExpect(jsonPath("$[0].durationMinutes").value(60))
                .andExpect(jsonPath("$[0].enrolledCount").value(4))
                .andExpect(jsonPath("$[0].maxMembers").value(12));
        verify(terminService).raspored(ID_TRENERA, LocalDate.of(2026, 11, 20));
    }

    @Test
    void rasporedBezDatumaProsljedjujeNull() throws Exception {
        posalji(get(RASPORED), Uloga.TRAINER, null).andExpect(status().isOk());
        verify(terminService).raspored(eq(ID_TRENERA), isNull());
    }

    @Test
    void neispravanDatumVraca400() throws Exception {
        posalji(get(RASPORED).param("date", "20.11.2026"), Uloga.TRAINER, null).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 10.5–10.7

    @Test
    void kreiranjeVraca201SaIdTerminaIGrupe() throws Exception {
        posalji(post(TERMINI), Uloga.TRAINER, ISPRAVAN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(70))
                .andExpect(jsonPath("$.groupId").value(50));

        ArgumentCaptor<TerminZahtjev> zahtjev = ArgumentCaptor.forClass(TerminZahtjev.class);
        verify(terminService).kreiraj(eq(ID_TRENERA), zahtjev.capture());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDateTime.of(2026, 11, 20, 18, 0),
                zahtjev.getValue().startTime());
    }

    @Test
    void izmjenaIBrisanjeVracaju200() throws Exception {
        posalji(put(TERMINI + "/70"), Uloga.TRAINER, ISPRAVAN).andExpect(status().isOk());
        posalji(delete(TERMINI + "/70"), Uloga.TRAINER, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Termin je obrisan."));
        verify(terminService).obrisi(ID_TRENERA, 70);
    }

    @Test
    void preklapanjeVraca409() throws Exception {
        when(terminService.kreiraj(anyInt(), any())).thenThrow(new KonfliktException("Termin se preklapa."));

        posalji(post(TERMINI), Uloga.TRAINER, ISPRAVAN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"startTime\":\"2026-11-20T18:00:00\",\"durationMinutes\":60,\"maxMembers\":12}",
            "{\"name\":\" \",\"startTime\":\"2026-11-20T18:00:00\",\"durationMinutes\":60,\"maxMembers\":12}",
            "{\"name\":\"Joga\",\"durationMinutes\":60,\"maxMembers\":12}",
            "{\"name\":\"Joga\",\"startTime\":\"20.11.2026 18:00\",\"durationMinutes\":60,\"maxMembers\":12}",
            "{\"name\":\"Joga\",\"startTime\":\"2026-11-20T18:00:00\",\"maxMembers\":12}",
            "{\"name\":\"Joga\",\"startTime\":\"2026-11-20T18:00:00\",\"durationMinutes\":10,\"maxMembers\":12}",
            "{\"name\":\"Joga\",\"startTime\":\"2026-11-20T18:00:00\",\"durationMinutes\":301,\"maxMembers\":12}",
            "{\"name\":\"Joga\",\"startTime\":\"2026-11-20T18:00:00\",\"durationMinutes\":60}",
            "{\"name\":\"Joga\",\"startTime\":\"2026-11-20T18:00:00\",\"durationMinutes\":60,\"maxMembers\":0}",
            "{\"name\":\"Joga\",\"startTime\":\"2026-11-20T18:00:00\",\"durationMinutes\":60,\"maxMembers\":2.5}"
    })
    void neispravnoTijeloVraca400(String tijelo) throws Exception {
        posalji(post(TERMINI), Uloga.TRAINER, tijelo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        posalji(put(TERMINI + "/70"), Uloga.TRAINER, tijelo).andExpect(status().isBadRequest());
        verify(terminService, never()).kreiraj(anyInt(), any());
        verify(terminService, never()).izmijeni(anyInt(), anyInt(), any());
    }

    @Test
    void sessionIdKojiNijeBrojVraca400() throws Exception {
        posalji(delete(TERMINI + "/abc"), Uloga.TRAINER, null).andExpect(status().isBadRequest());
    }
}
