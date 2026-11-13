package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.etf.pumpd.dto.PromjenaLozinkeZahtjev;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.repository.NalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class LozinkaServiceTest {

    private NalogRepository nalogRepository;
    private PasswordEncoder passwordEncoder;
    private LozinkaService servis;
    private Nalog nalog;

    @BeforeEach
    void postavi() {
        nalogRepository = mock(NalogRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        servis = new LozinkaService(nalogRepository, passwordEncoder);

        nalog = new Nalog("Ana", "Ilić", "ana@mail.com", null);
        nalog.setId(11);
        nalog.postaviPrivremenuLozinku(passwordEncoder.encode("Privremena7k"));
        when(nalogRepository.findById(11)).thenReturn(Optional.of(nalog));
    }

    @Test
    void uspjesnaPromjenaHesiraNovuLozinkuISkidaOznaku() {
        assertEquals("Lozinka je promijenjena.",
                servis.promijeniLozinku(11, new PromjenaLozinkeZahtjev("Privremena7k", "mojaNovaLozinka")).message());

        assertTrue(passwordEncoder.matches("mojaNovaLozinka", nalog.getLozinka()));
        assertFalse(passwordEncoder.matches("Privremena7k", nalog.getLozinka()));
        assertFalse(nalog.isMoraPromijenitiLozinku());
        verify(nalogRepository).save(nalog);
    }

    @Test
    void pogresnaStaraLozinkaBaca400INistaNeMijenja() {
        String stariHes = nalog.getLozinka();

        var izuzetak = assertStatus(400,
                () -> servis.promijeniLozinku(11, new PromjenaLozinkeZahtjev("pogresna", "mojaNovaLozinka")));

        assertEquals("Stara lozinka nije ispravna.", izuzetak.getReason());
        assertEquals(stariHes, nalog.getLozinka());
        assertTrue(nalog.isMoraPromijenitiLozinku());
        verify(nalogRepository, never()).save(any());
    }

    @Test
    void novaIstaKaoStaraBaca400() {
        var izuzetak = assertStatus(400,
                () -> servis.promijeniLozinku(11, new PromjenaLozinkeZahtjev("Privremena7k", "Privremena7k")));

        assertEquals("Nova lozinka mora biti razlicita od stare.", izuzetak.getReason());
        assertTrue(nalog.isMoraPromijenitiLozinku());
        verify(nalogRepository, never()).save(any());
    }
}
