import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { supabase } from '@/integrations/supabase/client';
import { useAuth } from '@/contexts/AuthContext';
import { Product, ProductStatus } from '@/types/product';
import type { Tables } from '@/integrations/supabase/types';
import {
  addGuestProduct,
  GUEST_PRODUCTS_KEY,
  readGuestProducts,
  removeGuestProduct,
  setGuestProductStatus,
  sortProductsByExpiration,
  updateGuestProduct,
} from '@/lib/guestProducts';
import { dbToProduct, productToDbInsert, productToDbUpdate } from '@/lib/productDb';

type ProductRow = Tables<'products'>;

export function useProducts() {
  const { household, user, members, isGuest } = useAuth();
  const householdId = household?.id;
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const instanceId = useRef(Math.random().toString(36).slice(2));
  const membersRef = useRef(members);
  membersRef.current = members;

  useEffect(() => {
    if (isGuest) {
      setProducts(readGuestProducts());
      setLoading(false);

      const handleStorage = (event: StorageEvent) => {
        if (event.key === GUEST_PRODUCTS_KEY) setProducts(readGuestProducts());
      };
      window.addEventListener('storage', handleStorage);
      return () => window.removeEventListener('storage', handleStorage);
    }

    if (!householdId) { setProducts([]); setLoading(false); return; }

    setLoading(true);
    supabase
      .from('products')
      .select('*')
      .eq('household_id', householdId)
      .then(({ data }) => {
        if (data) setProducts(data.map(r => dbToProduct(r, membersRef.current)));
        setLoading(false);
      });

    const channel = supabase
      .channel(`products:${householdId}:${instanceId.current}`)
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'products', filter: `household_id=eq.${householdId}` },
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
  }, [householdId, isGuest]);

  const addProduct = useCallback(async (product: Omit<Product, 'id' | 'addedAt'>) => {
    if (isGuest) {
      const next = addGuestProduct(product);
      setProducts(next);
      return;
    }

    if (!household || !user) return;
    await supabase.from('products').insert(productToDbInsert({
      ...product,
      addedAt: new Date().toISOString(),
      status: 'active',
    }, household.id, user.id));
  }, [household, isGuest, user]);

  const updateProduct = useCallback(async (id: string, updates: Partial<Omit<Product, 'id' | 'addedAt'>>) => {
    if (isGuest) {
      const next = updateGuestProduct(id, updates);
      setProducts(next);
      return;
    }

    await supabase.from('products').update(productToDbUpdate(updates)).eq('id', id);
  }, [isGuest]);

  const removeProduct = useCallback(async (id: string) => {
    if (isGuest) {
      const next = removeGuestProduct(id);
      setProducts(next);
      return;
    }

    await supabase.from('products').delete().eq('id', id);
  }, [isGuest]);

  const setProductStatus = useCallback(async (id: string, status: ProductStatus) => {
    if (isGuest) {
      const next = setGuestProductStatus(id, status);
      setProducts(next);
      return;
    }

    await supabase.from('products').update({
      status,
      status_changed_at: new Date().toISOString(),
      ...(status === 'active' ? { opened_at: null, days_after_opening: null } : {}),
    }).eq('id', id);
  }, [isGuest]);

  const sortedProducts = useMemo(() => sortProductsByExpiration(products), [products]);

  return { products: sortedProducts, loading, addProduct, updateProduct, removeProduct, setProductStatus };
}
