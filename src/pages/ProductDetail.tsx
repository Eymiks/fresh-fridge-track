import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Trash2, Calendar, Barcode, Clock, Tag, Pencil, AlertTriangle, CircleCheck, PackageOpen, UtensilsCrossed, RotateCcw, Scale, ShieldAlert, ChevronDown, Info, SearchX, RefreshCw, X, Snowflake } from 'lucide-react';
import { format, differenceInDays } from 'date-fns';
import { fr } from 'date-fns/locale';
import { toast } from 'sonner';
import { useProducts } from '@/hooks/useProducts';
import { useAuth } from '@/contexts/AuthContext';
import { supabase } from '@/integrations/supabase/client';
import { getExpirationStatus, getDaysUntilExpiration, getEffectiveExpirationDate, ProductStatus, RECOMMENDED_DAYS_AFTER_OPENING, CATEGORY_POST_EXPIRY_NOTES, PRODUCT_CATEGORIES, getPostExpiryNote, getRecommendedDaysAfterOpening, getFreezeDuration } from '@/types/product';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { PageTransition } from '@/components/PageTransition';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog';
import { motion, useScroll, useTransform, AnimatePresence } from 'framer-motion';

const statusConfig = {
  fresh: {
    bg: 'bg-success/10',
    border: 'border-success/30',
    badge: 'bg-success text-success-foreground',
    label: 'Frais',
    icon: CircleCheck,
    iconColor: 'text-success',
    gradient: 'from-success/20 to-success/5',
  },
  soon: {
    bg: 'bg-warning/10',
    border: 'border-warning/30',
    badge: 'bg-warning text-warning-foreground',
    label: 'Bientôt périmé',
    icon: Clock,
    iconColor: 'text-warning',
    gradient: 'from-warning/20 to-warning/5',
  },
  expired: {
    bg: 'bg-destructive/10',
    border: 'border-destructive/30',
    badge: 'bg-destructive text-destructive-foreground',
    label: 'Périmé',
    icon: AlertTriangle,
    iconColor: 'text-destructive',
    gradient: 'from-destructive/20 to-destructive/5',
  },
};

const nutriColors: Record<string, string> = {
  A: 'bg-green-600', B: 'bg-lime-500', C: 'bg-yellow-400', D: 'bg-orange-400', E: 'bg-red-500',
};
const novaColors: Record<number, string> = {
  1: 'bg-green-600', 2: 'bg-yellow-400', 3: 'bg-orange-400', 4: 'bg-red-500',
};
const ecoColors: Record<string, string> = {
  A: 'bg-green-600', B: 'bg-lime-500', C: 'bg-yellow-400', D: 'bg-orange-400', E: 'bg-red-500',
};

const allergenTranslations: Record<string, string> = {
  milk: 'Lait',
  gluten: 'Gluten',
  eggs: 'Œufs',
  nuts: 'Fruits à coque',
  peanuts: 'Arachides',
  soybeans: 'Soja',
  soya: 'Soja',
  fish: 'Poisson',
  celery: 'Céleri',
  mustard: 'Moutarde',
  sesame: 'Sésame',
  'sesame-seeds': 'Sésame',
  sulphur: 'Sulfites',
  'sulphur-dioxide-and-sulphites': 'Sulfites',
  lupin: 'Lupin',
  molluscs: 'Mollusques',
  crustaceans: 'Crustacés',
  wheat: 'Blé',
  barley: 'Orge',
  oats: 'Avoine',
  rye: 'Seigle',
  lactose: 'Lactose',
};

function translateAllergen(raw: string): string {
  const cleaned = raw.replace(/^(en|fr):/i, '').trim().toLowerCase().replace(/\s+/g, '-');
  return allergenTranslations[cleaned] || cleaned.charAt(0).toUpperCase() + cleaned.slice(1).replace(/-/g, ' ');
}

type ScoreDialogType = 'nutri' | 'nova' | 'eco' | null;

const scoreExplanations = {
  nutri: {
    title: 'Nutri-Score',
    description: 'Le Nutri-Score évalue la qualité nutritionnelle globale d\'un aliment de A (meilleur) à E (à limiter).',
    grades: [
      { grade: 'A', desc: 'Excellente qualité nutritionnelle', color: 'bg-green-600' },
      { grade: 'B', desc: 'Bonne qualité nutritionnelle', color: 'bg-lime-500' },
      { grade: 'C', desc: 'Qualité nutritionnelle moyenne', color: 'bg-yellow-400' },
      { grade: 'D', desc: 'Qualité nutritionnelle médiocre', color: 'bg-orange-400' },
      { grade: 'E', desc: 'Mauvaise qualité nutritionnelle', color: 'bg-red-500' },
    ],
  },
  nova: {
    title: 'Score NOVA',
    description: 'La classification NOVA évalue le degré de transformation des aliments.',
    grades: [
      { grade: '1', desc: 'Aliments non transformés ou peu transformés', color: 'bg-green-600' },
      { grade: '2', desc: 'Ingrédients culinaires transformés', color: 'bg-yellow-400' },
      { grade: '3', desc: 'Aliments transformés', color: 'bg-orange-400' },
      { grade: '4', desc: 'Produits ultra-transformés', color: 'bg-red-500' },
    ],
  },
  eco: {
    title: 'Eco-Score',
    description: 'L\'Eco-Score mesure l\'impact environnemental d\'un produit alimentaire de A (faible impact) à E (fort impact).',
    grades: [
      { grade: 'A', desc: 'Très faible impact environnemental', color: 'bg-green-600' },
      { grade: 'B', desc: 'Faible impact environnemental', color: 'bg-lime-500' },
      { grade: 'C', desc: 'Impact environnemental modéré', color: 'bg-yellow-400' },
      { grade: 'D', desc: 'Impact environnemental élevé', color: 'bg-orange-400' },
      { grade: 'E', desc: 'Impact environnemental très élevé', color: 'bg-red-500' },
    ],
  },
};

