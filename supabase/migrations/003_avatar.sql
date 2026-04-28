-- Colonne avatar_url sur household_members
ALTER TABLE household_members ADD COLUMN IF NOT EXISTS avatar_url TEXT;

-- Chaque membre peut mettre à jour sa propre ligne (ex: avatar_url)
CREATE POLICY "update_own_membership" ON household_members
  FOR UPDATE TO authenticated
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());
