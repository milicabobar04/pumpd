package org.etf.pumpd.controller;

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
import org.etf.pumpd.dto.ClanDetaljiDto;
import org.etf.pumpd.dto.ClanListaDto;
import org.etf.pumpd.dto.DodavanjeClanaOdgovor;
import org.etf.pumpd.dto.DodavanjeClanaZahtjev;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.security.JsonAccessDeniedHandler;
import org.etf.pumpd.security.JsonAuthenticationEntryPoint;
import org.etf.pumpd.security.JsonGreskaPisac;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.etf.pumpd.service.ClanoviService;
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
 * Test rute 11.1 zajedno sa pravilima pristupa iz SecurityConfig.
 * Ne treba bazu: servisi su mockovani, a token "token-<ULOGA>" predstavlja
 * prijavljenog korisnika te uloge.
 */
@WebMvcTest(ClanoviController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class})
class ClanoviControllerTest {

    private static final String RUTA = "/api/worker/members";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClanoviService clanoviService;
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
        when(clanoviService.listaClanova(any())).thenReturn(List.of());
    }

    private static String bearer(Uloga uloga) {
        return "Bearer token-" + uloga.name();
    }

    // ------------------------------------------------------------ pristup po ulogama

    @Test
    void bezTokenaVraca401() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void neispravanTokenVraca401() throws Exception {
        when(jwtService.procitaj("pogresan")).thenReturn(Optional.empty());

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer pogresan"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER"})
    void clanITrenerDobijaju403(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"WORKER", "ADMIN"})
    void radnikIAdministratorDobijaju200(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------ 11.1 odgovor

    @Test
    void vracaListuUFormatuIzUgovora() throws Exception {
        when(clanoviService.listaClanova(any())).thenReturn(List.of(
                new ClanListaDto(6, "Amar", "Hodžić", "clan1@pumpd.local", true)));

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberId").value(6))
                .andExpect(jsonPath("$[0].name").value("Amar"))
                .andExpect(jsonPath("$[0].surname").value("Hodžić"))
                .andExpect(jsonPath("$[0].email").value("clan1@pumpd.local"))
                .andExpect(jsonPath("$[0].membershipActive").value(true));
    }

    @Test
    void bezClanovaVracaPrazanNiz() throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void parametarSearchStizeDoServisa() throws Exception {
        mockMvc.perform(get(RUTA).param("search", "amar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Uloga.WORKER)))
                .andExpect(status().isOk());

        verify(clanoviService).listaClanova("amar");
    }

    // ------------------------------------------------------------ 11.2 detalji člana

    @Test
    void detaljiUFormatuIzUgovora() throws Exception {
        when(clanoviService.detaljiClana(6)).thenReturn(new ClanDetaljiDto(
                6, "Amar", "Hodžić", "clan1@pumpd.local", 1998, 80.0, 180.0,
                true, LocalDate.of(2026, 11, 30)));

        mockMvc.perform(get(RUTA + "/6").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.WORKER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(6))
                .andExpect(jsonPath("$.name").value("Amar"))
                .andExpect(jsonPath("$.surname").value("Hodžić"))
                .andExpect(jsonPath("$.email").value("clan1@pumpd.local"))
                .andExpect(jsonPath("$.birthYear").value(1998))
                .andExpect(jsonPath("$.weight").value(80.0))
                .andExpect(jsonPath("$.height").value(180.0))
                .andExpect(jsonPath("$.membershipActive").value(true))
                .andExpect(jsonPath("$.membershipValidUntil").value("2026-11-30"));
    }

    @Test
    void nepostojeciClanVraca404UFormatuGreske() throws Exception {
        when(clanoviService.detaljiClana(anyInt())).thenThrow(new ResursNijePronadjenException("Clan ne postoji."));

        mockMvc.perform(get(RUTA + "/99").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Clan ne postoji."));
    }

    @Test
    void idKojiNijeBrojVraca400() throws Exception {
        mockMvc.perform(get(RUTA + "/abc").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.WORKER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void clanNeMozeVidjetiDetaljeDrugogClana() throws Exception {
        mockMvc.perform(get(RUTA + "/6").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ 11.3 dodavanje člana

    private static final String ISPRAVAN_CLAN = """
            {"name":"Ana","surname":"Ilić","email":"ana@mail.com","password":"lozinka123",
             "birthYear":2000,"weight":60.5,"height":168.0}
            """;

    private ResultActions posalji(String tijelo, Uloga uloga) throws Exception {
        return mockMvc.perform(post(RUTA)
                .header(HttpHeaders.AUTHORIZATION, bearer(uloga))
                .contentType(MediaType.APPLICATION_JSON)
                .content(tijelo));
    }

    @Test
    void dodavanjeVraca201SaIdomIPorukom() throws Exception {
        when(clanoviService.dodajClana(any())).thenReturn(new DodavanjeClanaOdgovor(11, "Clan je uspjesno dodan."));

        posalji(ISPRAVAN_CLAN, Uloga.WORKER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(11))
                .andExpect(jsonPath("$.message").value("Clan je uspjesno dodan."));

        verify(clanoviService).dodajClana(new DodavanjeClanaZahtjev(
                "Ana", "Ilić", "ana@mail.com", "lozinka123", 2000, 60.5, 168.0));
    }

    @Test
    void opcionaPoljaSeMoguIzostaviti() throws Exception {
        when(clanoviService.dodajClana(any())).thenReturn(new DodavanjeClanaOdgovor(11, "ok"));

        posalji("""
                {"name":"Ana","surname":"Ilić","email":"ana@mail.com","password":"lozinka123"}
                """, Uloga.ADMIN)
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"surname\":\"Ilić\",\"email\":\"ana@mail.com\",\"password\":\"lozinka123\"}",
            "{\"name\":\"  \",\"surname\":\"Ilić\",\"email\":\"ana@mail.com\",\"password\":\"lozinka123\"}",
            "{\"name\":\"Ana\",\"surname\":\"Ilić\",\"email\":\"nije-email\",\"password\":\"lozinka123\"}",
            "{\"name\":\"Ana\",\"surname\":\"Ilić\",\"email\":\"ana@mail.com\",\"password\":\"kratka\"}",
            "{\"name\":\"Ana\",\"surname\":\"Ilić\",\"email\":\"ana@mail.com\",\"password\":\"lozinka123\",\"weight\":-5}",
            "{\"name\":\"Ana\",\"surname\":\"Ilić\",\"email\":\"ana@mail.com\",\"password\":\"lozinka123\",\"height\":301}",
            "{\"name\":\"Ana\",\"surname\":\"Ilić\",\"email\":\"ana@mail.com\",\"password\":\"lozinka123\",\"birthYear\":1899}"
    })
    void nevalidniPodaciVracaju400(String tijelo) throws Exception {
        posalji(tijelo, Uloga.WORKER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(clanoviService, never()).dodajClana(any());
    }

    @Test
    void porukaKazeKojePoljeNijeIspravno() throws Exception {
        posalji("""
                {"name":"Ana","surname":"Ilić","email":"nije-email","password":"lozinka123"}
                """, Uloga.WORKER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: Email nije ispravnog formata."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{ovo nije json", "[]"})
    void neispravnoTijeloVraca400(String tijelo) throws Exception {
        posalji(tijelo, Uloga.WORKER)
                .andExpect(status().isBadRequest());
    }

    @Test
    void zauzetEmailVraca409UFormatuGreske() throws Exception {
        when(clanoviService.dodajClana(any())).thenThrow(new KonfliktException("Email vec postoji."));

        posalji(ISPRAVAN_CLAN, Uloga.WORKER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email vec postoji."));
    }

    @Test
    void clanNeMozeDodatiClana() throws Exception {
        posalji(ISPRAVAN_CLAN, Uloga.MEMBER)
                .andExpect(status().isForbidden());

        verify(clanoviService, never()).dodajClana(any());
    }

    // ------------------------------------------------------------ uklanjanje (meko brisanje)

    @Test
    void uklanjanjeVraca200SaPorukom() throws Exception {
        when(clanoviService.ukloniClana(6)).thenReturn(new PorukaDto("Clan je uspjesno uklonjen."));

        mockMvc.perform(delete(RUTA + "/6").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Clan je uspjesno uklonjen."));
    }

    @Test
    void uklanjanjeNepostojecegVraca404() throws Exception {
        when(clanoviService.ukloniClana(anyInt())).thenThrow(new ResursNijePronadjenException("Clan ne postoji."));

        mockMvc.perform(delete(RUTA + "/99").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Clan ne postoji."));
    }

    @Test
    void uklanjanjeSaIdomKojiNijeBrojVraca400() throws Exception {
        mockMvc.perform(delete(RUTA + "/abc").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "TRAINER"})
    void uklanjanjeBezPravaVraca403(Uloga uloga) throws Exception {
        mockMvc.perform(delete(RUTA + "/6").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());

        verify(clanoviService, never()).ukloniClana(anyInt());
    }

    @Test
    void uklanjanjeBezTokenaVraca401() throws Exception {
        mockMvc.perform(delete(RUTA + "/6"))
                .andExpect(status().isUnauthorized());
    }
}
