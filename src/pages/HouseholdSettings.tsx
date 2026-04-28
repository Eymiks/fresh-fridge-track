import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Copy, Check, LogOut, Home, Pencil, X, Share2, UserMinus, Camera } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { supabase } from '@/integrations/supabase/client';
import { PageTransition } from '@/components/PageTransition';
import { motion } from 'framer-motion';
import { toast } from 'sonner';
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent,
  AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from '@/components/ui/alert-dialog';

export default function HouseholdSettings() {
  const navigate = useNavigate();
  const { household, members, displayName, signOut, user, refreshHousehold } = useAuth();
  const [copied, setCopied] = useState(false);
  const [editingName, setEditingName] = useState(false);
  const [nameValue, setNameValue] = useState('');
  const [saving, setSaving] = useState(false);
  const [removingMemberId, setRemovingMemberId] = useState<string | null>(null);
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [editingDisplayName, setEditingDisplayName] = useState(false);
  const [displayNameValue, setDisplayNameValue] = useState('');
  const [savingDisplayName, setSavingDisplayName] = useState(false);

  const myMember = members.find(m => m.user_id === user?.id);

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
    } catch {
      toast.error("Impossible de mettre à jour l'avatar");
    } finally {
      setAvatarUploading(false);
    }
  };

  const saveDisplayName = async () => {
    const trimmed = displayNameValue.trim();
    if (!trimmed || trimmed === displayName) {
      setEditingDisplayName(false);
      return;
    }
    setSavingDisplayName(true);
    const { error } = await supabase
      .from('household_members')
      .update({ display_name: trimmed })
      .eq('user_id', user!.id)
      .eq('household_id', household!.id);
    if (error) {
      toast.error('Impossible de modifier le nom');
    } else {
      await refreshHousehold();
      setEditingDisplayName(false);
    }
    setSavingDisplayName(false);
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
      await navigator.clipboard.writeText(household.invite_code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const startEdit = () => {
    setNameValue(household?.name ?? '');
    setEditingName(true);
  };

  const cancelEdit = () => setEditingName(false);

  const saveName = async () => {
    const trimmed = nameValue.trim();
    if (!household || !trimmed || trimmed === household.name) {
      setEditingName(false);
      return;
    }
    setSaving(true);
    const { error } = await supabase
      .from('households')
      .update({ name: trimmed })
      .eq('id', household.id);
    if (error) {
      toast.error('Impossible de renommer le foyer');
    } else {
      await refreshHousehold();
      setEditingName(false);
    }
    setSaving(false);
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

  const isOwner = household?.created_by === user?.id;
  const removingMember = members.find(m => m.user_id === removingMemberId);

  return (
    <PageTransition>
      <div className="min-h-screen bg-background pb-10">
        <div className="flex items-center gap-3 px-5 pt-12 pb-6">
          <button
            onClick={() => navigate('/')}
            className="p-2 rounded-full bg-card border border-border hover:bg-muted transition-colors"
          >
            <ArrowLeft className="w-5 h-5 text-foreground" />
          </button>
          <h1 className="text-xl font-extrabold text-foreground">Mon foyer</h1>
        </div>

        <div className="px-5 space-y-5">
          {/* Mon profil */}
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-2">
              Mon profil
            </h3>
            <div className="bg-card border border-border rounded-2xl p-4 flex items-center gap-4">
              <label className="relative shrink-0 cursor-pointer">
                <input
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={e => { const f = e.target.files?.[0]; if (f) handleAvatarUpload(f); e.target.value = ''; }}
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

              <div className="flex-1 min-w-0">
                {editingDisplayName ? (
                  <div className="flex items-center gap-2">
                    <input
                      value={displayNameValue}
                      onChange={e => setDisplayNameValue(e.target.value.slice(0, 30))}
                      onKeyDown={e => { if (e.key === 'Enter') saveDisplayName(); if (e.key === 'Escape') setEditingDisplayName(false); }}
                      autoFocus
                      maxLength={30}
                      className="flex-1 text-sm font-bold text-foreground bg-muted rounded-lg px-2 py-1 focus:outline-none focus:ring-2 focus:ring-primary/30 min-w-0"
                    />
                    <button
                      onClick={saveDisplayName}
                      disabled={savingDisplayName || !displayNameValue.trim()}
                      className="p-1.5 rounded-lg bg-primary/10 text-primary hover:bg-primary/20 transition-colors disabled:opacity-40 shrink-0"
                    >
                      <Check className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => setEditingDisplayName(false)}
                      className="p-1.5 rounded-lg hover:bg-muted transition-colors text-muted-foreground shrink-0"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <p className="font-bold text-sm text-foreground truncate">{displayName}</p>
                    <button
                      onClick={() => { setDisplayNameValue(displayName); setEditingDisplayName(true); }}
                      className="p-1.5 rounded-lg hover:bg-muted transition-colors text-muted-foreground shrink-0"
                    >
                      <Pencil className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}
                <p className="text-xs text-muted-foreground mt-0.5">Touchez l'avatar pour changer la photo</p>
              </div>
            </div>
          </motion.div>

          {/* Household name */}
          {household && (
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              className="flex items-center gap-3 bg-card border border-border rounded-2xl p-4"
            >
              <div className="w-11 h-11 bg-primary/10 rounded-xl flex items-center justify-center shrink-0">
                <Home className="w-5 h-5 text-primary" />
              </div>
              {editingName ? (
                <div className="flex-1 flex items-center gap-2">
                  <input
                    value={nameValue}
                    onChange={e => setNameValue(e.target.value.slice(0, 40))}
                    onKeyDown={e => { if (e.key === 'Enter') saveName(); if (e.key === 'Escape') cancelEdit(); }}
                    autoFocus
                    maxLength={40}
                    className="flex-1 text-sm font-bold text-foreground bg-muted rounded-lg px-2 py-1 focus:outline-none focus:ring-2 focus:ring-primary/30"
                  />
                  <button
                    onClick={saveName}
                    disabled={saving || !nameValue.trim()}
                    className="p-1.5 rounded-lg bg-primary/10 text-primary hover:bg-primary/20 transition-colors disabled:opacity-40"
                  >
                    <Check className="w-4 h-4" />
                  </button>
                  <button
                    onClick={cancelEdit}
                    className="p-1.5 rounded-lg hover:bg-muted transition-colors text-muted-foreground"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              ) : (
                <div className="flex-1">
                  <p className="font-bold text-sm text-foreground">{household.name}</p>
                  <p className="text-xs text-muted-foreground">{members.length} membre{members.length > 1 ? 's' : ''}</p>
                </div>
              )}
              {!editingName && isOwner && (
                <button
                  onClick={startEdit}
                  className="p-2 rounded-xl hover:bg-muted transition-colors text-muted-foreground"
                >
                  <Pencil className="w-4 h-4" />
                </button>
              )}
            </motion.div>
          )}

          {/* Invite code */}
          {household && (
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.06 }}
            >
              <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-2">
                Code d'invitation
              </h3>
              <div className="bg-card border border-border rounded-2xl p-4">
                <p className="text-xs text-muted-foreground mb-3">
                  Partagez ce code pour inviter un proche à rejoindre votre foyer.
                </p>
                <div className="flex items-center gap-2">
                  <span className="flex-1 text-2xl font-extrabold text-foreground font-mono tracking-widest text-center bg-muted rounded-xl py-3">
                    {household.invite_code}
                  </span>
                  <button
                    onClick={copyCode}
                    title="Copier"
                    className={`p-3 rounded-xl transition-colors ${
                      copied ? 'bg-success/10 text-success' : 'bg-muted hover:bg-muted/80 text-muted-foreground'
                    }`}
                  >
                    {copied ? <Check className="w-5 h-5" /> : <Copy className="w-5 h-5" />}
                  </button>
                  <button
                    onClick={shareCode}
                    title="Partager"
                    className="p-3 rounded-xl bg-muted hover:bg-muted/80 text-muted-foreground transition-colors"
                  >
                    <Share2 className="w-5 h-5" />
                  </button>
                </div>
              </div>
            </motion.div>
          )}

          {/* Members list */}
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.12 }}
          >
            <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-2">
              Membres
            </h3>
            <div className="space-y-2">
              {members.map(member => (
                <div
                  key={member.user_id}
                  className="flex items-center gap-3 bg-card border border-border rounded-2xl p-3"
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
                  <p className="text-sm font-semibold text-foreground flex-1">
                    {member.display_name}
                    {member.display_name === displayName && (
                      <span className="ml-2 text-[10px] font-bold text-muted-foreground bg-muted px-1.5 py-0.5 rounded-full">
                        Moi
                      </span>
                    )}
                  </p>
                  {isOwner && member.user_id !== user?.id && (
                    <button
                      onClick={() => setRemovingMemberId(member.user_id)}
                      title="Retirer du foyer"
                      className="p-2 rounded-xl text-muted-foreground hover:bg-destructive/10 hover:text-destructive transition-colors"
                    >
                      <UserMinus className="w-4 h-4" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </motion.div>

          {/* Sign out */}
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.18 }}
            className="pt-2"
          >
            <button
              onClick={signOut}
              className="w-full flex items-center justify-center gap-2 bg-destructive/10 text-destructive border border-destructive/20 rounded-2xl py-3.5 text-sm font-bold hover:bg-destructive/20 transition-colors"
            >
              <LogOut className="w-4 h-4" />
              Se déconnecter
            </button>
          </motion.div>
        </div>

        {/* Remove member confirmation */}
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
