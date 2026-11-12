package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.PratilacDto;
import org.etf.pumpd.dto.ZahtjevZaPracenjeDto;
import org.etf.pumpd.dto.ZahtjevZaPracenjeOdgovor;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Pracenje;
import org.etf.pumpd.entity.Trener;
import org.etf.pumpd.enums.OdlukaOZahtjevu;
import org.etf.pumpd.enums.StatusPracenja;
import org.etf.pumpd.enums.StatusPracenjaClana;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.PracenjeRepository;
import org.etf.pumpd.repository.TrenerRepository;

/**
 * Veza član–trener: zahtjev za praćenje i prekid (7.3, 7.4), a trenerova strana u 10.1–10.3.
 * Red u tabeli pracenje: na_cekanju (PENDING), aktivan (ACCEPTED), neaktivan (REJECTED).
 */
@Service
public class PracenjeService {

	static final String ZAHTJEV_VEC_POSLAT = "Zahtjev za pracenje je vec poslat.";
	static final String VEC_PRATITE = "Vec pratite ovog trenera.";

	private final PracenjeRepository pracenjeRepository;
	private final TrenerRepository trenerRepository;
	private final ClanRepository clanRepository;
	private final ObavjestenjeService obavjestenjeService;
	private final Clock clock;

	public PracenjeService(PracenjeRepository pracenjeRepository, TrenerRepository trenerRepository,
			ClanRepository clanRepository, ObavjestenjeService obavjestenjeService, Clock clock) {
		this.pracenjeRepository = pracenjeRepository;
		this.trenerRepository = trenerRepository;
		this.clanRepository = clanRepository;
		this.obavjestenjeService = obavjestenjeService;
		this.clock = clock;
	}

	/**
	 * 7.3: član šalje zahtjev aktivnom treneru. Odbijen zahtjev se može poslati ponovo
	 * (isti red se vraća na PENDING); zahtjev koji čeka ili postojeće praćenje daju 409.
	 * Trener dobija obavještenje.
	 */
	@Transactional
	public ZahtjevZaPracenjeOdgovor posaljiZahtjev(Integer trainerId, Integer idClana) {
		Trener trener = trenerRepository.pronadjiAktivnog(trainerId)
				.orElseThrow(() -> new ResursNijePronadjenException("Trener ne postoji."));
		Clan clan = clanRepository.pronadjiAktivnog(idClana)
				.orElseThrow(() -> new ResursNijePronadjenException("Clan ne postoji."));
		LocalDate danas = LocalDate.now(clock);

		Optional<Pracenje> postojece = pracenjeRepository.findByTrenerIdAndClanId(trainerId, idClana);
		Pracenje pracenje;
		if (postojece.isPresent()) {
			pracenje = postojece.get();
			switch (pracenje.getStatus()) {
				case PENDING -> throw new KonfliktException(ZAHTJEV_VEC_POSLAT);
				case ACCEPTED -> throw new KonfliktException(VEC_PRATITE);
				case REJECTED -> pracenje.posaljiPonovo(danas);
			}
		} else {
			try {
				// saveAndFlush odmah šalje INSERT, pa primarni ključ (trener, član) kod dvostrukog klika puca ovdje
				pracenje = pracenjeRepository.saveAndFlush(new Pracenje(trener, clan, danas, StatusPracenja.PENDING));
			} catch (DataIntegrityViolationException e) {
				throw new KonfliktException(ZAHTJEV_VEC_POSLAT);
			}
		}

		Nalog nalogClana = clan.getNalog();
		obavjestenjeService.posalji(trainerId, idClana,
				nalogClana.getIme() + " " + nalogClana.getPrezime() + " vam je poslao zahtjev za praćenje.");

		return new ZahtjevZaPracenjeOdgovor(pracenje.getId(), StatusPracenjaClana.PENDING,
				"Zahtjev za pracenje je poslat.");
	}

