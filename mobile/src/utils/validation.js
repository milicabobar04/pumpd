const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const MAX_NAME = 45;
export const MAX_EMAIL = 100;

export function validateName(value, label) {
  const v = value.trim();
  if (!v) 
    return `${label} je obavezno.`;
  if (v.length > MAX_NAME) 
    return `${label} može imati najviše ${MAX_NAME} znakova.`;
  return undefined;
}

export function validateEmail(value) {
  const v = value.trim();
  if (!v) 
    return 'Email je obavezan.';
  if (v.length > MAX_EMAIL) 
    return `Email može imati najviše ${MAX_EMAIL} znakova.`;
  if (!EMAIL_RE.test(v)) 
    return 'Email nije ispravnog formata.';
  return undefined;
}

export function validateOptionalNumber(value, { label, min, max, integer = false, minExclusive = false }) {
  const v = value.trim().replace(',', '.');
  if (!v) 
    return undefined;

  const n = Number(v);
  if (Number.isNaN(n)) 
    return `${label} mora biti broj.`;
  if (integer && !Number.isInteger(n)) 
    return `${label} mora biti cijeli broj.`;
  const tooLow = minExclusive ? n <= min : n < min;
  if (tooLow || n > max) {
    return minExclusive
      ? `${label} mora biti veća od ${min} i najviše ${max}.`
      : `${label} mora biti između ${min} i ${max}.`;
  }
  return undefined;
}

export function toNumberOrNull(value) {
  const v = value.trim().replace(',', '.');
  return v ? Number(v) : null;
}

export function onlyErrors(errors) {
  return Object.fromEntries(Object.entries(errors).filter(([, msg]) => msg));
}