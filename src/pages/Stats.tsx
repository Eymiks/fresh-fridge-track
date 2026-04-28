import {
  ArrowLeft, Apple, Milk, Beef, Fish, Croissant, Egg, Droplets, CupSoda,
  Snowflake, UtensilsCrossed, Archive, Wheat, Baby, Sparkles, Package,
  Leaf, Trash2, TrendingUp, TrendingDown, Flame, RefreshCw, RotateCcw,
} from 'lucide-react';
import type { ComponentType } from 'react';
import { useNavigate } from 'react-router-dom';
import { useProducts } from '@/hooks/useProducts';
import {
  getExpirationStatus, getDaysUntilExpiration, getEffectiveExpirationDate, PRODUCT_CATEGORIES,
} from '@/types/product';
import { PageTransition } from '@/components/PageTransition';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Tooltip, AreaChart, Area } from 'recharts';
import { subMonths, format, startOfMonth, endOfMonth, isWithinInterval, differenceInDays } from 'date-fns';
import { fr } from 'date-fns/locale';

const ICON_MAP: Record<string, ComponentType<{ className?: string }>> = {
  Apple, Milk, Beef, Fish, Croissant, Egg, Droplets, CupSoda,
  Snowflake, UtensilsCrossed, Archive, Wheat, Baby, Sparkles, Package,
};

const MEDALS = ['🥇', '🥈', '🥉'];

function scoreColor(s: number) {
  return s > 75 ? 'hsl(142, 71%, 45%)' : s > 50 ? 'hsl(38, 92%, 50%)' : 'hsl(0, 84%, 60%)';
}
function scoreLabel(s: number) {
  return s > 90 ? 'Excellent !' : s > 75 ? 'Très bien' : s > 50 ? 'Bien' : 'À améliorer';
}

function CategoryIcon({ categoryKey, className }: { categoryKey?: string; className?: string }) {
  const cat = PRODUCT_CATEGORIES.find(c => c.key === categoryKey);
  const Icon = (cat ? ICON_MAP[cat.icon as string] : null) ?? Package;
  return <Icon className={className ?? 'w-4 h-4 text-muted-foreground'} />;
}

