# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

Exprime-toi toujours en français

## Démarrage

Lorsque je demande de démarrer ou redémarrer le serveur, Kill d'abord les précédentes instances

## Commands

```bash
bun run dev -- --port 3000 --host   # dev server (https://localhost:3000, SSL via mkcert)
bun run build                        # production build
bun run lint                         # ESLint
bun run test                         # vitest (run once)
bun run test:watch                   # vitest (watch mode)
```

- `npm` is not available — use `bun` only.
- SSL (mkcert) is required for the Service Worker; the dev server must run on HTTPS.
- Kill the existing server (PowerShell): `netstat -ano | grep ":3000" | awk '{print $5}' | sort -u | while read pid; do taskkill //PID "$pid" //F; done`
- Network access: https://192.168.1.50:3000

## Architecture

### Auth & data flow

`AuthProvider` (`src/contexts/AuthContext.tsx`) is the root of all authenticated state. It listens to `supabase.auth.onAuthStateChange` and resolves the user's `household` + `members` array. Important: `TOKEN_REFRESHED` events fire a new `members` array reference — hooks that depend on `members` must use a `useRef` (see `membersRef` in `useProducts`) to avoid spurious effect re-runs that would tear down and recreate Supabase Realtime channels.

`AppRoutes` in `src/App.tsx` gates rendering: unauthenticated → `<Auth>`, no household → `<HouseholdSetup>`, otherwise the full route tree wrapped in `<AnimatePresence mode="wait">` for page transitions. `<Layout>` renders `<DesktopSidebar>` on non-mobile; mobile uses a bottom nav.

### Product data

`useProducts` (`src/hooks/useProducts.ts`) is the single source of truth for the product list. It:
- Fetches all products for the household once on mount.
- Subscribes to a Supabase Realtime `postgres_changes` channel. The channel name **must** include a per-instance suffix (`instanceId = useRef(Math.random()...)`) to avoid "cannot add postgres_changes callbacks after subscribe()" when two instances share the same channel name.
- Returns `sortedProducts` (sorted by expiration date ascending) via `useMemo`.
- In guest mode (`isGuest` flag from `AuthContext`), delegates all CRUD to `src/lib/guestProducts.ts` (localStorage-backed).

All DB ↔ domain mapping lives in `src/lib/productDb.ts`: `dbToProduct`, `productToDbInsert`, `productToDbUpdate`. Never bypass these when reading/writing product rows.

### Guest mode

`src/lib/guestProducts.ts` provides a full localStorage-backed CRUD layer for unauthenticated users:
- Key functions: `readGuestProducts`, `addGuestProduct`, `updateGuestProduct`, `removeGuestProduct`, `setGuestProductStatus`, `clearGuestProducts`, `hasGuestProducts`.
- `localStorage` key: `freshtrack-guest-products`.
- `AuthContext` exposes `isGuest: boolean`. When `isGuest` is true, `useProducts` routes all operations through `guestProducts.ts` instead of Supabase.
- `GuestImportDialog` (`src/components/GuestImportDialog.tsx`) is rendered inside `AppRoutes` and prompts the user to import guest products after sign-in.

### Product model & status lifecycle

`src/types/product.ts` is central:
- `ProductStatus`: `'active' | 'opened' | 'consumed' | 'thrown'`
- `getEffectiveExpirationDate(product)`: for opened products, returns `openedAt + daysAfterOpening` if that date is earlier than `expirationDate`. Always use this (not `product.expirationDate` directly) when computing freshness.
- `getRecommendedDaysAfterOpening(category, subcategory)`: returns the shelf-life preset for a given category to pre-fill the opening dialog.
- `getPostExpiryNote(category)`: returns a consumability tip shown after expiry on the detail page.
- `PRODUCT_CATEGORIES`: array of `{ key, label, icon, keywords, subcategories }`. Icon values are Lucide icon **name strings** (not components); callers maintain their own `Record<string, ComponentType>` lookup map.
- `matchCategory` / `matchSubcategory`: keyword-based auto-classification used after OpenFoodFacts lookups.

When reverting a product from consumed/thrown back to `active`, `setProductStatus` in `useProducts` must also clear `opened_at` and `days_after_opening` to `null`, otherwise the effective expiration date stays wrong.

### Supabase integration

- Client: `src/integrations/supabase/client.ts` (env vars `VITE_SUPABASE_URL`, `VITE_SUPABASE_PUBLISHABLE_KEY`).
- Types: `src/integrations/supabase/types.ts` — auto-generated, do not edit. Tables: `households`, `household_members`, `products`.
- Storage bucket `product-images`: two path conventions — product photos at `{household_id}/{product_id}_{timestamp}.{ext}`, member avatars at `{household_id}/avatar_{user_id}.{ext}` (upsert: true).
- RLS is enabled; all queries are automatically scoped to the authenticated user's household. See `supabase/migrations/` for the full policy history.
- `join_household_by_code()` is a `SECURITY DEFINER` Postgres function that bypasses RLS to allow invite-code joins.

