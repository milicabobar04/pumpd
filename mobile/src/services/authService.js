import apiClient from './apiClient';

// 1.1 POST /auth/login (SU-03)
// Vraća { id, name, surname, email, role, token, mustChangePassword }.
// Greška 401 (pogrešan email ili lozinka) ide dalje kao axios greška,
// pa je ekran prikazuje preko <ErrorMessage />.
export async function login(email, password) {
  const response = await apiClient.post('/auth/login', { email, password });
  return response.data;
}

// 1.2 POST /auth/logout (SU-04)
// Backend stavlja token na listu nevažećih, pa se više ne može koristiti
// ni ako ga neko sačuva. Token u zaglavlje dodaje apiClient.
export async function logout() {
  const response = await apiClient.post('/auth/logout');
  return response.data;
}
