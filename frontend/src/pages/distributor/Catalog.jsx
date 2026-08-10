import { useEffect, useState } from 'react';
import DashboardLayout, { distributorNav } from '../../components/DashboardLayout';
import { api, fmtINR } from '../../lib/api';
import { Button, Card, Input, Alert, Spinner, Badge, EmptyState } from '../../components/ui';
import { Package, Plus, Pencil, Trash2 } from 'lucide-react';

const emptyItem = { productId: '', productName: '', unitPrice: '', packSize: 1, stockQty: 0 };

export default function DistributorCatalog() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyItem);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');

  const load = async () => {
    try {
      setItems(await api.get('/distributors/me/catalog'));
    } catch { setItems([]); }
    setLoading(false);
  };

  useEffect(() => { load(); }, []);

  const startAdd = () => { setEditing(null); setForm(emptyItem); };
  const startEdit = (item) => {
    setEditing(item.id);
    setForm({ productId: item.productId, productName: item.productName, unitPrice: item.unitPrice, packSize: item.packSize, stockQty: item.stockQty });
  };

  const save = async () => {
    setError(''); setOk('');
    const payload = { ...form, unitPrice: Number(form.unitPrice), packSize: Number(form.packSize), stockQty: Number(form.stockQty) };
    try {
      if (editing) {
        await api.put(`/distributors/me/catalog/${editing}`, payload);
        setOk('Catalog item updated');
      } else {
        await api.post('/distributors/me/catalog', payload);
        setOk('Catalog item added');
      }
      setForm(emptyItem);
      setEditing(null);
      load();
    } catch (e) { setError(e.message); }
  };

  const remove = async (id) => {
    try {
      await api.del(`/distributors/me/catalog/${id}`);
      load();
    } catch (e) { setError(e.message); }
  };

  return (
    <DashboardLayout title="Wholesale Catalog" subtitle="Wholesale supply" navItems={distributorNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {ok && <Alert type="success" className="mb-4" onClose={() => setOk('')}>{ok}</Alert>}

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="p-6 lg:col-span-1">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-bold text-slate-900">{editing ? 'Edit item' : 'Add item'}</h2>
            {editing && <button onClick={startAdd} className="text-xs font-semibold text-brand-600 hover:text-brand-700">New item</button>}
          </div>
          <div className="space-y-3">
            <Input label="Product ID" value={form.productId} onChange={(e) => setForm({ ...form, productId: e.target.value })} placeholder="UUID" />
            <Input label="Product name" value={form.productName} onChange={(e) => setForm({ ...form, productName: e.target.value })} placeholder="e.g. Paracetamol 500mg" />
            <Input label="Unit price (₹)" type="number" value={form.unitPrice} onChange={(e) => setForm({ ...form, unitPrice: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Pack size" type="number" value={form.packSize} onChange={(e) => setForm({ ...form, packSize: e.target.value })} />
              <Input label="Stock qty" type="number" value={form.stockQty} onChange={(e) => setForm({ ...form, stockQty: e.target.value })} />
            </div>
            <Button className="w-full" onClick={save}>{editing ? 'Save changes' : 'Add to catalog'}</Button>
          </div>
        </Card>

        <div className="lg:col-span-2">
          {loading ? (
            <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
          ) : items.length === 0 ? (
            <EmptyState icon={Package} title="No catalog items" subtitle="Add your wholesale products to start selling to other shops." />
          ) : (
            <div className="space-y-3">
              {items.map((item) => (
                <Card key={item.id} className="flex items-center justify-between p-4">
                  <div className="flex items-center gap-3">
                    <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-50 text-brand-600"><Package className="h-5 w-5" /></span>
                    <div>
                      <p className="font-semibold text-slate-900">{item.productName}</p>
                      <p className="text-xs text-slate-500">
                        {fmtINR(item.unitPrice)} / pack of {item.packSize} · <span className={item.stockQty <= 0 ? 'font-semibold text-rose-600' : 'font-semibold text-emerald-600'}>{item.stockQty} in stock</span>
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge color={item.stockQty > 0 ? 'green' : 'red'}>{item.stockQty > 0 ? 'In stock' : 'Out of stock'}</Badge>
                    <button onClick={() => startEdit(item)} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-brand-600"><Pencil className="h-4 w-4" /></button>
                    <button onClick={() => remove(item.id)} className="rounded-lg p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-600"><Trash2 className="h-4 w-4" /></button>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
