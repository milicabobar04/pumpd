import * as SecureStore from 'expo-secure-store';

// Sesija (token + podaci o korisniku) se čuva u SecureStore, a ne u AsyncStorage:
// Android ga šifruje ključem iz Keystore-a, a iOS čuva u Keychain-u.
// Token je ključ za cijeli nalog, pa ne smije stajati kao običan tekst na telefonu.
const SESSION_KEY = 'pumpd.session';

// Vraća { token, user } ili null ako ništa nije sačuvano (ili je zapis neispravan).
export async function loadSession() {
  try {
    const raw = await SecureStore.getItemAsync(SESSION_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw);
    if (!session?.token || !session?.user?.role) return null;
    return session;
  } catch {
    // Neispravan JSON ili nedostupan Keychain: ponašaj se kao da niko nije prijavljen.
    return null;
  }
}

export async function saveSession(session) {
  try {
    await SecureStore.setItemAsync(SESSION_KEY, JSON.stringify(session));
  } catch (e) {
    // Aplikacija i dalje radi, samo će korisnik morati ponovo da se prijavi poslije restarta.
    console.warn('Sesija nije sačuvana:', e?.message);
  }
}

export async function clearSession() {
  try {
    await SecureStore.deleteItemAsync(SESSION_KEY);
  } catch (e) {
    console.warn('Sesija nije obrisana:', e?.message);
  }
}

// Provjera roka važenja JWT tokena bez poziva serveru (polje `exp` u payload-u).
// Ako token nije JWT (npr. mock server vrati obični string), smatra se važećim,
// a server će ga svakako odbiti sa 401 ako ne valja (apiClient tada odjavljuje korisnika).
export function isTokenExpired(token) {
  try {
    const payload = token.split('.')[1];
    if (!payload) return false;
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const { exp } = JSON.parse(atob(padded));
    return typeof exp === 'number' && exp * 1000 <= Date.now();
  } catch {
    return false;
  }
}
