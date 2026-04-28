-- ============================================================
-- FreshTrack — Schéma multi-foyer
-- À exécuter dans Supabase Dashboard > SQL Editor
-- ============================================================

-- Foyers
CREATE TABLE IF NOT EXISTS households (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name         TEXT NOT NULL,
  invite_code  TEXT UNIQUE NOT NULL DEFAULT upper(substring(replace(gen_random_uuid()::text, '-', ''), 1, 8)),
  created_by   UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at   TIMESTAMPTZ DEFAULT now()
);

-- Membres d'un foyer
CREATE TABLE IF NOT EXISTS household_members (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  household_id UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
  user_id      UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  display_name TEXT NOT NULL,
  joined_at    TIMESTAMPTZ DEFAULT now(),
  UNIQUE(household_id, user_id)
);

-- Produits
CREATE TABLE IF NOT EXISTS products (
  id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  household_id       UUID NOT NULL REFERENCES households(id) ON DELETE CASCADE,
  added_by           UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  name               TEXT NOT NULL,
  barcode            TEXT,
  expiration_date    DATE NOT NULL,
  added_at           TIMESTAMPTZ DEFAULT now(),
  image_url          TEXT,
  brand              TEXT,
  nutri_score        TEXT,
  category           TEXT,
  subcategory        TEXT,
  status             TEXT DEFAULT 'active',
  status_changed_at  TIMESTAMPTZ,
  quantity           TEXT,
  nova_group         INTEGER,
  eco_score          TEXT,
  allergens          TEXT,
  ingredients        TEXT,
  opened_at          TIMESTAMPTZ,
  days_after_opening INTEGER
);

-- ============================================================
-- Row Level Security
-- ============================================================
ALTER TABLE households        ENABLE ROW LEVEL SECURITY;
ALTER TABLE household_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE products          ENABLE ROW LEVEL SECURITY;

-- Households : créer son foyer
CREATE POLICY "create_household" ON households
  FOR INSERT TO authenticated
  WITH CHECK (auth.uid() = created_by);

-- Households : voir son foyer si membre
CREATE POLICY "members_view_household" ON households
  FOR SELECT USING (
    id IN (SELECT household_id FROM household_members WHERE user_id = auth.uid())
  );

-- Household_members : voir sa propre ligne (bootstrap sans récursion)
CREATE POLICY "view_own_membership" ON household_members
  FOR SELECT USING (user_id = auth.uid());

-- Household_members : voir tous les membres du même foyer
CREATE POLICY "view_household_members" ON household_members
  FOR SELECT USING (
    household_id IN (SELECT household_id FROM household_members WHERE user_id = auth.uid())
  );

-- Household_members : s'inscrire soi-même
CREATE POLICY "insert_own_membership" ON household_members
  FOR INSERT WITH CHECK (user_id = auth.uid());

-- Products : accès complet aux produits de son foyer
CREATE POLICY "household_products" ON products
  FOR ALL USING (
    household_id IN (SELECT household_id FROM household_members WHERE user_id = auth.uid())
  );

-- ============================================================
-- Fonction SECURITY DEFINER : rejoindre via code d'invitation
-- Bypasse le RLS pour la recherche par invite_code
-- ============================================================
CREATE OR REPLACE FUNCTION join_household_by_code(p_invite_code TEXT, p_display_name TEXT)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_household RECORD;
BEGIN
  SELECT id, name INTO v_household
  FROM households
  WHERE invite_code = upper(p_invite_code);

  IF NOT FOUND THEN
    RETURN json_build_object('error', 'Code invalide. Vérifiez et réessayez.');
  END IF;

  INSERT INTO household_members (household_id, user_id, display_name)
  VALUES (v_household.id, auth.uid(), p_display_name)
  ON CONFLICT (household_id, user_id) DO UPDATE SET display_name = p_display_name;

  RETURN json_build_object('success', true, 'household_id', v_household.id, 'name', v_household.name);
END;
$$;

-- ============================================================
-- Realtime sur la table products (ignoré si déjà membre)
-- ============================================================
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND tablename = 'products'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE products;
  END IF;
END $$;
