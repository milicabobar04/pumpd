package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.etf.pumpd.dto.ObavjestenjeDto;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Obavjestenje;
import org.etf.pumpd.repository.NalogRepository;
import org.etf.pumpd.repository.ObavjestenjeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ObavjestenjeServiceTest {

    private ObavjestenjeRepository obavjestenjeRepository;
    private NalogRepository nalogRepository;
    private ObavjestenjeService servis;

    private Nalog primalac;
    private Nalog posiljalac;

    @BeforeEach
    void postavi() {
        obavjestenjeRepository = mock(ObavjestenjeRepository.class);
        nalogRepository = mock(NalogRepository.class);
        servis = new ObavjestenjeService(obavjestenjeRepository, nalogRepository, SAT);

        primalac = new Nalog("Amar", "Hodžić", "clan1@pumpd.local", "hash");
        primalac.setId(5);
        posiljalac = new Nalog("Marko", "Petrović", "trener1@pumpd.local", "hash");
        posiljalac.setId(3);
        when(nalogRepository.getReferenceById(5)).thenReturn(primalac);
        when(nalogRepository.getReferenceById(3)).thenReturn(posiljalac);
    }

    private List<Obavjestenje> sacuvana(int ocekivanoPuta) {
        ArgumentCaptor<Obavjestenje> hvatac = ArgumentCaptor.forClass(Obavjestenje.class);
        verify(obavjestenjeRepository, times(ocekivanoPuta)).save(hvatac.capture());
        return hvatac.getAllValues();
    }

    // ------------------------------------------------------------ slanje (za druge module)

    @Test
    void sistemskoObavjestenjeNemaPosiljaocaINepročitanoJe() {
        servis.posalji(5, "  Termin je otkazan.  ");

        Obavjestenje o = sacuvana(1).get(0);
        assertSame(primalac, o.getPrimalac());
        assertNull(o.getPosiljalac());
        assertEquals("Termin je otkazan.", o.getSadrzaj());
        assertEquals(LocalDateTime.of(2026, 10, 29, 10, 15), o.getDatumVrijeme());
        assertFalse(o.isProcitano());
    }

    @Test
    void obavjestenjeMozeImatiPosiljaoca() {
        servis.posalji(5, 3, "Trener je prihvatio zahtjev.");

        assertSame(posiljalac, sacuvana(1).get(0).getPosiljalac());
    }

    @Test
    void nedostajuciPrimalacIliSadrzajJeGreskaPozivaoca() {
        assertThrows(IllegalArgumentException.class, () -> servis.posalji(null, "Tekst"));
        assertThrows(IllegalArgumentException.class, () -> servis.posalji(5, null));
        assertThrows(IllegalArgumentException.class, () -> servis.posalji(5, "   "));
        verify(obavjestenjeRepository, never()).save(any());
    }

    @Test
    void predugSadrzajSeSkracujeNa1000Znakova() {
        servis.posalji(5, "x".repeat(1500));

        String sadrzaj = sacuvana(1).get(0).getSadrzaj();
        assertEquals(1000, sadrzaj.length());
        assertTrue(sadrzaj.endsWith("..."));
    }

    @Test
    void sadrzajOd1000ZnakovaSeNeMijenja() {
        servis.posalji(5, "x".repeat(1000));

        assertEquals("x".repeat(1000), sacuvana(1).get(0).getSadrzaj());
    }

    @Test
    void posaljiSvimaSaljeJednoObavjestenjePoPrimaocuBezDuplikata() {
        servis.posaljiSvima(List.of(5, 3, 5), "Termin je otkazan.");

        List<Obavjestenje> poslana = sacuvana(2);
        assertSame(primalac, poslana.get(0).getPrimalac());
        assertSame(posiljalac, poslana.get(1).getPrimalac());
    }

    // ------------------------------------------------------------ 6.1 pregled

    @Test
    void mojaObavjestenjaSePrevodeUOblikIzUgovora() {
        Obavjestenje novo = obavjestenje(2, "Novi zahtjev.", LocalDateTime.of(2026, 10, 29, 9, 0), false);
        Obavjestenje staro = obavjestenje(1, "Dobrodošli.", LocalDateTime.of(2026, 10, 26, 8, 30), true);
        // repozitorij već vraća od najnovijeg
        when(obavjestenjeRepository.findByPrimalacIdOrderByDatumVrijemeDescIdDesc(5))
                .thenReturn(List.of(novo, staro));

        List<ObavjestenjeDto> lista = servis.mojaObavjestenja(5);

        assertEquals(2, lista.size());
        assertEquals(new ObavjestenjeDto(2, "Novi zahtjev.", LocalDateTime.of(2026, 10, 29, 9, 0), false), lista.get(0));
        assertEquals(new ObavjestenjeDto(1, "Dobrodošli.", LocalDateTime.of(2026, 10, 26, 8, 30), true), lista.get(1));
    }

    @Test
    void bezObavjestenjaVracaPraznuListu() {
        when(obavjestenjeRepository.findByPrimalacIdOrderByDatumVrijemeDescIdDesc(5)).thenReturn(List.of());

        assertTrue(servis.mojaObavjestenja(5).isEmpty());
    }

    @Test
    void oznacavanjeKaoProcitanoVracaBrojPromijenjenih() {
        when(obavjestenjeRepository.oznaciSvaKaoProcitana(5)).thenReturn(3);

        assertEquals(3, servis.oznaciSveKaoProcitana(5));
    }

    private Obavjestenje obavjestenje(int id, String sadrzaj, LocalDateTime vrijeme, boolean procitano) {
        Obavjestenje o = new Obavjestenje();
        o.setId(id);
        o.setSadrzaj(sadrzaj);
        o.setDatumVrijeme(vrijeme);
        o.setProcitano(procitano);
        o.setPrimalac(primalac);
        return o;
    }
}
