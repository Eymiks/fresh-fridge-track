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

### Phases terminées (Mission Codex 2026-05-06)

| Phase | Description | Fichiers principaux | État |
|-------|-------------|---------------------|------|
| 3 | Stats visuelles : jauge semicircle (Canvas Compose), barre de vie produits urgents, medals 🥇🥈🥉 | `StatsScreen.kt` | ✅ |
| 4 | ProductDetail : sticky header au scroll, grille 3 boutons côte à côte, auto-save notes | `ProductDetailScreen.kt` | ✅ |
| 5 | History : 3 sections colorées (Ouverts/Consommés/Jetés). Notifications : carte "permission refusée" + lien paramètres Android | `HistoryScreen.kt`, `NotificationsScreen.kt` | ✅ |
| 6 | Sélection image OFF (5 choix), email dans profil, bannière hors ligne, retry OCR avec backoff | `AddProductScreen.kt`, `AddProductViewModel.kt`, `SettingsScreen.kt`, `OfflineBanner.kt`, `DateScannerViewModel.kt` | ✅ |

### Journal de développement Android

**2026-05-06 — Étape 1 : stabilisation navigation/auth + avatar**
- Issues GitHub `Android` prises en compte : #1 `Upload image profil`, #2 `Lenteurs au démarrage`.
- `AppNavigation.kt` démarre désormais sur une route `loading` et ne renvoie plus vers `MAIN` à chaque nouvelle émission `AuthState.Authenticated` ; seules les transitions de gate auth (`AUTH` / `HOUSEHOLD_SETUP` / `MAIN`) déclenchent une navigation racine. Cela évite le retour inattendu à l'accueil après `refreshHousehold()`.
- `HouseholdRepository.uploadAvatar()` versionne l'URL publique avec `?v=<timestamp>` après l'upsert Storage pour forcer Coil à recharger l'avatar mis à jour.
- `IndexViewModel` garde `isLoading=true` pendant `AuthState.Loading` et pendant `fetchAndCache()` initial, afin d'éviter un écran d'accueil vide avant l'arrivée des produits.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 2 : optimisation scroll accueil**
- Issue GitHub `Android` prise en compte : #3 `Scroll accueil saccadé`.
- `IndexScreen.ProductCard()` n'utilise plus `height(IntrinsicSize.Min)` / `fillMaxHeight()` pour la bande de statut ; la bande est dessinée via `drawBehind`, ce qui évite une mesure intrinsèque coûteuse pour chaque item de la `LazyColumn`.
- Les images produit affichées dans les cartes utilisent `ContentScale.Crop` sur une taille stable de 52dp.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 3 : sélecteur de date produit**
- Issue GitHub `Android` prise en compte : #6 `Sélecteur de date produit`.
- `AddProductScreen.kt` ajoute un `DatePickerDialog` Material3 sur le champ `Date de péremption`, en complément de la saisie manuelle et du scanner caméra existants.
- Le DatePicker initialise sa sélection depuis la valeur du formulaire quand elle est valide, puis réécrit la date au format `JJ/MM/AAAA`; le flux est commun à l'ajout et à la modification de produit.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 4 : actions fixes sur ajout produit**
- Issue GitHub `Android` prise en compte : #4 `Ajout produit trop long`.
- `AddProductScreen.kt` déplace les actions principales dans le `bottomBar` du `Scaffold`, afin que `Ajouter au frigo`, `Enregistrer les modifications`, `Ajouter & scanner le suivant` et `Terminer` restent accessibles sans descendre en bas du formulaire.
- Le formulaire reste scrollable avec les mêmes champs ; cette étape ne réorganise pas encore les sections avancées.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 5 : robustesse upload avatar**
- Issue GitHub `Android` reprise : #1 `Upload image profil`.
- `HouseholdRepository.uploadAvatar()` utilise désormais un chemin Storage unique par timestamp, stocke l'URL publique réelle sans query string et relit la ligne `household_members` mise à jour via `select()`.
- `HouseholdSettingsViewModel` conserve un `avatarUrlOverride` pour afficher immédiatement la nouvelle photo après succès.
- `SettingsScreen` lit le fichier choisi dans `Dispatchers.IO` avant d'appeler l'upload.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 6 : insets des barres basses**
- Issue GitHub `Android` reprise : #4 `Ajout produit trop long`.
- `AddProductBottomBar` applique `navigationBarsPadding()` et `imePadding()` afin que les actions restent au-dessus de la barre système et du clavier.
- `MainScreen` et la barre batch de `IndexScreen` appliquent aussi `navigationBarsPadding()` sur leurs barres basses.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 7 : fluidité accueil**
- Issue GitHub `Android` reprise : #3 `Scroll accueil saccadé`.
- `IndexScreen` ajoute des `contentType` stables aux items de la `LazyColumn` et stabilise les callbacks produit par `remember(product.id, ...)`.
- `ProductCard` mémorise les calculs de statut/date et utilise un `ImageRequest` Coil stable sur une image contrainte à 52dp, sans activer de crossfade.
- Les animations de l'état vide et du FAB bubble menu respectent `reduceMotion`.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 8 : retour picker avatar**
- Issue GitHub `Android` reprise : #1 `Upload image profil`.
- `AppNavigation` ne renavigue plus vers `MAIN` quand l'utilisateur est déjà dans une route authentifiée (`settings`, détail, scanner, etc.) après une émission auth `Loading -> Authenticated`.
- `SettingsScreen` ne quitte plus silencieusement le callback avatar si le foyer, l'utilisateur ou la lecture de l'image échoue ; une erreur explicite est envoyée au snackbar.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 9 : fallback foyer Paramètres**
- Issue GitHub `Android` reprise : #1 `Upload image profil`.
- `SettingsScreen` utilise `AuthState.Authenticated.household` et `members` comme source de secours lorsque `HouseholdSettingsViewModel` n'a pas encore réémis le foyer après le retour du picker.
- Le fallback couvre l'upload avatar, l'affichage du profil, le nom du foyer, le code d'invitation et la liste des membres.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 10 : upload avatar indépendant de Compose**
- Issue GitHub `Android` reprise : #1 `Upload image profil`.
- `HouseholdSettingsViewModel` garde désormais en cache le dernier `userId` et `householdId` authentifiés, puis résout la cible de l'avatar côté ViewModel au moment de l'upload.
- `SettingsScreen` et `HouseholdSettingsScreen` ne dépendent plus du foyer capturé dans la callback du picker : ils lisent seulement les bytes de l'image et appellent `uploadCurrentUserAvatar()`.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 11 : sous-catégories Android**
- Issue GitHub `Android` prise en compte : #5 `Améliorer les sous-catégories`.
- `AddProductViewModel` applique désormais la catégorie automatique avec `matchCategory()` et complète la sous-catégorie vide avec `matchSubcategory()` au moment de l'enregistrement.
- `AddProductScreen` ajoute l'option `Automatique` aux catégories et remplace la saisie libre de sous-catégorie par un sélecteur lié à la catégorie courante.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 12 : formulaire d'ajout compact**
- Issue GitHub `Android` reprise : #4 `Ajout produit trop long`.
- `AddProductScreen` regroupe le formulaire en sections compactes : `Essentiel`, `Image`, `Conservation` et `Informations avancées`.
- Les champs de conservation et les informations avancées sont repliés par défaut, tandis que les champs essentiels et les actions image restent immédiatement accessibles.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 13 : bannière hors ligne accueil**
- Écart PWA / Android corrigé : l'indicateur hors ligne est visible sur l'accueil et les onglets principaux.
- `MainScreen` affiche désormais `OfflineBanner()` au-dessus du `NavHost` des onglets.
- `NotificationsScreen` ne rend plus sa propre bannière hors ligne pour éviter un doublon dans l'onglet Alertes.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 14 : fiche produit alignée PWA**
- Écart PWA / Android repris : `ProductDetailScreen` abandonne la `TopAppBar` et les tabs au profit d'un hero immersif, d'une carte produit flottante, d'une carte date limite et d'accordéons verticaux.
- Les actions rapides sont regroupées dans une carte dédiée avec `Ouvert`, `Consommé`, `Jeté`, congélation et remise active pour les produits archivés.
- Les actions bas d'écran affichent désormais `Modifier` puis `Supprimer` sous forme de boutons full-width avec `navigationBarsPadding()`.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 15 : permission notifications fiable**
- Écart Android corrigé : l'écran Alertes ne considère plus l'état initial Android 13+ comme des notifications définitivement bloquées.
- `AppPreferences` persiste `frigo-notif-permission-requested`, exposé par `NotificationsViewModel`.
- `NotificationsScreen` affiche la carte rouge "Notifications bloquées" uniquement après une demande de permission déjà effectuée, refusée, et sans rationale système.
- Vérifications : `./gradlew.bat :app:assembleDebug` OK ; `./gradlew.bat :app:testDebugUnitTest` OK (`NO-SOURCE`).

