import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Trash2, Barcode, Clock, Tag, Pencil, AlertTriangle, CircleCheck, PackageOpen, UtensilsCrossed, RotateCcw, Scale, ShieldAlert, Info, SearchX, RefreshCw, X, Snowflake, BarChart2, AlignLeft, Copy, Check } from 'lucide-react';
import { format } from 'date-fns';
import { fr } from 'date-fns/locale';
import { toast } from 'sonner';
import { useProducts } from '@/hooks/useProducts';
import { useAuth } from '@/contexts/AuthContext';
import { supabase } from '@/integrations/supabase/client';
import { getExpirationStatus, getDaysUntilExpiration, getEffectiveExpirationDate, ProductStatus, PRODUCT_CATEGORIES, getPostExpiryNote, getRecommendedDaysAfterOpening, getFreezeDuration } from '@/types/product';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { PageTransition } from '@/components/PageTransition';
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog';
import { motion, useScroll, useTransform } from 'framer-motion';

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

function parseAllergens(raw?: string): string[] {
  if (!raw) return [];
  return Array.from(new Set(raw.split(',').map(translateAllergen).filter(Boolean)));
}

function parseNutritionData(raw?: string): Record<string, number> {
  if (!raw) return {};
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    return Object.fromEntries(
      Object.entries(parsed)
        .map(([key, value]) => [key, typeof value === 'number' ? value : Number(value)])
        .filter(([, value]) => Number.isFinite(value))
    ) as Record<string, number>;
  } catch {
    return {};
  }
}

function formatNutritionValue(value: number, unit: string): string {
  const formatted = unit === 'kcal' || Number.isInteger(value) ? String(Math.round(value)) : value.toFixed(1);
  return `${formatted} ${unit}`;
}

function extractAdditives(ingredients?: string): string[] {
  if (!ingredients) return [];
  const matches = ingredients.match(/\bE\s?\d{3}[a-z]?\b/gi) ?? [];
  return Array.from(new Set(matches.map((match) => match.replace(/\s+/g, '').toUpperCase())));
}

