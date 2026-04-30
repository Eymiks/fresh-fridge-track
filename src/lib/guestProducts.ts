import { Product, ProductStatus } from '@/types/product';

export const GUEST_PRODUCTS_KEY = 'freshtrack-guest-products';

type ProductDraft = Omit<Product, 'id' | 'addedAt'>;
type ProductUpdate = Partial<Omit<Product, 'id' | 'addedAt'>>;
type StorageLike = Pick<Storage, 'getItem' | 'setItem' | 'removeItem'>;

function getStorage(storage?: StorageLike): StorageLike | null {
  if (storage) return storage;
  if (typeof window === 'undefined') return null;
  return window.localStorage;
}

function createGuestId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `guest_${Date.now().toString(36)}_${Math.random().toString(36).slice(2)}`;
}

export function sortProductsByExpiration(products: Product[]) {
  return [...products].sort(
    (a, b) => new Date(a.expirationDate).getTime() - new Date(b.expirationDate).getTime()
  );
}

export function readGuestProducts(storage?: StorageLike): Product[] {
  const target = getStorage(storage);
  if (!target) return [];

  try {
    const raw = target.getItem(GUEST_PRODUCTS_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter(product => product?.id && product?.name && product?.expirationDate) : [];
  } catch {
    return [];
  }
}

export function writeGuestProducts(products: Product[], storage?: StorageLike) {
  const target = getStorage(storage);
  if (!target) return;
  target.setItem(GUEST_PRODUCTS_KEY, JSON.stringify(products));
}

export function clearGuestProducts(storage?: StorageLike) {
  const target = getStorage(storage);
  if (!target) return;
  target.removeItem(GUEST_PRODUCTS_KEY);
}

export function hasGuestProducts(storage?: StorageLike) {
  return readGuestProducts(storage).length > 0;
}

export function createGuestProduct(product: ProductDraft, now = new Date()): Product {
  return {
    ...product,
    id: createGuestId(),
    addedAt: now.toISOString(),
    status: product.status ?? 'active',
    addedBy: 'guest',
    addedByName: 'Invité',
  };
}

export function addGuestProduct(product: ProductDraft, storage?: StorageLike) {
  const products = readGuestProducts(storage);
  const nextProduct = createGuestProduct(product);
  const next = [...products, nextProduct];
  writeGuestProducts(next, storage);
  return next;
}

export function updateGuestProduct(id: string, updates: ProductUpdate, storage?: StorageLike) {
  const next = readGuestProducts(storage).map(product =>
    product.id === id ? { ...product, ...updates } : product
  );
  writeGuestProducts(next, storage);
  return next;
}

export function removeGuestProduct(id: string, storage?: StorageLike) {
  const next = readGuestProducts(storage).filter(product => product.id !== id);
  writeGuestProducts(next, storage);
  return next;
}

export function setGuestProductStatus(id: string, status: ProductStatus, storage?: StorageLike) {
  const now = new Date().toISOString();
  const next = readGuestProducts(storage).map(product =>
    product.id === id
      ? {
          ...product,
          status,
          statusChangedAt: now,
          ...(status === 'active' ? { openedAt: undefined, daysAfterOpening: undefined } : {}),
        }
      : product
  );
  writeGuestProducts(next, storage);
  return next;
}
