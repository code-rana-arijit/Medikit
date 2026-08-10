import { useEffect, useState } from 'react';
import DashboardLayout, { distributorNav } from '../../components/DashboardLayout';
import { api, fmtINR } from '../../lib/api';
import { Card, Button, Select, StatusBadge, Spinner, EmptyState, Alert } from '../../components/ui';
import { ShoppingCart, ArrowRight } from 'lucide-react';

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

  const renderRow = (o, isIncoming) => (
    <Card key={o.id} className="flex flex-wrap items-center justify-between gap-3 p-4">
      <div className="min-w-0">
        <p className="font-semibold text-slate-900">{o.orderNumber}</p>
        <p className="text-xs text-slate-500">
          {new Date(o.createdAt).toLocaleString('en-IN')} · {o.items.length} item(s)
        </p>
        <p className="mt-1 text-sm font-bold text-slate-800">{fmtINR(o.totalAmount)}</p>
      </div>
      <div className="flex items-center gap-3">
        <StatusBadge status={o.status} />
        {isIncoming && NEXT_STATUS[o.status] && (
          <Button size="sm" variant="secondary" onClick={() => advance(o.id)}>
            {NEXT_STATUS[o.status] === 'CONFIRMED' ? 'Confirm' : NEXT_STATUS[o.status] === 'SHIPPED' ? 'Ship' : 'Deliver'}
            <ArrowRight className="ml-1 h-3.5 w-3.5" />
          </Button>
        )}
      </div>
    </Card>
  );

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
