import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Bell,
  BellOff,
  BellRing,
  Camera,
  Check,
  Copy,
  Home,
  Info,
  LogOut,
  Moon,
  Pencil,
  Settings as SettingsIcon,
  Share2,
  Sun,
  User,
  UserMinus,
  Users,
  X,
} from 'lucide-react';
import { motion } from 'framer-motion';
import { toast } from 'sonner';
import { PageTransition } from '@/components/PageTransition';
import { Switch } from '@/components/ui/switch';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { useAuth } from '@/contexts/AuthContext';
import { useNotificationSettings } from '@/hooks/useNotificationSettings';
import { useIsMobile } from '@/hooks/use-mobile';
import { supabase } from '@/integrations/supabase/client';

const DAY_OPTIONS = [1, 3, 7];

function Section({
  title,
  icon: Icon,
  children,
  className = '',
}: {
  title: string;
  icon: typeof User;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      className={`bg-card border border-border rounded-2xl p-4 sm:p-5 ${className}`}
    >
      <div className="flex items-center gap-2 mb-4">
        <div className="w-8 h-8 rounded-xl bg-primary/10 text-primary flex items-center justify-center">
          <Icon className="w-4 h-4" />
        </div>
        <h2 className="text-sm font-extrabold text-foreground">{title}</h2>
      </div>
      {children}
    </motion.section>
  );
}

