package org.etf.pumpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.etf.pumpd.service.TestPomocni.assertStatus;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SlikeServiceTest {

    private static final String IME = "7f3c9a2e-41b8-4d2a-9c11-5e8a0b6d2f44.jpg";

    @TempDir
    Path folder;

    private SlikeService servis;

    @BeforeEach
    void postavi() {
        servis = new SlikeService(folder.toString());
    }

    @AfterEach
    void ocisti() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Path napraviSliku(String ime) throws IOException {
        return Files.write(folder.resolve(ime), new byte[] { 1, 2, 3 });
    }

    // ------------------------------------------------------------ adrese i imena

    @Test
    void ispravnaAdresaDajePutanjuUFolderu() {
        assertEquals(folder.toAbsolutePath().resolve(IME), servis.putanja("/images/" + IME).orElseThrow());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "https://nekisajt.com/" + IME,             // vanjski link
            "/images/../application.properties",   // izlazak iz foldera
            "/images/../../etc/passwd",
            "/images/slika.jpg",                    // ime koje nije UUID
            "/images/7f3c9a2e-41b8-4d2a-9c11-5e8a0b6d2f44.gif",   // nedozvoljen nastavak
            "/images/7F3C9A2E-41B8-4D2A-9C11-5E8A0B6D2F44.jpg",   // server pravi samo mala slova
            "/images/" + IME + "/x",
            "/api/images/" + IME,                       // stari oblik (od korijena servera) više ne važi
            "images/" + IME,                         // bez / na početku
            ""})
    void neispravnaAdresaNemaPutanju(String adresa) {
        assertTrue(servis.putanja(adresa).isEmpty(), String.valueOf(adresa));
    }

    @Test
    void postojiSamoAkoFajlPostojiNaDisku() throws IOException {
        assertFalse(servis.postoji("/images/" + IME));

        napraviSliku(IME);

        assertTrue(servis.postoji("/images/" + IME));
        assertFalse(servis.postoji("https://nekisajt.com/" + IME));
    }

    // ------------------------------------------------------------ brisanje

    @Test
    void vanTransakcijeBriseOdmah() throws IOException {
        Path fajl = napraviSliku(IME);

        servis.obrisiPoslijeCommita("/images/" + IME);

        assertFalse(Files.exists(fajl));
    }

    @Test
    void uTransakcijiBriseTekPoslijeCommita() throws IOException {
        Path fajl = napraviSliku(IME);
        TransactionSynchronizationManager.initSynchronization();

        servis.obrisiPoslijeCommita("/images/" + IME);
        assertTrue(Files.exists(fajl), "prije commita slika još postoji");

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertFalse(Files.exists(fajl));
    }

    @Test
    void ponistenaTransakcijaNeBriseSliku() throws IOException {
        Path fajl = napraviSliku(IME);
        TransactionSynchronizationManager.initSynchronization();

        servis.obrisiPoslijeCommita("/images/" + IME);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertTrue(Files.exists(fajl));
    }

    @Test
    void brisanjeNepostojeceIliNeispravneSlikeNijeGreska() {
        servis.obrisiPoslijeCommita("/images/" + IME);
        servis.obrisiPoslijeCommita("/images/../nesto");
        servis.obrisiPoslijeCommita(null);
    }

    // ------------------------------------------------------------ čuvanje slike (POST /api/posts/images)

    /** Početni bajtovi stvarnih formata, uz malo "sadržaja" iza njih. */
    static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F' };
    static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D };
    static final byte[] WEBP = { 'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' ' };

    private static MockMultipartFile fajl(String ime, String tip, byte[] bajtovi) {
        return new MockMultipartFile("image", ime, tip, bajtovi);
    }

    @ParameterizedTest
    @CsvSource({"JPEG, jpg", "PNG, png", "WEBP, webp"})
    void cuvaSlikuPodNasumicnimImenomSaNastavkomPremaStvarnomFormatu(String format, String nastavak)
            throws IOException {
        byte[] bajtovi = switch (format) {
            case "JPEG" -> JPEG;
            case "PNG" -> PNG;
            default -> WEBP;
        };
        // ime i Content-Type od klijenta su namjerno pogrešni: server ih ne koristi
        String adresa = servis.sacuvaj(fajl("../../moja slika.gif", "text/plain", bajtovi));

        assertTrue(adresa.matches("/images/[0-9a-f-]{36}\\." + nastavak), adresa);
        Path sacuvan = servis.putanja(adresa).orElseThrow();
        assertEquals(folder.toAbsolutePath(), sacuvan.getParent());
        assertArrayEquals(bajtovi, Files.readAllBytes(sacuvan));
        assertTrue(servis.postoji(adresa));
    }

    @Test
    void dvijeIsteSlikeDobijajuRazlicitaImena() {
        assertFalse(servis.sacuvaj(fajl("a.jpg", "image/jpeg", JPEG)).equals(servis.sacuvaj(fajl("a.jpg", "image/jpeg", JPEG))));
    }

    @Test
    void folderSePraviAkoNePostoji() {
        SlikeService uNovomFolderu = new SlikeService(folder.resolve("novi/podfolder").toString());

        String adresa = uNovomFolderu.sacuvaj(fajl("a.png", "image/png", PNG));

        assertTrue(uNovomFolderu.postoji(adresa));
    }

    @Test
    void tekstPreimenovanUJpgSeOdbija() {
        var e = assertStatus(400, () -> servis.sacuvaj(fajl("virus.jpg", "image/jpeg", "<script>alert(1)</script>".getBytes())));

        assertEquals("Dozvoljene su samo JPEG, PNG i WebP slike.", e.getReason());
        assertEquals(0, folder.toFile().list().length, "ništa nije upisano");
    }

    @Test
    void gifSeOdbija() {
        assertStatus(400, () -> servis.sacuvaj(fajl("a.gif", "image/gif", "GIF89a....".getBytes())));
    }

    @Test
    void riffKojiNijeWebpSeOdbija() {
        // RIFF zaglavlje imaju i WAV/AVI fajlovi; slika je samo ako na bajtu 8 piše WEBP
        assertStatus(400, () -> servis.sacuvaj(fajl("a.webp", "image/webp",
                new byte[] { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E' })));
    }

    @Test
    void praznaSlikaSeOdbija() {
        var e = assertStatus(400, () -> servis.sacuvaj(fajl("a.jpg", "image/jpeg", new byte[0])));

        assertEquals("Slika je prazna.", e.getReason());
    }

    @Test
    void prekratakFajlSeOdbija() {
        assertStatus(400, () -> servis.sacuvaj(fajl("a.jpg", "image/jpeg", new byte[] { (byte) 0xFF })));
    }

    @ParameterizedTest
    @CsvSource({"a.jpg, image/jpeg", "a.png, image/png", "a.webp, image/webp"})
    void contentTypePremaNastavku(String ime, String tip) {
        assertEquals(MediaType.parseMediaType(tip), servis.tip(folder.resolve(ime)));
    }
}
