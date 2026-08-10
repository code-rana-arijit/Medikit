import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api, fmtINR } from '../lib/api';
import { Spinner, Button, Badge, Alert, EmptyState } from '../components/ui';
import ProductCard from '../components/ProductCard';
import { useCart } from '../lib/cart';
import { useAuth } from '../lib/auth';
import { Pill, Minus, Plus, ShieldCheck, Truck, RefreshCw, AlertTriangle, HeartPulse } from 'lucide-react';

export default function ProductDetail() {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [alternatives, setAlternatives] = useState([]);
  const [qty, setQty] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [interaction, setInteraction] = useState(null);
  const { addItem } = useCart();
  const { user } = useAuth();

  useEffect(() => {
    setLoading(true);
    api.get(`/products/${id}`)
      .then((p) => {
        setProduct(p);
        setError('');
        return api.get(`/products/${id}/alternatives`).then((a) => setAlternatives(a.content || [])).catch(() => setAlternatives([]));
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="flex justify-center py-24"><Spinner className="h-8 w-8" /></div>;
  if (error || !product) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16">
        <EmptyState icon={Pill} title="Product not found" subtitle={error || 'We could not load this product.'} />
      </div>
    );
  }

  const checkInteraction = async () => {
    try {
      const res = await api.post('/health/interactions/check', { drugs: [product.saltComposition || product.name] });
      setInteraction(res);
    } catch {
      setInteraction({ message: 'Health check unavailable right now.' });
    }
  };

  const onAdd = async () => {
    if (!user) { window.location.href = '/login'; return; }
    try {
      await addItem({
        productId: product.id,
        pharmacyId: product.pharmacyId,
        productName: product.name,
        unitPrice: product.sellingPrice,
        mrp: product.mrp,
        quantity: qty,
        imageUrl: product.imageUrl,
        prescriptionRequired: product.prescriptionRequired,
      });
      setError('');
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <nav className="mb-6 text-sm text-slate-500">
        <Link to="/products" className="hover:text-brand-600">Medicines</Link>
        <span className="mx-2">/</span>
        <span className="text-slate-800">{product.name}</span>
      </nav>

      {error && <Alert type="error" onClose={() => setError('')}>{error}</Alert>}

      <div className="grid gap-8 lg:grid-cols-2">
        <div className="flex items-center justify-center rounded-3xl border border-slate-200 bg-white p-10">
          {product.imageUrl ? (
            <img src={product.imageUrl} alt={product.name} className="max-h-96 object-contain" />
          ) : (
            <span className="flex h-40 w-40 items-center justify-center rounded-3xl bg-brand-100 text-brand-600"><Pill className="h-20 w-20" /></span>
          )}
        </div>

        <div>
          <div className="flex flex-wrap items-center gap-2">
            {product.prescriptionRequired && <Badge color="amber">Prescription required</Badge>}
            {product.discountPercent > 0 && <Badge color="red">{Math.round(product.discountPercent)}% off</Badge>}
            <Badge color="slate">{product.categoryName || 'Medicine'}</Badge>
          </div>
          <h1 className="mt-3 text-3xl font-bold text-slate-900">{product.name}</h1>
          {product.saltComposition && (
            <p className="mt-1 text-sm text-slate-500">Composition: {product.saltComposition}</p>
          )}
          {product.manufacturer && (
            <p className="mt-0.5 text-sm text-slate-500">Manufacturer: {product.manufacturer}</p>
          )}
          {product.packSize && <p className="mt-0.5 text-sm text-slate-500">Pack: {product.packSize} · {product.packaging}</p>}

          <div className="mt-6 flex items-end gap-3">
            <span className="text-4xl font-extrabold text-slate-900">{fmtINR(product.sellingPrice)}</span>
            {product.mrp > product.sellingPrice && (
              <span className="mb-1 text-xl text-slate-400 line-through">{fmtINR(product.mrp)}</span>
            )}
          </div>

          <div className="mt-6 flex flex-wrap items-center gap-4">
            <div className="flex items-center rounded-xl border border-slate-300 bg-white">
              <button className="px-3 py-2.5 text-slate-600 hover:text-brand-600" onClick={() => setQty(Math.max(1, qty - 1))}><Minus className="h-4 w-4" /></button>
              <span className="w-10 text-center font-semibold">{qty}</span>
              <button className="px-3 py-2.5 text-slate-600 hover:text-brand-600" onClick={() => setQty(qty + 1)}><Plus className="h-4 w-4" /></button>
            </div>
            <Button size="lg" onClick={onAdd} className="min-w-48">Add to cart</Button>
            <Link to="/cart"><Button size="lg" variant="secondary">Go to cart</Button></Link>
          </div>

          <div className="mt-6 grid gap-3 sm:grid-cols-3">
            {[
              { icon: Truck, title: 'Express delivery', desc: 'Within 15 min' },
              { icon: ShieldCheck, title: 'Genuine product', desc: 'Licensed pharmacy' },
              { icon: RefreshCw, title: 'Easy returns', desc: 'Within 7 days' },
            ].map(({ icon: Icon, title, desc }) => (
              <div key={title} className="rounded-2xl bg-slate-50 p-3 text-center">
                <Icon className="mx-auto h-5 w-5 text-brand-600" />
                <p className="mt-1 text-sm font-semibold text-slate-800">{title}</p>
                <p className="text-xs text-slate-500">{desc}</p>
              </div>
            ))}
          </div>

          <div className="mt-6 rounded-2xl border border-brand-100 bg-brand-50 p-4">
            <div className="flex items-start gap-3">
              <HeartPulse className="mt-0.5 h-5 w-5 shrink-0 text-brand-600" />
              <div className="flex-1">
                <p className="text-sm font-semibold text-brand-900">AI safety check</p>
                <p className="text-sm text-brand-800">Check this medicine for known drug interactions.</p>
                <Button variant="outline" size="sm" className="mt-2" onClick={checkInteraction}>Check interactions</Button>
                {interaction && (
                  <div className="mt-3 text-sm text-brand-900">
                    {Array.isArray(interaction.interactions) && interaction.interactions.length > 0 ? (
                      interaction.interactions.map((it, i) => (
                        <p key={i} className="flex items-start gap-2"><AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-500" /> {it}</p>
                      ))
                    ) : (
                      <p>{interaction.message || 'No known interactions found for this medicine alone.'}</p>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>

          {product.description && (
            <div className="mt-6">
              <h3 className="font-semibold text-slate-900">Description</h3>
              <p className="mt-1 text-sm leading-relaxed text-slate-600">{product.description}</p>
            </div>
          )}
        </div>
      </div>

      {alternatives.length > 0 && (
        <section className="mt-14">
          <h2 className="mb-4 text-xl font-bold text-slate-900">Alternatives</h2>
          <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
            {alternatives.slice(0, 4).map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
        </section>
      )}
    </div>
  );
}
