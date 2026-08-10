import { Link } from 'react-router-dom';
import { Pill, Plus, Check } from 'lucide-react';
import { fmtINR, api } from '../lib/api';
import { Badge, Button, cx } from './ui';
import { useCart } from '../lib/cart';
import { useState } from 'react';
import { useAuth } from '../lib/auth';

export default function ProductCard({ product, horizontal }) {
  const { addItem } = useCart();
  const { user } = useAuth();
  const [added, setAdded] = useState(false);

  const onAdd = async (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!user) {
      window.location.href = '/login';
      return;
    }
    try {
      await addItem({
        productId: product.id,
        pharmacyId: product.pharmacyId,
        productName: product.name,
        unitPrice: product.sellingPrice,
        mrp: product.mrp,
        quantity: 1,
        imageUrl: product.imageUrl,
        prescriptionRequired: product.prescriptionRequired,
      });
      setAdded(true);
      setTimeout(() => setAdded(false), 1200);
    } catch { /* handled upstream */ }
  };

  const img = product.imageUrl || null;

  return (
    <Link
      to={`/products/${product.id}`}
      className={cx(
        'group flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md',
        horizontal && 'sm:flex-row',
      )}
    >
      <div className={cx('relative flex items-center justify-center bg-slate-50', horizontal ? 'h-36 sm:w-40 sm:h-auto' : 'h-44')}>
        {img ? (
          <img src={img} alt={product.name} className="h-full w-full object-cover" />
        ) : (
          <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-100 text-brand-600"><Pill className="h-7 w-7" /></span>
        )}
        {product.discountPercent > 0 && (
          <Badge color="red" className="absolute left-2 top-2">{Math.round(product.discountPercent)}% off</Badge>
        )}
        {product.prescriptionRequired && (
          <Badge color="amber" className="absolute right-2 top-2">Rx</Badge>
        )}
      </div>
      <div className={cx('flex flex-1 flex-col p-4', horizontal && 'sm:p-5')}>
        <p className="text-xs font-medium text-brand-600">{product.categoryName || 'Medicine'}</p>
        <h3 className="mt-1 line-clamp-2 text-sm font-semibold text-slate-900 group-hover:text-brand-700">{product.name}</h3>
        {product.saltComposition && <p className="mt-0.5 line-clamp-1 text-xs text-slate-500">{product.saltComposition}</p>}
        {product.packSize && <p className="mt-1 text-xs text-slate-400">{product.packSize}</p>}
        <div className="mt-auto pt-3">
          <div className="flex items-end gap-1.5">
            <span className="text-lg font-bold text-slate-900">{fmtINR(product.sellingPrice)}</span>
            {product.mrp > product.sellingPrice && (
              <span className="mb-0.5 text-sm text-slate-400 line-through">{fmtINR(product.mrp)}</span>
            )}
          </div>
          <Button
            size="sm"
            className="mt-2 w-full"
            variant={added ? 'outline' : 'primary'}
            onClick={onAdd}
          >
            {added ? <Check className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
            {added ? 'Added' : 'Add to cart'}
          </Button>
        </div>
      </div>
    </Link>
  );
}
