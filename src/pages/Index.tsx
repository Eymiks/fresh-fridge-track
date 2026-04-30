import { useState, useMemo } from 'react';
import { useIsMobile } from '@/hooks/use-mobile';
import { Plus, LeafyGreen, Package, Layers, Search, AlertTriangle, Clock, CircleCheck, X, ArrowUpDown, LayoutGrid, Apple, Milk, Beef, Fish, CupSoda, Snowflake, Wheat, SlidersHorizontal, RotateCcw, UtensilsCrossed, Trash2, ChevronDown } from 'lucide-react';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { motion, AnimatePresence } from 'framer-motion';
import { useProducts } from '@/hooks/useProducts';
import { ProductCard } from '@/components/ProductCard';
import { AddProductSheet } from '@/components/AddProductSheet';
import { ProductEditorPayload, ProductEditorSheet } from '@/components/ProductEditorSheet';
import { getExpirationStatus, getEffectiveExpirationDate, PRODUCT_CATEGORIES, Product } from '@/types/product';
import { PageTransition } from '@/components/PageTransition';
import { useAppearance } from '@/contexts/AppearanceContext';

type AddMode = 'single' | 'multi';
type StatusFilter = 'all' | 'expired' | 'soon' | 'fresh';
type SortBy = 'expiration' | 'name' | 'added';

const statusFilters: { key: StatusFilter; label: string; icon: typeof AlertTriangle; color: string; activeColor: string }[] = [
  { key: 'all', label: 'Tous', icon: Package, color: 'text-muted-foreground', activeColor: 'bg-primary text-primary-foreground' },
  { key: 'expired', label: 'Périmés', icon: AlertTriangle, color: 'text-destructive', activeColor: 'bg-destructive text-destructive-foreground' },
  { key: 'soon', label: 'Bientôt', icon: Clock, color: 'text-warning', activeColor: 'bg-warning text-warning-foreground' },
  { key: 'fresh', label: 'Frais', icon: CircleCheck, color: 'text-success', activeColor: 'bg-success text-success-foreground' },
];

const sortOptions: { key: SortBy; label: string }[] = [
  { key: 'expiration', label: 'Date d\'expiration' },
  { key: 'name', label: 'Nom A-Z' },
  { key: 'added', label: 'Date d\'ajout' },
];

function SkeletonCard() {
  return (
    <div className="relative flex items-center gap-3 p-3 rounded-2xl bg-card border border-border overflow-hidden">
      <div className="absolute left-0 top-0 bottom-0 w-1 bg-muted rounded-l-2xl" />
      <div className="w-11 h-11 rounded-xl bg-muted animate-pulse ml-1 shrink-0" />
      <div className="flex-1 space-y-2 min-w-0">
        <div className="h-3 bg-muted rounded-full animate-pulse w-2/3" />
        <div className="h-2.5 bg-muted rounded-full animate-pulse w-1/3" />
      </div>
      <div className="w-10 h-5 bg-muted rounded-full animate-pulse shrink-0" />
    </div>
  );
}

