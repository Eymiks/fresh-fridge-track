import { useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import type { LucideIcon } from 'lucide-react';
import {
  BarChart3,
  Bell,
  Check,
  Home,
  History,
  Info,
  LogOut,
  Menu,
  Moon,
  Settings,
  Share2,
  Sun,
  Users,
} from 'lucide-react';
import { toast } from 'sonner';
import { Sheet, SheetContent, SheetTitle } from '@/components/ui/sheet';
import { useAuth } from '@/contexts/AuthContext';
import { useAppearance } from '@/contexts/AppearanceContext';
import { useProducts } from '@/hooks/useProducts';
import { useNotificationSettings } from '@/hooks/useNotificationSettings';
import { getEffectiveExpirationDate, getExpirationStatus } from '@/types/product';
import { cn } from '@/lib/utils';

const navItems: { to: string; icon: LucideIcon; label: string; exact?: boolean }[] = [
  { to: '/', icon: Home, label: 'Accueil', exact: true },
  { to: '/stats', icon: BarChart3, label: 'Statistiques' },
  { to: '/notifications', icon: Bell, label: 'Notifications' },
  { to: '/history', icon: History, label: 'Historique' },
  { to: '/settings', icon: Settings, label: 'Paramètres' },
  { to: '/credits', icon: Info, label: 'Crédits' },
];

function isRouteActive(pathname: string, to: string, exact?: boolean) {
  if (exact) return pathname === to;
  return pathname === to || pathname.startsWith(`${to}/`);
}

export function MobileMenu() {
  const navigate = useNavigate();
  const location = useLocation();
  const { household, members, displayName, signOut, user } = useAuth();
  const { products } = useProducts();
  const { enabled: notificationsEnabled, permission, isSupported } = useNotificationSettings();
  const { themeMode, setThemeMode } = useAppearance();
  const isDark = themeMode === 'dark';
  const [open, setOpen] = useState(false);
  const [copied, setCopied] = useState(false);

  const myMember = members.find(member => member.user_id === user?.id);
  const isProductPage = location.pathname.startsWith('/product/');

  const activeProducts = useMemo(
    () => products.filter(product => product.status !== 'consumed' && product.status !== 'thrown'),
    [products]
  );

  const alertCount = useMemo(
    () => activeProducts.filter(product => {
      const status = getExpirationStatus(getEffectiveExpirationDate(product));
      return status === 'expired' || status === 'soon';
    }).length,
    [activeProducts]
  );

  const historyCount = useMemo(
    () => products.filter(product =>
      product.status === 'opened' || product.status === 'consumed' || product.status === 'thrown'
    ).length,
    [products]
  );

  const goTo = (to: string) => {
    setOpen(false);
    navigate(to);
  };

  const toggleDark = () => {
    setThemeMode(isDark ? 'light' : 'dark');
    setOpen(false);
  };

  const copyInviteCode = async () => {
    if (!household) return;
    await navigator.clipboard.writeText(household.invite_code);
    setCopied(true);
    toast.success('Code copié');
    window.setTimeout(() => setCopied(false), 1800);
  };

  const shareInviteCode = async () => {
    if (!household) return;

    const text = `Rejoins mon frigo sur FreshTrack ! Code d'invitation : ${household.invite_code}`;
    try {
      if (navigator.share) {
        await navigator.share({ title: 'FreshTrack', text });
      } else {
        await copyInviteCode();
      }
    } catch {
      await copyInviteCode();
    }
    setOpen(false);
  };

  const handleSignOut = async () => {
    setOpen(false);
    await signOut();
  };

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label="Ouvrir le menu"
        className={cn(
          'fixed right-5 z-40 flex h-10 w-10 items-center justify-center rounded-xl border shadow-sm backdrop-blur transition-colors active:scale-95',
          isProductPage
            ? 'top-6 border-white/25 bg-background/25 text-white hover:bg-background/45 dark:text-foreground'
            : 'top-11 border-border/70 bg-background/85 text-foreground hover:bg-muted'
        )}
      >
        <Menu className="h-5 w-5" />
        {alertCount > 0 && (
          <span className="absolute right-1.5 top-1.5 h-2.5 w-2.5 rounded-full bg-destructive ring-2 ring-background" />
        )}
      </button>

      <Sheet open={open} onOpenChange={setOpen}>
        <SheetContent side="right" className="flex w-80 max-w-[88vw] flex-col gap-0 overflow-y-auto p-0">
          <SheetTitle className="sr-only">Menu mobile</SheetTitle>

          <div className="border-b border-border px-5 pb-5 pt-10">
            <div className="flex items-center gap-3 pr-8">
              {myMember?.avatar_url ? (
                <img
                  src={myMember.avatar_url}
                  alt={displayName}
                  className="h-12 w-12 shrink-0 rounded-2xl object-cover shadow-sm"
                />
              ) : (
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary shadow-sm">
                  <span className="text-lg font-extrabold text-primary-foreground">
                    {(displayName || user?.email || 'F').charAt(0).toUpperCase()}
                  </span>
                </div>
              )}

              <button type="button" onClick={() => goTo('/settings')} className="min-w-0 flex-1 text-left">
                <p className="truncate font-extrabold text-foreground">{displayName || 'Mon profil'}</p>
                <p className="truncate text-xs font-semibold text-muted-foreground">
                  {household?.name ?? 'Foyer'}
                </p>
              </button>
            </div>

            <div className="mt-4 grid grid-cols-3 gap-2">
              <div className="rounded-xl bg-muted/60 px-2.5 py-2 text-center">
                <p className="text-base font-extrabold leading-none text-foreground">{activeProducts.length}</p>
                <p className="mt-1 text-[10px] font-semibold text-muted-foreground">produits</p>
              </div>
              <div className="rounded-xl bg-muted/60 px-2.5 py-2 text-center">
                <p className={cn('text-base font-extrabold leading-none', alertCount > 0 ? 'text-destructive' : 'text-foreground')}>
                  {alertCount}
                </p>
                <p className="mt-1 text-[10px] font-semibold text-muted-foreground">à surveiller</p>
              </div>
              <div className="rounded-xl bg-muted/60 px-2.5 py-2 text-center">
                <p className="text-base font-extrabold leading-none text-foreground">{members.length}</p>
                <p className="mt-1 text-[10px] font-semibold text-muted-foreground">membres</p>
              </div>
            </div>
          </div>

          <nav className="flex-1 px-3 py-3">
            {navItems.map(({ to, icon: Icon, label, exact }) => {
              const active = isRouteActive(location.pathname, to, exact);
              const badge = to === '/notifications' ? alertCount : to === '/history' ? historyCount : 0;
              const notificationHint =
                to === '/notifications' && badge === 0 && isSupported
                  ? permission === 'granted' && notificationsEnabled ? 'On' : 'Off'
                  : null;

              return (
                <button
                  key={to}
                  type="button"
                  onClick={() => goTo(to)}
                  className={cn(
                    'flex min-h-12 w-full items-center gap-3 rounded-xl px-4 py-3 text-left text-sm font-semibold transition-colors',
                    active
                      ? 'bg-primary/10 text-primary'
                      : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                  )}
                >
                  <Icon className="h-5 w-5 shrink-0" />
                  <span className="min-w-0 flex-1 truncate">{label}</span>
                  {badge > 0 ? (
                    <span className={cn(
                      'rounded-full px-2 py-0.5 text-xs font-extrabold',
                      to === '/notifications'
                        ? 'bg-destructive/10 text-destructive'
                        : 'bg-muted text-muted-foreground'
                    )}>
                      {badge}
                    </span>
                  ) : notificationHint ? (
                    <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-bold text-muted-foreground">
                      {notificationHint}
                    </span>
                  ) : null}
                </button>
              );
            })}
          </nav>

          <div className="border-t border-border px-3 py-3">
            <button
              type="button"
              onClick={() => goTo('/settings')}
              className="mb-2 flex w-full items-center gap-3 rounded-xl bg-muted/60 px-4 py-3 text-left transition-colors hover:bg-muted"
            >
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <Users className="h-4 w-4" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-bold text-foreground">{household?.name ?? 'Foyer'}</p>
                <p className="truncate text-xs text-muted-foreground">
                  {members.length} membre{members.length > 1 ? 's' : ''}
                </p>
              </div>
            </button>

            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={toggleDark}
                className="flex items-center justify-center gap-2 rounded-xl bg-muted/60 px-3 py-3 text-xs font-bold text-foreground transition-colors hover:bg-muted"
              >
                {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
                {isDark ? 'Clair' : 'Sombre'}
              </button>
              <button
                type="button"
                onClick={shareInviteCode}
                disabled={!household}
                className="flex items-center justify-center gap-2 rounded-xl bg-muted/60 px-3 py-3 text-xs font-bold text-foreground transition-colors hover:bg-muted disabled:opacity-40"
              >
                {copied ? <Check className="h-4 w-4 text-success" /> : <Share2 className="h-4 w-4" />}
                Inviter
              </button>
            </div>

            <button
              type="button"
              onClick={handleSignOut}
              className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl bg-destructive/10 px-4 py-3 text-sm font-bold text-destructive transition-colors hover:bg-destructive/20"
            >
              <LogOut className="h-4 w-4" />
              Se déconnecter
            </button>
          </div>
        </SheetContent>
      </Sheet>
    </>
  );
}
