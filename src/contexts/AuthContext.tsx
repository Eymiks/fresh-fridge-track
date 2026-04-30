import { createContext, useContext, useEffect, useState, useCallback, useMemo } from 'react';
import { User } from '@supabase/supabase-js';
import { supabase } from '@/integrations/supabase/client';

const GUEST_MODE_KEY = 'freshtrack-guest-mode';

export interface Household {
  id: string;
  name: string;
  invite_code: string;
  created_by: string | null;
}

export interface Member {
  user_id: string;
  display_name: string;
  avatar_url?: string | null;
}

interface AuthContextType {
  user: User | null;
  household: Household | null;
  members: Member[];
  displayName: string;
  isGuest: boolean;
  loading: boolean;
  enterGuest: () => void;
  exitGuest: () => void;
  signOut: () => Promise<void>;
  refreshHousehold: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [household, setHousehold] = useState<Household | null>(null);
  const [members, setMembers] = useState<Member[]>([]);
  const [isGuest, setIsGuest] = useState(() =>
    typeof window !== 'undefined' && localStorage.getItem(GUEST_MODE_KEY) === 'true'
  );
  const [loading, setLoading] = useState(true);

  const fetchHousehold = useCallback(async (userId: string) => {
    const { data: myMembership } = await supabase
      .from('household_members')
      .select('household_id')
      .eq('user_id', userId)
      .maybeSingle();

    if (!myMembership) {
      setHousehold(null);
      setMembers([]);
      return;
    }

    const [{ data: hh }, { data: allMembers }] = await Promise.all([
      supabase
        .from('households')
        .select('id, name, invite_code, created_by')
        .eq('id', myMembership.household_id)
        .maybeSingle(),
      supabase
        .from('household_members')
        .select('user_id, display_name, avatar_url')
        .eq('household_id', myMembership.household_id),
    ]);

    if (hh) {
      setHousehold({ id: hh.id, name: hh.name, invite_code: hh.invite_code, created_by: hh.created_by });
      setMembers(allMembers ?? []);
    } else {
      setHousehold(null);
      setMembers([]);
    }
  }, []);

  useEffect(() => {
    let mounted = true;

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      const u = session?.user ?? null;
      if (!mounted) return;
      setUser(u);
      if (u) {
        localStorage.removeItem(GUEST_MODE_KEY);
        setIsGuest(false);
        fetchHousehold(u.id).finally(() => {
          if (mounted) setLoading(false);
        });
      } else {
        setHousehold(null);
        setMembers([]);
        setLoading(false);
      }
    });

    return () => {
      mounted = false;
      subscription.unsubscribe();
    };
  }, [fetchHousehold]);

  const enterGuest = useCallback(() => {
    localStorage.setItem(GUEST_MODE_KEY, 'true');
    setUser(null);
    setHousehold(null);
    setMembers([]);
    setIsGuest(true);
  }, []);

  const exitGuest = useCallback(() => {
    localStorage.removeItem(GUEST_MODE_KEY);
    setIsGuest(false);
  }, []);

  const signOut = async () => {
    await supabase.auth.signOut();
    localStorage.removeItem(GUEST_MODE_KEY);
    setUser(null);
    setHousehold(null);
    setMembers([]);
    setIsGuest(false);
  };

  const refreshHousehold = useCallback(async () => {
    if (user) await fetchHousehold(user.id);
  }, [user, fetchHousehold]);

  const displayName = useMemo(
    () => isGuest ? 'Invité' : members.find(m => m.user_id === user?.id)?.display_name ?? '',
    [isGuest, members, user?.id]
  );

  return (
    <AuthContext.Provider value={{ user, household, members, displayName, isGuest, loading, enterGuest, exitGuest, signOut, refreshHousehold }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
