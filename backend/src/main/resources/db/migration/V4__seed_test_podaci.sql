

SET @lozinka = '$2a$10$v1AZ6pp.ouikgmlNzl/i/OD.yRm78webOYds5oq4UbLtI9amFA6S2';

-- ---------------------------------------------------------------------
-- Informacije o teretani (jedan red, idteretana = 1)
-- ---------------------------------------------------------------------
INSERT INTO teretana (idteretana, naziv, adresa, vlasnik, radnoVrijeme, opis, kontakt,
                      cijenaClanarine, datumOsnivanja, kapacitet)
VALUES (1, 'Pumpd Gym', 'Ulica Zdravlja 12, Banja Luka', 'Pumpd d.o.o.',
        'Pon-Pet 06:00-22:00, Sub-Ned 08:00-20:00',
        'Moderna teretana sa grupnim treninzima, personalnim trenerima i prostorom za slobodne tegove.',
        '+387 51 123 456', 40.00, '2019-03-15', 80)
ON DUPLICATE KEY UPDATE idteretana = idteretana;

-- ---------------------------------------------------------------------
-- Nalozi
-- ---------------------------------------------------------------------
INSERT INTO nalog (ime, prezime, email, lozinka) VALUES
    ('Admir',  'Delić',     'admin@pumpd.local',  @lozinka),
    ('Emina',  'Kurtović',  'radnik@pumpd.local', @lozinka),
    ('Marko',  'Petrović',  'trener1@pumpd.local', @lozinka),
    ('Ivana',  'Jovanović', 'trener2@pumpd.local', @lozinka),
    ('Nikola', 'Savić',     'trener3@pumpd.local', @lozinka),
    ('Amar',   'Hodžić',    'clan1@pumpd.local',  @lozinka),
    ('Lejla',  'Begović',   'clan2@pumpd.local',  @lozinka),
    ('Tarik',  'Mujić',     'clan3@pumpd.local',  @lozinka),
    ('Sara',   'Kovač',     'clan4@pumpd.local',  @lozinka),
    ('Dino',   'Ćatić',     'clan5@pumpd.local',  @lozinka)
ON DUPLICATE KEY UPDATE idnalog = idnalog;

SET @admin   = (SELECT idnalog FROM nalog WHERE email = 'admin@pumpd.local');
SET @radnik  = (SELECT idnalog FROM nalog WHERE email = 'radnik@pumpd.local');
SET @trener1 = (SELECT idnalog FROM nalog WHERE email = 'trener1@pumpd.local');
SET @trener2 = (SELECT idnalog FROM nalog WHERE email = 'trener2@pumpd.local');
SET @trener3 = (SELECT idnalog FROM nalog WHERE email = 'trener3@pumpd.local');
SET @clan1   = (SELECT idnalog FROM nalog WHERE email = 'clan1@pumpd.local');
SET @clan2   = (SELECT idnalog FROM nalog WHERE email = 'clan2@pumpd.local');
SET @clan3   = (SELECT idnalog FROM nalog WHERE email = 'clan3@pumpd.local');
SET @clan4   = (SELECT idnalog FROM nalog WHERE email = 'clan4@pumpd.local');
SET @clan5   = (SELECT idnalog FROM nalog WHERE email = 'clan5@pumpd.local');

-- Uloge
INSERT INTO zaposleni (nalog_idnalog) VALUES (@admin), (@radnik)
ON DUPLICATE KEY UPDATE nalog_idnalog = nalog_idnalog;

INSERT INTO administrator (zaposleni_nalog_idnalog) VALUES (@admin)
ON DUPLICATE KEY UPDATE zaposleni_nalog_idnalog = zaposleni_nalog_idnalog;

INSERT INTO radnik (zaposleni_nalog_idnalog) VALUES (@radnik)
ON DUPLICATE KEY UPDATE zaposleni_nalog_idnalog = zaposleni_nalog_idnalog;

INSERT INTO trener (nalog_idnalog, tjelesnaTezina, visina, opis) VALUES
    (@trener1, 84.50, 183.00, 'Personalni trener, 8 godina iskustva u radu sa početnicima i povratnicima poslije povrede.'),
    (@trener2, 61.00, 170.00, 'Instruktorica joge i mobilnosti. Grupni treninzi za sve nivoe.'),
    (@trener3, 90.00, 188.00, 'CrossFit trener i bivši takmičar u dizanju tegova.')
ON DUPLICATE KEY UPDATE nalog_idnalog = nalog_idnalog;

INSERT INTO clan (nalog_idnalog, tjelesnaTezina, visina, pol, datumRodjenja, qrKod) VALUES
    (@clan1, 80.00, 180.00, 'musko',  '1998-04-12', 'SEED-QR-CLAN1'),
    (@clan2, 60.50, 168.00, 'zensko', '2001-09-03', 'SEED-QR-CLAN2'),
    (@clan3, 92.00, 185.00, 'musko',  '1990-01-25', 'SEED-QR-CLAN3'),
    (@clan4, 58.00, 165.00, 'zensko', '1995-11-30', 'SEED-QR-CLAN4'),
    (@clan5, 75.00, 178.00, 'musko',  '2003-06-18', 'SEED-QR-CLAN5')
