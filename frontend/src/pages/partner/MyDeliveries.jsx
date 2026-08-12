import { useEffect, useState } from 'react';
import DashboardLayout, { deliveryPartnerNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { Card, StatusBadge, Spinner, Alert, EmptyState, Select, Button } from '../../components/ui';
import { ClipboardList, MapPin, Timer, PackageCheck, RefreshCw } from 'lucide-react';

const NEXT_STATUS = {
  ASSIGNED: 'PICKED_UP',
  PICKED_UP: 'IN_TRANSIT',
  IN_TRANSIT: 'DELIVERED',
};

const ACTIVE = ['ASSIGNED', 'PICKED_UP', 'IN_TRANSIT'];

export default function PartnerDeliveries() {
  const [items, setItems] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = async () => {
    setError('');
    try {
      const q = statusFilter ? `?status=${encodeURIComponent(statusFilter)}` : '';
      const r = await api.get(`/delivery/partner${q}`);
      setItems(r?.content || []);
    } catch (e) { setError(e.message); setItems([]); }
    setLoading(false);
  };

  useEffect(() => { load(); }, [statusFilter]);

  const advance = async (d) => {
    const next = NEXT_STATUS[d.status];
    if (!next) return;
    setUpdating(d.id);
    setError(''); setNotice('');
    try {
      await api.put(`/delivery/${d.orderId}/status`, {
        status: next,
        coordinates: next === 'DELIVERED' ? { latitude: d.partnerLatitude ?? null, longitude: d.partnerLongitude ?? null } : null,
      });
      setNotice(`Delivery marked as ${next}`);
      await load();
    } catch (e) { setError(e.message); }
    setUpdating(null);
  };

  const filtered = statusFilter ? items : items.filter((d) => ACTIVE.includes(d.status) || d.status === 'DELIVERED');

  return (
    <DashboardLayout title="My deliveries" subtitle="Delivery partner" navItems={deliveryPartnerNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {notice && <Alert type="success" className="mb-4" onClose={() => setNotice('')}>{notice}</Alert>}

      <div className="mb-4 flex flex-wrap items-end gap-3">
        <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="w-48">
          <option value="">Active & delivered</option>
          <option value="ASSIGNED">ASSIGNED</option>
          <option value="PICKED_UP">PICKED_UP</option>
          <option value="IN_TRANSIT">IN_TRANSIT</option>
          <option value="DELIVERED">DELIVERED</option>
          <option value="CANCELLED">CANCELLED</option>
        </Select>
        <Button variant="secondary" size="sm" onClick={load}><RefreshCw className="h-4 w-4" /> Refresh</Button>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : filtered.length === 0 ? (
        <EmptyState icon={ClipboardList} title="No deliveries" subtitle="Deliveries you have claimed will appear here." />
      ) : (
        <div className="space-y-3">
          {filtered.map((d) => (
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
                  {d.deliveredAt && (
                    <p className="mt-1 flex items-center gap-1 text-xs text-emerald-600">
                      <PackageCheck className="h-3.5 w-3.5" /> Delivered {new Date(d.deliveredAt).toLocaleString('en-IN')}
                    </p>
                  )}
                </div>
                {NEXT_STATUS[d.status] && (
                  <Button onClick={() => advance(d)} disabled={updating === d.id} loading={updating === d.id} size="sm">
                    Mark {NEXT_STATUS[d.status]}
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
