export type ProductStatus = 'active' | 'consumed' | 'thrown' | 'opened';

export interface Product {
  id: string;
  name: string;
  barcode?: string;
  expirationDate: string; // ISO date string
  addedAt: string;
  imageUrl?: string;
  brand?: string;
  nutriScore?: string;
  category?: string;
  subcategory?: string;
  status?: ProductStatus;
  statusChangedAt?: string;
  quantity?: string;
  novaGroup?: number;
  ecoScore?: string;
  allergens?: string;
  ingredients?: string;
  openedAt?: string;
  daysAfterOpening?: number;
  addedBy?: string;
  addedByName?: string;
  notes?: string;
  frozenUntil?: string | null;
  nutritionData?: string;
}

export const FREEZE_MONTHS_BY_CATEGORY: Record<string, number> = {
  viandes: 6,
  poissons: 6,
  laitiers: 3,
  boulangerie: 3,
  fruits_legumes: 12,
  oeufs: 3,
  plats_prepares: 3,
  surgeles: 12,
  epicerie: 6,
};

export function getFreezeDuration(category?: string): number {
  return FREEZE_MONTHS_BY_CATEGORY[category ?? ''] ?? 3;
}

export function getEffectiveExpirationDate(product: Product): string {
  if (product.frozenUntil) return product.frozenUntil;
  if (product.openedAt && product.daysAfterOpening != null) {
    const openedDate = new Date(product.openedAt);
    openedDate.setDate(openedDate.getDate() + product.daysAfterOpening);
    const openedExpStr = openedDate.toISOString().split('T')[0];
    return openedExpStr < product.expirationDate ? openedExpStr : product.expirationDate;
  }
  return product.expirationDate;
}

export type ExpirationStatus = 'fresh' | 'soon' | 'expired';

