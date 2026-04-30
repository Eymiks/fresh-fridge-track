import { ProductEditorPayload, ProductEditorSheet } from '@/components/ProductEditorSheet';

interface AddProductSheetProps {
  open: boolean;
  mode: 'single' | 'multi';
  onClose: () => void;
  onAdd: (product: {
    name: string;
    barcode?: string;
    expirationDate: string;
    imageUrl?: string;
    brand?: string;
    nutriScore?: string;
    category?: string;
    subcategory?: string;
    quantity?: string;
    novaGroup?: number;
    ecoScore?: string;
    allergens?: string;
    ingredients?: string;
    notes?: string;
    nutritionData?: string;
  }) => void | Promise<void>;
}

function toCreatePayload(product: ProductEditorPayload) {
  return {
    name: product.name,
    barcode: product.barcode || undefined,
    expirationDate: product.expirationDate,
    imageUrl: product.imageUrl || undefined,
    brand: product.brand || undefined,
    nutriScore: product.nutriScore || undefined,
    category: product.category || undefined,
    subcategory: product.subcategory || undefined,
    quantity: product.quantity || undefined,
    novaGroup: product.novaGroup ?? undefined,
    ecoScore: product.ecoScore || undefined,
    allergens: product.allergens || undefined,
    ingredients: product.ingredients || undefined,
    notes: product.notes || undefined,
    nutritionData: product.nutritionData || undefined,
  };
}

export function AddProductSheet({ open, mode, onClose, onAdd }: AddProductSheetProps) {
  return (
    <ProductEditorSheet
      open={open}
      mode="create"
      variant={mode}
      onClose={onClose}
      onSubmit={(product) => onAdd(toCreatePayload(product))}
    />
  );
}
