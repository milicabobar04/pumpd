package org.etf.pumpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.etf.pumpd.dto.DodavanjeRadnikaOdgovor;
import org.etf.pumpd.dto.DodavanjeRadnikaZahtjev;
import org.etf.pumpd.dto.RadnikDto;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Radnik;
import org.etf.pumpd.entity.Zaposleni;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.NalogRepository;
import org.etf.pumpd.repository.RadnikRepository;
import org.etf.pumpd.repository.ZaposleniRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminRadniciServiceTest {

    private RadnikRepository radnikRepository;
    private ZaposleniRepository zaposleniRepository;
    private NalogRepository nalogRepository;
    private AdminRadniciService servis;

    @BeforeEach
    void postavi() {
        radnikRepository = mock(RadnikRepository.class);
        zaposleniRepository = mock(ZaposleniRepository.class);
        nalogRepository = mock(NalogRepository.class);
        // pravi NoviNalogService nad mock repozitorijumom, da test pokriva cijeli tok dodavanja radnika
        servis = new AdminRadniciService(radnikRepository, zaposleniRepository,
                new NoviNalogService(nalogRepository, new BCryptPasswordEncoder(4)));
    }

    private static Radnik radnik(int id, String ime, String prezime) {
        Nalog nalog = new Nalog(ime, prezime, ime.toLowerCase() + "@pumpd.local", "hash");
        nalog.setId(id);
        return new Radnik(new Zaposleni(nalog));
    }

    // ------------------------------------------------------------ 12.5 lista radnika

    @Test
    void listaVracaPodatkeIzNalogaRedomIzRepozitorijuma() {
        when(radnikRepository.sviSaNalogom()).thenReturn(List.of(
                radnik(12, "Emina", "Kurtović"), radnik(2, "Haris", "Zukić")));

        assertEquals(List.of(
                new RadnikDto(12, "Emina", "Kurtović", "emina@pumpd.local"),
                new RadnikDto(2, "Haris", "Zukić", "haris@pumpd.local")), servis.listaRadnika());
    }

    @Test
    void bezRadnikaVracaPraznuListu() {
        when(radnikRepository.sviSaNalogom()).thenReturn(List.of());

        assertTrue(servis.listaRadnika().isEmpty());
    }

    // ------------------------------------------------------------ 12.6 detalji radnika

    @Test
    void detaljiRadnika() {
        when(radnikRepository.pronadjiAktivnog(2)).thenReturn(Optional.of(radnik(2, "Emina", "Kurtović")));

        assertEquals(new RadnikDto(2, "Emina", "Kurtović", "emina@pumpd.local"), servis.detaljiRadnika(2));
    }

    @Test
    void nepostojeciRadnikBaca404() {
        when(radnikRepository.pronadjiAktivnog(99)).thenReturn(Optional.empty());

        ResursNijePronadjenException izuzetak =
                assertThrows(ResursNijePronadjenException.class, () -> servis.detaljiRadnika(99));
        assertEquals("Radnik ne postoji.", izuzetak.getMessage());
    }

    // ------------------------------------------------------------ 12.7 dodavanje radnika

    private static final DodavanjeRadnikaZahtjev ZAHTJEV =
            new DodavanjeRadnikaZahtjev("Haris", "Zukić", "haris@mail.com", "lozinka123");

    /** Baza dodijeli id 11 pri upisu naloga; save vraća isti objekat koji je dobio. */
    private void bazaUpisuje() {
        when(nalogRepository.existsByEmail(anyString())).thenReturn(false);
        when(nalogRepository.saveAndFlush(any(Nalog.class))).thenAnswer(poziv -> {
            Nalog n = poziv.getArgument(0);
            n.setId(11);
            return n;
        });
        when(zaposleniRepository.save(any(Zaposleni.class))).thenAnswer(poziv -> poziv.getArgument(0));
    }

    @Test
    void dodavanjeVracaIdNovogRadnika() {
        bazaUpisuje();

        DodavanjeRadnikaOdgovor odgovor = servis.dodajRadnika(ZAHTJEV);

        assertEquals(new DodavanjeRadnikaOdgovor(11, "Radnik je uspjesno dodan."), odgovor);
    }

    @Test
    void upisujeNalogZaposlenogIRadnikaTimRedom() {
        bazaUpisuje();

        servis.dodajRadnika(ZAHTJEV);

        ArgumentCaptor<Zaposleni> zaposleni = ArgumentCaptor.forClass(Zaposleni.class);
        ArgumentCaptor<Radnik> radnik = ArgumentCaptor.forClass(Radnik.class);
        InOrder redoslijed = inOrder(nalogRepository, zaposleniRepository, radnikRepository);
        redoslijed.verify(nalogRepository).saveAndFlush(any(Nalog.class));
        redoslijed.verify(zaposleniRepository).save(zaposleni.capture());
        redoslijed.verify(radnikRepository).save(radnik.capture());

        assertEquals(11, zaposleni.getValue().getNalog().getId());
        assertEquals("Haris", zaposleni.getValue().getNalog().getIme());
        assertSame(zaposleni.getValue(), radnik.getValue().getZaposleni());
    }

    @Test
    void zauzetEmailBaca409IRadnikSeNeUpisuje() {
        when(nalogRepository.existsByEmail("haris@mail.com")).thenReturn(true);

        assertThrows(KonfliktException.class, () -> servis.dodajRadnika(ZAHTJEV));
        verify(zaposleniRepository, never()).save(any());
        verify(radnikRepository, never()).save(any());
    }

    // ------------------------------------------------------------ 12.8 uklanjanje radnika

    @Test
    void uklanjanjeDeaktiviraNalogIOslobadjaEmail() {
        Radnik emina = radnik(2, "Emina", "Kurtović");
        when(radnikRepository.pronadjiAktivnog(2)).thenReturn(Optional.of(emina));

        assertEquals("Radnik je uspjesno uklonjen.", servis.ukloniRadnika(2).message());

        Nalog nalog = emina.getZaposleni().getNalog();
        assertFalse(nalog.isAktivan());
        assertEquals("obrisan-2@pumpd.invalid", nalog.getEmail());
    }

    @Test
    void uklanjanjeNepostojecegIliVecObrisanogRadnikaBaca404() {
        when(radnikRepository.pronadjiAktivnog(99)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.ukloniRadnika(99));
    }
}
