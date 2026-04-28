import { useState, useEffect } from 'react';

const KEYS = {
  enabled: 'frigo-notif-enabled',
  days: 'frigo-notif-days',
};
const DEFAULT_DAYS = 3;

export function useNotificationSettings() {
  const [enabled, setEnabledState] = useState<boolean>(
    () => localStorage.getItem(KEYS.enabled) === 'true'
  );
  const [days, setDaysState] = useState<number>(
    () => parseInt(localStorage.getItem(KEYS.days) || String(DEFAULT_DAYS))
  );
  const [permission, setPermission] = useState<NotificationPermission>(
    () => ('Notification' in window ? Notification.permission : 'denied')
  );

  // Sync state when another hook instance writes to localStorage
  useEffect(() => {
    const handler = (e: StorageEvent) => {
      if (e.key === KEYS.enabled) setEnabledState(e.newValue === 'true');
      if (e.key === KEYS.days && e.newValue) setDaysState(parseInt(e.newValue));
    };
    window.addEventListener('storage', handler);
    return () => window.removeEventListener('storage', handler);
  }, []);

  const setEnabled = (val: boolean) => {
    setEnabledState(val);
    localStorage.setItem(KEYS.enabled, val ? 'true' : 'false');
    window.dispatchEvent(new StorageEvent('storage', { key: KEYS.enabled, newValue: val ? 'true' : 'false' }));
  };

  const setDays = (val: number) => {
    setDaysState(val);
    localStorage.setItem(KEYS.days, String(val));
    window.dispatchEvent(new StorageEvent('storage', { key: KEYS.days, newValue: String(val) }));
  };

  const requestPermission = async (): Promise<NotificationPermission> => {
    if (!('Notification' in window)) return 'denied';
    const result = await Notification.requestPermission();
    setPermission(result);
    if (result === 'granted') setEnabled(true);
    return result;
  };

  const isSupported = 'Notification' in window;

  return { enabled, days, permission, isSupported, setEnabled, setDays, requestPermission };
}
