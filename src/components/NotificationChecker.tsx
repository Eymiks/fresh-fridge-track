import { useEffect } from 'react';
import { useProducts } from '@/hooks/useProducts';
import { useNotificationSettings } from '@/hooks/useNotificationSettings';
import { getDaysUntilExpiration, getEffectiveExpirationDate } from '@/types/product';
import { showNotification } from '@/lib/swNotify';

const NOTIF_LAST_CHECK = 'frigo-notif-last-check';
const NOTIF_DONE_IDS = 'frigo-notif-done-ids';

export function NotificationChecker() {
  const { products } = useProducts();
  const { enabled, days } = useNotificationSettings();

  useEffect(() => {
    if (!enabled || !('Notification' in window) || Notification.permission !== 'granted') return;
    if (products.length === 0) return;

    const run = async () => {
      const today = new Date().toISOString().split('T')[0];
      const lastCheck = localStorage.getItem(NOTIF_LAST_CHECK);
      let doneIds: string[] = JSON.parse(localStorage.getItem(NOTIF_DONE_IDS) || '[]');

      if (lastCheck !== today) {
        doneIds = [];
        localStorage.setItem(NOTIF_LAST_CHECK, today);
      }

      const toNotify = products
        .filter(p => p.status !== 'consumed' && p.status !== 'thrown' && !doneIds.includes(p.id))
        .map(p => ({ product: p, daysLeft: getDaysUntilExpiration(getEffectiveExpirationDate(p)) }))
        .filter(({ daysLeft }) => daysLeft >= 0 && daysLeft <= days);

      if (toNotify.length === 0) return;

      for (const { product: p, daysLeft } of toNotify) {
        const body =
          daysLeft === 0 ? "Expire aujourd'hui !"
          : daysLeft === 1 ? 'Expire demain'
          : `Expire dans ${daysLeft} jours`;
        await showNotification(p.name, { body, icon: '/pwa-192x192.png' });
      }

      const newIds = [...new Set([...doneIds, ...toNotify.map(({ product: p }) => p.id)])];
      localStorage.setItem(NOTIF_DONE_IDS, JSON.stringify(newIds));
    };

    run().catch(console.error);
  }, [products, enabled, days]);

  return null;
}
