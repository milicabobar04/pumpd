package org.etf.pumpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;

import org.etf.pumpd.dto.TreningIzIstorijeDto;
import org.etf.pumpd.entity.ClanNaTerminu;
import org.etf.pumpd.entity.Dolazak;
import org.etf.pumpd.entity.Grupa;
import org.etf.pumpd.entity.Termin;
import org.etf.pumpd.enums.StatusPrijave;
import org.etf.pumpd.enums.VrstaTreninga;
import org.etf.pumpd.repository.ClanNaTerminuRepository;
import org.etf.pumpd.repository.DolazakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Jedinični test 2.4 (IstorijaTreningaService) sa lažnim repozitorijumima. */
class IstorijaTreningaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("Europe/Sarajevo");
    private static final LocalDateTime SADA = LocalDateTime.of(2026, 11, 27, 12, 0);
    private static final int ID_CLANA = 40;

    private ClanNaTerminuRepository prijavaRepository;
    private DolazakRepository dolazakRepository;
    private IstorijaTreningaService servis;

    @BeforeEach
    void pripremi() {
        prijavaRepository = mock(ClanNaTerminuRepository.class);
        dolazakRepository = mock(DolazakRepository.class);
        servis = new IstorijaTreningaService(prijavaRepository, dolazakRepository,
                Clock.fixed(SADA.atZone(ZONA).toInstant(), ZONA));
    }

    private static ClanNaTerminu prijava(int idTermina, String naziv, LocalDateTime pocetak, Integer trajanje) {
        Grupa grupa = mock(Grupa.class);
        when(grupa.getNaziv()).thenReturn(naziv);
        Termin termin = mock(Termin.class);
        when(termin.getId()).thenReturn(idTermina);
        when(termin.getGrupa()).thenReturn(grupa);
        when(termin.getVrijeme()).thenReturn(pocetak);
        when(termin.getTrajanjeMinuta()).thenReturn(trajanje);
        when(termin.getKraj()).thenAnswer(p -> pocetak.plusMinutes(trajanje));
        ClanNaTerminu prijava = mock(ClanNaTerminu.class);
        when(prijava.getTermin()).thenReturn(termin);
        return prijava;
    }

    /** Dolazak bez evidentiranog izlaska. */
    private static Dolazak dolazak(int id, LocalDateTime ulazak) {
        return dolazak(id, ulazak, null);
    }

    private static Dolazak dolazak(int id, LocalDateTime ulazak, LocalDateTime izlazak) {
        Dolazak dolazak = mock(Dolazak.class);
        when(dolazak.getId()).thenReturn(id);
        when(dolazak.getVrijemeUlaska()).thenReturn(ulazak);
        when(dolazak.getVrijemeIzlaska()).thenReturn(izlazak);
        return dolazak;
    }

    private List<Integer> idTreninga() {
        return servis.istorija(ID_CLANA).stream().map(TreningIzIstorijeDto::trainingId).toList();
    }

    private void podaci(List<ClanNaTerminu> prijave, List<Dolazak> dolasci) {
        when(prijavaRepository.zapocetiTerminiClana(anyInt(), any(), any())).thenReturn(prijave);
        when(dolazakRepository.zavrseniBoravci(anyInt(), any())).thenReturn(dolasci);
    }

    @Test
    void grupniISamostalniOdNajnovijeg() {
        List<ClanNaTerminu> prijave = List.of(prijava(7, "Joga", SADA.minusDays(3).withHour(18), 60));
        List<Dolazak> dolasci = List.of(dolazak(50, SADA.minusDays(5).withHour(9)), dolazak(51, SADA.minusDays(1).withHour(7)));
        podaci(prijave, dolasci);

        assertEquals(List.of(
                new TreningIzIstorijeDto(51, "Samostalni trening", LocalDate.of(2026, 11, 26), VrstaTreninga.SOLO),
                new TreningIzIstorijeDto(7, "Joga", LocalDate.of(2026, 11, 24), VrstaTreninga.GROUP),
                new TreningIzIstorijeDto(50, "Samostalni trening", LocalDate.of(2026, 11, 22), VrstaTreninga.SOLO)),
                servis.istorija(ID_CLANA));
        verify(prijavaRepository).zapocetiTerminiClana(ID_CLANA, EnumSet.of(StatusPrijave.PRIJAVLJEN, StatusPrijave.PRISUTAN), SADA);
        verify(dolazakRepository).zavrseniBoravci(ID_CLANA, SADA.toLocalDate().atStartOfDay());
    }

    @Test
    void jutarnjiSamostalniIVecernjiGrupniSuDvaTreninga() {
        LocalDate dan = SADA.toLocalDate().minusDays(2);
        List<ClanNaTerminu> prijave = List.of(prijava(7, "Pilates", dan.atTime(18, 0), 60));
        List<Dolazak> dolasci = List.of(
                dolazak(50, dan.atTime(7, 0), dan.atTime(8, 30)),      // jutro: samostalni trening
                dolazak(51, dan.atTime(17, 45), dan.atTime(19, 15)),   // došla na Pilates
                dolazak(52, dan.atTime(19, 30), dan.atTime(20, 30)));  // ponovo poslije Pilatesa
        podaci(prijave, dolasci);

        List<TreningIzIstorijeDto> istorija = servis.istorija(ID_CLANA);

        assertEquals(List.of(52, 7, 50), istorija.stream().map(TreningIzIstorijeDto::trainingId).toList());
        assertEquals(List.of(VrstaTreninga.SOLO, VrstaTreninga.GROUP, VrstaTreninga.SOLO),
                istorija.stream().map(TreningIzIstorijeDto::type).toList());
    }

    @Test
    void preklapanjeNaGranicamaTermina() {
        LocalDate dan = SADA.toLocalDate().minusDays(1);
        List<ClanNaTerminu> prijave = List.of(prijava(7, "Pilates", dan.atTime(18, 0), 60));
        List<Dolazak> dolasci = List.of(
                dolazak(50, dan.atTime(17, 0), dan.atTime(18, 0)),     // izašla tačno kad je počeo: ne preklapa se
                dolazak(51, dan.atTime(19, 0), dan.atTime(20, 0)),     // ušla tačno kad je završio: ne preklapa se
                dolazak(52, dan.atTime(18, 30), dan.atTime(18, 40)),   // kratko, unutar termina
                dolazak(53, dan.atTime(17, 30), dan.atTime(18, 1)));   // minut preklapanja
        podaci(prijave, dolasci);

        assertEquals(List.of(51, 7, 50), idTreninga());
    }

    @Test
    void zaboravljenIzlazakSeRacunaDoKrajaDanaUlaska() {
        LocalDate dan = SADA.toLocalDate().minusDays(3);
        List<ClanNaTerminu> prijave = List.of(prijava(7, "Pilates", dan.atTime(18, 0), 60));
        List<Dolazak> dolasci = List.of(
                dolazak(50, dan.atTime(17, 45)),                 // bez izlaska, prije kraja termina: došla na Pilates
                dolazak(51, dan.atTime(19, 30)),                 // bez izlaska, poslije Pilatesa: samostalni
                dolazak(52, dan.minusDays(1).atTime(18, 30)));   // bez izlaska, dan ranije: ne preklapa se
        podaci(prijave, dolasci);

        assertEquals(List.of(51, 7, 52), idTreninga());
    }

    @Test
    void dolazakPrekoPonociSePorediSaTerminomSljedecegDana() {
        LocalDate dan = SADA.toLocalDate().minusDays(4);
        List<ClanNaTerminu> prijave = List.of(prijava(7, "Rano jutro", dan.plusDays(1).atTime(0, 30), 60));
        List<Dolazak> dolasci = List.of(dolazak(50, dan.atTime(23, 0), dan.plusDays(1).atTime(1, 0)));
        podaci(prijave, dolasci);

        assertEquals(List.of(7), idTreninga());
    }

    @Test
    void dolazakNaDanGrupnogTreningaSeNeBrojiDvaput() {
        LocalDateTime dan = SADA.minusDays(2);
        List<ClanNaTerminu> prijave = List.of(prijava(7, "Joga", dan.withHour(18), 60));
        List<Dolazak> dolasci = List.of(dolazak(50, dan.withHour(17).withMinute(45)), dolazak(51, dan.minusDays(1).withHour(17)));
        podaci(prijave, dolasci);

        List<TreningIzIstorijeDto> istorija = servis.istorija(ID_CLANA);

        assertEquals(List.of(7, 51), istorija.stream().map(TreningIzIstorijeDto::trainingId).toList());
        assertEquals(List.of(VrstaTreninga.GROUP, VrstaTreninga.SOLO), istorija.stream().map(TreningIzIstorijeDto::type).toList());
    }

    @Test
    void terminKojiJosTrajeNijeUIstoriji() {
        List<ClanNaTerminu> prijave = List.of(
                prijava(1, "Traje", SADA.minusMinutes(30), 60),
                prijava(2, "Upravo zavrsio", SADA.minusMinutes(60), 60),
                prijava(3, "Bez trajanja", SADA.minusMinutes(1), null));
        podaci(prijave, List.of());

        assertEquals(List.of(3, 2), servis.istorija(ID_CLANA).stream().map(TreningIzIstorijeDto::trainingId).toList());
    }

    @Test
    void terminKojiJosTrajeNeSkrivaDolazakTogDana() {
        // trening koji još traje nije u istoriji, pa ni ne skriva jutrošnji završeni dolazak
        List<ClanNaTerminu> prijave = List.of(prijava(1, "Traje", SADA.minusMinutes(30), 60));
        List<Dolazak> dolasci = List.of(dolazak(50, SADA.withHour(7)));
        podaci(prijave, dolasci);

        assertEquals(List.of(new TreningIzIstorijeDto(50, "Samostalni trening", SADA.toLocalDate(), VrstaTreninga.SOLO)),
                servis.istorija(ID_CLANA));
    }

    @Test
    void istoVrijemeSortiraPoIdStalno() {
        LocalDateTime isto = SADA.minusDays(4).withHour(10);
        List<Dolazak> dolasci = List.of(dolazak(50, isto), dolazak(52, isto), dolazak(51, isto));
        podaci(List.of(), dolasci);

        assertEquals(List.of(52, 51, 50), servis.istorija(ID_CLANA).stream().map(TreningIzIstorijeDto::trainingId).toList());
    }

    @Test
    void praznaIstorija() {
        podaci(List.of(), List.of());

        assertEquals(List.of(), servis.istorija(ID_CLANA));
    }
}
