import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { User } from '@supabase/supabase-js';
import { supabase } from '@/integrations/supabase/client';

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
  loading: boolean;
  signOut: () => Promise<void>;
  refreshHousehold: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [household, setHousehold] = useState<Household | null>(null);
  const [members, setMembers] = useState<Member[]>([]);
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

  const signOut = async () => {
    await supabase.auth.signOut();
    setUser(null);
    setHousehold(null);
    setMembers([]);
  };

  const refreshHousehold = useCallback(async () => {
    if (user) await fetchHousehold(user.id);
  }, [user, fetchHousehold]);

  const displayName = members.find(m => m.user_id === user?.id)?.display_name ?? '';

  return (
    <AuthContext.Provider value={{ user, household, members, displayName, loading, signOut, refreshHousehold }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
