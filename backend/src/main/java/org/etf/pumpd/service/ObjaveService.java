package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.etf.pumpd.dto.MojaObjavaDto;
import org.etf.pumpd.dto.NovaObjavaOdgovor;
import org.etf.pumpd.dto.ObjavaDto;
import org.etf.pumpd.dto.NovaObjavaZahtjev;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Objava;
import org.etf.pumpd.entity.SacuvanaObjava;
import org.etf.pumpd.entity.SacuvanaObjavaId;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.enums.StatusPracenja;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ObjavaRepository;
import org.etf.pumpd.repository.SacuvanaObjavaRepository;
import org.etf.pumpd.repository.TrenerRepository;

/** Objave trenera (8.5–8.7); feed i sačuvane objave (8.1–8.4) dolaze u istom servisu. */
@Service
public class ObjaveService {

	static final String VEC_SACUVANA = "Objava je vec sacuvana.";

	private final ObjavaRepository objavaRepository;
	private final SacuvanaObjavaRepository sacuvanaObjavaRepository;
	private final TrenerRepository trenerRepository;
	private final ClanRepository clanRepository;
	private final SlikeService slikeService;
	private final Clock clock;

	public ObjaveService(ObjavaRepository objavaRepository, SacuvanaObjavaRepository sacuvanaObjavaRepository,
			TrenerRepository trenerRepository, ClanRepository clanRepository, SlikeService slikeService,
			Clock clock) {
		this.objavaRepository = objavaRepository;
		this.sacuvanaObjavaRepository = sacuvanaObjavaRepository;
		this.trenerRepository = trenerRepository;
		this.clanRepository = clanRepository;
		this.slikeService = slikeService;
		this.clock = clock;
	}

	/**
	 * 8.6: nova objava prijavljenog trenera. Vrijeme postavlja server, na sekunde (oblik iz ugovora).
	 * Slika, ako je poslata, mora biti naša slika koja postoji (prethodno poslata na /api/posts/images).
	 */
	@Transactional
	public NovaObjavaOdgovor dodajObjavu(Integer idTrenera, NovaObjavaZahtjev zahtjev) {
		Trener trener = trenerRepository.pronadjiAktivnog(idTrenera)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nalog vise ne postoji."));

		String slika = ocistiSliku(zahtjev.imageUrl());
		if (slika != null && !slikeService.postoji(slika)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Slika ne postoji. Sliku prvo treba poslati na /api/posts/images.");
		}

		LocalDateTime vrijeme = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
		Objava objava = objavaRepository.save(new Objava(trener, zahtjev.content().trim(), slika, vrijeme));
		return new NovaObjavaOdgovor(objava.getId(), "Objava je dodata.");
	}

	/** 8.5: objave prijavljenog trenera, najnovije prve. */
	@Transactional(readOnly = true)
	public List<MojaObjavaDto> mojeObjave(Integer idTrenera) {
		return objavaRepository.findByTrenerIdOrderByVrijemeDescIdDesc(idTrenera).stream()
				.map(o -> new MojaObjavaDto(o.getId(), o.getSadrzaj(), o.getSlikaPath(), o.getVrijeme()))
				.toList();
	}

	/**
	 * 8.7: trajno brisanje sopstvene objave, zajedno sa njenim redovima u sačuvanim objavama
	 * (kod svih članova) i fajlom slike. Tuđa objava daje 403 i ništa se ne briše.
	 */
	@Transactional
	public PorukaDto obrisiObjavu(Integer idObjave, Integer idTrenera) {
		Objava objava = objavaRepository.findById(idObjave)
				.orElseThrow(() -> new ResursNijePronadjenException("Objava ne postoji."));
		if (!objava.getTrener().getNalog().getId().equals(idTrenera)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Objava pripada drugom treneru.");
		}

		sacuvanaObjavaRepository.obrisiZaObjavu(idObjave);
		objavaRepository.delete(objava);
		slikeService.obrisiPoslijeCommita(objava.getSlikaPath());
		return new PorukaDto("Objava je obrisana.");
	}

