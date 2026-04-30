import { Product, ProductStatus } from '@/types/product';
import type { Tables, TablesInsert, TablesUpdate } from '@/integrations/supabase/types';

type ProductRow = Tables<'products'>;

export function dbToProduct(row: ProductRow, members: { user_id: string; display_name: string }[]): Product {
  return {
    id: row.id,
    name: row.name,
    barcode: row.barcode ?? undefined,
    expirationDate: row.expiration_date,
    addedAt: row.added_at,
    imageUrl: row.image_url ?? undefined,
    brand: row.brand ?? undefined,
    nutriScore: row.nutri_score ?? undefined,
    category: row.category ?? undefined,
    subcategory: row.subcategory ?? undefined,
    status: (row.status as ProductStatus) ?? 'active',
    statusChangedAt: row.status_changed_at ?? undefined,
    quantity: row.quantity ?? undefined,
    novaGroup: row.nova_group ?? undefined,
    ecoScore: row.eco_score ?? undefined,
    allergens: row.allergens ?? undefined,
    ingredients: row.ingredients ?? undefined,
    openedAt: row.opened_at ?? undefined,
    daysAfterOpening: row.days_after_opening ?? undefined,
    addedBy: row.added_by ?? undefined,
    addedByName: members.find(m => m.user_id === row.added_by)?.display_name,
    notes: row.notes ?? undefined,
    frozenUntil: row.frozen_until ?? undefined,
    nutritionData: row.nutrition_data ?? undefined,
  };
}

export function productToDbInsert(
  product: Omit<Product, 'id'> | Product,
  householdId: string,
  userId: string
): TablesInsert<'products'> {
  return {
    household_id: householdId,
    added_by: userId,
    name: product.name,
    barcode: product.barcode ?? null,
    expiration_date: product.expirationDate,
    added_at: product.addedAt,
    image_url: product.imageUrl ?? null,
    brand: product.brand ?? null,
    nutri_score: product.nutriScore ?? null,
    category: product.category ?? null,
    subcategory: product.subcategory ?? null,
    status: product.status ?? 'active',
    status_changed_at: product.statusChangedAt ?? null,
    quantity: product.quantity ?? null,
    nova_group: product.novaGroup ?? null,
    eco_score: product.ecoScore ?? null,
    allergens: product.allergens ?? null,
    ingredients: product.ingredients ?? null,
    opened_at: product.openedAt ?? null,
    days_after_opening: product.daysAfterOpening ?? null,
    notes: product.notes ?? null,
    frozen_until: product.frozenUntil ?? null,
    nutrition_data: product.nutritionData ?? null,
  };
}

export function productToDbUpdate(p: Partial<Omit<Product, 'id' | 'addedAt'>>) {
  const row: TablesUpdate<'products'> = {};
  if (p.name !== undefined) row.name = p.name;
  if (p.barcode !== undefined) row.barcode = p.barcode;
  if (p.expirationDate !== undefined) row.expiration_date = p.expirationDate;
  if (p.imageUrl !== undefined) row.image_url = p.imageUrl;
  if (p.brand !== undefined) row.brand = p.brand;
  if (p.nutriScore !== undefined) row.nutri_score = p.nutriScore;
  if (p.category !== undefined) row.category = p.category;
  if (p.subcategory !== undefined) row.subcategory = p.subcategory;
  if (p.status !== undefined) row.status = p.status;
  if (p.statusChangedAt !== undefined) row.status_changed_at = p.statusChangedAt;
  if (p.quantity !== undefined) row.quantity = p.quantity;
  if (p.novaGroup !== undefined) row.nova_group = p.novaGroup;
  if (p.ecoScore !== undefined) row.eco_score = p.ecoScore;
  if (p.allergens !== undefined) row.allergens = p.allergens;
  if (p.ingredients !== undefined) row.ingredients = p.ingredients;
  if (p.openedAt !== undefined) row.opened_at = p.openedAt;
  if (p.daysAfterOpening !== undefined) row.days_after_opening = p.daysAfterOpening;
  if (p.notes !== undefined) row.notes = p.notes ?? null;
  if (p.frozenUntil !== undefined) row.frozen_until = p.frozenUntil ?? null;
  if (p.nutritionData !== undefined) row.nutrition_data = p.nutritionData ?? null;
  return row;
}
