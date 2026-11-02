package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
import static org.etf.pumpd.service.TestPomocni.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.etf.pumpd.dto.PrijavaProblemaOdgovor;
import org.etf.pumpd.dto.PrijavaProblemaZahtjev;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Problem;
import org.etf.pumpd.enums.StatusProblema;
import org.etf.pumpd.repository.NalogRepository;
import org.etf.pumpd.repository.ProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProblemServiceTest {

    private ProblemRepository problemRepository;
    private NalogRepository nalogRepository;
    private ProblemService servis;

    @BeforeEach
    void postavi() {
        problemRepository = mock(ProblemRepository.class);
        nalogRepository = mock(NalogRepository.class);
        servis = new ProblemService(problemRepository, nalogRepository, SAT);

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
}
