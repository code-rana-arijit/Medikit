import { useEffect, useState } from 'react';
import DashboardLayout, { deliveryPartnerNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { useAuth } from '../../lib/auth';
import { Card, StatusBadge, Spinner, Alert, EmptyState, Button } from '../../components/ui';
import { MapPin, CheckCircle2, Timer, RefreshCw } from 'lucide-react';

export default function PartnerAvailable() {
  const { user } = useAuth();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [claiming, setClaiming] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = async () => {
    setError('');
    try {
      const r = await api.get('/delivery/available');
      setItems(r?.content || []);
    } catch (e) { setError(e.message); setItems([]); }
    setLoading(false);
  };

  useEffect(() => { load(); }, []);

  const claim = async (orderId) => {
    setClaiming(orderId);
    setError(''); setNotice('');
    try {
      await api.post(`/delivery/${orderId}/claim`, {});
      setNotice('Delivery claimed successfully');
      await load();
    } catch (e) { setError(e.message); }
    setClaiming(null);
  };

  return (
    <DashboardLayout title="Available deliveries" subtitle="Delivery partner" navItems={deliveryPartnerNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {notice && <Alert type="success" className="mb-4" onClose={() => setNotice('')}>{notice}</Alert>}

      <div className="mb-4 flex items-center justify-between">
        <p className="text-sm text-slate-500">{items.length} delivery request(s) waiting to be claimed.</p>
        <Button variant="secondary" size="sm" onClick={load}><RefreshCw className="h-4 w-4" /> Refresh</Button>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : items.length === 0 ? (
        <EmptyState icon={MapPin} title="No deliveries available" subtitle="New delivery requests from confirmed orders will appear here." />
      ) : (
        <div className="space-y-3">
          {items.map((d) => (
            <Card key={d.id} className="p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <p className="font-semibold text-slate-900">Order {d.orderId.slice(0, 8)}</p>
                    <StatusBadge status={d.status} />
                  </div>
                  <div className="mt-1 grid gap-1 text-xs text-slate-500 sm:grid-cols-2">
                    <span className="font-mono">Order: {d.orderId}</span>
                    <span className="flex items-center gap-1"><Timer className="h-3.5 w-3.5" /> {d.estimatedMinutes} min estimate</span>
                    <span className="flex items-center gap-1 font-mono"><MapPin className="h-3.5 w-3.5" /> Customer {d.customerLatitude?.toFixed?.(4) ?? '—'}, {d.customerLongitude?.toFixed?.(4) ?? '—'}</span>
                    <span>Pharmacy: <span className="font-mono">{d.pharmacyId?.slice(0, 8)}</span></span>
                  </div>
                </div>
                <Button onClick={() => claim(d.orderId)} disabled={claiming === d.orderId} loading={claiming === d.orderId}>
                  <CheckCircle2 className="h-4 w-4" /> Claim
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </DashboardLayout>
  );
}
