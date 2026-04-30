import { describe, expect, it } from 'vitest';
import { parseExpirationDate } from './dateOcr';

const NOW = new Date('2026-04-30T12:00:00.000Z');

describe('parseExpirationDate', () => {
  it.each([
    ['14/08/2026', '2026-08-14'],
    ['14-08-2026', '2026-08-14'],
    ['14.08.2026', '2026-08-14'],
    ['14 08 2026', '2026-08-14'],
    ['14/08/26', '2026-08-14'],
    ['14-08-26', '2026-08-14'],
    ['14.08.26', '2026-08-14'],
    ['2026-08-14', '2026-08-14'],
    ['2026/08/14', '2026-08-14'],
    ['20260814', '2026-08-14'],
    ['14 août 2026', '2026-08-14'],
    ['14 aout 2026', '2026-08-14'],
    ['14 AUG 2026', '2026-08-14'],
    ['AOUT 2026', '2026-08-31'],
    ['AOÛT 2026', '2026-08-31'],
    ['08/2026', '2026-08-31'],
    ['08-2026', '2026-08-31'],
    ['2026/08', '2026-08-31'],
    ['EXP 140826', '2026-08-14'],
    ['DLC 14082026', '2026-08-14'],
    ['BB 14 08 26', '2026-08-14'],
    ['14O8/2026', '2026-08-14'],
    ['I4/08/26', '2026-08-14'],
    ['14/0B/2026', '2026-08-14'],
  ])('parses %s', (input, expected) => {
    expect(parseExpirationDate(input, NOW)?.date).toBe(expected);
  });

  it('rejects impossible dates', () => {
    expect(parseExpirationDate('32/08/2026', NOW)).toBeNull();
    expect(parseExpirationDate('14/13/2026', NOW)).toBeNull();
  });

  it('rejects dates outside the useful product range', () => {
    expect(parseExpirationDate('14/08/1986', NOW)).toBeNull();
    expect(parseExpirationDate('14/08/2045', NOW)).toBeNull();
  });
});
