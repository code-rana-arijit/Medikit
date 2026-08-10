import { useEffect, useState } from 'react';
import DashboardLayout, { pharmacyNav } from '../../components/DashboardLayout';
import { api, fmtINR } from '../../lib/api';
import { Card, Select, StatusBadge, Spinner, EmptyState, Alert, Input, Button } from '../../components/ui';
import { ClipboardList } from 'lucide-react';

export default function PharmacyOrders() {
  const [pharmacyId, setPharmacyId] = useState('');
  const [orders, setOrders] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const load = async () => {
    if (!pharmacyId) { setError('Enter a pharmacy ID first'); return; }
    setError(''); setLoading(true);
    try {
      const r = await api.get(`/orders/pharmacy/${pharmacyId}`);
      setOrders(r?.content || []);
    } catch (e) { setError(e.message); setOrders([]); }
    setLoading(false);
  };

  useEffect(() => { if (pharmacyId) load(); }, [statusFilter]);

  const filtered = statusFilter ? orders.filter((o) => o.status === statusFilter) : orders;

  return (
    <DashboardLayout title="Pharmacy Orders" subtitle="Store operations" navItems={pharmacyNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}

      <div className="mb-4 flex flex-wrap items-end gap-3">
        <Input label="Pharmacy ID" value={pharmacyId} onChange={(e) => setPharmacyId(e.target.value)} placeholder="UUID" className="w-80" />
        <Button onClick={load} disabled={loading}>{loading ? 'Loading…' : 'Load orders'}</Button>
        <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="w-44">
          <option value="">All statuses</option>
          {['PLACED', 'CONFIRMED', 'IN_FULFILLMENT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'REFUNDED'].map((s) => <option key={s} value={s}>{s}</option>)}
        </Select>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : orders.length === 0 && pharmacyId ? (
        <EmptyState icon={ClipboardList} title="No orders found" subtitle="Orders placed at this pharmacy will appear here." />
      ) : (
        <div className="space-y-3">
          {filtered.map((o) => (
            <Card key={o.id} className="flex flex-wrap items-center justify-between gap-3 p-4">
              <div>
                <p className="font-semibold text-slate-900">{o.orderNumber}</p>
                <p className="text-xs text-slate-500">
                  {new Date(o.createdAt).toLocaleString('en-IN')} · {o.items.length} item(s) · {o.paymentMethod}
                </p>
                <div className="mt-1 flex flex-wrap gap-1.5">
                  {o.items.slice(0, 3).map((it, i) => (
                    <span key={i} className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">{it.productName} ×{it.quantity}</span>
                  ))}
                  {o.items.length > 3 && <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500">+{o.items.length - 3}</span>}
                </div>
              </div>
              <div className="flex flex-col items-end gap-1">
                <p className="font-bold text-slate-900">{fmtINR(o.total)}</p>
                <StatusBadge status={o.status} />
              </div>
            </Card>
          ))}
        </div>
      )}
    </DashboardLayout>
  );
}
