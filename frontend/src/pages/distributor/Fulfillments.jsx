import { useEffect, useState } from 'react';
import DashboardLayout, { distributorNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { Card, Button, Select, StatusBadge, Spinner, EmptyState, Alert } from '../../components/ui';
import { Truck } from 'lucide-react';

const NEXT_STATUS = {
  CLAIMED: 'PICKED_UP',
  PICKED_UP: 'IN_TRANSIT',
  IN_TRANSIT: 'DELIVERED',
};

export default function DistributorFulfillments() {
  const [fulfillments, setFulfillments] = useState([]);
  const [claimId, setClaimId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');

  const load = async () => {
    try { setFulfillments(await api.get('/fulfillments')); } catch { setFulfillments([]); }
    setLoading(false);
  };

  useEffect(() => { load(); }, []);

  const claim = async () => {
    setError(''); setOk('');
    try {
      await api.post('/fulfillments/claim', { orderId: claimId });
      setOk('Order claimed — start fulfilment');
      setClaimId('');
      load();
    } catch (e) { setError(e.message); }
  };

  const advance = async (id) => {
    const f = fulfillments.find((x) => x.id === id);
    const target = NEXT_STATUS[f?.status];
    if (!target) return;
    try {
      await api.patch(`/fulfillments/${id}/status`, { status: target });
      load();
    } catch (e) { setError(e.message); }
  };

  return (
    <DashboardLayout title="Retail Fulfillments" subtitle="Wholesale supply" navItems={distributorNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {ok && <Alert type="success" className="mb-4" onClose={() => setOk('')}>{ok}</Alert>}

      <Card className="mb-6 p-5">
        <h2 className="font-bold text-slate-900">Claim a retail order</h2>
        <p className="mt-1 text-sm text-slate-500">Enter a customer order ID to claim and deliver it as a distributor.</p>
        <div className="mt-3 flex gap-2">
          <input
            value={claimId}
            onChange={(e) => setClaimId(e.target.value)}
            placeholder="Order UUID"
            className="flex-1 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none focus:border-brand-400 focus:ring-2 focus:ring-brand-100"
          />
          <Button onClick={claim}>Claim</Button>
        </div>
      </Card>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : fulfillments.length === 0 ? (
        <EmptyState icon={Truck} title="No fulfillments yet" subtitle="Claimed retail orders will appear here with live status." />
      ) : (
        <div className="space-y-3">
          {fulfillments.map((f) => (
            <Card key={f.id} className="flex flex-wrap items-center justify-between gap-3 p-4">
              <div>
                <p className="font-semibold text-slate-900">Order {f.orderId}</p>
                <p className="text-xs text-slate-500">Claimed {new Date(f.createdAt).toLocaleString('en-IN')}</p>
                {f.deliveredAt && <p className="text-xs text-slate-500">Delivered {new Date(f.deliveredAt).toLocaleString('en-IN')}</p>}
              </div>
              <div className="flex items-center gap-3">
                <StatusBadge status={f.status} />
                {NEXT_STATUS[f.status] && (
                  <Button size="sm" variant="secondary" onClick={() => advance(f.id)}>
                    {NEXT_STATUS[f.status] === 'PICKED_UP' ? 'Mark picked up' : NEXT_STATUS[f.status] === 'IN_TRANSIT' ? 'In transit' : 'Delivered'}
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
    </DashboardLayout>
  );
}
