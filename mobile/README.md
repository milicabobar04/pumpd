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

## Prijava u Fazi 0

Ekran prijave je privremen: četiri dugmeta, po jedno za svaku ulogu (`MEMBER`, `TRAINER`, `WORKER`, `ADMIN`).
U Fazi 1 ga zamjenjuje forma koja zove `POST /auth/login` i prosljeđuje odgovor u `signIn(...)`.

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
