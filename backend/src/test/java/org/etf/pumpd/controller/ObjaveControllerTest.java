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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.etf.pumpd.config.SecurityConfig;
import org.etf.pumpd.dto.MojaObjavaDto;
import org.etf.pumpd.dto.NovaObjavaOdgovor;
import org.etf.pumpd.dto.NovaObjavaZahtjev;
import org.etf.pumpd.dto.ObjavaDto;
import org.etf.pumpd.dto.PorukaDto;
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
import org.etf.pumpd.service.ObjaveService;
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

/** Test ruta 8.5–8.7 (trenerova strana objava) zajedno sa pravilima iz SecurityConfig (bez baze). */
@WebMvcTest(ObjaveController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JsonGreskaPisac.class, TrenutniKorisnik.class})
class ObjaveControllerTest {

    private static final String RUTA = "/api/posts";
    private static final String SLIKA = "/api/images/7f3c9a2e-41b8-4d2a-9c11-5e8a0b6d2f44.jpg";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObjaveService objaveService;
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
        when(objaveService.mojeObjave(anyInt())).thenReturn(List.of());
        when(objaveService.feed(anyInt())).thenReturn(List.of());
        when(objaveService.sacuvane(anyInt())).thenReturn(List.of());
        when(objaveService.sacuvaj(anyInt(), anyInt())).thenReturn(new PorukaDto("Objava je sacuvana."));
        when(objaveService.ukloniIzSacuvanih(anyInt(), anyInt()))
                .thenReturn(new PorukaDto("Objava je uklonjena iz sacuvanih."));
        when(objaveService.dodajObjavu(anyInt(), any())).thenReturn(new NovaObjavaOdgovor(31, "Objava je dodata."));
        when(objaveService.obrisiObjavu(anyInt(), anyInt())).thenReturn(new PorukaDto("Objava je obrisana."));
    }

    private static int idNaloga(Uloga uloga) {
        return uloga.ordinal() + 1;
    }

    private static String bearer(Uloga uloga) {
        return "Bearer token-" + uloga.name();
    }

    private ResultActions dodaj(String tijelo, Uloga uloga) throws Exception {
        return mockMvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(uloga))
                .contentType(MediaType.APPLICATION_JSON).content(tijelo));
    }

    // ------------------------------------------------------------ pristup

    @Test
    void bezTokenaVraca401() throws Exception {
        mockMvc.perform(get(RUTA + "/my-posts")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(RUTA + "/31")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"MEMBER", "WORKER", "ADMIN"})
    void samoTrenerUpravljaObjavama(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA + "/my-posts").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        dodaj("{\"content\":\"x\"}", uloga).andExpect(status().isForbidden());
        mockMvc.perform(delete(RUTA + "/31").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());

        verify(objaveService, never()).dodajObjavu(anyInt(), any());
        verify(objaveService, never()).obrisiObjavu(anyInt(), anyInt());
    }

    @Test
    void brisanjeObjaveIUklanjanjeIzSacuvanihSuRazliciteUloge() throws Exception {
        // DELETE /api/posts/{id} je trenerov (8.7), a DELETE /api/posts/{id}/save članov (8.4)
        mockMvc.perform(delete(RUTA + "/31").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(RUTA + "/31/save").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(RUTA + "/31").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------ 8.6 dodavanje

    @Test
    void dodavanjeVraca201IPrenosiTreneraIzTokena() throws Exception {
        dodaj("{\"content\":\"Novi trening!\",\"imageUrl\":\"" + SLIKA + "\"}", Uloga.TRAINER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(31))
                .andExpect(jsonPath("$.message").value("Objava je dodata."));

        verify(objaveService).dodajObjavu(idNaloga(Uloga.TRAINER), new NovaObjavaZahtjev("Novi trening!", SLIKA));
    }

    @Test
    void slikaSeMozeIzostaviti() throws Exception {
        dodaj("{\"content\":\"Bez slike\"}", Uloga.TRAINER).andExpect(status().isCreated());

        verify(objaveService).dodajObjavu(idNaloga(Uloga.TRAINER), new NovaObjavaZahtjev("Bez slike", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"content\":\"\"}", "{\"content\":\"   \"}", "{\"content\":null}",
            "", "{ovo nije json"})
    void prazanSadrzajIliNeispravnoTijeloVraca400(String tijelo) throws Exception {
        dodaj(tijelo, Uloga.TRAINER).andExpect(status().isBadRequest());
        verify(objaveService, never()).dodajObjavu(anyInt(), any());
    }

    @Test
    void predugSadrzajVraca400SaPorukom() throws Exception {
        dodaj("{\"content\":\"" + "a".repeat(501) + "\"}", Uloga.TRAINER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("content: Sadrzaj objave moze imati najvise 500 znakova."));
    }

    @Test
    void predugaAdresaSlikeVraca400() throws Exception {
        dodaj("{\"content\":\"x\",\"imageUrl\":\"/api/images/" + "a".repeat(250) + "\"}", Uloga.TRAINER)
                .andExpect(status().isBadRequest());
    }

    @Test
    void slikaKojaNePostojiVraca400IzServisa() throws Exception {
        when(objaveService.dodajObjavu(anyInt(), any())).thenThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Slika ne postoji. Sliku prvo treba poslati na /api/posts/images."));

        dodaj("{\"content\":\"x\",\"imageUrl\":\"https://nekisajt.com/a.jpg\"}", Uloga.TRAINER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Slika ne postoji. Sliku prvo treba poslati na /api/posts/images."));
    }

    // ------------------------------------------------------------ 8.5 sopstvene objave

    @Test
    void mojeObjaveUFormatuIzUgovora() throws Exception {
        when(objaveService.mojeObjave(idNaloga(Uloga.TRAINER))).thenReturn(List.of(
                new MojaObjavaDto(31, "Tekst", SLIKA, LocalDateTime.of(2026, 11, 22, 10, 30, 0)),
                new MojaObjavaDto(30, "Bez slike", null, LocalDateTime.of(2026, 11, 21, 9, 5, 7))));

        mockMvc.perform(get(RUTA + "/my-posts").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").value(31))
                .andExpect(jsonPath("$[0].content").value("Tekst"))
                .andExpect(jsonPath("$[0].imageUrl").value(SLIKA))
                // sekunde se ispisuju i kad su 0 (oblik YYYY-MM-DDTHH:mm:ss iz ugovora)
                .andExpect(jsonPath("$[0].postTimestamp").value("2026-11-22T10:30:00"))
                .andExpect(jsonPath("$[1].imageUrl").value(nullValue()))
                .andExpect(jsonPath("$[1].postTimestamp").value("2026-11-21T09:05:07"));
    }

    @Test
    void bezObjavaPrazanNiz() throws Exception {
        mockMvc.perform(get(RUTA + "/my-posts").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------ 8.7 brisanje

    @Test
    void brisanjePrenosiTreneraIzTokena() throws Exception {
        mockMvc.perform(delete(RUTA + "/31").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Objava je obrisana."));

        verify(objaveService).obrisiObjavu(31, idNaloga(Uloga.TRAINER));
    }

    @Test
    void nepostojecaObjavaVraca404() throws Exception {
        when(objaveService.obrisiObjavu(anyInt(), anyInt())).thenThrow(new ResursNijePronadjenException("Objava ne postoji."));

        mockMvc.perform(delete(RUTA + "/99").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Objava ne postoji."));
    }

    @Test
    void tudjaObjavaVraca403UFormatuGreske() throws Exception {
        when(objaveService.obrisiObjavu(anyInt(), anyInt()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Objava pripada drugom treneru."));

        mockMvc.perform(delete(RUTA + "/31").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Objava pripada drugom treneru."));
    }

    @Test
    void idKojiNijeBrojVraca400() throws Exception {
        mockMvc.perform(delete(RUTA + "/abc").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.TRAINER)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 8.1 feed

    @Test
    void feedBezTokenaVraca401() throws Exception {
        mockMvc.perform(get(RUTA + "/feed")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"TRAINER", "WORKER", "ADMIN"})
    void samoClanVidiFeed(Uloga uloga) throws Exception {
        mockMvc.perform(get(RUTA + "/feed").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        verify(objaveService, never()).feed(anyInt());
    }

    @Test
    void feedUFormatuIzUgovoraZaClanaIzTokena() throws Exception {
        when(objaveService.feed(idNaloga(Uloga.MEMBER))).thenReturn(List.of(
                new ObjavaDto(33, 4, "Ivana Jovanović", "Joga u subotu!", SLIKA,
                        LocalDateTime.of(2026, 11, 22, 10, 30, 0), true),
                new ObjavaDto(31, 3, "Marko Petrović", "Novi trening!", null,
                        LocalDateTime.of(2026, 11, 22, 9, 15, 7), false)));

        mockMvc.perform(get(RUTA + "/feed").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").value(33))
                .andExpect(jsonPath("$[0].trainerId").value(4))
                .andExpect(jsonPath("$[0].trainerName").value("Ivana Jovanović"))
                .andExpect(jsonPath("$[0].content").value("Joga u subotu!"))
                .andExpect(jsonPath("$[0].imageUrl").value(SLIKA))
                .andExpect(jsonPath("$[0].postTimestamp").value("2026-11-22T10:30:00"))
                // ključ mora biti "isSaved" (ugovor), a ne "saved"
                .andExpect(jsonPath("$[0].isSaved").value(true))
                .andExpect(jsonPath("$[0].saved").doesNotExist())
                .andExpect(jsonPath("$[1].imageUrl").value(nullValue()))
                .andExpect(jsonPath("$[1].isSaved").value(false));
    }

    @Test
    void prazanFeedJePrazanNiz() throws Exception {
        mockMvc.perform(get(RUTA + "/feed").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------ 8.2–8.4 sačuvane objave

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"TRAINER", "WORKER", "ADMIN"})
    void samoClanCuvaObjave(Uloga uloga) throws Exception {
        mockMvc.perform(post(RUTA + "/31/save").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(RUTA + "/saved").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(RUTA + "/31/save").header(HttpHeaders.AUTHORIZATION, bearer(uloga)))
                .andExpect(status().isForbidden());

        verify(objaveService, never()).sacuvaj(anyInt(), anyInt());
        verify(objaveService, never()).ukloniIzSacuvanih(anyInt(), anyInt());
    }

    @Test
    void sacuvaneBezTokenaVraca401() throws Exception {
        mockMvc.perform(post(RUTA + "/31/save")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RUTA + "/saved")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(RUTA + "/31/save")).andExpect(status().isUnauthorized());
    }

    @Test
    void cuvanjeVraca200IPrenosiClanaIzTokena() throws Exception {
        mockMvc.perform(post(RUTA + "/31/save").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Objava je sacuvana."));

        verify(objaveService).sacuvaj(31, idNaloga(Uloga.MEMBER));
    }

    @Test
    void cuvanjeNepostojeceObjaveVraca404() throws Exception {
        when(objaveService.sacuvaj(anyInt(), anyInt())).thenThrow(new ResursNijePronadjenException("Objava ne postoji."));

        mockMvc.perform(post(RUTA + "/99/save").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Objava ne postoji."));
    }

    @Test
    void vecSacuvanaVraca409UFormatuGreske() throws Exception {
        when(objaveService.sacuvaj(anyInt(), anyInt())).thenThrow(new KonfliktException("Objava je vec sacuvana."));

        mockMvc.perform(post(RUTA + "/31/save").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Objava je vec sacuvana."));
    }

    @Test
    void cuvanjeSaIdomKojiNijeBrojVraca400() throws Exception {
        mockMvc.perform(post(RUTA + "/abc/save").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sacuvaneUFormatuIzUgovora() throws Exception {
        when(objaveService.sacuvane(idNaloga(Uloga.MEMBER))).thenReturn(List.of(new ObjavaDto(
                31, 3, "Marko Petrović", "Novi trening!", SLIKA, LocalDateTime.of(2026, 11, 22, 9, 15, 7), true)));

        mockMvc.perform(get(RUTA + "/saved").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postId").value(31))
                .andExpect(jsonPath("$[0].trainerId").value(3))
                .andExpect(jsonPath("$[0].trainerName").value("Marko Petrović"))
                .andExpect(jsonPath("$[0].imageUrl").value(SLIKA))
                .andExpect(jsonPath("$[0].postTimestamp").value("2026-11-22T09:15:07"))
                .andExpect(jsonPath("$[0].isSaved").value(true));
    }

    @Test
    void bezSacuvanihPrazanNiz() throws Exception {
        mockMvc.perform(get(RUTA + "/saved").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void uklanjanjeVraca200IPrenosiClanaIzTokena() throws Exception {
        mockMvc.perform(delete(RUTA + "/31/save").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Objava je uklonjena iz sacuvanih."));

        verify(objaveService).ukloniIzSacuvanih(31, idNaloga(Uloga.MEMBER));
    }

    @Test
    void uklanjanjeObjaveKojaNijeSacuvanaVraca404() throws Exception {
        when(objaveService.ukloniIzSacuvanih(anyInt(), anyInt()))
                .thenThrow(new ResursNijePronadjenException("Objava nije u sacuvanim."));

        mockMvc.perform(delete(RUTA + "/31/save").header(HttpHeaders.AUTHORIZATION, bearer(Uloga.MEMBER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Objava nije u sacuvanim."));
    }
}