ON DUPLICATE KEY UPDATE nalog_idnalog = nalog_idnalog;

-- ---------------------------------------------------------------------
-- Članarine (clan5 je namjerno bez članarine)
-- ---------------------------------------------------------------------
INSERT INTO clanarina (vrsta, cijena, datumPocetka, datumIsteka, clan_nalog_idnalog) VALUES
    (NULL, 40.00,  DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 20 DAY),  @clan1),
    (NULL, 40.00,  DATE_SUB(CURDATE(), INTERVAL 30 DAY), CURDATE(),                             @clan2),
    (NULL, 40.00,  DATE_SUB(CURDATE(), INTERVAL 45 DAY), DATE_SUB(CURDATE(), INTERVAL 15 DAY),  @clan3),
    (NULL, 400.00, DATE_SUB(CURDATE(), INTERVAL 65 DAY), DATE_ADD(CURDATE(), INTERVAL 300 DAY), @clan4)
ON DUPLICATE KEY UPDATE clan_nalog_idnalog = clan_nalog_idnalog;

-- ---------------------------------------------------------------------
-- Grupni treninzi: svaki termin ima tačno jednu svoju grupu.
-- seed_termin: naziv grupe/termina, trener, kapacitet, dan (u odnosu na danas), početak
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS seed_termin;
CREATE TEMPORARY TABLE seed_termin (
    naziv        VARCHAR(45)  NOT NULL,
    opis         VARCHAR(500) NOT NULL,
    maxClanova   SMALLINT     NOT NULL,
    trener_email VARCHAR(100) NOT NULL,
    pomak_dana   INT          NOT NULL,
    pocetak      TIME         NOT NULL,
    trajanje     SMALLINT     NOT NULL
);

INSERT INTO seed_termin VALUES
    ('Jutarnji trening',    'Opšta kondicija za sve nivoe.',        10, 'trener1@pumpd.local',  1, '07:00:00', 60),
    ('Snaga i izdržljivost','Trening sa slobodnim tegovima.',        2, 'trener1@pumpd.local',  2, '18:00:00', 60),
    ('Joga za početnike',   'Spora joga, bez predznanja.',          12, 'trener2@pumpd.local',  1, '18:30:00', 75),
    ('Mobilnost',           'Istezanje i pokretljivost zglobova.',   3, 'trener2@pumpd.local',  3, '09:00:00', 60),
    ('CrossFit',            'Intenzivni funkcionalni trening.',     15, 'trener3@pumpd.local',  1, '20:00:00', 90),
    ('CrossFit (prošli)',   'Termin iz prošle sedmice.',            15, 'trener3@pumpd.local', -7, '20:00:00', 90);

INSERT INTO grupa (naziv, opis, maxClanova, trener_nalog_idnalog)
SELECT s.naziv, s.opis, s.maxClanova, n.idnalog
FROM seed_termin s
JOIN nalog n ON n.email = s.trener_email
WHERE NOT EXISTS (SELECT 1 FROM grupa g
                  WHERE g.naziv = s.naziv AND g.trener_nalog_idnalog = n.idnalog);

INSERT INTO termin (vrijeme, trajanje, sala, trener_nalog_idnalog, grupa_idgrupa)
SELECT TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL s.pomak_dana DAY), s.pocetak),
       s.trajanje, NULL, g.trener_nalog_idnalog, g.idgrupa
FROM seed_termin s
JOIN nalog n ON n.email = s.trener_email
JOIN grupa g ON g.naziv = s.naziv AND g.trener_nalog_idnalog = n.idnalog
WHERE NOT EXISTS (SELECT 1 FROM termin t WHERE t.grupa_idgrupa = g.idgrupa)
ORDER BY s.pomak_dana, s.pocetak;

-- ---------------------------------------------------------------------
-- Prijave na termine
--   prijavljen / prisutan = ACCEPTED (član je i u grupi), na_cekanju = PENDING, odbijen = REJECTED
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS seed_upis;
CREATE TEMPORARY TABLE seed_upis (
    naziv        VARCHAR(45)  NOT NULL,
    trener_email VARCHAR(100) NOT NULL,
    clan_email   VARCHAR(100) NOT NULL,
    status       VARCHAR(20)  NOT NULL
);

