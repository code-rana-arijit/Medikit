import { useEffect, useState } from 'react';
import DashboardLayout, { distributorNav } from '../../components/DashboardLayout';
import { api, fmtINR } from '../../lib/api';
import { Card, Button, Select, StatusBadge, Spinner, EmptyState, Alert } from '../../components/ui';
import { ShoppingCart, ArrowRight, ChevronDown, ChevronUp, Boxes } from 'lucide-react';

const NEXT_STATUS = {
  PENDING: 'CONFIRMED',
  CONFIRMED: 'SHIPPED',
  SHIPPED: 'DELIVERED',
};

export default function DistributorOrders() {
  const [tab, setTab] = useState('incoming');
  const [incoming, setIncoming] = useState([]);
  const [outgoing, setOutgoing] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expanded, setExpanded] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const [inc, out] = await Promise.all([
        api.get(`/distributor-orders/distributor${statusFilter ? `?status=${statusFilter}` : ''}`).catch(() => ({ content: [] })),
        api.get(`/distributor-orders${statusFilter ? `?status=${statusFilter}` : ''}`).catch(() => ({ content: [] })),
      ]);
      setIncoming(inc?.content || []);
      setOutgoing(out?.content || []);
    } finally { setLoading(false); }
  };

  useEffect(() => { load(); }, [statusFilter]);

  const advance = async (id) => {
    const order = [...incoming, ...outgoing].find((o) => o.id === id);
    const target = NEXT_STATUS[order?.status];
    if (!target) return;
    try {
      await api.patch(`/distributor-orders/${id}/status?status=${target}`);
      load();
    } catch (e) { setError(e.message); }
  };

  const renderRow = (o, isIncoming) => {
    const isOpen = expanded === o.id;
    return (
      <Card key={o.id} className="overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-3 p-4">
          <button
            onClick={() => setExpanded(isOpen ? null : o.id)}
            className="flex min-w-0 flex-1 items-center gap-3 text-left"
          >
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-600"><Boxes className="h-4.5 w-4.5" /></span>
            <div className="min-w-0">
              <p className="font-semibold text-slate-900">{o.orderNumber}</p>
              <p className="text-xs text-slate-500">
                {new Date(o.createdAt).toLocaleString('en-IN')} · {o.items.length} item(s) · updated {new Date(o.updatedAt).toLocaleString('en-IN')}
              </p>
            </div>
          </button>
          <div className="flex items-center gap-3">
            <p className="font-bold text-slate-900">{fmtINR(o.totalAmount)}</p>
            <StatusBadge status={o.status} />
            {isIncoming && NEXT_STATUS[o.status] && (
              <Button size="sm" variant="secondary" onClick={() => advance(o.id)}>
                {NEXT_STATUS[o.status] === 'CONFIRMED' ? 'Confirm' : NEXT_STATUS[o.status] === 'SHIPPED' ? 'Ship' : 'Deliver'}
                <ArrowRight className="ml-1 h-3.5 w-3.5" />
              </Button>
            )}
            <button onClick={() => setExpanded(isOpen ? null : o.id)} className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100">
              {isOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </button>
          </div>
        </div>

        {isOpen && (
          <div className="border-t border-slate-100 bg-slate-50/60 px-4 py-4">
            <div className="mb-3 grid gap-3 text-xs text-slate-500 sm:grid-cols-3">
              <div><span className="font-semibold text-slate-700">Order ID</span><p className="font-mono">{o.id}</p></div>
              <div><span className="font-semibold text-slate-700">Buyer</span><p className="font-mono">{isIncoming ? o.buyerUserId : 'You'}</p></div>
              <div><span className="font-semibold text-slate-700">Distributor</span><p className="font-mono">{o.distributorId}</p></div>
            </div>
            <div className="space-y-2">
              {o.items.map((it, i) => (
                <div key={i} className="flex items-center justify-between rounded-xl bg-white px-4 py-2.5 text-sm">
                  <div>
                    <p className="font-medium text-slate-800">{it.productName}</p>
                    <p className="text-xs text-slate-400">{fmtINR(it.unitPrice)} × {it.quantity}</p>
                  </div>
                  <p className="font-bold text-slate-900">{fmtINR(it.subtotal)}</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </Card>
    );
  };

  return (
    <DashboardLayout title="Supply Orders" subtitle="Wholesale supply" navItems={distributorNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}

      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex rounded-xl bg-slate-100 p-1">
          {['incoming', 'outgoing'].map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`rounded-lg px-4 py-1.5 text-sm font-semibold capitalize transition ${tab === t ? 'bg-white text-brand-700 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
            >
              {t === 'incoming' ? 'Received' : 'Placed'}
            </button>
          ))}
        </div>
        <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="w-40">
          <option value="">All statuses</option>
          {['PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'].map((s) => <option key={s} value={s}>{s}</option>)}
        </Select>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : (tab === 'incoming' ? incoming : outgoing).length === 0 ? (
        <EmptyState
          icon={ShoppingCart}
          title={tab === 'incoming' ? 'No supply orders received' : 'No orders placed'}
          subtitle={tab === 'incoming' ? 'Orders placed by other shops will appear here.' : 'Buy stock from other distributors on the Purchase page.'}
        />
      ) : (
        <div className="space-y-3">
          {(tab === 'incoming' ? incoming : outgoing).map((o) => renderRow(o, tab === 'incoming'))}
        </div>
      )}
    </DashboardLayout>
  );
}