### Pages overview

| Page | Path | Purpose |
|------|------|---------|
| `Index.tsx` | `/` | Main fridge view — product list grouped by status (expired/soon/fresh), filters, search, sort, multi-select with long-press (500ms, 8px tolerance), skeleton loading, contextual header color |
| `ProductDetail.tsx` | `/product/:id` | Full product view with status controls, opening dialog, image picker, nutritional badges (Nutri-Score/NOVA/Eco-Score), post-expiry tip, ingredients |
| `Stats.tsx` | `/stats` | Anti-gaspi score, utilization rate, monthly charts (Recharts), top thrown products, per-category rates |
| `History.tsx` | `/history` | Products with status consumed/thrown/opened |
| `Notifications.tsx` | `/notifications` | Active expiry alerts + notification preference settings (incl. guest mode toggle) |
| `Settings.tsx` | `/settings` | **"Mon profil"** section (avatar upload + display name inline-edit) + household name (owner only) + invite code + members list + appearance settings (theme/accent/density) |
| `HouseholdSetup.tsx` | — | Create or join household (shown when user has no household) |
| `Auth.tsx` | — | Email/password sign-in and sign-up |
| `Credits.tsx` | `/credits` | Attribution |

Note: `/household` redirects to `/settings` via `<Navigate to="/settings" replace />`.

### PWA & notifications

`src/lib/swNotify.ts` exports `showNotification(title, options)` which routes notifications through the active Service Worker registration (required for iOS PWA). `NotificationChecker` (`src/components/NotificationChecker.tsx`) is a side-effect-only component rendered inside `AppRoutes`; it checks expiring products once per day using `localStorage` to track which IDs have already been notified.

### Appearance

All appearance settings are managed by `AppearanceContext` (`src/contexts/AppearanceContext.tsx`). Never write local appearance logic — always use `useAppearance()`.

- `themeMode`: `'light' | 'dark' | 'system'` — persisted to `localStorage` key `frigo-theme-mode`. Applies `dark` class on `document.documentElement`. Legacy key `frigo-dark-mode` is still read on first load for backwards compatibility.
- `accentColor`: `'green' | 'blue' | 'violet' | 'orange' | 'rose' | 'cyan'` — persisted to `frigo-accent-color`. Updates `--primary` and `--ring` CSS variables.
- `density`: `'compact' | 'normal' | 'spacious'` — persisted to `frigo-density`.
- `reduceMotion`: `boolean` — persisted to `frigo-reduce-motion`. Adds `reduce-motion` class on `document.documentElement`.

### UI conventions

- All UI primitives come from `src/components/ui/` (shadcn/ui — do not hand-roll equivalents).
- Path alias `@/` → `src/`.
- Page transitions: every page wraps its content in `<PageTransition>` (`src/components/PageTransition.tsx`). A module-level `lastAnimatedPath` variable prevents the entrance animation from replaying when Chrome fires `onAuthStateChange` on tab focus.
- Layout: `<Layout>` in `App.tsx` renders `<DesktopSidebar>` on non-mobile; mobile gets a bottom nav via `NavLink` components. Use `useIsMobile()` to gate desktop-only UI.
- Toasts: use `sonner` (`import { toast } from 'sonner'`), not the legacy shadcn toaster.
- Dark mode: managed by `AppearanceContext` — never toggle `document.documentElement.classList` manually.

### Barcode / date scanning

`AddProductSheet` (`src/components/AddProductSheet.tsx`) is a thin wrapper that delegates the full create/edit flow to `ProductEditorSheet` (`src/components/ProductEditorSheet.tsx`). `ProductEditorSheet` orchestrates: barcode scan → date scan → form. After a barcode scan it calls the OpenFoodFacts API directly (no backend proxy) and falls back gracefully if not found. `BarcodeScanner` and `DateScanner` are wrappers around `html5-qrcode`.

Date OCR uses a two-layer approach:
1. **Local** (`src/lib/dateOcr.ts`): `parseExpirationDate(text)` parses raw OCR text, supports all common formats (DD/MM/YY, MM/YYYY, word months, etc.). `preprocessDateImage(canvas)` applies contrast/scaling before passing to Tesseract.js.
2. **Edge Function fallback**: `supabase/functions/ocr-date/` calls Gemini 2.5 Flash when local OCR yields no result.

### Testing

Tests live in `src/**/*.{test,spec}.{ts,tsx}` and run in jsdom via vitest. There is currently minimal test coverage; `src/test/setup.ts` imports `@testing-library/jest-dom` matchers.

