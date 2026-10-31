
-- ---------------------------------------------------------------------
-- nalog
-- ugovor: 1.1 prijava je preko emaila, 11.3/12.3/12.7 vraćaju 409 za dupli email
-- ---------------------------------------------------------------------
UPDATE nalog SET email = CONCAT('nalog', idnalog, '@pumpd.local') WHERE email IS NULL;
ALTER TABLE nalog MODIFY COLUMN email VARCHAR(100) NOT NULL;


-- ---------------------------------------------------------------------
-- clan
-- ugovor: 2.3, 11.8, 11.9 - QR kod člana (generisanje i prepoznavanje pri ulasku)
-- ---------------------------------------------------------------------
-- Vrijednost su 32 slučajna heksadecimalna znaka. UUID() se ne koristi, jer MySQL pravi
-- UUID verzije 1 (zasnovan na vremenu), čiji se sljedeći kodovi mogu pogoditi.
ALTER TABLE clan ADD COLUMN qrKod VARCHAR(64) NULL;
UPDATE clan SET qrKod = HEX(RANDOM_BYTES(16)) WHERE qrKod IS NULL;
ALTER TABLE clan
    MODIFY COLUMN qrKod VARCHAR(64) NOT NULL DEFAULT (HEX(RANDOM_BYTES(16))),
    ADD UNIQUE INDEX qrKod_UNIQUE (qrKod);


-- ---------------------------------------------------------------------
-- clanarina
-- ugovor: 11.6 ne šalje vrstu članarine, pa kolona ne smije biti obavezna
-- Mapiranje: datumPocetka = paymentDate, datumIsteka = validUntil, cijena = price.
-- isActive se ne čuva, računa se (validUntil >= današnji datum).
-- ---------------------------------------------------------------------
ALTER TABLE clanarina
    MODIFY COLUMN vrsta ENUM('mjesecna', 'sedmicna', 'godisnja') NULL;


-- ---------------------------------------------------------------------
-- dolazak (nova tabela)
-- ugovor: 11.8 ulazak, 11.9 izlazak, 5.3 zauzetost (redovi bez izlaska),
--         2.4 historija treninga (samostalni treninzi)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dolazak (
    iddolazak INT NOT NULL AUTO_INCREMENT,
    clan_nalog_idnalog INT NOT NULL,
    vrijemeUlaska DATETIME NOT NULL,
    vrijemeIzlaska DATETIME NULL,
    PRIMARY KEY (iddolazak),
    INDEX fk_dolazak_clan1_idx (clan_nalog_idnalog ASC, vrijemeUlaska ASC),
    INDEX dolazak_prisutni_idx (vrijemeIzlaska ASC),
    CONSTRAINT fk_dolazak_clan1
        FOREIGN KEY (clan_nalog_idnalog)
        REFERENCES clan (nalog_idnalog)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
) ENGINE = InnoDB;


-- ---------------------------------------------------------------------
-- plan_treninga
-- ugovor: 3.1-3.4 durationMinutes
-- Postojeći planovi dobijaju 60 minuta. Vlasnik plana je onaj ko ga je kreirao:
-- trener_nalog_idnalog (trener) ILI clan_nalog_idnalog (član), vidi 3.1.
-- ---------------------------------------------------------------------
ALTER TABLE plan_treninga
    ADD COLUMN trajanjeMinuta INT NOT NULL DEFAULT 60,
    ADD CONSTRAINT chk_plan_trajanje CHECK (trajanjeMinuta > 0);


-- ---------------------------------------------------------------------
-- vjezba, vjezba_u_treningu
-- ugovor: 3.x vježbu u planu pravi i mijenja sam plan (exerciseId dodjeljuje server,
--         šalje se samo naziv), bez administratora i kategorije.
-- Brojevi serija i ponavljanja su bili TINYINT (najviše 127).
-- ---------------------------------------------------------------------
ALTER TABLE vjezba
    MODIFY COLUMN administrator_zaposleni_nalog_idnalog INT NULL,
    MODIFY COLUMN kategorija_vjezbe_idkategorija_vjezbe INT NULL;

ALTER TABLE vjezba_u_treningu
    MODIFY COLUMN brojSerija SMALLINT NULL,
    MODIFY COLUMN brojPonavljanja SMALLINT NULL;