	/**
	 * 8.1: objave aktivnih trenera koje član prati (prihvaćeno praćenje), najnovije prve.
	 * Dva upita bez obzira na broj objava: objave sa trenerima, pa koje je član sačuvao (isSaved).
	 */
	@Transactional(readOnly = true)
	public List<ObjavaDto> feed(Integer idClana) {
		List<Objava> objave = objavaRepository.feed(idClana, StatusPracenja.ACCEPTED);
		if (objave.isEmpty()) {
			return List.of();
		}
		Set<Integer> sacuvane = new HashSet<>(
				sacuvanaObjavaRepository.sacuvaneMedju(idClana, objave.stream().map(Objava::getId).toList()));
		return objave.stream().map(o -> uDto(o, sacuvane.contains(o.getId()))).toList();
	}

	/**
	 * 8.2: član čuva objavu aktivnog trenera (praćenje nije uslov). Već sačuvana objava daje 409:
	 * provjera prije upisa, a kod istovremenog dvostrukog klika primarni ključ u bazi (vidi SacuvanaObjava).
	 */
	@Transactional
	public PorukaDto sacuvaj(Integer idObjave, Integer idClana) {
		Objava objava = objavaRepository.findById(idObjave)
				.filter(o -> o.getTrener().getNalog().isAktivan())
				.orElseThrow(() -> new ResursNijePronadjenException("Objava ne postoji."));
		Clan clan = clanRepository.pronadjiAktivnog(idClana)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nalog vise ne postoji."));

		if (sacuvanaObjavaRepository.existsById(new SacuvanaObjavaId(idClana, idObjave))) {
			throw new KonfliktException(VEC_SACUVANA);
		}
		try {
			sacuvanaObjavaRepository.saveAndFlush(new SacuvanaObjava(clan, objava, LocalDate.now(clock)));
		} catch (DataIntegrityViolationException e) {
			throw new KonfliktException(VEC_SACUVANA);
		}
		return new PorukaDto("Objava je sacuvana.");
	}

	/** 8.3: sačuvane objave člana, od zadnje sačuvane; ostaju i nakon prekida praćenja. */
	@Transactional(readOnly = true)
	public List<ObjavaDto> sacuvane(Integer idClana) {
		return sacuvanaObjavaRepository.sacuvaneClana(idClana).stream()
				.map(s -> uDto(s.getObjava(), true))
				.toList();
	}

	/**
	 * 8.4: uklanjanje iz sačuvanih. Ne provjerava se da li je trener aktivan, da član može ukloniti
	 * objavu koju je upravo vidio na ekranu i kad je trener u međuvremenu obrisan.
	 */
	@Transactional
	public PorukaDto ukloniIzSacuvanih(Integer idObjave, Integer idClana) {
		SacuvanaObjava sacuvana = sacuvanaObjavaRepository.findById(new SacuvanaObjavaId(idClana, idObjave))
				.orElseThrow(() -> new ResursNijePronadjenException("Objava nije u sacuvanim."));
		sacuvanaObjavaRepository.delete(sacuvana);
		return new PorukaDto("Objava je uklonjena iz sacuvanih.");
	}

	/** Isti oblik objave koristi i 8.3 (sačuvane objave). */
	static ObjavaDto uDto(Objava o, boolean sacuvana) {
		Nalog trener = o.getTrener().getNalog();
		return new ObjavaDto(o.getId(), trener.getId(), trener.getIme() + " " + trener.getPrezime(),
				o.getSadrzaj(), o.getSlikaPath(), o.getVrijeme(), sacuvana);
	}

	/** Prazna adresa slike (ili samo razmaci) znači objavu bez slike. */
	private String ocistiSliku(String imageUrl) {
		if (imageUrl == null) {
			return null;
		}
		String ociscena = imageUrl.trim();
		return ociscena.isEmpty() ? null : ociscena;
	}
}
