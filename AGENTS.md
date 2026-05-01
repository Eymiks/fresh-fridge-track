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
