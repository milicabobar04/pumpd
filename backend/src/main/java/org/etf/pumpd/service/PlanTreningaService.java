package org.etf.pumpd.service;

import org.etf.pumpd.dto.*;

import org.etf.pumpd.entity.*;
import org.etf.pumpd.enums.Uloga;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.PlanTreningaRepository;
import org.etf.pumpd.repository.VjezbaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;



@Service
public class PlanTreningaService {

	public PlanTreningaService(PlanTreningaRepository planRepository, VjezbaRepository vjezbaRepository, Clock clock) {
		super();
		this.planRepository = planRepository;
		this.vjezbaRepository = vjezbaRepository;
		this.clock = clock;
	}
	private final PlanTreningaRepository planRepository;
	private final VjezbaRepository vjezbaRepository;
	private final Clock clock;
	
	@Transactional(readOnly=true)
	public List<PlanListDto> listaPlanova(Integer idKorisnika)
	{
		return planRepository.planoviKorisnika(idKorisnika).stream().map(p->new PlanListDto(p.getId(),p.getNaziv(),p.getOpis(),p.getTrajanjeMinuta())).toList();
	}
	
	@Transactional(readOnly=true)
	public PlanDetaljiDto detaljiPlana(Integer idKorisnika,Integer planId)
	{
		PlanTreninga plan=ucitajSvoj(idKorisnika,planId);
		List<VjezbaDto> vjezbe=plan.getVjezbeUredom().stream().map(v->new VjezbaDto(v.getVjezba().getId(),v.getVjezba().getNaziv(),v.getBrojSerija(),v.getBrojPonavljanja())).toList();
		return new PlanDetaljiDto(plan.getId(),plan.getNaziv(),plan.getOpis(),plan.getTrajanjeMinuta(),vjezbe);
	}
	
	@Transactional
	public KreiranjePlanaOdgovor kreirajPlan(Integer idKorisnika, Uloga uloga, PlanZahtjev zahtjev) {
		if (zahtjev.exercises().stream().anyMatch(v -> v.exerciseId() != null)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Nova vjezba ne smije imati exerciseId.");
		}

		LocalDate danas = LocalDate.now(clock);
		PlanTreninga plan = switch (uloga) {
			case TRAINER -> PlanTreninga.zaTrenera(idKorisnika, danas);
			case MEMBER -> PlanTreninga.zaClana(idKorisnika, danas);
			default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Planove treninga prave samo clanovi i treneri.");
		};
		plan.postaviPodatke(zahtjev.name().trim(), opis(zahtjev.description()), zahtjev.durationMinutes());
		// plan prvo dobija id, jer ga stavke koriste u svom ključu
		planRepository.save(plan);
		zahtjev.exercises().forEach(v -> dodajVjezbu(plan, v));

		return new KreiranjePlanaOdgovor(plan.getId(), "Plan treninga je kreiran.");
	}
	
	@Transactional
	public PorukaDto izmijeniPlan(Integer idKorisnika, Integer planId, PlanZahtjev zahtjev) {
		PlanTreninga plan = ucitajSvoj(idKorisnika, planId);

		Map<Integer, VjezbaUTreningu> postojece = plan.getVjezbe().stream()
				.collect(Collectors.toMap(v -> v.getVjezba().getId(), Function.identity()));
		Set<Integer> zadrzane = new HashSet<>();
		for (VjezbaZahtjev v : zahtjev.exercises()) {
			if (v.exerciseId() == null) {
				continue;
			}
			if (!postojece.containsKey(v.exerciseId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Vjezba " + v.exerciseId() + " ne pripada ovom planu.");
			}
			if (!zadrzane.add(v.exerciseId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Vjezba " + v.exerciseId() + " se ponavlja u zahtjevu.");
			}
		}

		plan.postaviPodatke(zahtjev.name().trim(), opis(zahtjev.description()), zahtjev.durationMinutes());

		List<VjezbaUTreningu> uklonjene = postojece.values().stream()
				.filter(v -> !zadrzane.contains(v.getVjezba().getId()))
				.toList();
		plan.getVjezbe().removeAll(uklonjene);

		for (VjezbaZahtjev v : zahtjev.exercises()) {
			if (v.exerciseId() == null) {
				dodajVjezbu(plan, v);
			} else {
				postojece.get(v.exerciseId()).izmjeni(v.name().trim(), v.numSets(), v.numRepetitions());
			}
		}

		// stavke se brišu prije vježbi na koje pokazuju (strani ključ)
		planRepository.flush();
		vjezbaRepository.deleteAll(uklonjene.stream().map(VjezbaUTreningu::getVjezba).toList());
		return new PorukaDto("Plan treninga je izmijenjen.");
	}
	
	@Transactional
	public PorukaDto obrisiPlan(Integer idKorisnika, Integer planId) {
		PlanTreninga plan = ucitajSvoj(idKorisnika, planId);
		List<Vjezba> vjezbe = plan.getVjezbe().stream().map(VjezbaUTreningu::getVjezba).toList();

		planRepository.delete(plan);          // stavke plana se brišu kaskadno
		planRepository.flush();
		vjezbaRepository.deleteAll(vjezbe);
		return new PorukaDto("Plan treninga je obrisan.");
	}

	private PlanTreninga ucitajSvoj(Integer idKorisnika, Integer planId) {
		PlanTreninga plan = planRepository.findById(planId)
				.orElseThrow(() -> new ResursNijePronadjenException("Plan treninga ne postoji."));
		if (!plan.pripada(idKorisnika)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nemate pristup ovom planu.");
		}
		return plan;
	}

	private void dodajVjezbu(PlanTreninga plan, VjezbaZahtjev v) {
		Vjezba vjezba = vjezbaRepository.save(new Vjezba(v.name().trim()));
		plan.getVjezbe().add(new VjezbaUTreningu(vjezba, plan, v.numSets(), v.numRepetitions()));
	}

	
	private static String opis(String opis) {
		return opis == null || opis.isBlank() ? null : opis.trim();
	}
}