function ScoreBadge({ label, value, colorMap, onClick }: { label: string; value: string; colorMap: Record<string, string>; onClick?: () => void }) {
  const bg = colorMap[value] || 'bg-muted';
  return (
    <button onClick={onClick} className="flex flex-col items-center gap-0.5 group">
      <span className={`${bg} text-white text-xs font-extrabold w-8 h-8 rounded-lg flex items-center justify-center group-hover:ring-2 group-hover:ring-primary/40 transition-all`}>
        {value}
      </span>
      <span className="text-[9px] text-muted-foreground font-semibold">{label}</span>
    </button>
  );
}

const productStatusBadges: Record<string, { label: string; icon: typeof PackageOpen; color: string }> = {
  opened: { label: 'Ouvert', icon: PackageOpen, color: 'bg-blue-500/10 text-blue-600 border-blue-500/20' },
  consumed: { label: 'Consommé', icon: UtensilsCrossed, color: 'bg-success/10 text-success border-success/20' },
  thrown: { label: 'Jeté', icon: Trash2, color: 'bg-destructive/10 text-destructive border-destructive/20' },
};

function TimelineStep({ icon: Icon, label, date }: { icon: React.ElementType; label: string; date: string }) {
  return (
    <div className="flex flex-col items-center gap-0.5 min-w-0">
      <Icon className="w-3.5 h-3.5 text-muted-foreground" />
      <span className="text-[10px] font-bold text-card-foreground">{label}</span>
      <span className="text-[9px] text-muted-foreground">{format(new Date(date), 'd MMM', { locale: fr })}</span>
    </div>
  );
}