-- ---------------------------------------------------------------------
-- problem
-- ugovor: 4.1 javna ruta (nalog nije obavezan), title, status UNPROCESSED na početku,
--         4.2 reportDate. idproblem nije imao AUTO_INCREMENT.
-- Mapiranje statusa: neobradjen = UNPROCESSED, u_obradi = IN_PROGRESS, obradjen = PROCESSED.
-- ---------------------------------------------------------------------
ALTER TABLE problem MODIFY COLUMN idproblem INT NOT NULL AUTO_INCREMENT;
ALTER TABLE problem MODIFY COLUMN nalog_idnalog INT NULL;

ALTER TABLE problem ADD COLUMN naslov VARCHAR(150) NULL;
UPDATE problem SET naslov = COALESCE(NULLIF(LEFT(opis, 100), ''), '(bez naslova)') WHERE naslov IS NULL;
UPDATE problem SET status = 'neobradjen' WHERE status IS NULL;
UPDATE problem SET vrijeme = NOW() WHERE vrijeme IS NULL;
ALTER TABLE problem
    MODIFY COLUMN naslov VARCHAR(150) NOT NULL,
    MODIFY COLUMN status ENUM('neobradjen', 'u_obradi', 'obradjen') NOT NULL DEFAULT 'neobradjen',
    MODIFY COLUMN vrijeme DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;


-- ---------------------------------------------------------------------
-- obavjestenje
-- ugovor: 6.1 isSeen. idobavjestenje nije imao AUTO_INCREMENT.
-- Kolone nalog_idnalog i nalog_idnalog1 nisu govorile ko je primalac. Odluka:
--   nalog_idnalog  -> primalac_nalog_idnalog   (obavezan)
--   nalog_idnalog1 -> posiljalac_nalog_idnalog (nije obavezan, sistemska
--                                               obavještenja nemaju pošiljaoca)
-- U demo aplikaciji nijedna klasa ne koristi ovu tabelu.
-- ---------------------------------------------------------------------
ALTER TABLE obavjestenje MODIFY COLUMN idobavjestenje INT NOT NULL AUTO_INCREMENT;
ALTER TABLE obavjestenje RENAME COLUMN nalog_idnalog TO primalac_nalog_idnalog;
ALTER TABLE obavjestenje RENAME COLUMN nalog_idnalog1 TO posiljalac_nalog_idnalog;
ALTER TABLE obavjestenje MODIFY COLUMN posiljalac_nalog_idnalog INT NULL;

UPDATE obavjestenje SET sadrzaj = '' WHERE sadrzaj IS NULL;
UPDATE obavjestenje SET datumVrijeme = NOW() WHERE datumVrijeme IS NULL;
ALTER TABLE obavjestenje
    ADD COLUMN procitano BOOLEAN NOT NULL DEFAULT 0,
    MODIFY COLUMN sadrzaj VARCHAR(1000) NOT NULL,
    MODIFY COLUMN datumVrijeme DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;


-- ---------------------------------------------------------------------
-- teretana
-- ugovor: 5.1/5.2 membershipPrice, dateOfEstablishment; 5.3 capacity (može biti null)
-- Red sa idteretana = 1 je jedini red u tabeli (nalazi se u seed podacima).
-- Mapiranje: naziv = name, opis = description, radnoVrijeme = workingHours.
-- adresa, vlasnik i kontakt ugovor ne koristi, ostaju u tabeli.
-- ---------------------------------------------------------------------
ALTER TABLE teretana
    ADD COLUMN cijenaClanarine DECIMAL(8, 2) NULL,
    ADD COLUMN datumOsnivanja DATE NULL,
    ADD COLUMN kapacitet INT NULL;


-- ---------------------------------------------------------------------
-- pracenje
-- ugovor: 7.3 followRequestId, 10.1/10.2 requestId, 7.2 followStatus
-- Mapiranje statusa: na_cekanju = PENDING, aktivan = ACCEPTED,
--                    neaktivan = odbijen zahtjev (red ostaje, da 10.2 može vratiti 409
--                    "već obrađen"). Prekid praćenja (7.4) briše red.
-- ---------------------------------------------------------------------
ALTER TABLE pracenje
    ADD COLUMN idpracenje INT NOT NULL AUTO_INCREMENT,
    ADD UNIQUE INDEX idpracenje_UNIQUE (idpracenje);

UPDATE pracenje SET status = 'aktivan' WHERE status IS NULL;
UPDATE pracenje SET DatumPracenja = CURDATE() WHERE DatumPracenja IS NULL;
ALTER TABLE pracenje
    MODIFY COLUMN status ENUM('aktivan', 'neaktivan', 'na_cekanju') NOT NULL DEFAULT 'na_cekanju';


