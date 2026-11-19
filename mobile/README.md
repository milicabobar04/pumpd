# pumpd – mobilna aplikacija

React Native (Expo SDK 58) aplikacija za Android i iOS.

## Prvo pokretanje

U folderu `mobile/`:

```bash
npm install
cp .env.example .env
```

U `.env` upiši IP adresu svog računara (Windows: `ipconfig`, red „IPv4 Address“ pod „Wireless LAN adapter Wi-Fi“).
Telefon i računar moraju biti na istoj Wi-Fi mreži.

## Svakodnevno pokretanje

Dva terminala u `mobile/`:

```bash
npm run mock      # mock server iz backend/docs/openapi.yaml na portu 4010
npx expo start    # QR kod skenirati u Expo Go (Android) ili kamerom (iOS)
```

Poslije izmjene `.env` ili dodavanja novih fajlova: `npx expo start -c` (čisti keš).
Android emulator: u terminalu sa Expom pritisnuti `a`.

Ako `npm run mock` javi `EADDRINUSE`, port 4010 već zauzima stari mock (zatvoriti ga sa `Ctrl+C` u njegovom terminalu).

## Mock ili pravi backend

Aplikacija zove adresu iz `EXPO_PUBLIC_API_URL` u `.env`; kod se ne mijenja.

| Server | Vrijednost |
|---|---|
| Mock (Prism) | `http://<IP>:4010` |
| Pravi backend (Spring Boot) | `http://<IP>:8080/api` |

Mock odgovara prema `openapi.yaml`:
- na zaštićenim rutama vraća 401 bez zaglavlja `Authorization: Bearer ...` (sadržaj tokena ne provjerava),
- ne pamti podatke (POST/DELETE ne mijenjaju liste) i ne provjerava uloge (403),
- određeni status vraća na zahtjev iz Postmana/curla zaglavljem `Prefer: code=404`.

## Prijava i odjava (Faza 1, SU-03, SU-04)

- Ekran prijave zove `POST /auth/login` i odgovor prosljeđuje u `signIn(...)` iz `AuthContext`.
  `RootNavigator` zatim sam prikazuje dio aplikacije za ulogu korisnika.
- Token i podaci o korisniku čuvaju se u `expo-secure-store` (`services/sessionStorage.js`),
  pa korisnik ostaje prijavljen i poslije zatvaranja aplikacije. Istekao JWT se briše pri pokretanju.
- Odjava (ikona u headeru) traži potvrdu, zove `POST /auth/logout` i briše sesiju sa uređaja.
  Odgovor 401 na bilo kojoj ruti odjavljuje korisnika samo na uređaju.
- Ako prijava vrati `mustChangePassword: true`, korisnik vidi samo ekran promjene lozinke
  dok je ne promijeni (`PUT /users/my-password`).

Nalozi za testiranje nad pravim backendom su u `backend/src/main/resources/db/seed/seed_test_podaci.sql`
(`admin@`, `radnik@`, `trener1@`, `clan1@pumpd.local`). Mock server prihvata bilo koji email i lozinku
i uvijek vraća isti primjer korisnika iz `openapi.yaml`.

## Struktura

```
src/
├── components/   zajedničke komponente (Button, Card, ConfirmDialog, AppHeader, Logo)
├── context/      AuthContext: prijavljeni korisnik, token, uloga
├── navigation/   RootNavigator bira dio aplikacije po ulozi:
│                 MemberTabs i TrainerTabs (tab bar), WorkerStack i AdminStack (početni meni)
├── screens/      ekrani; common/ sadrži zajedničke i privremene ekrane
├── services/     apiClient (axios: adresa iz .env, JWT u zaglavlju, 401 -> odjava)
└── theme/        boje, fontovi, razmaci i stilovi dugmadi i kartica iz Figme
```
