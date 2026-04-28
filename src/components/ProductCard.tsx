import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useIsMobile } from '@/hooks/use-mobile';
import { motion, useMotionValue, useTransform, animate } from 'framer-motion';
import { Trash2, PackageOpen, UtensilsCrossed, MoreVertical, CalendarDays, Pencil, Check, Snowflake } from 'lucide-react';
import { Product, getExpirationStatus, getDaysUntilExpiration, getEffectiveExpirationDate } from '@/types/product';
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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

interface ProductCardProps {
  product: Product;
  onSetStatus: (id: string, status: 'consumed' | 'thrown') => void;
  onUpdateDate?: (id: string, date: string) => void;
  selectionMode?: boolean;
  isSelected?: boolean;
  onLongPress?: (id: string) => void;
  onToggleSelect?: (id: string) => void;
}

const statusColors = {
  fresh: 'bg-success',
  soon: 'bg-warning',
  expired: 'bg-destructive',
};

const statusConfig = {
  fresh: { badge: 'bg-success text-success-foreground' },
  soon: { badge: 'bg-warning text-warning-foreground' },
  expired: { badge: 'bg-destructive text-destructive-foreground' },
};

const nutriColors: Record<string, string> = {
  A: 'bg-green-600',
  B: 'bg-lime-500',
  C: 'bg-yellow-400',
  D: 'bg-orange-400',
  E: 'bg-red-500',
};

const productStatusConfig: Record<string, { icon: typeof PackageOpen; label: string; color: string }> = {
  opened: { icon: PackageOpen, label: 'Ouvert', color: 'text-blue-500 bg-blue-500/10' },
  consumed: { icon: UtensilsCrossed, label: 'Consommé', color: 'text-success bg-success/10' },
  thrown: { icon: Trash2, label: 'Jeté', color: 'text-destructive bg-destructive/10' },
};