function getNutritionInsights(nutrition: Record<string, number>, rowCount: number): { label: string; className: string }[] {
  const insights: { label: string; className: string }[] = [];

  if (nutrition.sugars >= 15) {
    insights.push({ label: 'Sucré', className: 'border-orange-500/20 bg-orange-500/10 text-orange-600' });
  }
  if (nutrition.salt >= 1.5) {
    insights.push({ label: 'Salé', className: 'border-orange-500/20 bg-orange-500/10 text-orange-600' });
  }
  if (nutrition.proteins >= 12) {
    insights.push({ label: 'Source de protéines', className: 'border-success/20 bg-success/10 text-success' });
  }
  if (nutrition.fiber >= 3) {
    insights.push({ label: 'Source de fibres', className: 'border-success/20 bg-success/10 text-success' });
  }
  if (rowCount > 0 && rowCount < 5) {
    insights.push({ label: 'Données partielles', className: 'border-blue-500/20 bg-blue-500/10 text-blue-600' });
  }

  return insights;
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

function ScoreBadge({ label, value, colorMap, onClick }: { label: string; value: string; colorMap: Record<string | number, string>; onClick?: () => void }) {
  const bg = colorMap[value] || 'bg-muted';
  return (
    <button onClick={onClick} className="flex h-11 flex-col items-center justify-start gap-0.5 group">
      <span className={`${bg} text-white text-xs font-extrabold w-8 h-8 rounded-lg flex items-center justify-center group-hover:ring-2 group-hover:ring-primary/40 transition-all`}>
        {value}
      </span>
      <span className="text-[9px] text-muted-foreground font-semibold">{label}</span>
    </button>
  );
}

function StatusPill({ icon: Icon, label, className }: { icon: React.ElementType; label: string; className: string }) {
  return (
    <div className="flex h-11 flex-col items-center justify-start gap-0.5">
      <span className={`inline-flex h-8 items-center gap-1.5 rounded-lg px-2.5 text-[11px] font-extrabold shadow-sm ${className}`}>
        <Icon className="h-3.5 w-3.5" />
        {label}
      </span>
      <span className="invisible text-[9px] font-semibold">État</span>
    </div>
  );
}

const productStatusBadges: Record<string, { label: string; icon: typeof PackageOpen; color: string }> = {
  opened: { label: 'Ouvert', icon: PackageOpen, color: 'bg-blue-500/10 text-blue-600 border-blue-500/20' },
  consumed: { label: 'Consommé', icon: UtensilsCrossed, color: 'bg-success/10 text-success border-success/20' },
  thrown: { label: 'Jeté', icon: Trash2, color: 'bg-destructive/10 text-destructive border-destructive/20' },
};

function InfoSectionCard({
  icon: Icon,
  title,
  eyebrow,
  children,
  className = '',
}: {
  icon: React.ElementType;
  title: string;
  eyebrow?: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <section className={`rounded-[1.75rem] border border-border bg-card p-4 shadow-sm ${className}`}>
      <div className="mb-4 flex items-center gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-primary/10">
          <Icon className="h-5 w-5 text-primary" />
        </div>
        <div className="min-w-0">
          {eyebrow && (
            <p className="text-[10px] font-extrabold uppercase tracking-[0.18em] text-muted-foreground">{eyebrow}</p>
          )}
          <h2 className="text-base font-black text-card-foreground">{title}</h2>
        </div>
      </div>
      {children}
    </section>
  );
}

function DetailRow({ icon: Icon, label, value }: { icon: React.ElementType; label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center gap-3">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-muted">
        <Icon className="h-4 w-4 text-primary" />
      </div>
      <div className="min-w-0">
        <p className="text-[11px] font-bold text-muted-foreground">{label}</p>
        <p className="text-sm font-extrabold text-card-foreground">{value}</p>
      </div>
    </div>
  );
}

function NutritionSummaryCard({ label, value, tone = 'default' }: { label: string; value: string; tone?: 'default' | 'warning' | 'success' }) {
  const toneClass = tone === 'warning'
    ? 'border-orange-500/20 bg-orange-500/10 text-orange-600'
    : tone === 'success'
      ? 'border-success/20 bg-success/10 text-success'
      : 'border-primary/10 bg-primary/5 text-card-foreground';

  return (
    <div className={`rounded-2xl border p-3 ${toneClass}`}>
      <p className="text-[10px] font-extrabold uppercase tracking-wide opacity-70">{label}</p>
      <p className="mt-1 text-base font-black">{value}</p>
    </div>
  );
}

function TimelineEvent({ icon: Icon, label, date, detail, tone = 'default' }: { icon: React.ElementType; label: string; date: string; detail?: string; tone?: 'default' | 'blue' | 'success' | 'danger' }) {
  const toneClass = tone === 'blue'
    ? 'bg-blue-500/10 text-blue-600'
    : tone === 'success'
      ? 'bg-success/10 text-success'
      : tone === 'danger'
        ? 'bg-destructive/10 text-destructive'
        : 'bg-primary/10 text-primary';

  return (
    <div className="relative flex gap-3 pb-4 last:pb-0">
      <div className="flex flex-col items-center">
        <span className={`flex h-9 w-9 items-center justify-center rounded-2xl ${toneClass}`}>
          <Icon className="h-4 w-4" />
        </span>
        <span className="mt-2 h-full w-px bg-border last:hidden" />
      </div>
      <div className="min-w-0 pt-0.5">
        <p className="text-sm font-extrabold text-card-foreground">{label}</p>
        <p className="text-xs font-semibold text-muted-foreground">{format(new Date(date), 'dd MMMM yyyy', { locale: fr })}</p>
        {detail && <p className="mt-1 text-xs text-muted-foreground">{detail}</p>}
      </div>
    </div>
  );
}

const ProductDetail = () => {
  const { scrollY } = useScroll();
  const heroY = useTransform(scrollY, [0, 300], [0, 80]);
  const heroScale = useTransform(scrollY, [0, 300], [1.1, 1.3]);
  const heroOpacity = useTransform(scrollY, [0, 250], [1, 0.3]);
  const heroBackOpacity = useTransform(scrollY, [60, 160], [1, 0]);
  const stickyOpacity = useTransform(scrollY, [110, 180], [0, 1]);
  const stickyY = useTransform(scrollY, [110, 180], [-8, 0]);
  const mainElementOpacity = useTransform(scrollY, [70, 150], [1, 0]);
  const stickyElementOpacity = useTransform(scrollY, [110, 180], [0, 1]);
  const [showStickyHeader, setShowStickyHeader] = useState(false);
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { products, loading, removeProduct, updateProduct, setProductStatus } = useProducts();
  const { household, isGuest } = useAuth();
  const [editing, setEditing] = useState(false);
  const [loadingImage, setLoadingImage] = useState(false);
  const [offImages, setOffImages] = useState<string[]>([]);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [editName, setEditName] = useState('');
  const [editDate, setEditDate] = useState('');
  const [editBrand, setEditBrand] = useState('');
  const [editCategory, setEditCategory] = useState('');
  const [scoreDialog, setScoreDialog] = useState<ScoreDialogType>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [daysInput, setDaysInput] = useState(3);
  const [showFullscreen, setShowFullscreen] = useState(false);
  const [notes, setNotes] = useState('');
  const [ingredientsExpanded, setIngredientsExpanded] = useState(false);

  const product = products.find(p => p.id === id);
  const productId = product?.id;
  const productNotes = product?.notes;

  useEffect(() => { window.scrollTo(0, 0); }, []);

  useEffect(() => {
    if (productId) setNotes(productNotes ?? '');
  }, [productId, productNotes]);

  useEffect(() => {
    const unsubscribe = scrollY.on('change', (v) => setShowStickyHeader(v > 90));
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
    if (isGuest) {
      toast.info('Les photos personnelles sont disponibles avec un compte.');
      return;
    }
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

  const notesDirty = notes !== (product.notes ?? '');
  const saveNotes = (showToast = false) => {
    updateProduct(product.id, { notes: notes || undefined });
    if (showToast) toast.success('Note sauvegardée');
  };

  const copyBarcode = async () => {
    if (!product.barcode) return;
    try {
      await navigator.clipboard.writeText(product.barcode);
      toast.success('Code-barres copié');
    } catch {
      toast.error('Impossible de copier le code-barres');
    }
  };

  const nutriGrade = (product.nutriScore || '?').toUpperCase();
  const statusBadge = productStatusBadges[currentProductStatus];
  const activeExplanation = scoreDialog ? scoreExplanations[scoreDialog] : null;

  const today = new Date();
  const openedDays = product.openedAt
    ? Math.max(0, Math.ceil((today.getTime() - new Date(product.openedAt).getTime()) / (1000 * 60 * 60 * 24)))
    : 0;
  const dayCounterValue = days < 0 ? Math.abs(days) : days === 0 ? '!' : days;
  const dayCounterLabel = days < 0
    ? `jour${Math.abs(days) > 1 ? 's' : ''} de retard`
    : days === 0
      ? "Expire aujourd'hui"
      : `jour${days > 1 ? 's' : ''} restant${days > 1 ? 's' : ''}`;
  const postExpiryNote = getPostExpiryNote(product.category, product.subcategory);

  const allergens = parseAllergens(product.allergens);
  const additives = extractAdditives(product.ingredients);
  const ingredientsIsLong = (product.ingredients?.length ?? 0) > 280;
  const displayedIngredients = product.ingredients && ingredientsIsLong && !ingredientsExpanded
    ? `${product.ingredients.slice(0, 280).trim()}…`
    : product.ingredients;
  const nutritionObj = parseNutritionData(product.nutritionData);
  const nutritionRows: [string, string, string][] = ([
    ['energy_kcal', 'Énergie', 'kcal'],
    ['fat', 'Matières grasses', 'g'],
    ['saturated_fat', 'dont saturées', 'g'],
    ['carbohydrates', 'Glucides', 'g'],
    ['sugars', 'dont sucres', 'g'],
    ['proteins', 'Protéines', 'g'],
    ['fiber', 'Fibres', 'g'],
    ['salt', 'Sel', 'g'],
  ] as [string, string, string][]).filter(([key]) => nutritionObj[key] != null);
  const nutritionSummary = ([
    ['energy_kcal', 'Énergie', 'kcal', 'default'],
    ['sugars', 'Sucres', 'g', nutritionObj.sugars >= 15 ? 'warning' : 'default'],
    ['salt', 'Sel', 'g', nutritionObj.salt >= 1.5 ? 'warning' : 'default'],
    ['proteins', 'Protéines', 'g', nutritionObj.proteins >= 12 ? 'success' : 'default'],
  ] as [string, string, string, 'default' | 'warning' | 'success'][]).filter(([key]) => nutritionObj[key] != null);
  const nutritionInsights = getNutritionInsights(nutritionObj, nutritionRows.length);

  return (
    <PageTransition>
      <div className="min-h-screen bg-background">
        {/* Sticky header */}
        <motion.div
          style={{ opacity: stickyOpacity, y: stickyY }}
          className={`fixed inset-x-0 top-0 z-30 px-3 pt-[calc(env(safe-area-inset-top)+0.75rem)] ${showStickyHeader ? 'pointer-events-auto' : 'pointer-events-none'}`}
        >
          <div className="flex h-14 items-center gap-2 rounded-2xl border border-border/70 bg-background/90 px-2 pr-14 shadow-lg shadow-foreground/5 backdrop-blur-xl">
            <button onClick={() => navigate('/')} className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl text-foreground transition-colors hover:bg-muted">
              <ArrowLeft className="h-5 w-5" />
            </button>
            <motion.div style={{ opacity: stickyElementOpacity }} className="flex h-8 w-8 flex-shrink-0 items-center justify-center overflow-hidden rounded-xl bg-muted">
              {product.imageUrl ? (
                <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover" />
              ) : (
                <span className="text-sm">🥬</span>
              )}
            </motion.div>
            <motion.span style={{ opacity: stickyElementOpacity }} className="min-w-0 flex-1 truncate text-sm font-extrabold text-foreground">{product.name}</motion.span>
          </div>
        </motion.div>

        <div className="relative overflow-hidden bg-background">
          <div className="relative h-[17rem] overflow-hidden md:h-[21rem]">
            {product.imageUrl ? (
              <>
                <motion.img src={product.imageUrl} alt="" className="absolute inset-0 h-full w-full object-cover blur-2xl" style={{ y: heroY, scale: heroScale, opacity: heroOpacity }} />
                <div className="absolute inset-0 bg-gradient-to-b from-foreground/35 via-foreground/55 to-background dark:from-background/30 dark:via-background/70 dark:to-background" />
              </>
            ) : (
              <div className={`absolute inset-0 bg-gradient-to-br ${config.gradient}`} />
            )}
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(255,255,255,0.26),transparent_32%),radial-gradient(circle_at_85%_10%,rgba(255,255,255,0.18),transparent_30%)]" />
            <motion.button
              onClick={() => navigate('/')}
              style={{ opacity: heroBackOpacity }}
              className={`fixed left-4 top-6 z-40 rounded-full border border-white/25 bg-background/25 p-2.5 text-white shadow-lg backdrop-blur-md transition-colors hover:bg-background/45 dark:text-foreground md:absolute md:z-10 ${showStickyHeader ? 'pointer-events-none' : ''}`}
            >
              <ArrowLeft className="h-5 w-5" />
            </motion.button>
          </div>

          <motion.div className="relative z-10 mx-auto -mt-36 w-full max-w-5xl px-4 pb-8 md:-mt-48 md:px-8" initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.08, duration: 0.28 }}>
            <div className="grid gap-4 lg:grid-cols-[minmax(0,1.08fr)_minmax(20rem,0.92fr)] lg:items-start">
              <section className="overflow-hidden rounded-[2rem] border border-white/35 bg-card/95 shadow-2xl shadow-foreground/10 backdrop-blur-xl dark:border-white/10">
                <div className="flex gap-4 p-4 md:p-5">
                  <motion.div className="shrink-0" initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ delay: 0.14, type: 'spring', stiffness: 280, damping: 24 }}>
                    <motion.div style={{ opacity: mainElementOpacity }} className="relative h-40 w-28 md:h-48 md:w-36">
                      {product.imageUrl ? (
                        <button onClick={() => setShowFullscreen(true)} className="block h-full w-full overflow-hidden rounded-[1.6rem] border-4 border-background bg-muted shadow-xl">
                          <img src={product.imageUrl} alt={product.name} className="h-full w-full object-cover" />
                        </button>
                      ) : (
                        <div className="flex h-full w-full items-center justify-center rounded-[1.6rem] border-4 border-background bg-muted text-4xl shadow-xl">
                          🥬
                        </div>
                      )}
                      <motion.button
                        onClick={() => {
                          if (product.barcode) {
                            fetchOFFImages();
                            return;
                          }
                          if (isGuest) {
                            toast.info('En mode invité, seules les images OpenFoodFacts sont disponibles.');
                            return;
                          }
                          fileInputRef.current?.click();
                        }}
                        disabled={loadingImage}
                        style={{ opacity: mainElementOpacity }}
                        className="absolute -bottom-2 -right-2 z-20 flex h-9 w-9 items-center justify-center rounded-full border border-border bg-background/95 shadow-xl backdrop-blur-sm transition-all hover:scale-105 hover:bg-background disabled:opacity-50"
                        aria-label="Changer l'image du produit"
                      >
                        <RefreshCw className={`h-4 w-4 text-muted-foreground ${loadingImage ? 'animate-spin' : ''}`} />
                      </motion.button>
                    </motion.div>
                    {!isGuest && (
                      <input
                        ref={fileInputRef}
                        type="file"
                        accept="image/*"
                        className="hidden"
                        onChange={e => { const f = e.target.files?.[0]; if (f) uploadImage(f); e.target.value = ''; }}
                      />
                    )}
                  </motion.div>

                  <div className="flex min-w-0 flex-1 flex-col justify-between py-1">
                    <div>
                      <p className="mb-2 text-[10px] font-extrabold uppercase tracking-[0.2em] text-muted-foreground">Fiche produit</p>
                      <motion.h1 style={{ opacity: mainElementOpacity }} className="text-2xl font-black leading-tight text-card-foreground md:text-4xl">{product.name}</motion.h1>
                      <p className="mt-2 text-sm font-semibold text-muted-foreground">
                        {[product.brand ? product.brand.charAt(0).toUpperCase() + product.brand.slice(1) : '', product.quantity].filter(Boolean).join(' · ') || 'Produit du foyer'}
                      </p>
                      {categoryLabel && (
                        <p className="mt-3 inline-flex max-w-full items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-xs font-extrabold text-primary">
                          <Tag className="h-3.5 w-3.5 shrink-0" />
                          <span className="truncate">{categoryLabel}{product.subcategory && ` · ${product.subcategory}`}</span>
                        </p>
                      )}
                    </div>
                  </div>
                </div>

                <div className="border-t border-border bg-background/55 p-4 md:p-5">
                  <div className="flex flex-wrap items-center justify-center gap-3 md:justify-start">
                    <ScoreBadge label="Nutri" value={nutriGrade} colorMap={nutriColors} onClick={() => setScoreDialog('nutri')} />
                    {product.novaGroup && (
                      <ScoreBadge label="NOVA" value={String(product.novaGroup)} colorMap={novaColors} onClick={() => setScoreDialog('nova')} />
                    )}
                    {product.ecoScore && (
                      <ScoreBadge label="Eco" value={product.ecoScore.toUpperCase()} colorMap={ecoColors} onClick={() => setScoreDialog('eco')} />
                    )}
                    <StatusPill icon={StatusIcon} label={config.label} className={config.badge} />
                    {statusBadge && (
                      <StatusPill icon={statusBadge.icon} label={statusBadge.label} className={`border ${statusBadge.color}`} />
                    )}
                    {product.frozenUntil && (
                      <StatusPill icon={Snowflake} label="Congelé" className="border border-blue-500/20 bg-blue-500/10 text-blue-500" />
                    )}
                  </div>
                </div>
              </section>

              <section className={`relative overflow-hidden rounded-[2rem] border p-4 text-center shadow-xl shadow-foreground/5 md:p-5 lg:sticky lg:top-20 ${config.border} ${
                status === 'fresh'
                  ? 'bg-gradient-to-br from-success/20 via-success/10 to-card'
                  : status === 'soon'
                    ? 'bg-gradient-to-br from-warning/20 via-warning/10 to-card'
                    : 'bg-gradient-to-br from-destructive/20 via-destructive/10 to-card'
              }`}>
                <div className="pointer-events-none absolute -right-12 -top-12 h-36 w-36 rounded-full bg-background/55 blur-2xl" />
                <div className="pointer-events-none absolute -bottom-16 left-1/2 h-40 w-40 -translate-x-1/2 rounded-full bg-background/40 blur-2xl" />
                <div className="relative">
                  <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-[1.1rem] border border-white/50 bg-background/75 shadow-lg shadow-foreground/5 backdrop-blur-sm md:mb-4 md:h-16 md:w-16 md:rounded-[1.35rem]">
                    <StatusIcon className={`h-6 w-6 md:h-8 md:w-8 ${config.iconColor}`} />
                  </div>
                  <p className="text-[10px] font-black uppercase tracking-[0.22em] text-muted-foreground">Date limite</p>
                  <p className={`mt-1 text-6xl font-black leading-none tracking-tight md:mt-2 md:text-7xl ${config.iconColor}`}>{dayCounterValue}</p>
                  <p className="mt-1 text-base font-black text-card-foreground md:mt-2 md:text-lg">{dayCounterLabel}</p>
                  <p className="mt-1 text-sm font-bold text-muted-foreground">
                    {format(new Date(effectiveDate), 'dd MMMM yyyy', { locale: fr })}
                  </p>
                </div>

                <div className="relative mt-4 space-y-2.5 md:mt-5 md:space-y-3">
                  {product.openedAt && product.daysAfterOpening != null && (
                    <div className="rounded-2xl border border-primary/20 bg-background/75 p-3 text-left shadow-sm backdrop-blur-sm">
                      <div className="flex items-center gap-2">
                        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                          <PackageOpen className="h-4 w-4 text-primary" />
                        </span>
                        <p className="text-sm font-extrabold text-primary">Ouvert depuis {openedDays} jour{openedDays > 1 ? 's' : ''}</p>
                      </div>
                      {effectiveDate !== product.expirationDate && (
                        <p className="mt-2 pl-10 text-xs font-semibold text-muted-foreground">
                          Date effective : {format(new Date(effectiveDate), 'dd MMMM yyyy', { locale: fr })}
                        </p>
                      )}
                    </div>
                  )}

                  {product.frozenUntil && (
                    <div className="rounded-2xl border border-blue-500/20 bg-background/75 p-3 text-left shadow-sm backdrop-blur-sm">
                      <div className="flex items-center gap-2">
                        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-blue-500/10">
                          <Snowflake className="h-4 w-4 text-blue-500" />
                        </span>
                        <p className="text-sm font-extrabold text-blue-500">Congelé jusqu'au {format(new Date(product.frozenUntil), 'dd MMM yyyy', { locale: fr })}</p>
                      </div>
                    </div>
                  )}

                  {postExpiryNote && (
                    <div className="flex items-start gap-3 rounded-2xl border border-blue-500/20 bg-background/75 p-3 text-left shadow-sm backdrop-blur-sm">
                      <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-blue-500/10">
                        <Info className="h-4 w-4 text-blue-500" />
                      </span>
                      <p className="text-sm text-card-foreground">
                        Ce produit peut généralement être consommé jusqu'à <span className="font-extrabold">{postExpiryNote}</span> après la date d'expiration.
                      </p>
                    </div>
                  )}
                </div>
              </section>
            </div>

            <motion.div className="mt-4 grid gap-4 lg:grid-cols-2" initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.16, duration: 0.25 }}>
              <InfoSectionCard icon={PackageOpen} title="Actions rapides" eyebrow="Statut">
                <div className="grid grid-cols-3 gap-2">
                  {([
                    { status: 'opened' as ProductStatus, icon: PackageOpen, label: 'Ouvert', color: 'text-blue-500', bg: 'bg-blue-500/10', activeBg: 'bg-blue-500 text-white' },
                    { status: 'consumed' as ProductStatus, icon: UtensilsCrossed, label: 'Consommé', color: 'text-success', bg: 'bg-success/10', activeBg: 'bg-success text-success-foreground' },
                    { status: 'thrown' as ProductStatus, icon: Trash2, label: 'Jeté', color: 'text-destructive', bg: 'bg-destructive/10', activeBg: 'bg-destructive text-destructive-foreground' },
                  ]).map(item => {
                    const isActive = currentProductStatus === item.status;
                    return (
                      <button key={item.status} onClick={() => handleStatusChange(isActive ? 'active' : item.status)}
                        className={`flex flex-col items-center gap-1.5 rounded-2xl border p-3 transition-all hover:-translate-y-0.5 ${isActive ? `${item.activeBg} border-transparent shadow-md` : `${item.bg} border-border hover:border-muted-foreground/20`}`}>
                        <item.icon className={`h-5 w-5 ${isActive ? '' : item.color}`} />
                        <span className={`text-[10px] font-extrabold ${isActive ? '' : item.color}`}>{item.label}</span>
                      </button>
                    );
                  })}
                </div>
                {currentProductStatus !== 'active' && (
                  <button onClick={() => handleStatusChange('active')} className="mt-3 flex w-full items-center justify-center gap-1.5 py-2 text-xs font-bold text-muted-foreground transition-colors hover:text-foreground">
                    <RotateCcw className="h-3.5 w-3.5" /> Remettre en actif
                  </button>
                )}
                {!product.frozenUntil ? (
                  <button onClick={handleFreeze} className="mt-3 flex w-full items-center justify-center gap-1.5 rounded-2xl border border-blue-500/20 bg-blue-500/10 py-3 text-xs font-extrabold text-blue-600 transition-colors hover:bg-blue-500/20">
                    <Snowflake className="h-4 w-4" /> Mettre au congélateur ({getFreezeDuration(product.category)} mois)
                  </button>
                ) : (
                  <button onClick={handleUnfreeze} className="mt-3 flex w-full items-center justify-center gap-1.5 py-2 text-xs font-bold text-blue-500 transition-colors hover:text-blue-700">
                    <RotateCcw className="h-3.5 w-3.5" /> Retirer du congélateur
                  </button>
                )}
              </InfoSectionCard>

              <Accordion type="multiple" defaultValue={['nutrition']} className="space-y-3 lg:col-span-2">
                <AccordionItem value="nutrition" className="overflow-hidden rounded-[1.75rem] border border-border bg-card shadow-sm">
                  <AccordionTrigger className="px-4 py-3.5 hover:no-underline [&[data-state=open]]:bg-primary/5">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-primary/10">
                        <BarChart2 className="h-5 w-5 text-primary" />
                      </div>
                      <p className="text-base font-black text-card-foreground">Nutrition & Allergènes</p>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="px-4 pb-4">
                    {nutritionRows.length === 0 && allergens.length === 0 ? (
                      <div className="rounded-2xl bg-muted p-4 text-center">
                        <p className="text-sm font-extrabold text-card-foreground">Aucune information nutritionnelle disponible</p>
                        <p className="mt-1 text-xs font-semibold text-muted-foreground">Change l'image ou complète les infos depuis OpenFoodFacts pour enrichir cette fiche.</p>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        {allergens.length > 0 && (
                          <div className="rounded-2xl border border-destructive/20 bg-destructive/10 p-3">
                            <div className="mb-2 flex items-center gap-2">
                              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-background/70">
                                <ShieldAlert className="h-4 w-4 text-destructive" />
                              </span>
                              <div>
                                <p className="text-sm font-black text-destructive">Allergènes à vérifier</p>
                                <p className="text-xs font-semibold text-destructive/80">À confirmer sur l'emballage avant consommation.</p>
                              </div>
                            </div>
                            <div className="flex flex-wrap gap-1.5">
                              {allergens.map((allergen) => (
                                <span key={allergen} className="rounded-full border border-destructive/20 bg-background/80 px-2.5 py-1 text-[10px] font-extrabold text-destructive">
                                  {allergen}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}

                        {nutritionSummary.length > 0 && (
                          <div className="grid grid-cols-2 gap-2 md:grid-cols-4">
                            {nutritionSummary.map(([key, label, unit, tone]) => (
                              <NutritionSummaryCard key={key} label={label} value={formatNutritionValue(nutritionObj[key], unit)} tone={tone} />
                            ))}
                          </div>
                        )}

                        {nutritionInsights.length > 0 && (
                          <div className="flex flex-wrap gap-1.5">
                            {nutritionInsights.map((insight) => (
                              <span key={insight.label} className={`rounded-full border px-2.5 py-1 text-[10px] font-extrabold ${insight.className}`}>
                                {insight.label}
                              </span>
                            ))}
                          </div>
                        )}

                        {nutritionRows.length > 0 && (
                          <div>
                            <h3 className="mb-2 text-xs font-extrabold uppercase tracking-wider text-muted-foreground">Valeurs nutritionnelles <span className="font-semibold normal-case">pour 100 g</span></h3>
                            <div className="overflow-hidden rounded-2xl border border-border">
                              {nutritionRows.map(([key, label, unit]) => (
                                <div key={key} className={`flex items-center justify-between gap-3 border-b border-border px-4 py-2.5 last:border-0 ${key === 'saturated_fat' || key === 'sugars' ? 'pl-7' : ''}`}>
                                  <span className="text-sm text-card-foreground">{label}</span>
                                  <span className="text-sm font-extrabold text-card-foreground">{formatNutritionValue(nutritionObj[key], unit)}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </AccordionContent>
                </AccordionItem>

                <AccordionItem value="ingredients" className="overflow-hidden rounded-[1.75rem] border border-border bg-card shadow-sm">
                  <AccordionTrigger className="px-4 py-3.5 hover:no-underline [&[data-state=open]]:bg-primary/5">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-primary/10">
                        <AlignLeft className="h-5 w-5 text-primary" />
                      </div>
                      <p className="text-base font-black text-card-foreground">Ingrédients</p>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="px-4 pb-4">
                    {product.ingredients ? (
                      <div className="space-y-3">
                        <div className="rounded-2xl border border-border bg-muted/50 p-4">
                          <p className="max-w-3xl text-sm leading-7 text-card-foreground">{displayedIngredients}</p>
                          {ingredientsIsLong && (
                            <button
                              type="button"
                              onClick={() => setIngredientsExpanded((expanded) => !expanded)}
                              className="mt-3 text-xs font-extrabold text-primary transition-colors hover:text-primary/80"
                            >
                              {ingredientsExpanded ? 'Voir moins' : 'Voir la liste complète'}
                            </button>
                          )}
                        </div>

                        {allergens.length > 0 && (
                          <div className="rounded-2xl border border-destructive/20 bg-destructive/10 p-3">
                            <p className="mb-2 text-xs font-extrabold uppercase tracking-wider text-destructive">Allergènes signalés</p>
                            <div className="flex flex-wrap gap-1.5">
                              {allergens.map((allergen) => (
                                <span key={allergen} className="rounded-full border border-destructive/20 bg-background/80 px-2.5 py-1 text-[10px] font-extrabold text-destructive">
                                  {allergen}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}

                        {additives.length > 0 && (
                          <div>
                            <p className="mb-2 text-xs font-extrabold uppercase tracking-wider text-muted-foreground">Additifs détectés</p>
                            <div className="flex flex-wrap gap-1.5">
                              {additives.map((additive) => (
                                <span key={additive} className="rounded-full border border-orange-500/20 bg-orange-500/10 px-2.5 py-1 text-[10px] font-extrabold text-orange-600">
                                  {additive}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}

                        <p className="text-[10px] font-semibold text-muted-foreground/70">
                          Liste issue d'OpenFoodFacts, à vérifier sur l'emballage.
                        </p>
                      </div>
                    ) : (
                      <div className="rounded-2xl bg-muted p-4 text-center">
                        <p className="text-sm font-extrabold text-card-foreground">Aucun ingrédient renseigné</p>
                        <p className="mt-1 text-xs font-semibold text-muted-foreground">Change l'image du produit ou complète la fiche pour retrouver la composition ici.</p>
                      </div>
                    )}
                  </AccordionContent>
                </AccordionItem>

                <AccordionItem value="details" className="overflow-hidden rounded-[1.75rem] border border-border bg-card shadow-sm">
                  <AccordionTrigger className="px-4 py-3.5 hover:no-underline [&[data-state=open]]:bg-primary/5">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-primary/10">
                        <Info className="h-5 w-5 text-primary" />
                      </div>
                      <p className="text-base font-black text-card-foreground">Détails & historique</p>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="px-4 pb-4">
                    <div className="space-y-4">
                      <div>
                        <h3 className="mb-3 text-xs font-extrabold uppercase tracking-wider text-muted-foreground">Identité</h3>
                        <div className="grid gap-3 sm:grid-cols-2">
                          {product.brand && <DetailRow icon={Tag} label="Marque" value={product.brand.charAt(0).toUpperCase() + product.brand.slice(1)} />}
                          {product.quantity && <DetailRow icon={Scale} label="Quantité" value={product.quantity} />}
                          {product.category && (
                            <DetailRow
                              icon={Tag}
                              label="Catégorie"
                              value={<>{categoryLabel}{product.subcategory && (<><span className="mx-1 font-semibold text-muted-foreground">›</span>{product.subcategory}</>)}</>}
                            />
                          )}
                        </div>
                      </div>

                      <div>
                        <label className="mb-1.5 block text-xs font-extrabold uppercase tracking-wide text-muted-foreground">Note du foyer</label>
                        <div className="relative">
                          <textarea
                            value={notes}
                            onChange={e => setNotes(e.target.value)}
                            onBlur={() => { if (notesDirty) saveNotes(); }}
                            placeholder="Ajouter une note utile pour tout le foyer…"
                            rows={3}
                            className="w-full resize-none rounded-2xl border border-border bg-muted py-2 pl-3 pr-14 pb-12 text-sm text-foreground focus:outline-none focus:ring-1 focus:ring-primary"
                          />
                          <button
                            type="button"
                            onMouseDown={(event) => event.preventDefault()}
                            onClick={() => saveNotes(true)}
                            className={`absolute bottom-3 right-3 flex h-9 w-9 items-center justify-center rounded-full border shadow-sm transition-all ${
                              notesDirty
                                ? 'border-primary/20 bg-primary text-primary-foreground hover:scale-105 hover:bg-primary/90'
                                : 'border-border bg-background/90 text-muted-foreground hover:text-primary'
                            }`}
                            aria-label="Sauvegarder la note"
                          >
                            <Check className="h-4 w-4" />
                          </button>
                        </div>
                      </div>

                      <div>
                        <h3 className="mb-3 text-xs font-extrabold uppercase tracking-wider text-muted-foreground">Historique</h3>
                        <div className="rounded-2xl border border-border bg-muted/40 p-3">
                          <TimelineEvent icon={Clock} label="Produit ajouté" date={product.addedAt} />
                          {product.openedAt && (
                            <TimelineEvent
                              icon={PackageOpen}
                              label="Produit ouvert"
                              date={product.openedAt}
                              detail={product.daysAfterOpening != null ? `À consommer idéalement sous ${product.daysAfterOpening} jour${product.daysAfterOpening > 1 ? 's' : ''} après ouverture.` : undefined}
                              tone="blue"
                            />
                          )}
                          {product.frozenUntil && (
                            <TimelineEvent icon={Snowflake} label="Congélation active" date={product.frozenUntil} detail="La date effective prend cette congélation en compte." tone="blue" />
                          )}
                          {product.status === 'consumed' && product.statusChangedAt && (
                            <TimelineEvent icon={UtensilsCrossed} label="Produit consommé" date={product.statusChangedAt} tone="success" />
                          )}
                          {product.status === 'thrown' && product.statusChangedAt && (
                            <TimelineEvent icon={Trash2} label="Produit jeté" date={product.statusChangedAt} tone="danger" />
                          )}
                        </div>
                        {product.barcode && (
                          <div className="mt-2 flex items-center justify-center gap-1.5 px-3">
                            <Barcode className="h-3.5 w-3.5 shrink-0 text-muted-foreground/45" />
                            <span className="truncate font-mono text-[10px] text-muted-foreground/50">{product.barcode}</span>
                            <button
                              type="button"
                              onClick={copyBarcode}
                              className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-muted-foreground/45 transition-colors hover:bg-muted hover:text-primary"
                              aria-label="Copier le code-barres"
                            >
                              <Copy className="h-3 w-3" />
                            </button>
                          </div>
                        )}
                      </div>

                      <p className="px-4 text-center text-[10px] text-muted-foreground/50">
                        Les informations proviennent d'OpenFoodFacts et peuvent être inexactes ou incomplètes.
                      </p>
                    </div>
                  </AccordionContent>
                </AccordionItem>
              </Accordion>
            </motion.div>

            <div className="mt-5 grid gap-2 pb-2 sm:grid-cols-2">
              <button onClick={openEdit} className="flex w-full items-center justify-center gap-2 rounded-2xl bg-primary px-4 py-3.5 font-extrabold text-primary-foreground transition-colors hover:bg-primary/90">
                <Pencil className="h-4 w-4" /> Modifier
              </button>
              <button onClick={() => setConfirmDelete(true)} className="flex w-full items-center justify-center gap-2 rounded-2xl bg-destructive/10 px-4 py-3.5 font-extrabold text-destructive transition-colors hover:bg-destructive/20">
                <Trash2 className="h-4 w-4" /> Supprimer
              </button>
            </div>
          </motion.div>
        </div>

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
            {!isGuest && (
              <label className="w-full py-2.5 rounded-xl text-sm font-bold bg-muted text-muted-foreground text-center cursor-pointer hover:bg-muted/80 transition-colors block">
                Importer une image
                <input type="file" accept="image/*" className="hidden" onChange={e => {
                  const f = e.target.files?.[0];
                  if (f) { setOffImages([]); setSelectedImage(null); uploadImage(f); }
                  e.target.value = '';
                }} />
              </label>
            )}
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