**2026-05-06 — Étape 16 : flux scan simple barcode puis date**
- Écart PWA / Android corrigé : le scan simple enchaîne désormais code-barres → scanner de date → formulaire prérempli.
- `AppNavigation` ajoute `DATE_SCANNER_ADD` et `ADD_PRODUCT_SCANNED_DATE`, avec dates encodées via `_` comme en multi-scan.
- `DateScannerScreen` garde le retour `savedStateHandle` pour le bouton caméra du formulaire, et navigue vers le formulaire seulement pour le flux d'ajout issu du scanner code-barres.
- Vérifications : `./gradlew.bat :app:assembleDebug` OK ; `./gradlew.bat :app:testDebugUnitTest` OK (`NO-SOURCE`).

**2026-05-06 — Étape 17 : courbe mensuelle Anti-Gaspi**
- Écart visuel Stats repris : l'évolution mensuelle Anti-Gaspi utilise désormais une courbe Vico (`rememberLineCartesianLayer` + `lineSeries`) au lieu d'un chart en colonnes.
- Les barres groupées Ajouts / consommés / jetés de l'onglet Tendances restent en colonnes, conformément à la référence PWA.
- À vérifier sur appareil : rendu exact de la courbe Vico, axes et lisibilité selon thème.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 18 : polish Historique et header accueil**
- `HistoryScreen` utilise des icônes Material plus proches des états PWA : paquet pour Ouverts, restaurant pour Consommés, corbeille pour Jetés.
- La couleur "Consommé" est harmonisée en vert (`ColorFresh`) dans les sections et les badges.
- `IndexScreen` rend le header dynamique plus visible : rouge/orange plus marqués en présence d'alertes, primaire subtil sinon.
- À vérifier sur appareil : rendu exact des contrastes header selon thème et couleur d'accent.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.

