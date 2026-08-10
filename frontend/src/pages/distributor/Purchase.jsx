import { useEffect, useState } from 'react';
import DashboardLayout, { distributorNav } from '../../components/DashboardLayout';
import { api, fmtINR } from '../../lib/api';
import { Card, Button, Input, Spinner, EmptyState, Alert, Badge } from '../../components/ui';
import { Boxes, ShoppingCart, Plus, Minus } from 'lucide-react';

export default function DistributorPurchase() {
  const [distributors, setDistributors] = useState([]);
  const [selected, setSelected] = useState(null);
  const [catalog, setCatalog] = useState([]);
  const [cart, setCart] = useState({});
  const [loading, setLoading] = useState(true);
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');

  useEffect(() => {
    api.get('/distributors')
      .then((d) => setDistributors(d || []))
      .catch(() => setDistributors([]))
      .finally(() => setLoading(false));
  }, []);

  const selectDist = async (d) => {
    setSelected(d);
    setCatalog([]);
    try { setCatalog(await api.get(`/distributors/${d.id}/catalog`)); } catch { setCatalog([]); }
  };

  const add = (item) => {
    setCart((c) => ({ ...c, [item.id]: (c[item.id] || 0) + 1 }));
  };
  const remove = (item) => {
    setCart((c) => {
      const q = (c[item.id] || 0) - 1;
      const next = { ...c };
      if (q <= 0) delete next[item.id]; else next[item.id] = q;
      return next;
    });
  };

  const total = Object.entries(cart).reduce((s, [id, qty]) => {
    const item = catalog.find((c) => c.id === id);
    return s + (item ? Number(item.unitPrice) * qty : 0);
  }, 0);

  const placeOrder = async () => {
    setError(''); setOk('');
    setPlacing(true);
    try {
      const items = Object.entries(cart).map(([id, qty]) => {
        const item = catalog.find((c) => c.id === id);
        return { productId: item.productId, quantity: qty };
      });
      await api.post('/distributor-orders', { distributorId: selected.id, items });
      setOk('Wholesale order placed! Track it under Orders > Placed.');
      setCart({});
    } catch (e) { setError(e.message); }
    setPlacing(false);
  };

  return (
    <DashboardLayout title="Buy Stock" subtitle="Wholesale supply" navItems={distributorNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {ok && <Alert type="success" className="mb-4" onClose={() => setOk('')}>{ok}</Alert>}

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-1">
          <h3 className="mb-3 font-bold text-slate-900">Select a distributor</h3>
          {loading ? (
            <div className="flex justify-center py-16"><Spinner className="h-8 w-8" /></div>
          ) : distributors.length === 0 ? (
            <EmptyState icon={Boxes} title="No distributors yet" subtitle="Other shops will appear here once they register." />
          ) : (
            <div className="space-y-2">
              {distributors.map((d) => (
                <button
                  key={d.id}
                  onClick={() => selectDist(d)}
                  className={`w-full rounded-xl border p-4 text-left transition ${selected?.id === d.id ? 'border-brand-400 bg-brand-50' : 'border-slate-200 bg-white hover:border-brand-200'}`}
                >
                  <p className="font-semibold text-slate-900">{d.shopName}</p>
                  <p className="text-xs text-slate-500">{d.city}</p>
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="lg:col-span-2">
          <h3 className="mb-3 font-bold text-slate-900">{selected ? `${selected.shopName} catalog` : 'Wholesale catalog'}</h3>
          {!selected ? (
            <EmptyState title="Pick a distributor" subtitle="Choose a supplier on the left to browse their wholesale prices." />
          ) : catalog.length === 0 ? (
            <EmptyState icon={Boxes} title="Empty catalog" subtitle="This distributor hasn't listed wholesale items yet." />
          ) : (
            <div className="space-y-2">
              {catalog.map((item) => (
                <Card key={item.id} className="flex items-center justify-between p-4">
                  <div>
                    <p className="font-semibold text-slate-900">{item.productName}</p>
                    <p className="text-xs text-slate-500">
                      {fmtINR(item.unitPrice)} / pack of {item.packSize}
                      <span className={`ml-2 ${item.stockQty > 0 ? 'font-semibold text-emerald-600' : 'font-semibold text-rose-600'}`}>{item.stockQty} in stock</span>
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {cart[item.id] ? (
                      <>
                        <button onClick={() => remove(item)} className="rounded-lg bg-slate-100 p-2 text-slate-600 hover:bg-slate-200"><Minus className="h-4 w-4" /></button>
                        <Badge color="brand">{cart[item.id]}</Badge>
                        <button onClick={() => add(item)} className="rounded-lg bg-slate-100 p-2 text-slate-600 hover:bg-slate-200"><Plus className="h-4 w-4" /></button>
                      </>
                    ) : (
                      <Button size="sm" variant="secondary" disabled={item.stockQty <= 0} onClick={() => add(item)}>Add</Button>
                    )}
                  </div>
                </Card>
              ))}

              {Object.keys(cart).length > 0 && (
                <Card className="flex items-center justify-between bg-brand-50 p-4">
                  <p className="font-bold text-slate-900">Total: {fmtINR(total)}</p>
                  <Button loading={placing} onClick={placeOrder}><ShoppingCart className="mr-1 h-4 w-4" /> Place wholesale order</Button>
                </Card>
              )}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
