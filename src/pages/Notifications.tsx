import { useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Bell, AlertTriangle, Clock, BellOff, BellRing } from 'lucide-react';
import { useProducts } from '@/hooks/useProducts';
import { useNotificationSettings } from '@/hooks/useNotificationSettings';
import { getExpirationStatus, getEffectiveExpirationDate } from '@/types/product';
import { ProductCard } from '@/components/ProductCard';
import { PageTransition } from '@/components/PageTransition';

const SEEN_KEY = 'frigo-seen-statuses';
const DAY_OPTIONS = [1, 3, 7];

const Notifications = () => {
  const navigate = useNavigate();
  const { products, removeProduct } = useProducts();
  const { enabled, days, permission, isSupported, setEnabled, setDays, requestPermission } =
    useNotificationSettings();
  const activeProducts = useMemo(
    () => products.filter(p => p.status !== 'consumed' && p.status !== 'thrown'),
    [products]
  );

  const expired = useMemo(
    () => activeProducts.filter(p => getExpirationStatus(getEffectiveExpirationDate(p)) === 'expired'),
    [activeProducts]
  );

  const soon = useMemo(
    () => activeProducts.filter(p => getExpirationStatus(getEffectiveExpirationDate(p)) === 'soon'),
    [activeProducts]
  );

  useEffect(() => {
    const ids = activeProducts
      .filter(p => {
        const s = getExpirationStatus(getEffectiveExpirationDate(p));
        return s === 'expired' || s === 'soon';
      })
      .map(p => p.id)
      .sort()
      .join(',');
    localStorage.setItem(SEEN_KEY, ids);
  }, [activeProducts]);

  const isEmpty = expired.length === 0 && soon.length === 0;

  return (
    <PageTransition>
      <div className="min-h-screen bg-background">
        {/* Header */}
        <div className="sticky top-0 z-20 bg-background/80 backdrop-blur-lg border-b border-border">
          <div className="flex items-center gap-3 px-4 py-3 pt-12">
            <button
              onClick={() => navigate(-1)}
              className="p-2 rounded-full hover:bg-muted transition-colors"
            >
              <ArrowLeft className="w-5 h-5 text-foreground" />
            </button>
            <h1 className="text-lg font-bold text-foreground">Notifications</h1>
          </div>
        </div>

        <div className="px-4 py-4 space-y-6">
          {/* Push notification settings */}
          <div className="bg-card border border-border rounded-2xl p-4 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <BellRing className="w-4 h-4 text-primary" />
                <span className="text-sm font-bold text-foreground">Notifications push</span>
              </div>
              {isSupported && permission === 'granted' && (
                <button
                  onClick={() => setEnabled(!enabled)}
                  className={`relative w-11 h-6 rounded-full transition-colors ${
                    enabled ? 'bg-primary' : 'bg-muted'
                  }`}
                >
                  <span
                    className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform ${
                      enabled ? 'translate-x-5' : 'translate-x-0'
                    }`}
                  />
                </button>
              )}
            </div>

            {!isSupported ? (
              <p className="text-xs text-muted-foreground">
                Non supporté sur cet appareil ou navigateur.
              </p>
            ) : permission === 'denied' ? (
              <div className="flex items-start gap-2">
                <BellOff className="w-4 h-4 text-destructive shrink-0 mt-0.5" />
                <p className="text-xs text-muted-foreground">
                  Notifications bloquées. Autorisez-les dans les paramètres du navigateur.
                </p>
              </div>
            ) : permission === 'default' ? (
              <button
                onClick={requestPermission}
                className="w-full flex items-center justify-center gap-2 bg-primary text-primary-foreground py-2.5 rounded-xl text-sm font-bold hover:bg-primary/90 transition-colors"
              >
                <Bell className="w-4 h-4" />
                Activer les notifications
              </button>
            ) : (
              <div className="space-y-3">
                <p className="text-xs text-muted-foreground font-semibold">Rappel avant expiration</p>
                <div className="flex gap-2">
                  {DAY_OPTIONS.map(d => (
                    <button
                      key={d}
                      onClick={() => setDays(d)}
                      disabled={!enabled}
                      className={`flex-1 py-2 rounded-xl text-xs font-bold transition-colors disabled:opacity-40 ${
                        days === d
                          ? 'bg-primary text-primary-foreground'
                          : 'bg-muted text-muted-foreground hover:bg-muted/80'
                      }`}
                    >
                      {d === 1 ? '1 jour' : `${d} jours`}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Product alerts */}
          {isEmpty ? (
            <div className="flex flex-col items-center justify-center py-16 text-muted-foreground">
              <Bell className="w-12 h-12 mb-3 opacity-40" />
              <p className="text-sm font-medium">Aucune alerte</p>
              <p className="text-xs mt-1">Tous vos produits sont frais !</p>
            </div>
          ) : (
            <>
              {expired.length > 0 && (
                <section>
                  <div className="flex items-center gap-2 mb-3">
                    <AlertTriangle className="w-4 h-4 text-destructive" />
                    <h2 className="text-sm font-bold text-destructive">Périmés ({expired.length})</h2>
                  </div>
                  <div className="space-y-3">
                    {expired.map(product => (
                      <ProductCard key={product.id} product={product} onRemove={removeProduct} />
                    ))}
                  </div>
                </section>
              )}

              {soon.length > 0 && (
                <section>
                  <div className="flex items-center gap-2 mb-3">
                    <Clock className="w-4 h-4 text-warning" />
                    <h2 className="text-sm font-bold text-warning">
                      Bientôt périmés ({soon.length})
                    </h2>
                  </div>
                  <div className="space-y-3">
                    {soon.map(product => (
                      <ProductCard key={product.id} product={product} onRemove={removeProduct} />
                    ))}
                  </div>
                </section>
              )}
            </>
          )}
        </div>
      </div>
    </PageTransition>
  );
};

export default Notifications;
