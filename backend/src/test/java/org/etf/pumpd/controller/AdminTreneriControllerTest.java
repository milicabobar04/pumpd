package org.etf.pumpd.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.DodavanjeTreneraOdgovor;
import org.etf.pumpd.dto.DodavanjeTreneraZahtjev;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.TrenerDetaljiDto;
import org.etf.pumpd.dto.TrenerListaDto;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.service.AdminTreneriService;
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

/** Test ruta 12.1 i 12.2 zajedno sa pravilima pristupa iz SecurityConfig (bez baze). */
@WebMvcTest(AdminTreneriController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class})
class AdminTreneriControllerTest {

    private static final String RUTA = "/api/admin/trainers";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminTreneriService adminTreneriService;
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
        when(adminTreneriService.listaTrenera()).thenReturn(List.of());
    }

    private static String bearer(Uloga uloga) {
        return "Bearer token-" + uloga.name();
    }

    // ------------------------------------------------------------ pristup po ulogama

    @Test
    void bezTokenaVraca401() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER", "WORKER"})
    void samoAdministratorImaPristupListi(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(adminTreneriService, never()).listaTrenera();
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER", "WORKER"})
    void samoAdministratorImaPristupDetaljima(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA + "/3").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());

        verify(adminTreneriService, never()).detaljiTrenera(anyInt());
    }

    @Test
    void administratorDobija200() throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------ 12.1 lista

    @Test
    void listaUFormatuIzUgovora() throws Exception {
        when(adminTreneriService.listaTrenera()).thenReturn(List.of(
                new TrenerListaDto(3, "Marko", "Petrović", "trener1@pumpd.local")));

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainerId").value(3))
                .andExpect(jsonPath("$[0].name").value("Marko"))
                .andExpect(jsonPath("$[0].surname").value("Petrović"))
                .andExpect(jsonPath("$[0].email").value("trener1@pumpd.local"));
    }

    @Test
    void bezTreneraVracaPrazanNiz() throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------ 12.2 detalji

    @Test
    void detaljiUFormatuIzUgovora() throws Exception {
        when(adminTreneriService.detaljiTrenera(3)).thenReturn(new TrenerDetaljiDto(
                3, "Marko", "Petrović", "trener1@pumpd.local", "Kondicioni trener.", 4.5, 12));

        mockMvc.perform(get(RUTA + "/3").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerId").value(3))
                .andExpect(jsonPath("$.name").value("Marko"))
                .andExpect(jsonPath("$.surname").value("Petrović"))
                .andExpect(jsonPath("$.email").value("trener1@pumpd.local"))
                .andExpect(jsonPath("$.biography").value("Kondicioni trener."))
                .andExpect(jsonPath("$.averageGrade").value(4.5))
                .andExpect(jsonPath("$.followerCount").value(12));
    }

    @Test
    void ocjenaIBiografijaSeSaljuKaoNull() throws Exception {
        when(adminTreneriService.detaljiTrenera(3)).thenReturn(new TrenerDetaljiDto(
                3, "Marko", "Petrović", "trener1@pumpd.local", null, null, 0));

        mockMvc.perform(get(RUTA + "/3").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biography").value(nullValue()))
                .andExpect(jsonPath("$.averageGrade").value(nullValue()))
                .andExpect(jsonPath("$.followerCount").value(0));
    }

    @Test
    void nepostojeciTrenerVraca404UFormatuGreske() throws Exception {
        when(adminTreneriService.detaljiTrenera(anyInt()))
                .thenThrow(new ResursNijePronadjenException("Trener ne postoji."));

        mockMvc.perform(get(RUTA + "/99").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Trener ne postoji."));
    }

    @Test
    void idKojiNijeBrojVraca400() throws Exception {
        mockMvc.perform(get(RUTA + "/abc").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 12.3 dodavanje trenera

    private static final String ISPRAVAN_TRENER = """
            {"name":"Ana","surname":"Ilić","email":"ana@mail.com",
             "biography":"Pilates instruktor."}
            """;

    private ResultActions posalji(String tijelo, Uloga uloga) throws Exception {
        return mockMvc.perform(post(RUTA)
                .header(HttpHeaders.AUTHORIZATION, bearer(uloga))
                .contentType(MediaType.APPLICATION_JSON)
                .content(tijelo));
    }

    @Test
    void dodavanjeVraca201SaIdomIPorukom() throws Exception {
        when(adminTreneriService.dodajTrenera(any()))
                .thenReturn(new DodavanjeTreneraOdgovor(11, "Privremena7k", "Trener je uspjesno dodan."));

        posalji(ISPRAVAN_TRENER, Uloga.ADMIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trainerId").value(11))
                .andExpect(jsonPath("$.temporaryPassword").value("Privremena7k"))
                .andExpect(jsonPath("$.message").value("Trener je uspjesno dodan."));

        verify(adminTreneriService).dodajTrenera(new DodavanjeTreneraZahtjev(
                "Ana", "Ilić", "ana@mail.com", "Pilates instruktor."));
    }

    @Test
    void biografijaSeMozeIzostaviti() throws Exception {
        when(adminTreneriService.dodajTrenera(any())).thenReturn(new DodavanjeTreneraOdgovor(11, "Privremena7k", "ok"));

        posalji("""
                {"name":"Ana","surname":"Ilić","email":"ana@mail.com"}
                """, Uloga.ADMIN)
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"surname\":\"Ilić\",\"email\":\"ana@mail.com\"}",
            "{\"name\":\"Ana\",\"surname\":\"\",\"email\":\"ana@mail.com\"}",
            "{\"name\":\"Ana\",\"surname\":\"Ilić\",\"email\":\"nije-email\"}",
    })
    void nevalidniPodaciVracaju400(String tijelo) throws Exception {
        posalji(tijelo, Uloga.ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(adminTreneriService, never()).dodajTrenera(any());
    }

    @Test
    void biografijaDuzaOd500ZnakovaVraca400() throws Exception {
        String biografija = "a".repeat(501);

        posalji("""
                {"name":"Ana","surname":"Ilić","email":"ana@mail.com","biography":"%s"}
                """.formatted(biografija), Uloga.ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("biography: Biografija moze imati najvise 500 znakova."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{ovo nije json"})
    void neispravnoTijeloVraca400(String tijelo) throws Exception {
        posalji(tijelo, Uloga.ADMIN)
                .andExpect(status().isBadRequest());
    }

    @Test
    void zauzetEmailVraca409UFormatuGreske() throws Exception {
        when(adminTreneriService.dodajTrenera(any())).thenThrow(new KonfliktException("Email vec postoji."));

        posalji(ISPRAVAN_TRENER, Uloga.ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email vec postoji."));
    }

    @Test
    void radnikNeMozeDodatiTrenera() throws Exception {
        posalji(ISPRAVAN_TRENER, Uloga.WORKER)
                .andExpect(status().isForbidden());

        verify(adminTreneriService, never()).dodajTrenera(any());
    }

    // ------------------------------------------------------------ uklanjanje (meko brisanje)

    @Test
    void uklanjanjeVraca200SaPorukom() throws Exception {
        when(adminTreneriService.ukloniTrenera(3)).thenReturn(new PorukaDto("Trener je uspjesno uklonjen."));

        mockMvc.perform(delete(RUTA + "/3").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Trener je uspjesno uklonjen."));
    }

    @Test
    void uklanjanjeNepostojecegVraca404() throws Exception {
        when(adminTreneriService.ukloniTrenera(anyInt())).thenThrow(new ResursNijePronadjenException("Trener ne postoji."));

        mockMvc.perform(delete(RUTA + "/99").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Trener ne postoji."));
    }

    @Test
    void uklanjanjeSaIdomKojiNijeBrojVraca400() throws Exception {
        mockMvc.perform(delete(RUTA + "/abc").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER", "WORKER"})
    void uklanjanjeBezPravaVraca403(Uloga uloga) throws Exception {
        mockMvc.perform(delete(RUTA + "/3").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());

        verify(adminTreneriService, never()).ukloniTrenera(anyInt());
    }

    @Test
    void uklanjanjeBezTokenaVraca401() throws Exception {
        mockMvc.perform(delete(RUTA + "/3"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------ privremena lozinka

    @Test
    void lozinkaIzStarogFrontendaSeIgnorise() throws Exception {
        when(adminTreneriService.dodajTrenera(any())).thenReturn(new DodavanjeTreneraOdgovor(11, "Privremena7k", "ok"));

        posalji("{\"name\":\"Ana\",\"surname\":\"Ilić\",\"email\":\"ana@mail.com\",\"password\":\"x\"}", Uloga.ADMIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.temporaryPassword").value("Privremena7k"));
    }
}
