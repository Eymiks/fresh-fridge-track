import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { supabase } from '@/integrations/supabase/client';
import { useAuth } from '@/contexts/AuthContext';
import { Product, ProductStatus } from '@/types/product';
import type { Tables } from '@/integrations/supabase/types';

type ProductRow = Tables<'products'>;

function dbToProduct(row: ProductRow, members: { user_id: string; display_name: string }[]): Product {
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

function productToDbUpdate(p: Partial<Omit<Product, 'id' | 'addedAt'>>) {
  const row: Tables<'products'>['Update'] = {};
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

export function useProducts() {
  const { household, user, members } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const instanceId = useRef(Math.random().toString(36).slice(2));
  const membersRef = useRef(members);
  membersRef.current = members;

  useEffect(() => {
    if (!household) { setProducts([]); setLoading(false); return; }

    setLoading(true);
    supabase
      .from('products')
      .select('*')
      .eq('household_id', household.id)
      .then(({ data }) => {
        if (data) setProducts(data.map(r => dbToProduct(r, membersRef.current)));
        setLoading(false);
      });

    const channel = supabase
      .channel(`products:${household.id}:${instanceId.current}`)
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'products', filter: `household_id=eq.${household.id}` },
        (payload) => {
          if (payload.eventType === 'INSERT') {
            setProducts(prev => [...prev, dbToProduct(payload.new as ProductRow, membersRef.current)]);
          } else if (payload.eventType === 'UPDATE') {
            setProducts(prev => prev.map(p =>
              p.id === (payload.new as ProductRow).id
                ? dbToProduct(payload.new as ProductRow, membersRef.current)
                : p
            ));
          } else if (payload.eventType === 'DELETE') {
            setProducts(prev => prev.filter(p => p.id !== (payload.old as { id: string }).id));
          }
        }
      )
      .subscribe();

    return () => { supabase.removeChannel(channel); };
  }, [household?.id]);

  const addProduct = useCallback(async (product: Omit<Product, 'id' | 'addedAt'>) => {
    if (!household || !user) return;
    await supabase.from('products').insert({
      household_id: household.id,
      added_by: user.id,
      name: product.name,
      barcode: product.barcode ?? null,
      expiration_date: product.expirationDate,
      image_url: product.imageUrl ?? null,
      brand: product.brand ?? null,
      nutri_score: product.nutriScore ?? null,
      category: product.category ?? null,
      subcategory: product.subcategory ?? null,
      status: 'active',
      quantity: product.quantity ?? null,
      nova_group: product.novaGroup ?? null,
      eco_score: product.ecoScore ?? null,
      allergens: product.allergens ?? null,
      ingredients: product.ingredients ?? null,
      days_after_opening: product.daysAfterOpening ?? null,
      nutrition_data: product.nutritionData ?? null,
    });
  }, [household, user]);

  const updateProduct = useCallback(async (id: string, updates: Partial<Omit<Product, 'id' | 'addedAt'>>) => {
    await supabase.from('products').update(productToDbUpdate(updates)).eq('id', id);
  }, []);

  const removeProduct = useCallback(async (id: string) => {
    await supabase.from('products').delete().eq('id', id);
  }, []);

  const setProductStatus = useCallback(async (id: string, status: ProductStatus) => {
    await supabase.from('products').update({
      status,
      status_changed_at: new Date().toISOString(),
      ...(status === 'active' ? { opened_at: null, days_after_opening: null } : {}),
    }).eq('id', id);
  }, []);

  const sortedProducts = useMemo(() => [...products].sort(
    (a, b) => new Date(a.expirationDate).getTime() - new Date(b.expirationDate).getTime()
  ), [products]);

  return { products: sortedProducts, loading, addProduct, updateProduct, removeProduct, setProductStatus };
}
