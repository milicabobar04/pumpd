package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
import static org.etf.pumpd.service.TestPomocni.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.etf.pumpd.dto.PrijavaProblemaOdgovor;
import org.etf.pumpd.dto.ProblemDto;
import org.etf.pumpd.dto.PromjenaStatusaProblemaOdgovor;
import org.etf.pumpd.dto.PrijavaProblemaZahtjev;
import org.etf.pumpd.entity.Administrator;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Problem;
import org.etf.pumpd.enums.StatusProblema;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.AdministratorRepository;
import org.etf.pumpd.repository.NalogRepository;
import org.etf.pumpd.repository.ProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProblemServiceTest {

    private ProblemRepository problemRepository;
    private NalogRepository nalogRepository;
    private AdministratorRepository administratorRepository;
    private ProblemService servis;

    @BeforeEach
    void postavi() {
        problemRepository = mock(ProblemRepository.class);
        nalogRepository = mock(NalogRepository.class);
        administratorRepository = mock(AdministratorRepository.class);
        servis = new ProblemService(problemRepository, nalogRepository, administratorRepository, SAT);

        // baza dodjeljuje ID pri čuvanju
        when(problemRepository.save(any(Problem.class))).thenAnswer(poziv -> {
            Problem problem = poziv.getArgument(0);
            problem.setId(7);
            return problem;
        });
    }

    private Problem sacuvaniProblem() {
        ArgumentCaptor<Problem> hvatac = ArgumentCaptor.forClass(Problem.class);
        verify(problemRepository).save(hvatac.capture());
        return hvatac.getValue();
    }

    @Test
    void prijavljenKorisnikPravaPrijavuVezanuZaSvojNalog() {
        Nalog nalog = new Nalog("Amar", "Hodžić", "clan1@pumpd.local", "hash");
        nalog.setId(5);
        when(nalogRepository.findById(5)).thenReturn(Optional.of(nalog));

        PrijavaProblemaOdgovor odgovor = servis.prijavi(5, new PrijavaProblemaZahtjev("  Pokvaren aparat ", " Traka 3 ne radi "));

        assertEquals(7, odgovor.issueId());
        assertEquals(StatusProblema.UNPROCESSED, odgovor.status());
        assertEquals("Problem je uspjesno prijavljen", odgovor.message());

        Problem problem = sacuvaniProblem();
        assertEquals("Pokvaren aparat", problem.getNaslov());
        assertEquals("Traka 3 ne radi", problem.getOpis());
        assertEquals(StatusProblema.UNPROCESSED, problem.getStatus());
        assertEquals(LocalDateTime.of(2026, 10, 29, 10, 15), problem.getVrijeme());
        assertSame(nalog, problem.getPrijavio());
        assertNull(problem.getObradio());
    }

    @Test
    void neprijavljenKorisnikPravaAnonimnuPrijavu() {
        servis.prijavi(null, new PrijavaProblemaZahtjev("Naslov", "Opis"));

        assertNull(sacuvaniProblem().getPrijavio());
        verify(nalogRepository, never()).findById(any());
    }

    @Test
    void tokenNalogaKojiNePostojiDajeAnonimnuPrijavu() {
        when(nalogRepository.findById(99)).thenReturn(Optional.empty());

        servis.prijavi(99, new PrijavaProblemaZahtjev("Naslov", "Opis"));

        assertNull(sacuvaniProblem().getPrijavio());
    }

    @Test
    void praznNaslovIliOpisJeNeispravan() {
        assertStatus(400, () -> servis.prijavi(null, new PrijavaProblemaZahtjev(null, "Opis")));
        assertStatus(400, () -> servis.prijavi(null, new PrijavaProblemaZahtjev("   ", "Opis")));
        assertStatus(400, () -> servis.prijavi(null, new PrijavaProblemaZahtjev("Naslov", null)));
        assertStatus(400, () -> servis.prijavi(null, new PrijavaProblemaZahtjev("Naslov", "")));
        verify(problemRepository, never()).save(any());
    }

    @Test
    void preDugNaslovIliOpisJeNeispravan() {
        assertStatus(400, () -> servis.prijavi(null, new PrijavaProblemaZahtjev("a".repeat(151), "Opis")));
        assertStatus(400, () -> servis.prijavi(null, new PrijavaProblemaZahtjev("Naslov", "a".repeat(501))));
        verify(problemRepository, never()).save(any());
    }

    @Test
    void granicneDuzineSuDozvoljene() {
        servis.prijavi(null, new PrijavaProblemaZahtjev("a".repeat(150), "b".repeat(500)));

        Problem problem = sacuvaniProblem();
        assertEquals(150, problem.getNaslov().length());
        assertEquals(500, problem.getOpis().length());
    }

    // ------------------------------------------------------------ 4.2 lista problema

    private static Problem problem(int id, String naslov, StatusProblema status, LocalDateTime vrijeme) {
        Problem p = new Problem();
        p.setId(id);
        p.setNaslov(naslov);
        p.setOpis("Opis " + id);
        p.setStatus(status);
        p.setVrijeme(vrijeme);
        return p;
    }

    @Test
    void listaBezFilteraVracaSveUFormatuIzUgovora() {
        when(problemRepository.findAllByOrderByVrijemeDescIdDesc()).thenReturn(List.of(
                problem(2, "Nema tople vode", StatusProblema.IN_PROGRESS, LocalDateTime.of(2026, 10, 28, 23, 59)),
                problem(1, "Traka", StatusProblema.UNPROCESSED, LocalDateTime.of(2026, 10, 27, 8, 0))));

        List<ProblemDto> lista = servis.listaProblema(null);

        assertEquals(List.of(
                new ProblemDto(2, "Nema tople vode", "Opis 2", StatusProblema.IN_PROGRESS, LocalDate.of(2026, 10, 28)),
                new ProblemDto(1, "Traka", "Opis 1", StatusProblema.UNPROCESSED, LocalDate.of(2026, 10, 27))), lista);
        verify(problemRepository, never()).findByStatusOrderByVrijemeDescIdDesc(any());
    }

    @Test
    void listaSaFilteromPitaRepozitorijumZaTajStatus() {
        when(problemRepository.findByStatusOrderByVrijemeDescIdDesc(StatusProblema.PROCESSED)).thenReturn(List.of());

        assertTrue(servis.listaProblema(StatusProblema.PROCESSED).isEmpty());
        verify(problemRepository, never()).findAllByOrderByVrijemeDescIdDesc();
    }

    // ------------------------------------------------------------ 4.3 promjena statusa

    @Test
    void promjenaStatusaPostavljaStatusIAdministratora() {
        Problem p = problem(1, "Traka", StatusProblema.UNPROCESSED, LocalDateTime.of(2026, 10, 27, 8, 0));
        when(problemRepository.findById(1)).thenReturn(Optional.of(p));
        // postojeći problem već ima id; save ga ne mijenja (za razliku od nove prijave u postavi())
        when(problemRepository.save(any(Problem.class))).thenAnswer(poziv -> poziv.getArgument(0));
        Administrator admin = mock(Administrator.class);
        when(administratorRepository.getReferenceById(1)).thenReturn(admin);

        PromjenaStatusaProblemaOdgovor odgovor = servis.promijeniStatus(1, StatusProblema.IN_PROGRESS, 1);

        assertEquals(new PromjenaStatusaProblemaOdgovor(1, StatusProblema.IN_PROGRESS,
                "Status problema je promijenjen."), odgovor);
        Problem sacuvan = sacuvaniProblem();
        assertEquals(StatusProblema.IN_PROGRESS, sacuvan.getStatus());
        assertSame(admin, sacuvan.getObradio());
    }

    @Test
    void istiStatusNijeGreska() {
        Problem p = problem(1, "Traka", StatusProblema.PROCESSED, LocalDateTime.of(2026, 10, 27, 8, 0));
        when(problemRepository.findById(1)).thenReturn(Optional.of(p));

        assertEquals(StatusProblema.PROCESSED, servis.promijeniStatus(1, StatusProblema.PROCESSED, 1).status());
    }

    @Test
    void nepostojeciProblemBaca404() {
        when(problemRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class,
                () -> servis.promijeniStatus(99, StatusProblema.PROCESSED, 1));
        verify(problemRepository, never()).save(any());
    }
}
