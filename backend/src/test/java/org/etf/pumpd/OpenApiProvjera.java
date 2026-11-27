package org.etf.pumpd;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

/**
 * Usklađenost dokumentacije sa kodom: svaki odgovor koji backend vrati u integracionim testovima
 * (MockMvc) provjerava se prema docs/openapi.yaml: da li je statusni kod dokumentovan za tu rutu i
 * da li tijelo odgovara šemi (polja, tipovi, obavezna polja, enum vrijednosti).
 *
 * Uključena je za sve testove koji koriste MySqlTestKonfiguracija. Provjerava se samo odgovor, ne
 * zahtjev, jer testovi namjerno šalju i neispravne zahtjeve (provjera grešaka 400).
 *
 * Režim (sistemsko svojstvo openapi.provjera): "strogo" (podrazumijevano) obara test čim odgovor ne
 * odgovara dokumentaciji; "izvjestaj" ne obara testove, nego neslaganja upisuje u
 * target/openapi-neslaganja.txt (korisno da se vidi sve odjednom).
 *
 * Pokrivenost: na kraju testova u target/openapi-pokrivenost.txt piše, za svaku rutu iz
 * specifikacije, koji statusni kodovi su viđeni; ruta bez ijednog uspješnog (2xx) odgovora je označena.
 */
public final class OpenApiProvjera implements ResultHandler {

	private static final Path SPECIFIKACIJA = Path.of("docs/openapi.yaml");
	private static final Path IZVJESTAJ = Path.of("target/openapi-neslaganja.txt");
	private static final boolean STROGO = !"izvjestaj".equals(System.getProperty("openapi.provjera", "strogo"));

	private static final OpenApiInteractionValidator VALIDATOR = OpenApiInteractionValidator
			.createFor(bezNedokumentovanihPolja())
			.withBasePathOverride("/api")
			.build();

	/**
	 * Specifikacija u memoriji (fajl se ne mijenja) u kojoj objekti iz odgovora ne dozvoljavaju polja
	 * koja nisu navedena. OpenAPI ih inače podrazumijevano dozvoljava, pa polje koje backend vraća,
	 * a nije dokumentovano, ne bi bilo primijećeno.
	 */
	private static OpenAPI bezNedokumentovanihPolja() {
		ParseOptions opcije = new ParseOptions();
		opcije.setResolve(true);
		// i zajednički odgovori (#/components/responses/...) se raspakuju u svaku rutu
		opcije.setResolveFully(true);
		OpenAPI api = new OpenAPIV3Parser().read(SPECIFIKACIJA.toAbsolutePath().toUri().toString(), null, opcije);
		Set<Schema<?>> obradjene = Collections.newSetFromMap(new IdentityHashMap<>());
		if (api.getComponents() != null && api.getComponents().getSchemas() != null) {
			api.getComponents().getSchemas().values().forEach(sema -> zatvori(sema, obradjene));
		}
		api.getPaths().values().forEach(putanja -> putanja.readOperations().forEach(operacija ->
				operacija.getResponses().values().forEach(odgovor -> {
					if (odgovor.getContent() != null) {
						odgovor.getContent().values().forEach(tip -> zatvori(tip.getSchema(), obradjene));
					}
				})));
		return api;
	}

	private static void zatvori(Schema<?> sema, Set<Schema<?>> obradjene) {
		if (sema == null || !obradjene.add(sema)) {
			return;
		}
		if (sema.getProperties() != null && !sema.getProperties().isEmpty() && sema.getAdditionalProperties() == null) {
			sema.setAdditionalProperties(false);
		}
		if (sema.getProperties() != null) {
			sema.getProperties().values().forEach(polje -> zatvori(polje, obradjene));
		}
		zatvori(sema.getItems(), obradjene);
	}

	private static final Set<String> ZAPISANO = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private static final Path POKRIVENOST = Path.of("target/openapi-pokrivenost.txt");
	/** Rute iz specifikacije: "METODA /api/putanja" -> izraz za prepoznavanje stvarne adrese. */
	private static final Map<String, Pattern> RUTE = new TreeMap<>();
	private static final Map<String, Set<Integer>> VIDJENI_KODOVI = new ConcurrentHashMap<>();