	/**
	 * 7.4: prekid praćenja (ACCEPTED) ili povlačenje zahtjeva koji čeka (PENDING); oba brišu red.
	 * Odbijen zahtjev ili nepostojeći red daju 404 (odbijanje ostaje zapisano).
	 * Ne provjerava se da li je trener aktivan, da se praćenje obrisanog trenera može ukloniti.
	 */
	@Transactional
	public PorukaDto prekini(Integer trainerId, Integer idClana) {
		Pracenje pracenje = pracenjeRepository.findByTrenerIdAndClanId(trainerId, idClana)
				.filter(p -> p.getStatus() != StatusPracenja.REJECTED)
				.orElseThrow(() -> new ResursNijePronadjenException("Ne pratite ovog trenera."));

		String poruka = pracenje.getStatus() == StatusPracenja.PENDING
				? "Zahtjev za pracenje je povucen."
				: "Pracenje je prekinuto.";
		pracenjeRepository.delete(pracenje);
		return new PorukaDto(poruka);
	}

	// ------------------------------------------------------------ trenerova strana (10.1–10.3)

	/** 10.1: zahtjevi koji čekaju ovog trenera, najstariji prvi, bez obrisanih članova. */
	@Transactional(readOnly = true)
	public List<ZahtjevZaPracenjeDto> zahtjeviZaTrenera(Integer idTrenera) {
		return pracenjeRepository.zahtjeviTrenera(idTrenera, StatusPracenja.PENDING).stream()
				.map(p -> {
					Nalog n = p.getClan().getNalog();
					return new ZahtjevZaPracenjeDto(p.getId(), n.getId(), n.getIme() + " " + n.getPrezime(), p.getDatum());
				})
				.toList();
	}

	/**
	 * 10.2: trener prihvata ili odbija zahtjev upućen njemu. Zahtjev drugog trenera ili
	 * obrisanog člana daje 404 (kao da ne postoji); već obrađen zahtjev daje 409.
	 * Član dobija obavještenje o odluci.
	 */
	@Transactional
	public PorukaDto obradiZahtjev(Integer requestId, OdlukaOZahtjevu odluka, Integer idTrenera) {
		Pracenje pracenje = pracenjeRepository.findById(requestId)
				.filter(p -> p.getTrener().getNalog().getId().equals(idTrenera))
				.filter(p -> p.getClan().getNalog().isAktivan())
				.orElseThrow(() -> new ResursNijePronadjenException("Zahtjev ne postoji."));
		if (pracenje.getStatus() != StatusPracenja.PENDING) {
			throw new KonfliktException("Zahtjev je vec obradjen.");
		}

		Nalog trener = pracenje.getTrener().getNalog();
		String imeTrenera = trener.getIme() + " " + trener.getPrezime();
		Integer idClana = pracenje.getClan().getNalog().getId();

		if (odluka == OdlukaOZahtjevu.ACCEPTED) {
			pracenje.prihvati();
			obavjestenjeService.posalji(idClana, idTrenera, imeTrenera + " je prihvatio vaš zahtjev za praćenje.");
			return new PorukaDto("Zahtjev je prihvacen.");
		}
		pracenje.odbij();
		obavjestenjeService.posalji(idClana, idTrenera, imeTrenera + " je odbio vaš zahtjev za praćenje.");
		return new PorukaDto("Zahtjev je odbijen.");
	}

	/** 10.3: članovi koji prate ovog trenera (prihvaćeni), bez obrisanih, po prezimenu pa imenu. */
	@Transactional(readOnly = true)
	public List<PratilacDto> pratioci(Integer idTrenera) {
		return pracenjeRepository.pratiociTrenera(idTrenera, StatusPracenja.ACCEPTED).stream()
				.map(p -> p.getClan().getNalog())
				.map(n -> new PratilacDto(n.getId(), n.getIme(), n.getPrezime()))
				.toList();
	}
}
