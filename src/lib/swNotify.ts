async function getActiveRegistration(): Promise<ServiceWorkerRegistration> {
  if (!('serviceWorker' in navigator)) throw new Error('ServiceWorker non supporté');

  const existing = await navigator.serviceWorker.getRegistrations();
  for (const reg of existing) {
    if (reg.active) return reg;
  }

  const reg = await navigator.serviceWorker.register('/sw.js');
  if (reg.active) return reg;

  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('SW activation timeout (8s)')), 8000);
    // eslint-disable-next-line prefer-const
    let poll: ReturnType<typeof setInterval>;

    const done = () => {
      clearTimeout(timer);
      clearInterval(poll);
      resolve(reg);
    };

    const listenSW = (sw: ServiceWorker) => {
      sw.addEventListener('statechange', () => {
        if (sw.state === 'activated') done();
      });
    };

    if (reg.installing) listenSW(reg.installing);
    if (reg.waiting) listenSW(reg.waiting);
    reg.addEventListener('updatefound', () => {
      if (reg.installing) listenSW(reg.installing);
    });

    poll = setInterval(() => {
      if (reg.active) done();
    }, 200);
  });
}

export async function showNotification(title: string, options: NotificationOptions): Promise<void> {
  const reg = await getActiveRegistration();
  await reg.showNotification(title, options);
}
