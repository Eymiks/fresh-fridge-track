import { describe, expect, it, beforeEach } from 'vitest';
import {
  addGuestProduct,
  clearGuestProducts,
  GUEST_PRODUCTS_KEY,
  readGuestProducts,
  removeGuestProduct,
  setGuestProductStatus,
  sortProductsByExpiration,
  updateGuestProduct,
  writeGuestProducts,
} from '@/lib/guestProducts';
import { Product } from '@/types/product';

function createMemoryStorage() {
  const values = new Map<string, string>();
  return {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
    removeItem: (key: string) => values.delete(key),
  };
}

describe('guestProducts', () => {
  let storage: ReturnType<typeof createMemoryStorage>;

  beforeEach(() => {
    storage = createMemoryStorage();
  });

  it('adds readable local products with guest metadata', () => {
    const next = addGuestProduct({
      name: 'Yaourt',
      expirationDate: '2026-05-03',
    }, storage);

    expect(next).toHaveLength(1);
    expect(next[0]).toMatchObject({
      name: 'Yaourt',
      expirationDate: '2026-05-03',
      status: 'active',
      addedBy: 'guest',
      addedByName: 'Invité',
    });
    expect(readGuestProducts(storage)[0].id).toBe(next[0].id);
  });

  it('updates, removes, and clears products', () => {
    const [product] = addGuestProduct({ name: 'Lait', expirationDate: '2026-05-01' }, storage);

    updateGuestProduct(product.id, { notes: 'Ouvert hier', quantity: '1L' }, storage);
    expect(readGuestProducts(storage)[0]).toMatchObject({ notes: 'Ouvert hier', quantity: '1L' });

    removeGuestProduct(product.id, storage);
    expect(readGuestProducts(storage)).toEqual([]);

    addGuestProduct({ name: 'Pain', expirationDate: '2026-05-02' }, storage);
    clearGuestProducts(storage);
    expect(readGuestProducts(storage)).toEqual([]);
  });

  it('sets status and clears opening fields when reverted to active', () => {
    const [product] = addGuestProduct({ name: 'Fromage', expirationDate: '2026-05-04' }, storage);

    updateGuestProduct(product.id, { openedAt: '2026-04-30T10:00:00.000Z', daysAfterOpening: 3 }, storage);
    setGuestProductStatus(product.id, 'consumed', storage);
    expect(readGuestProducts(storage)[0].status).toBe('consumed');

    setGuestProductStatus(product.id, 'active', storage);
    expect(readGuestProducts(storage)[0]).toMatchObject({ status: 'active' });
    expect(readGuestProducts(storage)[0].openedAt).toBeUndefined();
    expect(readGuestProducts(storage)[0].daysAfterOpening).toBeUndefined();
  });

  it('sorts by expiration date without mutating input', () => {
    const products: Product[] = [
      { id: '2', name: 'B', expirationDate: '2026-05-03', addedAt: '2026-04-30T00:00:00.000Z' },
      { id: '1', name: 'A', expirationDate: '2026-05-01', addedAt: '2026-04-30T00:00:00.000Z' },
    ];

    const sorted = sortProductsByExpiration(products);

    expect(sorted.map(product => product.id)).toEqual(['1', '2']);
    expect(products.map(product => product.id)).toEqual(['2', '1']);
  });

  it('returns an empty list for invalid persisted data', () => {
    storage.setItem(GUEST_PRODUCTS_KEY, '{nope');
    expect(readGuestProducts(storage)).toEqual([]);
  });

  it('filters malformed products from persisted data', () => {
    writeGuestProducts([
      { id: 'ok', name: 'Riz', expirationDate: '2026-06-01', addedAt: '2026-04-30T00:00:00.000Z' },
      { id: '', name: 'Broken', expirationDate: '2026-06-01', addedAt: '2026-04-30T00:00:00.000Z' },
    ], storage);

    expect(readGuestProducts(storage).map(product => product.id)).toEqual(['ok']);
  });
});
