-- ============================================================
-- FreshTrack — Politiques RLS pour actions propriétaire
-- À exécuter dans Supabase Dashboard > SQL Editor
-- ============================================================

-- Households : le créateur peut renommer son foyer
CREATE POLICY "owner_update_household" ON households
  FOR UPDATE TO authenticated
  USING (auth.uid() = created_by)
  WITH CHECK (auth.uid() = created_by);

-- Household_members : le créateur du foyer peut retirer n'importe quel membre
CREATE POLICY "owner_remove_members" ON household_members
  FOR DELETE TO authenticated
  USING (
    household_id IN (
      SELECT id FROM households WHERE created_by = auth.uid()
    )
  );
