import { useEffect, useState } from 'react';
import DashboardLayout, { pharmacyNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { Card, Button, Input, Spinner, EmptyState, Alert, Badge } from '../../components/ui';
import { Package, Plus, Search, ShoppingCart, ArrowUpCircle } from 'lucide-react';

export default function PharmacyInventory() {
  const [pharmacyId, setPharmacyId] = useState('');
  const [productIds, setProductIds] = useState('');
  const [stock, setStock] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');

  const load = async () => {
    const ids = productIds.split(',').map((s) => s.trim()).filter(Boolean);
    if (!pharmacyId || ids.length === 0) { setError('Enter a pharmacy ID and at least one product ID'); return; }
    setError(''); setLoading(true);
    try {
      const r = await api.post('/inventory/stock/bulk', { pharmacyId, productIds: ids });
      setStock(r || []);
    } catch (e) { setError(e.message); setStock([]); }
    setLoading(false);
  };

  const updateStock = async (s, key, value) => {
    const next = stock.map((x) => (x.productId === s.productId ? { ...x, [key]: value } : x));
    setStock(next);
  };

  const save = async (s) => {
    setOk(''); setError('');
    try {
      await api.put('/inventory/stock', {
        productId: s.productId,
        pharmacyId,
        quantityAvailable: s.quantityAvailable,
        minStockLevel: s.minStockLevel ?? 0,
        maxStockLevel: s.maxStockLevel ?? 1000,
        active: s.active !== false,
      });
      setOk(`Saved ${s.productId.slice(0, 8)}…`);
    } catch (e) { setError(e.message); }
  };

  const suggestedQty = (s) => Math.max(0, (s.maxStockLevel ?? 1000) - s.availableQuantity);

  const restockAll = async () => {
    setOk(''); setError('');
    try {
      for (const s of stock) {
        if (s.availableQuantity < (s.maxStockLevel ?? 1000)) {
          await api.put('/inventory/stock', {
            productId: s.productId,
            pharmacyId,
            quantityAvailable: s.quantityAvailable + suggestedQty(s),
            minStockLevel: s.minStockLevel ?? 0,
            maxStockLevel: s.maxStockLevel ?? 1000,
            active: s.active !== false,
          });
        }
      }
      setOk('All items restocked to max levels');
      load();
    } catch (e) { setError(e.message); }
  };

  const lowStock = stock.filter((s) => s.availableQuantity <= s.minStockLevel);

  return (
    <DashboardLayout title="Inventory" subtitle="Store operations" navItems={pharmacyNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {ok && <Alert type="success" className="mb-4" onClose={() => setOk('')}>{ok}</Alert>}

      <Card className="mb-6 p-5">
        <h2 className="font-bold text-slate-900">Query stock levels</h2>
        <div className="mt-3 flex flex-wrap items-end gap-3">
          <Input label="Pharmacy ID" value={pharmacyId} onChange={(e) => setPharmacyId(e.target.value)} placeholder="UUID" className="w-72" />
          <Input label="Product IDs (comma-separated)" value={productIds} onChange={(e) => setProductIds(e.target.value)} placeholder="uuid1, uuid2, …" className="flex-1 min-w-64" />
          <Button onClick={load} disabled={loading}><Search className="mr-1 h-4 w-4" /> {loading ? 'Loading…' : 'Load'}</Button>
        </div>
      </Card>

      {lowStock.length > 0 && (
        <Card className="mb-6 border-amber-200 bg-amber-50 p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 className="flex items-center gap-2 font-bold text-amber-900"><ShoppingCart className="h-5 w-5" /> Restock suggestions</h3>
              <p className="mt-1 text-sm text-amber-700">{lowStock.length} item(s) at or below minimum stock level. Suggested order quantities below.</p>
            </div>
            <Button size="sm" onClick={restockAll}><ArrowUpCircle className="mr-1 h-4 w-4" /> Restock all to max</Button>
          </div>
        </Card>
      )}

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : stock.length === 0 ? (
        <EmptyState icon={Package} title="No stock loaded" subtitle="Enter a pharmacy and product IDs to view and manage stock levels." />
      ) : (
        <div className="space-y-3">
          {stock.map((s) => {
            const isLow = s.availableQuantity <= s.minStockLevel;
            const need = suggestedQty(s);
            return (
              <Card key={s.productId} className={`p-4 ${isLow ? 'border-amber-300' : ''}`}>
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="font-mono text-sm font-semibold text-slate-800">{s.productId}</p>
                    <p className="mt-1 text-xs text-slate-500">Last updated {new Date(s.updatedAt).toLocaleString('en-IN')}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    {isLow && <Badge color="amber">Low stock · order {need} more</Badge>}
                    <Badge color={s.availableQuantity > s.minStockLevel ? 'green' : 'red'}>
                      {s.availableQuantity > s.minStockLevel ? 'In stock' : 'Low stock'}
                    </Badge>
                  </div>
                </div>
                <div className="mt-4 grid gap-3 sm:grid-cols-5">
                  <Input label="On-hand" type="number" value={s.quantityAvailable} onChange={(e) => updateStock(s, 'quantityAvailable', Number(e.target.value))} />
                  <Input label="Reserved" type="number" value={s.reservedQuantity} onChange={(e) => updateStock(s, 'reservedQuantity', Number(e.target.value))} disabled />
                  <Input label="Min level" type="number" value={s.minStockLevel} onChange={(e) => updateStock(s, 'minStockLevel', Number(e.target.value))} />
                  <Input label="Max level" type="number" value={s.maxStockLevel} onChange={(e) => updateStock(s, 'maxStockLevel', Number(e.target.value))} />
                  <div className="flex items-end">
                    {need > 0 && (
                      <Button size="sm" variant="secondary" className="w-full" onClick={() => updateStock(s, 'quantityAvailable', s.quantityAvailable + need)}>
                        <Plus className="mr-1 h-3.5 w-3.5" /> Add {need}
                      </Button>
                    )}
                  </div>
                </div>
                <div className="mt-3 flex justify-end">
                  <Button size="sm" onClick={() => save(s)}><Plus className="mr-1 h-3.5 w-3.5" /> Save</Button>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </DashboardLayout>
  );
}