---

## Android — État d'implémentation (2026-05-05)

L'application Android native (`android/`) vise la parité complète avec la PWA. Architecture : Kotlin + Jetpack Compose, Hilt, Room, Supabase, CameraX + ML Kit, WorkManager, Vico (charts).

### Phases terminées

**Phase 1 — Stabilisation Room**
- `FreshTrackDatabase.kt` : `exportSchema = true` (génère les JSON de schéma versionnés)
- `DatabaseModule.kt` : suppression de `.fallbackToDestructiveMigration()` — évite la perte de données en production lors des mises à jour d'APK
- `build.gradle.kts` : argument KSP `room.schemaLocation` → `$projectDir/schemas`

**Phase 2 — Mode multi-scan**
- `AppNavigation.kt` : 3 nouvelles routes (`BARCODE_SCANNER_MULTI`, `DATE_SCANNER_MULTI`, `ADD_PRODUCT_MULTI`) + helpers `dateScannerMulti()`, `addProductMulti()`. Les dates dans les URLs utilisent `_` à la place de `/` pour éviter les conflits de routing.
- `IndexScreen.kt` : paramètre `onMultiScanClick`, état `fabExpanded`, composables `FabBubbleMenu` + `BubbleOption` avec spring animation (2 bulles : "Un produit" / "Plusieurs produits").
- `BarcodeScannerScreen.kt` : paramètre `isMultiScan` — en mode multi, navigue vers `DATE_SCANNER_MULTI` sans popper le scanner barcode du back-stack.
- `DateScannerScreen.kt` : paramètre `isMultiScan` — en mode multi, navigue vers `ADD_PRODUCT_MULTI` avec `popUpTo(DATE_SCANNER_MULTI)` au lieu de passer par `savedStateHandle`.
- `AddProductScreen.kt` : paramètres `isMultiMode` + `initialDate`. Affiche "Ajouter & scanner le suivant" (reset vers `BARCODE_SCANNER_MULTI` fresh) + "Terminer" (`popBackStack` jusqu'à `BARCODE_SCANNER_MULTI` inclusive) en mode multi.

### Phases restantes

| Phase | Description | Fichiers principaux |
|-------|-------------|---------------------|
| 3 | Stats visuelles : jauge semicircle (Canvas Compose), barre de vie produits urgents, medals 🥇🥈🥉 | `StatsScreen.kt` |
| 4 | ProductDetail : sticky header au scroll, grille 3 boutons côte à côte, auto-save notes | `ProductDetailScreen.kt` |
| 5 | History : 3 sections colorées (Ouverts/Consommés/Jetés). Notifications : carte "permission refusée" + lien paramètres Android | `HistoryScreen.kt`, `NotificationsScreen.kt` |
| 6 | Sélection image OFF (5 choix), email dans profil, bannière hors ligne, retry OCR avec backoff | `AddProductScreen.kt`, `AddProductViewModel.kt`, `SettingsScreen.kt` |

### Journal de développement Android

**2026-05-06 — Étape 1 : stabilisation navigation/auth + avatar**
- Issues GitHub `Android` prises en compte : #1 `Upload image profil`, #2 `Lenteurs au démarrage`.
- `AppNavigation.kt` démarre désormais sur une route `loading` et ne renvoie plus vers `MAIN` à chaque nouvelle émission `AuthState.Authenticated` ; seules les transitions de gate auth (`AUTH` / `HOUSEHOLD_SETUP` / `MAIN`) déclenchent une navigation racine. Cela évite le retour inattendu à l'accueil après `refreshHousehold()`.
- `HouseholdRepository.uploadAvatar()` versionne l'URL publique avec `?v=<timestamp>` après l'upsert Storage pour forcer Coil à recharger l'avatar mis à jour.
- `IndexViewModel` garde `isLoading=true` pendant `AuthState.Loading` et pendant `fetchAndCache()` initial, afin d'éviter un écran d'accueil vide avant l'arrivée des produits.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

---

## Android — Référence fonctionnalités PWA

Ce guide documente les comportements de la PWA à reproduire sur Android. Toujours consulter cette section avant d'implémenter ou modifier un écran Android.

### Index (frigo principal)

- **Header dynamique** : fond rouge si produits périmés, orange si produits bientôt périmés, primaire sinon. Affiche compteur d'alertes.
- **3 stat-cards cliquables** : Périmés / Bientôt / Frais — cliquer applique un filtre rapide.
- **Sections collapsibles** : Périmés / Bientôt périmés / Frais, chacune pliable/dépliable.
- **Swipe** : gauche = consommé, droite = jeté.
- **Long-press multi-select** : 500ms, tolérance 8px de mouvement. Barre batch en bas (supprimer, changer statut).
- **FAB bubble menu** : FAB "+" s'anime en "×" et fait apparaître 2 bulles en spring : "Un produit" et "Plusieurs produits".
- **Pull-to-refresh**, skeleton loading, état vide animé.

### ProductDetail

- **Hero scroll** : image plein-largeur (300dp) avec gradient selon statut (rouge/orange/vert).
- **Sticky header** : apparaît après ~110dp de scroll — affiche bouton retour, miniature image, nom du produit (backdrop blur sur PWA). Sur Android : header compact dans la TopAppBar ou overlay dynamique.
- **Actions produit** : grille 3 boutons côte à côte, toujours visibles : [Ouvert] [Consommé] [Jeté]. Bouton "Remettre actif" séparé pour les produits archivés.
- **Accordéons** (PWA) vs **Tabs** (Android acceptable) : sur la PWA, 3 accordéons verticaux (Nutrition & Allergènes / Ingrédients / Détails & Historique). Sur Android, tabs sont une adaptation native valide.
- **Notes auto-save** : sauvegarder automatiquement à la perte de focus (`onFocusChanged hasFocus=false`), pas de bouton "Sauvegarder" explicite.
- **Boutons Modifier/Supprimer** : full-width en bas de page sur la PWA. Sur Android, icônes dans TopAppBar acceptable.

### Stats

**Onglet Frigo** :
- Barre de distribution 3 segments (Frais / Bientôt / Périmés).
- Produits urgents : chaque item affiche une barre de vie animée — largeur = `daysLeft / totalLifeDays * 100%`. Couleur : verte si >50%, orange si 20-50%, rouge si <20%.

**Onglet Anti-Gaspi** :
- Jauge demi-cercle (SVG/Canvas) colorée dynamiquement : vert si score >75%, orange si 50-75%, rouge si <50%. Texte d'évaluation ("Excellent !", "Bien !", "À améliorer").
- Score mensuel + trend vs mois précédent + streak + taux d'utilisation.
- Chart aire sur 6 mois (Vico sur Android, Recharts sur PWA).
- Top gaspillés avec medals : 🥇🥈🥉 pour le top 3.

**Onglet Tendances** :
- Chart barres groupées (ajoutés / consommés / jetés par mois).
- Durée moyenne de conservation.
- Top récurrents avec medals 🥇🥈🥉.

### History

- 3 sections distinctes avec icônes colorées :
  - "Ouverts" : icône `PackageOpen`, couleur primary
  - "Consommés" : icône `UtensilsCrossed`, couleur success/green
  - "Jetés" : icône `Trash2`, couleur destructive/red
- Les filtres chips peuvent masquer les sections entières (pas seulement les items).

### Notifications

- Filtres Tout / Périmés / Bientôt (chips).
- Toggle notifications + choix délai (1/3/7 jours avant péremption).
- Si permission notifications refusée définitivement → Card rouge "Notifications bloquées" + bouton "Ouvrir les paramètres" (`Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)`).

### Settings

- Section "Mon profil" : avatar, pseudo (édition inline), **email affiché** (à récupérer depuis `AuthRepository`).
- Apparence : thème (clair/sombre/système), couleur accent, densité, réduire animations.
- Foyer : nom du foyer (owner seulement), code invitation (copier + partager), liste membres + suppression.

### Mode multi-scan (flux complet)

```
FAB → "Plusieurs produits"
  → BarcodeScannerScreen (isMultiScan=true)
  → DateScannerScreen (isMultiScan=true, barcode=xxx)
  → AddProductScreen (isMultiMode=true, barcode=xxx, initialDate=dd/MM/yyyy)
  → "Ajouter & scanner le suivant" → navigate BARCODE_SCANNER_MULTI (popUpTo inclusive)
  → [répète depuis BarcodeScannerScreen]
  → "Terminer" → popBackStack jusqu'à BARCODE_SCANNER_MULTI inclusive
```

Sentinelles URL : barcode vide → `"-"`, date vide → `"-"`. Les dates utilisent `_` à la place de `/` dans l'URL.

### Indicateur hors ligne

Bannière jaune en haut de l'Index (ou MainScreen) : "Hors ligne — les données affichées peuvent être obsolètes". Utiliser `ConnectivityManager` + `registerNetworkCallback` sur Android.

### Barcode / date scanning

- Lookup OpenFoodFacts après scan barcode : récupère jusqu'à 5 images (principale, recto, nutrition, ingrédients, packaging). Afficher un dialog de sélection avec previews — ne pas auto-sélectionner la première.
- OCR date : d'abord ML Kit local (`TextRecognition`), puis Edge Function Gemini 2.5 Flash si pas de résultat (cooldown 30s). Ajouter retry avec backoff exponentiel (2 tentatives, délai 2s) sur l'Edge Function.