**2026-05-06 — Étape 19 : fallback manuel scanner date**
- `DateScannerScreen` ajoute une action commune de saisie manuelle qui conserve le contexte du flux.
- En scan simple, "Saisir manuellement" ouvre le formulaire avec le code-barres conservé ; en multi-scan, il ouvre le formulaire chaîne avec date vide.
- Le bouton manuel est visible même quand la caméra est autorisée mais qu'aucune date n'est encore détectée.
- Vérifications : `./gradlew.bat :app:assembleDebug` OK ; `./gradlew.bat :app:testDebugUnitTest` OK (`NO-SOURCE`).

**2026-05-06 — Étape 20 : notifications expirées et permissions cohérentes**
- `ExpirationCheckWorker` passe par `ProductRepository` et utilise des snapshots domaine pour les foyers authentifiés et le mode invité.
- Les notifications de fond incluent désormais les produits déjà périmés, en plus des produits bientôt périmés.
- `SettingsScreen` partage le suivi `frigo-notif-permission-requested` via `AppearanceViewModel` et désactive les notifications si la permission Android est refusée depuis Paramètres.
- À vérifier sur appareil : exécution WorkManager réelle, notifications en mode invité, et retour depuis les paramètres Android.
- Vérifications : `./gradlew.bat :app:assembleDebug` OK ; `./gradlew.bat :app:testDebugUnitTest` OK (`NO-SOURCE`).

**2026-05-07 — Mission alignement visuel Android / PWA**
- Thème Android rapproché de la PWA : fond vert très pâle, surfaces carte, bordures, couleurs statut, arrondis et graisses typographiques Compose.
- `AuthScreen`, `IndexScreen`, `AddProductScreen` et `AppNavigation` polis visuellement sans changement de logique métier : Auth vertical, header accueil, stat-cards, cartes produit avec badges courts, sections formulaire et barre de navigation.
- Commits créés par étape : thème, Auth, accueil, cartes produit, formulaire produit, navigation + documentation.
- À vérifier sur appareil : contraste clair/sombre, header accueil selon alertes, clavier sur Auth/AddProduct, bottom nav avec barre système, lisibilité des badges courts.