-- ---------------------------------------------------------------------
-- recenzija
-- ugovor: 7.5 ocjena 1-5 (400 van opsega). idrecenzija nije imao AUTO_INCREMENT.
-- ---------------------------------------------------------------------
ALTER TABLE recenzija MODIFY COLUMN idrecenzija INT NOT NULL AUTO_INCREMENT;
ALTER TABLE recenzija
    ADD CONSTRAINT chk_recenzija_ocjena CHECK (ocjena IS NULL OR ocjena BETWEEN 1 AND 5);


-- ---------------------------------------------------------------------
-- objava
-- ugovor: 8.6 dodavanje objave. idobjava nije imao AUTO_INCREMENT.
-- Mapiranje: sadrzaj = content, vrijeme = postTimestamp, slikaPath = imageUrl.
-- ---------------------------------------------------------------------
-- idobjava referencira tabela sacuvana_objava, pa MySQL ne dopušta izmjenu dok
-- su provjere stranih ključeva uključene. Vrijednosti se ne mijenjaju.
SET @stari_fk_checks = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE objava MODIFY COLUMN idobjava INT NOT NULL AUTO_INCREMENT;
SET FOREIGN_KEY_CHECKS = @stari_fk_checks;


-- ---------------------------------------------------------------------
-- grupa, termin
-- ugovor: 9 "svaki termin ima tačno jednu grupu", 10.5 pravi termin i grupu zajedno,
--         maxMembers je Integer (TINYINT je dopuštao najviše 127).
-- Postojeći termini bez grupe dobijaju svoju grupu (naziv "Termin <id>", 20 mjesta).
-- Nije dodan UNIQUE na termin.grupa_idgrupa, jer demo aplikacija dopušta više
-- termina u istoj grupi; pravilo "jedan termin = jedna grupa" čuva servis.
-- ---------------------------------------------------------------------
ALTER TABLE grupa MODIFY COLUMN maxClanova SMALLINT NULL;

-- Pri izmjeni termina radi trigger preklapanja. Ovdje se mijenja samo grupa,
-- pa se stari trigger uklanja i vraća u V3.
DROP TRIGGER IF EXISTS provjeri_termin_update;

INSERT INTO grupa (naziv, opis, maxClanova, trener_nalog_idnalog)
SELECT CONCAT('Termin ', t.idtermin), CONCAT('__migracija:', t.idtermin), 20, t.trener_nalog_idnalog
FROM termin t
WHERE t.grupa_idgrupa IS NULL;

UPDATE termin t
JOIN grupa g ON g.opis = CONCAT('__migracija:', t.idtermin)
SET t.grupa_idgrupa = g.idgrupa
WHERE t.grupa_idgrupa IS NULL;

UPDATE grupa SET opis = NULL WHERE opis LIKE '\_\_migracija:%';

ALTER TABLE termin MODIFY COLUMN grupa_idgrupa INT NOT NULL;


-- ---------------------------------------------------------------------
-- clan_na_terminu
-- ugovor: 9.1 enrollmentStatus (NONE/PENDING/ACCEPTED/REJECTED), 9.2 requestId,
--         10.12 requestDate, 10.13 requestId
-- Mapiranje: nema reda = NONE, na_cekanju = PENDING, prijavljen (i prisutan) = ACCEPTED,
--            odbijen = REJECTED. Odjava (9.3) briše red.
-- Nove vrijednosti su dodane na kraj ENUM-a, da migracija ne prepisuje tabelu.
-- ---------------------------------------------------------------------
ALTER TABLE clan_na_terminu
    ADD COLUMN idzahtjev INT NOT NULL AUTO_INCREMENT,
    ADD UNIQUE INDEX idzahtjev_UNIQUE (idzahtjev),
    ADD COLUMN datumZahtjeva DATE NULL;

UPDATE clan_na_terminu SET status = 'prijavljen' WHERE status IS NULL;
UPDATE clan_na_terminu SET datumZahtjeva = CURDATE() WHERE datumZahtjeva IS NULL;
ALTER TABLE clan_na_terminu
    MODIFY COLUMN status ENUM('prijavljen', 'otkazao', 'prisutan', 'na_cekanju', 'odbijen')
        NOT NULL DEFAULT 'na_cekanju';