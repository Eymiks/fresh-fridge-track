import { useState } from 'react';
import { Users, Plus, Hash, ArrowRight, LeafyGreen } from 'lucide-react';
import { supabase } from '@/integrations/supabase/client';
import { useAuth } from '@/contexts/AuthContext';

type Mode = 'choose' | 'create' | 'join';

export default function HouseholdSetup() {
  const { user, refreshHousehold, signOut } = useAuth();
  const [mode, setMode] = useState<Mode>('choose');
  const [householdName, setHouseholdName] = useState('');
  const [inviteCode, setInviteCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const displayName: string = (user?.user_metadata?.display_name as string) || 'Membre';

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    setError('');
    setLoading(true);
    const { data: hh, error: errHh } = await supabase
      .from('households')
      .insert({ name: householdName.trim(), created_by: user.id })
      .select('id')
      .single();

    if (errHh || !hh) {
      setError(errHh?.message ?? 'Erreur lors de la création.');
      setLoading(false);
      return;
    }

    const { error: errMember } = await supabase
      .from('household_members')
      .insert({ household_id: hh.id, user_id: user.id, display_name: displayName });

    if (errMember) {
      setError(errMember.message);
    } else {
      await refreshHousehold();
    }
    setLoading(false);
  };

  const handleJoin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    setError('');
    setLoading(true);
    const { data, error: err } = await supabase.rpc('join_household_by_code', {
      p_invite_code: inviteCode.trim().toUpperCase(),
      p_display_name: displayName,
    });

    if (err) {
      setError(err.message);
    } else {
      const result = data as { error?: string; success?: boolean };
      if (result?.error) {
        setError(result.error);
      } else {
        await refreshHousehold();
      }
    }
    setLoading(false);
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center px-5">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center mb-8">
          <div className="w-16 h-16 bg-primary/10 rounded-2xl flex items-center justify-center mb-3">
            <LeafyGreen className="w-8 h-8 text-primary" />
          </div>
          <h1 className="text-2xl font-extrabold text-foreground">Bienvenue, {displayName} !</h1>
          <p className="text-sm text-muted-foreground mt-1 text-center">
            Créez votre foyer ou rejoignez celui d'un proche.
          </p>
        </div>

        {error && (
          <div className="bg-destructive/10 border border-destructive/20 text-destructive text-sm font-semibold rounded-xl px-4 py-3 mb-4">
            {error}
          </div>
        )}

        {mode === 'choose' && (
          <div className="space-y-3">
            <button
              onClick={() => setMode('create')}
              className="w-full flex items-center gap-4 bg-card border border-border rounded-2xl p-4 hover:bg-muted transition-colors text-left"
            >
              <div className="w-11 h-11 bg-primary/10 rounded-xl flex items-center justify-center shrink-0">
                <Plus className="w-5 h-5 text-primary" />
              </div>
              <div>
                <p className="font-bold text-sm text-foreground">Créer un foyer</p>
                <p className="text-xs text-muted-foreground">Je suis le premier de ma famille</p>
              </div>
              <ArrowRight className="w-4 h-4 text-muted-foreground ml-auto" />
            </button>

            <button
              onClick={() => setMode('join')}
              className="w-full flex items-center gap-4 bg-card border border-border rounded-2xl p-4 hover:bg-muted transition-colors text-left"
            >
              <div className="w-11 h-11 bg-success/10 rounded-xl flex items-center justify-center shrink-0">
                <Users className="w-5 h-5 text-success" />
              </div>
              <div>
                <p className="font-bold text-sm text-foreground">Rejoindre un foyer</p>
                <p className="text-xs text-muted-foreground">J'ai un code d'invitation</p>
              </div>
              <ArrowRight className="w-4 h-4 text-muted-foreground ml-auto" />
            </button>

            <button onClick={signOut} className="w-full text-center text-xs text-muted-foreground mt-4 hover:text-foreground transition-colors">
              Se déconnecter
            </button>
          </div>
        )}

        {mode === 'create' && (
          <form onSubmit={handleCreate} className="space-y-3">
            <div className="relative">
              <Users className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <input
                type="text"
                placeholder="Nom du foyer (ex: Famille Dupont)"
                value={householdName}
                onChange={e => setHouseholdName(e.target.value)}
                className="w-full pl-10 pr-4 py-3 bg-card border border-border rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/30"
                required
                autoFocus
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-primary text-primary-foreground py-3 rounded-xl text-sm font-bold hover:bg-primary/90 transition-colors disabled:opacity-60"
            >
              {loading ? 'Création…' : 'Créer le foyer'}
            </button>
            <button type="button" onClick={() => { setMode('choose'); setError(''); }} className="w-full text-center text-xs text-muted-foreground hover:text-foreground transition-colors">
              Retour
            </button>
          </form>
        )}

        {mode === 'join' && (
          <form onSubmit={handleJoin} className="space-y-3">
            <div className="relative">
              <Hash className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <input
                type="text"
                placeholder="Code d'invitation (ex: ABCD1234)"
                value={inviteCode}
                onChange={e => setInviteCode(e.target.value.toUpperCase())}
                className="w-full pl-10 pr-4 py-3 bg-card border border-border rounded-xl text-sm font-mono uppercase focus:outline-none focus:ring-2 focus:ring-primary/30"
                required
                maxLength={8}
                autoFocus
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-success text-success-foreground py-3 rounded-xl text-sm font-bold hover:bg-success/90 transition-colors disabled:opacity-60"
            >
              {loading ? 'Vérification…' : 'Rejoindre le foyer'}
            </button>
            <button type="button" onClick={() => { setMode('choose'); setError(''); }} className="w-full text-center text-xs text-muted-foreground hover:text-foreground transition-colors">
              Retour
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
