package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.KreiranjePlanaOdgovor;
import org.etf.pumpd.dto.PlanDetaljiDto;
import org.etf.pumpd.dto.PlanListDto;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.VjezbaDto;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.KorisnikService;
import org.etf.pumpd.service.NevazeciTokenService;
import org.etf.pumpd.service.PlanTreningaService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Rute 3.1–3.5 zajedno sa pravilima pristupa iz SecurityConfig i validacijom tijela.
 * Servis je mockovan; token "token-<ULOGA>" je korisnik te uloge sa id = ordinal + 1.
 */
@WebMvcTest(PlanTreningaController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class PlanTreningaControllerTest {

    private static final String RUTA = "/api/training-plans";
    private static final String ISPRAVAN = """
            {"name":"Snaga","description":null,"durationMinutes":60,
             "exercises":[{"name":"Cucanj","numSets":4,"numRepetitions":8}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanTreningaService planService;
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
        when(planService.listaPlanova(anyInt()))
                .thenReturn(List.of(new PlanListDto(10, "Snaga", null, 60)));
        when(planService.detaljiPlana(anyInt(), anyInt())).thenReturn(new PlanDetaljiDto(10, "Snaga", "Opis", 60,
                List.of(new VjezbaDto(21, "Cucanj", 4, 8))));
        when(planService.kreirajPlan(anyInt(), any(), any()))
                .thenReturn(new KreiranjePlanaOdgovor(10, "Plan treninga je kreiran."));
        when(planService.izmijeniPlan(anyInt(), anyInt(), any())).thenReturn(new PorukaDto("Plan treninga je izmijenjen."));
        when(planService.obrisiPlan(anyInt(), anyInt())).thenReturn(new PorukaDto("Plan treninga je obrisan."));
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
        posalji(get(RUTA), null, null).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"WORKER", "ADMIN"})
    void radnikIAdministratorDobijaju403(Uloga uloga) throws Exception {
        posalji(get(RUTA), uloga, null).andExpect(status().isForbidden());
        posalji(post(RUTA), uloga, ISPRAVAN).andExpect(status().isForbidden());
        verify(planService, never()).kreirajPlan(anyInt(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER"})
    void clanITrenerVideSvojePlanove(Uloga uloga) throws Exception {
        posalji(get(RUTA), uloga, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planId").value(10))
                .andExpect(jsonPath("$[0].durationMinutes").value(60));
        verify(planService).listaPlanova(uloga.ordinal() + 1);
    }

    // ------------------------------------------------------------ odgovori

    @Test
    void detaljiUFormatuIzUgovora() throws Exception {
        posalji(get(RUTA + "/10"), Uloga.MEMBER, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(10))
                .andExpect(jsonPath("$.exercises[0].exerciseId").value(21))
                .andExpect(jsonPath("$.exercises[0].numSets").value(4))
                .andExpect(jsonPath("$.exercises[0].numRepetitions").value(8));
    }

    @Test
    void kreiranjeVraca201IProsljedjujeUlogu() throws Exception {
        posalji(post(RUTA), Uloga.TRAINER, ISPRAVAN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planId").value(10));
        verify(planService).kreirajPlan(eq(Uloga.TRAINER.ordinal() + 1), eq(Uloga.TRAINER), any());
    }

    @Test
    void izmjenaIBrisanjeVracaju200() throws Exception {
        posalji(put(RUTA + "/10"), Uloga.MEMBER, ISPRAVAN).andExpect(status().isOk());
        posalji(delete(RUTA + "/10"), Uloga.MEMBER, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Plan treninga je obrisan."));
    }

    // ------------------------------------------------------------ validacija (400)

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"durationMinutes\":60,\"exercises\":[{\"name\":\"A\",\"numSets\":1,\"numRepetitions\":1}]}",
            "{\"name\":\" \",\"durationMinutes\":60,\"exercises\":[{\"name\":\"A\",\"numSets\":1,\"numRepetitions\":1}]}",
            "{\"name\":\"Plan\",\"exercises\":[{\"name\":\"A\",\"numSets\":1,\"numRepetitions\":1}]}",
            "{\"name\":\"Plan\",\"durationMinutes\":0,\"exercises\":[{\"name\":\"A\",\"numSets\":1,\"numRepetitions\":1}]}",
            "{\"name\":\"Plan\",\"durationMinutes\":60,\"exercises\":[]}",
            "{\"name\":\"Plan\",\"durationMinutes\":60}",
            "{\"name\":\"Plan\",\"durationMinutes\":60,\"exercises\":[{\"name\":\"\",\"numSets\":1,\"numRepetitions\":1}]}",
            "{\"name\":\"Plan\",\"durationMinutes\":60,\"exercises\":[{\"name\":\"A\",\"numSets\":0,\"numRepetitions\":1}]}",
            "{\"name\":\"Plan\",\"durationMinutes\":60,\"exercises\":[{\"name\":\"A\",\"numSets\":1}]}",
            "{\"name\":\"Plan\",\"durationMinutes\":60,\"exercises\":[{\"name\":\"A\",\"numSets\":1.5,\"numRepetitions\":1}]}",
            "{\"name\":\"Plan\",\"durationMinutes\":60,\"exercises\":[null]}"
    })
    void neispravnoTijeloVraca400(String tijelo) throws Exception {
        posalji(post(RUTA), Uloga.MEMBER, tijelo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        posalji(put(RUTA + "/10"), Uloga.MEMBER, tijelo).andExpect(status().isBadRequest());
        verify(planService, never()).kreirajPlan(anyInt(), any(), any());
        verify(planService, never()).izmijeniPlan(anyInt(), anyInt(), any());
    }

    @Test
    void predugNazivVraca400() throws Exception {
        String tijelo = """
                {"name":"%s","durationMinutes":60,"exercises":[{"name":"A","numSets":1,"numRepetitions":1}]}
                """.formatted("x".repeat(46));
        posalji(post(RUTA), Uloga.MEMBER, tijelo).andExpect(status().isBadRequest());
    }

    @Test
    void planIdKojiNijeBrojVraca400() throws Exception {
        posalji(get(RUTA + "/abc"), Uloga.MEMBER, null).andExpect(status().isBadRequest());
    }
}
