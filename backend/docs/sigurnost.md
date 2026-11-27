# pumpd: sigurnost backenda

Pregled sigurnosnih mjera backenda prema nefunkcionalnom zahtjevu **2.4.3 Sigurnost** iz
specifikacije, zajedno sa testovima koji ih dokazuju i poznatim ograničenjima.

> Zahtjev 2.4.3, ukratko: pristup samo autorizovanim korisnicima, prijava korisničkim imenom
> (email) i lozinkom za svaki modul sa privatnim podacima, heširanje lozinki, stroga ograničenja
> pristupa po ulogama, zaštita osjetljivih podataka i usklađenost sa GDPR-om.

---

## 1. Prijava i tokeni

| Mjera | Kako je urađeno |
|---|---|
| Prijava | `POST /api/auth/login` sa emailom i lozinkom. Pogrešan email i pogrešna lozinka daju **istu poruku** (401), pa se iz odgovora ne može saznati koji emailovi postoje. |
| Ujednačeno vrijeme odgovora | Za nepostojeći email server ipak provjerava lozinku nad lažnim BCrypt hešom, da odgovor ne bude primjetno brži i ne otkrije da email ne postoji. |
| Token | JWT potpisan ključem HS256 (najmanje 256 bita), važi 8 sati (`JWT_TRAJANJE`). Token nosi samo id naloga i jedinstveni id tokena (`jti`). |
| Uloga iz baze | Uloga se pri **svakom zahtjevu** čita iz baze, a ne iz tokena. Promjena ili brisanje naloga zato važi odmah, i za token koji je već izdat. |
| Odjava | `POST /api/auth/logout` upisuje `jti` u tabelu poništenih tokena, pa se token više ne može koristiti, iako mu rok još nije istekao. |
| Obrisan nalog | Token obrisanog (deaktiviranog) naloga se odbija (401). |
| Lažni token | Token sa pogrešnim potpisom, istekao ili neispravan token daje 401. |

## 2. Lozinke

| Mjera | Kako je urađeno |
|---|---|
| Heširanje | Lozinke se čuvaju samo kao **BCrypt heš** (cijena 10), nikad kao čist tekst. BCrypt za svaku lozinku dodaje nasumičnu so, pa ista lozinka kod dva naloga daje različite heševe. |
| Privremena lozinka | Novi nalog (11.3, 12.3, 12.7) dobija nasumičnu lozinku od 12 znakova (`SecureRandom`, bez znakova koji se lako zamijene, npr. `0/O`, `1/l`). Prikazuje se **jednom**, radniku ili administratoru koji pravi nalog. |
| Obavezna promjena | Nalog sa privremenom lozinkom ima `mustChangePassword = true`, pa aplikacija nakon prijave traži novu lozinku (`PUT /api/users/my-password`). |
| Promjena lozinke | Traži staru lozinku; nova ima 8 do 72 znaka (72 je granica BCrypt-a). |
| Lozinka nikad u odgovoru | Nijedna ruta ne vraća entitet `Nalog` niti polje sa hešom. Jedini izuzetak je `temporaryPassword` pri kreiranju naloga. |
| Logovi | Lozinke i tokeni se ne upisuju u logove. |

## 3. Pristup po ulogama

Uloge su `MEMBER`, `TRAINER`, `WORKER` i `ADMIN`. Pravila su u `config/SecurityConfig.java` i
prate polje „Pristup“ iz API ugovora.

- Javne su samo: prijava (1.1), prijava problema (4.1), informacije o teretani (5.1) i čitanje
  slike objave (`GET /api/images/{ime}`, ime je nasumičan UUID koji se ne može pogoditi).
- Sve ostale rute traže prijavu, **uključujući rute koje ne postoje** (`anyRequest().authenticated()`),
  pa se bez tokena ne može ni ispitivati koje rute postoje.
- Pogrešna uloga dobija **403**, a zahtjev bez tokena **401**, uz poruku u JSON obliku.
- Pored uloge, servisi provjeravaju i **vlasništvo**: plan treninga mijenja samo njegov vlasnik,
  objavu briše samo njen autor, termin mijenja samo trener koji ga je napravio, zahtjev za
  prijavu na termin obrađuje samo trener tog termina. Tuđi resurs daje 403 ili 404, kao što
  ugovor propisuje.

## 4. Zaštita podataka i ulaza

| Mjera | Kako je urađeno |
|---|---|
| SQL injection | Svi upiti idu kroz JPA i parametre (`:ime`), bez spajanja teksta u SQL. |
| Provjera ulaza | Tijela zahtjeva se provjeravaju (`@Valid`: obavezna polja, dužine, opsezi); neispravan JSON ili tip daje 400. Decimalni broj u cjelobrojnom polju je greška, a ne tiho skraćivanje. |
| Slike objava | Tip slike se provjerava **po sadržaju fajla** (JPEG, PNG, WebP), a ne po nastavku ili zaglavlju koje šalje klijent. Ime fajla daje server (UUID), pa se ne može podmetnuti putanja (`../`). Najviše 5 MB. |
| Greške | Odgovori na greške ne sadrže stack trace ni interne poruke (u `prod` profilu je to i eksplicitno isključeno). |
| Istovremeni zahtjevi | Kapacitet grupe, dvostruki klik i slične situacije čuvaju ograničenja u bazi i zaključavanje reda, pa nema nedosljednih podataka. |

## 5. Produkcijske postavke (server za predaju)

Server se za predaju pokreće sa `SPRING_PROFILES_ACTIVE=prod`. Tada **ne postoje podrazumijevane
vrijednosti** za tajne i bazu (`application-prod.properties`), a `config/ProdukcijskaProvjera.java`
prije pokretanja provjerava:

| Postavka | Pravilo |
|---|---|
| `JWT_SECRET` | obavezan, Base64, najmanje 256 bita, i **ne smije** biti razvojni ključ iz `application.properties` (on je javan u gitu) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | obavezni |
| `SLIKE_FOLDER` | obavezan, apsolutna putanja do trajnog foldera |

Ako bilo šta nedostaje, server se **ne pokreće** i ispiše sve propuste odjednom. Lokalno (profil
`dev`) i u testovima važe podrazumijevane vrijednosti, da razvoj ne traži podešavanje.

Ključ se pravi sa: `openssl rand -base64 32`. Tajne se drže u fajlu `.env` na serveru, koji ne
ide u git.

## 6. GDPR

| Pravo ili načelo | Stanje |
|---|---|
| Najmanje podataka | Čuvaju se samo podaci potrebni za rad teretane (ime, email, mjerenja, članarina, dolasci, treninzi). |
| Uvid u svoje podatke | Korisnik vidi i mijenja svoj profil (2.1, 2.2), istoriju treninga (2.4), članarinu (2.3) i planove (3.x). |
| Brisanje naloga | Brisanje je **meko**: nalog se deaktivira, ne može se prijaviti i ne prikazuje se nigdje, a email se oslobađa (`obrisan-<id>@pumpd.invalid`). Ime i povezani podaci (dolasci, recenzije, istorija) ostaju u bazi, zbog evidencije teretane i dosljednosti statistike. |
| Pristup po potrebi | Lične podatke člana vide samo zaposleni (radnik, administrator) i, u ograničenom obimu, njegov trener (pratioci, članovi grupe). |

## 7. Kako je provjereno (testovi)

| Test | Šta dokazuje |
|---|---|
| `PristupPoUlogamaIntegracioniTest` | **Za svih 69 ruta** iz `openapi.yaml` i svaku ulogu: dozvoljena uloga prolazi, zabranjena dobija 403, bez tokena 401 (osim javnih ruta). Ukupno 345 provjera nad pravim serverom i bazom. Test pada i ako se pojavi ruta bez upisanog pristupa. Odjavljen token, lažni token i token potpisan drugim ključem daju 401. |
| `LozinkeIntegracioniTest` | Sve lozinke u bazi su BCrypt heš (cijena ≥ 10); nova i promijenjena lozinka se ne čuvaju kao tekst; ista lozinka daje različite heševe; 13 stvarnih odgovora sa podacima korisnika ne sadrži heš. |
| `OdgovoriBezLozinkiTest` | Nijedna ruta ne vraća entitet niti polje sa lozinkom (provjera svih kontrolera). |
| `ProdukcijskaProvjeraTest` | U profilu `prod` server se bez postavki ne pokreće; razvojni, kratak ili neispravan ključ i relativna putanja za slike se odbijaju. |
| Testovi kontrolera (`*ControllerTest`) | Pristup po ulogama za svaku rutu, zajedno sa pravilima iz `SecurityConfig`. |
| `TokAIntegracioniTest` | Token obrisanog naloga daje 401 i obrisan nalog se ne može prijaviti. |

Pokretanje: `cd backend && ./mvnw test` (integracioni testovi traže Docker).

## 8. Poznata ograničenja

Ovo je svjesno ostavljeno za stvarnu produkciju; za odbranu na zatvorenoj mreži nije potrebno.

| Ograničenje | Šta bi se uradilo u stvarnoj produkciji |
|---|---|
| **Nema HTTPS-a.** Na odbrani aplikacija radi na zatvorenoj mreži (hotspot), pa se lozinke i tokeni šalju bez šifrovanja veze. | Reverse proxy sa TLS certifikatom (npr. Caddy ili Nginx + Let's Encrypt); backend dostupan samo preko njega. |
| **Podaci u bazi nisu šifrovani.** Lozinke su heširane, ali lični podaci (mjerenja, uplate, dolasci) su u bazi kao običan tekst. | Šifrovanje diska ili baze (*encryption at rest*), šifrovane rezervne kopije, baza dostupna samo backendu (firewall). |
| **Seed nalozi.** Migracija V4 pravi demo naloge (`admin@pumpd.local`, `radnik@`, `trener1–3@`, `clan1–5@`) sa lozinkom `pumpd123`, koja je u repozitoriju. Služe samo za demonstraciju. | Seed nalozi se ne prave u produkciji, ili im se lozinke postavljaju iz tajnih postavki uz obaveznu promjenu pri prvoj prijavi. |
| **Nema ograničenja broja pokušaja prijave.** | Zaključavanje ili usporavanje nakon više pogrešnih pokušaja (*rate limiting*). |
| **Token se ne može opozvati na drugim uređajima.** Odjava poništava samo token sa kojim je urađena; ostali važe do isteka (8 h). | Kraći rok tokena uz *refresh token*, i odjava sa svih uređaja pri promjeni lozinke. |
| **Slike objava su javne** (svako ko zna adresu može je otvoriti). Ime je nasumičan UUID, pa se adresa ne može pogoditi. Metapodaci iz fotografije (EXIF, npr. GPS lokacija) se ne uklanjaju. | Uklanjanje EXIF podataka pri slanju; po potrebi potpisane adrese sa rokom važenja. |
| **Meko brisanje čuva lične podatke** obrisanog naloga. | Na zahtjev korisnika (GDPR, pravo na brisanje) anonimizacija imena i ličnih podataka, uz zadržavanje samo onoga što zakon traži. |
