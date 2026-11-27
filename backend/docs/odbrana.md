# pumpd: pokretanje za odbranu

Backend i baza rade na **jednom laptopu** u Dockeru, a telefoni se na njih spajaju preko iste
Wi-Fi mreže (hotspot).

```
        hotspot (telefon ili laptop)
   ┌────────────┬────────────┬────────────┐
 telefon      telefon      telefon      laptop
 (član)       (trener)     (radnik)     ├─ backend :8080  (Docker, profil prod)
                                        ├─ MySQL          (Docker, nije dostupan sa mreže)
                                        └─ Expo / Metro   (ako se koristi Expo Go)
```

## 1. Šta treba instalirati (jednom)

- **Docker Desktop** (Windows, macOS) ili Docker Engine sa Compose dodatkom (Linux).
  Provjera: `docker compose version`.
- Projekat sa GitHub-a (`git clone`, grana `develop`).
- Za aplikaciju na telefonu: ono što traži `mobile/README.md` (Node, Expo).

Java, Maven i MySQL **ne treba** instalirati: sve je u kontejnerima.

## 2. Tajne postavke: fajl `.env` (jednom)

U korijenu projekta:

```bash
cp .env.example .env          # Windows (PowerShell): Copy-Item .env.example .env
```

U `.env` upisati svoje vrijednosti:

| Varijabla | Šta | Primjer kako napraviti |
|---|---|---|
| `DB_USER` | korisnik baze za aplikaciju | `pumpd` |
| `DB_PASSWORD` | njegova lozinka | izmisliti jaku lozinku |
| `DB_ROOT_PASSWORD` | lozinka root korisnika baze | druga jaka lozinka |
| `JWT_SECRET` | ključ za tokene (Base64, ≥ 256 bita) | vidi ispod |

Ključ za `JWT_SECRET`:

```bash
openssl rand -base64 32
```
```powershell
# Windows PowerShell 7 (ako nema openssl):
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

`.env` **nikad ne ide u git** (u `.gitignore` je). Ako nešto nedostaje, `docker compose` ili
backend odmah ispišu šta treba dodati.

## 3. Pokretanje

```bash
docker compose up -d --build
```

Prvi put traje nekoliko minuta (preuzimanje i pravljenje). Baza se pravi sama (Flyway), zajedno sa
demo podacima. Provjera na laptopu:

```bash
curl http://localhost:8080/api/gym/info
docker compose logs -f backend      # logovi; treba da piše: The following 1 profile is active: "prod"
```

## 4. Mreža i telefoni

1. Laptop i telefoni na **istom hotspotu**. Fakultetska ili javna Wi-Fi mreža često ne dozvoljava
   uređajima da se međusobno vide, pa telefon ne može doći do laptopa.
2. IP adresa laptopa na toj mreži:
   - Windows: `ipconfig` → „IPv4 Address“ pod Wi-Fi adapterom;
   - Linux: `ip -4 addr` (npr. `192.168.43.10`);
   - macOS: `ipconfig getifaddr en0`.
3. Dozvoliti port **8080** u firewallu laptopa (ako telefon ne dobija odgovor):
   - Windows (PowerShell kao administrator):
     `New-NetFirewallRule -DisplayName "pumpd 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow`
   - Linux (ufw): `sudo ufw allow 8080/tcp`
4. **Provjera sa telefona:** u pregledaču otvoriti `http://IP_LAPTOPA:8080/api/gym/info`; treba
   da se prikaže JSON sa informacijama o teretani.
5. U aplikaciji (`mobile/.env`): `EXPO_PUBLIC_API_URL=http://IP_LAPTOPA:8080/api`, pa pokrenuti
   aplikaciju ponovo sa očišćenim kešom (`npm start -- -c`). Ako se pravi APK, adresa se ugrađuje
   pri pravljenju, pa APK treba napraviti **sa IP adresom koja će važiti na odbrani**.

IP adresa se može promijeniti kad se laptop ponovo spoji na hotspot. Ako aplikacija odjednom javi
„Nema veze sa serverom“, prvo provjeriti IP.

## 5. Demo nalozi

Svi imaju lozinku `pumpd123` (samo za demonstraciju; vidi `sigurnost.md`, poznata ograničenja):

| Uloga | Email |
|---|---|
| administrator | `admin@pumpd.local` |
| radnik | `radnik@pumpd.local` |
| treneri | `trener1@pumpd.local`, `trener2@pumpd.local`, `trener3@pumpd.local` |
| članovi | `clan1@pumpd.local` … `clan5@pumpd.local` |

## 6. Svakodnevne komande

| Šta | Komanda |
|---|---|
| zaustaviti (podaci ostaju) | `docker compose down` |
| ponovo pokrenuti | `docker compose up -d` |
| nakon izmjene koda (nova verzija backenda) | `docker compose up -d --build` |
| logovi backenda | `docker compose logs -f backend` |
| **obrisati sve podatke** i krenuti od početka (nova baza sa demo podacima, bez slika) | `docker compose down -v` pa `docker compose up -d` |

Baza i slike objava su u Docker volumima (`baza-podaci`, `slike`), pa prežive gašenje laptopa i
`docker compose down`. Briše ih samo `down -v`.

## 7. Generalna proba (nekoliko dana prije odbrane)

Na **istom laptopu**, sa **istim telefonima** i hotspotom koji će se koristiti na odbrani:

- [ ] `docker compose up -d --build` prolazi, a u logu je profil `prod`
- [ ] sa telefona se otvara `http://IP_LAPTOPA:8080/api/gym/info`
- [ ] prijava u aplikaciji za svaku ulogu (admin, radnik, trener, član)
- [ ] glavni scenariji po ulogama (članarina i QR kod, ulazak i izlazak, praćenje trenera, objava sa slikom, prijava na termin i prihvatanje, istorija treninga)
- [ ] gašenje i ponovno pokretanje laptopa, pa `docker compose up -d`: podaci i slike su tu
- [ ] po potrebi `docker compose down -v` da odbrana počne sa čistim demo podacima

## 8. Kad nešto ne radi

| Problem | Uzrok i rješenje |
|---|---|
| `required variable ... is missing a value` | u `.env` nedostaje ta vrijednost |
| backend se ugasi, u logu `Server nije pokrenut, jer produkcijske postavke nisu potpune` | ispisano je šta nedostaje ili nije ispravno (npr. `JWT_SECRET`) |
| `port is already allocated` (8080) | nešto drugo koristi port 8080 (npr. backend pokrenut iz IDE-a); ugasiti ga ili u `.env` postaviti `BACKEND_PORT=8081` (i u aplikaciji :8081) |
| telefon: „Nema veze sa serverom“ | nije ista mreža, pogrešan IP u `EXPO_PUBLIC_API_URL`, ili firewall blokira 8080 (korak 4) |
| pregledač na telefonu otvara stranicu, a aplikacija ne | `EXPO_PUBLIC_API_URL` mora završavati sa `/api`; poslije izmjene `npm start -- -c` |
| pogrešno vrijeme termina ili dolazaka | vremenska zona: `TZ` u `.env` (podrazumijevano `Europe/Sarajevo`) |