const ProductDetail = () => {
  const { scrollY } = useScroll();
  const heroY = useTransform(scrollY, [0, 300], [0, 80]);
  const heroScale = useTransform(scrollY, [0, 300], [1.1, 1.3]);
  const heroOpacity = useTransform(scrollY, [0, 250], [1, 0.3]);
  const thumbY = useTransform(scrollY, [0, 200], [0, 30]);
  const [showStickyHeader, setShowStickyHeader] = useState(false);
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { products, loading, removeProduct, updateProduct, setProductStatus } = useProducts();
  const { household } = useAuth();
  const [editing, setEditing] = useState(false);
  const [loadingImage, setLoadingImage] = useState(false);
  const [offImages, setOffImages] = useState<string[]>([]);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [editName, setEditName] = useState('');
  const [editDate, setEditDate] = useState('');
  const [editBrand, setEditBrand] = useState('');
  const [editCategory, setEditCategory] = useState('');
  const [ingredientsOpen, setIngredientsOpen] = useState(false);
  const [scoreDialog, setScoreDialog] = useState<ScoreDialogType>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [daysInput, setDaysInput] = useState(3);
  const [showFullscreen, setShowFullscreen] = useState(false);
  const [notes, setNotes] = useState('');

  const product = products.find(p => p.id === id);

  useEffect(() => { window.scrollTo(0, 0); }, []);

  useEffect(() => {
    if (product) setNotes(product.notes ?? '');
  }, [product?.id]);

  useEffect(() => {
    const unsubscribe = scrollY.on('change', (v) => setShowStickyHeader(v > 180));
    return unsubscribe;
  }, [scrollY]);

  const fetchOFFImages = async () => {
    if (!product.barcode) return;
    setLoadingImage(true);
    try {
      const res = await fetch(`https://world.openfoodfacts.org/api/v0/product/${product.barcode}.json`);
      const data = await res.json();
      const p = data?.product;
      if (!p) { toast.error('Produit introuvable sur OpenFoodFacts'); setLoadingImage(false); return; }
      const candidates = [
        p.image_url, p.image_front_url,
        p.image_nutrition_url, p.image_ingredients_url, p.image_packaging_url,
      ].filter(Boolean) as string[];
      const unique = [...new Set(candidates)];
      if (unique.length === 0) {
        toast.error('Aucune image disponible sur OpenFoodFacts');
      } else {
        setOffImages(unique);
        setSelectedImage(unique[0]);
      }
    } catch {
      toast.error('Erreur lors de la récupération des images');
    }
    setLoadingImage(false);
  };

  const applyImage = async (imageUrl: string) => {
    await Promise.all([
      updateProduct(product.id, { imageUrl }),
      product.barcode && household
        ? supabase.from('products').update({ image_url: imageUrl })
            .eq('household_id', household.id)
            .eq('barcode', product.barcode)
            .neq('id', product.id)
        : Promise.resolve(),
    ]);
    setOffImages([]);
    setSelectedImage(null);
    toast.success('Image mise à jour');
  };

  const uploadImage = async (file: File) => {
    if (!household) return;
    setLoadingImage(true);
    const ext = file.name.split('.').pop() ?? 'jpg';
    const path = `${household.id}/${product.id}_${Date.now()}.${ext}`;
    const { error } = await supabase.storage.from('product-images').upload(path, file, { upsert: true });
    if (error) { toast.error(`Upload: ${error.message}`); setLoadingImage(false); return; }
    const { data } = supabase.storage.from('product-images').getPublicUrl(path);
    await applyImage(data.publicUrl);
    setLoadingImage(false);
  };

  if (loading && !product) return null;
  if (!product) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-5">
        <SearchX className="w-16 h-16 text-muted-foreground mb-4" />
        <h2 className="text-xl font-extrabold text-foreground mb-2">Produit introuvable</h2>
        <button
          onClick={() => navigate('/')}
          className="mt-4 px-6 py-3 bg-primary text-primary-foreground rounded-xl font-bold"
        >
          Retour
        </button>
      </div>
    );
  }

  const effectiveDate = getEffectiveExpirationDate(product);
  const status = getExpirationStatus(effectiveDate);
  const days = getDaysUntilExpiration(effectiveDate);
  const config = statusConfig[status];
  const StatusIcon = config.icon;
  const currentProductStatus = product.status || 'active';

  const handleRemove = () => { removeProduct(product.id); navigate('/'); };
  const categoryLabel = product.category ? (PRODUCT_CATEGORIES.find(c => c.key === product.category)?.label || product.category) : '';
  const openEdit = () => {
    setEditName(product.name);
    setEditDate(product.expirationDate.split('T')[0]);
    setEditBrand(product.brand || '');
    setEditCategory(product.category || '');
    setEditing(true);
  };
  const handleSave = () => {
    if (!editName.trim() || !editDate) return;
    updateProduct(product.id, { name: editName.trim(), expirationDate: editDate, brand: editBrand.trim() || undefined, category: editCategory.trim() || undefined });
    setEditing(false);
  };
  const handleStatusChange = (newStatus: ProductStatus) => {
    if (newStatus === 'opened') {
      const rec = getRecommendedDaysAfterOpening(product.category, product.subcategory);
      setDaysInput(rec.days);
      setOpenDialog(true);
      return;
    }
    setProductStatus(product.id, newStatus);
  };
  const handleConfirmOpened = () => {
    setProductStatus(product.id, 'opened');
    updateProduct(product.id, { openedAt: new Date().toISOString(), daysAfterOpening: daysInput });
    setOpenDialog(false);
  };

  const handleFreeze = () => {
    const months = getFreezeDuration(product.category);
    const until = new Date();
    until.setMonth(until.getMonth() + months);
    updateProduct(product.id, { frozenUntil: until.toISOString().split('T')[0] });
  };

  const handleUnfreeze = () => updateProduct(product.id, { frozenUntil: null });

  const nutriGrade = product.nutriScore || '?';
  const statusBadge = productStatusBadges[currentProductStatus];
  const activeExplanation = scoreDialog ? scoreExplanations[scoreDialog] : null;

  const today = new Date();
  const addedDate = new Date(product.addedAt);
  const expDate = new Date(effectiveDate);
  const totalDays = differenceInDays(expDate, addedDate);
  const elapsedDays = differenceInDays(today, addedDate);
  const expiryProgress = totalDays > 0 ? Math.min(Math.max(elapsedDays / totalDays, 0), 1) : 1;
  const progressBarColor = status === 'fresh' ? 'bg-success' : status === 'soon' ? 'bg-warning' : 'bg-destructive';

  return (
    <PageTransition>
      <div className="min-h-screen bg-background">
        {/* Sticky header */}
        <AnimatePresence>
          {showStickyHeader && product && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.2 }}
              className="fixed top-0 inset-x-0 z-30 bg-background/80 backdrop-blur-lg border-b border-border"
            >
              <div className="flex items-center gap-2 px-4 pt-10 pb-2">
                <button onClick={() => navigate('/')} className="p-1.5 rounded-full hover:bg-muted transition-colors flex-shrink-0">
                  <ArrowLeft className="w-5 h-5 text-foreground" />
                </button>
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt={product.name} className="w-7 h-7 rounded-lg object-cover flex-shrink-0" />
                ) : (
                  <div className="w-7 h-7 rounded-lg bg-muted flex items-center justify-center flex-shrink-0">
                    <span className="text-sm">🥬</span>
                  </div>
                )}
                <span className="font-semibold text-sm text-foreground truncate flex-1 min-w-0">{product.name}</span>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Hero header with blurred background + parallax */}
        <div className="relative h-52 overflow-hidden">
          {product.imageUrl ? (
            <>
              <motion.img src={product.imageUrl} alt="" className="absolute inset-0 w-full h-full object-cover blur-xl" style={{ y: heroY, scale: heroScale, opacity: heroOpacity }} />
              <div className="absolute inset-0 bg-foreground/40 dark:bg-background/60" />
            </>
          ) : (
            <div className={`absolute inset-0 bg-gradient-to-b ${config.gradient}`} />
          )}
          {/* Back button - glass style */}
          <button onClick={() => navigate('/')} className="absolute top-12 left-4 z-10 p-2.5 rounded-full bg-background/30 backdrop-blur-md border border-white/20 hover:bg-background/50 transition-colors text-white dark:text-foreground">
            <ArrowLeft className="w-5 h-5" />
          </button>
        </div>

        {/* Product image overlapping hero with parallax */}
        <motion.div className="flex justify-center -mt-16 relative z-10 mb-3" style={{ y: thumbY }}>
          <motion.div className="relative" initial={{ scale: 0.85, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ delay: 0.08, type: 'spring', stiffness: 300, damping: 25 }}>
            {product.imageUrl ? (
              <button onClick={() => setShowFullscreen(true)} className="w-28 h-40 rounded-2xl overflow-hidden shadow-xl border-4 border-background block">
                <img src={product.imageUrl} alt={product.name} className="w-full h-full object-cover" />
              </button>
            ) : (
              <div className="w-28 h-40 rounded-2xl bg-muted flex items-center justify-center shadow-xl border-4 border-background">
                <span className="text-4xl">🥬</span>
              </div>
            )}
            <button
              onClick={() => product.barcode ? fetchOFFImages() : fileInputRef.current?.click()}
              disabled={loadingImage}
              className="absolute -bottom-2 -right-2 w-7 h-7 rounded-full bg-background border border-border shadow flex items-center justify-center hover:bg-muted transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 text-muted-foreground ${loadingImage ? 'animate-spin' : ''}`} />
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={e => { const f = e.target.files?.[0]; if (f) uploadImage(f); e.target.value = ''; }}
            />
          </motion.div>
        </motion.div>

        {/* Content */}
        <motion.div className="px-5" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.12, duration: 0.25 }}>
          {/* Name + brand */}
          <div className="text-center mb-4">
            <h1 className="text-2xl font-extrabold text-foreground">{product.name}</h1>
            <p className="text-sm text-muted-foreground mt-0.5">
              {[product.brand ? product.brand.charAt(0).toUpperCase() + product.brand.slice(1) : '', product.quantity].filter(Boolean).join(' · ')}
            </p>
          </div>

          {/* Badges row - all aligned */}
          <div className="flex items-start justify-center gap-3 mb-3">
            <ScoreBadge label="Nutri" value={nutriGrade} colorMap={nutriColors} onClick={() => setScoreDialog('nutri')} />
            {product.novaGroup && (
              <ScoreBadge label="NOVA" value={String(product.novaGroup)} colorMap={novaColors} onClick={() => setScoreDialog('nova')} />
            )}
            {product.ecoScore && (
              <ScoreBadge label="Eco" value={product.ecoScore.toUpperCase()} colorMap={ecoColors} onClick={() => setScoreDialog('eco')} />
            )}
            <div className="flex flex-col items-center gap-0.5">
              <span className={`flex items-center gap-1 text-xs font-bold h-8 px-3 rounded-lg ${config.badge}`}>
                <StatusIcon className="w-3.5 h-3.5" />
                {config.label}
              </span>
              <span className="text-[9px] text-muted-foreground font-semibold">État</span>
            </div>
          </div>

          {/* Product status badge */}
          {statusBadge && (
            <div className="flex justify-center mb-2">
              <span className={`inline-flex items-center gap-1.5 text-xs font-bold px-3 py-1.5 rounded-full border ${statusBadge.color}`}><statusBadge.icon className="w-3.5 h-3.5" />{statusBadge.label}</span>
            </div>
          )}

          {/* Frozen badge */}
          {product.frozenUntil && (
            <div className="flex justify-center mb-4">
              <span className="inline-flex items-center gap-1.5 text-xs font-bold px-3 py-1.5 rounded-full border bg-blue-500/10 text-blue-500 border-blue-500/20">
                <Snowflake className="w-3.5 h-3.5" /> Congelé jusqu'au {format(new Date(product.frozenUntil), 'dd MMM yyyy', { locale: fr })}
              </span>
            </div>
          )}

          {/* Opened info */}
          {product.openedAt && product.daysAfterOpening != null && (
            <div className="rounded-2xl p-4 mb-4 bg-primary/10 border border-primary/20 text-center">
              <p className="text-sm font-bold text-primary flex items-center justify-center gap-1.5">
                <PackageOpen className="h-4 w-4" /> Ouvert depuis {Math.max(0, Math.ceil((new Date().getTime() - new Date(product.openedAt).getTime()) / (1000 * 60 * 60 * 24)))} jour{Math.ceil((new Date().getTime() - new Date(product.openedAt).getTime()) / (1000 * 60 * 60 * 24)) > 1 ? 's' : ''}
              </p>
              {effectiveDate !== product.expirationDate && (
                <p className="text-xs text-muted-foreground mt-1">
                  Date effective : {format(new Date(effectiveDate), 'dd MMMM yyyy', { locale: fr })}
                </p>
              )}
            </div>
          )}

          {/* Expiry progress bar */}
          <div className="mb-3">
            <div className="h-2 rounded-full bg-muted overflow-hidden">
              <div className={`h-full rounded-full transition-all ${progressBarColor}`} style={{ width: `${expiryProgress * 100}%` }} />
            </div>
          </div>

          {/* Day counter */}
          <div className={`rounded-2xl p-5 mb-5 ${config.bg} ${config.border} border text-center`}>
            <p className={`text-4xl font-black ${config.iconColor}`}>
              {days < 0 ? Math.abs(days) : days === 0 ? '!' : days}
            </p>
            <p className="text-sm font-semibold text-card-foreground mt-1">
              {days < 0 ? `jour${Math.abs(days) > 1 ? 's' : ''} de retard` : days === 0 ? "Expire aujourd'hui" : `jour${days > 1 ? 's' : ''} restant${days > 1 ? 's' : ''}`}
            </p>
          </div>

          {/* Post-expiry note */}
          {(() => {
            const note = getPostExpiryNote(product.category, product.subcategory);
            return note ? (
              <div className="flex items-start gap-3 rounded-2xl p-4 mb-5 bg-blue-500/10 border border-blue-500/20">
                <Info className="w-5 h-5 text-blue-500 shrink-0 mt-0.5" />
                <p className="text-sm text-card-foreground">
                  Ce produit peut généralement être consommé jusqu'à <span className="font-semibold">{note}</span> après la date d'expiration.
                </p>
              </div>
            ) : null;
          })()}

          {/* Status actions */}
          <div className="mb-5">
            <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-2">Statut du produit</h3>
            <div className="grid grid-cols-3 gap-2">
              {([
                { status: 'opened' as ProductStatus, icon: PackageOpen, label: 'Ouvert', color: 'text-blue-500', bg: 'bg-blue-500/10', activeBg: 'bg-blue-500 text-white' },
                { status: 'consumed' as ProductStatus, icon: UtensilsCrossed, label: 'Consommé', color: 'text-success', bg: 'bg-success/10', activeBg: 'bg-success text-success-foreground' },
                { status: 'thrown' as ProductStatus, icon: Trash2, label: 'Jeté', color: 'text-destructive', bg: 'bg-destructive/10', activeBg: 'bg-destructive text-destructive-foreground' },
              ]).map(item => {
                const isActive = currentProductStatus === item.status;
                return (
                  <button key={item.status} onClick={() => handleStatusChange(isActive ? 'active' : item.status)}
                    className={`flex flex-col items-center gap-1.5 p-3 rounded-2xl border transition-colors ${isActive ? `${item.activeBg} border-transparent` : `${item.bg} border-border hover:border-muted-foreground/20`}`}>
                    <item.icon className={`w-5 h-5 ${isActive ? '' : item.color}`} />
                    <span className={`text-[10px] font-bold ${isActive ? '' : item.color}`}>{item.label}</span>
                  </button>
                );
              })}
            </div>
            {currentProductStatus !== 'active' && (
              <button onClick={() => handleStatusChange('active')} className="w-full mt-2 flex items-center justify-center gap-1.5 py-2 text-xs font-semibold text-muted-foreground hover:text-foreground transition-colors">
                <RotateCcw className="w-3.5 h-3.5" /> Remettre en actif
              </button>
            )}
            {!product.frozenUntil ? (
              <button onClick={handleFreeze} className="w-full mt-2 flex items-center justify-center gap-1.5 py-2.5 rounded-2xl bg-blue-500/10 text-blue-600 text-xs font-bold border border-blue-500/20 hover:bg-blue-500/20 transition-colors">
                <Snowflake className="w-4 h-4" /> Mettre au congélateur ({getFreezeDuration(product.category)} mois)
              </button>
            ) : (
              <button onClick={handleUnfreeze} className="w-full mt-2 flex items-center justify-center gap-1.5 py-2 text-xs font-semibold text-blue-500 hover:text-blue-700 transition-colors">
                <RotateCcw className="w-3.5 h-3.5" /> Retirer du congélateur
              </button>
            )}
          </div>

          {/* Allergens */}
          {product.allergens && (
            <div className="mb-5">
              <div className="flex items-center gap-2 mb-2">
                <ShieldAlert className="w-4 h-4 text-destructive" />
                <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Allergènes</h3>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {product.allergens.split(',').map((a, i) => {
                  const translated = translateAllergen(a);
                  if (!translated) return null;
                  return (
                    <span key={i} className="text-[10px] font-bold px-2.5 py-1 rounded-full bg-destructive/10 text-destructive border border-destructive/20">
                      {translated}
                    </span>
                  );
                })}
              </div>
            </div>
          )}

          {/* Nutrition */}
          {product.nutritionData && (() => {
            const n = JSON.parse(product.nutritionData) as Record<string, number>;
            const rows: [string, string, string][] = [
              ['energy_kcal', 'Énergie', 'kcal'],
              ['fat', 'Matières grasses', 'g'],
              ['saturated_fat', 'dont saturées', 'g'],
              ['carbohydrates', 'Glucides', 'g'],
              ['sugars', 'dont sucres', 'g'],
              ['proteins', 'Protéines', 'g'],
              ['fiber', 'Fibres', 'g'],
              ['salt', 'Sel', 'g'],
            ].filter(([key]) => n[key] != null) as [string, string, string][];
            if (rows.length === 0) return null;
            return (
              <div className="mb-5">
                <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-2">Valeurs nutritionnelles <span className="font-normal normal-case">pour 100 g</span></h3>
                <div className="bg-card rounded-2xl border border-border divide-y divide-border">
                  {rows.map(([key, label, unit]) => (
                    <div key={key} className={`flex justify-between items-center px-4 py-2.5 ${key === 'saturated_fat' || key === 'sugars' ? 'pl-7' : ''}`}>
                      <span className="text-sm text-card-foreground">{label}</span>
                      <span className="text-sm font-bold text-card-foreground">{Number(n[key]).toFixed(1)} {unit}</span>
                    </div>
                  ))}
                </div>
              </div>
            );
          })()}

          {/* Details card */}
          <div className="bg-card rounded-2xl border border-border p-4 space-y-3">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center"><Calendar className="w-4 h-4 text-primary" /></div>
              <div>
                <p className="text-[11px] text-muted-foreground font-semibold">Date de péremption</p>
                <p className="text-sm font-bold text-card-foreground">{format(new Date(product.expirationDate), 'dd MMMM yyyy', { locale: fr })}</p>
              </div>
            </div>
            {product.quantity && (<><div className="h-px bg-border" /><div className="flex items-center gap-3"><div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center"><Scale className="w-4 h-4 text-primary" /></div><div><p className="text-[11px] text-muted-foreground font-semibold">Quantité</p><p className="text-sm font-bold text-card-foreground">{product.quantity}</p></div></div></>)}
            {product.category && (<><div className="h-px bg-border" /><div className="flex items-center gap-3"><div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center"><Tag className="w-4 h-4 text-primary" /></div><div><p className="text-[11px] text-muted-foreground font-semibold">Catégorie</p><p className="text-sm font-bold text-card-foreground">{categoryLabel}{product.subcategory && (<><span className="text-muted-foreground font-normal mx-1">›</span>{product.subcategory}</>)}</p></div></div></>)}
            <div className="h-px bg-border" />
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center"><Clock className="w-4 h-4 text-primary" /></div>
              <div>
                <p className="text-[11px] text-muted-foreground font-semibold">Ajouté le</p>
                <p className="text-sm font-bold text-card-foreground">{format(new Date(product.addedAt), 'dd MMMM yyyy', { locale: fr })}</p>
              </div>
            </div>
          </div>

          {/* Notes */}
          <div className="mt-4">
            <label className="text-xs font-bold text-muted-foreground uppercase tracking-wide mb-1.5 block">Notes</label>
            <textarea
              value={notes}
              onChange={e => setNotes(e.target.value)}
              onBlur={() => updateProduct(product.id, { notes: notes || undefined })}
              placeholder="Ajouter une note…"
              rows={2}
              className="w-full px-3 py-2 rounded-xl bg-muted text-sm text-foreground resize-none border border-border focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          {/* Ingredients */}
          {product.ingredients && (
            <Collapsible open={ingredientsOpen} onOpenChange={setIngredientsOpen} className="mt-4">
              <CollapsibleTrigger className="w-full flex items-center justify-between bg-card rounded-2xl border border-border px-4 py-3">
                <span className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Ingrédients</span>
                <ChevronDown className={`w-4 h-4 text-muted-foreground transition-transform ${ingredientsOpen ? 'rotate-180' : ''}`} />
              </CollapsibleTrigger>
              <CollapsibleContent className="bg-card rounded-b-2xl border border-t-0 border-border px-4 py-3">
                <p className="text-xs text-card-foreground leading-relaxed">{product.ingredients}</p>
              </CollapsibleContent>
            </Collapsible>
          )}

          <div className="mt-5 space-y-2">
            <button onClick={openEdit} className="w-full flex items-center justify-center gap-2 px-4 py-3.5 bg-primary text-primary-foreground rounded-2xl font-bold hover:bg-primary/90 transition-colors">
              <Pencil className="w-4 h-4" /> Modifier
            </button>
            <button onClick={() => setConfirmDelete(true)} className="w-full flex items-center justify-center gap-2 px-4 py-3.5 bg-destructive/10 text-destructive rounded-2xl font-bold hover:bg-destructive/20 transition-colors">
              <Trash2 className="w-4 h-4" /> Supprimer
            </button>
          </div>

          {/* Timeline */}
          <div className="mt-5">
            <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-3">Historique</h3>
            <div className="flex items-center gap-2 flex-wrap">
              <TimelineStep icon={Clock} label="Ajouté" date={product.addedAt} />
              {product.openedAt && (
                <>
                  <span className="text-muted-foreground text-sm">→</span>
                  <TimelineStep icon={PackageOpen} label="Ouvert" date={product.openedAt} />
                </>
              )}
              {product.frozenUntil && (
                <>
                  <span className="text-muted-foreground text-sm">→</span>
                  <div className="flex flex-col items-center gap-0.5 min-w-0">
                    <Snowflake className="w-3.5 h-3.5 text-blue-500" />
                    <span className="text-[10px] font-bold text-blue-500">Congelé</span>
                    <span className="text-[9px] text-muted-foreground">jusqu'au {format(new Date(product.frozenUntil), 'd MMM', { locale: fr })}</span>
                  </div>
                </>
              )}
              {(product.status === 'consumed' || product.status === 'thrown') && product.statusChangedAt && (
                <>
                  <span className="text-muted-foreground text-sm">→</span>
                  <TimelineStep
                    icon={product.status === 'consumed' ? UtensilsCrossed : Trash2}
                    label={product.status === 'consumed' ? 'Consommé' : 'Jeté'}
                    date={product.statusChangedAt}
                  />
                </>
              )}
            </div>
          </div>

          {/* Barcode */}
          {product.barcode && (
            <div className="flex items-center justify-center gap-1.5 mt-4">
              <Barcode className="w-3.5 h-3.5 text-muted-foreground/50" />
              <span className="text-[10px] text-muted-foreground/50 font-mono">{product.barcode}</span>
            </div>
          )}

          {/* OpenFoodFacts mention */}
          <p className="text-[10px] text-muted-foreground/40 text-center mt-3 mb-8 px-4">
            Les informations proviennent d'OpenFoodFacts et peuvent être inexactes ou incomplètes.
          </p>
        </motion.div>

        {/* Fullscreen image overlay */}
        {showFullscreen && product.imageUrl && (
          <div className="fixed inset-0 z-50 bg-black/95 flex items-center justify-center" onClick={() => setShowFullscreen(false)}>
            <img src={product.imageUrl} alt={product.name} className="max-w-full max-h-full object-contain" />
            <button className="absolute top-4 right-4 p-2 rounded-full bg-white/20 text-white" onClick={() => setShowFullscreen(false)}>
              <X className="w-5 h-5" />
            </button>
          </div>
        )}

        {/* Image picker from OpenFoodFacts */}
        <Dialog open={offImages.length > 0} onOpenChange={(o) => { if (!o) { setOffImages([]); setSelectedImage(null); } }}>
          <DialogContent className="max-w-sm rounded-2xl">
            <DialogHeader>
              <DialogTitle>Choisir une image</DialogTitle>
              <DialogDescription>Sélectionnez une image depuis OpenFoodFacts.</DialogDescription>
            </DialogHeader>
            <div className="grid grid-cols-3 gap-2 py-2">
              {offImages.map((url) => (
                <button
                  key={url}
                  onClick={() => setSelectedImage(url)}
                  className={`relative rounded-xl overflow-hidden border-2 transition-colors aspect-square ${
                    selectedImage === url ? 'border-primary' : 'border-transparent'
                  }`}
                >
                  <img src={url} alt="" className="w-full h-full object-cover" />
                  {selectedImage === url && (
                    <div className="absolute inset-0 bg-primary/20 flex items-center justify-center">
                      <div className="w-5 h-5 rounded-full bg-primary flex items-center justify-center">
                        <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                        </svg>
                      </div>
                    </div>
                  )}
                </button>
              ))}
            </div>
            <label className="w-full py-2.5 rounded-xl text-sm font-bold bg-muted text-muted-foreground text-center cursor-pointer hover:bg-muted/80 transition-colors block">
              Importer une image
              <input type="file" accept="image/*" className="hidden" onChange={e => {
                const f = e.target.files?.[0];
                if (f) { setOffImages([]); setSelectedImage(null); uploadImage(f); }
                e.target.value = '';
              }} />
            </label>
            <div className="flex gap-2 mt-1">
              <button onClick={() => { setOffImages([]); setSelectedImage(null); }} className="flex-1 py-2.5 rounded-xl text-sm font-bold bg-muted text-muted-foreground hover:bg-muted/80 transition-colors">Annuler</button>
              <button
                onClick={() => selectedImage && applyImage(selectedImage)}
                disabled={!selectedImage}
                className="flex-1 py-2.5 rounded-xl text-sm font-bold bg-primary text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
              >
                Appliquer
              </button>
            </div>
          </DialogContent>
        </Dialog>

        {/* Delete confirmation */}
        <AlertDialog open={confirmDelete} onOpenChange={setConfirmDelete}>
          <AlertDialogContent className="max-w-sm rounded-2xl">
            <AlertDialogHeader>
              <AlertDialogTitle>Supprimer ce produit ?</AlertDialogTitle>
              <AlertDialogDescription>
                Cette action est irréversible. Le produit « {product.name} » sera définitivement supprimé.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Annuler</AlertDialogCancel>
              <AlertDialogAction onClick={handleRemove} className="bg-destructive text-destructive-foreground hover:bg-destructive/90">Supprimer</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>

        {/* Edit Sheet */}
        <Sheet open={editing} onOpenChange={setEditing}>
          <SheetContent side="bottom" className="rounded-t-2xl max-h-[85dvh] overflow-y-auto">
            <SheetHeader><SheetTitle>Modifier le produit</SheetTitle></SheetHeader>
            <div className="space-y-4 mt-4 pb-safe">
              <div><Label htmlFor="edit-name">Nom</Label><Input id="edit-name" value={editName} onChange={e => setEditName(e.target.value)} onFocus={e => setTimeout(() => e.target.scrollIntoView({ behavior: 'smooth', block: 'center' }), 300)} /></div>
              <div><Label htmlFor="edit-date">Date de péremption</Label><Input id="edit-date" type="date" value={editDate} onChange={e => setEditDate(e.target.value)} /></div>
              <div><Label htmlFor="edit-brand">Marque</Label><Input id="edit-brand" value={editBrand} onChange={e => setEditBrand(e.target.value)} onFocus={e => setTimeout(() => e.target.scrollIntoView({ behavior: 'smooth', block: 'center' }), 300)} /></div>
              <div className="relative">
                <Label htmlFor="edit-category">Catégorie</Label>
                <select
                  id="edit-category"
                  value={editCategory}
                  onChange={e => setEditCategory(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl bg-muted border border-border text-sm appearance-none"
                >
                  <option value="">Aucune</option>
                  {PRODUCT_CATEGORIES.filter(c => c.key !== 'all').map(c => (
                    <option key={c.key} value={c.key}>{c.label}</option>
                  ))}
                </select>
              </div>
              <button onClick={handleSave} className="w-full py-3 bg-primary text-primary-foreground rounded-xl font-bold hover:bg-primary/90 transition-colors">Enregistrer</button>
            </div>
          </SheetContent>
        </Sheet>

        {/* Opening days dialog */}
        <Dialog open={openDialog} onOpenChange={setOpenDialog}>
          <DialogContent className="max-w-sm rounded-2xl">
            <DialogHeader>
              <DialogTitle>Durée après ouverture</DialogTitle>
              <DialogDescription>Combien de jours ce produit reste-t-il bon après ouverture ?</DialogDescription>
            </DialogHeader>
            <div className="space-y-4 mt-2">
              <div>
                <div className="flex items-center gap-3">
                  <Input
                    type="number"
                    min={1}
                    max={90}
                    value={daysInput}
                    onChange={e => setDaysInput(Math.max(1, parseInt(e.target.value) || 1))}
                    className="text-center text-lg font-bold w-24"
                  />
                  <span className="text-sm text-muted-foreground font-semibold">jours</span>
                </div>
                {(() => {
                  const rec = getRecommendedDaysAfterOpening(product.category, product.subcategory);
                  return (
                    <p className="text-xs text-muted-foreground mt-2 flex items-center gap-1">
                      <Info className="h-3.5 w-3.5 text-primary" /> Recommandé : <span className="font-semibold text-foreground">{rec.label}</span>
                    </p>
                  );
                })()}
              </div>
              <button
                onClick={handleConfirmOpened}
                className="w-full py-3 bg-primary text-primary-foreground rounded-xl font-bold hover:bg-primary/90 transition-colors"
              >
                Confirmer
              </button>
            </div>
          </DialogContent>
        </Dialog>


        <Dialog open={!!scoreDialog} onOpenChange={(open) => !open && setScoreDialog(null)}>
          <DialogContent className="max-w-sm rounded-2xl">
            {activeExplanation && (
              <>
                <DialogHeader>
                  <DialogTitle>{activeExplanation.title}</DialogTitle>
                  <DialogDescription>{activeExplanation.description}</DialogDescription>
                </DialogHeader>
                <div className="space-y-2 mt-2">
                  {activeExplanation.grades.map(g => {
                    const currentValue = scoreDialog === 'nutri' ? nutriGrade : scoreDialog === 'nova' ? String(product.novaGroup || '') : (product.ecoScore || '').toUpperCase();
                    const isActive = g.grade === currentValue;
                    return (
                      <div key={g.grade} className={`flex items-center gap-3 rounded-xl px-3 py-2 transition-colors ${isActive ? 'bg-primary/10 ring-1 ring-primary/30' : ''}`}>
                        <span className={`${g.color} text-white text-xs font-extrabold w-8 h-8 rounded-lg flex items-center justify-center shrink-0`}>{g.grade}</span>
                        <span className={`text-sm ${isActive ? 'font-bold text-foreground' : 'text-card-foreground'}`}>{g.desc}</span>
                      </div>
                    );
                  })}
                </div>
              </>
            )}
          </DialogContent>
        </Dialog>
      </div>
    </PageTransition>
  );
};

export default ProductDetail;