const Stats = () => {
  const navigate = useNavigate();
  const { products } = useProducts();
  const now = new Date();

  // ── Base groups ──────────────────────────────────────────────────────────
  const activeProducts = products.filter(p => !p.status || p.status === 'active' || p.status === 'opened');
  const consumed = products.filter(p => p.status === 'consumed');
  const thrown = products.filter(p => p.status === 'thrown');

  // ── Frigo ─────────────────────────────────────────────────────────────────
  const expired = activeProducts.filter(p => getExpirationStatus(getEffectiveExpirationDate(p)) === 'expired');
  const soon    = activeProducts.filter(p => getExpirationStatus(getEffectiveExpirationDate(p)) === 'soon');
  const fresh   = activeProducts.filter(p => getExpirationStatus(getEffectiveExpirationDate(p)) === 'fresh');
  const total   = activeProducts.length;

  const urgentProducts = [...activeProducts]
    .sort((a, b) =>
      new Date(getEffectiveExpirationDate(a)).getTime() - new Date(getEffectiveExpirationDate(b)).getTime()
    )
    .slice(0, 5);

  const catGridMap: Record<string, number> = {};
  activeProducts.forEach(p => {
    const key = p.category || 'autre';
    catGridMap[key] = (catGridMap[key] || 0) + 1;
  });

  // ── Monthly data (6 months, shared across tabs) ────────────────────────
  const monthlyData = Array.from({ length: 6 }, (_, i) => {
    const date  = subMonths(now, 5 - i);
    const start = startOfMonth(date);
    const end   = endOfMonth(date);
    const mConsumed = products.filter(p =>
      p.status === 'consumed' && p.statusChangedAt &&
      isWithinInterval(new Date(p.statusChangedAt), { start, end })
    ).length;
    const mThrown = products.filter(p =>
      p.status === 'thrown' && p.statusChangedAt &&
      isWithinInterval(new Date(p.statusChangedAt), { start, end })
    ).length;
    const mAdded = products.filter(p =>
      isWithinInterval(new Date(p.addedAt), { start, end })
    ).length;
    const mTotal = mConsumed + mThrown;
    return {
      month:    format(date, 'MMM', { locale: fr }),
      score:    mTotal > 0 ? Math.round((mConsumed / mTotal) * 100) : null,
      consumed: mConsumed,
      thrown:   mThrown,
      added:    mAdded,
    };
  });

  // ── Anti-Gaspi ────────────────────────────────────────────────────────────
  const currentMonth = monthlyData[5];
  const prevMonth    = monthlyData[4];
  const currentScore = currentMonth.score;
  const trend = currentScore !== null && prevMonth.score !== null ? currentScore - prevMonth.score : null;

  const streak = (() => {
    let count = 0;
    for (let i = monthlyData.length - 1; i >= 0; i--) {
      const { score } = monthlyData[i];
      if (score === null) continue;
      if (score >= 75) count++;
      else break;
    }
    return count;
  })();

  const utilizedRates = consumed
    .filter(p => p.statusChangedAt && p.addedAt)
    .map(p => {
      const lifeTotal = differenceInDays(new Date(p.expirationDate), new Date(p.addedAt));
      const lifeUsed  = differenceInDays(new Date(p.statusChangedAt!), new Date(p.addedAt));
      return lifeTotal > 0 ? Math.min(100, Math.round((lifeUsed / lifeTotal) * 100)) : null;
    })
    .filter((v): v is number => v !== null);
  const avgUtilization = utilizedRates.length > 0
    ? Math.round(utilizedRates.reduce((a, b) => a + b, 0) / utilizedRates.length)
    : null;

  const catWasteMap: Record<string, { consumed: number; thrown: number }> = {};
  products.forEach(p => {
    if (p.status !== 'consumed' && p.status !== 'thrown') return;
    const key = p.category || 'autre';
    if (!catWasteMap[key]) catWasteMap[key] = { consumed: 0, thrown: 0 };
    if (p.status === 'consumed') catWasteMap[key].consumed++;
    else catWasteMap[key].thrown++;
  });
  const catScores = Object.entries(catWasteMap)
    .filter(([, { consumed: c, thrown: t }]) => c + t >= 2)
    .map(([key, { consumed: c, thrown: t }]) => {
      const cat = PRODUCT_CATEGORIES.find(cat => cat.key === key);
      return {
        label: cat?.label ?? (key.charAt(0).toUpperCase() + key.slice(1)),
        score: Math.round((c / (c + t)) * 100),
      };
    })
    .sort((a, b) => a.score - b.score);

  const throwMap: Record<string, { name: string; count: number }> = {};
  thrown.forEach(p => {
    const key = p.name.toLowerCase().trim();
    if (!throwMap[key]) throwMap[key] = { name: p.name, count: 0 };
    throwMap[key].count++;
  });
  const topThrown = Object.values(throwMap).sort((a, b) => b.count - a.count).slice(0, 3);

  // ── Tendances ─────────────────────────────────────────────────────────────
  const daysToConsume = consumed
    .filter(p => p.statusChangedAt)
    .map(p => differenceInDays(new Date(p.statusChangedAt!), new Date(p.addedAt)));
  const avgDaysToConsume = daysToConsume.length > 0
    ? Math.round(daysToConsume.reduce((a, b) => a + b, 0) / daysToConsume.length)
    : null;

  const recurrentMap: Record<string, { name: string; count: number }> = {};
  products.forEach(p => {
    const key = p.barcode || p.name.toLowerCase().trim();
    if (!recurrentMap[key]) recurrentMap[key] = { name: p.name, count: 0 };
    recurrentMap[key].count++;
  });
  const topRecurrent = Object.values(recurrentMap)
    .filter(({ count }) => count >= 2)
    .sort((a, b) => b.count - a.count)
    .slice(0, 3);

  // ── SVG gauge constants ────────────────────────────────────────────────
  const R = 75;
  const halfCirc = Math.PI * R;
  const gaugeColor = currentScore !== null ? scoreColor(currentScore) : 'hsl(var(--muted-foreground))';

  return (
    <PageTransition>
      <div className="min-h-screen bg-background pb-20">
        <div className="flex items-center gap-3 px-5 pt-12 pb-4">
          <button
            onClick={() => navigate('/')}
            className="p-2 rounded-full bg-card border border-border hover:bg-muted transition-colors"
          >
            <ArrowLeft className="w-5 h-5 text-foreground" />
          </button>
          <h1 className="text-xl font-extrabold text-foreground">Statistiques</h1>
        </div>

        <Tabs defaultValue="frigo" className="px-5">
          <TabsList className="grid grid-cols-3 mb-6">
            <TabsTrigger value="frigo">Frigo</TabsTrigger>
            <TabsTrigger value="antigaspi">Anti-Gaspi</TabsTrigger>
            <TabsTrigger value="tendances">Tendances</TabsTrigger>
          </TabsList>

          {/* ────────────────────── FRIGO ──────────────────────────────────── */}
          <TabsContent value="frigo" className="space-y-4">
            {/* Hero card */}
            <div className="bg-card border border-border rounded-2xl p-5">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-1">En stock</p>
              <p className="text-5xl font-extrabold text-card-foreground mb-4">{total}</p>
              {total > 0 ? (
                <>
                  <div className="flex h-3 rounded-full overflow-hidden gap-0.5">
                    {expired.length > 0 && (
                      <div className="bg-destructive rounded-l-full" style={{ width: `${(expired.length / total) * 100}%` }} />
                    )}
                    {soon.length > 0 && (
                      <div className="bg-warning" style={{ width: `${(soon.length / total) * 100}%` }} />
                    )}
                    {fresh.length > 0 && (
                      <div className="bg-success rounded-r-full" style={{ width: `${(fresh.length / total) * 100}%` }} />
                    )}
                  </div>
                  <div className="flex gap-4 mt-2.5">
                    {expired.length > 0 && (
                      <span className="flex items-center gap-1 text-[10px] font-semibold text-muted-foreground">
                        <span className="w-2 h-2 rounded-full bg-destructive inline-block" />{expired.length} périmé{expired.length > 1 ? 's' : ''}
                      </span>
                    )}
                    {soon.length > 0 && (
                      <span className="flex items-center gap-1 text-[10px] font-semibold text-muted-foreground">
                        <span className="w-2 h-2 rounded-full bg-warning inline-block" />{soon.length} bientôt
                      </span>
                    )}
                    {fresh.length > 0 && (
                      <span className="flex items-center gap-1 text-[10px] font-semibold text-muted-foreground">
                        <span className="w-2 h-2 rounded-full bg-success inline-block" />{fresh.length} frais
                      </span>
                    )}
                  </div>
                </>
              ) : (
                <p className="text-sm text-muted-foreground">Aucun produit en stock</p>
              )}
            </div>

            {/* Urgent products */}
            {urgentProducts.length > 0 && (
              <div className="bg-card border border-border rounded-2xl p-4">
                <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-3">
                  Bientôt à expirer
                </p>
                <div className="space-y-3">
                  {urgentProducts.map(p => {
                    const effectiveDate = getEffectiveExpirationDate(p);
                    const daysLeft = getDaysUntilExpiration(effectiveDate);
                    const status   = getExpirationStatus(effectiveDate);
                    const addedTs  = new Date(p.addedAt).getTime();
                    const expTs    = new Date(effectiveDate).getTime();
                    const nowTs    = now.getTime();
                    const lifeTotal = expTs - addedTs;
                    const lifePct   = lifeTotal > 0
                      ? Math.min(100, Math.max(0, Math.round(((nowTs - addedTs) / lifeTotal) * 100)))
                      : 100;

                    const badgeText = daysLeft <= 0 ? 'Périmé' : daysLeft === 1 ? 'Demain' : `${daysLeft}j`;
                    const badgeClass = status === 'expired'
                      ? 'bg-destructive text-destructive-foreground'
                      : status === 'soon'
                      ? 'bg-warning/20 text-warning'
                      : 'bg-success/10 text-success';
                    const barClass = lifePct >= 80 ? 'bg-destructive' : lifePct >= 55 ? 'bg-warning' : 'bg-primary';

                    return (
                      <div key={p.id}>
                        <div className="flex items-center gap-2 mb-1.5">
                          {p.imageUrl ? (
                            <img src={p.imageUrl} alt={p.name} className="w-8 h-8 rounded-lg object-cover flex-shrink-0" />
                          ) : (
                            <div className="w-8 h-8 rounded-lg bg-muted flex items-center justify-center flex-shrink-0">
                              <CategoryIcon categoryKey={p.category} />
                            </div>
                          )}
                          <span className="flex-1 text-sm font-semibold text-card-foreground truncate">{p.name}</span>
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${badgeClass}`}>{badgeText}</span>
                        </div>
                        <div className="h-1.5 bg-muted rounded-full overflow-hidden ml-10">
                          <div className={`h-full rounded-full transition-all ${barClass}`} style={{ width: `${lifePct}%` }} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Category grid */}
            {Object.keys(catGridMap).length > 0 && (
              <div className="bg-card border border-border rounded-2xl p-4">
                <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-3">Par catégorie</p>
                <div className="grid grid-cols-3 gap-2">
                  {PRODUCT_CATEGORIES
                    .filter(c => c.key !== 'all' && catGridMap[c.key])
                    .map(cat => {
                      const IconComp = ICON_MAP[cat.icon as string] ?? Package;
                      return (
                        <div key={cat.key} className="flex flex-col items-center gap-1 p-2.5 bg-muted/40 rounded-xl">
                          <IconComp className="w-5 h-5 text-primary" />
                          <span className="text-lg font-extrabold text-card-foreground">{catGridMap[cat.key]}</span>
                          <span className="text-[9px] font-semibold text-muted-foreground text-center leading-tight">{cat.label}</span>
                        </div>
                      );
                    })
                  }
                </div>
              </div>
            )}

            {total === 0 && (
              <div className="text-center py-16 text-muted-foreground">
                <Package className="w-10 h-10 mx-auto mb-3 opacity-30" />
                <p className="text-sm">Aucun produit en stock</p>
              </div>
            )}
          </TabsContent>

          {/* ─────────────────── ANTI-GASPI ────────────────────────────────── */}
          <TabsContent value="antigaspi" className="space-y-4">
            {/* SVG semicircle gauge */}
            <div className="bg-card border border-border rounded-2xl p-5 flex flex-col items-center">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-2 self-start">
                Score Anti-Gaspi — ce mois
              </p>
              {currentScore !== null ? (
                <>
                  <svg viewBox="0 0 200 115" className="w-full max-w-[220px]">
                    <circle
                      cx="100" cy="110" r={R}
                      fill="none"
                      stroke="hsl(var(--muted))"
                      strokeWidth="20"
                      strokeDasharray={`${halfCirc} 9999`}
                      strokeLinecap="round"
                      transform="rotate(-180 100 110)"
                    />
                    <circle
                      cx="100" cy="110" r={R}
                      fill="none"
                      stroke={gaugeColor}
                      strokeWidth="20"
                      strokeDasharray={`${(currentScore / 100) * halfCirc} 9999`}
                      strokeLinecap="round"
                      transform="rotate(-180 100 110)"
                    />
                    <text x="100" y="82" textAnchor="middle" fontSize={34} fontWeight={800} fill="hsl(var(--card-foreground))">{currentScore}%</text>
                    <text x="100" y="101" textAnchor="middle" fontSize={12} fontWeight={600} fill={gaugeColor}>{scoreLabel(currentScore)}</text>
                  </svg>
                  {trend !== null && (
                    <div className={`flex items-center gap-1 text-sm font-semibold -mt-1 ${trend >= 0 ? 'text-success' : 'text-destructive'}`}>
                      {trend >= 0
                        ? <TrendingUp className="w-4 h-4" />
                        : <TrendingDown className="w-4 h-4" />
                      }
                      {trend > 0 ? '+' : ''}{trend}% vs mois dernier
                    </div>
                  )}
                </>
              ) : (
                <p className="text-sm text-muted-foreground text-center py-8">
                  Marquez des produits comme consommés ou jetés pour voir votre score.
                </p>
              )}
            </div>

            {/* 3 metric cards */}
            <div className="grid grid-cols-3 gap-2">
              <div className="bg-card border border-border rounded-2xl p-3 text-center">
                <Flame className="w-5 h-5 mx-auto mb-1 text-warning" />
                <p className="text-2xl font-extrabold text-card-foreground">{streak}</p>
                <p className="text-[9px] font-semibold text-muted-foreground leading-tight mt-0.5">Mois ≥75% consécutifs</p>
              </div>
              <div className="bg-card border border-border rounded-2xl p-3 text-center">
                <Leaf className="w-5 h-5 mx-auto mb-1 text-success" />
                <p className="text-2xl font-extrabold text-card-foreground">
                  {avgUtilization !== null ? `${avgUtilization}%` : '—'}
                </p>
                <p className="text-[9px] font-semibold text-muted-foreground leading-tight mt-0.5">Taux d'utilisation</p>
              </div>
              <div className="bg-card border border-border rounded-2xl p-3 text-center">
                <UtensilsCrossed className="w-5 h-5 mx-auto mb-1 text-primary" />
                <p className="text-2xl font-extrabold text-card-foreground">
                  {currentMonth.consumed + currentMonth.thrown > 0
                    ? `${currentMonth.consumed}/${currentMonth.thrown}`
                    : '—'}
                </p>
                <p className="text-[9px] font-semibold text-muted-foreground leading-tight mt-0.5">Conso/Jetés ce mois</p>
              </div>
            </div>

            {/* 6-month trend area chart */}
            {monthlyData.some(d => d.score !== null) && (
              <div className="bg-card border border-border rounded-2xl p-4">
                <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-3">Tendance 6 mois</p>
                <ResponsiveContainer width="100%" height={90}>
                  <AreaChart data={monthlyData.map(d => ({ ...d, score: d.score ?? 0 }))}>
                    <defs>
                      <linearGradient id="gaspiGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="hsl(142, 71%, 45%)" stopOpacity={0.3} />
                        <stop offset="100%" stopColor="hsl(142, 71%, 45%)" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <XAxis dataKey="month" tick={{ fontSize: 10, fill: 'hsl(var(--muted-foreground))' }} axisLine={false} tickLine={false} />
                    <Area type="monotone" dataKey="score" stroke="hsl(142, 71%, 45%)" strokeWidth={2} fill="url(#gaspiGrad)" dot={false} />
                    <Tooltip formatter={(v: number) => [`${v}%`, 'Score']} />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            )}

            {/* Category waste scores */}
            {catScores.length > 0 && (
              <div className="bg-card border border-border rounded-2xl p-4">
                <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-3">Par catégorie</p>
                <div className="space-y-3">
                  {catScores.map(({ label, score }) => {
                    const barClass  = score > 75 ? 'bg-success' : score > 50 ? 'bg-warning' : 'bg-destructive';
                    const textClass = score > 75 ? 'text-success' : score > 50 ? 'text-warning' : 'text-destructive';
                    return (
                      <div key={label}>
                        <div className="flex justify-between items-center mb-1">
                          <span className="text-xs font-semibold text-card-foreground">{label}</span>
                          <span className={`text-xs font-bold ${textClass}`}>{score}%</span>
                        </div>
                        <div className="h-2 bg-muted rounded-full overflow-hidden">
                          <div className={`h-full rounded-full ${barClass}`} style={{ width: `${score}%` }} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Top wasted products */}
            {topThrown.length > 0 && (
              <div className="bg-card border border-border rounded-2xl p-4">
                <div className="flex items-center gap-2 mb-3">
                  <Trash2 className="w-4 h-4 text-destructive" />
                  <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Produits les plus gaspillés</p>
                </div>
                <div className="space-y-2">
                  {topThrown.map(({ name, count }, i) => (
                    <div
                      key={name}
                      className={`flex items-center justify-between p-2.5 rounded-xl ${count >= 3 ? 'bg-destructive/5' : 'bg-muted/50'}`}
                    >
                      <div className="flex items-center gap-2">
                        <span className="text-base">{MEDALS[i]}</span>
                        <span className={`text-sm font-semibold ${count >= 3 ? 'text-destructive' : 'text-card-foreground'}`}>{name}</span>
                      </div>
                      <span className={`text-xs font-bold ${count >= 3 ? 'text-destructive' : 'text-muted-foreground'}`}>×{count}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {consumed.length === 0 && thrown.length === 0 && (
              <div className="text-center py-12 text-muted-foreground">
                <Leaf className="w-10 h-10 mx-auto mb-3 opacity-30" />
                <p className="text-sm">Marquez des produits pour voir vos stats anti-gaspi</p>
              </div>
            )}
          </TabsContent>

          {/* ─────────────────── TENDANCES ─────────────────────────────────── */}
          <TabsContent value="tendances" className="space-y-4">
            {/* Grouped bar chart: added / consumed / thrown */}
            <div className="bg-card border border-border rounded-2xl p-4">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-2">Ajouts vs Consommations</p>
              <div className="flex gap-4 mb-3">
                <span className="flex items-center gap-1.5 text-[10px] font-semibold text-muted-foreground">
                  <span className="w-2 h-2 rounded-full bg-primary inline-block" />Ajoutés
                </span>
                <span className="flex items-center gap-1.5 text-[10px] font-semibold text-muted-foreground">
                  <span className="w-2 h-2 rounded-full bg-success inline-block" />Consommés
                </span>
                <span className="flex items-center gap-1.5 text-[10px] font-semibold text-muted-foreground">
                  <span className="w-2 h-2 rounded-full bg-destructive inline-block" />Jetés
                </span>
              </div>
              <ResponsiveContainer width="100%" height={150}>
                <BarChart data={monthlyData} barSize={8} barGap={2} barCategoryGap="30%">
                  <XAxis dataKey="month" tick={{ fontSize: 10, fill: 'hsl(var(--muted-foreground))' }} axisLine={false} tickLine={false} />
                  <YAxis hide />
                  <Tooltip />
                  <Bar dataKey="added"    name="Ajoutés"    fill="hsl(var(--primary))"    radius={[4, 4, 0, 0]} />
                  <Bar dataKey="consumed" name="Consommés"  fill="hsl(142, 71%, 45%)"     radius={[4, 4, 0, 0]} />
                  <Bar dataKey="thrown"   name="Jetés"      fill="hsl(0, 84%, 60%)"       radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            {/* Average days to consume */}
            <div className="bg-card border border-border rounded-2xl p-5">
              <div className="flex items-center gap-2 mb-2">
                <RefreshCw className="w-4 h-4 text-primary" />
                <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Durée moyenne avant consommation</p>
              </div>
              {avgDaysToConsume !== null ? (
                <>
                  <p className="text-4xl font-extrabold text-card-foreground">
                    {avgDaysToConsume}
                    <span className="text-base font-semibold text-muted-foreground ml-1.5">jours</span>
                  </p>
                  <p className="text-xs text-muted-foreground mt-1">
                    En moyenne après l'ajout au frigo
                  </p>
                </>
              ) : (
                <p className="text-sm text-muted-foreground py-2">Pas encore de données</p>
              )}
            </div>

            {/* Top recurring products */}
            {topRecurrent.length > 0 && (
              <div className="bg-card border border-border rounded-2xl p-4">
                <div className="flex items-center gap-2 mb-3">
                  <RotateCcw className="w-4 h-4 text-primary" />
                  <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Produits récurrents</p>
                </div>
                <div className="space-y-2">
                  {topRecurrent.map(({ name, count }, i) => (
                    <div key={name} className="flex items-center justify-between p-2.5 rounded-xl bg-muted/50">
                      <div className="flex items-center gap-2">
                        <span className="text-base">{MEDALS[i]}</span>
                        <span className="text-sm font-semibold text-card-foreground">{name}</span>
                      </div>
                      <span className="text-xs font-bold text-primary">×{count}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {products.length === 0 && (
              <div className="text-center py-16 text-muted-foreground">
                <TrendingUp className="w-10 h-10 mx-auto mb-3 opacity-30" />
                <p className="text-sm">Ajoutez des produits pour voir les tendances</p>
              </div>
            )}
          </TabsContent>
        </Tabs>
      </div>
    </PageTransition>
  );
};

export default Stats;