INSERT INTO seed_upis VALUES
    ('Jutarnji trening',     'trener1@pumpd.local', 'clan1@pumpd.local', 'prijavljen'),
    ('Jutarnji trening',     'trener1@pumpd.local', 'clan4@pumpd.local', 'prijavljen'),
    ('Jutarnji trening',     'trener1@pumpd.local', 'clan3@pumpd.local', 'na_cekanju'),
    ('Snaga i izdržljivost', 'trener1@pumpd.local', 'clan1@pumpd.local', 'prijavljen'),
    ('Snaga i izdržljivost', 'trener1@pumpd.local', 'clan2@pumpd.local', 'prijavljen'),
    ('Joga za početnike',    'trener2@pumpd.local', 'clan2@pumpd.local', 'odbijen'),
    ('Mobilnost',            'trener2@pumpd.local', 'clan3@pumpd.local', 'prijavljen'),
    ('Mobilnost',            'trener2@pumpd.local', 'clan4@pumpd.local', 'prijavljen'),
    ('CrossFit (prošli)',    'trener3@pumpd.local', 'clan2@pumpd.local', 'prisutan'),
    ('CrossFit (prošli)',    'trener3@pumpd.local', 'clan4@pumpd.local', 'prisutan');

INSERT INTO clan_u_grupi (clan_nalog_idnalog, grupa_idgrupa, datumPristupanja)
SELECT c.idnalog, g.idgrupa, CURDATE()
FROM seed_upis u
JOIN nalog tr ON tr.email = u.trener_email
JOIN grupa g ON g.naziv = u.naziv AND g.trener_nalog_idnalog = tr.idnalog
JOIN nalog c ON c.email = u.clan_email
WHERE u.status IN ('prijavljen', 'prisutan')
  AND NOT EXISTS (SELECT 1 FROM clan_u_grupi x
                  WHERE x.clan_nalog_idnalog = c.idnalog AND x.grupa_idgrupa = g.idgrupa);

INSERT INTO clan_na_terminu (termin_idtermin, clan_nalog_idnalog, status, datumZahtjeva)
SELECT t.idtermin, c.idnalog, u.status,
       IF(u.status = 'prisutan', DATE_SUB(CURDATE(), INTERVAL 8 DAY), CURDATE())
FROM seed_upis u
JOIN nalog tr ON tr.email = u.trener_email
JOIN grupa g ON g.naziv = u.naziv AND g.trener_nalog_idnalog = tr.idnalog
JOIN termin t ON t.grupa_idgrupa = g.idgrupa
JOIN nalog c ON c.email = u.clan_email
WHERE NOT EXISTS (SELECT 1 FROM clan_na_terminu x
                  WHERE x.termin_idtermin = t.idtermin AND x.clan_nalog_idnalog = c.idnalog);

DROP TEMPORARY TABLE seed_upis;
DROP TEMPORARY TABLE seed_termin;

-- ---------------------------------------------------------------------
-- Obavještenja (6.1): za clan1 jedno pročitano i jedno nepročitano, za trener1 jedno
-- ---------------------------------------------------------------------
INSERT INTO obavjestenje (sadrzaj, datumVrijeme, primalac_nalog_idnalog, posiljalac_nalog_idnalog, procitano)
SELECT 'Dobrodošli u Pumpd! Vaš nalog je spreman.', DATE_SUB(NOW(), INTERVAL 3 DAY), @clan1, NULL, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM obavjestenje o
                  WHERE o.primalac_nalog_idnalog = @clan1 AND o.sadrzaj = 'Dobrodošli u Pumpd! Vaš nalog je spreman.');

INSERT INTO obavjestenje (sadrzaj, datumVrijeme, primalac_nalog_idnalog, posiljalac_nalog_idnalog, procitano)
SELECT 'Vaša članarina ističe za 20 dana.', DATE_SUB(NOW(), INTERVAL 2 HOUR), @clan1, NULL, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM obavjestenje o
                  WHERE o.primalac_nalog_idnalog = @clan1 AND o.sadrzaj = 'Vaša članarina ističe za 20 dana.');

INSERT INTO obavjestenje (sadrzaj, datumVrijeme, primalac_nalog_idnalog, posiljalac_nalog_idnalog, procitano)
SELECT 'Imate novi zahtjev za prijavu na termin "Jutarnji trening".', DATE_SUB(NOW(), INTERVAL 1 HOUR), @trener1, NULL, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM obavjestenje o
                  WHERE o.primalac_nalog_idnalog = @trener1
                    AND o.sadrzaj = 'Imate novi zahtjev za prijavu na termin "Jutarnji trening".');

-- ---------------------------------------------------------------------
-- Prijave problema (4.1): jedna anonimna i jedna od prijavljenog člana
-- ---------------------------------------------------------------------
INSERT INTO problem (naslov, opis, status, vrijeme, nalog_idnalog, administrator_zaposleni_nalog_idnalog)
SELECT 'Neispravna traka za trčanje', 'Traka broj 3 se zaustavlja tokom treninga.',
       'neobradjen', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM problem p WHERE p.naslov = 'Neispravna traka za trčanje');

INSERT INTO problem (naslov, opis, status, vrijeme, nalog_idnalog, administrator_zaposleni_nalog_idnalog)
SELECT 'Nema tople vode', 'U muškoj svlačionici nema tople vode od jutros.',
       'u_obradi', DATE_SUB(NOW(), INTERVAL 1 DAY), @clan1, @admin
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM problem p WHERE p.naslov = 'Nema tople vode');