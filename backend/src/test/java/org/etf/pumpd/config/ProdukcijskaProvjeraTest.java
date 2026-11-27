package org.etf.pumpd.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.etf.pumpd.BackendApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.mock.env.MockEnvironment;

/** Provjera produkcijskih postavki (2.4.3): bez obaveznih vrijednosti server se u profilu prod ne pokreće. */
class ProdukcijskaProvjeraTest {

    private static final String DOBAR_KLJUC = Base64.getEncoder().encodeToString(new byte[32]).replace('A', 'Q');

    private static MockEnvironment ispravno() {
        return new MockEnvironment()
                .withProperty("jwt.secret", DOBAR_KLJUC)
                .withProperty("spring.datasource.url", "jdbc:mysql://baza:3306/pumpd")
                .withProperty("spring.datasource.username", "pumpd")
                .withProperty("spring.datasource.password", "jaka-lozinka")
                .withProperty("pumpd.slike.folder", "/var/pumpd/slike");
    }

    private static String greska(MockEnvironment postavke) {
        return assertThrows(IllegalStateException.class, () -> ProdukcijskaProvjera.provjeri(postavke)).getMessage();
    }

    @Test
    void ispravnePostavkeProlaze() {
        assertDoesNotThrow(() -> ProdukcijskaProvjera.provjeri(ispravno()));
    }

    @Test
    void bezIjednePostavkeNabrajaSve() {
        String poruka = greska(new MockEnvironment());

        for (String dio : new String[] {"JWT_SECRET", "DB_URL", "DB_USER", "DB_PASSWORD", "SLIKE_FOLDER"}) {
            assertTrue(poruka.contains(dio + " mora biti postavljen"), poruka);
        }
    }

    @Test
    void razvojniKljucIzGitaNijeDozvoljen() {
        String poruka = greska(ispravno().withProperty("jwt.secret", ProdukcijskaProvjera.RAZVOJNI_JWT_KLJUC));
        assertTrue(poruka.contains("razvojni ključ"), poruka);
    }

    @Test
    void razvojniKljucJeIstiKaoUApplicationProperties() throws Exception {
        // ako se ključ u application.properties promijeni, i provjera mora znati novi
        String postavke = new String(getClass().getResourceAsStream("/application.properties").readAllBytes());
        assertTrue(postavke.contains("${JWT_SECRET:" + ProdukcijskaProvjera.RAZVOJNI_JWT_KLJUC + "}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"kratak", "c2Ftbw==", "nije base64 !!!"})
    void slabIliNeispravanKljuc(String kljuc) {
        String poruka = greska(ispravno().withProperty("jwt.secret", kljuc));
        assertTrue(poruka.contains("JWT_SECRET mora"), poruka);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", ""})
    void praznaLozinkaBazeNijeDozvoljena(String lozinka) {
        assertTrue(greska(ispravno().withProperty("spring.datasource.password", lozinka)).contains("DB_PASSWORD"));
    }

    @Test
    void relativanFolderZaSlikeNijeDozvoljen() {
        String poruka = greska(ispravno().withProperty("pumpd.slike.folder", "uploads/slike"));
        assertTrue(poruka.contains("apsolutna putanja"), poruka);
    }

    @Test
    void serverSeUProdProfiluBezPostavkiNePokrece() {
        // stvarno pokretanje: provjera mora pasti prije baze (inače bi poruka bila o bazi)
        Exception e = assertThrows(Exception.class, () -> new SpringApplicationBuilder(BackendApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("prod")
                .run("--spring.profiles.active=prod", "--jwt.secret=", "--spring.datasource.url=",
                        "--spring.datasource.username=", "--spring.datasource.password=", "--pumpd.slike.folder="));
        Throwable uzrok = e;
        while (uzrok.getCause() != null && !(uzrok instanceof IllegalStateException
                && uzrok.getMessage() != null && uzrok.getMessage().startsWith("Server nije pokrenut"))) {
            uzrok = uzrok.getCause();
        }
        assertTrue(uzrok.getMessage().startsWith("Server nije pokrenut"), uzrok.toString());
        assertTrue(uzrok.getMessage().contains("JWT_SECRET mora biti postavljen u produkciji."));
    }
}
