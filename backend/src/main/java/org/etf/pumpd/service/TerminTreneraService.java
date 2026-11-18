package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

import org.etf.pumpd.dto.*;
import org.etf.pumpd.entity.*;
import org.etf.pumpd.enums.StatusPrijave;
import org.etf.pumpd.exception.*;
import org.etf.pumpd.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TerminTreneraService {

	
	public TerminTreneraService(TerminRepository terminRepository, GrupaRepository grupaRepository,
			ClanUGrupiRepository clanUGrupiRepository, ClanNaTerminuRepository prijavaRepository,
			TrenerRepository trenerRepository, ObavjestenjeService obavjestenjeService, Clock clock) {
		super();
		this.terminRepository = terminRepository;
		this.grupaRepository = grupaRepository;
		this.clanUGrupiRepository = clanUGrupiRepository;
		this.prijavaRepository = prijavaRepository;
		this.trenerRepository = trenerRepository;
		this.obavjestenjeService = obavjestenjeService;
		this.clock = clock;
	}
	private static final DateTimeFormatter FORMAT_VREMENA = DateTimeFormatter.ofPattern("dd.MM.yyyy. 'u' HH:mm");

	private final TerminRepository terminRepository;
	private final GrupaRepository grupaRepository;
	private final ClanUGrupiRepository clanUGrupiRepository;
	private final ClanNaTerminuRepository prijavaRepository;
	private final TrenerRepository trenerRepository;
	private final ObavjestenjeService obavjestenjeService;
	private final Clock clock;
	
	@Transactional(readOnly=true)
	public List<TerminRasporedDto> raspored(Integer idTrenera, LocalDate datum) {
		LocalDate dan = datum != null ? datum : LocalDate.now(clock);
		return terminRepository.terminiTreneraIzmedju(idTrenera, dan.atStartOfDay(), dan.plusDays(1).atStartOfDay())
				.stream()
				.map(t -> new TerminRasporedDto(t.getId(), t.getGrupa().getNaziv(), t.getVrijeme(),
						t.getTrajanjeMinuta(), clanUGrupiRepository.brojClanova(t.getGrupa().getId()),
						t.getGrupa().getMaxClanova()))
				.toList();
	}
	
	@Transactional
	public KreiranjeTerminaOdgovor kreiraj(Integer idTrenera, TerminZahtjev zahtjev) {
		LocalDateTime pocetak = pocetak(zahtjev);
		provjeriDaJeUBuducnosti(pocetak);
		provjeriPreklapanje(idTrenera, pocetak, zahtjev.durationMinutes(), null);

		Trener trener = trenerRepository.getReferenceById(idTrenera);
		Grupa grupa = grupaRepository.save(new Grupa(zahtjev.name().trim(), zahtjev.maxMembers(), trener));
		Termin termin = terminRepository.save(new Termin(pocetak, zahtjev.durationMinutes(), trener, grupa));

		return new KreiranjeTerminaOdgovor(termin.getId(), grupa.getId(), "Termin je dodat.");
	}
	
	@Transactional
	public PorukaDto izmijeni(Integer idTrenera, Integer sessionId, TerminZahtjev zahtjev) {
		Termin termin = ucitajSvoj(idTrenera, sessionId);
		if (termin.jePoceo(sada())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Termin koji je poceo ili prosao ne moze se mijenjati.");
		}
		LocalDateTime pocetak = pocetak(zahtjev);
		provjeriDaJeUBuducnosti(pocetak);
		provjeriPreklapanje(idTrenera, pocetak, zahtjev.durationMinutes(), termin.getId());

		
		Grupa grupa = grupaRepository.zakljucaj(termin.getGrupa().getId()).orElseThrow();
		long brojClanova = clanUGrupiRepository.brojClanova(grupa.getId());
		if (zahtjev.maxMembers() < brojClanova) {
			throw new KonfliktException("Kapacitet ne moze biti manji od broja prijavljenih clanova ("
					+ brojClanova + ").");
		}

		boolean pomjeren = !pocetak.equals(termin.getVrijeme())
				|| !zahtjev.durationMinutes().equals(termin.getTrajanjeMinuta());
		grupa.izmjeni(zahtjev.name().trim(), zahtjev.maxMembers());
		termin.izmijeni(pocetak, zahtjev.durationMinutes());

		if (pomjeren) {
			obavijesti(idTrenera, clanUGrupiRepository.idClanova(grupa.getId()),
					"Termin \"" + grupa.getNaziv() + "\" je pomjeren na " + FORMAT_VREMENA.format(pocetak)
							+ " (" + zahtjev.durationMinutes() + " min).");
		}
		return new PorukaDto("Termin je izmijenjen.");
	}

	
	@Transactional
	public PorukaDto obrisi(Integer idTrenera, Integer sessionId) {
		Termin termin = ucitajSvoj(idTrenera, sessionId);
		if (termin.jePoceo(sada())) {
			throw new KonfliktException("Termin koji je poceo ili prosao ne moze se obrisati.");
		}
		Grupa grupa = termin.getGrupa();

		
		Set<Integer> primaoci = new LinkedHashSet<>(clanUGrupiRepository.idClanova(grupa.getId()));
		primaoci.addAll(prijavaRepository.idClanova(termin.getId(), EnumSet.of(StatusPrijave.NA_CEKANJU)));
		obavijesti(idTrenera, primaoci, "Termin \"" + grupa.getNaziv() + "\" zakazan za "
				+ FORMAT_VREMENA.format(termin.getVrijeme()) + " je otkazan.");

		prijavaRepository.obrisiSveZaTermin(termin.getId());
		clanUGrupiRepository.obrisiSveIzGrupe(grupa.getId());
		terminRepository.delete(termin);
		grupaRepository.delete(grupa);
		return new PorukaDto("Termin je obrisan.");
	}
	
	private Termin ucitajSvoj(Integer idTrenera, Integer sessionId) {
		Termin termin = terminRepository.saGrupom(sessionId)
				.orElseThrow(() -> new ResursNijePronadjenException("Termin ne postoji."));
		if (!termin.pripadaTreneru(idTrenera)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nemate pristup ovom terminu.");
		}
		return termin;
	}

	
	private void provjeriDaJeUBuducnosti(LocalDateTime pocetak) {
		if (!pocetak.isAfter(sada())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Termin ne moze poceti u proslosti.");
		}
	}
	
	private void provjeriPreklapanje(Integer idTrenera, LocalDateTime pocetak, int trajanje, Integer izuzetiId) {
		List<Termin> kandidati = terminRepository.terminiTreneraIzmedju(idTrenera,
				pocetak.minusDays(1), pocetak.plusMinutes(trajanje));
		for (Termin t : kandidati) {
			if (!t.getId().equals(izuzetiId) && t.preklapaSe(pocetak, trajanje)) {
				throw new KonfliktException("Termin se preklapa sa terminom \"" + t.getGrupa().getNaziv()
						+ "\" (" + FORMAT_VREMENA.format(t.getVrijeme()) + ").");
			}
		}
	}
	
	private void obavijesti(Integer idTrenera, Collection<Integer> primaoci, String sadrzaj) {
		for (Integer idClana : new LinkedHashSet<>(primaoci)) {
			obavjestenjeService.posalji(idClana, idTrenera, sadrzaj);
		}
	}
	
	private static LocalDateTime pocetak(TerminZahtjev zahtjev) {
		return zahtjev.startTime().truncatedTo(ChronoUnit.SECONDS);
	}

	private LocalDateTime sada() {
		return LocalDateTime.now(clock);
	}
	
	
	
}
