package org.etf.pumpd.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Slike objava na disku servera (folder pumpd.slike.folder). Adresa slike je /api/images/<ime>,
 * a ime je uvijek UUID sa nastavkom jpg, png ili webp koje je napravio server. Sve ostalo se odbija,
 * pa se preko adrese ne može doći do fajla van foldera (npr. "../").
 */
@Service
public class SlikeService {

	public static final String PREFIKS = "/api/images/";

	private static final Logger log = LoggerFactory.getLogger(SlikeService.class);
	private static final Pattern IME = Pattern.compile(
			"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

	private final Path folder;

	public SlikeService(@Value("${pumpd.slike.folder}") String folder) {
		this.folder = Path.of(folder).toAbsolutePath().normalize();
	}

	/** Dozvoljeni formati, prepoznati po prvim bajtovima fajla (ne po imenu ni Content-Type, koje šalje klijent). */
	enum Format {
		JPG(MediaType.IMAGE_JPEG),
		PNG(MediaType.IMAGE_PNG),
		WEBP(MediaType.parseMediaType("image/webp"));

		final MediaType tip;

		Format(MediaType tip) {
			this.tip = tip;
		}

		String nastavak() {
			return name().toLowerCase();
		}

		static Optional<Format> prepoznaj(byte[] b) {
			if (pocinje(b, 0, 0xFF, 0xD8, 0xFF)) {
				return Optional.of(JPG);
			}
			if (pocinje(b, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
				return Optional.of(PNG);
			}
			if (pocinje(b, 0, 'R', 'I', 'F', 'F') && pocinje(b, 8, 'W', 'E', 'B', 'P')) {
				return Optional.of(WEBP);
			}
			return Optional.empty();
		}

		static Optional<Format> poNastavku(String ime) {
			String nastavak = ime.substring(ime.lastIndexOf('.') + 1);
			return Arrays.stream(values()).filter(f -> f.nastavak().equals(nastavak)).findFirst();
		}

		private static boolean pocinje(byte[] b, int od, int... ocekivano) {
			if (b.length < od + ocekivano.length) {
				return false;
			}
			for (int i = 0; i < ocekivano.length; i++) {
				if ((b[od + i] & 0xFF) != ocekivano[i]) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Slanje slike (POST /api/posts/images): provjera stvarnog formata, upis pod nasumičnim imenom
	 * (UUID + nastavak prema formatu) i vraćanje adrese /api/images/<ime>. Ime koje je poslao klijent
	 * se ne koristi. Veličinu (5 MB) provjerava Spring prije nego što zahtjev stigne ovdje.
	 */
	public String sacuvaj(MultipartFile slika) {
		byte[] bajtovi;
		try {
			bajtovi = slika.getBytes();
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slika nije mogla biti procitana.");
		}
		if (bajtovi.length == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slika je prazna.");
		}
		Format format = Format.prepoznaj(bajtovi).orElseThrow(() -> new ResponseStatusException(
				HttpStatus.BAD_REQUEST, "Dozvoljene su samo JPEG, PNG i WebP slike."));

		String ime = UUID.randomUUID() + "." + format.nastavak();
		try {
			Files.createDirectories(folder);
			Files.write(folder.resolve(ime), bajtovi, StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			log.error("Slika {} nije sacuvana u {}", ime, folder, e);
			throw new IllegalStateException("Slika nije sacuvana.", e);
		}
		return PREFIKS + ime;
	}

	/** Čitanje slike (GET /api/images/{ime}): Content-Type prema nastavku imena koje je dao server. */
	public MediaType tip(Path slika) {
		return Format.poNastavku(slika.getFileName().toString()).map(f -> f.tip)
				.orElse(MediaType.APPLICATION_OCTET_STREAM);
	}

	/** Putanja fajla za našu adresu slike; prazno ako adresa nije u ispravnom obliku. */
	public Optional<Path> putanja(String imageUrl) {
		if (imageUrl == null || !imageUrl.startsWith(PREFIKS)) {
			return Optional.empty();
		}
		return putanjaPoImenu(imageUrl.substring(PREFIKS.length()));
	}

	/** Putanja fajla za ime slike; prazno ako ime nije UUID sa dozvoljenim nastavkom. */
	public Optional<Path> putanjaPoImenu(String ime) {
		if (ime == null || !IME.matcher(ime).matches()) {
			return Optional.empty();
		}
		return Optional.of(folder.resolve(ime));
	}

	/** 8.6: da li adresa pokazuje na našu sliku koja postoji na disku. */
	public boolean postoji(String imageUrl) {
		return putanja(imageUrl).map(Files::isRegularFile).orElse(false);
	}

	/**
	 * 8.7: briše fajl slike tek kad se transakcija uspješno završi (ako brisanje iz baze ne uspije,
	 * slika ostaje). Van transakcije briše odmah. Greška pri brisanju fajla se samo zapisuje u log.
	 */
	public void obrisiPoslijeCommita(String imageUrl) {
		Optional<Path> putanja = putanja(imageUrl);
		if (putanja.isEmpty()) {
			return;
		}
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					obrisi(putanja.get());
				}
			});
		} else {
			obrisi(putanja.get());
		}
	}

	private void obrisi(Path fajl) {
		try {
			Files.deleteIfExists(fajl);
		} catch (IOException e) {
			log.warn("Slika {} nije obrisana: {}", fajl, e.getMessage());
		}
	}
}
