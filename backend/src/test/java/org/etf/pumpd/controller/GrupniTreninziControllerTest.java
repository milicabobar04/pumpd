package org.etf.pumpd.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.TerminGrupnogTreningaDto;
import org.etf.pumpd.dto.ZahtjevZaPrijavuOdgovor;
import org.etf.pumpd.enums.StatusPrijaveClana;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.security.TrenutniKorisnik;
import org.etf.pumpd.service.GrupniTreninziService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

/** Test ruta 9.1–9.3 zajedno sa pravilima iz SecurityConfig (bez baze). */
@WebMvcTest(GrupniTreninziController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class GrupniTreninziControllerTest {

    private static final String RUTA = "/api/group-trainings";
    private static final LocalDateTime POCETAK = LocalDateTime.of(2026, 12, 1, 18, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GrupniTreninziService grupniTreninziService;
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
        when(grupniTreninziService.pregled(any(), any(), any())).thenReturn(List.of());
        when(grupniTreninziService.posaljiZahtjev(anyInt(), anyInt())).thenReturn(
                new ZahtjevZaPrijavuOdgovor(77, StatusPrijaveClana.PENDING, "Zahtjev za prijavu je poslat."));
        when(grupniTreninziService.odjava(anyInt(), anyInt())).thenReturn(new PorukaDto("Odjavljeni ste sa termina."));
    }

    private ResultActions prijava(Uloga uloga, String id) throws Exception {
        return mockMvc.perform(post(RUTA + "/" + id + "/enrollment").header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name()));
    }

    private ResultActions odjava(Uloga uloga, String id) throws Exception {
        return mockMvc.perform(delete(RUTA + "/" + id + "/enrollment").header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name()));
    }

    private static int idNaloga(Uloga uloga) {
        return uloga.ordinal() + 1;
    }

    private ResultActions pregled(Uloga uloga, String upit) throws Exception {
        return mockMvc.perform(get(RUTA + upit).header(HttpHeaders.AUTHORIZATION, "Bearer token-" + uloga.name()));
    }

    private static TerminGrupnogTreningaDto termin(StatusPrijaveClana status) {
        return new TerminGrupnogTreningaDto(12, 7, "Pilates", 3, "Marko Markovic", POCETAK, 60, 4, 10, status);
    }

    // ------------------------------------------------------------ pristup

    @Test
    void bezTokenaVraca401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        verify(grupniTreninziService, never()).pregled(any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"TRAINER", "ADMIN"})
    void trenerIAdminNemajuPristup(Uloga uloga) throws Exception {
        pregled(uloga, "").andExpect(status().isForbidden());
        verify(grupniTreninziService, never()).pregled(any(), any(), any());
    }

    @Test
    void postNaPregledNijeDozvoljen() throws Exception {
        // pravilo za 9.1 važi samo za GET, a ostalo traži samo prijavu i ne postoji (405)
        mockMvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer token-MEMBER"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ------------------------------------------------------------ 9.1

    @Test
    void clanDobijaTermineSaStatusomPrijave() throws Exception {
        when(grupniTreninziService.pregled(idNaloga(Uloga.MEMBER), null, null))
                .thenReturn(List.of(termin(StatusPrijaveClana.PENDING)));

        pregled(Uloga.MEMBER, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sessionId").value(12))
                .andExpect(jsonPath("$[0].groupId").value(7))
                .andExpect(jsonPath("$[0].name").value("Pilates"))
                .andExpect(jsonPath("$[0].trainerId").value(3))
                .andExpect(jsonPath("$[0].trainerName").value("Marko Markovic"))
                .andExpect(jsonPath("$[0].startTime").value("2026-12-01T18:00:00"))
                .andExpect(jsonPath("$[0].durationMinutes").value(60))
                .andExpect(jsonPath("$[0].enrolledCount").value(4))
                .andExpect(jsonPath("$[0].maxMembers").value(10))
                .andExpect(jsonPath("$[0].enrollmentStatus").value("PENDING"));
    }

    @Test
    void radnikDobijaTermineBezStatusaPrijave() throws Exception {
        when(grupniTreninziService.pregled(isNull(), any(), any())).thenReturn(List.of(termin(null)));

        pregled(Uloga.WORKER, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(12))
                .andExpect(jsonPath("$[0].enrollmentStatus").doesNotExist());
        // radniku se ne prosljeđuje id, pa servis ne čita prijave
        verify(grupniTreninziService).pregled(null, null, null);
    }

    @Test
    void filteriSeProsljedjujuServisu() throws Exception {
        pregled(Uloga.MEMBER, "?date=2026-12-01&trainerId=3").andExpect(status().isOk());
        verify(grupniTreninziService).pregled(idNaloga(Uloga.MEMBER), LocalDate.of(2026, 12, 1), 3);

        pregled(Uloga.WORKER, "?trainerId=5").andExpect(status().isOk());
        verify(grupniTreninziService).pregled(null, null, 5);
    }

    @Test
    void praznaListaJe200() throws Exception {
        pregled(Uloga.MEMBER, "?date=2030-01-01").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"?date=01.12.2026", "?date=2026-13-01", "?date=danas", "?trainerId=abc", "?trainerId=1.5"})
    void neispravanFilterVraca400(String upit) throws Exception {
        pregled(Uloga.MEMBER, upit)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verify(grupniTreninziService, never()).pregled(any(), any(), any());
    }

    // ------------------------------------------------------------ 9.2 i 9.3

    @Test
    void prijavaIOdjavaBezTokenaVraca401() throws Exception {
        mockMvc.perform(post(RUTA + "/12/enrollment")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(RUTA + "/12/enrollment")).andExpect(status().isUnauthorized());
        verify(grupniTreninziService, never()).posaljiZahtjev(anyInt(), anyInt());
        verify(grupniTreninziService, never()).odjava(anyInt(), anyInt());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"TRAINER", "WORKER", "ADMIN"})
    void samoClanSePrijavljujeIOdjavljuje(Uloga uloga) throws Exception {
        prijava(uloga, "12").andExpect(status().isForbidden());
        odjava(uloga, "12").andExpect(status().isForbidden());
        verify(grupniTreninziService, never()).posaljiZahtjev(anyInt(), anyInt());
        verify(grupniTreninziService, never()).odjava(anyInt(), anyInt());
    }

    @Test
    void prijavaVraca201SaIdZahtjeva() throws Exception {
        prijava(Uloga.MEMBER, "12")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(77))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Zahtjev za prijavu je poslat."));
        verify(grupniTreninziService).posaljiZahtjev(12, idNaloga(Uloga.MEMBER));
    }

    @Test
    void prijavaSaTijelomIgnoriseTijelo() throws Exception {
        // ugovor: tijelo je prazno; ako aplikacija ipak pošalje JSON, ne smeta
        mockMvc.perform(post(RUTA + "/12/enrollment").header(HttpHeaders.AUTHORIZATION, "Bearer token-MEMBER")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    void odjavaVraca200() throws Exception {
        odjava(Uloga.MEMBER, "12")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Odjavljeni ste sa termina."));
        verify(grupniTreninziService).odjava(12, idNaloga(Uloga.MEMBER));
    }

    @Test
    void greskeServisaSePrenoseUOdgovor() throws Exception {
        when(grupniTreninziService.posaljiZahtjev(1, idNaloga(Uloga.MEMBER)))
                .thenThrow(new ResursNijePronadjenException("Termin ne postoji."));
        when(grupniTreninziService.posaljiZahtjev(2, idNaloga(Uloga.MEMBER)))
                .thenThrow(new KonfliktException("Termin je popunjen."));
        when(grupniTreninziService.posaljiZahtjev(3, idNaloga(Uloga.MEMBER)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Termin je vec poceo."));
        when(grupniTreninziService.odjava(4, idNaloga(Uloga.MEMBER)))
                .thenThrow(new ResursNijePronadjenException("Niste prijavljeni na termin niti imate zahtjev na cekanju."));

        prijava(Uloga.MEMBER, "1").andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Termin ne postoji."));
        prijava(Uloga.MEMBER, "2").andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Termin je popunjen."));
        prijava(Uloga.MEMBER, "3").andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Termin je vec poceo."));
        odjava(Uloga.MEMBER, "4").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Niste prijavljeni na termin niti imate zahtjev na cekanju."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5"})
    void neispravanIdTerminaVraca400(String id) throws Exception {
        prijava(Uloga.MEMBER, id).andExpect(status().isBadRequest());
        odjava(Uloga.MEMBER, id).andExpect(status().isBadRequest());
        verify(grupniTreninziService, never()).posaljiZahtjev(anyInt(), anyInt());
    }

    @Test
    void ostaleMetodeNaPrijaviNisuDozvoljene() throws Exception {
        mockMvc.perform(get(RUTA + "/12/enrollment").header(HttpHeaders.AUTHORIZATION, "Bearer token-MEMBER"))
                .andExpect(status().isMethodNotAllowed());
    }
}
