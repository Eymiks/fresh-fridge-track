import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  AlertTriangle,
  Bell,
  BellOff,
  BellRing,
  ChevronDown,
  CheckCircle2,
  Clock,
  Home,
} from 'lucide-react';
import { useProducts } from '@/hooks/useProducts';
import { useNotificationSettings } from '@/hooks/useNotificationSettings';
import { getExpirationStatus, getEffectiveExpirationDate } from '@/types/product';
import { ProductCard } from '@/components/ProductCard';
import { PageTransition } from '@/components/PageTransition';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group';
import { useIsMobile } from '@/hooks/use-mobile';
import { cn } from '@/lib/utils';

const SEEN_KEY = 'frigo-seen-statuses';
const DAY_OPTIONS = [1, 3, 7];
type AlertFilter = 'all' | 'expired' | 'soon';

const formatHeaderSubtitle = (expiredCount: number, soonCount: number) => {
  if (expiredCount > 0 && soonCount > 0) {
    return `${expiredCount} périmé${expiredCount > 1 ? 's' : ''} · ${soonCount} bientôt`;
  }
  if (expiredCount > 0) return `${expiredCount} périmé${expiredCount > 1 ? 's' : ''}`;
  if (soonCount > 0) return `${soonCount} bientôt périmé${soonCount > 1 ? 's' : ''}`;
  return 'Aucun produit à vérifier';
};

