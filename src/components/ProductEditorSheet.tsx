import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Barcode,
  Camera,
  Check,
  ImagePlus,
  Loader2,
  PackagePlus,
  RefreshCw,
  Save,
  ScanBarcode,
  ScanText,
  Sparkles,
  X,
} from 'lucide-react';
import { toast } from 'sonner';
import { BarcodeScanner } from './BarcodeScanner';
import { DateScanner } from './DateScanner';
import { useAuth } from '@/contexts/AuthContext';
import { supabase } from '@/integrations/supabase/client';
import { useIsMobile } from '@/hooks/use-mobile';
import { Product, matchCategory, matchSubcategory, PRODUCT_CATEGORIES } from '@/types/product';
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { Textarea } from '@/components/ui/textarea';

type EditorMode = 'create' | 'edit';
type CreateVariant = 'single' | 'multi';
type ScanStep = 'form' | 'barcode' | 'date';

type NullableTextField =
  | 'barcode'
  | 'imageUrl'
  | 'brand'
  | 'nutriScore'
  | 'category'
  | 'subcategory'
  | 'quantity'
  | 'ecoScore'
  | 'allergens'
  | 'ingredients'
  | 'notes'
  | 'nutritionData';

export type ProductEditorPayload = Omit<
  Partial<Omit<Product, 'id' | 'addedAt'>>,
  NullableTextField | 'name' | 'expirationDate' | 'novaGroup'
> & {
  name: string;
  expirationDate: string;
  novaGroup?: number | null;
} & Partial<Record<NullableTextField, string | null>>;

interface ProductEditorSheetProps {
  open: boolean;
  mode: EditorMode;
  variant?: CreateVariant;
  product?: Product;
  onClose: () => void;
  onSubmit: (product: ProductEditorPayload) => void | Promise<void>;
}

interface ProductFormState {
  name: string;
  barcode: string;
  expirationDate: string;
  imageUrl: string;
  brand: string;
  nutriScore: string;
  category: string;
  quantity: string;
  novaGroup: string;
  ecoScore: string;
  allergens: string;
  ingredients: string;
  notes: string;
  nutritionData: string;
}

const emptyForm: ProductFormState = {
  name: '',
  barcode: '',
  expirationDate: '',
  imageUrl: '',
  brand: '',
  nutriScore: '',
  category: '',
  quantity: '',
  novaGroup: '',
  ecoScore: '',
  allergens: '',
  ingredients: '',
  notes: '',
  nutritionData: '',
};

function productToForm(product?: Product): ProductFormState {
  if (!product) return emptyForm;
  return {
    name: product.name,
    barcode: product.barcode ?? '',
    expirationDate: product.expirationDate.split('T')[0],
    imageUrl: product.imageUrl ?? '',
    brand: product.brand ?? '',
    nutriScore: product.nutriScore ?? '',
    category: product.category ?? '',
    quantity: product.quantity ?? '',
    novaGroup: product.novaGroup != null ? String(product.novaGroup) : '',
    ecoScore: product.ecoScore ?? '',
    allergens: product.allergens ?? '',
    ingredients: product.ingredients ?? '',
    notes: product.notes ?? '',
    nutritionData: product.nutritionData ?? '',
  };
}

function uniqueUrls(urls: Array<string | undefined | null>) {
  return [...new Set(urls.filter(Boolean) as string[])];
}

