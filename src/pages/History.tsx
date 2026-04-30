import { useNavigate } from 'react-router-dom';
import { ArrowLeft, PackageOpen, UtensilsCrossed, Trash2, Inbox } from 'lucide-react';
import { useProducts } from '@/hooks/useProducts';
import { ProductCard } from '@/components/ProductCard';
import { PageTransition } from '@/components/PageTransition';

const sections = [
  { key: 'opened' as const, label: 'Ouverts', icon: PackageOpen, color: 'text-primary' },
  { key: 'consumed' as const, label: 'Consommés', icon: UtensilsCrossed, color: 'text-success' },
  { key: 'thrown' as const, label: 'Jetés', icon: Trash2, color: 'text-destructive' },
];

const History = () => {
  const navigate = useNavigate();
  const { products, setProductStatus } = useProducts();

  const grouped = {
    opened: products.filter(p => p.status === 'opened'),
    consumed: products.filter(p => p.status === 'consumed'),
    thrown: products.filter(p => p.status === 'thrown'),
  };

  const total = grouped.opened.length + grouped.consumed.length + grouped.thrown.length;

  return (
    <PageTransition>
      <div className="min-h-screen bg-background pb-8">
        <div className="sticky top-0 z-30 bg-background/80 backdrop-blur-lg border-b border-border px-4 py-3 flex items-center gap-3">
          <button onClick={() => navigate(-1)} className="p-2 rounded-xl hover:bg-muted transition-colors">
            <ArrowLeft className="w-5 h-5 text-foreground" />
          </button>
          <h1 className="text-lg font-bold text-foreground">Historique</h1>
        </div>

        {total === 0 ? (
          <div className="flex flex-col items-center justify-center mt-32 text-muted-foreground gap-3">
            <Inbox className="w-12 h-12" />
            <p className="text-sm font-medium">Aucun produit dans l'historique</p>
          </div>
        ) : (
          <div className="px-4 mt-4 space-y-6">
            {sections.map(({ key, label, icon: Icon, color }) => {
              const items = grouped[key];
              if (items.length === 0) return null;
              return (
                <div key={key}>
                  <div className="flex items-center gap-2 mb-3">
                    <Icon className={`w-5 h-5 ${color}`} />
                    <h2 className="text-sm font-bold text-foreground">{label}</h2>
                    <span className="text-xs text-muted-foreground">({items.length})</span>
                  </div>
                  <div className="space-y-3">
                    {items.map(product => (
                      <ProductCard key={product.id} product={product} onSetStatus={setProductStatus} />
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </PageTransition>
  );
};

export default History;
