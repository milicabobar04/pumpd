package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.etf.pumpd.dto.ClanDetaljiDto;
import org.etf.pumpd.dto.ClanListaDto;
import org.etf.pumpd.dto.DodavanjeClanaOdgovor;
import org.etf.pumpd.dto.DodavanjeClanaZahtjev;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Clanarina;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ClanarinaRepository;

/** Upravljanje članovima za zaposlene (radnik i administrator)*/
@Service
public class ClanoviService {

	private final ClanRepository clanRepository;
	private final ClanarinaRepository clanarinaRepository;
	private final NoviNalogService noviNalogService;
	private final Clock clock;

	public ClanoviService(ClanRepository clanRepository, ClanarinaRepository clanarinaRepository,
			NoviNalogService noviNalogService, Clock clock) {
		this.clanRepository = clanRepository;
		this.clanarinaRepository = clanarinaRepository;
		this.noviNalogService = noviNalogService;
		this.clock = clock;
	}

	/** 11.1: lista članova sa opcionom pretragom. */
	@Transactional(readOnly = true)
	public List<ClanListaDto> listaClanova(String search) {
		List<Clan> clanovi = clanRepository.pretrazi(ocistiPretragu(search));
		if (clanovi.isEmpty()) {
			return List.of();
		}

		List<Integer> idClanova = clanovi.stream().map(this::idClana).toList();
		Map<Integer, Clanarina> clanarine = clanarinaRepository.findAllById(idClanova).stream()
				.collect(Collectors.toMap(Clanarina::getId, Function.identity()));
		LocalDate danas = LocalDate.now(clock);

		return clanovi.stream()
				.map(clan -> new ClanListaDto(
						idClana(clan),
						clan.getNalog().getIme(),
						clan.getNalog().getPrezime(),
						clan.getNalog().getEmail(),
						imaAktivnuClanarinu(clanarine.get(idClana(clan)), danas)))
				.toList();
	}

	/** 11.2: detalji jednog člana. Id koji ne pripada članu (npr. treneru) daje 404. */
	@Transactional(readOnly = true)
	public ClanDetaljiDto detaljiClana(Integer memberId) {
		Clan clan = ucitajAktivnogClana(memberId);
		Nalog nalog = clan.getNalog();
		Clanarina clanarina = clanarinaRepository.findById(memberId).orElse(null);
		LocalDate danas = LocalDate.now(clock);

		return new ClanDetaljiDto(
				nalog.getId(),
				nalog.getIme(),
				nalog.getPrezime(),
				nalog.getEmail(),
				clan.getDatumRodjenja() == null ? null : clan.getDatumRodjenja().getYear(),
				clan.getTjelesnaTezina(),
				clan.getVisina(),
				imaAktivnuClanarinu(clanarina, danas),
				clanarina == null ? null : clanarina.getDatumIsteka());
	}

	/** 11.3: novi nalog sa ulogom MEMBER. Nalog i član se upisuju u istoj transakciji. */
	@Transactional
	public DodavanjeClanaOdgovor dodajClana(DodavanjeClanaZahtjev zahtjev) {
		provjeriGodinuRodjenja(zahtjev.birthYear());
		Nalog nalog = noviNalogService.kreirajNalog(zahtjev.name(), zahtjev.surname(), zahtjev.email(),
				zahtjev.password());

		LocalDate datumRodjenja = zahtjev.birthYear() == null ? null : LocalDate.of(zahtjev.birthYear(), 1, 1);
		clanRepository.save(new Clan(nalog, zahtjev.weight(), zahtjev.height(), null, datumRodjenja));

		return new DodavanjeClanaOdgovor(nalog.getId(), "Clan je uspjesno dodan.");
	}

	/**
	 * 11.4: meko brisanje člana (nalog.aktivan = false, email se oslobađa).
	 * Podaci vezani za člana (članarina, dolasci, recenzije...) ostaju zbog istorije.
	 * Nalog je učitan u ovoj transakciji, pa se izmjena upisuje pri njenom završetku.
	 */
	@Transactional
	public PorukaDto ukloniClana(Integer memberId) {
		ucitajAktivnogClana(memberId).getNalog().deaktiviraj();
		return new PorukaDto("Clan je uspjesno uklonjen.");
	}

	/** Obrisan (neaktivan) član ili id koji ne pripada članu daju 404. */
	private Clan ucitajAktivnogClana(Integer memberId) {
		return clanRepository.pronadjiAktivnog(memberId)
				.orElseThrow(() -> new ResursNijePronadjenException("Clan ne postoji."));
	}

	private void provjeriGodinuRodjenja(Integer godina) {
		int trenutnaGodina = Year.now(clock).getValue();
		if (godina != null && godina > trenutnaGodina) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Godina rodjenja ne moze biti poslije " + trenutnaGodina + ".");
		}
	}

	/** Id člana je isti kao id naloga (@MapsId); čita se preko naloga jer je uvijek postavljen. */
	private Integer idClana(Clan clan) {
		return clan.getNalog().getId();
	}

	private boolean imaAktivnuClanarinu(Clanarina clanarina, LocalDate danas) {
		return clanarina != null && clanarina.jeAktivna(danas);
	}

	/** Prazna pretraga (ili samo razmaci) znači "bez filtera". */
	private String ocistiPretragu(String search) {
		if (search == null) {
			return null;
		}
		String ocisceno = search.trim();
		return ocisceno.isEmpty() ? null : ocisceno;
	}
}
