package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.etf.pumpd.dto.IzlazakOdgovor;
import org.etf.pumpd.dto.UlazakOdgovor;
import org.etf.pumpd.dto.ZauzetostDto;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Clanarina;
import org.etf.pumpd.entity.Dolazak;
import org.etf.pumpd.entity.Nalog;
import org.etf.pumpd.entity.Teretana;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ClanarinaRepository;
import org.etf.pumpd.repository.DolazakRepository;
import org.etf.pumpd.repository.TeretanaRepository;

/**
 * Evidencija dolaska i izlaska skeniranjem QR koda (11.8, 11.9) i zauzetost teretane (5.3).
 *
 * Član je "u teretani" ako ima boravak bez izlaska koji je počeo danas. Boravak iz ranijeg dana
 * kome nije evidentiran izlazak (član zaboravio skenirati) se ne računa: ne blokira novi ulazak
 * i ne ulazi u zauzetost. Poznato ograničenje: boravak preko ponoći se ne može zatvoriti izlaskom.
 */
@Service
public class DolazakService {

	private final ClanRepository clanRepository;
	private final ClanarinaRepository clanarinaRepository;
	private final DolazakRepository dolazakRepository;
	private final TeretanaRepository teretanaRepository;
	private final Clock clock;

	public DolazakService(ClanRepository clanRepository, ClanarinaRepository clanarinaRepository,
			DolazakRepository dolazakRepository, TeretanaRepository teretanaRepository, Clock clock) {
		this.clanRepository = clanRepository;
		this.clanarinaRepository = clanarinaRepository;
		this.dolazakRepository = dolazakRepository;
		this.teretanaRepository = teretanaRepository;
		this.clock = clock;
	}

	/**
	 * 11.8: član bez aktivne članarine dobija isAllowed = false i ulazak se ne upisuje.
	 * Nepoznat QR kod (ili obrisan član) daje 404, a član koji je već u teretani 409.
	 */
	@Transactional
	public UlazakOdgovor ulazak(String qrKod) {
		Clan clan = ucitajClanaPoQrKodu(qrKod);
		Integer idClana = idClana(clan);
		String ime = punoIme(clan);
		LocalDate danas = LocalDate.now(clock);

		Optional<Clanarina> clanarina = clanarinaRepository.findById(idClana);
		if (clanarina.isEmpty()) {
			return new UlazakOdgovor(idClana, ime, false, "Clan nema clanarinu.");
		}
		if (!clanarina.get().jeAktivna(danas)) {
			return new UlazakOdgovor(idClana, ime, false,
					"Clanarina je istekla " + clanarina.get().getDatumIsteka() + ".");
		}
		if (!otvoreniDanas(idClana).isEmpty()) {
			throw new KonfliktException("Clan je vec u teretani.");
		}

		dolazakRepository.save(new Dolazak(clan, sada()));
		return new UlazakOdgovor(idClana, ime, true, "Dolazak je evidentiran.");
	}

	/** 11.9: zatvara današnji otvoreni boravak. Izlazak bez ulaska daje 409. */
	@Transactional
	public IzlazakOdgovor izlazak(String qrKod) {
		Clan clan = ucitajClanaPoQrKodu(qrKod);

		List<Dolazak> otvoreni = otvoreniDanas(idClana(clan));
		if (otvoreni.isEmpty()) {
			throw new KonfliktException("Clan nema evidentiran ulazak danas.");
		}

		LocalDateTime vrijeme = sada();
		// najnoviji je prvi; ako ih je zbog istovremenog skeniranja više, zatvaraju se svi
		otvoreni.forEach(d -> d.evidentirajIzlazak(vrijeme));
		return new IzlazakOdgovor(idClana(clan), punoIme(clan), vrijeme, "Izlazak je evidentiran.");
	}

	/** 5.3: broj članova trenutno u teretani i kapacitet iz informacija o teretani. */
	@Transactional(readOnly = true)
	public ZauzetostDto zauzetost() {
		long prisutni = dolazakRepository.brojPrisutnihOd(pocetakDana());
		Integer kapacitet = teretanaRepository.findById(Teretana.ID).map(Teretana::getKapacitet).orElse(null);
		return new ZauzetostDto(prisutni, kapacitet);
	}

	private Clan ucitajClanaPoQrKodu(String qrKod) {
		return clanRepository.pronadjiAktivnogPoQrKodu(qrKod.trim())
				.orElseThrow(() -> new ResursNijePronadjenException("Clan sa ovim QR kodom ne postoji."));
	}

	private List<Dolazak> otvoreniDanas(Integer idClana) {
		return dolazakRepository.otvoreniOd(idClana, pocetakDana());
	}

	private LocalDateTime pocetakDana() {
		return LocalDate.now(clock).atStartOfDay();
	}

	/** Kolone su DATETIME bez dijelova sekunde, pa se vrijeme odmah zaokružuje na sekunde. */
	private LocalDateTime sada() {
		return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
	}

	/** Id člana je isti kao id naloga (@MapsId); čita se preko naloga jer je uvijek postavljen. */
	private static Integer idClana(Clan clan) {
		return clan.getNalog().getId();
	}

	private static String punoIme(Clan clan) {
		Nalog nalog = clan.getNalog();
		return nalog.getIme() + " " + nalog.getPrezime();
	}
}
