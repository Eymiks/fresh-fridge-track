export type DatePrecision = 'day' | 'month';

export interface ParsedExpirationDate {
  date: string;
  raw: string;
  precision: DatePrecision;
}

const MONTH_WORDS: Record<string, number> = {
  JANVIER: 1,
  JANV: 1,
  JAN: 1,
  JANUARY: 1,
  FEVRIER: 2,
  FEVR: 2,
  FEV: 2,
  FEBRUARY: 2,
  FEB: 2,
  MARS: 3,
  MAR: 3,
  MARCH: 3,
  AVRIL: 4,
  AVR: 4,
  APRIL: 4,
  APR: 4,
  MAI: 5,
  MAY: 5,
  JUIN: 6,
  JUN: 6,
  JUNE: 6,
  JUILLET: 7,
  JUIL: 7,
  JULY: 7,
  JUL: 7,
  AOUT: 8,
  AOU: 8,
  AUGUST: 8,
  AUG: 8,
  SEPTEMBRE: 9,
  SEPT: 9,
  SEP: 9,
  SEPTEMBER: 9,
  OCTOBRE: 10,
  OCT: 10,
  OCTOBER: 10,
  NOVEMBRE: 11,
  NOV: 11,
  NOVEMBER: 11,
  DECEMBRE: 12,
  DEC: 12,
  DECEMBER: 12,
};

function stripAccents(value: string) {
  return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}