**2026-05-07 — Suite polish accueil Android d'après capture PWA**
- `IndexScreen` reprend davantage la capture PWA : stat-cards placées dans le header, barre recherche + tri + filtre sur une seule ligne, suppression des chips catégories permanentes.
- Le bouton filtre carré ouvre un menu combinant statut et catégorie via les setters existants, sans changer la logique métier ni le modèle de données.
- La banner des produits périmés affiche désormais une action `Voir`, les sections utilisent des icônes Compose plutôt que des emoji texte, et les cartes produit alignent badge date + Nutri-Score à droite avec image stable 52dp.
- Commit créé : `827badf android: affiner l'accueil d'après la capture PWA`.
- Vérifications : `./gradlew.bat :app:assembleDebug` OK ; `./gradlew.bat :app:testDebugUnitTest` OK (`NO-SOURCE`) ; APK installé et lancé sur l'appareil `ZY22HVMV3X`.
- À vérifier sur appareil : densité exacte des cartes sur petit écran et lisibilité du menu filtre en mode sombre.

**2026-05-07 — Polish Stats Android**
- `StatsScreen` remplace le `TabRow` Material brut par un segmented control plus proche de la PWA et ajoute un header d'écran plus dense.
- Les blocs Frigo, urgence, catégories, Anti-Gaspi, tendances et tops produits utilisent désormais des cartes bordées `rounded-2xl` avec titres en pastilles teintées, pour une hiérarchie visuelle plus cohérente avec la PWA.
- Les états de chargement et de vide passent par des cartes centrées au lieu de simples textes isolés.
- Commit : `android: polir les statistiques façon PWA`.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.
- À vérifier sur appareil : lisibilité exacte des charts Vico une fois enchâssés dans les cartes, et densité verticale sur petit écran.

**2026-05-07 — Polish Historique Android**
- `HistoryScreen` adopte un header plus compact, des filtres pill avec compteur intégré et des sections plus proches du vocabulaire visuel de la PWA.
- Les cartes produit gagnent une bordure légère, des coins plus généreux et une action `Réactiver` plus discrète que le `TextButton` Material d'origine.
- L'état vide passe dans une carte centrée avec icône teintée pour rester cohérent avec les autres écrans Android polis.
- Commit : `android: harmoniser l'historique avec la PWA`.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.
- À vérifier sur appareil : confort de lecture des filtres sur petit écran et densité réelle des cartes réactivables.

**2026-05-07 — Polish Alertes Android**
- `NotificationsScreen` reçoit un header plus dense, des filtres pill custom et des en-têtes de sections avec icône + compteur plus proches de la PWA.
- La carte de réglage des notifications push abandonne le rendu Material trop neutre au profit d'une surface bordée avec pastille d'icône et rappels visuels plus cohérents.
- Les états vides et la carte `Notifications bloquées` sont harmonisés avec le reste de l'application Android.
- Commit : `android: polir les alertes façon PWA`.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.
- À vérifier sur appareil : contraste de la section `Bientôt périmés`, confort des pills horizontales et rendu de la carte permission refusée.

**2026-05-07 — Finalisation visuelle formulaire produit**
- `AddProductScreen` affiche désormais le chargement dans une vraie carte bordée, au lieu d'un simple spinner isolé au centre.
- Les erreurs sont regroupées dans une carte teintée avec icône d'alerte, et les sections du formulaire gagnent une pastille d'icône pour mieux rappeler la hiérarchie visuelle de la PWA.
- Le dialog de sélection d'images OpenFoodFacts est densifié avec carte bordée et titrage plus net, sans changer le flux de sélection existant.
- Commit : `android: finaliser les états visuels produit`.
- Vérification : `./gradlew.bat :app:assembleDebug` OK.
- À vérifier sur appareil : confort du bas de formulaire avec clavier, contraste de la carte d'erreur, et rendu du dialog image en mode sombre.

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
- **Boutons Modifier/Supprimer** : full-width en bas de page sur la PWA. Sur Android, boutons fixes en bas avec `navigationBarsPadding()`.

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
