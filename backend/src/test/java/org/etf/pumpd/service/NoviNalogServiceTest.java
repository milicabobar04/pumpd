package org.etf.pumpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.repository.NalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Zajednički dio dodavanja naloga (11.3, 12.3, 12.7). */
class NoviNalogServiceTest {

    private NalogRepository nalogRepository;
    private PasswordEncoder passwordEncoder;
    private NoviNalogService servis;

    @BeforeEach
    void postavi() {
        nalogRepository = mock(NalogRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(4); // mala cijena da test bude brz
        servis = new NoviNalogService(nalogRepository, passwordEncoder);
    }

    /** Baza dodijeli id 11 pri upisu naloga. */
    private void bazaDodjeljujeId() {
        when(nalogRepository.existsByEmail(anyString())).thenReturn(false);
        when(nalogRepository.saveAndFlush(any(Nalog.class))).thenAnswer(poziv -> {
            Nalog n = poziv.getArgument(0);
            n.setId(11);
            return n;
        });
    }

    @Test
    void vracaUpisaniNalogSaIdom() {
        bazaDodjeljujeId();

        Nalog nalog = servis.kreirajNalog("Ana", "Ilić", "ana@mail.com", "lozinka123");

        assertEquals(11, nalog.getId());
    }

    @Test
    void lozinkaSeCuvaHesirana() {
        bazaDodjeljujeId();

        Nalog nalog = servis.kreirajNalog("Ana", "Ilić", "ana@mail.com", "lozinka123");

        assertNotEquals("lozinka123", nalog.getLozinka());
        assertTrue(passwordEncoder.matches("lozinka123", nalog.getLozinka()));
    }

    @Test
    void imePrezimeIEmailSeCisteOdRazmaka() {
        bazaDodjeljujeId();

        Nalog nalog = servis.kreirajNalog(" Ana ", " Ilić ", " ana@mail.com ", "lozinka123");

        assertEquals("Ana", nalog.getIme());
        assertEquals("Ilić", nalog.getPrezime());
        assertEquals("ana@mail.com", nalog.getEmail());
        verify(nalogRepository).existsByEmail("ana@mail.com");
    }

    @Test
    void zauzetEmailBaca409INistaSeNeUpisuje() {
        when(nalogRepository.existsByEmail("ana@mail.com")).thenReturn(true);

        KonfliktException izuzetak = assertThrows(KonfliktException.class,
                () -> servis.kreirajNalog("Ana", "Ilić", "ana@mail.com", "lozinka123"));

        assertEquals("Email vec postoji.", izuzetak.getMessage());
        verify(nalogRepository, never()).saveAndFlush(any());
    }

    @Test
    void istovremeniUpisIstogEmailaBaca409() {
        when(nalogRepository.existsByEmail(anyString())).thenReturn(false);
        when(nalogRepository.saveAndFlush(any(Nalog.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'email_UNIQUE'"));

        KonfliktException izuzetak = assertThrows(KonfliktException.class,
                () -> servis.kreirajNalog("Ana", "Ilić", "ana@mail.com", "lozinka123"));

        assertEquals("Email vec postoji.", izuzetak.getMessage());
    }

    @Test
    void nalogSeUpisujeOdmahPrekoSaveAndFlush() {
        bazaDodjeljujeId();

        Nalog nalog = servis.kreirajNalog("Ana", "Ilić", "ana@mail.com", "lozinka123");

        verify(nalogRepository).saveAndFlush(nalog);
        verify(nalogRepository, never()).save(any());
    }
}
