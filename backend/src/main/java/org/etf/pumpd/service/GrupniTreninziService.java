package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.TerminGrupnogTreningaDto;
import org.etf.pumpd.dto.ZahtjevZaPrijavuOdgovor;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.ClanNaTerminu;
import org.etf.pumpd.entity.ClanNaTerminuId;
import org.etf.pumpd.entity.ClanUGrupiId;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Termin;
import org.etf.pumpd.enums.StatusPrijave;
import org.etf.pumpd.enums.StatusPrijaveClana;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanNaTerminuRepository;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ClanUGrupiRepository;
import org.etf.pumpd.repository.TerminRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Članova strana grupnih treninga (9.x). */
@Service
public class GrupniTreninziService {

	static final String TERMIN_NE_POSTOJI = "Termin ne postoji.";
	static final String ZAHTJEV_VEC_POSLAT = "Zahtjev je vec poslat.";
	static final String VEC_PRIJAVLJEN = "Vec ste prijavljeni na termin.";
	static final String NIJE_PRIJAVLJEN = "Niste prijavljeni na termin niti imate zahtjev na cekanju.";
	/** Isti oblik vremena kao u obavještenjima B1 (10.7, 10.10). */
	private static final DateTimeFormatter FORMAT_VREMENA = DateTimeFormatter.ofPattern("dd.MM.yyyy. 'u' HH:mm");

	private final TerminRepository terminRepository;
	private final ClanUGrupiRepository clanUGrupiRepository;
	private final ClanNaTerminuRepository prijavaRepository;
	private final ClanRepository clanRepository;
	private final ObavjestenjeService obavjestenjeService;
	private final Clock clock;

	public GrupniTreninziService(TerminRepository terminRepository, ClanUGrupiRepository clanUGrupiRepository,
			ClanNaTerminuRepository prijavaRepository, ClanRepository clanRepository,
			ObavjestenjeService obavjestenjeService, Clock clock) {
		this.terminRepository = terminRepository;
		this.clanUGrupiRepository = clanUGrupiRepository;
		this.prijavaRepository = prijavaRepository;
		this.clanRepository = clanRepository;
		this.obavjestenjeService = obavjestenjeService;
		this.clock = clock;
	}

	/**
	 * 9.1: bez datuma termini od danas nadalje, sa datumom termini tog dana; opciono samo jednog
	 * trenera. idClana != null (član) dodaje enrollmentStatus, a za radnika (null) polje izostaje.
	 * Broj članova i statusi se čitaju sa po jednim upitom za sve termine.
	 */
	@Transactional(readOnly = true)
	public List<TerminGrupnogTreningaDto> pregled(Integer idClana, LocalDate datum, Integer idTrenera) {
		LocalDateTime od = (datum != null ? datum : LocalDate.now(clock)).atStartOfDay();
		LocalDateTime doVremena = datum != null ? datum.plusDays(1).atStartOfDay() : null;
		List<Termin> termini = terminRepository.pregledTermina(od, doVremena, idTrenera);
		if (termini.isEmpty()) {
			return List.of();
		}

		Map<Integer, Long> brojClanova = clanUGrupiRepository
				.brojClanovaPoGrupama(termini.stream().map(t -> t.getGrupa().getId()).distinct().toList())
				.stream()
				.collect(Collectors.toMap(red -> (Integer) red[0], red -> (Long) red[1]));
		Map<Integer, ClanNaTerminu> prijave = idClana == null ? Map.of()
				: prijavaRepository.prijaveClana(idClana, termini.stream().map(Termin::getId).toList())
						.stream()
						.collect(Collectors.toMap(p -> p.getId().getTerminId(), p -> p));

		return termini.stream().map(t -> {
			Nalog trener = t.getTrener().getNalog();
			StatusPrijaveClana status = idClana == null ? null
					: StatusPrijaveClana.iz(prijave.containsKey(t.getId()) ? prijave.get(t.getId()).getStatus() : null);
			return new TerminGrupnogTreningaDto(t.getId(), t.getGrupa().getId(), t.getGrupa().getNaziv(),
					t.getTrener().getId(), trener.getIme() + " " + trener.getPrezime(), t.getVrijeme(),
					t.getTrajanjeMinuta(), brojClanova.getOrDefault(t.getGrupa().getId(), 0L),
					t.getGrupa().getMaxClanova(), status);
		}).toList();
	}