export function getExpirationStatus(expirationDate: string): ExpirationStatus {
  const now = new Date();
  const exp = new Date(expirationDate);
  const diffDays = Math.ceil((exp.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
  if (diffDays < 0) return 'expired';
  if (diffDays <= 3) return 'soon';
  return 'fresh';
}

export const PRODUCT_CATEGORIES = [
  { key: 'all', label: 'Tous', icon: 'LayoutGrid', keywords: [], subcategories: [] },
  { key: 'fruits_legumes', label: 'Fruits & Légumes', icon: 'Apple', keywords: ['fruit', 'légume', 'legume', 'salade', 'tomate', 'pomme', 'banane', 'carotte', 'courgette', 'poivron', 'oignon', 'ail', 'avocat', 'citron', 'orange', 'fraise', 'raisin', 'melon', 'pastèque', 'poire', 'cerise', 'abricot', 'pêche', 'mangue', 'ananas', 'kiwi', 'clémentine', 'mandarine', 'épinard', 'brocoli', 'chou', 'haricot vert', 'petit pois', 'concombre', 'aubergine', 'radis', 'navet', 'betterave', 'artichaut', 'asperge', 'endive', 'fenouil', 'poireau', 'céleri', 'champignon', 'vegetable', 'légumes', 'fruits'], subcategories: [
    { key: 'agrumes', label: 'Agrumes', keywords: ['citron', 'orange', 'clémentine', 'mandarine', 'pamplemousse', 'lime'] },
    { key: 'herbes', label: 'Herbes', keywords: ['basilic', 'persil', 'ciboulette', 'menthe', 'thym', 'romarin', 'coriandre', 'aneth', 'estragon', 'sauge', 'origan'] },
    { key: 'fruits', label: 'Fruits', keywords: ['fruit', 'pomme', 'banane', 'fraise', 'raisin', 'melon', 'pastèque', 'poire', 'cerise', 'abricot', 'pêche', 'mangue', 'ananas', 'kiwi', 'framboise', 'myrtille', 'figue', 'prune'] },
    { key: 'legumes', label: 'Légumes', keywords: ['légume', 'legume', 'tomate', 'carotte', 'courgette', 'poivron', 'oignon', 'ail', 'avocat', 'épinard', 'brocoli', 'chou', 'haricot', 'petit pois', 'concombre', 'aubergine', 'radis', 'navet', 'betterave', 'artichaut', 'asperge', 'endive', 'fenouil', 'poireau', 'céleri', 'champignon', 'salade', 'vegetable'] },
  ] },
  { key: 'laitiers', label: 'Laitiers', icon: 'Milk', keywords: ['lait', 'yaourt', 'yogourt', 'fromage', 'crème', 'beurre', 'dairy', 'cheese', 'cream', 'milk', 'mozzarella', 'emmental', 'gruyère', 'camembert', 'comté', 'roquefort', 'chèvre', 'ricotta', 'mascarpone', 'feta', 'parmesan', 'raclette', 'reblochon', 'skyr', 'kéfir', 'petit-suisse', 'faisselle', 'laitier', 'laitiers', 'crème fraîche'], subcategories: [
    { key: 'fromages', label: 'Fromages', keywords: ['fromage', 'cheese', 'mozzarella', 'emmental', 'gruyère', 'camembert', 'comté', 'roquefort', 'chèvre', 'ricotta', 'mascarpone', 'feta', 'parmesan', 'raclette', 'reblochon'] },
    { key: 'yaourts', label: 'Yaourts', keywords: ['yaourt', 'yogourt', 'yogurt', 'skyr', 'kéfir', 'petit-suisse', 'faisselle'] },
    { key: 'beurres', label: 'Beurres', keywords: ['beurre', 'butter'] },
    { key: 'cremes', label: 'Crèmes', keywords: ['crème', 'cream', 'crème fraîche'] },
    { key: 'laits', label: 'Laits', keywords: ['lait', 'milk'] },
  ] },
  { key: 'viandes', label: 'Viandes', icon: 'Beef', keywords: ['viande', 'poulet', 'porc', 'bœuf', 'boeuf', 'veau', 'agneau', 'dinde', 'canard', 'lapin', 'steak', 'saucisse', 'jambon', 'lard', 'bacon', 'merguez', 'chipolata', 'côtelette', 'rôti', 'escalope', 'meat', 'chicken', 'beef', 'pork', 'charcuterie', 'pâté', 'rillettes', 'terrine', 'boudin', 'andouillette'], subcategories: [
    { key: 'volaille', label: 'Volaille', keywords: ['poulet', 'dinde', 'canard', 'pintade', 'chicken', 'volaille'] },
    { key: 'boeuf', label: 'Bœuf', keywords: ['bœuf', 'boeuf', 'beef', 'veau', 'steak', 'rôti'] },
    { key: 'porc', label: 'Porc', keywords: ['porc', 'pork', 'jambon', 'lard', 'bacon', 'côtelette'] },
    { key: 'charcuterie', label: 'Charcuterie', keywords: ['charcuterie', 'saucisse', 'merguez', 'chipolata', 'pâté', 'rillettes', 'terrine', 'boudin', 'andouillette', 'saucisson', 'chorizo'] },
  ] },
  { key: 'poissons', label: 'Poissons', icon: 'Fish', keywords: ['poisson', 'saumon', 'thon', 'cabillaud', 'crevette', 'moule', 'huître', 'sardine', 'maquereau', 'truite', 'sole', 'bar', 'dorade', 'merlu', 'colin', 'lieu', 'fish', 'seafood', 'crustacé', 'fruit de mer', 'calamar', 'poulpe', 'homard', 'langouste', 'crabe', 'surimi'], subcategories: [
    { key: 'crustaces', label: 'Crustacés', keywords: ['crevette', 'crustacé', 'homard', 'langouste', 'crabe', 'langoustine', 'gambas'] },
    { key: 'fumes', label: 'Fumés', keywords: ['fumé', 'smoked', 'saumon fumé', 'truite fumée', 'haddock'] },
    { key: 'poissons', label: 'Poissons', keywords: ['poisson', 'saumon', 'thon', 'cabillaud', 'sardine', 'maquereau', 'truite', 'sole', 'bar', 'dorade', 'merlu', 'colin', 'lieu', 'fish'] },
  ] },
  { key: 'boulangerie', label: 'Boulangerie', icon: 'Croissant', keywords: ['pain', 'baguette', 'croissant', 'brioche', 'viennoiserie', 'pain de mie', 'toast', 'muffin', 'bagel', 'focaccia', 'ciabatta', 'pain complet', 'pain aux céréales', 'fougasse', 'naan', 'pita', 'wrap', 'tortilla', 'crêpe', 'gaufre', 'bakery', 'bread'], subcategories: [
    { key: 'viennoiseries', label: 'Viennoiseries', keywords: ['croissant', 'brioche', 'viennoiserie', 'pain au chocolat', 'chausson', 'muffin'] },
    { key: 'pains', label: 'Pains', keywords: ['pain', 'baguette', 'bagel', 'focaccia', 'ciabatta', 'fougasse', 'naan', 'pita', 'wrap', 'tortilla', 'bread'] },
  ] },
  { key: 'oeufs', label: 'Œufs', icon: 'Egg', keywords: ['oeuf', 'œuf', 'oeufs', 'œufs', 'egg', 'eggs'], subcategories: [] },
  { key: 'condiments', label: 'Condiments & Sauces', icon: 'Droplets', keywords: ['sauce', 'ketchup', 'mayonnaise', 'moutarde', 'vinaigre', 'huile', 'vinaigrette', 'soja', 'tabasco', 'sriracha', 'pesto', 'harissa', 'curry', 'épice', 'épices', 'poivre', 'paprika', 'cumin', 'curcuma', 'cannelle', 'herbes', 'basilic', 'thym', 'romarin', 'origan', 'condiment', 'assaisonnement'], subcategories: [
    { key: 'huiles', label: 'Huiles', keywords: ['huile', 'oil'] },
    { key: 'vinaigres', label: 'Vinaigres', keywords: ['vinaigre', 'vinegar'] },
    { key: 'epices', label: 'Épices', keywords: ['épice', 'épices', 'poivre', 'paprika', 'cumin', 'curcuma', 'cannelle', 'sel', 'spice'] },
    { key: 'sauces', label: 'Sauces', keywords: ['sauce', 'ketchup', 'mayonnaise', 'moutarde', 'soja', 'tabasco', 'sriracha', 'pesto', 'harissa', 'vinaigrette'] },
  ] },
  { key: 'boissons', label: 'Boissons', icon: 'CupSoda', keywords: ['boisson', 'jus', 'eau', 'soda', 'coca', 'limonade', 'thé', 'café', 'bière', 'vin', 'alcool', 'sirop', 'smoothie', 'nectar', 'drink', 'beverage', 'lait de', 'energy', 'ice tea', 'infusion', 'tisane', 'chocolat chaud'], subcategories: [
    { key: 'jus', label: 'Jus', keywords: ['jus', 'juice', 'nectar', 'smoothie'] },
    { key: 'sodas', label: 'Sodas', keywords: ['soda', 'coca', 'limonade', 'pepsi', 'sprite', 'fanta', 'energy', 'ice tea'] },
    { key: 'cafes', label: 'Cafés', keywords: ['café', 'coffee', 'thé', 'tea', 'infusion', 'tisane', 'chocolat chaud'] },
    { key: 'alcools', label: 'Alcools', keywords: ['bière', 'vin', 'alcool', 'beer', 'wine', 'whisky', 'vodka', 'rhum', 'gin', 'champagne'] },
  ] },
  { key: 'surgeles', label: 'Surgelés', icon: 'Snowflake', keywords: ['surgelé', 'surgelés', 'glace', 'frozen', 'glacé', 'sorbet', 'congelé', 'congélation', 'pizza surgelée', 'frites', 'légumes surgelés'], subcategories: [
    { key: 'glaces', label: 'Glaces', keywords: ['glace', 'glacé', 'sorbet', 'ice cream'] },
    { key: 'legumes', label: 'Légumes', keywords: ['légumes surgelés', 'frites', 'épinards surgelés'] },
    { key: 'plats', label: 'Plats', keywords: ['pizza surgelée', 'plat surgelé', 'lasagne surgelée'] },
  ] },
  { key: 'plats_prepares', label: 'Plats préparés', icon: 'UtensilsCrossed', keywords: ['plat préparé', 'plat cuisiné', 'barquette', 'micro-ondes', 'ready meal', 'meal prep', 'salade composée', 'sandwich', 'quiche', 'tarte salée', 'pizza', 'lasagne', 'hachis', 'gratin', 'taboulé', 'couscous'], subcategories: [
    { key: 'sandwichs', label: 'Sandwichs', keywords: ['sandwich', 'wrap', 'panini', 'burger'] },
    { key: 'pizzas', label: 'Pizzas', keywords: ['pizza'] },
    { key: 'salades', label: 'Salades', keywords: ['salade composée', 'taboulé'] },
  ] },
  { key: 'conserves', label: 'Conserves', icon: 'Archive', keywords: ['conserve', 'boîte', 'bocal', 'en conserve', 'canned', 'légumes en conserve', 'compote', 'confiture', 'cornichon', 'olive', 'tomate pelée', 'concentré', 'cassoulet'], subcategories: [
    { key: 'confitures', label: 'Confitures', keywords: ['confiture', 'jam', 'marmelade', 'compote'] },
    { key: 'legumes', label: 'Légumes', keywords: ['légumes en conserve', 'cornichon', 'olive', 'tomate pelée', 'concentré', 'maïs'] },
    { key: 'plats', label: 'Plats', keywords: ['cassoulet', 'ravioli en conserve', 'plat en conserve'] },
  ] },
  { key: 'epicerie', label: 'Épicerie', icon: 'Wheat', keywords: ['pâtes', 'pâte', 'riz', 'céréales', 'farine', 'sucre', 'sel', 'biscuit', 'gâteau', 'chocolat', 'miel', 'nouilles', 'semoule', 'quinoa', 'lentilles', 'pois chiches', 'haricots secs', 'cookie', 'crackers', 'chips', 'snack', 'bonbon', 'céréale', 'pasta', 'rice', 'cereal', 'granola', 'muesli'], subcategories: [
    { key: 'pates', label: 'Pâtes', keywords: ['pâtes', 'pasta', 'nouilles', 'spaghetti', 'penne', 'tagliatelle', 'ravioli'] },
    { key: 'riz', label: 'Riz', keywords: ['riz', 'rice', 'basmati', 'risotto'] },
    { key: 'cereales', label: 'Céréales', keywords: ['céréales', 'céréale', 'cereal', 'granola', 'muesli', 'cornflakes', 'flocons d\'avoine'] },
    { key: 'biscuits', label: 'Biscuits', keywords: ['biscuit', 'cookie', 'crackers', 'gâteau', 'bonbon', 'chocolat', 'snack', 'chips'] },
    { key: 'legumineuses', label: 'Légumineuses', keywords: ['lentilles', 'pois chiches', 'haricots secs', 'quinoa', 'semoule'] },
  ] },
  { key: 'bebe', label: 'Bébé', icon: 'Baby', keywords: ['bébé', 'baby', 'infantile', 'petit pot', 'lait infantile', 'compote bébé', 'céréales bébé', 'purée bébé'], subcategories: [] },
  { key: 'hygiene', label: 'Hygiène & Beauté', icon: 'Sparkles', keywords: ['shampooing', 'gel douche', 'savon', 'dentifrice', 'déodorant', 'crème solaire', 'lotion', 'maquillage', 'hygiène', 'beauté', 'cosmétique', 'soin', 'rasoir', 'coton', 'lingette'], subcategories: [
    { key: 'bouche', label: 'Bouche', keywords: ['dentifrice', 'bain de bouche', 'brosse à dents'] },
    { key: 'visage', label: 'Visage', keywords: ['crème visage', 'maquillage', 'démaquillant', 'lotion visage', 'masque visage'] },
    { key: 'corps', label: 'Corps', keywords: ['shampooing', 'gel douche', 'savon', 'déodorant', 'crème solaire', 'lotion', 'rasoir', 'coton', 'lingette'] },
  ] },
  { key: 'autre', label: 'Autre', icon: 'Package', keywords: [], subcategories: [] },
] as const;

export type ProductCategoryKey = typeof PRODUCT_CATEGORIES[number]['key'];

export function matchCategory(openFoodFactsCategory?: string, productName?: string): string {
  const text = `${openFoodFactsCategory || ''} ${productName || ''}`.toLowerCase();
  if (!text.trim()) return 'autre';
  
  for (const cat of PRODUCT_CATEGORIES) {
    if (cat.key === 'all' || cat.key === 'autre') continue;
    if (cat.keywords.some(kw => text.includes(kw))) {
      return cat.key;
    }
  }
  return 'autre';
}

export function matchSubcategory(categoryKey?: string, openFoodFactsCategory?: string, productName?: string): string | undefined {
  if (!categoryKey) return undefined;
  const cat = PRODUCT_CATEGORIES.find(c => c.key === categoryKey);
  if (!cat || !cat.subcategories || cat.subcategories.length === 0) return undefined;
  const text = `${openFoodFactsCategory || ''} ${productName || ''}`.toLowerCase();
  if (!text.trim()) return undefined;
  for (const sub of cat.subcategories) {
    if (sub.keywords.some(kw => text.includes(kw))) {
      return sub.label;
    }
  }
  return undefined;
}

export const RECOMMENDED_DAYS_AFTER_OPENING: Record<string, { days: number; label: string }> = {
  fruits_legumes: { days: 3, label: 'Fruits & Légumes : 2-4 jours' },
  laitiers: { days: 3, label: 'Laitiers : 2-4 jours' },
  viandes: { days: 2, label: 'Viandes : 1-2 jours' },
  poissons: { days: 1, label: 'Poissons : 1 jour' },
  boulangerie: { days: 3, label: 'Boulangerie : 2-4 jours' },
  oeufs: { days: 2, label: 'Œufs : 1-2 jours (si cuits)' },
  condiments: { days: 30, label: 'Condiments : 1-3 mois' },
  boissons: { days: 5, label: 'Boissons : 3-7 jours' },
  surgeles: { days: 2, label: 'Surgelés (décongelés) : 1-2 jours' },
  plats_prepares: { days: 2, label: 'Plats préparés : 1-2 jours' },
  conserves: { days: 5, label: 'Conserves : 3-5 jours' },
  epicerie: { days: 14, label: 'Épicerie : 7-30 jours' },
  bebe: { days: 1, label: 'Bébé : 24 heures' },
  hygiene: { days: 180, label: 'Hygiène : 6-12 mois' },
  autre: { days: 3, label: 'Général : 3 jours' },
};

// Recommandations par sous-catégorie (clé = "categoryKey:SubcategoryLabel"). Prioritaire sur la catégorie.
export const SUBCATEGORY_DAYS_AFTER_OPENING: Record<string, { days: number; label: string }> = {
  // Fruits & Légumes
  "fruits_legumes:Fruits": { days: 4, label: 'Fruits : 3-5 jours' },
  "fruits_legumes:Légumes": { days: 4, label: 'Légumes : 3-5 jours' },
  "fruits_legumes:Agrumes": { days: 7, label: 'Agrumes : 1 semaine' },
  "fruits_legumes:Herbes": { days: 2, label: 'Herbes : 1-3 jours' },
  // Laitiers
  "laitiers:Fromages": { days: 7, label: 'Fromages : 5-10 jours' },
  "laitiers:Yaourts": { days: 2, label: 'Yaourts : 1-2 jours' },
  "laitiers:Beurres": { days: 21, label: 'Beurres : 2-3 semaines' },
  "laitiers:Crèmes": { days: 3, label: 'Crèmes : 2-4 jours' },
  "laitiers:Laits": { days: 4, label: 'Laits : 3-5 jours' },
  // Viandes
  "viandes:Volaille": { days: 2, label: 'Volaille : 1-2 jours' },
  "viandes:Bœuf": { days: 3, label: 'Bœuf : 2-3 jours' },
  "viandes:Porc": { days: 2, label: 'Porc : 1-2 jours' },
  "viandes:Charcuterie": { days: 5, label: 'Charcuterie : 3-5 jours' },
  // Poissons
  "poissons:Poissons": { days: 1, label: 'Poissons : 1 jour' },
  "poissons:Crustacés": { days: 1, label: 'Crustacés : 1 jour' },
  "poissons:Fumés": { days: 3, label: 'Fumés : 2-3 jours' },
  // Boulangerie
  "boulangerie:Pains": { days: 3, label: 'Pains : 2-3 jours' },
  "boulangerie:Viennoiseries": { days: 2, label: 'Viennoiseries : 1-2 jours' },
  // Boissons
  "boissons:Jus": { days: 4, label: 'Jus : 3-5 jours' },
  "boissons:Sodas": { days: 3, label: 'Sodas : 2-3 jours' },
  "boissons:Cafés": { days: 2, label: 'Cafés/Thés : 1-2 jours' },
  "boissons:Alcools": { days: 30, label: 'Alcools : plusieurs semaines' },
  // Épicerie
  "epicerie:Pâtes": { days: 5, label: 'Pâtes (cuites) : 3-5 jours' },
  "epicerie:Riz": { days: 4, label: 'Riz (cuit) : 3-4 jours' },
  "epicerie:Biscuits": { days: 14, label: 'Biscuits : 1-2 semaines' },
  "epicerie:Céréales": { days: 30, label: 'Céréales : 1 mois' },
  "epicerie:Légumineuses": { days: 4, label: 'Légumineuses (cuites) : 3-5 jours' },
  // Condiments
  "condiments:Sauces": { days: 30, label: 'Sauces : 1 mois' },
  "condiments:Huiles": { days: 180, label: 'Huiles : 6 mois' },
  "condiments:Vinaigres": { days: 365, label: 'Vinaigres : 1 an' },
  "condiments:Épices": { days: 365, label: 'Épices : 1 an' },
  // Surgelés
  "surgeles:Glaces": { days: 30, label: 'Glaces : 1 mois (au congélateur)' },
  "surgeles:Légumes": { days: 2, label: 'Légumes (décongelés) : 1-2 jours' },
  "surgeles:Plats": { days: 2, label: 'Plats (décongelés) : 1-2 jours' },
  // Plats préparés
  "plats_prepares:Sandwichs": { days: 1, label: 'Sandwichs : 24h' },
  "plats_prepares:Pizzas": { days: 2, label: 'Pizzas : 1-2 jours' },
  "plats_prepares:Salades": { days: 1, label: 'Salades : 24h' },
  // Conserves
  "conserves:Légumes": { days: 4, label: 'Légumes : 3-5 jours' },
  "conserves:Confitures": { days: 30, label: 'Confitures : 1 mois' },
  "conserves:Plats": { days: 3, label: 'Plats : 2-3 jours' },
  // Hygiène
  "hygiene:Corps": { days: 180, label: 'Corps : 6-12 mois' },
  "hygiene:Visage": { days: 180, label: 'Visage : 6 mois' },
  "hygiene:Bouche": { days: 90, label: 'Bouche : 3 mois' },
};

export function getRecommendedDaysAfterOpening(categoryKey?: string, subcategoryLabel?: string): { days: number; label: string } {
  if (categoryKey && subcategoryLabel) {
    const key = `${categoryKey}:${subcategoryLabel}`;
    if (key in SUBCATEGORY_DAYS_AFTER_OPENING) {
      return SUBCATEGORY_DAYS_AFTER_OPENING[key];
    }
  }
  if (categoryKey && categoryKey in RECOMMENDED_DAYS_AFTER_OPENING) {
    return RECOMMENDED_DAYS_AFTER_OPENING[categoryKey];
  }
  return RECOMMENDED_DAYS_AFTER_OPENING['autre'];
}

export const CATEGORY_POST_EXPIRY_NOTES: Record<string, string | null> = {
  fruits_legumes: "quelques jours",
  laitiers: "1 semaine",
  viandes: null,
  poissons: null,
  boulangerie: "1 à 2 jours",
  oeufs: "4 semaines",
  condiments: "2 à 6 mois",
  boissons: "2 à 3 mois",
  surgeles: "2 à 3 mois",
  plats_prepares: null,
  conserves: "1 à 2 ans",
  epicerie: "1 à 3 mois",
  bebe: null,
  hygiene: "6 à 12 mois",
  autre: null,
};

// Notes par sous-catégorie (clé = "categoryKey:SubcategoryLabel"). Prioritaire sur la note de catégorie.
export const SUBCATEGORY_POST_EXPIRY_NOTES: Record<string, string | null> = {
  // Fruits & Légumes
  "fruits_legumes:Fruits": "3 à 5 jours",
  "fruits_legumes:Légumes": "3 à 5 jours",
  "fruits_legumes:Agrumes": "1 à 2 semaines",
  "fruits_legumes:Herbes": "2 à 3 jours",
  // Laitiers
  "laitiers:Fromages": "1 à 2 semaines",
  "laitiers:Yaourts": "2 à 3 semaines",
  "laitiers:Beurres": "1 mois",
  "laitiers:Crèmes": "3 à 5 jours",
  "laitiers:Laits": "4 à 7 jours",
  // Viandes
  "viandes:Volaille": null,
  "viandes:Bœuf": "1 à 2 jours",
  "viandes:Porc": null,
  "viandes:Charcuterie": "3 à 5 jours",
  // Poissons
  "poissons:Poissons": null,
  "poissons:Crustacés": null,
  "poissons:Fumés": "2 à 3 jours",
  // Boulangerie
  "boulangerie:Pains": "2 à 3 jours",
  "boulangerie:Viennoiseries": "1 jour",
  // Boissons
  "boissons:Jus": "1 à 2 semaines",
  "boissons:Sodas": "3 à 6 mois",
  "boissons:Cafés": "6 à 12 mois",
  "boissons:Alcools": "plusieurs années",
  // Épicerie
  "epicerie:Pâtes": "1 à 2 ans",
  "epicerie:Riz": "1 à 2 ans",
  "epicerie:Biscuits": "1 à 3 mois",
  "epicerie:Céréales": "3 à 6 mois",
  "epicerie:Légumineuses": "1 à 2 ans",
  // Condiments
  "condiments:Sauces": "1 à 3 mois",
  "condiments:Huiles": "6 à 12 mois",
  "condiments:Vinaigres": "plusieurs années",
  "condiments:Épices": "1 à 2 ans",
  // Surgelés
  "surgeles:Glaces": "2 à 3 mois",
  "surgeles:Légumes": "6 à 12 mois",
  "surgeles:Plats": "2 à 3 mois",
  // Plats préparés
  "plats_prepares:Sandwichs": null,
  "plats_prepares:Pizzas": "1 à 2 jours",
  "plats_prepares:Salades": null,
  // Conserves
  "conserves:Légumes": "1 à 2 ans",
  "conserves:Confitures": "1 à 2 ans",
  "conserves:Plats": "1 an",
  // Hygiène
  "hygiene:Corps": "6 à 12 mois",
  "hygiene:Visage": "3 à 6 mois",
  "hygiene:Bouche": "3 à 6 mois",
};

export function getPostExpiryNote(categoryKey?: string, subcategoryLabel?: string): string | null {
  if (categoryKey && subcategoryLabel) {
    const key = `${categoryKey}:${subcategoryLabel}`;
    if (key in SUBCATEGORY_POST_EXPIRY_NOTES) {
      return SUBCATEGORY_POST_EXPIRY_NOTES[key];
    }
  }
  if (categoryKey && categoryKey in CATEGORY_POST_EXPIRY_NOTES) {
    return CATEGORY_POST_EXPIRY_NOTES[categoryKey];
  }
  return null;
}

export function getDaysUntilExpiration(expirationDate: string): number {
  const now = new Date();
  const exp = new Date(expirationDate);
  return Math.ceil((exp.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
}
