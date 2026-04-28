import { useState, useCallback, useEffect, useRef } from 'react';
import { X, ScanBarcode, ScanText, Loader2, Plus, ChevronDown, RefreshCw, Camera } from 'lucide-react';
import { BarcodeScanner } from './BarcodeScanner';
import { DateScanner } from './DateScanner';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { toast } from 'sonner';
import { matchCategory, matchSubcategory, PRODUCT_CATEGORIES } from '@/types/product';
import { supabase } from '@/integrations/supabase/client';
import { useAuth } from '@/contexts/AuthContext';

type ScanStep = 'barcode' | 'date' | 'form';

interface AddProductSheetProps {
  open: boolean;
  mode: 'single' | 'multi';
  onClose: () => void;
  onAdd: (product: {
    name: string;
    barcode?: string;
    expirationDate: string;
    imageUrl?: string;
    brand?: string;
    nutriScore?: string;
    category?: string;
    subcategory?: string;
    quantity?: string;
    novaGroup?: number;
    ecoScore?: string;
    allergens?: string;
    ingredients?: string;
    nutritionData?: string;
  }) => void;
}

export function AddProductSheet({ open, mode, onClose, onAdd }: AddProductSheetProps) {
  const { household } = useAuth();
  const [name, setName] = useState('');
  const [barcode, setBarcode] = useState('');
  const [expirationDate, setExpirationDate] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [brand, setBrand] = useState('');
  const [nutriScore, setNutriScore] = useState('');
  const [category, setCategory] = useState('');
  const [quantity, setQuantity] = useState('');
  const [novaGroup, setNovaGroup] = useState<number | undefined>();
  const [ecoScore, setEcoScore] = useState('');
  const [allergens, setAllergens] = useState('');
  const [ingredients, setIngredients] = useState('');
  const [nutritionData, setNutritionData] = useState('');
  const [step, setStep] = useState<ScanStep>('form');
  const [lookingUp, setLookingUp] = useState(false);
  const [offImages, setOffImages] = useState<string[]>([]);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [loadingImage, setLoadingImage] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (open) {
      resetFields();
      setStep(mode === 'multi' ? 'barcode' : 'form');
    }
  }, [open, mode]);

  const resetFields = () => {
    setName('');
    setBarcode('');
    setExpirationDate('');
    setImageUrl('');
    setBrand('');
    setNutriScore('');
    setCategory('');
    setQuantity('');
    setNovaGroup(undefined);
    setEcoScore('');
    setAllergens('');
    setIngredients('');
    setNutritionData('');
    setOffImages([]);
    setSelectedImage(null);
    setLoadingImage(false);
  };

  const lookupBarcode = useCallback(async (code: string) => {
    setBarcode(code);
    setLookingUp(true);
    setStep('date'); // show loader overlay immediately
    try {
      const res = await fetch(`https://world.openfoodfacts.org/api/v0/product/${code}.json`);
      const data = await res.json();
      if (data.status === 1 && data.product) {
        const p = data.product;
        setName(p.product_name || '');
        const offImage = p.image_url || p.image_front_url || p.image_small_url || '';
        let preferredImage = offImage;
        if (!offImage && household && code) {
          const { data: existing } = await supabase
            .from('products')
            .select('image_url')
            .eq('household_id', household.id)
            .eq('barcode', code)
            .not('image_url', 'is', null)
            .limit(1)
            .maybeSingle();
          if (existing?.image_url) preferredImage = existing.image_url;
        }
        setImageUrl(preferredImage);
        setBrand(p.brands || '');
        const grade = (p.nutriscore_grade || '').toUpperCase();
        setNutriScore(grade === 'UNKNOWN' ? '' : grade);
        setCategory(matchCategory(p.categories?.split(',')[0]?.trim(), p.product_name));
        setQuantity(p.quantity || '');
        if (p.nova_group) setNovaGroup(Number(p.nova_group));
        const eco = (p.ecoscore_grade || '').toUpperCase();
        setEcoScore(eco === 'UNKNOWN' || eco === 'NOT-APPLICABLE' ? '' : eco);
        setAllergens(p.allergens || '');
        setIngredients(p.ingredients_text_fr || p.ingredients_text || '');
        const n = p.nutriments || {};
        const nutrition: Record<string, number> = {};
        const nFields: [string, string][] = [
          ['energy_kcal', 'energy-kcal_100g'], ['proteins', 'proteins_100g'],
          ['carbohydrates', 'carbohydrates_100g'], ['fat', 'fat_100g'],
          ['saturated_fat', 'saturated-fat_100g'], ['sugars', 'sugars_100g'],
          ['fiber', 'fiber_100g'], ['salt', 'salt_100g'],
        ];
        nFields.forEach(([key, offKey]) => { if (n[offKey] != null) nutrition[key] = n[offKey]; });
        if (Object.keys(nutrition).length > 0) setNutritionData(JSON.stringify(nutrition));
        toast.success('Produit trouvé !');
      } else {
        toast.error('Produit non trouvé. Entrez le nom manuellement.');
      }
    } catch {
      toast.error('Erreur de recherche. Entrez le nom manuellement.');
    }
    setLookingUp(false);
  }, []);

  const fetchOFFImages = useCallback(async () => {
    if (!barcode) return;
    setLoadingImage(true);
    try {
      const res = await fetch(`https://world.openfoodfacts.org/api/v0/product/${barcode}.json`);
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
  }, [barcode]);

  const applyImage = (url: string) => {
    setImageUrl(url);
    setOffImages([]);
    setSelectedImage(null);
  };

  const uploadImage = async (file: File) => {
    if (!household) return;
    setLoadingImage(true);
    const ext = file.name.split('.').pop() ?? 'jpg';
    const path = `${household.id}/upload_${Date.now().toString(36)}.${ext}`;
    const { error } = await supabase.storage.from('product-images').upload(path, file, { upsert: true });
    if (error) { toast.error(`Upload: ${error.message}`); setLoadingImage(false); return; }
    const { data } = supabase.storage.from('product-images').getPublicUrl(path);
    applyImage(data.publicUrl);
    setLoadingImage(false);
  };

  const handleDateFound = useCallback((date: string) => {
    setExpirationDate(date);
    toast.success('Date détectée !', { duration: 1500 });
    setStep('form');
  }, []);

  const handleSubmit = () => {
    if (!name.trim() || !expirationDate) {
      toast.error('Veuillez remplir le nom et la date de péremption.');
      return;
    }
    const finalCategory = category || matchCategory(undefined, name.trim());
    const finalSubcategory = matchSubcategory(finalCategory, undefined, name.trim());
    onAdd({
      name: name.trim(),
      barcode: barcode || undefined,
      expirationDate,
      imageUrl: imageUrl || undefined,
      brand: brand || undefined,
      nutriScore: nutriScore || undefined,
      category: finalCategory || undefined,
      subcategory: finalSubcategory,
      quantity: quantity || undefined,
      novaGroup: novaGroup,
      ecoScore: ecoScore || undefined,
      allergens: allergens || undefined,
      ingredients: ingredients || undefined,
      nutritionData: nutritionData || undefined,
    });
    if (mode === 'multi') {
      resetFields();
      setStep('barcode');
      toast.success('Produit ajouté ! Scannez le suivant.');
    } else {
      resetFields();
      onClose();
      toast.success('Produit ajouté !');
    }
  };

  const handleClose = () => {
    resetFields();
    onClose();
  };

  if (!open) return null;

  if (step === 'barcode') {
    return (
      <BarcodeScanner
        onScan={(code) => lookupBarcode(code)}
        onClose={handleClose}
      />
    );
  }

  if (step === 'date' && lookingUp) {
    return (
      <div className="fixed inset-0 z-50 bg-foreground/80 flex items-center justify-center">
        <div className="bg-card rounded-2xl p-8 flex flex-col items-center gap-3">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
          <p className="font-bold text-card-foreground">Recherche du produit...</p>
          {barcode && <p className="text-xs text-muted-foreground">{barcode}</p>}
        </div>
      </div>
    );
  }

  if (step === 'date') {
    return (
      <DateScanner
        onDateFound={handleDateFound}
        onClose={() => setStep('form')}
      />
    );
  }

  return (
    <>
      <div className="fixed inset-0 z-40 bg-foreground/40" onClick={handleClose} />
      <div className="fixed bottom-0 left-0 right-0 z-40 bg-card rounded-t-3xl shadow-2xl max-h-[85vh] flex flex-col animate-in slide-in-from-bottom">
        <div className="flex items-center justify-between p-5 border-b border-border shrink-0">
          <h2 className="text-lg font-extrabold text-card-foreground">
            {mode === 'multi' ? 'Confirmer le produit' : 'Ajouter un produit'}
          </h2>
          <button onClick={handleClose} className="p-2 rounded-xl hover:bg-muted transition-colors">
            <X className="w-5 h-5 text-muted-foreground" />
          </button>
        </div>

        <div className="p-5 space-y-5 overflow-y-auto flex-1 pb-10">
          {/* Image + product info */}
          <div className="flex items-center gap-3 p-3 bg-muted rounded-xl">
            <div className="relative shrink-0">
              {imageUrl ? (
                <img src={imageUrl} alt={name} className="w-16 h-16 rounded-lg object-cover" />
              ) : (
                <div className="w-16 h-16 rounded-lg bg-background/60 border border-border flex items-center justify-center">
                  <Camera className="w-6 h-6 text-muted-foreground/40" />
                </div>
              )}
              <button
                type="button"
                onClick={() => barcode ? fetchOFFImages() : fileInputRef.current?.click()}
                disabled={loadingImage}
                className="absolute -bottom-2 -right-2 w-6 h-6 rounded-full bg-background border border-border shadow flex items-center justify-center hover:bg-muted transition-colors disabled:opacity-50"
              >
                {loadingImage
                  ? <Loader2 className="w-3 h-3 animate-spin text-muted-foreground" />
                  : <RefreshCw className="w-3 h-3 text-muted-foreground" />}
              </button>
            </div>
            <div className="min-w-0">
              <span className="text-sm font-bold text-card-foreground block truncate">
                {name || <span className="font-normal text-muted-foreground">Aucun produit scanné</span>}
              </span>
              {barcode && <p className="text-xs text-muted-foreground truncate">{barcode}</p>}
            </div>
          </div>

          <div>
            <label className="text-sm font-bold text-card-foreground mb-2 block">Nom du produit</label>
            <input
              type="text"
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="Ex: Yaourt nature"
              className="w-full px-4 py-3 rounded-xl bg-muted border border-border text-card-foreground placeholder:text-muted-foreground text-sm"
            />
          </div>

          <div>
            <label className="text-sm font-bold text-card-foreground mb-2 block">Code-barres</label>
            <div className="flex gap-2">
              <input
                type="text"
                value={barcode}
                onChange={e => setBarcode(e.target.value)}
                placeholder="Ex: 3017620422003"
                className="flex-1 px-4 py-3 rounded-xl bg-muted border border-border text-card-foreground placeholder:text-muted-foreground text-sm"
              />
              <button
                onClick={() => setStep('barcode')}
                className="px-3 py-3 bg-secondary text-secondary-foreground rounded-xl font-bold"
              >
                <ScanBarcode className="w-4 h-4" />
              </button>
            </div>
          </div>

          <div>
            <label className="text-sm font-bold text-card-foreground mb-2 block">Date de péremption</label>
            <div className="flex gap-2">
              <input
                type="date"
                value={expirationDate}
                onChange={e => setExpirationDate(e.target.value)}
                className="flex-1 px-4 py-3 rounded-xl bg-muted border border-border text-card-foreground text-sm"
              />
              <button
                onClick={() => setStep('date')}
                className="px-3 py-3 bg-secondary text-secondary-foreground rounded-xl font-bold"
              >
                <ScanText className="w-4 h-4" />
              </button>
            </div>
          </div>

          <div>
            <label className="text-sm font-bold text-card-foreground mb-2 block">Catégorie</label>
            <div className="relative">
              <select
                value={category}
                onChange={e => setCategory(e.target.value)}
                className="w-full px-4 py-3 rounded-xl bg-muted border border-border text-card-foreground text-sm appearance-none pr-10"
              >
                <option value="">Automatique</option>
                {PRODUCT_CATEGORIES.filter(c => c.key !== 'all').map(c => (
                  <option key={c.key} value={c.key}>{c.label}</option>
                ))}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground pointer-events-none" />
            </div>
          </div>

          <button
            onClick={handleSubmit}
            className="w-full py-4 bg-primary text-primary-foreground rounded-2xl font-extrabold text-base flex items-center justify-center gap-2 shadow-lg shadow-primary/20"
          >
            <Plus className="w-5 h-5" />
            {mode === 'multi' ? 'Ajouter & scanner le suivant' : 'Ajouter au frigo'}
          </button>
        </div>
      </div>

      {/* Hidden file input for direct device upload */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={e => { const f = e.target.files?.[0]; if (f) uploadImage(f); e.target.value = ''; }}
      />

      {/* Image picker dialog */}
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
            <button
              onClick={() => { setOffImages([]); setSelectedImage(null); }}
              className="flex-1 py-2.5 rounded-xl text-sm font-bold bg-muted text-muted-foreground hover:bg-muted/80 transition-colors"
            >
              Annuler
            </button>
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
    </>
  );
}