	static {
		try {
			String yaml = Files.readString(SPECIFIKACIJA);
			Matcher red = Pattern.compile("(?m)^  (/\\S*):\\s*$|^    (get|post|put|patch|delete):\\s*$").matcher(yaml);
			String putanja = null;
			while (red.find()) {
				if (red.group(1) != null) {
					putanja = red.group(1);
				} else {
					String izraz = "/api" + putanja.replaceAll("\\{[^}]+}", "[^/]+");
					RUTE.put(red.group(2).toUpperCase() + " /api" + putanja, Pattern.compile(izraz));
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(OpenApiProvjera::zapisiPokrivenost));
	}

	private static void zabiljezi(String metoda, String adresa, int status) {
		RUTE.forEach((ruta, izraz) -> {
			if (ruta.startsWith(metoda + " ") && izraz.matcher(adresa).matches()) {
				VIDJENI_KODOVI.computeIfAbsent(ruta, r -> new ConcurrentSkipListSet<>()).add(status);
			}
		});
	}

	private static void zapisiPokrivenost() {
		StringBuilder tekst = new StringBuilder();
		long bezUspjeha = 0;
		for (String ruta : RUTE.keySet()) {
			Set<Integer> kodovi = VIDJENI_KODOVI.getOrDefault(ruta, Set.of());
			boolean uspjeh = kodovi.stream().anyMatch(k -> k >= 200 && k < 300);
			bezUspjeha += uspjeh ? 0 : 1;
			tekst.append(uspjeh ? "   " : "!! ").append(ruta).append("  ").append(kodovi).append('\n');
		}
		tekst.insert(0, "Ruta: " + RUTE.size() + ", bez uspješnog odgovora (!!): " + bezUspjeha + "\n\n");
		try {
			Files.createDirectories(POKRIVENOST.getParent());
			Files.writeString(POKRIVENOST, tekst);
		} catch (IOException e) {
			// pokrivenost je samo pregled; ne smije oboriti gašenje testova
		}
	}

	@Override
	public void handle(MvcResult rezultat) throws Exception {
		MockHttpServletRequest zahtjev = rezultat.getRequest();
		MockHttpServletResponse odgovor = rezultat.getResponse();
		if (!zahtjev.getRequestURI().startsWith("/api/")) {
			return;
		}
		zabiljezi(zahtjev.getMethod(), zahtjev.getRequestURI(), odgovor.getStatus());
		SimpleResponse.Builder tijelo = SimpleResponse.Builder.status(odgovor.getStatus());
		if (odgovor.getContentType() != null) {
			tijelo.withContentType(odgovor.getContentType());
		}
		// binarni odgovor (slika) se ne čita kao tekst; provjeravaju se status i Content-Type
		if (odgovor.getContentType() == null || !odgovor.getContentType().startsWith("image/")) {
			String tekst = odgovor.getContentAsString(StandardCharsets.UTF_8);
			if (!tekst.isEmpty()) {
				tijelo.withBody(tekst);
			}
		}

		ValidationReport izvjestaj = VALIDATOR.validateResponse(zahtjev.getRequestURI(),
				Request.Method.valueOf(zahtjev.getMethod()), tijelo.build());
		String poruke = izvjestaj.getMessages().stream()
				.filter(p -> p.getLevel() == ValidationReport.Level.ERROR)
				.filter(p -> !opravdano(p.getKey(), odgovor))
				.map(p -> "  [" + p.getKey() + "] " + p.getMessage().replace('\n', ' '))
				.collect(Collectors.joining("\n"));
		if (poruke.isEmpty()) {
			return;
		}
		String opis = zahtjev.getMethod() + " " + zahtjev.getRequestURI() + " -> " + odgovor.getStatus() + "\n" + poruke;
		if (STROGO) {
			throw new AssertionError("Odgovor ne odgovara docs/openapi.yaml:\n" + opis
					+ "\n  tijelo: " + odgovor.getContentAsString(StandardCharsets.UTF_8));
		}
		zapisi(opis);
	}

	private static synchronized void zapisi(String opis) throws IOException {
		if (ZAPISANO.add(opis)) {
			Files.createDirectories(IZVJESTAJ.getParent());
			Files.writeString(IZVJESTAJ, opis + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		}
	}

	/**
	 * Slučajevi koji nisu neslaganje: ruta koje nema u specifikaciji i zato daje 401 ili 404 (test
	 * namjerno zove nepostojeću rutu), i slika (binarni odgovor), čije se tijelo ovdje ne čita.
	 */
	private static boolean opravdano(String kljuc, MockHttpServletResponse odgovor) {
		return switch (kljuc) {
			case "validation.request.path.missing" -> odgovor.getStatus() == 401 || odgovor.getStatus() == 404;
			case "validation.response.body.missing" -> odgovor.getContentType() != null
					&& odgovor.getContentType().startsWith("image/");
			default -> false;
		};
	}
}