export default function Settings() {
  const navigate = useNavigate();
  const isMobile = useIsMobile();
  const { household, members, displayName, signOut, user, refreshHousehold } = useAuth();
  const {
    enabled,
    days,
    permission,
    isSupported,
    setEnabled,
    setDays,
    requestPermission,
  } = useNotificationSettings();

  const [isDark, setIsDark] = useState(() => document.documentElement.classList.contains('dark'));
  const [copied, setCopied] = useState(false);
  const [editingName, setEditingName] = useState(false);
  const [nameValue, setNameValue] = useState('');
  const [savingName, setSavingName] = useState(false);
  const [editingDisplayName, setEditingDisplayName] = useState(false);
  const [displayNameValue, setDisplayNameValue] = useState('');
  const [savingDisplayName, setSavingDisplayName] = useState(false);
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [removingMemberId, setRemovingMemberId] = useState<string | null>(null);

  const myMember = members.find(m => m.user_id === user?.id);
  const isOwner = household?.created_by === user?.id;
  const removingMember = members.find(m => m.user_id === removingMemberId);

  useEffect(() => {
    const saved = localStorage.getItem('frigo-dark-mode');
    const next = saved === 'true';
    setIsDark(next);
    document.documentElement.classList.toggle('dark', next);
  }, []);

  const toggleDark = () => {
    const next = !isDark;
    setIsDark(next);
    document.documentElement.classList.toggle('dark', next);
    localStorage.setItem('frigo-dark-mode', next ? 'true' : 'false');
  };

  const handleAvatarUpload = async (file: File) => {
    if (!user || !household) return;
    setAvatarUploading(true);
    try {
      const ext = file.name.split('.').pop() ?? 'jpg';
      const path = `${household.id}/avatar_${user.id}.${ext}`;
      const { error: uploadError } = await supabase.storage
        .from('product-images')
        .upload(path, file, { upsert: true });
      if (uploadError) throw uploadError;

      const { data: { publicUrl } } = supabase.storage.from('product-images').getPublicUrl(path);
      const { error: updateError } = await supabase
        .from('household_members')
        .update({ avatar_url: publicUrl })
        .eq('user_id', user.id)
        .eq('household_id', household.id);
      if (updateError) throw updateError;

      await refreshHousehold();
      toast.success('Avatar mis à jour');
    } catch {
      toast.error("Impossible de mettre à jour l'avatar");
    } finally {
      setAvatarUploading(false);
    }
  };

  const saveDisplayName = async () => {
    const trimmed = displayNameValue.trim();
    if (!trimmed || trimmed === displayName || !user || !household) {
      setEditingDisplayName(false);
      return;
    }

    setSavingDisplayName(true);
    const { error } = await supabase
      .from('household_members')
      .update({ display_name: trimmed })
      .eq('user_id', user.id)
      .eq('household_id', household.id);

    if (error) {
      toast.error('Impossible de modifier le nom');
    } else {
      await refreshHousehold();
      setEditingDisplayName(false);
      toast.success('Profil mis à jour');
    }
    setSavingDisplayName(false);
  };

  const startEditHousehold = () => {
    setNameValue(household?.name ?? '');
    setEditingName(true);
  };

  const saveHouseholdName = async () => {
    const trimmed = nameValue.trim();
    if (!household || !trimmed || trimmed === household.name) {
      setEditingName(false);
      return;
    }

    setSavingName(true);
    const { error } = await supabase
      .from('households')
      .update({ name: trimmed })
      .eq('id', household.id);

    if (error) {
      toast.error('Impossible de renommer le foyer');
    } else {
      await refreshHousehold();
      setEditingName(false);
      toast.success('Foyer renommé');
    }
    setSavingName(false);
  };

  const copyCode = async () => {
    if (!household) return;
    await navigator.clipboard.writeText(household.invite_code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const shareCode = async () => {
    if (!household) return;
    const text = `Rejoins mon frigo sur Fresh Fridge ! Code d'invitation : ${household.invite_code}`;
    if (navigator.share) {
      await navigator.share({ title: 'Fresh Fridge', text });
    } else {
      await copyCode();
    }
  };

  const confirmRemoveMember = async () => {
    if (!household || !removingMemberId) return;
    const { error } = await supabase
      .from('household_members')
      .delete()
      .eq('user_id', removingMemberId)
      .eq('household_id', household.id);

    if (error) {
      toast.error('Impossible de retirer ce membre');
    } else {
      await refreshHousehold();
      toast.success('Membre retiré du foyer');
    }
    setRemovingMemberId(null);
  };

  return (
    <PageTransition>
      <div className="min-h-screen bg-background pb-10">
        <div className={`sticky top-0 z-20 bg-background/85 backdrop-blur-lg border-b border-border ${isMobile ? 'pt-10' : 'pt-0'}`}>
          <div className="max-w-5xl mx-auto px-5 py-4 flex items-center gap-3">
            {isMobile && (
              <button
                onClick={() => navigate(-1)}
                className="p-2 rounded-full hover:bg-muted transition-colors shrink-0"
              >
                <ArrowLeft className="w-5 h-5 text-foreground" />
              </button>
            )}

            {myMember?.avatar_url ? (
              <img
                src={myMember.avatar_url}
                alt={displayName}
                className="w-11 h-11 rounded-2xl object-cover shadow-sm shrink-0"
              />
            ) : (
              <div className="w-11 h-11 rounded-2xl bg-primary flex items-center justify-center shadow-sm shrink-0">
                <span className="text-lg font-extrabold text-primary-foreground">
                  {displayName.charAt(0).toUpperCase()}
                </span>
              </div>
            )}

            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <SettingsIcon className="w-4 h-4 text-primary shrink-0" />
                <h1 className="text-xl font-extrabold text-foreground truncate">Paramètres</h1>
              </div>
              <p className="text-xs font-semibold text-muted-foreground truncate">
                {displayName} · {household?.name}
              </p>
            </div>
          </div>
        </div>

        <main className="max-w-5xl mx-auto px-5 py-5 grid gap-4 lg:grid-cols-2">
          <Section title="Profil" icon={User}>
            <div className="flex items-center gap-4">
              <label className="relative shrink-0 cursor-pointer">
                <input
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={e => {
                    const file = e.target.files?.[0];
                    if (file) handleAvatarUpload(file);
                    e.target.value = '';
                  }}
                />
                {myMember?.avatar_url ? (
                  <img
                    src={myMember.avatar_url}
                    alt={displayName}
                    className="w-16 h-16 rounded-2xl object-cover shadow-sm"
                  />
                ) : (
                  <div className="w-16 h-16 rounded-2xl bg-primary flex items-center justify-center shadow-sm">
                    <span className="text-2xl font-extrabold text-primary-foreground">
                      {displayName.charAt(0).toUpperCase()}
                    </span>
                  </div>
                )}
                <div className={`absolute -bottom-1 -right-1 w-6 h-6 rounded-full bg-card border-2 border-background flex items-center justify-center shadow-sm ${avatarUploading ? 'animate-pulse' : ''}`}>
                  <Camera className="w-3 h-3 text-muted-foreground" />
                </div>
              </label>

              <div className="min-w-0 flex-1 space-y-2">
                {editingDisplayName ? (
                  <div className="flex items-center gap-2">
                    <input
                      value={displayNameValue}
                      onChange={e => setDisplayNameValue(e.target.value.slice(0, 30))}
                      onKeyDown={e => {
                        if (e.key === 'Enter') saveDisplayName();
                        if (e.key === 'Escape') setEditingDisplayName(false);
                      }}
                      autoFocus
                      maxLength={30}
                      className="flex-1 min-w-0 text-sm font-bold text-foreground bg-muted rounded-lg px-2 py-1.5 focus:outline-none focus:ring-2 focus:ring-primary/30"
                    />
                    <button
                      onClick={saveDisplayName}
                      disabled={savingDisplayName || !displayNameValue.trim()}
                      className="p-2 rounded-lg bg-primary/10 text-primary hover:bg-primary/20 transition-colors disabled:opacity-40 shrink-0"
                    >
                      <Check className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => setEditingDisplayName(false)}
                      className="p-2 rounded-lg hover:bg-muted transition-colors text-muted-foreground shrink-0"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <p className="font-extrabold text-foreground truncate">{displayName}</p>
                    <button
                      onClick={() => {
                        setDisplayNameValue(displayName);
                        setEditingDisplayName(true);
                      }}
                      className="p-1.5 rounded-lg hover:bg-muted transition-colors text-muted-foreground shrink-0"
                    >
                      <Pencil className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}
                <p className="text-xs text-muted-foreground truncate">{user?.email ?? 'Email non disponible'}</p>
              </div>
            </div>
          </Section>

          <Section title="Apparence" icon={isDark ? Moon : Sun}>
            <div className="flex items-center justify-between gap-4 rounded-xl bg-muted/60 px-4 py-3">
              <div>
                <p className="text-sm font-bold text-foreground">{isDark ? 'Mode sombre' : 'Mode clair'}</p>
                <p className="text-xs text-muted-foreground">Préférence conservée sur cet appareil.</p>
              </div>
              <Switch checked={isDark} onCheckedChange={toggleDark} aria-label="Basculer le theme" />
            </div>
          </Section>

          <Section title="Notifications" icon={BellRing}>
            <div className="space-y-4">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <p className="text-sm font-bold text-foreground">Notifications push</p>
                  <p className="text-xs text-muted-foreground">
                    Alertes pour les produits bientôt expirés.
                  </p>
                </div>
                {isSupported && permission === 'granted' && (
                  <Switch checked={enabled} onCheckedChange={setEnabled} aria-label="Notifications push" />
                )}
              </div>

              {!isSupported ? (
                <p className="text-xs text-muted-foreground">Non supporté sur cet appareil ou navigateur.</p>
              ) : permission === 'denied' ? (
                <div className="flex items-start gap-2 rounded-xl bg-destructive/10 border border-destructive/20 px-3 py-2.5">
                  <BellOff className="w-4 h-4 text-destructive shrink-0 mt-0.5" />
                  <p className="text-xs text-muted-foreground">
                    Notifications bloquées. Autorisez-les dans les paramètres du navigateur.
                  </p>
                </div>
              ) : permission === 'default' ? (
                <button
                  onClick={requestPermission}
                  className="w-full flex items-center justify-center gap-2 bg-primary text-primary-foreground py-3 rounded-xl text-sm font-bold hover:bg-primary/90 transition-colors"
                >
                  <Bell className="w-4 h-4" />
                  Activer les notifications
                </button>
              ) : (
                <div>
                  <p className="text-xs text-muted-foreground font-semibold mb-2">Rappel avant expiration</p>
                  <div className="grid grid-cols-3 gap-2">
                    {DAY_OPTIONS.map(option => (
                      <button
                        key={option}
                        onClick={() => setDays(option)}
                        disabled={!enabled}
                        className={`py-2.5 rounded-xl text-xs font-bold transition-colors disabled:opacity-40 ${
                          days === option
                            ? 'bg-primary text-primary-foreground'
                            : 'bg-muted text-muted-foreground hover:bg-muted/80'
                        }`}
                      >
                        {option === 1 ? '1 jour' : `${option} jours`}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </Section>

          <Section title="Foyer" icon={Home}>
            <div className="space-y-4">
              {household && (
                <div className="flex items-center gap-3 rounded-xl bg-muted/60 px-4 py-3">
                  <div className="w-10 h-10 bg-primary/10 rounded-xl flex items-center justify-center shrink-0">
                    <Home className="w-4 h-4 text-primary" />
                  </div>
                  {editingName ? (
                    <div className="flex-1 flex items-center gap-2 min-w-0">
                      <input
                        value={nameValue}
                        onChange={e => setNameValue(e.target.value.slice(0, 40))}
                        onKeyDown={e => {
                          if (e.key === 'Enter') saveHouseholdName();
                          if (e.key === 'Escape') setEditingName(false);
                        }}
                        autoFocus
                        maxLength={40}
                        className="flex-1 min-w-0 text-sm font-bold text-foreground bg-background rounded-lg px-2 py-1.5 focus:outline-none focus:ring-2 focus:ring-primary/30"
                      />
                      <button
                        onClick={saveHouseholdName}
                        disabled={savingName || !nameValue.trim()}
                        className="p-2 rounded-lg bg-primary/10 text-primary hover:bg-primary/20 transition-colors disabled:opacity-40 shrink-0"
                      >
                        <Check className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => setEditingName(false)}
                        className="p-2 rounded-lg hover:bg-muted transition-colors text-muted-foreground shrink-0"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ) : (
                    <>
                      <div className="flex-1 min-w-0">
                        <p className="font-bold text-sm text-foreground truncate">{household.name}</p>
                        <p className="text-xs text-muted-foreground">{members.length} membre{members.length > 1 ? 's' : ''}</p>
                      </div>
                      {isOwner && (
                        <button
                          onClick={startEditHousehold}
                          className="p-2 rounded-xl hover:bg-background transition-colors text-muted-foreground shrink-0"
                        >
                          <Pencil className="w-4 h-4" />
                        </button>
                      )}
                    </>
                  )}
                </div>
              )}

              {household && (
                <div>
                  <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-2">
                    Code d'invitation
                  </p>
                  <div className="flex items-center gap-2">
                    <span className="flex-1 text-xl sm:text-2xl font-extrabold text-foreground font-mono tracking-widest text-center bg-muted rounded-xl py-3 min-w-0">
                      {household.invite_code}
                    </span>
                    <button
                      onClick={copyCode}
                      title="Copier"
                      className={`p-3 rounded-xl transition-colors shrink-0 ${
                        copied ? 'bg-success/10 text-success' : 'bg-muted hover:bg-muted/80 text-muted-foreground'
                      }`}
                    >
                      {copied ? <Check className="w-5 h-5" /> : <Copy className="w-5 h-5" />}
                    </button>
                    <button
                      onClick={shareCode}
                      title="Partager"
                      className="p-3 rounded-xl bg-muted hover:bg-muted/80 text-muted-foreground transition-colors shrink-0"
                    >
                      <Share2 className="w-5 h-5" />
                    </button>
                  </div>
                </div>
              )}

              <div>
                <div className="flex items-center gap-2 mb-2">
                  <Users className="w-3.5 h-3.5 text-muted-foreground" />
                  <p className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Membres</p>
                </div>
                <div className="space-y-2">
                  {members.map(member => (
                    <div
                      key={member.user_id}
                      className="flex items-center gap-3 rounded-xl bg-muted/60 px-3 py-2.5"
                    >
                      {member.avatar_url ? (
                        <img
                          src={member.avatar_url}
                          alt={member.display_name}
                          className="w-9 h-9 rounded-full object-cover shrink-0"
                        />
                      ) : (
                        <div className="w-9 h-9 bg-primary/10 rounded-full flex items-center justify-center shrink-0">
                          <span className="text-sm font-extrabold text-primary">
                            {member.display_name.charAt(0).toUpperCase()}
                          </span>
                        </div>
                      )}
                      <p className="text-sm font-semibold text-foreground flex-1 min-w-0 truncate">
                        {member.display_name}
                        {member.user_id === user?.id && (
                          <span className="ml-2 text-[10px] font-bold text-muted-foreground bg-background px-1.5 py-0.5 rounded-full">
                            Moi
                          </span>
                        )}
                      </p>
                      {isOwner && member.user_id !== user?.id && (
                        <button
                          onClick={() => setRemovingMemberId(member.user_id)}
                          title="Retirer du foyer"
                          className="p-2 rounded-xl text-muted-foreground hover:bg-destructive/10 hover:text-destructive transition-colors shrink-0"
                        >
                          <UserMinus className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </Section>

          <Section title="Application" icon={Info} className="lg:col-span-2">
            <div className="grid gap-3 sm:grid-cols-2">
              <button
                onClick={() => navigate('/credits')}
                className="flex items-center justify-between gap-3 rounded-xl bg-muted/60 px-4 py-3 text-left hover:bg-muted transition-colors"
              >
                <div>
                  <p className="text-sm font-bold text-foreground">Crédits</p>
                  <p className="text-xs text-muted-foreground">Attributions et bibliothèques utilisées.</p>
                </div>
                <Info className="w-4 h-4 text-muted-foreground shrink-0" />
              </button>

              <button
                onClick={signOut}
                className="flex items-center justify-center gap-2 rounded-xl bg-destructive/10 border border-destructive/20 px-4 py-3 text-sm font-bold text-destructive hover:bg-destructive/20 transition-colors"
              >
                <LogOut className="w-4 h-4" />
                Se déconnecter
              </button>
            </div>
          </Section>
        </main>

        <AlertDialog open={removingMemberId !== null} onOpenChange={open => !open && setRemovingMemberId(null)}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Retirer ce membre ?</AlertDialogTitle>
              <AlertDialogDescription>
                {removingMember?.display_name} sera retiré du foyer. Il pourra rejoindre à nouveau avec un code d'invitation.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Annuler</AlertDialogCancel>
              <AlertDialogAction
                onClick={confirmRemoveMember}
                className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              >
                <UserMinus className="w-4 h-4 mr-1.5" />
                Retirer
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </PageTransition>
  );
}
