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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.RecenzijaDto;
import org.etf.pumpd.dto.RecenzijaOdgovor;
import org.etf.pumpd.dto.RecenzijaZahtjev;
import org.etf.pumpd.dto.TrenerZaClanaDetaljiDto;
import org.etf.pumpd.dto.TrenerZaClanaDto;
import org.etf.pumpd.dto.ZahtjevZaPracenjeOdgovor;
import org.etf.pumpd.enums.StatusPracenjaClana;
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
import org.etf.pumpd.service.PracenjeService;
import org.etf.pumpd.service.RecenzijeService;
import org.etf.pumpd.service.TreneriService;
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

/** Test ruta 7.1 i 7.2 zajedno sa pravilima pristupa iz SecurityConfig (bez baze). */
@WebMvcTest(TreneriController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class TreneriControllerTest {

    private static final String RUTA = "/api/trainers";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreneriService treneriService;
    @MockitoBean
    private PracenjeService pracenjeService;
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
        when(treneriService.listaTrenera()).thenReturn(List.of());
    }

    private static int idNaloga(Uloga uloga) {
        return uloga.ordinal() + 1;
    }

    private static String bearer(Uloga uloga) {
        return "Bearer token-" + uloga.name();
    }

    // ------------------------------------------------------------ pristup

    @Test
    void bezTokenaVraca401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"TRAINER", "WORKER", "ADMIN"})
    void samoClanVidiListuTrenera(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        verify(treneriService, never()).listaTrenera();
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"TRAINER", "WORKER", "ADMIN"})
    void samoClanVidiDetaljeTrenera(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA + "/3").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        verify(treneriService, never()).detaljiTrenera(anyInt(), anyInt());
    }

    // ------------------------------------------------------------ 7.1

    @Test
    void listaUFormatuIzUgovora() throws Exception {
        when(treneriService.listaTrenera()).thenReturn(List.of(
                new TrenerZaClanaDto(3, "Marko", "Petrović", 4.3),
                new TrenerZaClanaDto(4, "Ivana", "Jovanović", null)));

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainerId").value(3))
                .andExpect(jsonPath("$[0].name").value("Marko"))
                .andExpect(jsonPath("$[0].surname").value("Petrović"))
                .andExpect(jsonPath("$[0].averageGrade").value(4.3))
                .andExpect(jsonPath("$[1].averageGrade").value(nullValue()));
    }

    @Test
    void bezTreneraVracaPrazanNiz() throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------ 7.2

    @Test
    void detaljiUFormatuIzUgovoraZaPrijavljenogClana() throws Exception {
        when(treneriService.detaljiTrenera(3, idNaloga(Uloga.MEMBER))).thenReturn(new TrenerZaClanaDetaljiDto(
                3, "Marko", "Petrović", "Kondicioni trener.", 4.5, StatusPracenjaClana.PENDING,
                List.of(new RecenzijaDto(9, "Amar Hodžić", 5, "Odličan.", LocalDate.of(2026, 11, 10)))));

        mockMvc.perform(get(RUTA + "/3").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerId").value(3))
                .andExpect(jsonPath("$.biography").value("Kondicioni trener."))
                .andExpect(jsonPath("$.averageGrade").value(4.5))
                .andExpect(jsonPath("$.followStatus").value("PENDING"))
                .andExpect(jsonPath("$.reviews[0].reviewId").value(9))
                .andExpect(jsonPath("$.reviews[0].memberName").value("Amar Hodžić"))
                .andExpect(jsonPath("$.reviews[0].grade").value(5))
                .andExpect(jsonPath("$.reviews[0].comment").value("Odličan."))
                .andExpect(jsonPath("$.reviews[0].date").value("2026-11-10"));
    }

    @Test
    void detaljiBezRecenzijaImajuPrazanNizINull() throws Exception {
        when(treneriService.detaljiTrenera(3, idNaloga(Uloga.MEMBER))).thenReturn(new TrenerZaClanaDetaljiDto(
                3, "Marko", "Petrović", null, null, StatusPracenjaClana.NONE, List.of()));

        mockMvc.perform(get(RUTA + "/3").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(jsonPath("$.biography").value(nullValue()))
                .andExpect(jsonPath("$.averageGrade").value(nullValue()))
                .andExpect(jsonPath("$.followStatus").value("NONE"))
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.reviews").isEmpty());
    }

    @Test
    void nepostojeciTrenerVraca404() throws Exception {
        when(treneriService.detaljiTrenera(anyInt(), anyInt()))
                .thenThrow(new ResursNijePronadjenException("Trener ne postoji."));

        mockMvc.perform(get(RUTA + "/99").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Trener ne postoji."));
    }

    @Test
    void idKojiNijeBrojVraca400() throws Exception {
        mockMvc.perform(get(RUTA + "/abc").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 7.3 zahtjev za praćenje

    @Test
    void zahtjevVraca201ZaPrijavljenogClana() throws Exception {
        when(pracenjeService.posaljiZahtjev(3, idNaloga(Uloga.MEMBER))).thenReturn(
                new ZahtjevZaPracenjeOdgovor(15, StatusPracenjaClana.PENDING, "Zahtjev za pracenje je poslat."));

        mockMvc.perform(post(RUTA + "/3/follow").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.followRequestId").value(15))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Zahtjev za pracenje je poslat."));
    }

    @Test
    void zahtjevZaNepostojecegTreneraVraca404() throws Exception {
        when(pracenjeService.posaljiZahtjev(anyInt(), anyInt()))
                .thenThrow(new ResursNijePronadjenException("Trener ne postoji."));

        mockMvc.perform(post(RUTA + "/99/follow").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void dupliZahtjevVraca409UFormatuGreske() throws Exception {
        when(pracenjeService.posaljiZahtjev(anyInt(), anyInt()))
                .thenThrow(new KonfliktException("Zahtjev za pracenje je vec poslat."));

        mockMvc.perform(post(RUTA + "/3/follow").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Zahtjev za pracenje je vec poslat."));
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"TRAINER", "WORKER", "ADMIN"})
    void samoClanSaljeZahtjev(Uloga uloga) throws Exception {
        mockMvc.perform(post(RUTA + "/3/follow").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        verify(pracenjeService, never()).posaljiZahtjev(anyInt(), anyInt());
    }

    @Test
    void zahtjevSaIdomKojiNijeBrojVraca400() throws Exception {
        mockMvc.perform(post(RUTA + "/abc/follow").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 7.4 prekid

    @Test
    void prekidVraca200SaPorukom() throws Exception {
        when(pracenjeService.prekini(3, idNaloga(Uloga.MEMBER))).thenReturn(new PorukaDto("Pracenje je prekinuto."));

        mockMvc.perform(delete(RUTA + "/3/follow").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pracenje je prekinuto."));
    }

    @Test
    void prekidBezPracenjaVraca404() throws Exception {
        when(pracenjeService.prekini(anyInt(), anyInt()))
                .thenThrow(new ResursNijePronadjenException("Ne pratite ovog trenera."));

        mockMvc.perform(delete(RUTA + "/3/follow").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ne pratite ovog trenera."));
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"TRAINER", "WORKER", "ADMIN"})
    void samoClanPrekidaPracenje(Uloga uloga) throws Exception {
        mockMvc.perform(delete(RUTA + "/3/follow").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        verify(pracenjeService, never()).prekini(anyInt(), anyInt());
    }

    // ------------------------------------------------------------ 7.5 recenzija

    private ResultActions recenzija(String id, String tijelo, Uloga uloga)
            throws Exception {
        return mockMvc.perform(post(RUTA + "/" + id + "/reviews")
                .header(HttpHeaders.AUTHORIZATION, bearer(uloga))
                .contentType(MediaType.APPLICATION_JSON)
                .content(tijelo));
    }

    @Test
    void recenzijaVraca201SaNovimProsjekom() throws Exception {
        when(recenzijeService.ostaviRecenziju(3, idNaloga(Uloga.MEMBER), new RecenzijaZahtjev(5, "Odličan.")))
                .thenReturn(new RecenzijaOdgovor(21, 4.5, "Recenzija je sacuvana."));

        recenzija("3", "{\"grade\":5,\"comment\":\"Odličan.\"}", Uloga.MEMBER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").value(21))
                .andExpect(jsonPath("$.averageGrade").value(4.5))
                .andExpect(jsonPath("$.message").value("Recenzija je sacuvana."));
    }

    @Test
    void komentarSeMozeIzostaviti() throws Exception {
        when(recenzijeService.ostaviRecenziju(3, idNaloga(Uloga.MEMBER), new RecenzijaZahtjev(4, null)))
                .thenReturn(new RecenzijaOdgovor(21, 4.0, "Recenzija je sacuvana."));

        recenzija("3", "{\"grade\":4}", Uloga.MEMBER).andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"grade\":0}", "{\"grade\":6}", "{\"grade\":-1}", "{\"grade\":null}", "{}",
            "{\"grade\":4.5}", "{\"grade\":\"pet\"}", "", "{ovo nije json"})
    void ocjenaVanOpsegaIliNeispravnoTijeloVraca400(String tijelo) throws Exception {
        recenzija("3", tijelo, Uloga.MEMBER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verify(recenzijeService, never()).ostaviRecenziju(anyInt(), anyInt(), any());
    }

    @Test
    void predugKomentarVraca400SaPorukom() throws Exception {
        recenzija("3", "{\"grade\":5,\"comment\":\"" + "a".repeat(501) + "\"}", Uloga.MEMBER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("comment: Komentar moze imati najvise 500 znakova."));
    }

    @Test
    void recenzijaZaNepostojecegTreneraVraca404() throws Exception {
        when(recenzijeService.ostaviRecenziju(anyInt(), anyInt(), any()))
                .thenThrow(new ResursNijePronadjenException("Trener ne postoji."));

        recenzija("99", "{\"grade\":5}", Uloga.MEMBER).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"TRAINER", "WORKER", "ADMIN"})
    void samoClanOstavljaRecenziju(Uloga uloga) throws Exception {
        recenzija("3", "{\"grade\":5}", uloga).andExpect(status().isForbidden());
        verify(recenzijeService, never()).ostaviRecenziju(anyInt(), anyInt(), any());
    }
}
