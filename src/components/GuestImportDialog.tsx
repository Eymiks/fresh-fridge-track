import { useEffect, useState } from 'react';
import { Download, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog';
import { useAuth } from '@/contexts/AuthContext';
import { clearGuestProducts, readGuestProducts } from '@/lib/guestProducts';
import { productToDbInsert } from '@/lib/productDb';
import { supabase } from '@/integrations/supabase/client';

export function GuestImportDialog() {
  const { user, household } = useAuth();
  const [open, setOpen] = useState(false);
  const [dismissed, setDismissed] = useState(false);
  const [importing, setImporting] = useState(false);
  const [guestCount, setGuestCount] = useState(0);

  useEffect(() => {
    if (!user || !household || dismissed) return;
    const count = readGuestProducts().length;
    setGuestCount(count);
    setOpen(count > 0);
  }, [dismissed, household, user]);

  const handleImport = async () => {
    if (!user || !household) return;

    const guestProducts = readGuestProducts();
    if (guestProducts.length === 0) {
      setOpen(false);
      return;
    }

    setImporting(true);
    const { error } = await supabase
      .from('products')
      .insert(guestProducts.map(product => productToDbInsert(product, household.id, user.id)));

    if (error) {
      toast.error("Impossible d'importer les produits invités");
      setImporting(false);
      return;
    }

    clearGuestProducts();
    setGuestCount(0);
    setOpen(false);
    toast.success(`${guestProducts.length} produit${guestProducts.length > 1 ? 's' : ''} importé${guestProducts.length > 1 ? 's' : ''}`);
    setImporting(false);
  };

  const handleDiscard = () => {
    clearGuestProducts();
    setGuestCount(0);
    setOpen(false);
    toast.success('Données invitées supprimées');
  };

  const handleLater = () => {
    setDismissed(true);
    setOpen(false);
  };

  return (
    <AlertDialog open={open} onOpenChange={nextOpen => !nextOpen && !importing && handleLater()}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Importer le frigo invité ?</AlertDialogTitle>
          <AlertDialogDescription>
            {guestCount} produit{guestCount > 1 ? 's' : ''} créé{guestCount > 1 ? 's' : ''} en mode invité peu{guestCount > 1 ? 'vent' : 't'} être ajouté{guestCount > 1 ? 's' : ''} à votre foyer.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter className="gap-2 sm:gap-0">
          <AlertDialogCancel onClick={handleLater}>Plus tard</AlertDialogCancel>
          <button
            type="button"
            onClick={handleDiscard}
            disabled={importing}
            className="inline-flex items-center justify-center gap-1.5 rounded-md border border-destructive/20 bg-destructive/10 px-4 py-2 text-sm font-semibold text-destructive transition-colors hover:bg-destructive/20 disabled:opacity-50"
          >
            <Trash2 className="h-4 w-4" />
            Supprimer
          </button>
          <AlertDialogAction onClick={event => { event.preventDefault(); handleImport(); }} disabled={importing}>
            <Download className="h-4 w-4 mr-1.5" />
            {importing ? 'Import...' : 'Importer'}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