function normalizeText(value: string) {
  return stripAccents(value)
    .toUpperCase()
    .replace(/[’']/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function correctCandidateDigits(value: string) {
  return value
    .replace(/[OoQ]/g, '0')
    .replace(/[Il|]/g, '1')
    .replace(/B/g, '8');
}

function resolveYear(value: string | number) {
  const year = typeof value === 'number' ? value : Number(value);
  if (!Number.isInteger(year)) return null;
  if (year < 100) return year <= 79 ? 2000 + year : 1900 + year;
  return year;
}

function lastDayOfMonth(year: number, month: number) {
  return new Date(year, month, 0).getDate();
}

function toIsoDate(year: number, month: number, day: number) {
  return [
    String(year).padStart(4, '0'),
    String(month).padStart(2, '0'),
    String(day).padStart(2, '0'),
  ].join('-');
}

function isValidDate(year: number, month: number, day: number, now: Date) {
  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) return false;
  if (month < 1 || month > 12) return false;
  if (day < 1 || day > lastDayOfMonth(year, month)) return false;

  const candidate = new Date(year, month - 1, day);
  const minDate = new Date(now.getFullYear() - 2, 0, 1);
  const maxDate = new Date(now.getFullYear() + 10, 11, 31);
  return candidate >= minDate && candidate <= maxDate;
}

export function normalizeDate(
  day: string | number,
  month: string | number,
  year: string | number,
  now = new Date(),
  precision: DatePrecision = 'day',
): string | null {
  const resolvedYear = resolveYear(year);
  const resolvedMonth = Number(month);
  const resolvedDay = precision === 'month' ? lastDayOfMonth(resolvedYear ?? 0, resolvedMonth) : Number(day);

  if (resolvedYear == null || !isValidDate(resolvedYear, resolvedMonth, resolvedDay, now)) return null;
  return toIsoDate(resolvedYear, resolvedMonth, resolvedDay);
}

function monthWordPattern() {
  return Object.keys(MONTH_WORDS).sort((a, b) => b.length - a.length).join('|');
}

function pushDate(
  results: ParsedExpirationDate[],
  raw: string,
  year: string | number,
  month: string | number,
  day: string | number,
  now: Date,
  precision: DatePrecision = 'day',
) {
  const date = normalizeDate(day, month, year, now, precision);
  if (date) results.push({ date, raw: raw.trim(), precision });
}

function parseFromTextVariant(text: string, now: Date) {
  const results: ParsedExpirationDate[] = [];
  const wordMonth = monthWordPattern();

  for (const match of text.matchAll(new RegExp(`\\b(\\d{1,2})\\s*(?:${wordMonth})\\s*(\\d{2,4})\\b`, 'g'))) {
    const monthWord = match[0].replace(match[1], '').replace(match[2], '').trim();
    pushDate(results, match[0], match[2], MONTH_WORDS[monthWord], match[1], now);
  }

  for (const match of text.matchAll(new RegExp(`\\b(?:${wordMonth})\\s*(\\d{2,4})\\b`, 'g'))) {
    const monthWord = match[0].replace(match[1], '').trim();
    pushDate(results, match[0], match[1], MONTH_WORDS[monthWord], 1, now, 'month');
  }

  for (const match of text.matchAll(/\b(\d{4})[/.\-\s](\d{1,2})[/.\-\s](\d{1,2})\b/g)) {
    pushDate(results, match[0], match[1], match[2], match[3], now);
  }

  for (const match of text.matchAll(/\b(\d{1,2})[/.\-\s](\d{1,2})[/.\-\s](\d{2,4})\b/g)) {
    pushDate(results, match[0], match[3], match[2], match[1], now);
  }

  for (const match of text.matchAll(/\b(\d{4})(\d{2})(\d{2})\b/g)) {
    pushDate(results, match[0], match[1], match[2], match[3], now);
  }

  for (const match of text.matchAll(/\b(?:EXP|DLC|DDM|BB|BEST BEFORE|USE BY)?\s*(\d{2})(\d{2})(\d{2}|\d{4})\b/g)) {
    pushDate(results, match[0], match[3], match[2], match[1], now);
  }

  for (const match of text.matchAll(/\b(\d{2})(\d{2})[/.\-\s](\d{2,4})\b/g)) {
    pushDate(results, match[0], match[3], match[2], match[1], now);
  }

  for (const match of text.matchAll(/\b(\d{1,2})[/.\-\s](\d{4})\b/g)) {
    pushDate(results, match[0], match[2], match[1], 1, now, 'month');
  }

  for (const match of text.matchAll(/\b(\d{4})[/.\-\s](\d{1,2})\b/g)) {
    pushDate(results, match[0], match[1], match[2], 1, now, 'month');
  }

  return results;
}

function sortByBestCandidate(a: ParsedExpirationDate, b: ParsedExpirationDate) {
  if (a.precision !== b.precision) return a.precision === 'day' ? -1 : 1;
  return a.date.localeCompare(b.date);
}

export function parseExpirationDate(text: string, now = new Date()): ParsedExpirationDate | null {
  const normalized = normalizeText(text);
  if (!normalized) return null;

  const corrected = normalized
    .split(/([^A-Z0-9/.\-\s])/)
    .map((part) => /[\dOoQIl|B]/.test(part) ? correctCandidateDigits(part) : part)
    .join('');

  const results = [
    ...parseFromTextVariant(normalized, now),
    ...parseFromTextVariant(corrected, now),
  ];

  const unique = Array.from(new Map(results.map((result) => [result.date, result])).values());
  return unique.sort(sortByBestCandidate)[0] ?? null;
}

export function preprocessDateImage(sourceCanvas: HTMLCanvasElement): HTMLCanvasElement {
  const output = document.createElement('canvas');
  output.width = Math.max(1, sourceCanvas.width * 2);
  output.height = Math.max(1, sourceCanvas.height * 2);

  const ctx = output.getContext('2d', { willReadFrequently: true });
  if (!ctx) return sourceCanvas;

  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(sourceCanvas, 0, 0, output.width, output.height);

  const image = ctx.getImageData(0, 0, output.width, output.height);
  for (let i = 0; i < image.data.length; i += 4) {
    const gray = image.data[i] * 0.299 + image.data[i + 1] * 0.587 + image.data[i + 2] * 0.114;
    const contrasted = Math.max(0, Math.min(255, (gray - 128) * 1.55 + 128));
    const value = contrasted > 170 ? 255 : contrasted < 78 ? 0 : contrasted;
    image.data[i] = value;
    image.data[i + 1] = value;
    image.data[i + 2] = value;
  }
  ctx.putImageData(image, 0, 0);

  return output;
}
