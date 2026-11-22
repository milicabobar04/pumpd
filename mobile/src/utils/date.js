// Server: 'gggg-mm-dd' (LocalDate). Ekran: 'dd.mm.gggg.'
const pad = (n) => String(n).padStart(2, '0');

export function formatDate(iso) {
  if (!iso) return '–';
  const [y, m, d] = String(iso).slice(0, 10).split('-');
  return d ? `${d}.${m}.${y}.` : String(iso);
}

// 'dd.mm.gggg' ili 'dd.mm.gggg.' -> 'gggg-mm-dd'; neispravan ili nepostojeći datum -> null
export function parseDate(text) {
  const match = /^(\d{1,2})\.(\d{1,2})\.(\d{4})\.?$/.exec(String(text).trim());
  if (!match) return null;
  const d = Number(match[1]);
  const m = Number(match[2]);
  const y = Number(match[3]);
  const date = new Date(Date.UTC(y, m - 1, d));
  if (date.getUTCFullYear() !== y || date.getUTCMonth() !== m - 1 || date.getUTCDate() !== d) return null;
  return `${y}-${pad(m)}-${pad(d)}`;
}

export function todayIso() {
  const t = new Date();
  return `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`;
}