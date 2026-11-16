package org.etf.pumpd.service;

import static org.etf.pumpd.service.TestPomocni.SAT;
import static org.etf.pumpd.service.TestPomocni.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.etf.pumpd.dto.PlanDetaljiDto;
import org.etf.pumpd.dto.PlanZahtjev;
import org.etf.pumpd.dto.VjezbaZahtjev;
import org.etf.pumpd.entity.PlanTreninga;
import org.etf.pumpd.entity.Vjezba;
import org.etf.pumpd.entity.VjezbaUTreningu;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.PlanTreningaRepository;
import org.etf.pumpd.repository.VjezbaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlanTreningaServiceTest {

    private static final LocalDate DANAS = LocalDate.of(2026, 10, 29);
    private static final int CLAN = 6;
    private static final int TRENER = 3;
    private static final int DRUGI = 99;

    private PlanTreningaRepository planRepository;
    private VjezbaRepository vjezbaRepository;
    private PlanTreningaService servis;
    private final AtomicInteger sljedeciId = new AtomicInteger(100);

    @BeforeEach
    void postavi() {
        planRepository = mock(PlanTreningaRepository.class);
        vjezbaRepository = mock(VjezbaRepository.class);
        servis = new PlanTreningaService(planRepository, vjezbaRepository, SAT);

        // baza dodjeljuje id pri čuvanju (IDENTITY)
        when(planRepository.save(any(PlanTreninga.class))).thenAnswer(p -> postaviId(p.getArgument(0), 10));
        when(vjezbaRepository.save(any(Vjezba.class)))
                .thenAnswer(v -> postaviId(v.getArgument(0), sljedeciId.incrementAndGet()));
    }

    private static <T> T postaviId(T entitet, int id) throws Exception {
        Field polje = entitet.getClass().getDeclaredField("id");
        polje.setAccessible(true);
        polje.set(entitet, id);
        return entitet;
    }

    private static VjezbaZahtjev nova(String naziv) {
        return new VjezbaZahtjev(null, naziv, 3, 12);
    }

    private static PlanZahtjev zahtjev(VjezbaZahtjev... vjezbe) {
        return new PlanZahtjev("Snaga", "Gornji dio tijela", 60, List.of(vjezbe));
    }

    /** Postojeći plan člana CLAN sa vježbama čiji su id-jevi 1, 2, ... */
    private PlanTreninga planClana(String... nazivi) throws Exception {
        PlanTreninga plan = postaviId(PlanTreninga.zaClana(CLAN, DANAS), 10);
        plan.postaviPodatke("Stari", null, 45);
        for (int i = 0; i < nazivi.length; i++) {
            Vjezba vjezba = postaviId(new Vjezba(nazivi[i]), i + 1);
            plan.getVjezbe().add(new VjezbaUTreningu(vjezba, plan, 3, 10));
        }
        when(planRepository.findById(10)).thenReturn(Optional.of(plan));
        return plan;
    }

    // ------------------------------------------------------------ 3.3 kreiranje

    @Test
    void clanKreiraSvojPlanSaVjezbama() {
        ArgumentCaptor<PlanTreninga> sacuvan = ArgumentCaptor.forClass(PlanTreninga.class);

        var odgovor = servis.kreirajPlan(CLAN, Uloga.MEMBER, zahtjev(nova(" Cucanj "), nova("Mrtvo dizanje")));

        assertEquals(10, odgovor.planId());
        verify(planRepository).save(sacuvan.capture());
        PlanTreninga plan = sacuvan.getValue();
        assertEquals(CLAN, plan.getIdClana());
        assertNull(plan.getIdTrenera());
        assertEquals(DANAS, plan.getDatumKreiranja());
        assertEquals(2, plan.getVjezbe().size());
        assertEquals("Cucanj", plan.getVjezbeUredom().get(0).getVjezba().getNaziv());
    }

    @Test
    void trenerKreiraPlanKaoVlasnikTrener() {
        ArgumentCaptor<PlanTreninga> sacuvan = ArgumentCaptor.forClass(PlanTreninga.class);

        servis.kreirajPlan(TRENER, Uloga.TRAINER, zahtjev(nova("Zgib")));

        verify(planRepository).save(sacuvan.capture());
        assertEquals(TRENER, sacuvan.getValue().getIdTrenera());
        assertNull(sacuvan.getValue().getIdClana());
    }

    @Test
    void prazanOpisSeCuvaKaoNull() {
        ArgumentCaptor<PlanTreninga> sacuvan = ArgumentCaptor.forClass(PlanTreninga.class);

        servis.kreirajPlan(CLAN, Uloga.MEMBER, new PlanZahtjev("Snaga", "   ", 60, List.of(nova("Zgib"))));

        verify(planRepository).save(sacuvan.capture());
        assertNull(sacuvan.getValue().getOpis());
    }

    @Test
    void novaVjezbaSaExerciseIdDaje400() {
        assertStatus(400, () -> servis.kreirajPlan(CLAN, Uloga.MEMBER,
                zahtjev(new VjezbaZahtjev(5, "Zgib", 3, 10))));
        verify(planRepository, never()).save(any());
    }

    @Test
    void radnikNeMozeKreiratiPlan() {
        assertStatus(403, () -> servis.kreirajPlan(CLAN, Uloga.WORKER, zahtjev(nova("Zgib"))));
    }

    // ------------------------------------------------------------ 3.1, 3.2 pregled i vlasništvo

    @Test
    void detaljiVracajuVjezbeURedoslijeduDodavanja() throws Exception {
        planClana("Cucanj", "Iskorak");

        PlanDetaljiDto detalji = servis.detaljiPlana(CLAN, 10);

        assertEquals(2, detalji.exercises().size());
        assertEquals(1, detalji.exercises().get(0).exerciseId());
        assertEquals("Iskorak", detalji.exercises().get(1).name());
    }

    @Test
    void nepostojeciPlanDaje404() {
        when(planRepository.findById(10)).thenReturn(Optional.empty());

        assertThrows(ResursNijePronadjenException.class, () -> servis.detaljiPlana(CLAN, 10));
        assertThrows(ResursNijePronadjenException.class, () -> servis.obrisiPlan(CLAN, 10));
    }

    @Test
    void tudjiPlanDaje403ZaSveOperacije() throws Exception {
        planClana("Cucanj");

        assertStatus(403, () -> servis.detaljiPlana(DRUGI, 10));
        assertStatus(403, () -> servis.izmijeniPlan(DRUGI, 10, zahtjev(nova("Zgib"))));
        assertStatus(403, () -> servis.obrisiPlan(DRUGI, 10));
        verify(planRepository, never()).delete(any());
    }

    // ------------------------------------------------------------ 3.4 izmjena

    @Test
    void izmjenaMijenjaZadrzaneDodajeNoveIBriseIzostavljene() throws Exception {
        PlanTreninga plan = planClana("Cucanj", "Iskorak", "Plank");

        servis.izmijeniPlan(CLAN, 10, new PlanZahtjev("Novi naziv", null, 50, List.of(
                new VjezbaZahtjev(1, "Cucanj sa sipkom", 5, 5),   // izmijenjena
                nova("Zgib"))));                                  // nova; 2 i 3 se brišu

        assertEquals("Novi naziv", plan.getNaziv());
        assertEquals(50, plan.getTrajanjeMinuta());
        List<VjezbaUTreningu> vjezbe = plan.getVjezbeUredom();
        assertEquals(2, vjezbe.size());
        assertEquals("Cucanj sa sipkom", vjezbe.get(0).getVjezba().getNaziv());
        assertEquals(5, vjezbe.get(0).getBrojSerija());
        assertEquals("Zgib", vjezbe.get(1).getVjezba().getNaziv());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Vjezba>> obrisane = ArgumentCaptor.forClass(List.class);
        verify(vjezbaRepository).deleteAll(obrisane.capture());
        assertEquals(List.of("Iskorak", "Plank"),
                obrisane.getValue().stream().map(Vjezba::getNaziv).sorted().toList());
    }

    @Test
    void exerciseIdKojiNePripadaPlanuDaje400INistaSeNeMijenja() throws Exception {
        PlanTreninga plan = planClana("Cucanj");

        assertStatus(400, () -> servis.izmijeniPlan(CLAN, 10, zahtjev(new VjezbaZahtjev(77, "Zgib", 3, 10))));
        assertEquals("Stari", plan.getNaziv());
        assertEquals(1, plan.getVjezbe().size());
    }

    @Test
    void ponovljenExerciseIdDaje400() throws Exception {
        planClana("Cucanj");

        assertStatus(400, () -> servis.izmijeniPlan(CLAN, 10, zahtjev(
                new VjezbaZahtjev(1, "Cucanj", 3, 10), new VjezbaZahtjev(1, "Cucanj", 4, 8))));
    }

    // ------------------------------------------------------------ 3.5 brisanje

    @Test
    void brisanjeBrisePlanIVjezbe() throws Exception {
        PlanTreninga plan = planClana("Cucanj", "Iskorak");

        servis.obrisiPlan(CLAN, 10);

        verify(planRepository).delete(plan);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Vjezba>> obrisane = ArgumentCaptor.forClass(List.class);
        verify(vjezbaRepository).deleteAll(obrisane.capture());
        assertEquals(2, obrisane.getValue().size());
    }

    @Test
    void listaVracaSamoPlanoveKorisnika() throws Exception {
        PlanTreninga plan = postaviId(PlanTreninga.zaClana(CLAN, DANAS), 10);
        plan.postaviPodatke("Snaga", null, 60);
        when(planRepository.planoviKorisnika(CLAN)).thenReturn(List.of(plan));

        var lista = servis.listaPlanova(CLAN);

        assertEquals(1, lista.size());
        assertEquals("Snaga", lista.get(0).name());
        assertTrue(servis.listaPlanova(DRUGI).isEmpty());
    }
}