export function ProductCard({
  product,
  onSetStatus,
  onUpdateDate,
  selectionMode = false,
  isSelected = false,
  onLongPress,
  onToggleSelect,
}: ProductCardProps) {
  const navigate = useNavigate();
  const isMobile = useIsMobile();
  const [pendingAction, setPendingAction] = useState<'consumed' | 'thrown' | null>(null);
  const [showDateDialog, setShowDateDialog] = useState(false);
  const [pendingDate, setPendingDate] = useState('');

  const longPressTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pointerDownPos = useRef<{ x: number; y: number } | null>(null);

  const effectiveDate = getEffectiveExpirationDate(product);
  const status = getExpirationStatus(effectiveDate);
  const days = getDaysUntilExpiration(effectiveDate);
  const config = statusConfig[status];
  const x = useMotionValue(0);
  const consumedOpacity = useTransform(x, [0, 60, 120], [0, 0.5, 1]);
  const thrownOpacity = useTransform(x, [-120, -60, 0], [1, 0.5, 0]);

  const daysText = days < 0
    ? `${Math.abs(days)}j`
    : days === 0
      ? "Auj."
      : `${days}j`;

  const nutriGrade = (product.nutriScore || '?').toUpperCase();
  const nutriBg = nutriColors[nutriGrade] || 'bg-muted';

  const productStatus = product.status || 'active';
  const isInactive = productStatus === 'consumed' || productStatus === 'thrown';
  const pStatusConfig = productStatusConfig[productStatus];

  const handleDragEnd = (_: any, info: { offset: { x: number } }) => {
    if (!selectionMode) {
      if (info.offset.x > 120) setPendingAction('consumed');
      else if (info.offset.x < -120) setPendingAction('thrown');
    }
    animate(x, 0, { type: 'spring', stiffness: 300, damping: 30 });
  };

  const handlePointerDown = (e: React.PointerEvent) => {
    if (!onLongPress || selectionMode) return;
    pointerDownPos.current = { x: e.clientX, y: e.clientY };
    longPressTimer.current = setTimeout(() => {
      longPressTimer.current = null;
      pointerDownPos.current = null;
      onLongPress(product.id);
    }, 500);
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!longPressTimer.current || !pointerDownPos.current) return;
    const dx = e.clientX - pointerDownPos.current.x;
    const dy = e.clientY - pointerDownPos.current.y;
    if (Math.sqrt(dx * dx + dy * dy) > 8) {
      clearTimeout(longPressTimer.current);
      longPressTimer.current = null;
    }
  };

  const handlePointerUp = () => {
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current);
      longPressTimer.current = null;
    }
    pointerDownPos.current = null;
  };

  const handleCardClick = () => {
    if (selectionMode) {
      onToggleSelect?.(product.id);
    } else {
      navigate(`/product/${product.id}`);
    }
  };

  const openDateDialog = () => {
    setPendingDate(product.expirationDate ?? '');
    setShowDateDialog(true);
  };

  const confirmDateUpdate = () => {
    if (pendingDate) onUpdateDate?.(product.id, pendingDate);
    setShowDateDialog(false);
  };

  return (
    <>
      <div
        className={`relative overflow-hidden rounded-2xl ${isInactive ? 'opacity-50' : ''} ${isSelected ? 'ring-2 ring-primary' : ''}`}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
        onContextMenu={e => { if (onLongPress) e.preventDefault(); }}
      >
        {/* Swipe backgrounds — mobile only, hidden in selection mode */}
        {isMobile && !selectionMode && (
          <>
            <motion.div
              className="absolute inset-0 bg-success flex items-center pl-4 gap-2 rounded-2xl"
              style={{ opacity: consumedOpacity }}
            >
              <UtensilsCrossed className="w-5 h-5 text-success-foreground" />
              <span className="text-xs font-bold text-success-foreground">Consommé</span>
            </motion.div>
            <motion.div
              className="absolute inset-0 bg-destructive flex items-center justify-end pr-4 gap-2 rounded-2xl"
              style={{ opacity: thrownOpacity }}
            >
              <span className="text-xs font-bold text-destructive-foreground">Jeté</span>
              <Trash2 className="w-5 h-5 text-destructive-foreground" />
            </motion.div>
          </>
        )}

        {/* Swipeable card */}
        <motion.div
          drag={isMobile && !selectionMode ? "x" : false}
          dragConstraints={{ left: -150, right: 150 }}
          dragElastic={0.1}
          onDragEnd={handleDragEnd}
          style={{ x }}
          onClick={handleCardClick}
          className="relative flex items-center gap-3 p-3 rounded-2xl bg-card border border-border cursor-pointer active:scale-[0.98] will-change-transform overflow-hidden select-none"
        >
          {/* Status color indicator */}
          <div className={`absolute left-0 top-0 bottom-0 w-1 ${statusColors[status]} rounded-l-2xl`} />

          {/* Selection indicator */}
          {selectionMode && (
            <div className={`ml-1 w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0 transition-colors ${
              isSelected ? 'bg-primary border-primary' : 'border-muted-foreground/40 bg-transparent'
            }`}>
              {isSelected && <Check className="w-3 h-3 text-primary-foreground" strokeWidth={3} />}
            </div>
          )}

          <div className="relative ml-1 shrink-0">
            {product.imageUrl ? (
              <img src={product.imageUrl} alt={product.name} className="w-11 h-11 rounded-xl object-cover" />
            ) : (
              <div className="w-11 h-11 rounded-xl bg-muted flex items-center justify-center text-xl">
                🧊
              </div>
            )}
            {product.frozenUntil && (
              <div className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-blue-500 flex items-center justify-center shadow">
                <Snowflake className="w-2.5 h-2.5 text-white" />
              </div>
            )}
          </div>
          <div className="flex-1 min-w-0">
            <p className="font-bold text-sm text-card-foreground truncate">{product.name}</p>
            {product.brand && (
              <p className="text-xs text-muted-foreground truncate">{product.brand.charAt(0).toUpperCase() + product.brand.slice(1)}</p>
            )}
            {product.addedByName && (
              <p className="text-[10px] text-muted-foreground/70 truncate">Ajouté par {product.addedByName}</p>
            )}
            {pStatusConfig && (
              <div className={`inline-flex items-center gap-1 mt-0.5 px-1.5 py-0.5 rounded-full text-[9px] font-bold ${pStatusConfig.color}`}>
                <pStatusConfig.icon className="w-2.5 h-2.5" />
                {pStatusConfig.label}
              </div>
            )}
          </div>
          <div className="flex items-center gap-1.5 shrink-0">
            <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${config.badge}`}>
              {daysText}
            </span>
            {isMobile ? (
              <>
                <span className={`${nutriBg} text-white text-[10px] font-extrabold w-5 h-5 rounded flex items-center justify-center`}>
                  {nutriGrade}
                </span>
                {!selectionMode && onUpdateDate && (
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <button
                        onClick={e => e.stopPropagation()}
                        className="w-7 h-7 rounded-lg hover:bg-muted flex items-center justify-center transition-colors text-muted-foreground"
                      >
                        <MoreVertical className="w-4 h-4" />
                      </button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" onClick={e => e.stopPropagation()}>
                      <DropdownMenuItem onSelect={openDateDialog}>
                        <CalendarDays className="w-4 h-4 mr-2" />
                        Modifier la date
                      </DropdownMenuItem>
                      <DropdownMenuItem onSelect={() => navigate(`/product/${product.id}`)}>
                        <Pencil className="w-4 h-4 mr-2" />
                        Modifier
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                )}
              </>
            ) : (
              <>
                <button
                  onClick={(e) => { e.stopPropagation(); setPendingAction('consumed'); }}
                  className="w-7 h-7 rounded-lg bg-success/10 hover:bg-success/20 flex items-center justify-center transition-colors"
                  title="Marquer comme consommé"
                >
                  <UtensilsCrossed className="w-3.5 h-3.5 text-success" />
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); setPendingAction('thrown'); }}
                  className="w-7 h-7 rounded-lg bg-destructive/10 hover:bg-destructive/20 flex items-center justify-center transition-colors"
                  title="Marquer comme jeté"
                >
                  <Trash2 className="w-3.5 h-3.5 text-destructive" />
                </button>
                {onUpdateDate && (
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <button
                        onClick={e => e.stopPropagation()}
                        className="w-7 h-7 rounded-lg hover:bg-muted flex items-center justify-center transition-colors text-muted-foreground"
                        title="Plus d'options"
                      >
                        <MoreVertical className="w-4 h-4" />
                      </button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" onClick={e => e.stopPropagation()}>
                      <DropdownMenuItem onSelect={openDateDialog}>
                        <CalendarDays className="w-4 h-4 mr-2" />
                        Modifier la date
                      </DropdownMenuItem>
                      <DropdownMenuItem onSelect={() => navigate(`/product/${product.id}`)}>
                        <Pencil className="w-4 h-4 mr-2" />
                        Modifier
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                )}
              </>
            )}
          </div>
        </motion.div>
      </div>

      {/* Status confirmation dialog */}
      <AlertDialog open={pendingAction !== null} onOpenChange={(open) => !open && setPendingAction(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {pendingAction === 'consumed' ? 'Marquer comme consommé ?' : 'Marquer comme jeté ?'}
            </AlertDialogTitle>
            <AlertDialogDescription>
              « {product.name} » sera déplacé dans l'historique.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Annuler</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => { onSetStatus(product.id, pendingAction!); setPendingAction(null); }}
              className={pendingAction === 'consumed'
                ? 'bg-success text-success-foreground hover:bg-success/90'
                : 'bg-destructive text-destructive-foreground hover:bg-destructive/90'}
            >
              {pendingAction === 'consumed'
                ? <><UtensilsCrossed className="w-4 h-4 mr-1.5" />Consommé</>
                : <><Trash2 className="w-4 h-4 mr-1.5" />Jeté</>}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Date edit dialog */}
      <Dialog open={showDateDialog} onOpenChange={setShowDateDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Modifier la date d'expiration</DialogTitle>
          </DialogHeader>
          <div className="py-2">
            <input
              type="date"
              value={pendingDate}
              onChange={e => setPendingDate(e.target.value)}
              className="w-full border border-border rounded-xl px-3 py-2.5 text-sm bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary/30"
            />
          </div>
          <DialogFooter>
            <button
              onClick={() => setShowDateDialog(false)}
              className="px-4 py-2 rounded-xl text-sm font-semibold text-muted-foreground hover:bg-muted transition-colors"
            >
              Annuler
            </button>
            <button
              onClick={confirmDateUpdate}
              disabled={!pendingDate}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-primary text-primary-foreground text-sm font-bold hover:bg-primary/90 disabled:opacity-40 transition-colors"
            >
              <Check className="w-4 h-4" />
              Confirmer
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
