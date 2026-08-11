import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout, { pharmacyNav } from '../../components/DashboardLayout';
import { api, fmtINR } from '../../lib/api';
import { Card, Select, StatusBadge, Spinner, EmptyState, Alert, Input, Button } from '../../components/ui';
import { ClipboardList, ChevronDown, ChevronUp, MapPinned } from 'lucide-react';

export default function PharmacyOrders() {
  const [pharmacyId, setPharmacyId] = useState('');
  const [orders, setOrders] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [expanded, setExpanded] = useState(null);

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
          {['CREATED', 'PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'FAILED'].map((s) => <option key={s} value={s}>{s}</option>)}
        </Select>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : orders.length === 0 && pharmacyId ? (
        <EmptyState icon={ClipboardList} title="No orders found" subtitle="Orders placed at this pharmacy will appear here." />
      ) : (
        <div className="space-y-3">
          {filtered.map((o) => {
            const isOpen = expanded === o.id;
            return (
              <Card key={o.id} className="overflow-hidden">
                <div className="flex flex-wrap items-center justify-between gap-3 p-4">
                  <button onClick={() => setExpanded(isOpen ? null : o.id)} className="flex min-w-0 flex-1 items-center gap-3 text-left">
                    <div className="min-w-0">
                      <p className="font-semibold text-slate-900">{o.orderNumber}</p>
                      <p className="text-xs text-slate-500">
                        {new Date(o.createdAt).toLocaleString('en-IN')} · {o.paymentMethod} · {o.paymentStatus}
                      </p>
                      <div className="mt-1 flex flex-wrap gap-1.5">
                        {o.items.slice(0, 3).map((it, i) => (
                          <span key={i} className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">{it.productName} ×{it.quantity}</span>
                        ))}
                        {o.items.length > 3 && <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500">+{o.items.length - 3}</span>}
                      </div>
                    </div>
                  </button>
                  <div className="flex items-center gap-3">
                    <p className="font-bold text-slate-900">{fmtINR(o.total)}</p>
                    <StatusBadge status={o.status} />
                    <Link to={`/orders/${o.id}`} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-brand-600"><MapPinned className="h-4 w-4" /></Link>
                    <button onClick={() => setExpanded(isOpen ? null : o.id)} className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100">
                      {isOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                {isOpen && (
                  <div className="border-t border-slate-100 bg-slate-50/60 px-4 py-4">
                    <div className="mb-3 grid gap-3 text-xs text-slate-500 sm:grid-cols-3">
                      <div><span className="font-semibold text-slate-700">Customer</span><p className="font-mono">{o.userId}</p></div>
                      <div><span className="font-semibold text-slate-700">Address</span><p>{o.deliveryAddress || '—'}</p></div>
                      <div><span className="font-semibold text-slate-700">Slot</span><p className="font-mono">{o.deliverySlotId || '—'}</p></div>
                    </div>
                    <div className="space-y-2">
                      {o.items.map((it, i) => (
                        <div key={i} className="flex items-center justify-between rounded-xl bg-white px-4 py-2.5 text-sm">
                          <div>
                            <p className="font-medium text-slate-800">{it.productName}</p>
                            <p className="text-xs text-slate-400">{fmtINR(it.unitPrice)} × {it.quantity} · MRP {fmtINR(it.mrp)}</p>
                          </div>
                          <p className="font-bold text-slate-900">{fmtINR(it.lineTotal)}</p>
                        </div>
                      ))}
                    </div>
                    <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1 border-t border-slate-200 pt-3 text-sm">
                      <span className="text-slate-500">Subtotal <b className="text-slate-900">{fmtINR(o.subtotal)}</b></span>
                      <span className="text-slate-500">Delivery <b className="text-slate-900">{fmtINR(o.deliveryFee)}</b></span>
                      <span className="text-emerald-600">Discount <b>-{fmtINR(o.discount)}</b></span>
                      <span className="text-slate-900 font-bold">Total {fmtINR(o.total)}</span>
                      {o.cancellationReason && <span className="text-rose-600">Reason: {o.cancellationReason}</span>}
                    </div>
                  </div>
                )}
              </Card>
            );
          })}
        </div>
      )}
    </DashboardLayout>
  );
}
