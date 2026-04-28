import { useState, useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import { LeafyGreen, BarChart3, Bell, History, Info, Moon, Sun, Home, Users } from 'lucide-react';
import { useProducts } from '@/hooks/useProducts';
import { useAuth } from '@/contexts/AuthContext';
import { getExpirationStatus, getEffectiveExpirationDate } from '@/types/product';

const navItems = [
  { to: '/', icon: Home, label: 'Accueil', exact: true },
  { to: '/stats', icon: BarChart3, label: 'Statistiques' },
  { to: '/notifications', icon: Bell, label: 'Notifications' },
  { to: '/history', icon: History, label: 'Historique' },
  { to: '/household', icon: Users, label: 'Mon foyer' },
  { to: '/credits', icon: Info, label: 'Crédits' },
];

export function DesktopSidebar() {
  const { products } = useProducts();
  const { displayName } = useAuth();
  const [isDark, setIsDark] = useState(() => document.documentElement.classList.contains('dark'));

  useEffect(() => {
    const saved = localStorage.getItem('frigo-dark-mode');
    if (saved === 'true') {
      setIsDark(true);
      document.documentElement.classList.add('dark');
    }
  }, []);

  const alertCount = products.filter(p => {
    if (p.status === 'consumed' || p.status === 'thrown') return false;
    const s = getExpirationStatus(getEffectiveExpirationDate(p));
    return s === 'expired' || s === 'soon';
  }).length;

  const toggleDark = () => {
    const next = !isDark;
    setIsDark(next);
    document.documentElement.classList.toggle('dark', next);
    localStorage.setItem('frigo-dark-mode', next ? 'true' : 'false');
  };

  return (
    <aside className="w-56 shrink-0 h-screen bg-card border-r border-border flex flex-col">
      <div className="flex items-center gap-2.5 px-5 py-6 border-b border-border">
        <div className="w-9 h-9 bg-primary/10 rounded-xl flex items-center justify-center">
          <LeafyGreen className="w-5 h-5 text-primary" />
        </div>
        <span className="text-lg font-extrabold text-foreground">FreshTrack</span>
      </div>

      <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
        {navItems.map(({ to, icon: Icon, label, exact }) => (
          <NavLink
            key={to}
            to={to}
            end={exact}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold transition-colors ${
                isActive
                  ? 'bg-primary/10 text-primary'
                  : 'text-muted-foreground hover:bg-muted hover:text-foreground'
              }`
            }
          >
            <div className="relative shrink-0">
              <Icon className="w-4 h-4" />
              {to === '/notifications' && alertCount > 0 && (
                <span className="absolute -top-1 -right-1 w-2 h-2 bg-destructive rounded-full" />
              )}
            </div>
            {label}
            {to === '/notifications' && alertCount > 0 && (
              <span className="ml-auto text-[10px] font-bold text-destructive bg-destructive/10 px-1.5 py-0.5 rounded-full">
                {alertCount}
              </span>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="p-3 border-t border-border space-y-1">
        {displayName && (
          <div className="flex items-center gap-2.5 px-3 py-2">
            <div className="w-7 h-7 bg-primary/10 rounded-full flex items-center justify-center shrink-0">
              <span className="text-xs font-extrabold text-primary">{displayName.charAt(0).toUpperCase()}</span>
            </div>
            <span className="text-sm font-semibold text-foreground truncate">{displayName}</span>
          </div>
        )}
        <button
          onClick={toggleDark}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
        >
          {isDark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          {isDark ? 'Mode clair' : 'Mode sombre'}
        </button>
      </div>
    </aside>
  );
}
