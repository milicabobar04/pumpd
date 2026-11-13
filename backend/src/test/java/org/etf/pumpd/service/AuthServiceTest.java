package org.etf.pumpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.etf.pumpd.dto.PrijavaOdgovor;
import org.etf.pumpd.dto.PrijavaZahtjev;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.NeispravnaPrijavaException;
import org.etf.pumpd.security.JwtPodaci;
import org.etf.pumpd.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private static final String TAJNA = "CgQjUrqARTGnaq7/JWnCUsnbfPkUn66hTYqmmnGnzHQ=";
    private static final Instant SADA = Instant.parse("2026-11-09T10:00:00Z");

    private KorisnikService korisnikService;
    private NevazeciTokenService nevazeciTokenService;
    private JwtService jwtService;
    private AuthService servis;
    private Nalog nalog;

    @BeforeEach
    void postavi() {
        korisnikService = mock(KorisnikService.class);
        nevazeciTokenService = mock(NevazeciTokenService.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder(4);
        jwtService = new JwtService(TAJNA, Duration.ofHours(8), Clock.fixed(SADA, ZoneOffset.UTC));
        servis = new AuthService(korisnikService, encoder, jwtService, nevazeciTokenService);

        nalog = new Nalog("Marko", "Petrović", "trener1@pumpd.local", encoder.encode("lozinka123"));
        nalog.setId(3);
    }

    @Test
    void ispravnaPrijavaVracaPodatkeUloguIToken() {
        when(korisnikService.pronadjiPoEmailu("trener1@pumpd.local")).thenReturn(Optional.of(nalog));
        when(korisnikService.odrediUlogu(3)).thenReturn(Optional.of(Uloga.TRAINER));

        PrijavaOdgovor odgovor = servis.prijava(new PrijavaZahtjev("trener1@pumpd.local", "lozinka123"));

        assertEquals(3, odgovor.id());
        assertEquals("Marko", odgovor.name());
        assertEquals("Petrović", odgovor.surname());
        assertEquals("trener1@pumpd.local", odgovor.email());
        assertEquals(Uloga.TRAINER, odgovor.role());
        assertEquals(false, odgovor.mustChangePassword());

        JwtPodaci podaci = jwtService.procitaj(odgovor.token()).orElseThrow();
        assertEquals(3, podaci.idNalog());
        assertEquals(SADA.plus(Duration.ofHours(8)), podaci.istice());
        assertFalse(podaci.jti().isBlank());
    }

    @Test
    void pogresnaLozinkaDaje401() {
        when(korisnikService.pronadjiPoEmailu("trener1@pumpd.local")).thenReturn(Optional.of(nalog));

        assertThrows(NeispravnaPrijavaException.class,
                () -> servis.prijava(new PrijavaZahtjev("trener1@pumpd.local", "pogresna")));
    }

    @Test
    void nepostojeciEmailDaje401() {
        when(korisnikService.pronadjiPoEmailu("nema@pumpd.local")).thenReturn(Optional.empty());

        assertThrows(NeispravnaPrijavaException.class,
                () -> servis.prijava(new PrijavaZahtjev("nema@pumpd.local", "lozinka123")));
    }

    @Test
    void nalogBezUlogeDaje401() {
        when(korisnikService.pronadjiPoEmailu("trener1@pumpd.local")).thenReturn(Optional.of(nalog));
        when(korisnikService.odrediUlogu(3)).thenReturn(Optional.empty());

        assertThrows(NeispravnaPrijavaException.class,
                () -> servis.prijava(new PrijavaZahtjev("trener1@pumpd.local", "lozinka123")));
    }

    @Test
    void odjavaUpisujeJtiIIstekUListuNevazecih() {
        Instant istice = SADA.plus(Duration.ofHours(8));
        servis.odjava(new JwtPodaci(3, "jti-1", istice));

        verify(nevazeciTokenService).ponisti("jti-1", istice);
    }

    @Test
    void tokenSaDrugimKljucemIliIstekaoNijeValidan() {
        String token = jwtService.generisiToken(3, Uloga.MEMBER);

        JwtService drugiKljuc = new JwtService("bm92aS1rbGp1Yy16YS10ZXN0LW5vdmkta2xqdWMtMzJi",
                Duration.ofHours(8), Clock.fixed(SADA, ZoneOffset.UTC));
        assertTrue(drugiKljuc.procitaj(token).isEmpty());

        JwtService kasnije = new JwtService(TAJNA, Duration.ofHours(8),
                Clock.fixed(SADA.plus(Duration.ofHours(9)), ZoneOffset.UTC));
        assertTrue(kasnije.procitaj(token).isEmpty());

        assertTrue(jwtService.procitaj("nije.jwt.token").isEmpty());
    }

    @Test
    void prijavaSaPrivremenomLozinkomTraziPromjenu() {
        nalog.postaviPrivremenuLozinku(new BCryptPasswordEncoder(4).encode("Privremena7k"));
        when(korisnikService.pronadjiPoEmailu("trener1@pumpd.local")).thenReturn(Optional.of(nalog));
        when(korisnikService.odrediUlogu(3)).thenReturn(Optional.of(Uloga.TRAINER));

        PrijavaOdgovor odgovor = servis.prijava(new PrijavaZahtjev("trener1@pumpd.local", "Privremena7k"));

        assertEquals(true, odgovor.mustChangePassword());
    }
}