const Notifications = () => {
  const navigate = useNavigate();
  const isMobile = useIsMobile();
  const { products, setProductStatus, updateProduct } = useProducts();
  const { enabled, days, permission, isSupported, setEnabled, setDays, requestPermission } =
    useNotificationSettings();
  const [filter, setFilter] = useState<AlertFilter>('all');
  const [settingsOpen, setSettingsOpen] = useState(() => !isSupported || permission !== 'granted');

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
  const alertCount = expired.length + soon.length;
  const showExpired = filter === 'all' || filter === 'expired';
  const showSoon = filter === 'all' || filter === 'soon';
  const filteredCount = filter === 'expired' ? expired.length : filter === 'soon' ? soon.length : alertCount;
  const needsPushAttention = !isSupported || permission !== 'granted';
  const pushEnabled = isSupported && permission === 'granted' && enabled;
  const pushStatus = !isSupported
    ? 'Indisponible'
    : permission === 'denied'
      ? 'Bloquées'
      : permission === 'default'
        ? 'À activer'
        : enabled
          ? 'Activées'
          : 'En pause';
  const pushStatusClass = pushEnabled
    ? 'text-success bg-success/10'
    : permission === 'denied'
      ? 'text-destructive bg-destructive/10'
      : 'text-muted-foreground bg-muted';

  const renderProducts = (items: typeof activeProducts) => (
    <div className="space-y-3">
      {items.map(product => (
        <ProductCard
          key={product.id}
          product={product}
          onSetStatus={setProductStatus}
          onUpdateDate={(id, date) => updateProduct(id, { expirationDate: date })}
        />
      ))}
    </div>
  );

  return (
    <PageTransition>
      <div className={`min-h-screen bg-background ${isMobile ? 'pb-24' : 'pb-10'}`}>
        <div className={`sticky top-0 z-20 bg-background/85 backdrop-blur-lg border-b border-border ${isMobile ? 'pt-10' : 'pt-0'}`}>
          <div className="max-w-5xl mx-auto flex items-center gap-3 px-5 py-3 pr-20">
            {isMobile && (
              <button
                onClick={() => navigate(-1)}
                className="p-2 rounded-full hover:bg-muted transition-colors shrink-0"
              >
                <ArrowLeft className="w-5 h-5 text-foreground" />
              </button>
            )}

            <div className="min-w-0 flex-1">
              <h1 className="text-xl font-extrabold text-foreground truncate">Alertes</h1>
              <p className="text-xs font-semibold text-muted-foreground truncate">
                {formatHeaderSubtitle(expired.length, soon.length)}
              </p>
            </div>
          </div>
        </div>

        <main className="max-w-5xl mx-auto px-5 py-4 space-y-4">
          <div className="flex flex-wrap items-center gap-2 text-xs font-bold text-muted-foreground">
            {expired.length > 0 && (
              <button
                type="button"
                onClick={() => setFilter('expired')}
                className="inline-flex items-center gap-1.5 rounded-full bg-destructive/10 px-3 py-1.5 text-destructive"
              >
                <AlertTriangle className="h-3.5 w-3.5" />
                {expired.length} périmé{expired.length > 1 ? 's' : ''}
              </button>
            )}
            {soon.length > 0 && (
              <button
                type="button"
                onClick={() => setFilter('soon')}
                className="inline-flex items-center gap-1.5 rounded-full bg-warning/10 px-3 py-1.5 text-warning"
              >
                <Clock className="h-3.5 w-3.5" />
                {soon.length} bientôt
              </button>
            )}
            <button
              type="button"
              onClick={() => setSettingsOpen(open => !open)}
              className={cn('inline-flex items-center gap-1.5 rounded-full px-3 py-1.5', pushStatusClass)}
            >
              {pushEnabled ? <BellRing className="h-3.5 w-3.5" /> : <BellOff className="h-3.5 w-3.5" />}
              Push · {pushStatus}{permission === 'granted' ? ` · ${days}j` : ''}
            </button>
          </div>

          <section
            className={cn(
              'bg-card border border-border rounded-2xl transition-colors',
              needsPushAttention || settingsOpen ? 'p-4 sm:p-5 space-y-4' : 'p-3'
            )}
          >
            <button
              type="button"
              onClick={() => setSettingsOpen(open => !open)}
              className="flex w-full items-center justify-between gap-3 text-left"
            >
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-sm font-extrabold text-foreground">Notifications push</h2>
                  <span className={cn('rounded-full px-2 py-0.5 text-[10px] font-extrabold', pushStatusClass)}>
                    {pushStatus}{permission === 'granted' ? ` · ${days}j` : ''}
                  </span>
                </div>
                {(needsPushAttention || settingsOpen) && (
                  <p className="mt-1 text-xs text-muted-foreground">
                    Recevez un rappel avant qu'un produit arrive à expiration.
                  </p>
                )}
              </div>

              <div className="flex shrink-0 items-center gap-3">
                {isSupported && permission === 'granted' && (
                  <Switch
                    checked={enabled}
                    onCheckedChange={setEnabled}
                    aria-label="Notifications push"
                    onClick={event => event.stopPropagation()}
                  />
                )}
                <ChevronDown
                  className={cn(
                    'h-4 w-4 text-muted-foreground transition-transform',
                    settingsOpen && 'rotate-180'
                  )}
                />
              </div>
            </button>

            {(settingsOpen || needsPushAttention) && (
              <>
                {!isSupported ? (
                  <p className="text-xs text-muted-foreground">
                    Non supporté sur cet appareil ou navigateur.
                  </p>
                ) : permission === 'denied' ? (
                  <div className="flex items-start gap-2 rounded-xl bg-destructive/10 border border-destructive/20 px-3 py-2.5">
                    <BellOff className="w-4 h-4 text-destructive shrink-0 mt-0.5" />
                    <p className="text-xs text-muted-foreground">
                      Notifications bloquées. Autorisez-les dans les paramètres du navigateur.
                    </p>
                  </div>
                ) : permission === 'default' ? (
                  <Button onClick={requestPermission} className="w-full rounded-xl font-bold">
                    <Bell className="w-4 h-4" />
                    Activer les notifications
                  </Button>
                ) : (
                  <div className="space-y-3">
                    <p className="text-xs text-muted-foreground font-semibold">Rappel avant expiration</p>
                    <ToggleGroup
                      type="single"
                      value={String(days)}
                      onValueChange={value => value && setDays(Number(value))}
                      className="grid w-full grid-cols-3 gap-2"
                      variant="outline"
                    >
                      {DAY_OPTIONS.map(d => (
                        <ToggleGroupItem
                          key={d}
                          value={String(d)}
                          disabled={!enabled}
                          className="rounded-xl text-xs font-bold data-[state=on]:bg-primary data-[state=on]:text-primary-foreground"
                        >
                          {d === 1 ? '1 jour' : `${d} jours`}
                        </ToggleGroupItem>
                      ))}
                    </ToggleGroup>
                  </div>
                )}
              </>
            )}
          </section>

          <Tabs value={filter} onValueChange={value => setFilter(value as AlertFilter)} className="w-full">
            <TabsList className="grid w-full grid-cols-3 rounded-xl">
              <TabsTrigger value="all" className="rounded-lg text-xs font-bold">
                Tout
                <span className="ml-1.5 text-[10px] opacity-70">{alertCount}</span>
              </TabsTrigger>
              <TabsTrigger value="expired" className="rounded-lg text-xs font-bold">
                Périmés
                <span className="ml-1.5 text-[10px] opacity-70">{expired.length}</span>
              </TabsTrigger>
              <TabsTrigger value="soon" className="rounded-lg text-xs font-bold">
                Bientôt
                <span className="ml-1.5 text-[10px] opacity-70">{soon.length}</span>
              </TabsTrigger>
            </TabsList>
          </Tabs>

          {isEmpty ? (
            <div className="flex flex-col items-center justify-center rounded-2xl border border-border bg-card px-6 py-14 text-center">
              <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-success/10 text-success">
                <CheckCircle2 className="h-8 w-8" />
              </div>
              <h2 className="text-lg font-extrabold text-foreground">Tout est sous contrôle</h2>
              <p className="mt-1 max-w-sm text-sm text-muted-foreground">
                Aucun produit n'est périmé ou proche de sa date limite.
              </p>
              <Button onClick={() => navigate('/')} variant="outline" className="mt-5 rounded-xl font-bold">
                <Home className="h-4 w-4" />
                Retour au frigo
              </Button>
            </div>
          ) : filteredCount === 0 ? (
            <div className="rounded-2xl border border-border bg-card px-6 py-10 text-center">
              <Bell className="mx-auto mb-3 h-9 w-9 text-muted-foreground/50" />
              <p className="text-sm font-bold text-foreground">Aucune alerte dans ce filtre</p>
              <p className="mt-1 text-xs text-muted-foreground">Essayez l'onglet Tout pour voir les autres alertes.</p>
            </div>
          ) : (
            <div className="space-y-6">
              {showExpired && expired.length > 0 && (
                <section className="space-y-3">
                  <div className="flex items-center gap-2">
                    <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-destructive/10">
                      <AlertTriangle className="h-4 w-4 text-destructive" />
                    </div>
                    <h2 className="text-sm font-extrabold text-destructive flex-1">Périmés</h2>
                    <span className="rounded-full bg-destructive/10 px-2.5 py-1 text-xs font-extrabold text-destructive">
                      {expired.length}
                    </span>
                  </div>
                  {renderProducts(expired)}
                </section>
              )}

              {showSoon && soon.length > 0 && (
                <section className="space-y-3">
                  <div className="flex items-center gap-2">
                    <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-warning/10">
                      <Clock className="h-4 w-4 text-warning" />
                    </div>
                    <h2 className="text-sm font-extrabold text-warning flex-1">Bientôt périmés</h2>
                    <span className="rounded-full bg-warning/10 px-2.5 py-1 text-xs font-extrabold text-warning">
                      {soon.length}
                    </span>
                  </div>
                  {renderProducts(soon)}
                </section>
              )}
            </div>
          )}
        </main>
      </div>
    </PageTransition>
  );
};

export default Notifications;
