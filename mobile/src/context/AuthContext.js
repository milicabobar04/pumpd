import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { setAuthToken, setOnUnauthorized } from '../services/apiClient';
import { logout as logoutRequest } from '../services/authService';
import {
  loadSession,
  saveSession,
  clearSession,
  isTokenExpired,
} from '../services/sessionStorage';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // null - niko nije prijavljen, u suprotnom { token, user }
  const [session, setSession] = useState(null);
  // true dok se pri pokretanju čita sačuvana sesija iz SecureStore-a.
  // Bez ovoga bi se na trenutak prikazao ekran prijave i prijavljenom korisniku.
  const [isRestoring, setIsRestoring] = useState(true);

  // Pri pokretanju aplikacije: vrati sačuvanu sesiju ako token još važi.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const saved = await loadSession();
      if (cancelled) return;
      if (saved && !isTokenExpired(saved.token)) {
        setAuthToken(saved.token);
        setSession(saved);
      } else if (saved) {
        await clearSession();
      }
      setIsRestoring(false);
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  // Poslije uspješnog POST /auth/login (1.1).
  // loginResponse: { id, name, surname, email, role, token, mustChangePassword }
  const signIn = useCallback(async (loginResponse) => {
    const { token, ...user } = loginResponse;
    const next = { token, user };
    setAuthToken(token);
    setSession(next);
    await saveSession(next);
  }, []);

  // Samo lokalna odjava: briše token iz memorije i sa uređaja.
  // Koristi je apiClient kad server vrati 401 (token je istekao ili je nevažeći),
  // pa nema smisla zvati /auth/logout.
  const signOut = useCallback(async () => {
    setAuthToken(null);
    setSession(null);
    await clearSession();
  }, []);

  // Odjava na zahtjev korisnika (SU-04): prvo POST /auth/logout (1.2), pa lokalna odjava.
  // Ako server nije dostupan, korisnik se ipak odjavljuje na telefonu, jer je to njegova namjera;
  // token će svakako isteći na serveru.
  const logout = useCallback(async () => {
    try {
      await logoutRequest();
    } catch (e) {
      console.warn('POST /auth/logout nije uspio, odjava samo na uređaju:', e?.message);
    } finally {
      await signOut();
    }
  }, [signOut]);

  // Poslije uspješne promjene privremene lozinke (PUT /users/my-password).
  // RootNavigator tada pušta korisnika u dio aplikacije za njegovu ulogu.
  const markPasswordChanged = useCallback(async () => {
    if (!session) return;
    const next = { ...session, user: { ...session.user, mustChangePassword: false } };
    setSession(next);
    await saveSession(next);
  }, [session]);

  useEffect(() => {
    setOnUnauthorized(signOut);
  }, [signOut]);

  const value = useMemo(
    () => ({
      user: session?.user ?? null,
      token: session?.token ?? null,
      role: session?.user?.role ?? null,
      isSignedIn: session !== null,
      mustChangePassword: session?.user?.mustChangePassword === true,
      isRestoring,
      signIn,
      signOut,
      logout,
      markPasswordChanged,
    }),
    [session, isRestoring, signIn, signOut, logout, markPasswordChanged],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth() se mora koristiti unutar <AuthProvider>');
  }
  return ctx;
}
