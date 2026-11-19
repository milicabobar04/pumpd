package org.etf.pumpd.controller;

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.ClanGrupeDto;
import org.etf.pumpd.dto.GrupaDto;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.GrupaTreneraService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Rute 10.8–10.11 zajedno sa pravilima pristupa iz SecurityConfig i validacijom tijela.
 * Servis je mockovan; token "token-<ULOGA>" je korisnik te uloge sa id = ordinal + 1.
 */
@WebMvcTest(GrupaTreneraController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class GrupaTreneraControllerTest {

    private static final String GRUPE = "/api/trainer/groups";
    private static final String CLANOVI = GRUPE + "/50/members";
    private static final int ID_TRENERA = Uloga.TRAINER.ordinal() + 1;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GrupaTreneraService grupaService;
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
        when(grupaService.listaGrupa(anyInt())).thenReturn(List.of(
                new GrupaDto(50, 70, "Joga", LocalDateTime.of(2026, 11, 20, 18, 0), 4, 12)));
        when(grupaService.clanoviGrupe(anyInt(), anyInt())).thenReturn(List.of(new ClanGrupeDto(6, "Ana", "Babic")));
        when(grupaService.dodajClana(anyInt(), anyInt(), anyInt())).thenReturn(new PorukaDto("Clan je dodat u grupu."));
        when(grupaService.ukloniClana(anyInt(), anyInt(), anyInt())).thenReturn(new PorukaDto("Clan je uklonjen iz grupe."));
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
        posalji(get(GRUPE), null, null).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "WORKER", "ADMIN"})
    void samoTrenerImaPristup(Uloga uloga) throws Exception {
        posalji(get(GRUPE), uloga, null).andExpect(status().isForbidden());
        posalji(get(CLANOVI), uloga, null).andExpect(status().isForbidden());
        posalji(post(CLANOVI), uloga, "{\"memberId\":6}").andExpect(status().isForbidden());
        posalji(delete(CLANOVI + "/6"), uloga, null).andExpect(status().isForbidden());
        verify(grupaService, never()).dodajClana(anyInt(), anyInt(), anyInt());
    }

    // ------------------------------------------------------------ odgovori

    @Test
    void listaGrupaUFormatuIzUgovora() throws Exception {
        posalji(get(GRUPE), Uloga.TRAINER, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(50))
                .andExpect(jsonPath("$[0].sessionId").value(70))
                .andExpect(jsonPath("$[0].name").value("Joga"))
                .andExpect(jsonPath("$[0].startTime").value("2026-11-20T18:00:00"))
                .andExpect(jsonPath("$[0].memberCount").value(4))
                .andExpect(jsonPath("$[0].maxMembers").value(12));
        verify(grupaService).listaGrupa(ID_TRENERA);
    }

    @Test
    void clanoviGrupeUFormatuIzUgovora() throws Exception {
        posalji(get(CLANOVI), Uloga.TRAINER, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberId").value(6))
                .andExpect(jsonPath("$[0].name").value("Ana"))
                .andExpect(jsonPath("$[0].surname").value("Babic"));
        verify(grupaService).clanoviGrupe(ID_TRENERA, 50);
    }

    @Test
    void dodavanjeVraca201() throws Exception {
        posalji(post(CLANOVI), Uloga.TRAINER, "{\"memberId\":6}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Clan je dodat u grupu."));
        verify(grupaService).dodajClana(ID_TRENERA, 50, 6);
    }

    @Test
    void uklanjanjeVraca200() throws Exception {
        posalji(delete(CLANOVI + "/6"), Uloga.TRAINER, null).andExpect(status().isOk());
        verify(grupaService).ukloniClana(ID_TRENERA, 50, 6);
    }

    @Test
    void punaGrupaVraca409() throws Exception {
        when(grupaService.dodajClana(anyInt(), anyInt(), anyInt())).thenThrow(new KonfliktException("Grupa je puna."));

        posalji(post(CLANOVI), Uloga.TRAINER, "{\"memberId\":6}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Grupa je puna."));
    }

    // ------------------------------------------------------------ validacija (400)

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"memberId\":null}", "{\"memberId\":0}", "{\"memberId\":-3}",
            "{\"memberId\":\"abc\"}", "{\"memberId\":6.5}"})
    void neispravnoTijeloVraca400(String tijelo) throws Exception {
        posalji(post(CLANOVI), Uloga.TRAINER, tijelo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verify(grupaService, never()).dodajClana(anyInt(), anyInt(), anyInt());
    }

    @Test
    void idKojiNijeBrojVraca400() throws Exception {
        posalji(get(GRUPE + "/abc/members"), Uloga.TRAINER, null).andExpect(status().isBadRequest());
        posalji(delete(CLANOVI + "/abc"), Uloga.TRAINER, null).andExpect(status().isBadRequest());
    }
}