export function ProductEditorSheet({
  open,
  mode,
  variant = 'single',
  product,
  onClose,
  onSubmit,
}: ProductEditorSheetProps) {
  const { household, isGuest } = useAuth();
  const isMobile = useIsMobile();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [form, setForm] = useState<ProductFormState>(emptyForm);
  const [step, setStep] = useState<ScanStep>('form');
  const [lookingUp, setLookingUp] = useState(false);
  const [saving, setSaving] = useState(false);
  const [loadingImage, setLoadingImage] = useState(false);
  const [offImages, setOffImages] = useState<string[]>([]);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);

  const isEditing = mode === 'edit';
  const hasAdvancedData = Boolean(
    form.barcode ||
    form.nutriScore ||
    form.novaGroup ||
    form.ecoScore ||
    form.allergens ||
    form.ingredients ||
    form.notes,
  );

  useEffect(() => {
    if (!open) return;
    setForm(productToForm(product));
    setOffImages([]);
    setSelectedImage(null);
    setLoadingImage(false);
    setLookingUp(false);
    setSaving(false);
    setStep(mode === 'create' && variant === 'multi' ? 'barcode' : 'form');
  }, [open, product, mode, variant]);

  const updateField = <K extends keyof ProductFormState>(key: K, value: ProductFormState[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const lookupBarcode = useCallback(async (code: string, options?: { openDateScannerAfterLookup?: boolean }) => {
    const cleanCode = code.trim();
    if (!cleanCode) return;
    updateField('barcode', cleanCode);
    setStep(options?.openDateScannerAfterLookup ? 'date' : 'form');
    setLookingUp(true);

    try {
      const res = await fetch(`https://world.openfoodfacts.org/api/v0/product/${cleanCode}.json`);
      const data = await res.json();

      if (data.status !== 1 || !data.product) {
        toast.error('Produit non trouvé. Complétez la fiche manuellement.');
        return;
      }

      const p = data.product;
      const offImage = p.image_url || p.image_front_url || p.image_small_url || '';
      let preferredImage = offImage;

      if (!offImage && household && cleanCode) {
        const { data: existing } = await supabase
          .from('products')
          .select('image_url')
          .eq('household_id', household.id)
          .eq('barcode', cleanCode)
          .not('image_url', 'is', null)
          .limit(1)
          .maybeSingle();

        if (existing?.image_url) preferredImage = existing.image_url;
      }

      const grade = (p.nutriscore_grade || '').toUpperCase();
      const eco = (p.ecoscore_grade || '').toUpperCase();
      const matchedCategory = matchCategory(p.categories?.split(',')[0]?.trim(), p.product_name);
      const n = p.nutriments || {};
      const nutrition: Record<string, number> = {};
      const nutritionFields: [string, string][] = [
        ['energy_kcal', 'energy-kcal_100g'],
        ['proteins', 'proteins_100g'],
        ['carbohydrates', 'carbohydrates_100g'],
        ['fat', 'fat_100g'],
        ['saturated_fat', 'saturated-fat_100g'],
        ['sugars', 'sugars_100g'],
        ['fiber', 'fiber_100g'],
        ['salt', 'salt_100g'],
      ];

      nutritionFields.forEach(([key, offKey]) => {
        if (n[offKey] != null) nutrition[key] = n[offKey];
      });

      setForm((current) => ({
        ...current,
        barcode: cleanCode,
        name: p.product_name || current.name,
        imageUrl: preferredImage || current.imageUrl,
        brand: p.brands || current.brand,
        nutriScore: grade === 'UNKNOWN' ? '' : grade,
        category: matchedCategory || current.category,
        quantity: p.quantity || current.quantity,
        novaGroup: p.nova_group ? String(Number(p.nova_group)) : current.novaGroup,
        ecoScore: eco === 'UNKNOWN' || eco === 'NOT-APPLICABLE' ? '' : eco,
        allergens: p.allergens || current.allergens,
        ingredients: p.ingredients_text_fr || p.ingredients_text || current.ingredients,
        nutritionData: Object.keys(nutrition).length > 0 ? JSON.stringify(nutrition) : current.nutritionData,
      }));
      toast.success('Produit trouvé !');
    } catch {
      toast.error('Erreur de recherche. Complétez la fiche manuellement.');
    } finally {
      setLookingUp(false);
    }
  }, [household]);

  const fetchOFFImages = useCallback(async () => {
    if (!form.barcode.trim()) {
      toast.info('Ajoutez un code-barres pour chercher les images OpenFoodFacts.');
      return;
    }

    setLoadingImage(true);
    try {
      const res = await fetch(`https://world.openfoodfacts.org/api/v0/product/${form.barcode.trim()}.json`);
      const data = await res.json();
      const p = data?.product;

      if (!p) {
        toast.error('Produit introuvable sur OpenFoodFacts');
        return;
      }

      const candidates = uniqueUrls([
        p.image_url,
        p.image_front_url,
        p.image_nutrition_url,
        p.image_ingredients_url,
        p.image_packaging_url,
      ]);

      if (candidates.length === 0) {
        toast.error('Aucune image disponible sur OpenFoodFacts');
      } else {
        setOffImages(candidates);
        setSelectedImage(candidates[0]);
      }
    } catch {
      toast.error('Erreur lors de la récupération des images');
    } finally {
      setLoadingImage(false);
    }
  }, [form.barcode]);

  const uploadImage = async (file: File) => {
    if (isGuest) {
      toast.info('Les photos personnelles sont disponibles avec un compte.');
      return;
    }
    if (!household) return;

    setLoadingImage(true);
    const ext = file.name.split('.').pop() ?? 'jpg';
    const prefix = product?.id ?? 'upload';
    const path = `${household.id}/${prefix}_${Date.now().toString(36)}.${ext}`;
    const { error } = await supabase.storage.from('product-images').upload(path, file, { upsert: true });

    if (error) {
      toast.error(`Upload: ${error.message}`);
      setLoadingImage(false);
      return;
    }

    const { data } = supabase.storage.from('product-images').getPublicUrl(path);
    updateField('imageUrl', data.publicUrl);
    setLoadingImage(false);
  };

  const handleDateFound = (date: string) => {
    updateField('expirationDate', date);
    setStep('form');
    toast.success('Date détectée !', { duration: 1500 });
  };

  const resetForNextScan = () => {
    setForm(emptyForm);
    setOffImages([]);
    setSelectedImage(null);
    setStep('barcode');
  };

  const optionalText = (value: string) => {
    const trimmed = value.trim();
    if (trimmed) return trimmed;
    return isEditing ? null : undefined;
  };

  const handleSubmit = async () => {
    if (!form.name.trim() || !form.expirationDate) {
      toast.error('Veuillez remplir le nom et la date de péremption.');
      return;
    }

    const finalCategory = form.category || matchCategory(undefined, form.name.trim());
    const finalSubcategory = matchSubcategory(finalCategory, undefined, form.name.trim());
    const nova = form.novaGroup ? Number(form.novaGroup) : undefined;

    const payload: ProductEditorPayload = {
      name: form.name.trim(),
      expirationDate: form.expirationDate,
      barcode: optionalText(form.barcode),
      imageUrl: optionalText(form.imageUrl),
      brand: optionalText(form.brand),
      nutriScore: optionalText(form.nutriScore),
      category: finalCategory || (isEditing ? null : undefined),
      subcategory: finalSubcategory ?? (isEditing ? null : undefined),
      quantity: optionalText(form.quantity),
      novaGroup: Number.isFinite(nova) ? nova : isEditing ? null : undefined,
      ecoScore: optionalText(form.ecoScore),
      allergens: optionalText(form.allergens),
      ingredients: optionalText(form.ingredients),
      notes: optionalText(form.notes),
      nutritionData: optionalText(form.nutritionData),
    };

    setSaving(true);
    try {
      await onSubmit(payload);
      if (mode === 'create' && variant === 'multi') {
        resetForNextScan();
        toast.success('Produit ajouté ! Scannez le suivant.');
      } else {
        onClose();
        toast.success(isEditing ? 'Produit mis à jour' : 'Produit ajouté !');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleClose = () => {
    setOffImages([]);
    setSelectedImage(null);
    onClose();
  };

  if (!open) return null;

  if (step === 'barcode') {
    return (
      <BarcodeScanner
        onScan={(code) => lookupBarcode(code, { openDateScannerAfterLookup: mode === 'create' && variant === 'multi' })}
        onClose={handleClose}
      />
    );
  }

  if (step === 'date') {
    return <DateScanner onDateFound={handleDateFound} onClose={() => setStep('form')} />;
  }

  const title = isEditing ? 'Modifier le produit' : variant === 'multi' ? 'Confirmer le produit' : 'Ajouter un produit';
  const description = isEditing
    ? 'Corrigez les informations utiles pour tout le foyer.'
    : variant === 'multi'
      ? 'Vérifiez les infos trouvées avant de scanner le suivant.'
      : 'Renseignez uniquement l’essentiel, le reste peut attendre.';
  const actionLabel = isEditing
    ? 'Enregistrer'
    : variant === 'multi'
      ? 'Ajouter & scanner le suivant'
      : 'Ajouter au frigo';
  const ActionIcon = isEditing ? Save : PackagePlus;

  const content = (
    <div className="relative flex max-h-[88dvh] w-full min-w-0 flex-col overflow-hidden">
      <div className="border-b border-border px-5 py-4 md:px-6">
        <div className="flex items-start justify-between gap-4 pr-8 md:pr-0">
          <div>
            <h2 className="text-xl font-black text-foreground">{title}</h2>
            <p className="mt-1 text-sm font-medium text-muted-foreground">{description}</p>
          </div>
          <Badge variant="secondary" className="hidden shrink-0 md:inline-flex">
            {isEditing ? 'Édition' : variant === 'multi' ? 'Scan en chaîne' : 'Ajout'}
          </Badge>
        </div>
      </div>

      <div className="min-w-0 flex-1 overflow-y-auto px-5 py-5 md:px-6">
        <div className="grid min-w-0 gap-5 lg:grid-cols-[18rem_minmax(0,1fr)]">
          <aside className="min-w-0 space-y-3">
            <div className="min-w-0 overflow-hidden rounded-2xl border border-border bg-muted/40 p-3">
              <div className="flex min-w-0 gap-3 lg:block">
                <div className="relative h-24 w-24 shrink-0 overflow-hidden rounded-2xl border border-border bg-background lg:h-52 lg:w-full">
                  {form.imageUrl ? (
                    <img src={form.imageUrl} alt={form.name || 'Produit'} className="h-full w-full object-cover" />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center">
                      <Camera className="h-9 w-9 text-muted-foreground/45" />
                    </div>
                  )}
                  {loadingImage && (
                    <div className="absolute inset-0 flex items-center justify-center bg-background/60">
                      <Loader2 className="h-6 w-6 animate-spin text-primary" />
                    </div>
                  )}
                </div>

                <div className="min-w-0 flex-1 overflow-hidden lg:mt-3">
                  <p className="line-clamp-2 break-words text-base font-black leading-tight text-card-foreground">
                    {form.name || 'Nouveau produit'}
                  </p>
                  <p className="mt-1 truncate text-sm font-semibold text-muted-foreground">
                    {[form.brand, form.quantity].filter(Boolean).join(' · ') || 'Fiche du foyer'}
                  </p>
                  {form.barcode && (
                    <p className="mt-2 flex items-center gap-1.5 truncate font-mono text-[11px] text-muted-foreground">
                      <Barcode className="h-3.5 w-3.5 shrink-0" />
                      {form.barcode}
                    </p>
                  )}
                </div>
              </div>

              <div className="mt-3 grid min-w-0 grid-cols-2 gap-2">
                <Button type="button" variant="secondary" size="sm" className="min-w-0 px-2" onClick={fetchOFFImages} disabled={loadingImage}>
                  <RefreshCw className="h-4 w-4" />
                  <span className="truncate">Images</span>
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="min-w-0 px-2"
                  onClick={() => {
                    if (isGuest) {
                      toast.info('En mode invité, seules les images OpenFoodFacts sont disponibles.');
                      return;
                    }
                    fileInputRef.current?.click();
                  }}
                  disabled={loadingImage}
                >
                  <ImagePlus className="h-4 w-4" />
                  <span className="truncate">Importer</span>
                </Button>
              </div>

              {form.imageUrl && (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="mt-2 w-full text-muted-foreground"
                  onClick={() => updateField('imageUrl', '')}
                >
                  <X className="h-4 w-4" />
                  Retirer l’image
                </Button>
              )}
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) uploadImage(file);
                event.target.value = '';
              }}
            />
          </aside>

          <div className="min-w-0 space-y-5">
            <section className="rounded-2xl border border-border bg-card p-4 shadow-sm">
              <div className="mb-4 flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-primary" />
                <h3 className="text-sm font-black uppercase tracking-wide text-card-foreground">Essentiel</h3>
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <div className="md:col-span-2">
                  <Label htmlFor="product-name">Nom du produit</Label>
                  <Input
                    id="product-name"
                    value={form.name}
                    onChange={(event) => updateField('name', event.target.value)}
                    placeholder="Ex: Yaourt nature"
                    className="mt-1.5 h-11 rounded-xl"
                  />
                </div>

                <div>
                  <Label htmlFor="product-date">Date de péremption</Label>
                  <div className="mt-1.5 flex gap-2">
                    <Input
                      id="product-date"
                      type="date"
                      value={form.expirationDate}
                      onChange={(event) => updateField('expirationDate', event.target.value)}
                      className="h-11 rounded-xl"
                    />
                    <Button type="button" variant="secondary" size="icon" className="h-11 w-11 rounded-xl" onClick={() => setStep('date')}>
                      <ScanText className="h-4 w-4" />
                      <span className="sr-only">Scanner la date</span>
                    </Button>
                  </div>
                </div>

                <div>
                  <Label htmlFor="product-category">Catégorie</Label>
                  <select
                    id="product-category"
                    value={form.category}
                    onChange={(event) => updateField('category', event.target.value)}
                    className="mt-1.5 flex h-11 w-full rounded-xl border border-input bg-background px-3 py-2 text-sm text-foreground ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                  >
                    <option value="">Automatique</option>
                    {PRODUCT_CATEGORIES.filter((category) => category.key !== 'all').map((category) => (
                      <option key={category.key} value={category.key}>{category.label}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <Label htmlFor="product-brand">Marque</Label>
                  <Input
                    id="product-brand"
                    value={form.brand}
                    onChange={(event) => updateField('brand', event.target.value)}
                    placeholder="Ex: Danone"
                    className="mt-1.5 h-11 rounded-xl"
                  />
                </div>

                <div>
                  <Label htmlFor="product-quantity">Quantité</Label>
                  <Input
                    id="product-quantity"
                    value={form.quantity}
                    onChange={(event) => updateField('quantity', event.target.value)}
                    placeholder="Ex: 4 x 125g"
                    className="mt-1.5 h-11 rounded-xl"
                  />
                </div>
              </div>
            </section>

            <Accordion type="single" collapsible defaultValue={hasAdvancedData ? 'advanced' : undefined}>
              <AccordionItem value="advanced" className="rounded-2xl border border-border bg-card px-4 shadow-sm">
                <AccordionTrigger className="py-4 text-sm font-black uppercase tracking-wide hover:no-underline">
                  Avancé
                </AccordionTrigger>
                <AccordionContent className="space-y-4 pb-4">
                  <div>
                    <Label htmlFor="product-barcode">Code-barres</Label>
                    <div className="mt-1.5 flex gap-2">
                      <Input
                        id="product-barcode"
                        value={form.barcode}
                        onChange={(event) => updateField('barcode', event.target.value)}
                        placeholder="Ex: 3017620422003"
                        className="h-11 rounded-xl font-mono"
                      />
                      <Button type="button" variant="secondary" size="icon" className="h-11 w-11 rounded-xl" onClick={() => setStep('barcode')}>
                        <ScanBarcode className="h-4 w-4" />
                        <span className="sr-only">Scanner le code-barres</span>
                      </Button>
                      <Button type="button" variant="outline" size="icon" className="h-11 w-11 rounded-xl" onClick={() => lookupBarcode(form.barcode)}>
                        <RefreshCw className="h-4 w-4" />
                        <span className="sr-only">Rechercher le produit</span>
                      </Button>
                    </div>
                  </div>

                  <div className="grid gap-3 md:grid-cols-3">
                    <div>
                      <Label htmlFor="product-nutri">Nutri-Score</Label>
                      <select
                        id="product-nutri"
                        value={form.nutriScore}
                        onChange={(event) => updateField('nutriScore', event.target.value)}
                        className="mt-1.5 flex h-11 w-full rounded-xl border border-input bg-background px-3 py-2 text-sm text-foreground"
                      >
                        <option value="">Aucun</option>
                        {['A', 'B', 'C', 'D', 'E'].map((grade) => <option key={grade} value={grade}>{grade}</option>)}
                      </select>
                    </div>
                    <div>
                      <Label htmlFor="product-nova">NOVA</Label>
                      <select
                        id="product-nova"
                        value={form.novaGroup}
                        onChange={(event) => updateField('novaGroup', event.target.value)}
                        className="mt-1.5 flex h-11 w-full rounded-xl border border-input bg-background px-3 py-2 text-sm text-foreground"
                      >
                        <option value="">Aucun</option>
                        {[1, 2, 3, 4].map((grade) => <option key={grade} value={grade}>{grade}</option>)}
                      </select>
                    </div>
                    <div>
                      <Label htmlFor="product-eco">Eco-Score</Label>
                      <select
                        id="product-eco"
                        value={form.ecoScore}
                        onChange={(event) => updateField('ecoScore', event.target.value)}
                        className="mt-1.5 flex h-11 w-full rounded-xl border border-input bg-background px-3 py-2 text-sm text-foreground"
                      >
                        <option value="">Aucun</option>
                        {['A', 'B', 'C', 'D', 'E'].map((grade) => <option key={grade} value={grade}>{grade}</option>)}
                      </select>
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="product-notes">Notes du foyer</Label>
                    <Textarea
                      id="product-notes"
                      value={form.notes}
                      onChange={(event) => updateField('notes', event.target.value)}
                      placeholder="Ex: À garder pour les repas du soir"
                      className="mt-1.5 min-h-20 resize-none rounded-xl"
                    />
                  </div>

                  <div>
                    <Label htmlFor="product-allergens">Allergènes</Label>
                    <Input
                      id="product-allergens"
                      value={form.allergens}
                      onChange={(event) => updateField('allergens', event.target.value)}
                      placeholder="Ex: Lait, gluten"
                      className="mt-1.5 h-11 rounded-xl"
                    />
                  </div>

                  <div>
                    <Label htmlFor="product-ingredients">Ingrédients</Label>
                    <Textarea
                      id="product-ingredients"
                      value={form.ingredients}
                      onChange={(event) => updateField('ingredients', event.target.value)}
                      placeholder="Liste d’ingrédients issue de l’emballage ou d’OpenFoodFacts"
                      className="mt-1.5 min-h-24 resize-none rounded-xl"
                    />
                  </div>
                </AccordionContent>
              </AccordionItem>
            </Accordion>
          </div>
        </div>
      </div>

      <div className="border-t border-border bg-card/95 px-5 py-4 backdrop-blur md:px-6">
        <Button type="button" className="h-12 w-full rounded-2xl text-sm font-extrabold" onClick={handleSubmit} disabled={saving || lookingUp}>
          {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <ActionIcon className="h-4 w-4" />}
          {actionLabel}
        </Button>
      </div>

      <Dialog open={offImages.length > 0} onOpenChange={(nextOpen) => { if (!nextOpen) { setOffImages([]); setSelectedImage(null); } }}>
        <DialogContent className="max-w-sm rounded-2xl">
          <DialogHeader>
            <DialogTitle>Choisir une image</DialogTitle>
            <DialogDescription>Sélectionnez une image depuis OpenFoodFacts.</DialogDescription>
          </DialogHeader>
          <div className="grid grid-cols-3 gap-2 py-2">
            {offImages.map((url) => (
              <button
                key={url}
                type="button"
                onClick={() => setSelectedImage(url)}
                className={`relative aspect-square overflow-hidden rounded-xl border-2 transition-colors ${
                  selectedImage === url ? 'border-primary' : 'border-transparent'
                }`}
              >
                <img src={url} alt="" className="h-full w-full object-cover" />
                {selectedImage === url && (
                  <div className="absolute inset-0 flex items-center justify-center bg-primary/20">
                    <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-primary-foreground">
                      <Check className="h-4 w-4" />
                    </div>
                  </div>
                )}
              </button>
            ))}
          </div>
          <div className="grid grid-cols-2 gap-2">
            <Button type="button" variant="secondary" onClick={() => { setOffImages([]); setSelectedImage(null); }}>
              Annuler
            </Button>
            <Button
              type="button"
              disabled={!selectedImage}
              onClick={() => {
                if (!selectedImage) return;
                updateField('imageUrl', selectedImage);
                setOffImages([]);
                setSelectedImage(null);
              }}
            >
              Appliquer
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );

  if (isMobile) {
    return (
      <Sheet open={open} onOpenChange={(nextOpen) => { if (!nextOpen) handleClose(); }}>
        <SheetContent side="bottom" className="max-h-[92dvh] overflow-hidden rounded-t-3xl p-0">
          <SheetHeader className="sr-only">
            <SheetTitle>{title}</SheetTitle>
            <SheetDescription>{description}</SheetDescription>
          </SheetHeader>
          {content}
        </SheetContent>
      </Sheet>
    );
  }

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => { if (!nextOpen) handleClose(); }}>
      <DialogContent className="max-w-5xl overflow-hidden rounded-2xl p-0">
        <DialogHeader className="sr-only">
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        {content}
      </DialogContent>
    </Dialog>
  );
}