	/**
	 * 9.2: zahtjev za prijavu (PENDING). Odbijen ili otkazan zahtjev se ponavlja u istom redu,
	 * pa requestId ostaje isti. Mjesto zauzimaju samo prihvaćeni članovi (clan_u_grupi), a kapacitet
	 * se ponovo provjerava pri prihvatanju (10.13). Trener dobija obavještenje.
	 */
	@Transactional
	public ZahtjevZaPrijavuOdgovor posaljiZahtjev(Integer idTermina, Integer idClana) {
		Termin termin = terminRepository.saGrupomITrenerom(idTermina)
				.filter(t -> t.getTrener().getNalog().isAktivan())
				.orElseThrow(() -> new ResursNijePronadjenException(TERMIN_NE_POSTOJI));
		if (!termin.getVrijeme().isAfter(LocalDateTime.now(clock))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Termin je vec poceo.");
		}
		LocalDate danas = LocalDate.now(clock);
		ClanNaTerminu prijava = prijavaRepository.findById(new ClanNaTerminuId(idTermina, idClana)).orElse(null);
		if (prijava != null) {
			switch (prijava.getStatus()) {
				case NA_CEKANJU -> throw new KonfliktException(ZAHTJEV_VEC_POSLAT);
				case PRIJAVLJEN, PRISUTAN -> throw new KonfliktException(VEC_PRIJAVLJEN);
				case ODBIJEN, OTKAZAO -> { }
			}
		}
		if (clanUGrupiRepository.brojClanova(termin.getGrupa().getId()) >= termin.getGrupa().getMaxClanova()) {
			throw new KonfliktException("Termin je popunjen.");
		}

		Clan clan = clanRepository.pronadjiAktivnog(idClana)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Potrebna je prijava."));
		if (prijava != null) {
			prijava.ponoviZahtjev(danas);
		} else {
			try {
				// save() kod složenog ključa radi merge, ali reda nema (provjereno iznad), pa je to INSERT;
				// istovremeni drugi zahtjev istog člana odbija primarni ključ u bazi
				prijava = prijavaRepository.saveAndFlush(new ClanNaTerminu(clan, termin, StatusPrijave.NA_CEKANJU, danas));
			} catch (DataIntegrityViolationException e) {
				throw new KonfliktException(ZAHTJEV_VEC_POSLAT);
			}
		}

		Nalog nalogClana = clan.getNalog();
		obavjestenjeService.posalji(termin.getTrener().getId(), idClana,
				nalogClana.getIme() + " " + nalogClana.getPrezime() + " je poslao zahtjev za prijavu na termin "
						+ opisTermina(termin) + ".");
		return new ZahtjevZaPrijavuOdgovor(prijava.getIdZahtjeva(), StatusPrijaveClana.PENDING,
				"Zahtjev za prijavu je poslat.");
	}

	/**
	 * 9.3: zahtjev na čekanju se povlači (briše), a prihvaćen član se odjavljuje sa termina i iz grupe,
	 * uz obavještenje treneru. Odjava nakon početka termina nije moguća, da se istorija ne mijenja.
	 * Termin obrisanog trenera ne smeta: član i tada može ukloniti svoju prijavu.
	 */
	@Transactional
	public PorukaDto odjava(Integer idTermina, Integer idClana) {
		ClanNaTerminu prijava = prijavaRepository.findById(new ClanNaTerminuId(idTermina, idClana))
				.filter(p -> p.getStatus() == StatusPrijave.NA_CEKANJU || p.getStatus().jePrihvacen())
				.orElseThrow(() -> new ResursNijePronadjenException(NIJE_PRIJAVLJEN));
		if (prijava.getStatus() == StatusPrijave.NA_CEKANJU) {
			prijavaRepository.delete(prijava);
			return new PorukaDto("Zahtjev za prijavu je povucen.");
		}

		Termin termin = terminRepository.saGrupomITrenerom(idTermina)
				.orElseThrow(() -> new ResursNijePronadjenException(NIJE_PRIJAVLJEN));
		if (!termin.getVrijeme().isAfter(LocalDateTime.now(clock))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Termin je vec poceo, odjava nije moguca.");
		}
		prijavaRepository.delete(prijava);
		clanUGrupiRepository.deleteById(new ClanUGrupiId(idClana, termin.getGrupa().getId()));

		if (termin.getTrener().getNalog().isAktivan()) {
			Nalog nalogClana = prijava.getClan().getNalog();
			obavjestenjeService.posalji(termin.getTrener().getId(), idClana,
					nalogClana.getIme() + " " + nalogClana.getPrezime() + " se odjavio sa termina "
							+ opisTermina(termin) + ".");
		}
		return new PorukaDto("Odjavljeni ste sa termina.");
	}

	private static String opisTermina(Termin termin) {
		return "\"" + termin.getGrupa().getNaziv() + "\" (" + FORMAT_VREMENA.format(termin.getVrijeme()) + ")";
	}
}