const Index = () => {
  const isMobile = useIsMobile();
  const { density } = useAppearance();
  const spaceClass = density === 'compact' ? 'space-y-2' : density === 'spacious' ? 'space-y-4' : 'space-y-3';
  const { products, loading, addProduct, removeProduct, setProductStatus, updateProduct } = useProducts();
  const [showBubble, setShowBubble] = useState(false);
  const [addMode, setAddMode] = useState<AddMode | null>(null);
  const [editingProductId, setEditingProductId] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');
  const [sortBy, setSortBy] = useState<SortBy>('expiration');
  const [categoryFilter, setCategoryFilter] = useState('all');
  const [showSortMenu, setShowSortMenu] = useState(false);

  // Collapsible sections
  const [collapsedSections, setCollapsedSections] = useState<Set<string>>(new Set());
  const toggleSection = (key: string) => {
    setCollapsedSections(prev => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key); else next.add(key);
      return next;
    });
  };

  // Multi-select state
  const [selectionMode, setSelectionMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  const enterSelection = (id: string) => {
    setSelectionMode(true);
    setSelectedIds(new Set([id]));
  };

  const toggleSelect = (id: string) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const exitSelection = () => {
    setSelectionMode(false);
    setSelectedIds(new Set());
  };

  const handleBatchStatus = async (status: 'consumed' | 'thrown') => {
    await Promise.all([...selectedIds].map(id => setProductStatus(id, status)));
    exitSelection();
  };

  const handleBatchDelete = async () => {
    await Promise.all([...selectedIds].map(id => removeProduct(id)));
    exitSelection();
  };

  const activeProducts = useMemo(() =>
    products.filter(p => p.status !== 'consumed' && p.status !== 'thrown'),
    [products]
  );
  const editingProduct = editingProductId ? products.find(product => product.id === editingProductId) : undefined;

  const handleModeSelect = (mode: AddMode) => {
    setShowBubble(false);
    setAddMode(mode);
  };

  const expired = activeProducts.filter(p => getExpirationStatus(getEffectiveExpirationDate(p)) === 'expired');
  const soon = activeProducts.filter(p => getExpirationStatus(getEffectiveExpirationDate(p)) === 'soon');
  const fresh = activeProducts.filter(p => getExpirationStatus(getEffectiveExpirationDate(p)) === 'fresh');

  const filteredProducts = useMemo(() => {
    let result = [...activeProducts];

    if (categoryFilter !== 'all') {
      result = result.filter(p => p.category === categoryFilter);
    }
    if (statusFilter !== 'all') {
      result = result.filter(p => getExpirationStatus(getEffectiveExpirationDate(p)) === statusFilter);
    }
    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter(p =>
        p.name.toLowerCase().includes(q) ||
        (p.brand && p.brand.toLowerCase().includes(q))
      );
    }

    result.sort((a, b) => {
      if (sortBy === 'expiration') return new Date(getEffectiveExpirationDate(a)).getTime() - new Date(getEffectiveExpirationDate(b)).getTime();
      if (sortBy === 'name') return a.name.localeCompare(b.name);
      return new Date(b.addedAt).getTime() - new Date(a.addedAt).getTime();
    });

    return result;
  }, [activeProducts, statusFilter, categoryFilter, search, sortBy]);

  const hasActiveFilter = statusFilter !== 'all' || categoryFilter !== 'all' || search.trim();
  const shouldShowSortedList = hasActiveFilter || sortBy !== 'expiration';

  const cardProps = (p: typeof activeProducts[0]) => ({
    product: p,
    onSetStatus: setProductStatus,
    onUpdateDate: (id: string, date: string) => updateProduct(id, { expirationDate: date }),
    onEditProduct: (id: string) => setEditingProductId(id),
    selectionMode,
    isSelected: selectedIds.has(p.id),
    onLongPress: enterSelection,
    onToggleSelect: toggleSelect,
  });

  return (
    <PageTransition>
      <div className={`min-h-screen bg-background ${isMobile ? 'pb-24' : 'pb-8'}`}>
        {/* Header */}
        <div className={`border-b px-5 ${isMobile ? 'pt-12' : 'pt-6'} pb-6 transition-colors duration-500 ${
          expired.length > 0
            ? 'bg-destructive/5 border-destructive/15'
            : soon.length > 0
            ? 'bg-warning/5 border-warning/15'
            : 'bg-card border-border'
        }`}>
          <div className="flex items-center justify-between mb-5">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 bg-primary/10 rounded-xl flex items-center justify-center">
                <LeafyGreen className="w-5 h-5 text-primary" />
              </div>
              <h1 className="text-xl font-extrabold text-foreground">FreshTrack</h1>
            </div>
            {!isMobile && (
              <button
                onClick={() => setAddMode('single')}
                className="flex items-center gap-2 bg-primary text-primary-foreground px-4 py-2 rounded-xl text-sm font-bold hover:bg-primary/90 transition-colors shadow-sm"
              >
                <Plus className="w-4 h-4" />
                Ajouter
              </button>
            )}
          </div>

          {/* Mini stat cards */}
          {activeProducts.length > 0 && (
            <div className="grid grid-cols-3 gap-3 mb-1">
              {[
                { count: expired.length, label: 'Périmés', icon: AlertTriangle, color: 'text-destructive', bg: 'bg-destructive/10', filter: 'expired' as StatusFilter },
                { count: soon.length, label: 'Bientôt', icon: Clock, color: 'text-warning', bg: 'bg-warning/10', filter: 'soon' as StatusFilter },
                { count: fresh.length, label: 'Frais', icon: CircleCheck, color: 'text-success', bg: 'bg-success/10', filter: 'fresh' as StatusFilter },
              ].map((stat) => {
                const isActive = statusFilter === stat.filter;
                return (
                  <button
                    key={stat.label}
                    onClick={() => setStatusFilter(isActive ? 'all' : stat.filter)}
                    className={`flex items-center gap-2.5 bg-background rounded-xl p-3.5 border transition-all ${
                      isActive ? 'ring-2 ring-primary border-primary/30' : 'border-border'
                    }`}
                  >
                    <div className={`w-9 h-9 rounded-lg ${stat.bg} flex items-center justify-center`}>
                      <stat.icon className={`w-4.5 h-4.5 ${stat.color}`} />
                    </div>
                    <div className="text-left">
                      <p className="text-base font-extrabold text-foreground leading-none">{stat.count}</p>
                      <p className="text-[10px] text-muted-foreground font-semibold">{stat.label}</p>
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* Search bar + sort */}
        {products.length > 0 && (
          <div className="px-5 mt-4 flex gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <input
                type="text"
                placeholder="Rechercher un produit..."
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="w-full pl-9 pr-9 py-2.5 bg-card border border-border rounded-xl text-sm text-card-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/30 shadow-sm"
              />
              {search && (
                <button onClick={() => setSearch('')} className="absolute right-3 top-1/2 -translate-y-1/2">
                  <X className="w-4 h-4 text-muted-foreground" />
                </button>
              )}
            </div>
            {/* Sort button */}
            <div className="relative">
              <button
                onClick={() => setShowSortMenu(v => !v)}
                className="h-[42px] px-3 bg-card border border-border rounded-xl shadow-sm flex items-center justify-center"
              >
                <ArrowUpDown className="w-4 h-4 text-muted-foreground" />
              </button>
              <AnimatePresence>
                {showSortMenu && (
                  <>
                    <div className="fixed inset-0 z-30" onClick={() => setShowSortMenu(false)} />
                    <motion.div
                      initial={{ opacity: 0, y: -4, scale: 0.95 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: -4, scale: 0.95 }}
                      transition={{ duration: 0.12 }}
                      className="absolute right-0 top-full mt-1 bg-card border border-border rounded-xl shadow-lg z-40 overflow-hidden min-w-[160px]"
                    >
                      {sortOptions.map(opt => (
                        <button
                          key={opt.key}
                          onClick={() => { setSortBy(opt.key); setShowSortMenu(false); }}
                          className={`w-full text-left px-4 py-2.5 text-xs font-semibold transition-colors ${
                            sortBy === opt.key ? 'bg-primary/10 text-primary' : 'text-card-foreground hover:bg-muted'
                          }`}
                        >
                          {opt.label}
                        </button>
                      ))}
                    </motion.div>
                  </>
                )}
              </AnimatePresence>
            </div>
            {/* Filter button */}
            <Popover modal>
              <PopoverTrigger asChild>
                <button className="relative h-[42px] px-3 bg-card border border-border rounded-xl shadow-sm flex items-center justify-center gap-1.5">
                  <SlidersHorizontal className="w-4 h-4 text-muted-foreground" />
                  {(statusFilter !== 'all' || categoryFilter !== 'all') && (
                    <span className="absolute -top-1.5 -right-1.5 w-4 h-4 rounded-full bg-primary text-primary-foreground text-[10px] font-bold flex items-center justify-center">
                      {(statusFilter !== 'all' ? 1 : 0) + (categoryFilter !== 'all' ? 1 : 0)}
                    </span>
                  )}
                </button>
              </PopoverTrigger>
              <PopoverContent align="end" className="w-64 p-3 animate-scale-in origin-top-right">
                <p className="text-[10px] font-bold uppercase text-muted-foreground tracking-wider mb-2">Statut</p>
                <div className="flex flex-col gap-1 mb-3">
                  {statusFilters.map(f => {
                    const isActive = statusFilter === f.key;
                    return (
                      <button
                        key={f.key}
                        onClick={() => setStatusFilter(f.key)}
                        className={`flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-semibold transition-colors ${
                          isActive ? f.activeColor : 'text-card-foreground hover:bg-muted'
                        }`}
                      >
                        <f.icon className="w-3.5 h-3.5" />
                        {f.label}
                      </button>
                    );
                  })}
                </div>
                <div className="h-px bg-border mb-3" />
                <p className="text-[10px] font-bold uppercase text-muted-foreground tracking-wider mb-2">Catégorie</p>
                <div className="grid grid-cols-3 gap-1.5">
                  {PRODUCT_CATEGORIES.map(cat => {
                    const iconMap: Record<string, React.ComponentType<{ className?: string }>> = {
                      LayoutGrid, Apple, Milk, Beef, Fish, CupSoda, Snowflake, Wheat, Package,
                    };
                    const IconComp = iconMap[cat.icon] || Package;
                    const isActive = categoryFilter === cat.key;
                    return (
                      <button
                        key={cat.key}
                        onClick={() => setCategoryFilter(cat.key)}
                        className={`flex flex-col items-center gap-1 px-2 py-2 rounded-lg text-[10px] font-semibold transition-colors ${
                          isActive
                            ? 'bg-primary text-primary-foreground shadow-sm'
                            : 'text-muted-foreground hover:bg-muted'
                        }`}
                      >
                        <IconComp className="w-4 h-4" />
                        {cat.label}
                      </button>
                    );
                  })}
                </div>
                {(statusFilter !== 'all' || categoryFilter !== 'all') && (
                  <button
                    onClick={() => { setStatusFilter('all'); setCategoryFilter('all'); }}
                    className="flex items-center justify-center gap-1.5 w-full mt-3 py-2 rounded-lg text-xs font-semibold text-muted-foreground hover:bg-muted transition-colors"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                    Réinitialiser
                  </button>
                )}
              </PopoverContent>
            </Popover>
          </div>
        )}

        {/* Alert banner */}
        {expired.length > 0 && statusFilter !== 'expired' && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            className="mx-5 mt-4"
          >
            <div className="flex items-center justify-between bg-destructive/10 border border-destructive/20 rounded-xl px-4 py-3">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-destructive" />
                <span className="text-xs font-bold text-destructive">
                  {expired.length} produit{expired.length > 1 ? 's' : ''} périmé{expired.length > 1 ? 's' : ''}
                </span>
              </div>
              <button
                onClick={() => setStatusFilter('expired')}
                className="text-xs font-bold text-destructive bg-destructive/10 hover:bg-destructive/20 px-3 py-1 rounded-full transition-colors"
              >
                Voir
              </button>
            </div>
          </motion.div>
        )}

        {/* Product list */}
        <div className={`px-5 mt-5 ${spaceClass}`}>
          {loading ? (
            <div className="space-y-3">
              {Array.from({ length: 5 }).map((_, i) => <SkeletonCard key={i} />)}
            </div>
          ) : products.length === 0 ? (
            <div className="text-center py-16">
              <motion.div
                className="mx-auto mb-6 w-24 h-24 relative"
                animate={{ y: [0, -6, 0] }}
                transition={{ duration: 2.5, repeat: Infinity, ease: 'easeInOut' }}
              >
                <svg viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-full h-full">
                  <rect x="20" y="8" width="56" height="80" rx="8" className="fill-muted stroke-muted-foreground/30" strokeWidth="2" />
                  <line x1="20" y1="42" x2="76" y2="42" className="stroke-muted-foreground/20" strokeWidth="2" />
                  <rect x="62" y="26" width="3" height="10" rx="1.5" className="fill-muted-foreground/40" />
                  <rect x="62" y="50" width="3" height="10" rx="1.5" className="fill-muted-foreground/40" />
                  <motion.circle
                    cx="48" cy="56" r="12"
                    className="fill-primary/20"
                    animate={{ opacity: [0.3, 0.7, 0.3], scale: [0.9, 1.1, 0.9] }}
                    transition={{ duration: 2, repeat: Infinity }}
                  />
                  <motion.g
                    animate={{ opacity: [0.5, 1, 0.5] }}
                    transition={{ duration: 1.5, repeat: Infinity }}
                  >
                    <Snowflake x="36" y="46" width="16" height="16" className="text-primary" />
                  </motion.g>
                </svg>
              </motion.div>
              <h2 className="text-xl font-extrabold text-foreground mb-2">Frigo vide !</h2>
              <p className="text-muted-foreground text-sm mb-5">
                Ajoutez votre premier produit pour commencer
              </p>
              <button
                onClick={() => { setShowBubble(false); setAddMode('single'); }}
                className="inline-flex items-center gap-2 bg-primary text-primary-foreground px-5 py-2.5 rounded-full text-sm font-bold shadow-lg shadow-primary/20 active:scale-95 transition-transform"
              >
                <Plus className="w-4 h-4" />
                Ajouter un produit
              </button>
            </div>
          ) : filteredProducts.length === 0 ? (
            <div className="text-center py-12">
              <Search className="w-10 h-10 text-muted-foreground mx-auto mb-3" />
              <p className="text-sm text-muted-foreground font-semibold">Aucun produit trouvé</p>
            </div>
          ) : shouldShowSortedList ? (
            <div className="space-y-3">
              {filteredProducts.map(p => (
                <ProductCard key={p.id} {...cardProps(p)} />
              ))}
            </div>
          ) : (
            <div className="space-y-6">
              {[
                { key: 'expired', items: expired, Icon: AlertTriangle, label: 'Périmés', color: 'text-destructive' },
                { key: 'soon', items: soon, Icon: Clock, label: 'Bientôt périmés', color: 'text-warning' },
                { key: 'fresh', items: fresh, Icon: CircleCheck, label: 'Frais', color: 'text-success' },
              ].map(({ key, items, Icon, label, color }) => items.length > 0 && (
                <div key={key}>
                  <button
                    onClick={() => toggleSection(key)}
                    className="flex items-center gap-1.5 mb-3 w-full group"
                  >
                    <Icon className={`w-3.5 h-3.5 ${color}`} />
                    <h3 className={`text-xs font-bold uppercase tracking-wider flex-1 text-left ${color}`}>
                      {label}
                      <span className="ml-1.5 font-normal opacity-60">({items.length})</span>
                    </h3>
                    <ChevronDown
                      className={`w-4 h-4 ${color} opacity-60 transition-transform duration-200 ${
                        collapsedSections.has(key) ? '-rotate-90' : ''
                      }`}
                    />
                  </button>
                  <AnimatePresence initial={false}>
                    {!collapsedSections.has(key) && (
                      <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.2, ease: 'easeInOut' }}
                        className="overflow-hidden"
                      >
                        <div className="space-y-3 pb-1">
                          {items.map(p => (
                            <ProductCard key={p.id} {...cardProps(p)} />
                          ))}
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Backdrop — mobile only */}
        <AnimatePresence>
          {isMobile && showBubble && (
            <motion.div
              className="fixed inset-0 z-40 bg-foreground/50"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.12 }}
              onClick={() => setShowBubble(false)}
            />
          )}
        </AnimatePresence>

        {/* Bubble menu — mobile only */}
        <AnimatePresence>
          {isMobile && showBubble && (
            <motion.div
              className="fixed bottom-24 right-6 z-50 flex flex-col gap-2"
              initial={{ opacity: 0, y: 20, scale: 0.85 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 20, scale: 0.85 }}
              transition={{ type: 'spring', stiffness: 400, damping: 25 }}
            >
              {[
                { mode: 'multi' as const, icon: Layers, title: 'Plusieurs produits', sub: 'Scan en chaîne', bg: 'bg-accent/10', color: 'text-accent-foreground' },
                { mode: 'single' as const, icon: Package, title: 'Un produit', sub: 'Ajout manuel', bg: 'bg-primary/10', color: 'text-primary' },
              ].map((item, i) => (
                <motion.button
                  key={item.mode}
                  onClick={() => handleModeSelect(item.mode)}
                  className="flex items-center gap-3 bg-card border border-border rounded-2xl px-4 py-3 shadow-xl hover:bg-muted transition-colors"
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.06, type: 'spring', stiffness: 400, damping: 25 }}
                >
                  <div className={`w-9 h-9 ${item.bg} rounded-lg flex items-center justify-center`}>
                    <item.icon className={`w-5 h-5 ${item.color}`} />
                  </div>
                  <div className="text-left">
                    <p className="text-sm font-bold text-card-foreground">{item.title}</p>
                    <p className="text-[10px] text-muted-foreground">{item.sub}</p>
                  </div>
                </motion.button>
              ))}
            </motion.div>
          )}
        </AnimatePresence>

        {/* FAB — mobile only, hidden in selection mode */}
        {isMobile && addMode === null && !selectionMode && (
          <motion.button
            onClick={() => setShowBubble(prev => !prev)}
            animate={{ rotate: showBubble ? 45 : 0 }}
            transition={{ type: 'spring', stiffness: 300, damping: 20 }}
            className="fixed bottom-6 right-6 w-16 h-16 bg-primary text-primary-foreground rounded-full shadow-xl shadow-primary/30 flex items-center justify-center active:scale-95 z-50"
          >
            <Plus className="w-7 h-7" strokeWidth={3} />
          </motion.button>
        )}

        {/* Batch action bar — shown in selection mode */}
        <AnimatePresence>
          {selectionMode && (
            <motion.div
              initial={{ y: 80, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: 80, opacity: 0 }}
              transition={{ type: 'spring', stiffness: 400, damping: 30 }}
              className="fixed bottom-0 inset-x-0 z-50 bg-card border-t border-border px-4 py-3 flex items-center gap-2"
            >
              <button
                onClick={exitSelection}
                className="p-2 rounded-xl hover:bg-muted transition-colors text-muted-foreground shrink-0"
              >
                <X className="w-5 h-5" />
              </button>
              <span className="text-sm font-bold text-foreground flex-1 truncate">
                {selectedIds.size} sélectionné{selectedIds.size > 1 ? 's' : ''}
              </span>
              <button
                onClick={() => handleBatchStatus('consumed')}
                disabled={selectedIds.size === 0}
                title="Marquer comme consommés"
                className="w-10 h-10 rounded-xl bg-success/10 text-success flex items-center justify-center hover:bg-success/20 disabled:opacity-40 transition-colors shrink-0"
              >
                <UtensilsCrossed className="w-4 h-4" />
              </button>
              <button
                onClick={() => handleBatchStatus('thrown')}
                disabled={selectedIds.size === 0}
                title="Marquer comme jetés"
                className="w-10 h-10 rounded-xl bg-warning/10 text-warning flex items-center justify-center hover:bg-warning/20 disabled:opacity-40 transition-colors shrink-0"
              >
                <Trash2 className="w-4 h-4" />
              </button>
              <button
                onClick={handleBatchDelete}
                disabled={selectedIds.size === 0}
                title="Supprimer"
                className="w-10 h-10 rounded-xl bg-destructive/10 text-destructive flex items-center justify-center hover:bg-destructive/20 disabled:opacity-40 transition-colors shrink-0"
              >
                <X className="w-4 h-4" />
              </button>
            </motion.div>
          )}
        </AnimatePresence>

        <AddProductSheet open={addMode !== null} mode={addMode || 'single'} onClose={() => setAddMode(null)} onAdd={addProduct} />
        {editingProduct && (
          <ProductEditorSheet
            open={editingProductId !== null}
            mode="edit"
            product={editingProduct}
            onClose={() => setEditingProductId(null)}
            onSubmit={async (updates: ProductEditorPayload) => {
              await updateProduct(editingProduct.id, updates as Partial<Omit<Product, 'id' | 'addedAt'>>);
              setEditingProductId(null);
            }}
          />
        )}
      </div>
    </PageTransition>
  );
};

export default Index;
